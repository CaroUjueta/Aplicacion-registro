package com.example.registro.utils

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

sealed interface UiText {
    data class Dynamic(val value: String) : UiText
    data class Resource(@StringRes val id: Int, val args: List<Any> = emptyList()) : UiText

    fun asString(context: Context): String = when (this) {
        is Dynamic -> value
        is Resource -> context.getString(id, *args.toTypedArray())
    }

    @Composable
    fun asString(): String = asString(LocalContext.current)
}

fun String?.orResource(@StringRes id: Int): UiText =
    this?.let { UiText.Dynamic(it) } ?: UiText.Resource(id)
