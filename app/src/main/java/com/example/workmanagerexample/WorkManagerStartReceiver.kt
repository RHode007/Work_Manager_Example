package com.example.workmanagerexample

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class WorkManagerStartReceiver : BroadcastReceiver() {
    var mWorkManager: WorkManager? = null

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val myWorkBuilder = PeriodicWorkRequest.Builder(
                SendLogWorker::class.java,
                PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS,
                TimeUnit.MILLISECONDS
            ).build()
            mWorkManager = WorkManager.getInstance(context)
            mWorkManager!!.enqueue(myWorkBuilder)
        }
    }
}