package com.example.registro.repository

import android.net.Uri

interface StorageRepository {
    fun subirFotoPerfil(uid: String, uri: Uri, onResult: (Result<String>) -> Unit)
}
