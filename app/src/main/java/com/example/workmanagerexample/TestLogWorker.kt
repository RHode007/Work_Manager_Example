package com.example.workmanagerexample

import android.content.Context
import android.util.Log
import androidx.work.*
import java.util.concurrent.TimeUnit


class TestLogWorker(private val context: Context,params: WorkerParameters) : Worker(context, params) {
    fun doWork1(): Result {
        try {
            for (i in 0..5) {
                Log.i("furkanpasa", "Logging $i")
            }
            return Result.success()
        }catch (e: Exception) {
            return Result.failure()
        }
    }

    override fun doWork(): Result {
        Log.i("tracer:", "Worker executed")
        // Indicate whether the work finished successfully with the Result
        val mywork = OneTimeWorkRequest.Builder(TestLogWorker::class.java)
            .setInitialDelay(25, TimeUnit.SECONDS)
            .addTag(MainActivity.TAG_SEND_LOG)
            .build()
        WorkManager.getInstance(context).enqueue(mywork)
        return Result.success()
    }
}