package com.akeshari.splitblind.sync

import android.util.Log
import com.akeshari.splitblind.crypto.CryptoEngine
import com.akeshari.splitblind.crypto.EncryptedEnvelope
import com.akeshari.splitblind.data.database.dao.ExpenseDao
import com.akeshari.splitblind.data.database.dao.GroupDao
import com.akeshari.splitblind.data.database.dao.ProcessedOpDao
import com.akeshari.splitblind.data.database.dao.SettlementDao
import com.akeshari.splitblind.data.database.entity.ExpenseEntity
import com.akeshari.splitblind.data.database.entity.MemberEntity
import com.akeshari.splitblind.data.database.entity.ProcessedOpEntity
import com.akeshari.splitblind.data.database.entity.SettlementEntity
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import javax.inject.Inject
import javax.inject.Singleton

// Matches the web app's op format: {id, type, data: {...}, hlc, author}
@Serializable
data class OpPayload(
    val id: String,
    val type: String,
    val data: OpData,
    val hlc: Long = 0,
    val author: String = ""
)

@Serializable
data class OpData(
    // member_join
    val memberId: String? = null,
    val displayName: String? = null,
    val joinedAt: Long? = null,
    // expense
    val expenseId: String? = null,
    val groupId: String? = null,
    val description: String? = null,
    val amountCents: Long? = null,
    val currency: String? = null,
    val paidBy: String? = null,
    val splitAmong: List<String>? = null,
    val createdAt: Long? = null,
    val isDeleted: Boolean = false,
    val tag: String? = null,
    val paidByMap: Map<String, Long>? = null,
    val splitMode: String? = null,
    val splitDetails: Map<String, Long>? = null,
    // settlement
    val settlementId: String? = null,
    val fromMember: String? = null,
    val toMember: String? = null
)

private val json = Json { ignoreUnknownKeys = true }

