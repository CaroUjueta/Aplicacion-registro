package com.example.registro.utils

import android.content.Context
import java.io.File

object ImageUtils {
    fun crearArchivoTemporal(context: Context): File {
        val carpeta = File(context.cacheDir, "fotos").apply { mkdirs() }
        return File.createTempFile("perfil_", ".jpg", carpeta)
    }
}
