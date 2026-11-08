package com.example.registro.utils

import android.util.Patterns
import androidx.annotation.StringRes
import com.example.registro.R
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException

object Validators {
    fun emailValido(email: String) = Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()
    fun passwordValida(password: String) = password.length >= 6
    fun telefonoValido(telefono: String) = telefono.trim().matches(Regex("^\\+?[0-9]{7,15}$"))

    @StringRes
    fun mensajeError(e: Throwable?): Int = when (e) {
        is FirebaseAuthInvalidCredentialsException, is FirebaseAuthInvalidUserException -> R.string.error_wrong_credentials
        is FirebaseAuthUserCollisionException -> R.string.error_email_in_use
        is FirebaseAuthWeakPasswordException -> R.string.error_weak_password
        is FirebaseNetworkException -> R.string.error_network
        is FirebaseTooManyRequestsException -> R.string.error_too_many
        else -> R.string.error_unknown
    }
}
