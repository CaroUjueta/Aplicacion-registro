package com.example.registro.repository

import com.example.registro.data.Usuario

fun interface Subscription {
    fun cancel()
}

data class CambioUsuario(val usuario: Usuario, val salio: Boolean)

interface UserRepository {
    fun guardar(uid: String, usuario: Usuario, onResult: (Result<Unit>) -> Unit)
    fun obtener(uid: String, onResult: (Result<Usuario?>) -> Unit)
    fun observar(uid: String, onChange: (Result<Usuario?>) -> Unit): Subscription
    fun actualizar(uid: String, campos: Map<String, Any?>, onResult: (Result<Unit>) -> Unit = {})
    fun eliminar(uid: String, onResult: (Result<Unit>) -> Unit = {})
    fun observarEnLinea(onChange: (Result<CambioUsuario>) -> Unit): Subscription
}
