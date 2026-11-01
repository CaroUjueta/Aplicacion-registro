package com.example.registro.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.registro.R
import com.example.registro.viewmodels.MapState
import com.mapbox.geojson.Point
import com.mapbox.maps.ViewAnnotationAnchor
import com.mapbox.maps.extension.compose.ComposeMapInitOptions
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.MapViewportState
import com.mapbox.maps.extension.compose.annotation.ViewAnnotation
import com.mapbox.maps.extension.compose.annotation.generated.PolylineAnnotationGroup
import com.mapbox.maps.extension.style.layers.properties.generated.LineCap
import com.mapbox.maps.extension.style.layers.properties.generated.LineJoin
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationOptions
import com.mapbox.maps.viewannotation.annotationAnchor
import com.mapbox.maps.viewannotation.geometry
import com.mapbox.maps.viewannotation.viewAnnotationOptions

@Composable
fun MapComponent(
    state: MapState,
    viewport: MapViewportState,
    margenInferior: Dp = 0.dp,
    modifier: Modifier = Modifier,
) {
    val miPosicion = state.miPosicion
    val colorMio = MaterialTheme.colorScheme.primary
    val miNombre = stringResource(R.string.you)

    Box(modifier = modifier) {
        MapboxMap(
            Modifier.fillMaxSize(),
            mapViewportState = viewport,
            // TextureView: evita que el SurfaceView se dibuje en negro en algunos celulares
            composeMapInitOptions = with(LocalDensity.current) {
                ComposeMapInitOptions(pixelRatio = density, textureView = true)
            },
            scaleBar = { },
            // Brújula abajo a la izquierda, por encima del panel (solo aparece cuando el mapa no mira al norte)
            compass = {
                Compass(
                    Modifier,
                    PaddingValues(start = 16.dp, bottom = margenInferior + 12.dp),
                    Alignment.BottomStart,
                    true,
                    true,
                )
            },
        ) {
            // La primera vez que llega la ubicación, la cámara vuela hasta ella
            MapEffect(miPosicion != null) {
                miPosicion?.let { viewport.setCameraOptions { center(it); zoom(16.0) } }
            }

            // Línea del recorrido propio (solo mientras se comparte la ubicación)
            if (state.miRuta.size > 1) {
                Recorrido(puntos = state.miRuta, color = colorMio)
            }
            // Línea del recorrido de cada usuario en línea
            state.otros.values.forEach { otro ->
                key("ruta-${otro.usuario.id}") {
                    if (otro.ruta.size > 1) Recorrido(puntos = otro.ruta, color = colorDeUsuario(otro.usuario.id))
                }
            }

            // Mi marcador
            miPosicion?.let { punto ->
                key("yo") {
                    ViewAnnotation(options = opcionesMarcador(punto)) {
                        UserMarker(
                            nombre = state.miPerfil?.nombre ?: miNombre,
                            fotoUrl = state.miPerfil?.fotoPerfilUrl,
                            color = colorMio
                        )
                    }
                }
            }
            // Marcadores de los demás usuarios en línea
            state.otros.values.forEach { otro ->
                key(otro.usuario.id) {
                    ViewAnnotation(options = opcionesMarcador(otro.posicion)) {
                        UserMarker(
                            nombre = otro.usuario.nombre,
                            fotoUrl = otro.usuario.fotoPerfilUrl,
                            color = colorDeUsuario(otro.usuario.id)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Recorrido(puntos: List<Point>, color: Color) {
    PolylineAnnotationGroup(annotations = listOf(PolylineAnnotationOptions().withPoints(puntos))) {
        lineCap = LineCap.ROUND
        lineJoin = LineJoin.ROUND
        lineWidth = 12.0
        lineColor = color
        lineBorderWidth = 3.0
        lineOpacity = 0.95
        lineBorderColor = Color.White
    }
}

private fun opcionesMarcador(punto: Point) = viewAnnotationOptions {
    geometry(punto)
    annotationAnchor { anchor(ViewAnnotationAnchor.BOTTOM) }
    allowOverlap(true)
}
