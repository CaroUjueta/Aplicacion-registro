package com.example.registro.utils

import android.util.Log
import android.util.Patterns
import com.example.registro.R
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.storage.StorageException

object Validators {
    private const val TAG = "Registro"

    fun emailValido(email: String) = Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()
    fun passwordValida(password: String) = password.length >= 6
    fun telefonoValido(telefono: String) = telefono.trim().matches(Regex("^\\+?[0-9]{7,15}$"))

    fun mensajeError(e: Throwable?): UiText {
        if (e != null) Log.e(TAG, "Error de Firebase", e)
        return when (e) {
            is FirebaseAuthWeakPasswordException -> UiText.Resource(R.string.error_weak_password)
            is FirebaseAuthUserCollisionException -> UiText.Resource(R.string.error_email_in_use)
            is FirebaseAuthRecentLoginRequiredException -> UiText.Resource(R.string.error_recent_login)
            is FirebaseAuthInvalidCredentialsException, is FirebaseAuthInvalidUserException -> UiText.Resource(R.string.error_wrong_credentials)
            is FirebaseNetworkException -> UiText.Resource(R.string.error_network)
            is FirebaseTooManyRequestsException -> UiText.Resource(R.string.error_too_many)
            is FirebaseFirestoreException -> when (e.code) {
                FirebaseFirestoreException.Code.PERMISSION_DENIED -> UiText.Resource(R.string.error_permission_denied)
                FirebaseFirestoreException.Code.NOT_FOUND -> UiText.Resource(R.string.error_profile_not_found)
                FirebaseFirestoreException.Code.UNAVAILABLE -> UiText.Resource(R.string.error_network)
                else -> e.localizedMessage.orResource(R.string.error_unknown)
            }
            is StorageException -> when (e.errorCode) {
                StorageException.ERROR_NOT_AUTHORIZED -> UiText.Resource(R.string.error_permission_denied)
                StorageException.ERROR_RETRY_LIMIT_EXCEEDED -> UiText.Resource(R.string.error_network)
                else -> e.localizedMessage.orResource(R.string.error_photo_upload)
            }
            else -> e?.localizedMessage.orResource(R.string.error_unknown)
        }
    }
}
