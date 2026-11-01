package com.example.registro.data

// Datos secundarios del usuario (Firestore: perfilesMapa/{uid}). El password vive solo en Firebase Auth.
data class Usuario(
    val id: String = "",
    val nombre: String = "",
    val email: String = "",
    val telefono: String = "",
    val enLinea: Boolean = false,
    val latitud: Double? = null,
    val longitud: Double? = null,
    val fotoPerfilUrl: String? = null,
)
