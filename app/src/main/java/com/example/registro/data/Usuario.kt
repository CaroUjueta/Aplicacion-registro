package com.example.registro.data

data class Usuario(
    val id: String = "",
    val nombre: String = "",
    val email: String = "",
    val telefono: String = "",
    val enLinea: Boolean = false,
    val latitud: Double? = null,
    val longitud: Double? = null,
    val fotoPerfilUrl: String? = null,
    val ultimaActividad: Long = 0L,
)
