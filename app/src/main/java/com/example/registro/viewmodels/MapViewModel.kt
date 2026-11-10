package com.example.registro.viewmodels

import androidx.lifecycle.ViewModel
import com.example.registro.data.Usuario
import com.example.registro.repository.FirestoreUserRepository
import com.example.registro.repository.Subscription
import com.example.registro.repository.UserRepository
import com.example.registro.utils.UiText
import com.example.registro.utils.Validators
import com.mapbox.geojson.Point
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update

data class OtroUsuario(val usuario: Usuario, val posicion: Point, val ruta: List<Point>)

data class MapState(
    val miPerfil: Usuario? = null,
    val rastreando: Boolean = false,
    val otros: Map<String, OtroUsuario> = emptyMap(),
)

class MapViewModel(
    private val users: UserRepository = FirestoreUserRepository(),
) : ViewModel() {
    private val _state = MutableStateFlow(MapState())
    val state: StateFlow<MapState> = _state.asStateFlow()

    private val _mensajes = Channel<UiText>(Channel.BUFFERED)
    val mensajes: Flow<UiText> = _mensajes.receiveAsFlow()

    private var uid: String? = null
    private var suscripciones = emptyList<Subscription>()

    fun observar(uid: String) {
        if (this.uid == uid) return
        dejarDeObservar()
        this.uid = uid
        suscripciones = listOf(
            users.observar(uid) { resultado ->
                resultado.onSuccess { perfil -> _state.update { it.copy(miPerfil = perfil) } }
                    .onFailure { avisar(it) }
            },
            users.observarEnLinea { resultado ->
                resultado.onSuccess { aplicarCambio(uid, it.usuario, it.salio) }
                    .onFailure { avisar(it) }
            },
        )
    }

    private fun avisar(e: Throwable) {
        _mensajes.trySend(Validators.mensajeError(e))
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
                otros[usuario.id] = OtroUsuario(usuario, punto, if (ruta.lastOrNull() == punto) ruta else ruta + punto)
            }
            estado.copy(otros = otros)
        }
    }

    fun conectar(latitud: Double?, longitud: Double?, onFallo: () -> Unit = {}) {
        val miUid = uid ?: return
        if (_state.value.rastreando) return
        _state.update { it.copy(rastreando = true) }
        users.actualizar(miUid, mapOf("enLinea" to true, "latitud" to latitud, "longitud" to longitud)) { resultado ->
            resultado.onFailure {
                _state.update { estado -> estado.copy(rastreando = false) }
                avisar(it)
                onFallo()
            }
        }
    }

    fun registrarPosicion(latitud: Double, longitud: Double) {
        val miUid = uid ?: return
        if (!_state.value.rastreando) return
        users.actualizar(miUid, mapOf("latitud" to latitud, "longitud" to longitud))
    }

    fun desconectar(onListo: () -> Unit = {}) {
        val estabaRastreando = _state.value.rastreando
        _state.update { it.copy(rastreando = false) }
        val miUid = uid
        if (estabaRastreando && miUid != null) marcarDesconectado(miUid, onListo) else onListo()
    }

    private fun marcarDesconectado(miUid: String, onListo: () -> Unit = {}) {
        users.actualizar(miUid, mapOf("enLinea" to false, "latitud" to null, "longitud" to null)) { onListo() }
    }

    override fun onCleared() {
        val miUid = uid
        if (_state.value.rastreando && miUid != null) marcarDesconectado(miUid)
        suscripciones.forEach { it.cancel() }
    }
}
