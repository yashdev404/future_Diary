package com.example.futurediary.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor() : ViewModel() {
    private val auth = FirebaseAuth.getInstance()

    private val _isUserLoggedIn = MutableStateFlow(auth.currentUser != null)
    val isUserLoggedIn: StateFlow<Boolean> = _isUserLoggedIn

    private val _isAnonymous = MutableStateFlow(auth.currentUser?.isAnonymous ?: false)
    val isAnonymous: StateFlow<Boolean> = _isAnonymous

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        // Silent anonymous sign-in if no user exists
        if (auth.currentUser == null) {
            signInAnonymously()
        }
    }

    fun onLoginSuccess() {
        _isUserLoggedIn.value = true
        _isAnonymous.value = auth.currentUser?.isAnonymous ?: false
    }

    fun signInAnonymously(onSuccess: (() -> Unit)? = null) {
        _isLoading.value = true
        auth.signInAnonymously().addOnCompleteListener { task ->
            _isLoading.value = false
            if (task.isSuccessful) {
                onLoginSuccess()
                onSuccess?.invoke()
            } else {
                _error.value = task.exception?.message
            }
        }
    }

    fun logout() {
        auth.signOut()
        _isUserLoggedIn.value = false
        _isAnonymous.value = false
    }

    fun linkAccount(email: String, password: String, onComplete: (Boolean, String?) -> Unit) {
        val user = auth.currentUser ?: return
        val credential = com.google.firebase.auth.EmailAuthProvider.getCredential(email, password)
        
        _isLoading.value = true
        user.linkWithCredential(credential)
            .addOnCompleteListener { task ->
                _isLoading.value = false
                if (task.isSuccessful) {
                    _isAnonymous.value = false
                    onComplete(true, null)
                } else {
                    onComplete(false, task.exception?.message)
                }
            }
    }

    fun clearError() {
        _error.value = null
    }
}
