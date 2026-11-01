package com.example.registro.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.registro.R
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material3.Surface
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import coil3.compose.AsyncImage
import com.example.registro.ui.components.BotonIconoCircular
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.example.registro.ui.components.MapComponent
import com.example.registro.viewmodels.MapViewModel

private const val MAP_ZOOM = 16.0

@Composable
fun MapScreen(
    viewModel: MapViewModel,
    nombreBienvenida: String?,
    onPerfil: () -> Unit,
    onCerrarSesion: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    var menuAbierto by remember { mutableStateOf(false) }
    var bienvenidaMostrada by rememberSaveable { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }

    fun tienePermiso() = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    // Permiso de ubicación: si se concede, se enciende el rastreo que el usuario pidió con el switch
    val permisos = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { resultado ->
        if (resultado.values.any { it }) {
            viewModel.ubicarUnaVez()
            viewModel.iniciarRastreo()
        } else {
            Toast.makeText(context, R.string.location_permission_denied, Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(Unit) {
        if (tienePermiso()) viewModel.ubicarUnaVez()
        if (!bienvenidaMostrada && nombreBienvenida != null) {
            bienvenidaMostrada = true
            snackbar.showSnackbar(context.getString(R.string.welcome_user, nombreBienvenida))
        }
    }

    val viewport = rememberMapViewportState {
        setCameraOptions {
            zoom(MAP_ZOOM)
            center(Point.fromLngLat(-74.0641630119028, 4.627389316647156))
            pitch(0.0)
        }
    }

    val densidad = LocalDensity.current
    var alturaPanel by remember { mutableStateOf(0.dp) }

    Box(modifier.fillMaxSize()) {
        // Mapa a pantalla completa (estilo Life360)
        MapComponent(state = state, viewport = viewport, margenInferior = alturaPanel, modifier = Modifier.fillMaxSize())

        // Barra flotante superior: pastilla con el título y el menú
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surface, shadowElevation = 4.dp) {
                Row(
                    modifier = Modifier.padding(start = 14.dp, end = 16.dp, top = 10.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Rounded.Place, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Text(stringResource(R.string.map_title), style = MaterialTheme.typography.titleSmall)
                }
            }
            Box {
                BotonIconoCircular(
                    icono = Icons.Rounded.MoreVert,
                    descripcion = stringResource(R.string.menu),
                    onClick = { menuAbierto = true },
                )
                DropdownMenu(expanded = menuAbierto, onDismissRequest = { menuAbierto = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_profile)) },
                        leadingIcon = { Icon(Icons.Rounded.AccountCircle, contentDescription = null) },
                        onClick = { menuAbierto = false; onPerfil() }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_logout)) },
                        leadingIcon = { Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null) },
                        onClick = { menuAbierto = false; onCerrarSesion() }
                    )
                }
            }
        }

        // Abajo: botón de centrar justo encima del panel de estado
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.End) {
                BotonIconoCircular(
                    icono = Icons.Rounded.MyLocation,
                    descripcion = stringResource(R.string.center_location),
                    onClick = {
                        state.miPosicion?.let { viewport.flyTo(CameraOptions.Builder().center(it).zoom(MAP_ZOOM).build()) }
                    },
                )
            }
            PanelUbicacion(
                modifier = Modifier.onSizeChanged { alturaPanel = with(densidad) { it.height.toDp() } },
                nombre = state.miPerfil?.nombre ?: stringResource(R.string.you),
                fotoUrl = state.miPerfil?.fotoPerfilUrl,
                rastreando = state.rastreando,
                enLinea = state.otros.size + if (state.rastreando) 1 else 0,
                onCambio = { encender ->
                    when {
                        !encender -> viewModel.detenerRastreo()
                        tienePermiso() -> viewModel.iniciarRastreo()
                        else -> permisos.launch(
                            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                        )
                    }
                }
            )
        }

        SnackbarHost(snackbar, Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 72.dp))
    }
}

// Panel inferior redondeado: tu foto, tu estado y el switch de ubicación
@Composable
private fun PanelUbicacion(
    modifier: Modifier = Modifier,
    nombre: String,
    fotoUrl: String?,
    rastreando: Boolean,
    enLinea: Int,
    onCambio: (Boolean) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 12.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            Modifier.navigationBarsPadding().padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    border = BorderStroke(3.dp, if (rastreando) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.size(52.dp)
                ) {
                    if (fotoUrl != null) {
                        AsyncImage(fotoUrl, nombre, contentScale = ContentScale.Crop, modifier = Modifier.size(52.dp))
                    } else {
                        Box(contentAlignment = Alignment.Center) {
                            Text(nombre.firstOrNull()?.uppercase() ?: "?", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
                Column(Modifier.weight(1f)) {
                    Text(nombre, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        stringResource(if (rastreando) R.string.location_on else R.string.location_off),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = rastreando, onCheckedChange = onCambio)
            }
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                Text(
                    stringResource(R.string.users_online, enLinea),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }
        }
    }
}
