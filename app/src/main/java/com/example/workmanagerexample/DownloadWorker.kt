package com.example.workmanagerexample

import android.content.Context
import android.os.Build
import android.telephony.SmsManager
import androidx.work.Data
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.squareup.moshi.*
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.IOException

class DownloadWorker(val context: Context, workerParameters: WorkerParameters) : Worker(context, workerParameters) {

    private val client = OkHttpClient()
    private val moshi = Moshi.Builder().build()
    private var photoListAdapter: JsonAdapter<List<PhoneList>> = moshi.adapter(
        Types.newParameterizedType(
            MutableList::class.java,
            PhoneList::class.java
        )
    )

    override fun doWork(): Result {
        val requestGetList = Request.Builder()
            .url("https://api.avtomagen.ru/sms/index.php?api=354983284a5d8912bad0c41c808ad1e9")
            .build()
        client.newCall(requestGetList).execute().use { responseGetList ->
            //if (!responseGetList.isSuccessful) throw IOException("Unexpected code $responseGetList")
            val outputData = Data.Builder()
                .putString("user_data", responseGetList.body!!.source().toString())
                .build()
            /*if (!responseGetList.isSuccessful) return {
                val outputData = Data.Builder()
                    .putString("user_data", responseGetList.body!!.source().toString())
                    .build()
                Result.failure(outputData)
            }*/
            if (!responseGetList.isSuccessful) return Result.failure(outputData)

            val gist : List<PhoneList> = photoListAdapter.fromJson(responseGetList.body!!.source())
                ?: throw IOException("Unexpected code $responseGetList")
            //TODO check for null
            /*for (key in gist!!) {
                println(key.id)
                println(key.id_user)
                println(key.to_send)
                println(key.is_sended)
                println(key.number_tel)
            }*/
            //val test = workDataOf("id" to gist[0].number_tel)

            var result = Unit
            for (item in gist){
                sendSMS(item.number_tel!!, "test")
            }

            val status=true //TODO sms status check in future
            if (status) {
                val formBody = FormBody.Builder()
                    .add("id", gist[0].id.toString())
                    .add("status", "true")
                    .build()
                val requestResponse = Request.Builder()
                    .url("https://api.avtomagen.ru/sms/index.php?api=354983284a5d8912bad0c41c808ad1e9")
                    .post(formBody)
                    .build()

                client.newCall(requestResponse).execute().use { responsePostList ->
                    if (!responsePostList.isSuccessful) throw IOException("Unexpected code $responsePostList")

                    println(responsePostList.body!!.string())
                }
            }
        }
        val outputData = Data.Builder()
            .putString("user_data", "Nice!!")
            .build()
        return Result.success(outputData)
    }

    private fun sendSMS(phoneNumber: String, message: String) {
        val smsManager: SmsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(SmsManager::class.java)
        } else {
            SmsManager.getDefault()
        }
        println("sent to $phoneNumber")
        return smsManager.sendTextMessage(phoneNumber, null, message, null, null)
    }

    @JsonClass(generateAdapter = true)
    data class PhoneList(
        val id: Int?,
        val id_user: Int?,
        val to_send: Int?,
        @Json(name = "sended") val is_sended: Int?,
        val number_tel: String?,
    )
}