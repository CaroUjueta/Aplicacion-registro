package com.example.registro.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.registro.R
import com.example.registro.data.UsuarioRepository
import com.example.registro.ui.components.AppTextField
import com.example.registro.ui.components.BotonPrincipal
import com.example.registro.ui.components.BotonSecundario
import com.example.registro.ui.components.ProfilePhotoPicker
import com.example.registro.utils.Validators
import androidx.compose.ui.Alignment

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    uid: String,
    emailInicial: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val perfil by remember(uid) { UsuarioRepository.perfil(uid) }.collectAsState(initial = null)

    var nombre by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }
    var cargado by remember { mutableStateOf(false) }
    var fotoNueva by remember { mutableStateOf<Uri?>(null) }
    var intentado by remember { mutableStateOf(false) }

    var actual by remember { mutableStateOf("") }
    var nueva by remember { mutableStateOf("") }
    var intentadoClave by remember { mutableStateOf(false) }

    LaunchedEffect(perfil) {
        perfil?.let {
            if (!cargado) {
                nombre = it.nombre
                telefono = it.telefono
                cargado = true
            }
        }
    }

    fun aviso(@androidx.annotation.StringRes mensaje: Int) = Toast.makeText(context, mensaje, Toast.LENGTH_SHORT).show()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.profile_title)) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ProfilePhotoPicker(
                foto = fotoNueva ?: perfil?.fotoPerfilUrl,
                onFotoElegida = { uri ->
                    fotoNueva = uri
                    UsuarioRepository.cambiarFoto(context, uid, uri) { resultado ->
                        aviso(if (resultado.isSuccess) R.string.photo_uploaded else R.string.error_photo_upload)
                    }
                }
            )

            AppTextField(perfil?.email ?: emailInicial, {}, stringResource(R.string.field_email), enabled = false)
            AppTextField(
                nombre, { nombre = it }, stringResource(R.string.field_name),
                error = if (intentado && nombre.isBlank()) stringResource(R.string.error_name_required) else null
            )
            AppTextField(
                telefono, { telefono = it }, stringResource(R.string.field_phone), keyboardType = KeyboardType.Phone,
                error = if (intentado && !Validators.telefonoValido(telefono)) stringResource(R.string.error_phone_invalid) else null
            )
            BotonPrincipal(
                texto = stringResource(R.string.save_changes),
                onClick = {
                    intentado = true
                    if (nombre.isNotBlank() && Validators.telefonoValido(telefono)) {
                        UsuarioRepository.actualizarPerfil(uid, nombre, telefono) { resultado ->
                            aviso(if (resultado.isSuccess) R.string.profile_saved else Validators.mensajeError(resultado.exceptionOrNull()))
                        }
                    }
                },
            )

            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            Text(stringResource(R.string.password_section), style = MaterialTheme.typography.titleMedium)
            AppTextField(actual, { actual = it }, stringResource(R.string.field_current_password), isPassword = true)
            AppTextField(
                nueva, { nueva = it }, stringResource(R.string.field_new_password), isPassword = true,
                error = if (intentadoClave && !Validators.passwordValida(nueva)) stringResource(R.string.error_password_short) else null
            )
            BotonSecundario(
                texto = stringResource(R.string.change_password),
                onClick = {
                    intentadoClave = true
                    if (actual.isNotEmpty() && Validators.passwordValida(nueva)) {
                        UsuarioRepository.cambiarContrasena(actual, nueva) { resultado ->
                            if (resultado.isSuccess) {
                                actual = ""
                                nueva = ""
                                intentadoClave = false
                            }
                            aviso(if (resultado.isSuccess) R.string.password_changed else Validators.mensajeError(resultado.exceptionOrNull()))
                        }
                    }
                },
            )
        }
    }
}
