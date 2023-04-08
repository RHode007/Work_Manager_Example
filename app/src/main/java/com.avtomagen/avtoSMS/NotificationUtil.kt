package com.avtomagen.avtoSMS

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.avtomagen.avtoSMS.MainActivity.Companion.CHANNEL_ID

fun NotificationManager.sendNotification(context: Context, title: String = "Succeed", desc : String = "Worker is completed") {

    val notifyId = 42633687

    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
        .setContentTitle(title)
        .setContentText(desc)
        .setSmallIcon(R.drawable.ic_baseline_celebration_24)

    notify(notifyId, notification.build())
}