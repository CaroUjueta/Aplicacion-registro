package com.example.registro.viewmodels

import android.content.Context
import android.net.Uri
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import com.example.registro.R
import com.example.registro.data.Usuario
import com.example.registro.repository.FirebaseStorageRepository
import com.example.registro.repository.FirestoreUserRepository
import com.example.registro.repository.StorageRepository
import com.example.registro.repository.UserRepository
import com.example.registro.utils.ImageUtils
import com.example.registro.utils.Validators
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow

sealed interface AuthState {
    data object Idle : AuthState
    data object Loading : AuthState
    data class Error(@StringRes val mensaje: Int) : AuthState
}

sealed interface AuthEvent {
    data object IrAlMapa : AuthEvent
    data object IrALogin : AuthEvent
    data object ContrasenaCambiada : AuthEvent
    data class Mensaje(@StringRes val texto: Int) : AuthEvent
}

class AuthViewModel(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val users: UserRepository = FirestoreUserRepository(),
    private val storage: StorageRepository = FirebaseStorageRepository(),
) : ViewModel() {
    private val _state = MutableStateFlow<AuthState>(AuthState.Idle)
    val state: StateFlow<AuthState> = _state.asStateFlow()

    private val _currentUser = MutableStateFlow(auth.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val authListener = FirebaseAuth.AuthStateListener { _currentUser.value = it.currentUser }

    private val _events = Channel<AuthEvent>(Channel.BUFFERED)
    val events: Flow<AuthEvent> = _events.receiveAsFlow()

    init {
        auth.addAuthStateListener(authListener)
    }

    override fun onCleared() {
        auth.removeAuthStateListener(authListener)
    }

    fun login(email: String, password: String) {
        _state.value = AuthState.Loading
        auth.signInWithEmailAndPassword(email.trim(), password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                _state.value = AuthState.Idle
                _events.trySend(AuthEvent.IrAlMapa)
            } else {
                _state.value = AuthState.Error(Validators.mensajeError(task.exception))
            }
        }
    }

    fun register(
        context: Context,
        nombre: String,
        email: String,
        password: String,
        telefono: String,
        foto: Uri?,
    ) {
        _state.value = AuthState.Loading
        auth.createUserWithEmailAndPassword(email.trim(), password).addOnCompleteListener { task ->
            val uid = task.result?.user?.uid
            if (!task.isSuccessful || uid == null) {
                _state.value = AuthState.Error(Validators.mensajeError(task.exception))
                return@addOnCompleteListener
            }
            val usuario = Usuario(nombre = nombre.trim(), email = email.trim(), telefono = telefono.trim())
            val bytes = foto?.let { ImageUtils.comprimir(context, it) }
            if (bytes == null) {
                guardarPerfil(uid, usuario)
            } else {
                storage.subirFotoPerfil(uid, bytes) { subida ->
                    guardarPerfil(uid, usuario.copy(fotoPerfilUrl = subida.getOrNull()))
                }
            }
        }
    }

    private fun guardarPerfil(uid: String, usuario: Usuario) {
        users.guardar(uid, usuario) { resultado ->
            if (resultado.isSuccess) {
                _state.value = AuthState.Idle
                _events.trySend(AuthEvent.IrAlMapa)
            } else {
                _state.value = AuthState.Error(Validators.mensajeError(resultado.exceptionOrNull()))
            }
        }
    }

    fun olvideContrasena(email: String) {
        _state.value = AuthState.Loading
        auth.sendPasswordResetEmail(email.trim()).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                _state.value = AuthState.Idle
                _events.trySend(AuthEvent.Mensaje(R.string.reset_email_sent))
            } else {
                _state.value = AuthState.Error(Validators.mensajeError(task.exception))
            }
        }
    }

    fun cambiarContrasena(actual: String, nueva: String) {
        val user = auth.currentUser
        val email = user?.email
        if (user == null || email == null) return
        user.reauthenticate(EmailAuthProvider.getCredential(email, actual))
            .continueWithTask { reauth ->
                if (reauth.isSuccessful) user.updatePassword(nueva) else Tasks.forException(reauth.exception ?: IllegalStateException())
            }
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _events.trySend(AuthEvent.ContrasenaCambiada)
                    _events.trySend(AuthEvent.Mensaje(R.string.password_changed))
                } else {
                    _events.trySend(AuthEvent.Mensaje(Validators.mensajeError(task.exception)))
                }
            }
    }

    fun mostrarError(@StringRes mensaje: Int) {
        _state.value = AuthState.Error(mensaje)
    }

    fun limpiarEstado() {
        _state.value = AuthState.Idle
    }

    fun logout() {
        auth.signOut()
        _state.value = AuthState.Idle
        _events.trySend(AuthEvent.IrALogin)
    }
}
