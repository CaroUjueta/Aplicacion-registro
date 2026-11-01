package com.example.registro.data

import android.content.Context
import android.net.Uri
import com.example.registro.utils.ImageUtils
import com.google.firebase.Firebase
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.storage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

// Cambio en la lista de usuarios en línea: el usuario entró/se movió, o dejó de estar en línea
data class CambioUsuario(val usuario: Usuario, val salio: Boolean)

/**
 * Firebase Auth: email + password. Firestore: nombre, teléfono, enLinea y posición.
 * Storage: foto de perfil en registro/{uid}/perfil.jpg.
 */
object UsuarioRepository {
    private val auth get() = Firebase.auth
    private val usuarios get() = Firebase.firestore.collection("perfilesMapa")
    private val storage get() = Firebase.storage

    val uidActual: String? get() = auth.currentUser?.uid
    val haySesion: Boolean get() = auth.currentUser != null

    // ---------- Autenticación ----------

    fun iniciarSesion(email: String, password: String, onResultado: (Result<Unit>) -> Unit) {
        auth.signInWithEmailAndPassword(email.trim(), password).addOnCompleteListener { task ->
            onResultado(if (task.isSuccessful) Result.success(Unit) else Result.failure(task.exception ?: UnknownError()))
        }
    }

    // Crea la cuenta en Auth, sube la foto (si hay) y guarda el resto de datos en Firestore
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
            val uid = task.result?.user?.uid
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
                usuarios.document(uid).set(datos).addOnCompleteListener {
                    onResultado(if (it.isSuccessful) Result.success(Unit) else Result.failure(it.exception ?: UnknownError()))
                }
            }
            if (foto == null) guardar(null)
            else subirFoto(context, uid, foto) { guardar(it.getOrNull()) }
        }
    }

    fun cerrarSesion() = auth.signOut()

    // La contraseña es competencia de Auth: hay que reautenticar antes de cambiarla
    fun cambiarContrasena(actual: String, nueva: String, onResultado: (Result<Unit>) -> Unit) {
        val user = auth.currentUser
        val email = user?.email
        if (user == null || email == null) {
            onResultado(Result.failure(IllegalStateException("Sin sesión")))
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

    // ---------- Perfil ----------

    fun perfil(uid: String): Flow<Usuario?> = callbackFlow {
        val registro = usuarios.document(uid).addSnapshotListener { snap, _ ->
            trySend(snap?.aUsuario())
        }
        awaitClose { registro.remove() }
    }

    fun actualizarPerfil(uid: String, nombre: String, telefono: String, onResultado: (Result<Unit>) -> Unit) {
        usuarios.document(uid).update(mapOf("nombre" to nombre.trim(), "telefono" to telefono.trim()))
            .addOnCompleteListener {
                onResultado(if (it.isSuccessful) Result.success(Unit) else Result.failure(it.exception ?: UnknownError()))
            }
    }

    fun cambiarFoto(context: Context, uid: String, foto: Uri, onResultado: (Result<Unit>) -> Unit) {
        subirFoto(context, uid, foto) { subida ->
            subida.onSuccess { url ->
                usuarios.document(uid).update("fotoPerfilUrl", url).addOnCompleteListener {
                    onResultado(if (it.isSuccessful) Result.success(Unit) else Result.failure(it.exception ?: UnknownError()))
                }
            }.onFailure { onResultado(Result.failure(it)) }
        }
    }

    private fun subirFoto(context: Context, uid: String, foto: Uri, onResultado: (Result<String>) -> Unit) {
        val bytes = ImageUtils.comprimir(context, foto)
        if (bytes == null) {
            onResultado(Result.failure(IllegalArgumentException("Foto ilegible")))
            return
        }
        val ref = storage.reference.child("registro/$uid/perfil.jpg")
        ref.putBytes(bytes)
            .continueWithTask { tarea ->
                if (!tarea.isSuccessful) throw tarea.exception ?: UnknownError()
                ref.downloadUrl
            }
            .addOnCompleteListener {
                onResultado(if (it.isSuccessful) Result.success(it.result.toString()) else Result.failure(it.exception ?: UnknownError()))
            }
    }

    // ---------- Localización en tiempo real ----------

    fun marcarEnLinea(uid: String, latitud: Double?, longitud: Double?, onListo: () -> Unit = {}) {
        usuarios.document(uid).update(mapOf("enLinea" to true, "latitud" to latitud, "longitud" to longitud))
            .addOnCompleteListener { onListo() }
    }

    fun marcarDesconectado(uid: String, onListo: () -> Unit = {}) {
        usuarios.document(uid).update(mapOf("enLinea" to false, "latitud" to null, "longitud" to null))
            .addOnCompleteListener { onListo() }
    }

    fun actualizarPosicion(uid: String, latitud: Double, longitud: Double) {
        usuarios.document(uid).update(mapOf("latitud" to latitud, "longitud" to longitud))
    }

    // Escucha a los usuarios en línea. Cuando uno se desconecta deja de cumplir el filtro y llega como REMOVED
    fun cambiosEnLinea(): Flow<List<CambioUsuario>> = callbackFlow {
        val registro = usuarios.whereEqualTo("enLinea", true).addSnapshotListener { snap, _ ->
            if (snap == null) return@addSnapshotListener
            val cambios = snap.documentChanges.mapNotNull { cambio ->
                cambio.document.aUsuario()?.let { CambioUsuario(it, cambio.type == DocumentChange.Type.REMOVED) }
            }
            trySend(cambios)
        }
        awaitClose { registro.remove() }
    }

    private fun DocumentSnapshot.aUsuario(): Usuario? {
        if (!exists()) return null
        return Usuario(
            id = id,
            nombre = getString("nombre").orEmpty(),
            email = getString("email").orEmpty(),
            telefono = getString("telefono").orEmpty(),
            enLinea = getBoolean("enLinea") ?: false,
            latitud = getDouble("latitud"),
            longitud = getDouble("longitud"),
            fotoPerfilUrl = getString("fotoPerfilUrl"),
        )
    }
}
