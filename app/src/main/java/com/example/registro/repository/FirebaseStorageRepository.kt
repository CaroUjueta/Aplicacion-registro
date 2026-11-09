package com.example.registro.repository

import com.google.firebase.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.storage

class FirebaseStorageRepository(
    storage: FirebaseStorage = Firebase.storage
) : StorageRepository {

    private val raiz = storage.reference.child("registro")

    override fun subirFotoPerfil(uid: String, bytes: ByteArray, onResult: (Result<String>) -> Unit) {
        val ref = raiz.child("$uid/perfil.jpg")
        ref.putBytes(bytes)
            .continueWithTask { ref.downloadUrl }
            .addOnCompleteListener { tarea ->
                if (tarea.isSuccessful) {
                    // el parámetro v evita que Coil muestre la foto anterior en caché
                    onResult(Result.success("${tarea.result}&v=${System.currentTimeMillis()}"))
                } else {
                    onResult(Result.failure(tarea.exception ?: UnknownError()))
                }
            }
    }
}
