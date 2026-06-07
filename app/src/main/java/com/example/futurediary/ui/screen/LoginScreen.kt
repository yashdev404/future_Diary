package com.example.futurediary.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    val auth = FirebaseAuth.getInstance()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Welcome to Future Diary", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(32.dp))
        
        OutlinedTextField(
            value = email,
            onValueChange = { 
                email = it
                error = null 
            },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = password,
            onValueChange = { 
                password = it
                error = null
            },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        )
        
        if (error != null) {
            Text(
                text = error!!, 
                color = MaterialTheme.colorScheme.error, 
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        if (isLoading) {
            CircularProgressIndicator()
        } else {
            Button(
                onClick = {
                    if (email.isNotBlank() && password.isNotBlank()) {
                        isLoading = true
                        auth.signInWithEmailAndPassword(email.trim(), password)
                            .addOnCompleteListener { task ->
                                isLoading = false
                                if (task.isSuccessful) {
                                    onLoginSuccess()
                                } else {
                                    error = task.exception?.message
                                }
                            }
                    } else {
                        error = "Please fill in all fields"
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Login")
            }
            
            TextButton(
                onClick = {
                    if (email.isNotBlank() && password.isNotBlank()) {
                        isLoading = true
                        auth.createUserWithEmailAndPassword(email.trim(), password)
                            .addOnCompleteListener { task ->
                                isLoading = false
                                if (task.isSuccessful) {
                                    onLoginSuccess()
                                } else {
                                    error = task.exception?.message
                                }
                            }
                    } else {
                        error = "Please fill in all fields"
                    }
                }
            ) {
                Text("Don't have an account? Sign Up")
            }
        }
    }
}
