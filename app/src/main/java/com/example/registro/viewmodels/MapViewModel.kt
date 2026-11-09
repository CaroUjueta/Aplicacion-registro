package com.example.registro.viewmodels

import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import com.example.registro.data.Usuario
import com.example.registro.location.LocationTracker
import com.example.registro.repository.RealtimeUserRepository
import com.example.registro.repository.Subscription
import com.example.registro.repository.UserRepository
import com.mapbox.geojson.Point
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class OtroUsuario(val usuario: Usuario, val posicion: Point, val ruta: List<Point>)

data class MapState(
    val miPerfil: Usuario? = null,
    val rastreando: Boolean = false,
    val miPosicion: Point? = null,
    val miRuta: List<Point> = emptyList(),
    val otros: Map<String, OtroUsuario> = emptyMap(),
)

class MapViewModel(application: Application) : AndroidViewModel(application) {
    private val users: UserRepository = RealtimeUserRepository()
    private val tracker = LocationTracker(application)

    private val _state = MutableStateFlow(MapState())
    val state: StateFlow<MapState> = _state.asStateFlow()

    private var uid: String? = null
    private var suscripciones = emptyList<Subscription>()

    fun observar(uid: String) {
        if (this.uid == uid) return
        dejarDeObservar()
        this.uid = uid
        suscripciones = listOf(
            users.observar(uid) { perfil -> _state.update { it.copy(miPerfil = perfil) } },
            users.observarEnLinea { usuario, salio -> aplicarCambio(uid, usuario, salio) },
        )
    }

    fun dejarDeObservar() {
        suscripciones.forEach { it.cancel() }
        suscripciones = emptyList()
        uid = null
        _state.value = MapState()
    }

    private fun aplicarCambio(miUid: String, usuario: Usuario, salio: Boolean) {
        if (usuario.id == miUid) return
        _state.update { estado ->
            val otros = estado.otros.toMutableMap()
            val lat = usuario.latitud
            val lng = usuario.longitud
            if (salio || !usuario.enLinea) {
                otros.remove(usuario.id)
            } else if (lat != null && lng != null) {
                val punto = Point.fromLngLat(lng, lat)
                val ruta = otros[usuario.id]?.ruta.orEmpty()
                // solo se agrega a la ruta si el usuario realmente se movió
                otros[usuario.id] = OtroUsuario(usuario, punto, if (ruta.lastOrNull() == punto) ruta else ruta + punto)
            }
            estado.copy(otros = otros)
        }
    }

    fun ubicarUnaVez() {
        tracker.ubicacionUnica { loc -> if (_state.value.miPosicion == null) actualizarMiPosicion(loc, guardar = false) }
    }

    fun iniciarRastreo() {
        val miUid = uid ?: return
        if (_state.value.rastreando) return
        _state.update { it.copy(rastreando = true, miRuta = emptyList()) }
        val pos = _state.value.miPosicion
        users.actualizar(miUid, mapOf("enLinea" to true, "latitud" to pos?.latitude(), "longitud" to pos?.longitude()))
        tracker.iniciar { loc -> actualizarMiPosicion(loc, guardar = true) }
    }

    fun detenerRastreo(onListo: () -> Unit = {}) {
        tracker.detener()
        val estabaRastreando = _state.value.rastreando
        _state.update { it.copy(rastreando = false, miRuta = emptyList()) }
        val miUid = uid
        if (estabaRastreando && miUid != null) marcarDesconectado(miUid, onListo) else onListo()
    }

    private fun marcarDesconectado(miUid: String, onListo: () -> Unit = {}) {
        users.actualizar(miUid, mapOf("enLinea" to false, "latitud" to null, "longitud" to null)) { onListo() }
    }

    private fun actualizarMiPosicion(loc: Location, guardar: Boolean) {
        val punto = Point.fromLngLat(loc.longitude, loc.latitude)
        _state.update { it.copy(miPosicion = punto, miRuta = if (it.rastreando) it.miRuta + punto else it.miRuta) }
        val miUid = uid
        if (guardar && miUid != null) {
            users.actualizar(miUid, mapOf("latitud" to loc.latitude, "longitud" to loc.longitude))
        }
    }

    override fun onCleared() {
        val miUid = uid
        if (_state.value.rastreando && miUid != null) {
            tracker.detener()
            marcarDesconectado(miUid)
        }
        suscripciones.forEach { it.cancel() }
    }
}
