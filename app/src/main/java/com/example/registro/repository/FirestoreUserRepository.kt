package com.example.registro.repository

import com.example.registro.data.Usuario
import com.google.android.gms.tasks.Task
import com.google.firebase.Firebase
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore

class FirestoreUserRepository(
    db: FirebaseFirestore = Firebase.firestore
) : UserRepository {

    private val usuarios = db.collection(USUARIOS)

    override fun guardar(uid: String, usuario: Usuario, onResult: (Result<Unit>) -> Unit) {
        val datos = mapOf(
            "nombre" to usuario.nombre,
            "email" to usuario.email,
            "telefono" to usuario.telefono,
            "enLinea" to usuario.enLinea,
            "fotoPerfilUrl" to usuario.fotoPerfilUrl,
        )
        usuarios.document(uid).set(datos, SetOptions.merge()).addOnCompleteListener { onResult(it.toUnitResult()) }
    }

    override fun obtener(uid: String, onResult: (Result<Usuario?>) -> Unit) {
        usuarios.document(uid).get().addOnCompleteListener { tarea ->
            onResult(
                if (tarea.isSuccessful) Result.success(tarea.result?.aUsuario())
                else Result.failure(tarea.exception ?: UnknownError())
            )
        }
    }

    override fun observar(uid: String, onChange: (Result<Usuario?>) -> Unit): Subscription {
        val registro = usuarios.document(uid).addSnapshotListener { snap, error ->
            onChange(if (error != null) Result.failure(error) else Result.success(snap?.aUsuario()))
        }
        return Subscription { registro.remove() }
    }

    override fun actualizar(uid: String, campos: Map<String, Any?>, onResult: (Result<Unit>) -> Unit) {
        usuarios.document(uid).update(campos).addOnCompleteListener { onResult(it.toUnitResult()) }
    }

    override fun eliminar(uid: String, onResult: (Result<Unit>) -> Unit) {
        usuarios.document(uid).delete().addOnCompleteListener { onResult(it.toUnitResult()) }
    }

    override fun observarEnLinea(onChange: (Result<CambioUsuario>) -> Unit): Subscription {
        val registro = usuarios.whereEqualTo("enLinea", true).limit(100).addSnapshotListener { snap, error ->
            if (error != null || snap == null) {
                if (error != null) onChange(Result.failure(error))
                return@addSnapshotListener
            }
            for (cambio in snap.documentChanges) {
                val usuario = cambio.document.aUsuario() ?: continue
                onChange(Result.success(CambioUsuario(usuario, cambio.type == DocumentChange.Type.REMOVED)))
            }
        }
        return Subscription { registro.remove() }
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
            ultimaActividad = getLong("ultimaActividad") ?: 0L,
        )
    }

    private companion object {
        const val USUARIOS = "perfilesMapa"
    }
}

internal fun Task<*>.toUnitResult(): Result<Unit> =
    if (isSuccessful) Result.success(Unit) else Result.failure(exception ?: UnknownError())
