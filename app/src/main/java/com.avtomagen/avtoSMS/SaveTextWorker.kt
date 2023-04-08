package com.avtomagen.avtoSMS

import android.content.Context
import androidx.room.Room
import androidx.work.Data
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.avtomagen.avtoSMS.MainActivity.Companion.KEY_API_INPUT_MODE_TEXT
import com.avtomagen.avtoSMS.MainActivity.Companion.KEY_DEFAULT_TEXT_INPUT_USER_TEXT

class SaveTextWorker(val context: Context, workerParameters: WorkerParameters) : Worker(context, workerParameters) {

    companion object {
        const val KEY_DEFAULT_TEXT_WORKER_RESULT = "key.api_key.result"
    }

    override fun doWork(): Result {
        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "AvtoDB"
        ).allowMainThreadQueries().build()
        when (inputData.getString(KEY_API_INPUT_MODE_TEXT)){
            "setDefaultText" -> {
                db.userDao().updateDefaultText(0, inputData.getString(KEY_DEFAULT_TEXT_INPUT_USER_TEXT)!!)
                log(db,"Key set ${inputData.getString(KEY_DEFAULT_TEXT_INPUT_USER_TEXT)!!.take(5)}")
                db.close()
                val outputData = Data.Builder()
                    .putString(KEY_DEFAULT_TEXT_WORKER_RESULT, "Ключ ${
                        inputData.getString(KEY_DEFAULT_TEXT_INPUT_USER_TEXT)!!.take(5)} предоставлен")
                    .build()
                return Result.success(outputData)
            }
            "getDefaultText" ->{
                var c: String? = db.userDao().getById(0)?.defaultText
                c = if(c.isNullOrEmpty()) "Текст не предоставлен" else "$c"
                db.close()
                val outputData = Data.Builder()
                    .putString(KEY_DEFAULT_TEXT_WORKER_RESULT, c)
                    .build()
                return Result.success(outputData)
            }
        }
        return Result.failure()
    }

    private fun log(db: AppDatabase, text: String) {
        if (db.userDao().getById(0).logging == 1)
            db.loggerDao().insertAll(Logger(text = text))
    }
}