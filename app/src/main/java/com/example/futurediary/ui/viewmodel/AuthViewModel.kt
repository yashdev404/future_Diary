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

    fun onLoginSuccess() {
        _isUserLoggedIn.value = true
    }

    fun logout() {
        auth.signOut()
        _isUserLoggedIn.value = false
    }
}
