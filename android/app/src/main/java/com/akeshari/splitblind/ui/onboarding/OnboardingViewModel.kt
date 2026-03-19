package com.akeshari.splitblind.ui.onboarding

import androidx.lifecycle.ViewModel
import com.akeshari.splitblind.crypto.Identity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val identity: Identity
) : ViewModel() {

    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name

    fun setName(name: String) {
        _name.value = name
    }

    fun save(): Boolean {
        val trimmed = _name.value.trim()
        if (trimmed.isBlank()) return false
        identity.displayName = trimmed
        return true
    }
}
