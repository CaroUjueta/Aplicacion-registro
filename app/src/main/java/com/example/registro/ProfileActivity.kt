package com.example.registro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.activity.viewModels
import com.example.registro.ui.screens.ProfileScreen
import com.example.registro.viewmodels.UserViewModel
import com.example.registro.ui.theme.AppTheme

class ProfileActivity : ComponentActivity() {
    private val userViewModel: UserViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val uid = intent.getStringExtra(Extras.UID)
        if (uid == null) {
            finish()
            return
        }
        userViewModel.observar(uid)
        val email = intent.getStringExtra(Extras.EMAIL).orEmpty()
        setContent {
            AppTheme {
                ProfileScreen(uid = uid, emailInicial = email, userViewModel = userViewModel, modifier = Modifier.fillMaxSize())
            }
        }
    }
}
