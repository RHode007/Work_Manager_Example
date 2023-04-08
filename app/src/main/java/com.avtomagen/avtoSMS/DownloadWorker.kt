package com.avtomagen.avtoSMS

import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.telephony.SmsManager
import androidx.room.Room.databaseBuilder
import androidx.work.*
import com.squareup.moshi.*
import okhttp3.*
import okio.IOException
import java.net.InetAddress

class DownloadWorker(val context: Context, workerParameters: WorkerParameters) : Worker(context, workerParameters) {

    private val client = OkHttpClient()
    private val moshi = Moshi.Builder().build()
    private var phoneListAdapter: JsonAdapter<List<PhoneList>> = moshi.adapter(
        Types.newParameterizedType(
            MutableList::class.java,
            PhoneList::class.java
        )
    )

    @JsonClass(generateAdapter = true)
    data class PhoneList(
        val id: Int?,
        val id_user: Int?,
        val to_send: Int?,
        @Json(name = "sended") val is_sended: Int?,
        val number_tel: String?,
    )

    private val db = databaseBuilder(
        applicationContext,
        AppDatabase::class.java, "AvtoDB"
    ).allowMainThreadQueries().build()

    /* всратая логика
    переодичный запуск воркера:
    если есть иннет:
    получение апи из бд
    запрос и получение номеров
    получение из бд и отправить статус неотправленных номеров в прошлый раз
    проверка на аномалию(в статусе pending)
    установка ресивера смс:
        ок
        TODO если неправильный номер отправить другой статус на сервер !!
        если ошибка связи то ???, смс появится в отправленых но для повторной отправки нужно нажать на него, удалить нельзя просто не обрабатываем
            если нет связи для отправвки статуса то записать в бд
    установка pending и отправка смс
     */

    override fun doWork(): Result {
        val phoneNumber = db.phoneNumberDao()
        val loggerDao = db.loggerDao()
        val apiKey = db.userDao().getApi()
        if (isInternetAvailable()) {
        val requestGetList = Request.Builder()
            .url("https://api.avtomagen.ru/sms/index.php?api=$apiKey")
            .build()
        client.newCall(requestGetList).execute().use { responseGetList ->
            val responseJsonString = responseGetList.body!!.string()
            val outputData = Data.Builder()
                .putString("response", responseJsonString)
                .build()
            if (!responseGetList.isSuccessful) return Result.failure(outputData)
            if ("\"errorCode\":103" in responseJsonString) return Result.failure(outputData)

            val gist: List<PhoneList> = phoneListAdapter.fromJson(responseJsonString)
                ?: return Result.failure(outputData)
            val mutableGist = gist.toMutableList()
            val phoneNumbersList: List<PhoneNumber> = phoneNumber.getAll()
//                println("from local db $phoneNumbersList")
            loggerDao.insertAll(Logger(text = "from remote db $mutableGist"))
            loggerDao.insertAll(Logger(text = "from local db $phoneNumbersList"))
            for (itemLocal in phoneNumbersList) {
                gist.forEachIndexed { _, itemRemote ->
                    if ((itemLocal.id == itemRemote.id)) {
//                            println(itemRemote.number_tel)
                        sendStatusToServer(
                            apiKey,
                            itemLocal.id,
                            itemLocal.status!!,
                            itemLocal.number,
                            itemLocal.text
                        )
                        phoneNumber.delete(itemLocal)
                        mutableGist.remove(itemRemote)
                    }
                    if ((itemLocal.id == itemRemote.id) && (itemLocal.status!! == "pending")){
                        loggerDao.insertAll(Logger(text = "!!anomaly skipped $itemRemote"))
                        mutableGist.remove(itemRemote)
                    }
                }
            }
            if (mutableGist.isEmpty()) return Result.success()

//            println("from remote after local $mutableGist")
            loggerDao.insertAll(Logger(text = "from remote after local $mutableGist"))
            responseGetList.body!!.close()
            val smsStatusListener = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    val db = databaseBuilder(
                        applicationContext,
                        AppDatabase::class.java, "AvtoDB"
                    ).allowMainThreadQueries().build()
                    when (resultCode) {
                        Activity.RESULT_OK -> {
//                            println("SMS sent successfully.")
                            db.loggerDao().insertAll(Logger(text = "SMS sent successfully."))
                            sendStatusToServer(
                                apiKey,
                                intent?.getIntExtra("id", 0) ?: 0,
                                "true",
                                intent?.getStringExtra("phoneNumber"),
                                intent?.getStringExtra("message")
                            )
                            sendNotification(
                                intent?.getIntExtra("id", 0) ?: 0,
                                intent?.getStringExtra("phoneNumber")!!,
                                "")
                        }
                        Activity.RESULT_CANCELED -> {
//                            println("SMS failed to send.")
                            db.loggerDao().insertAll(Logger(text = "SMS failed to send."))
                            sendStatusToServer(
                                apiKey,
                                intent?.getIntExtra("id", 0) ?: 0,
                                "false",
                                intent?.getStringExtra("phoneNumber"),
                                intent?.getStringExtra("message")
                            )
                            sendNotification(
                                intent?.getIntExtra("id", 0) ?: 0,
                                intent?.getStringExtra("phoneNumber")!!,
                                " не")
                        }
                        SmsManager.RESULT_ERROR_NO_SERVICE -> {
//                            println("RESULT_ERROR_NO_SERVICE.")
                            db.loggerDao().insertAll(Logger(text = "RESULT_ERROR_NO_SERVICE"))
                            sendStatusToServer(
                                apiKey,
                                intent?.getIntExtra("id", 0) ?: 0,
                                "false",
                                intent?.getStringExtra("phoneNumber"),
                                intent?.getStringExtra("message")
                            )
                            sendNotification(
                                intent?.getIntExtra("id", 0) ?: 0,
                                intent?.getStringExtra("phoneNumber")!!,
                                " не")
                        }
                        SmsManager.RESULT_NETWORK_REJECT -> {
//                            println("RESULT_NETWORK_REJECT.")
                            db.loggerDao().insertAll(Logger(text = "RESULT_NETWORK_REJECT"))
                            sendStatusToServer(
                                apiKey,
                                intent?.getIntExtra("id", 0) ?: 0,
                                "false",
                                intent?.getStringExtra("phoneNumber"),
                                intent?.getStringExtra("message")
                            )
                            sendNotification(
                                intent?.getIntExtra("id", 0) ?: 0,
                                intent?.getStringExtra("phoneNumber")!!,
                                " не")
                        }
                        else ->{
//                            println("some shit happened, unhandled sms response $resultCode.")
                            db.loggerDao().insertAll(Logger(text = "some shit happened, unhandled sms response $resultCode"))
                            sendStatusToServer(
                                apiKey,
                                intent?.getIntExtra("id", 0) ?: 0,
                                "false",
                                intent?.getStringExtra("phoneNumber"),
                                intent?.getStringExtra("message")
                            )
                            sendNotification(
                                intent?.getIntExtra("id", 0) ?: 0,
                                intent?.getStringExtra("phoneNumber")!!,
                                " не")
                        }
                    }
                }
            }
            applicationContext.registerReceiver(smsStatusListener, IntentFilter("SMS_SENT"))
            for (item in mutableGist) {
                sendSMS(
                    item.id!!,
                    "+" + item.number_tel!!,
                    "Ваш заказ готов к выдаче. Работаем с 8 до 16. Автомагазин"
                )
            }
        }