@Singleton
class SyncEngine @Inject constructor(
    private val firebaseDatabase: FirebaseDatabase,
    private val groupDao: GroupDao,
    private val expenseDao: ExpenseDao,
    private val settlementDao: SettlementDao,
    private val processedOpDao: ProcessedOpDao
) {
    private val TAG = "SyncEngine"
    private val listeners = mutableMapOf<String, Pair<DatabaseReference, ChildEventListener>>()
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _syncStatus = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val syncStatus: StateFlow<Map<String, Boolean>> = _syncStatus

    fun fullSync(groupId: String, groupKeyBase64: String) {
        firebaseDatabase.getReference("groups/$groupId/ops").orderByChild("t").get()
            .addOnSuccessListener { snapshot ->
                scope.launch {
                    for (child in snapshot.children) {
                        try {
                            val d = child.child("d").getValue(String::class.java) ?: continue
                            val n = child.child("n").getValue(String::class.java) ?: continue
                            val envelope = EncryptedEnvelope(d = d, n = n)
                            val plaintext = CryptoEngine.decrypt(groupKeyBase64, envelope)
                            val op = json.decodeFromString<OpPayload>(plaintext)
                            processOp(op, groupId)
                            child.key?.let { processedOpDao.markProcessed(ProcessedOpEntity(opId = it)) }
                        } catch (e: Exception) {
                            Log.w(TAG, "Full sync: failed to process op ${child.key}", e)
                        }
                    }
                }
            }
            .addOnFailureListener { e -> Log.e(TAG, "Full sync failed for $groupId", e) }
    }

    fun startListening(groupId: String, groupKeyBase64: String) {
        if (listeners.containsKey(groupId)) return
        // Do a full pull first
        fullSync(groupId, groupKeyBase64)

        val ref = firebaseDatabase.getReference("groups/$groupId/ops")
        val listener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                scope.launch {
                    try {
                        val opId = snapshot.key ?: return@launch
                        if (processedOpDao.isProcessed(opId)) return@launch

                        val d = snapshot.child("d").getValue(String::class.java) ?: return@launch
                        val n = snapshot.child("n").getValue(String::class.java) ?: return@launch

                        val envelope = EncryptedEnvelope(d = d, n = n)
                        val plaintext = CryptoEngine.decrypt(groupKeyBase64, envelope)
                        val op = json.decodeFromString<OpPayload>(plaintext)

                        processOp(op, groupId)
                        processedOpDao.markProcessed(ProcessedOpEntity(opId = opId))
                        updateSyncStatus(groupId, true)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing op: ${snapshot.key}", e)
                    }
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Firebase listener cancelled: ${error.message}")
                updateSyncStatus(groupId, false)
            }
        }

        ref.addChildEventListener(listener)
        listeners[groupId] = Pair(ref, listener)
        updateSyncStatus(groupId, true)
    }

    fun stopListening(groupId: String) {
        listeners.remove(groupId)?.let { (ref, listener) ->
            ref.removeEventListener(listener)
        }
        _syncStatus.value = _syncStatus.value - groupId
    }

    fun pushOp(groupId: String, groupKeyBase64: String, payload: OpPayload) {
        val plaintext = json.encodeToString(payload)
        val envelope = CryptoEngine.encrypt(groupKeyBase64, plaintext)
        val ref = firebaseDatabase.getReference("groups/$groupId/ops").push()
        val opId = ref.key ?: return

        val data = mapOf(
            "d" to envelope.d,
            "n" to envelope.n,
            "t" to ServerValue.TIMESTAMP
        )

        // Pre-mark as processed so we don't re-process our own op
        scope.launch {
            processedOpDao.markProcessed(ProcessedOpEntity(opId = opId))
        }

        ref.setValue(data).addOnFailureListener { e ->
            Log.e(TAG, "Failed to push op", e)
        }
    }

    private suspend fun processOp(op: OpPayload, groupId: String) {
        val d = op.data
        val now = System.currentTimeMillis()
        when (op.type) {
            "member_join" -> {
                groupDao.insertMember(
                    MemberEntity(
                        groupId = groupId,
                        memberId = d.memberId ?: op.author,
                        displayName = d.displayName ?: "Unknown",
                        joinedAt = d.joinedAt ?: now,
                        hlcTimestamp = op.hlc
                    )
                )
            }
            "expense" -> {
                expenseDao.insertExpense(
                    ExpenseEntity(
                        expenseId = d.expenseId ?: op.id,
                        groupId = groupId,
                        description = d.description ?: "",
                        amountCents = d.amountCents ?: 0,
                        currency = d.currency ?: "INR",
                        paidBy = d.paidBy ?: "",
                        splitAmong = json.encodeToString(d.splitAmong ?: emptyList()),
                        createdAt = d.createdAt ?: now,
                        hlcTimestamp = op.hlc,
                        tag = d.tag,
                        paidByMap = d.paidByMap?.let { json.encodeToString(it) },
                        splitMode = d.splitMode,
                        splitDetails = d.splitDetails?.let { json.encodeToString(it) }
                    )
                )
            }
            "settlement" -> {
                settlementDao.insertSettlement(
                    SettlementEntity(
                        settlementId = d.settlementId ?: op.id,
                        groupId = groupId,
                        fromMember = d.fromMember ?: "",
                        toMember = d.toMember ?: "",
                        amountCents = d.amountCents ?: 0,
                        createdAt = d.createdAt ?: now,
                        hlcTimestamp = op.hlc
                    )
                )
            }
        }
    }

    /**
     * Create a short invite code in Firebase at links/{code} = {g: groupId, n: name}.
     * Returns the 8-char code.
     */
    fun createShortCode(groupId: String, groupName: String, groupKey: String): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        val code = (1..8).map { chars.random() }.joinToString("")
        val data = mapOf("g" to groupId, "n" to groupName, "k" to groupKey)
        firebaseDatabase.getReference("links/$code").setValue(data)
            .addOnFailureListener { e -> Log.e(TAG, "Failed to create short link", e) }
        return code
    }

    /**
     * Resolve a short code from Firebase at links/{code}.
     * Calls onResult with (groupId, groupName) or null if not found.
     */
    fun resolveShortCode(code: String, onResult: (Triple<String, String, String?>?) -> Unit) {
        firebaseDatabase.getReference("links/$code").get()
            .addOnSuccessListener { snapshot ->
                val groupId = snapshot.child("g").getValue(String::class.java)
                val name = snapshot.child("n").getValue(String::class.java) ?: "Shared Group"
                val key = snapshot.child("k").getValue(String::class.java)
                if (groupId != null) {
                    onResult(Triple(groupId, name, key))
                } else {
                    onResult(null)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to resolve short code: $code", e)
                onResult(null)
            }
    }

    private fun updateSyncStatus(groupId: String, connected: Boolean) {
        _syncStatus.value = _syncStatus.value + (groupId to connected)
    }
}
