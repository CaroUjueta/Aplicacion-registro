package com.example.registro.repository

import android.net.Uri
import com.google.firebase.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.storage
import com.google.firebase.storage.storageMetadata

class FirebaseStorageRepository(
    storage: FirebaseStorage = Firebase.storage
) : StorageRepository {

    private val raiz = storage.reference.child("registro")

    override fun subirFotoPerfil(uid: String, uri: Uri, onResult: (Result<String>) -> Unit) {
        val ref = raiz.child("$uid/perfil.jpg")
        val metadata = storageMetadata { contentType = "image/jpeg" }
        ref.putFile(uri, metadata).continueWithTask { task ->
            if (!task.isSuccessful) throw task.exception ?: UnknownError()
            ref.downloadUrl
        }.addOnCompleteListener { task ->
            onResult(
                if (task.isSuccessful) Result.success("${task.result}&v=${System.currentTimeMillis()}")
                else Result.failure(task.exception ?: UnknownError())
            )
        }
    }
}
