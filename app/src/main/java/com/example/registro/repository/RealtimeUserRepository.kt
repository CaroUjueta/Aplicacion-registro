package com.example.registro.repository

import com.example.registro.data.Usuario
import com.google.android.gms.tasks.Task
import com.google.firebase.Firebase
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database

class RealtimeUserRepository(
    db: FirebaseDatabase = Firebase.database
) : UserRepository {

    private val usuarios = db.reference.child("usuarios")

    override fun guardar(uid: String, usuario: Usuario, onResult: (Result<Unit>) -> Unit) {
        val datos = mapOf(
            "nombre" to usuario.nombre,
            "email" to usuario.email,
            "telefono" to usuario.telefono,
            "enLinea" to usuario.enLinea,
            "fotoPerfilUrl" to usuario.fotoPerfilUrl,
        )
        usuarios.child(uid).setValue(datos).addOnCompleteListener { onResult(it.aResultado()) }
    }

    override fun observar(uid: String, onChange: (Usuario?) -> Unit): Subscription {
        val ref = usuarios.child(uid)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) = onChange(snapshot.aUsuario())
            override fun onCancelled(error: DatabaseError) {}
        }
        ref.addValueEventListener(listener)
        return Subscription { ref.removeEventListener(listener) }
    }

    override fun actualizar(uid: String, campos: Map<String, Any?>, onResult: (Result<Unit>) -> Unit) {
        usuarios.child(uid).updateChildren(campos).addOnCompleteListener { onResult(it.aResultado()) }
    }

    override fun observarEnLinea(onChange: (Usuario, Boolean) -> Unit): Subscription {
        val consulta = usuarios.orderByChild("enLinea").equalTo(true).limitToFirst(100)
        val listener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) = avisar(snapshot, false)
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) = avisar(snapshot, false)
            override fun onChildRemoved(snapshot: DataSnapshot) = avisar(snapshot, true)
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}

            private fun avisar(snapshot: DataSnapshot, salio: Boolean) {
                snapshot.aUsuario()?.let { onChange(it, salio) }
            }
        }
        consulta.addChildEventListener(listener)
        return Subscription { consulta.removeEventListener(listener) }
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

internal fun Task<*>.aResultado(): Result<Unit> =
    if (isSuccessful) Result.success(Unit) else Result.failure(exception ?: UnknownError())
