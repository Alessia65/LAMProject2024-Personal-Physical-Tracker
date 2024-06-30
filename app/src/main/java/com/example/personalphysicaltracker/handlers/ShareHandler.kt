package com.example.personalphysicaltracker.handlers

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.personalphysicaltracker.activities.ActivityType
import com.example.personalphysicaltracker.activities.PhysicalActivity
import com.example.personalphysicaltracker.activities.WalkingActivity

object ShareHandler {

    fun exportActivitiesToCSV(context: Context, activities: List<PhysicalActivity>) {
        val fileName = "activities.csv"
        val csvContent = generateCSVContent(activities) // Genera il contenuto CSV

        val csvUri = saveCSVFileToMediaStore(context, fileName, csvContent) // Salva il file CSV nel MediaStore
        csvUri?.let {
            shareCSVFile(it, context) // Condividi il file CSV
        }
    }

    private fun generateCSVContent(activities: List<PhysicalActivity>): String {
        val stringBuilder = StringBuilder()
        stringBuilder.append("Activity,Date,Started At,Ended At,Duration,Steps\n")
        activities.forEach { activity ->
            val startTime = activity.start.substring(11)
            val endTime = activity.end.substring(11)
            val steps = if (activity.getActivityTypeName() == ActivityType.WALKING) {
                (activity as WalkingActivity).getSteps().toString()
            } else {
                ""
            }
            stringBuilder.append("${activity.getActivityTypeName()},${activity.date},$startTime,$endTime,${activity.duration},$steps\n")
        }
        return stringBuilder.toString()
    }

    private fun saveCSVFileToMediaStore(context: Context, fileName: String, csvContent: String): Uri? {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS) // Imposta il percorso relativo
            }
        }

        var csvUri: Uri? = null
        try {
            val contentResolver: ContentResolver = context.contentResolver
            val contentUri = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            csvUri = contentResolver.insert(contentUri, contentValues) // Inserisci il file nel MediaStore

            csvUri?.let { uri ->
                contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(csvContent.toByteArray()) // Scrivi il contenuto nel file
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return csvUri
    }

    private fun shareCSVFile(csvUri: Uri, context: Context) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, csvUri) // Aggiungi l'URI del file CSV
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share CSV")) // Avvia l'intenzione di condivisione
    }
}