package com.example.registro.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.registro.R
import com.example.registro.data.UsuarioRepository
import com.example.registro.ui.components.AppTextField
import com.example.registro.ui.components.BotonPrincipal
import com.example.registro.ui.components.EnlaceAccion
import com.example.registro.utils.Validators

@Composable
fun LoginScreen(
    onLogin: (String) -> Unit,
    onRegistrar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var intentado by rememberSaveable { mutableStateOf(false) }
    var cargando by remember { mutableStateOf(false) }
    var errorServidor by remember { mutableStateOf<Int?>(null) }
    var mensajeInfo by remember { mutableStateOf<Int?>(null) }

    val errorEmail = if (intentado && !Validators.emailValido(email)) stringResource(R.string.error_email_invalid) else null
    val errorPassword = if (intentado && !Validators.passwordValida(password)) stringResource(R.string.error_password_short) else null

    Surface(modifier = modifier, color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)
        ) {
            Icon(
                imageVector = Icons.Rounded.Place,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(72.dp)
            )
            Text(stringResource(R.string.login_title), style = MaterialTheme.typography.headlineLarge)
            Text(
                stringResource(R.string.login_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            AppTextField(email, { email = it }, stringResource(R.string.field_email), error = errorEmail,
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Email, enabled = !cargando)
            AppTextField(password, { password = it }, stringResource(R.string.field_password), error = errorPassword,
                isPassword = true, enabled = !cargando)

            errorServidor?.let {
                Text(stringResource(it), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
            mensajeInfo?.let {
                Text(
                    stringResource(it),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }

            BotonPrincipal(
                texto = stringResource(R.string.login_button),
                onClick = {
                    intentado = true
                    if (Validators.emailValido(email) && Validators.passwordValida(password)) {
                        cargando = true
                        errorServidor = null
                        mensajeInfo = null
                        UsuarioRepository.iniciarSesion(email, password) { resultado ->
                            cargando = false
                            resultado.onSuccess { onLogin(email.trim()) }
                                .onFailure { errorServidor = Validators.mensajeError(it) }
                        }
                    }
                },
                cargando = cargando,
            )
            EnlaceAccion(
                texto = stringResource(R.string.forgot_password),
                onClick = {
                    mensajeInfo = null
                    if (!Validators.emailValido(email)) {
                        errorServidor = R.string.error_email_required_reset
                    } else {
                        cargando = true
                        errorServidor = null
                        UsuarioRepository.recuperarContrasena(email) { resultado ->
                            cargando = false
                            resultado.onSuccess { mensajeInfo = R.string.reset_email_sent }
                                .onFailure { errorServidor = Validators.mensajeError(it) }
                        }
                    }
                },
                enabled = !cargando
            )
            EnlaceAccion(stringResource(R.string.go_to_register), onRegistrar, enabled = !cargando)
        }
    }
}
