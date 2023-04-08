package com.avtomagen.avtoSMS

import android.app.NotificationManager
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.avtomagen.avtoSMS.MainActivity.Companion.KEY_SMS_NOTIFICATION_DESC
import com.avtomagen.avtoSMS.MainActivity.Companion.KEY_SMS_NOTIFICATION_TITLE

class NotificationWorker(val context: Context, userParameters: WorkerParameters) :
    Worker(context, userParameters) {

    override fun doWork(): Result {
        try {

            val downloadTitle = inputData.getString(KEY_SMS_NOTIFICATION_TITLE) ?: return Result.failure()
            val downloadDesc = inputData.getString(KEY_SMS_NOTIFICATION_DESC) ?: return Result.failure()

            val notificationManager: NotificationManager =
                context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager

            notificationManager.sendNotification(context, downloadTitle, downloadDesc)

            return Result.success()
        } catch (e: Exception) {
            return Result.failure()
        }
    }
}