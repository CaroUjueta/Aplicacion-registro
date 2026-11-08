package com.example.registro.data

import android.content.Context
import android.net.Uri
import com.example.registro.utils.ImageUtils
import com.google.firebase.Firebase
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import com.google.firebase.storage.storage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

data class CambioUsuario(val usuario: Usuario, val salio: Boolean)

object UsuarioRepository {
    private val auth get() = Firebase.auth
    private val usuarios get() = Firebase.database.reference.child("usuarios")
    private val storage get() = Firebase.storage

    val uidActual: String? get() = auth.currentUser?.uid
    val emailActual: String? get() = auth.currentUser?.email
    val haySesion: Boolean get() = auth.currentUser != null

    fun iniciarSesion(email: String, password: String, onResultado: (Result<Unit>) -> Unit) {
        auth.signInWithEmailAndPassword(email.trim(), password).addOnCompleteListener { task ->
            onResultado(if (task.isSuccessful) Result.success(Unit) else Result.failure(task.exception ?: UnknownError()))
        }
    }

    fun recuperarContrasena(email: String, onResultado: (Result<Unit>) -> Unit) {
        auth.sendPasswordResetEmail(email.trim()).addOnCompleteListener { task ->
            onResultado(if (task.isSuccessful) Result.success(Unit) else Result.failure(task.exception ?: UnknownError()))
        }
    }

    fun registrar(
        context: Context,
        nombre: String,
        email: String,
        password: String,
        telefono: String,
        foto: Uri?,
        onResultado: (Result<Unit>) -> Unit,
    ) {
        auth.createUserWithEmailAndPassword(email.trim(), password).addOnCompleteListener { task ->
            val uid = if (task.isSuccessful) task.result?.user?.uid else null
            if (!task.isSuccessful || uid == null) {
                onResultado(Result.failure(task.exception ?: UnknownError()))
                return@addOnCompleteListener
            }
            fun guardar(url: String?) {
                val datos = mapOf(
                    "nombre" to nombre.trim(),
                    "email" to email.trim(),
                    "telefono" to telefono.trim(),
                    "enLinea" to false,
                    "latitud" to null,
                    "longitud" to null,
                    "fotoPerfilUrl" to url,
                )
                usuarios.child(uid).setValue(datos).addOnCompleteListener {
                    onResultado(if (it.isSuccessful) Result.success(Unit) else Result.failure(it.exception ?: UnknownError()))
                }
            }
            if (foto == null) guardar(null)
            else subirFoto(context, uid, foto) { guardar(it.getOrNull()) }
        }
    }

    fun cerrarSesion() = auth.signOut()

    fun cambiarContrasena(actual: String, nueva: String, onResultado: (Result<Unit>) -> Unit) {
        val user = auth.currentUser
        val email = user?.email
        if (user == null || email == null) {
            onResultado(Result.failure(IllegalStateException()))
            return
        }
        user.reauthenticate(EmailAuthProvider.getCredential(email, actual))
            .continueWithTask { tarea ->
                if (!tarea.isSuccessful) throw tarea.exception ?: UnknownError()
                user.updatePassword(nueva)
            }
            .addOnCompleteListener {
                onResultado(if (it.isSuccessful) Result.success(Unit) else Result.failure(it.exception ?: UnknownError()))
            }
    }

    fun perfil(uid: String): Flow<Usuario?> = callbackFlow {
        val ref = usuarios.child(uid)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(snapshot.aUsuario())
            }

            override fun onCancelled(error: DatabaseError) {}
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    fun actualizarPerfil(uid: String, nombre: String, telefono: String, onResultado: (Result<Unit>) -> Unit) {
        usuarios.child(uid).updateChildren(mapOf("nombre" to nombre.trim(), "telefono" to telefono.trim()))
            .addOnCompleteListener {
                onResultado(if (it.isSuccessful) Result.success(Unit) else Result.failure(it.exception ?: UnknownError()))
            }
    }

    fun cambiarFoto(context: Context, uid: String, foto: Uri, onResultado: (Result<Unit>) -> Unit) {
        subirFoto(context, uid, foto) { subida ->
            subida.onSuccess { url ->
                usuarios.child(uid).child("fotoPerfilUrl").setValue(url).addOnCompleteListener {
                    onResultado(if (it.isSuccessful) Result.success(Unit) else Result.failure(it.exception ?: UnknownError()))
                }
            }.onFailure { onResultado(Result.failure(it)) }
        }
    }

    private fun subirFoto(context: Context, uid: String, foto: Uri, onResultado: (Result<String>) -> Unit) {
        val bytes = ImageUtils.comprimir(context, foto)
        if (bytes == null) {
            onResultado(Result.failure(IllegalArgumentException()))
            return
        }
        val ref = storage.reference.child("registro/$uid/perfil.jpg")
        ref.putBytes(bytes)
            .continueWithTask { tarea ->
                if (!tarea.isSuccessful) throw tarea.exception ?: UnknownError()
                ref.downloadUrl
            }
            .addOnCompleteListener {
                onResultado(
                    if (it.isSuccessful) Result.success("${it.result}&v=${System.currentTimeMillis()}")
                    else Result.failure(it.exception ?: UnknownError())
                )
            }
    }

    fun marcarEnLinea(uid: String, latitud: Double?, longitud: Double?, onListo: () -> Unit = {}) {
        usuarios.child(uid).updateChildren(mapOf("enLinea" to true, "latitud" to latitud, "longitud" to longitud))
            .addOnCompleteListener { onListo() }
    }

    fun marcarDesconectado(uid: String, onListo: () -> Unit = {}) {
        usuarios.child(uid).updateChildren(mapOf("enLinea" to false, "latitud" to null, "longitud" to null))
            .addOnCompleteListener { onListo() }
    }

    fun actualizarPosicion(uid: String, latitud: Double, longitud: Double) {
        usuarios.child(uid).updateChildren(mapOf<String, Any>("latitud" to latitud, "longitud" to longitud))
    }

    fun cambiosEnLinea(): Flow<List<CambioUsuario>> = callbackFlow {
        val consulta = usuarios.orderByChild("enLinea").equalTo(true).limitToFirst(100)
        val listener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) = enviar(snapshot, false)
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) = enviar(snapshot, false)
            override fun onChildRemoved(snapshot: DataSnapshot) = enviar(snapshot, true)
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}

            private fun enviar(snapshot: DataSnapshot, salio: Boolean) {
                snapshot.aUsuario()?.let { trySend(listOf(CambioUsuario(it, salio))) }
            }
        }
        consulta.addChildEventListener(listener)
        awaitClose { consulta.removeEventListener(listener) }
    }

    private fun DataSnapshot.aUsuario(): Usuario? {
        if (!exists()) return null
        return Usuario(
            id = key.orEmpty(),
            nombre = child("nombre").getValue(String::class.java).orEmpty(),
            email = child("email").getValue(String::class.java).orEmpty(),
            telefono = child("telefono").getValue(String::class.java).orEmpty(),
            enLinea = child("enLinea").getValue(Boolean::class.java) ?: false,
            latitud = child("latitud").getValue(Double::class.java),
            longitud = child("longitud").getValue(Double::class.java),
            fotoPerfilUrl = child("fotoPerfilUrl").getValue(String::class.java),
        )
    }
}
