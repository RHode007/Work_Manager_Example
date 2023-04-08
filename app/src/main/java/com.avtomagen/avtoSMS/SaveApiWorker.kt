package com.avtomagen.avtoSMS

import android.content.Context
import androidx.room.Room
import androidx.work.Data
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.avtomagen.avtoSMS.MainActivity.Companion.KEY_API_INPUT_MODE_TEXT
import com.avtomagen.avtoSMS.MainActivity.Companion.KEY_API_INPUT_USER_TEXT

class SaveApiWorker(val context: Context, workerParameters: WorkerParameters) : Worker(context, workerParameters) {

    companion object {
        const val KEY_API_WORKER_RESULT = "key.api_key.result"
    }

    override fun doWork(): Result {
        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "AvtoDB"
        ).allowMainThreadQueries().build()
        when (inputData.getString(KEY_API_INPUT_MODE_TEXT)){
            "setApi" -> {
                db.userDao().insertAll(User(0, inputData.getString(KEY_API_INPUT_USER_TEXT)))
                db.loggerDao().insertAll(Logger(text = "Key set ${inputData.getString(KEY_API_INPUT_USER_TEXT)!!.take(5)}"))
                db.close()
                val outputData = Data.Builder()
                    .putString(KEY_API_WORKER_RESULT, "Ключ ${
                        inputData.getString(KEY_API_INPUT_USER_TEXT)!!.take(5)} предоставлен")
                    .build()
                return Result.success(outputData)
            }
            "getApiStatus" ->{
                var c: String? = db.userDao().getApi()
                c = if(c.isNullOrEmpty()) "Ключ не предоставлен" else "Ключ ${c.take(5)} предоставлен"
                db.close()
                val outputData = Data.Builder()
                    .putString(KEY_API_WORKER_RESULT, c)
                    .build()
                return Result.success(outputData)
            }
        }
        return Result.failure()
    }
}