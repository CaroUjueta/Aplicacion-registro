package com.example.registro

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.example.registro.data.UsuarioRepository
import com.example.registro.ui.screens.LoginScreen
import com.example.registro.ui.theme.AppTheme

class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (UsuarioRepository.haySesion) {
            irAlMapa(UsuarioRepository.emailActual)
            return
        }
        enableEdgeToEdge()
        setContent {
            AppTheme {
                LoginScreen(
                    modifier = Modifier.fillMaxSize(),
                    onLogin = { email -> irAlMapa(email) },
                    onRegistrar = { startActivity(Intent(this, RegisterActivity::class.java)) }
                )
            }
        }
    }

    private fun irAlMapa(email: String?) {
        val intent = Intent(this, MapActivity::class.java).putExtra(Extras.EMAIL, email)
        startActivity(intent)
        finish()
    }
}