        //applicationContext.registerReceiver(smsStatusListener, IntentFilter("SMS_DELIVERED"))
        db.close()
        val outputData = Data.Builder()
            .putString("user_data", "Nice!!")
            .build()
        return Result.success(outputData)
        }
        db.loggerDao().insertAll(Logger(text = "no internet"))
        db.close()
        val outputData = Data.Builder()
            .putString("user_data", "no internet")
            .build()
        return Result.failure(outputData)
    }

    private fun log(inputString: String): String {
        return if (inputString.startsWith("+")) {
            inputString.substring(1)
        } else {
            inputString
        }
    }

    private fun removePlus(inputString: String): String {
        return if (inputString.startsWith("+")) {
            inputString.substring(1)
        } else {
            inputString
        }
    }

    private fun isInternetAvailable(): Boolean {
        return try {
            val ipAddr: InetAddress = InetAddress.getByName("api.avtomagen.ru")
            !ipAddr.equals("")
        } catch (e: Exception) {
            false
        }
    }

    private fun sendNotification(id: Int, phoneNumber: String, status: String) {
        val workManager = WorkManager.getInstance(applicationContext)

        val data = Data.Builder()
            .putString(MainActivity.KEY_SMS_NOTIFICATION_TITLE, "Id $id")
            .putString(MainActivity.KEY_SMS_NOTIFICATION_DESC,
                "Смс$status отправлено на номер $phoneNumber"
            )
            .build()
        val notificationWorker = OneTimeWorkRequestBuilder<NotificationWorker>()
            .setInputData(data)
            .build()

        workManager.enqueue(notificationWorker)
    }

    private fun sendSMS(id: Int, phoneNumber: String, text: String) {
        val smsManager: SmsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(SmsManager::class.java)
        } else {
            @Suppress("DEPRECATION")
            SmsManager.getDefault()
        }

        val sentIntent = PendingIntent.getBroadcast(
            applicationContext,
            id,
            Intent("SMS_SENT")
                .putExtra("id", id)
                .putExtra("phoneNumber", removePlus(phoneNumber))
                .putExtra("message", text),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        db.phoneNumberDao().insertAll(PhoneNumber(id, "pending", phoneNumber, text))
        return smsManager.sendTextMessage(phoneNumber, null, text, sentIntent, null)
    }

    private fun sendStatusToServer(
        api: String,
        id: Int?,
        status: String,
        number: String?,
        text: String?
    ) {
        val formBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("api", api)
            .addFormDataPart("id", id.toString())
            .addFormDataPart("status", status) //status.toString())
            .build()
        val requestResponse = Request.Builder()
            .url("https://api.avtomagen.ru/sms/index.php")
            .post(formBody)
            .build()
        client.newCall(requestResponse).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // Handle failure
//                println("failure sendStatusToServer. $id")
                val db = databaseBuilder(
                    applicationContext,
                    AppDatabase::class.java, "AvtoDB"
                ).allowMainThreadQueries().build()
                db.loggerDao().insertAll(Logger(text = "failure sendStatusToServer. $id"))
                db.phoneNumberDao().insertAll(PhoneNumber(id!!, status, number, text))
                db.close()
            }

            override fun onResponse(call: Call, response: Response) {
                // Handle success
                if (!response.isSuccessful) {
                    throw IOException("Exception $response")
                }
//                println("success sendStatusToServer. $id")
                val db = databaseBuilder(
                    applicationContext,
                    AppDatabase::class.java, "AvtoDB"
                ).allowMainThreadQueries().build()
                db.loggerDao().insertAll(Logger(text = "success sendStatusToServer. $id"))
                response.body?.close()
                db.close()
            }
        })
        db.close()
    }
}