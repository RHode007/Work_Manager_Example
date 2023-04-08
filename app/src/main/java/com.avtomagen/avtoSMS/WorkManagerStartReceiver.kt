package com.avtomagen.avtoSMS

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.WorkInfo
import androidx.work.WorkManager

class WorkManagerStartReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val list = WorkManager.getInstance(context).getWorkInfosByTag(MainActivity.TAG_SEND_SMS).get().filter {
                it.state == WorkInfo.State.ENQUEUED
            }
            when (list.size){
                0 -> MainActivity().setPeriodicallySendingSms()
                1 -> return
                else -> {
                    MainActivity().cancelSendingSms()
                    MainActivity().setPeriodicallySendingSms()
                }
            }
        }
    }
}