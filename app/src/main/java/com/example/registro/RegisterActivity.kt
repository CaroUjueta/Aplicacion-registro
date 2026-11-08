package com.example.registro

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.example.registro.ui.screens.RegisterScreen
import com.example.registro.ui.theme.AppTheme

class RegisterActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppTheme {
                RegisterScreen(
                    modifier = Modifier.fillMaxSize(),
                    onRegistrado = { nombre ->
                        val intent = Intent(this, MapActivity::class.java)
                            .putExtra(Extras.NOMBRE, nombre)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        startActivity(intent)
                    },
                    onVolver = { finish() }
                )
            }
        }
    }
}
