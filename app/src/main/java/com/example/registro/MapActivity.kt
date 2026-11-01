package com.example.registro

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.example.registro.data.UsuarioRepository
import com.example.registro.ui.screens.MapScreen
import com.example.registro.ui.theme.AppTheme
import com.example.registro.viewmodels.MapViewModel

// Pantalla principal: mapa + switch de ubicación + menú (perfil / cerrar sesión)
class MapActivity : ComponentActivity() {
    private val viewModel: MapViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val nombre = intent.getStringExtra(Extras.NOMBRE)
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
                                .putExtra(Extras.UID, UsuarioRepository.uidActual)
                                .putExtra(Extras.EMAIL, perfil?.email)
                        )
                    },
                    onCerrarSesion = {
                        viewModel.cerrarSesion {
                            startActivity(
                                Intent(this, LoginActivity::class.java)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                            )
                        }
                    }
                )
            }
        }
    }
}
