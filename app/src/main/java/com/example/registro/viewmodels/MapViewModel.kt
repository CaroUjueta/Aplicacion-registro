package com.example.registro.viewmodels

import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.registro.data.CambioUsuario
import com.example.registro.data.Usuario
import com.example.registro.data.UsuarioRepository
import com.example.registro.location.LocationTracker
import com.mapbox.geojson.Point
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Otro usuario en línea: sus datos, su posición actual y la ruta que ha recorrido mientras lo vemos
data class OtroUsuario(val usuario: Usuario, val posicion: Point, val ruta: List<Point>)

data class MapState(
    val miPerfil: Usuario? = null,
    val rastreando: Boolean = false,
    val miPosicion: Point? = null,
    val miRuta: List<Point> = emptyList(),
    val otros: Map<String, OtroUsuario> = emptyMap(),
)

class MapViewModel(application: Application) : AndroidViewModel(application) {
    private val uid = UsuarioRepository.uidActual.orEmpty()
    private val tracker = LocationTracker(application)

    private val _state = MutableStateFlow(MapState())
    val state: StateFlow<MapState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            UsuarioRepository.perfil(uid).collect { perfil -> _state.update { it.copy(miPerfil = perfil) } }
        }
        // addSnapshotListener sobre los usuarios en línea: cada movimiento llega como un cambio
        viewModelScope.launch {
            UsuarioRepository.cambiosEnLinea().collect { cambios -> cambios.forEach(::aplicarCambio) }
        }
    }

    private fun aplicarCambio(cambio: CambioUsuario) {
        val usuario = cambio.usuario
        if (usuario.id == uid) return
        _state.update { estado ->
            val otros = estado.otros.toMutableMap()
            val lat = usuario.latitud
            val lng = usuario.longitud
            if (cambio.salio || !usuario.enLinea) {
                // Se desconectó: se limpian su marcador y su línea
                otros.remove(usuario.id)
            } else if (lat != null && lng != null) {
                val punto = Point.fromLngLat(lng, lat)
                val anterior = otros[usuario.id]
                val ruta = when {
                    anterior == null -> listOf(punto)
                    anterior.posicion == punto -> anterior.ruta
                    else -> anterior.ruta + punto
                }
                otros[usuario.id] = OtroUsuario(usuario, punto, ruta)
            }
            estado.copy(otros = otros)
        }
    }

    // Centra el mapa en el usuario al abrirlo (sin escribir nada en la base de datos)
    fun ubicarUnaVez() {
        tracker.ubicacionUnica { loc -> if (_state.value.miPosicion == null) actualizarMiPosicion(loc, guardar = false) }
    }

    fun iniciarRastreo() {
        if (_state.value.rastreando) return
        _state.update { it.copy(rastreando = true, miRuta = emptyList()) }
        UsuarioRepository.marcarEnLinea(uid, _state.value.miPosicion?.latitude(), _state.value.miPosicion?.longitude())
        tracker.iniciar { loc -> actualizarMiPosicion(loc, guardar = true) }
    }

    fun detenerRastreo(onListo: () -> Unit = {}) {
        tracker.detener()
        val estabaRastreando = _state.value.rastreando
        // Se borra la línea recorrida y se marca desconectado
        _state.update { it.copy(rastreando = false, miRuta = emptyList()) }
        if (estabaRastreando) UsuarioRepository.marcarDesconectado(uid, onListo) else onListo()
    }

    private fun actualizarMiPosicion(loc: Location, guardar: Boolean) {
        val punto = Point.fromLngLat(loc.longitude, loc.latitude)
        _state.update { it.copy(miPosicion = punto, miRuta = if (it.rastreando) it.miRuta + punto else it.miRuta) }
        if (guardar) UsuarioRepository.actualizarPosicion(uid, loc.latitude, loc.longitude)
    }

    // Primero se marca desconectado (todavía hay sesión para escribir) y luego se cierra la sesión
    fun cerrarSesion(onListo: () -> Unit) {
        detenerRastreo {
            UsuarioRepository.cerrarSesion()
            onListo()
        }
    }

    override fun onCleared() {
        // Si la actividad se cierra con el rastreo encendido, no se deja al usuario "en línea"
        if (_state.value.rastreando) {
            tracker.detener()
            UsuarioRepository.marcarDesconectado(uid)
        }
    }
}
