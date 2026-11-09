package com.example.registro

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.example.registro.viewmodels.AuthEvent
import com.example.registro.viewmodels.AuthViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import com.example.registro.ui.screens.MapScreen
import com.example.registro.ui.theme.AppTheme
import com.example.registro.viewmodels.MapViewModel

class MapActivity : ComponentActivity() {
    private val viewModel: MapViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            irALogin()
            return
        }
        viewModel.observar(uid)
        lifecycleScope.launch {
            authViewModel.events.collect { if (it is AuthEvent.IrALogin) irALogin() }
        }
        val nombre = intent.getStringExtra(Extras.NOMBRE)
        val email = intent.getStringExtra(Extras.EMAIL)
        setContent {
            AppTheme {
                MapScreen(
                    viewModel = viewModel,
                    nombreBienvenida = nombre,
                    modifier = Modifier.fillMaxSize(),
                    onPerfil = {
                        val perfil = viewModel.state.value.miPerfil
                        startActivity(
                            Intent(this, ProfileActivity::class.java)
                                .putExtra(Extras.UID, uid)
                                .putExtra(Extras.EMAIL, perfil?.email ?: email)
                        )
                    },
                    onCerrarSesion = {
                        // primero se marca al usuario fuera de línea y después se cierra la sesión
                        viewModel.detenerRastreo { authViewModel.logout() }
                    }
                )
            }
        }
    }

    private fun irALogin() {
        startActivity(
            Intent(this, LoginActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
        finish()
    }
}
