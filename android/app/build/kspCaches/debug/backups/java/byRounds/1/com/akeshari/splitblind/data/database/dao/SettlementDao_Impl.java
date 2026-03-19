package com.akeshari.splitblind.data.database.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.akeshari.splitblind.data.database.entity.SettlementEntity;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class SettlementDao_Impl implements SettlementDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<SettlementEntity> __insertionAdapterOfSettlementEntity;

  public SettlementDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfSettlementEntity = new EntityInsertionAdapter<SettlementEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `settlements` (`settlementId`,`groupId`,`fromMember`,`toMember`,`amountCents`,`createdAt`,`isDeleted`,`hlcTimestamp`) VALUES (?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final SettlementEntity entity) {
        statement.bindString(1, entity.getSettlementId());
        statement.bindString(2, entity.getGroupId());
        statement.bindString(3, entity.getFromMember());
        statement.bindString(4, entity.getToMember());
        statement.bindLong(5, entity.getAmountCents());
        statement.bindLong(6, entity.getCreatedAt());
        final int _tmp = entity.isDeleted() ? 1 : 0;
        statement.bindLong(7, _tmp);
        statement.bindLong(8, entity.getHlcTimestamp());
      }
    };
  }

  @Override
  public Object insertSettlement(final SettlementEntity settlement,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfSettlementEntity.insert(settlement);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<SettlementEntity>> getSettlements(final String groupId) {
    final String _sql = "SELECT * FROM settlements WHERE groupId = ? AND isDeleted = 0 ORDER BY createdAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, groupId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"settlements"}, new Callable<List<SettlementEntity>>() {
      @Override
      @NonNull
      public List<SettlementEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfSettlementId = CursorUtil.getColumnIndexOrThrow(_cursor, "settlementId");
          final int _cursorIndexOfGroupId = CursorUtil.getColumnIndexOrThrow(_cursor, "groupId");
          final int _cursorIndexOfFromMember = CursorUtil.getColumnIndexOrThrow(_cursor, "fromMember");
          final int _cursorIndexOfToMember = CursorUtil.getColumnIndexOrThrow(_cursor, "toMember");
          final int _cursorIndexOfAmountCents = CursorUtil.getColumnIndexOrThrow(_cursor, "amountCents");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfIsDeleted = CursorUtil.getColumnIndexOrThrow(_cursor, "isDeleted");
          final int _cursorIndexOfHlcTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "hlcTimestamp");
          final List<SettlementEntity> _result = new ArrayList<SettlementEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final SettlementEntity _item;
            final String _tmpSettlementId;
            _tmpSettlementId = _cursor.getString(_cursorIndexOfSettlementId);
            final String _tmpGroupId;
            _tmpGroupId = _cursor.getString(_cursorIndexOfGroupId);
            final String _tmpFromMember;
            _tmpFromMember = _cursor.getString(_cursorIndexOfFromMember);
            final String _tmpToMember;
            _tmpToMember = _cursor.getString(_cursorIndexOfToMember);
            final long _tmpAmountCents;
            _tmpAmountCents = _cursor.getLong(_cursorIndexOfAmountCents);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final boolean _tmpIsDeleted;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsDeleted);
            _tmpIsDeleted = _tmp != 0;
            final long _tmpHlcTimestamp;
            _tmpHlcTimestamp = _cursor.getLong(_cursorIndexOfHlcTimestamp);
            _item = new SettlementEntity(_tmpSettlementId,_tmpGroupId,_tmpFromMember,_tmpToMember,_tmpAmountCents,_tmpCreatedAt,_tmpIsDeleted,_tmpHlcTimestamp);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getSettlementsList(final String groupId,
      final Continuation<? super List<SettlementEntity>> $completion) {
    final String _sql = "SELECT * FROM settlements WHERE groupId = ? AND isDeleted = 0";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, groupId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<SettlementEntity>>() {
      @Override
      @NonNull
      public List<SettlementEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfSettlementId = CursorUtil.getColumnIndexOrThrow(_cursor, "settlementId");
          final int _cursorIndexOfGroupId = CursorUtil.getColumnIndexOrThrow(_cursor, "groupId");
          final int _cursorIndexOfFromMember = CursorUtil.getColumnIndexOrThrow(_cursor, "fromMember");
          final int _cursorIndexOfToMember = CursorUtil.getColumnIndexOrThrow(_cursor, "toMember");
          final int _cursorIndexOfAmountCents = CursorUtil.getColumnIndexOrThrow(_cursor, "amountCents");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfIsDeleted = CursorUtil.getColumnIndexOrThrow(_cursor, "isDeleted");
          final int _cursorIndexOfHlcTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "hlcTimestamp");
          final List<SettlementEntity> _result = new ArrayList<SettlementEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final SettlementEntity _item;
            final String _tmpSettlementId;
            _tmpSettlementId = _cursor.getString(_cursorIndexOfSettlementId);
            final String _tmpGroupId;
            _tmpGroupId = _cursor.getString(_cursorIndexOfGroupId);
            final String _tmpFromMember;
            _tmpFromMember = _cursor.getString(_cursorIndexOfFromMember);
            final String _tmpToMember;
            _tmpToMember = _cursor.getString(_cursorIndexOfToMember);
            final long _tmpAmountCents;
            _tmpAmountCents = _cursor.getLong(_cursorIndexOfAmountCents);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final boolean _tmpIsDeleted;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsDeleted);
            _tmpIsDeleted = _tmp != 0;
            final long _tmpHlcTimestamp;
            _tmpHlcTimestamp = _cursor.getLong(_cursorIndexOfHlcTimestamp);
            _item = new SettlementEntity(_tmpSettlementId,_tmpGroupId,_tmpFromMember,_tmpToMember,_tmpAmountCents,_tmpCreatedAt,_tmpIsDeleted,_tmpHlcTimestamp);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
