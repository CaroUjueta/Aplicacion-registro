package com.example.registro.repository

interface StorageRepository {
    fun subirFotoPerfil(uid: String, bytes: ByteArray, onResult: (Result<String>) -> Unit)
}
