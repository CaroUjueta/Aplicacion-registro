package com.example.registro.ui.components

import android.net.Uri
import android.widget.Toast
import androidx.camera.compose.CameraXViewfinder
import androidx.camera.viewfinder.compose.MutableCoordinateTransformer
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FlipCameraAndroid
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.registro.R
import com.example.registro.viewmodels.CameraViewModel

@Composable
fun CameraPreviewContent(
    onFotoTomada: (Uri) -> Unit,
    onCerrar: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CameraViewModel = viewModel(),
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(lifecycleOwner) {
        viewModel.bindToCamera(context.applicationContext, lifecycleOwner)
    }

    Box(modifier = modifier.background(Color.Black)) {
        uiState.surfaceRequest?.let { request ->
            val transformer = remember { MutableCoordinateTransformer() }
            CameraXViewfinder(
                surfaceRequest = request,
                coordinateTransformer = transformer,
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures { toque ->
                            with(transformer) { viewModel.tapToFocus(toque.transform()) }
                        }
                    }
            )
        }

        BotonIconoCircular(
            icono = Icons.Rounded.Close,
            descripcion = stringResource(R.string.camera_close),
            onClick = onCerrar,
            modifier = Modifier.align(Alignment.TopStart).statusBarsPadding().padding(16.dp),
        )
        BotonIconoCircular(
            icono = Icons.Rounded.FlipCameraAndroid,
            descripcion = stringResource(R.string.camera_flip),
            onClick = viewModel::toggleCameraLens,
            modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(16.dp),
        )
        FilledIconButton(
            onClick = {
                viewModel.takePhoto(context) { uri ->
                    if (uri != null) onFotoTomada(uri)
                    else Toast.makeText(context, R.string.camera_error, Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(24.dp).size(72.dp)
        ) {
            Icon(Icons.Rounded.PhotoCamera, stringResource(R.string.camera_capture), modifier = Modifier.size(36.dp))
        }
    }
}
