package com.example.registro.viewmodels

import android.content.Context
import android.net.Uri
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.core.SurfaceRequest
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.compose.ui.geometry.Offset
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import com.example.registro.utils.ImageUtils
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class CameraState(
    val surfaceRequest: SurfaceRequest? = null,
    val useBackLens: Boolean = false,
)

class CameraViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(CameraState())
    val uiState: StateFlow<CameraState> = _uiState.asStateFlow()

    private var surfaceMeteringPointFactory: SurfaceOrientedMeteringPointFactory? = null
    private var cameraControl: CameraControl? = null
    private var lifecycleOwner: LifecycleOwner? = null
    private var processCameraProvider: ProcessCameraProvider? = null
    private var selector = CameraSelector.DEFAULT_FRONT_CAMERA
    private val imageCapture = ImageCapture.Builder()
        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
        .build()

    private val cameraPreviewUseCase = Preview.Builder().build().apply {
        setSurfaceProvider { request ->
            surfaceMeteringPointFactory = SurfaceOrientedMeteringPointFactory(
                request.resolution.width.toFloat(),
                request.resolution.height.toFloat()
            )
            _uiState.update { it.copy(surfaceRequest = request) }
        }
    }

    suspend fun bindToCamera(appContext: Context, lifecycleOwner: LifecycleOwner) {
        this.lifecycleOwner = lifecycleOwner
        processCameraProvider = ProcessCameraProvider.awaitInstance(appContext)
        val camera = processCameraProvider?.bindToLifecycle(lifecycleOwner, selector, cameraPreviewUseCase, imageCapture)
        cameraControl = camera?.cameraControl
        try {
            awaitCancellation()
        } finally {
            cameraControl = null
            surfaceMeteringPointFactory = null
            processCameraProvider?.unbindAll()
        }
    }

    fun tapToFocus(coords: Offset) {
        val punto = surfaceMeteringPointFactory?.createPoint(coords.x, coords.y) ?: return
        cameraControl?.startFocusAndMetering(FocusMeteringAction.Builder(punto).build())
    }

    fun toggleCameraLens() {
        val atras = !_uiState.value.useBackLens
        _uiState.update { it.copy(useBackLens = atras) }
        selector = if (atras) CameraSelector.DEFAULT_BACK_CAMERA else CameraSelector.DEFAULT_FRONT_CAMERA
        val owner = lifecycleOwner
        val provider = processCameraProvider
        if (owner != null && provider != null) {
            provider.unbindAll()
            cameraControl = provider.bindToLifecycle(owner, selector, cameraPreviewUseCase, imageCapture).cameraControl
        }
    }

    fun takePhoto(context: Context, onResultado: (Uri?) -> Unit) {
        val archivo = ImageUtils.crearArchivoTemporal(context)
        imageCapture.takePicture(
            ImageCapture.OutputFileOptions.Builder(archivo).build(),
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) = onResultado(Uri.fromFile(archivo))
                override fun onError(exception: ImageCaptureException) = onResultado(null)
            }
        )
    }
}
