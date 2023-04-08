package com.avtomagen.avtoSMS

import android.app.NotificationManager
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import androidx.work.Worker
import androidx.work.WorkerParameters

class UploadWorker(val context: Context, userParameters: WorkerParameters) : Worker(context, userParameters) {

    override fun doWork(): Result {
        try {

            val uploadTitle = inputData.getString(MainActivity.KEY_UPLOAD_TITLE) ?: return Result.failure()
            val uploadDesc = inputData.getString(MainActivity.KEY_UPLOAD_DESC) ?: return Result.failure()

            val notificationManager : NotificationManager = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.sendNotification(context, uploadTitle, uploadDesc)


            return Result.success()
        }catch (e: Exception) {
            return Result.failure()
        }
    }
}