package com.example.registro.repository

import com.example.registro.data.Usuario

fun interface Subscription {
    fun cancel()
}

interface UserRepository {
    fun guardar(uid: String, usuario: Usuario, onResult: (Result<Unit>) -> Unit)
    fun observar(uid: String, onChange: (Usuario?) -> Unit): Subscription
    fun actualizar(uid: String, campos: Map<String, Any?>, onResult: (Result<Unit>) -> Unit = {})
    fun observarEnLinea(onChange: (usuario: Usuario, salio: Boolean) -> Unit): Subscription
}
