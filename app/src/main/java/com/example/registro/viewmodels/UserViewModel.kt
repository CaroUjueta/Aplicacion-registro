package com.example.registro.viewmodels

import android.net.Uri
import androidx.lifecycle.ViewModel
import com.example.registro.R
import com.example.registro.data.Usuario
import com.example.registro.repository.FirebaseStorageRepository
import com.example.registro.repository.FirestoreUserRepository
import com.example.registro.repository.StorageRepository
import com.example.registro.repository.Subscription
import com.example.registro.repository.UserRepository
import com.example.registro.utils.UiText
import com.example.registro.utils.Validators
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow

class UserViewModel(
    private val users: UserRepository = FirestoreUserRepository(),
    private val storage: StorageRepository = FirebaseStorageRepository(),
) : ViewModel() {
    private val _perfil = MutableStateFlow<Usuario?>(null)
    val perfil: StateFlow<Usuario?> = _perfil.asStateFlow()

    private val _mensajes = Channel<UiText>(Channel.BUFFERED)
    val mensajes: Flow<UiText> = _mensajes.receiveAsFlow()

    private var suscripcion: Subscription? = null

    fun observar(uid: String) {
        suscripcion?.cancel()
        suscripcion = users.observar(uid) { resultado ->
            resultado.onSuccess { _perfil.value = it }
                .onFailure { avisar(Validators.mensajeError(it)) }
        }
    }

    fun dejarDeObservar() {
        suscripcion?.cancel()
        suscripcion = null
        _perfil.value = null
    }

    fun guardarCambios(uid: String, nombre: String, telefono: String) {
        users.actualizar(uid, mapOf("nombre" to nombre.trim(), "telefono" to telefono.trim())) { resultado ->
            avisar(
                if (resultado.isSuccess) UiText.Resource(R.string.profile_saved)
                else Validators.mensajeError(resultado.exceptionOrNull())
            )
        }
    }

    fun cambiarFoto(uid: String, foto: Uri) {
        storage.subirFotoPerfil(uid, foto) { subida ->
            subida.onFailure { avisar(Validators.mensajeError(it)) }
            subida.onSuccess { url ->
                users.actualizar(uid, mapOf("fotoPerfilUrl" to url)) { resultado ->
                    avisar(
                        if (resultado.isSuccess) UiText.Resource(R.string.photo_uploaded)
                        else Validators.mensajeError(resultado.exceptionOrNull())
                    )
                }
            }
        }
    }

    private fun avisar(mensaje: UiText) {
        _mensajes.trySend(mensaje)
    }

    override fun onCleared() {
        suscripcion?.cancel()
    }
}
