package com.example.registro.ui.components

import android.Manifest
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.registro.R

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ProfilePhotoPicker(
    foto: Any?,
    onFotoElegida: (Uri) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var camaraAbierta by rememberSaveable { mutableStateOf(false) }

    val permisoCamara = rememberPermissionState(Manifest.permission.CAMERA) { concedido ->
        if (concedido) camaraAbierta = true
        else Toast.makeText(context, R.string.camera_permission_denied, Toast.LENGTH_SHORT).show()
    }
    val galeria = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let(onFotoElegida)
    }

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            border = BorderStroke(3.dp, MaterialTheme.colorScheme.primary),
            modifier = Modifier.size(120.dp)
        ) {
            if (foto != null) {
                AsyncImage(
                    model = foto,
                    contentDescription = stringResource(R.string.profile_photo),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(120.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.Person,
                    contentDescription = stringResource(R.string.profile_photo),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(120.dp).padding(24.dp)
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 12.dp)) {
            BotonSecundario(texto = stringResource(R.string.take_photo), icono = Icons.Rounded.CameraAlt, fillWidth = false, onClick = {
                if (permisoCamara.status.isGranted) camaraAbierta = true
                else permisoCamara.launchPermissionRequest()
            })
            BotonSecundario(texto = stringResource(R.string.pick_photo), icono = Icons.Rounded.PhotoLibrary, fillWidth = false, onClick = {
                galeria.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            })
        }
    }
    if (camaraAbierta) {
        Dialog(
            onDismissRequest = { camaraAbierta = false },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
        ) {
            CameraPreviewContent(
                onFotoTomada = { uri -> camaraAbierta = false; onFotoElegida(uri) },
                onCerrar = { camaraAbierta = false },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
