package com.example.registro.viewmodels

import android.content.Context
import android.net.Uri
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import com.example.registro.R
import com.example.registro.data.Usuario
import com.example.registro.repository.FirebaseStorageRepository
import com.example.registro.repository.RealtimeUserRepository
import com.example.registro.repository.StorageRepository
import com.example.registro.repository.Subscription
import com.example.registro.repository.UserRepository
import com.example.registro.utils.ImageUtils
import com.example.registro.utils.Validators
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow

class UserViewModel(
    private val users: UserRepository = RealtimeUserRepository(),
    private val storage: StorageRepository = FirebaseStorageRepository(),
) : ViewModel() {
    private val _perfil = MutableStateFlow<Usuario?>(null)
    val perfil: StateFlow<Usuario?> = _perfil.asStateFlow()

    private val _mensajes = Channel<Int>(Channel.BUFFERED)
    val mensajes: Flow<Int> = _mensajes.receiveAsFlow()

    private var suscripcion: Subscription? = null

    fun observar(uid: String) {
        suscripcion?.cancel()
        suscripcion = users.observar(uid) { _perfil.value = it }
    }

    fun dejarDeObservar() {
        suscripcion?.cancel()
        suscripcion = null
        _perfil.value = null
    }

    fun guardarCambios(uid: String, nombre: String, telefono: String) {
        users.actualizar(uid, mapOf("nombre" to nombre.trim(), "telefono" to telefono.trim())) { resultado ->
            avisar(if (resultado.isSuccess) R.string.profile_saved else Validators.mensajeError(resultado.exceptionOrNull()))
        }
    }

    fun cambiarFoto(context: Context, uid: String, foto: Uri) {
        val bytes = ImageUtils.comprimir(context, foto)
        if (bytes == null) {
            avisar(R.string.error_photo_upload)
            return
        }
        storage.subirFotoPerfil(uid, bytes) { subida ->
            val url = subida.getOrNull()
            if (url == null) {
                avisar(R.string.error_photo_upload)
            } else {
                users.actualizar(uid, mapOf("fotoPerfilUrl" to url)) { avisar(R.string.photo_uploaded) }
            }
        }
    }

    private fun avisar(@StringRes mensaje: Int) {
        _mensajes.trySend(mensaje)
    }

    override fun onCleared() {
        suscripcion?.cancel()
    }
}
