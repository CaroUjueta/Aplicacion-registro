package com.example.registro.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.File

object ImageUtils {
    private const val LADO_MAXIMO = 512

    fun crearArchivoTemporal(context: Context): File {
        val carpeta = File(context.cacheDir, "fotos").apply { mkdirs() }
        return File.createTempFile("perfil_", ".jpg", carpeta)
    }

    private fun matrizOrientacion(context: Context, uri: Uri): Matrix {
        val orientacion = context.contentResolver.openInputStream(uri)?.use {
            ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        } ?: ExifInterface.ORIENTATION_NORMAL
        return Matrix().apply {
            when (orientacion) {
                ExifInterface.ORIENTATION_ROTATE_90 -> postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> postScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> postScale(1f, -1f)
                ExifInterface.ORIENTATION_TRANSPOSE -> {
                    postRotate(90f)
                    postScale(-1f, 1f)
                }
                ExifInterface.ORIENTATION_TRANSVERSE -> {
                    postRotate(270f)
                    postScale(-1f, 1f)
                }
            }
        }
    }

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
            val escala = minOf(1f, LADO_MAXIMO.toFloat() / maxOf(it.width, it.height))
            val matriz = matrizOrientacion(context, uri).apply { postScale(escala, escala) }
            val final = Bitmap.createBitmap(it, 0, 0, it.width, it.height, matriz, true)
            ByteArrayOutputStream().also { salida -> final.compress(Bitmap.CompressFormat.JPEG, 85, salida) }.toByteArray()
        }
    } catch (_: Exception) {
        null
    }
}
