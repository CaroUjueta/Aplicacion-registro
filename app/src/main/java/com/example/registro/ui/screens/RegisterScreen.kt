package com.example.registro.ui.screens

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.registro.R
import com.example.registro.data.UsuarioRepository
import com.example.registro.ui.components.AppTextField
import com.example.registro.ui.components.BotonPrincipal
import com.example.registro.ui.components.EnlaceAccion
import com.example.registro.ui.components.ProfilePhotoPicker
import com.example.registro.utils.Validators

@Composable
fun RegisterScreen(
    onRegistrado: (String) -> Unit,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var nombre by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var telefono by rememberSaveable { mutableStateOf("") }
    var foto by rememberSaveable { mutableStateOf<Uri?>(null) }
    var intentado by rememberSaveable { mutableStateOf(false) }
    var cargando by remember { mutableStateOf(false) }
    var errorServidor by remember { mutableStateOf<Int?>(null) }

    val errorNombre = if (intentado && nombre.isBlank()) stringResource(R.string.error_name_required) else null
    val errorEmail = if (intentado && !Validators.emailValido(email)) stringResource(R.string.error_email_invalid) else null
    val errorPassword = if (intentado && !Validators.passwordValida(password)) stringResource(R.string.error_password_short) else null
    val errorTelefono = if (intentado && !Validators.telefonoValido(telefono)) stringResource(R.string.error_phone_invalid) else null

    Surface(modifier = modifier, color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(stringResource(R.string.register_title), style = MaterialTheme.typography.headlineLarge)
            ProfilePhotoPicker(foto = foto, onFotoElegida = { foto = it })
            Text(
                stringResource(R.string.photo_optional),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
            AppTextField(nombre, { nombre = it }, stringResource(R.string.field_name), error = errorNombre, enabled = !cargando)
            AppTextField(email, { email = it }, stringResource(R.string.field_email), error = errorEmail,
                keyboardType = KeyboardType.Email, enabled = !cargando)
            AppTextField(password, { password = it }, stringResource(R.string.field_password), error = errorPassword,
                isPassword = true, enabled = !cargando)
            AppTextField(telefono, { telefono = it }, stringResource(R.string.field_phone), error = errorTelefono,
                keyboardType = KeyboardType.Phone, enabled = !cargando)

            errorServidor?.let {
                Text(stringResource(it), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }

            BotonPrincipal(
                texto = stringResource(R.string.register_button),
                onClick = {
                    intentado = true
                    val valido = nombre.isNotBlank() && Validators.emailValido(email) &&
                        Validators.passwordValida(password) && Validators.telefonoValido(telefono)
                    if (valido) {
                        cargando = true
                        errorServidor = null
                        UsuarioRepository.registrar(context, nombre, email, password, telefono, foto) { resultado ->
                            cargando = false
                            resultado.onSuccess { onRegistrado(nombre.trim()) }
                                .onFailure { errorServidor = Validators.mensajeError(it) }
                        }
                    }
                },
                cargando = cargando,
            )
            EnlaceAccion(stringResource(R.string.go_to_login), onVolver, enabled = !cargando)
        }
    }
}
