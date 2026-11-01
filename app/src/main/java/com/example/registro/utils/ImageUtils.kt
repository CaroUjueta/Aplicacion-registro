package com.example.registro.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.File

object ImageUtils {
    private const val LADO_MAXIMO = 512

    // Archivo temporal donde CameraX guarda la foto recién tomada
    fun crearArchivoTemporal(context: Context): File {
        val carpeta = File(context.cacheDir, "fotos").apply { mkdirs() }
        return File.createTempFile("perfil_", ".jpg", carpeta)
    }

    // Reduce la foto a 512 px y la comprime en JPEG: sube rápido y el marcador la carga sin esperar
    fun comprimir(context: Context, uri: Uri): ByteArray? = try {
        val opciones = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opciones) }
        var muestreo = 1
        while (opciones.outWidth / muestreo > LADO_MAXIMO * 2 || opciones.outHeight / muestreo > LADO_MAXIMO * 2) {
            muestreo *= 2
        }
        val bitmap = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = muestreo })
        }
        bitmap?.let {
            val escala = LADO_MAXIMO.toFloat() / maxOf(it.width, it.height)
            val final = if (escala < 1f) {
                Bitmap.createScaledBitmap(it, (it.width * escala).toInt(), (it.height * escala).toInt(), true)
            } else it
            ByteArrayOutputStream().also { salida -> final.compress(Bitmap.CompressFormat.JPEG, 85, salida) }.toByteArray()
        }
    } catch (_: Exception) {
        null
    }
}
