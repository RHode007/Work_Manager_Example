package com.avtomagen.avtoSMS

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.room.Room
import androidx.work.*
import com.avtomagen.avtoSMS.SaveApiWorker.Companion.KEY_API_WORKER_RESULT
import com.avtomagen.avtoSMS.databinding.ActivityMainBinding
import com.avtomagen.avtoSMS.databinding.DebugBinding
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var bindingMainActivity: ActivityMainBinding

    private val SMS_PERMISSION_CODE = 100

    companion object {
        const val KEY_API_INPUT_USER_TEXT = "key.api_input_user.text"
        const val KEY_SMS_NOTIFICATION_TITLE = "key.sms_notification.title"
        const val KEY_SMS_NOTIFICATION_DESC = "key.sms_notification.desc"
        const val KEY_UPLOAD_TITLE = "key.upload.title"
        const val KEY_UPLOAD_DESC = "key.upload.desc"
        const val KEY_API_INPUT_MODE_TEXT = "key.api_input_mode.text"
        const val TAG_SEND_SMS = "tag.send.sms"
        const val CHANNEL_ID = "4747"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //val binding: ActivityMainBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        bindingMainActivity = DataBindingUtil.setContentView(this, R.layout.activity_main)

        createNotificationChannel()

        bindingMainActivity.buttonSaveApi.setOnClickListener {
            val userText = bindingMainActivity.editTextUserComment.text?.toString() ?: ""
            if (userText == "1234") enableDebugAccess(0)
            else setApiKey(userText)
        }
        getApikey()

        val list = WorkManager.getInstance(this).getWorkInfosByTag(TAG_SEND_SMS).get().filter {
            it.state == WorkInfo.State.ENQUEUED
        }
        when (list.size){
            0 -> bindingMainActivity.startDownloadWorker.isChecked = false
            1 -> {
                bindingMainActivity.startDownloadWorker.isChecked = true

            }
            else -> {
                cancelSendingSms()
                bindingMainActivity.startDownloadWorker.isChecked = false
            }
        }
        
        bindingMainActivity.startDownloadWorker.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                setPeriodicallySendingSms()
            } else {
                cancelSendingSms()
            }
        }
        //Check for debug button visibility
        checkDebugAccess(0)

        // Check for SMS permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
            != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted, request it
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.SEND_SMS), SMS_PERMISSION_CODE)
        }

        // Check for ignore battery consumption
        permissionBattery()
    }

    private fun getApikey(){
        val workManager = WorkManager.getInstance(this)

        val data = Data.Builder()
            .putString(KEY_API_INPUT_MODE_TEXT, "getApiStatus")
            .build()
        val saveApiWorker = OneTimeWorkRequestBuilder<SaveApiWorker>()
            .setInputData(data)
            .build()

        workManager.enqueue(saveApiWorker)

        workManager.getWorkInfoByIdLiveData(saveApiWorker.id)
            .observe(this) { workInfo ->
                if (workInfo.state == WorkInfo.State.SUCCEEDED) {
                    bindingMainActivity.textInputLayout.hint = workInfo.outputData.getString(KEY_API_WORKER_RESULT)
                }
            }
    }

    private fun setApiKey(userText: String) {
        val workManager = WorkManager.getInstance(this)

        val data = Data.Builder()
            .putString(KEY_API_INPUT_MODE_TEXT, "setApi")
            .putString(KEY_API_INPUT_USER_TEXT, userText)
            .build()
        val saveApiWorker = OneTimeWorkRequestBuilder<SaveApiWorker>()
            .setInputData(data)
            .build()

        workManager.enqueue(saveApiWorker)

        workManager.getWorkInfoByIdLiveData(saveApiWorker.id)
            .observe(this) { workInfo ->
                if (workInfo.state == WorkInfo.State.SUCCEEDED) {
                    bindingMainActivity.textViewWorkState.text = "Ключ установлен"
                    bindingMainActivity.textInputLayout.hint = workInfo.outputData.getString(KEY_API_WORKER_RESULT)
                }
            }
    }

    private fun enableDebugAccess(id: Int) {
        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "AvtoDB"
        ).allowMainThreadQueries().build()
        db.userDao().updateDebugAccess(id, 1)
        bindingMainActivity.buttonDebug.visibility = View.VISIBLE
        db.close()
    }

    private fun checkDebugAccess(id: Int) {
        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "AvtoDB"
        ).allowMainThreadQueries().build()
        val user = db.userDao().getById(0)
        if(user.debugAccess == 1) bindingMainActivity.buttonDebug.visibility = View.VISIBLE
        db.close()
    }

    fun setPeriodicallySendingSms() {
        val workManager = WorkManager.getInstance(this)
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.UNMETERED)
            .setRequiresBatteryNotLow(true)
            .build()
        val downloadWorker = PeriodicWorkRequestBuilder<DownloadWorker>(15, TimeUnit.MINUTES)
            .addTag(TAG_SEND_SMS)
            .setConstraints(constraints)
            .build()

        workManager.enqueue(downloadWorker)

        workManager.getWorkInfoByIdLiveData(downloadWorker.id)
            .observe(this) { workInfo ->
                when (workInfo.state.name){
                    "ENQUEUED" -> bindingMainActivity.textViewWorkState.text = "Работает"
                    "FAILED" -> bindingMainActivity.textViewWorkState.text = "Ошибка"
                    "CANCELLED" -> bindingMainActivity.textViewWorkState.text = "Отключено"
                    "BLOCKED" -> bindingMainActivity.textViewWorkState.text = "Отложено?"
                    else -> {
                        bindingMainActivity.textViewWorkState.text = workInfo.state.name
                    }
                }

                if (workInfo.state == WorkInfo.State.SUCCEEDED) {
                    val apiWorkerResult = workInfo.outputData.getString(KEY_API_WORKER_RESULT)
                    Toast.makeText(this, apiWorkerResult, Toast.LENGTH_SHORT).show()
                }
            }
    }

    fun goToDebug(view: View?) {
        val intent = Intent(this, DebugActivity::class.java)
        startActivity(intent)
    }

    fun cancelSendingSms() {
        WorkManager.getInstance(this).cancelAllWorkByTag(TAG_SEND_SMS)
    }


    @SuppressLint("BatteryLife", "ObsoleteSdkInt")
    private fun permissionBattery() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val packageName: String = baseContext.packageName
            val pm: PowerManager =
                baseContext.getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                val intent = Intent()
                intent.action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                intent.data = Uri.parse("package:$packageName")
                baseContext.startActivity(intent)
            }
        }
    }

    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.channel_name)
            val descriptionText = getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)

        }
    }

    /*private fun setOneTimeGoldenRatioFilterRequest() {

        val workManager = WorkManager.getInstance(this)

        val downloadData = workDataOf(
            KEY_DOWNLOAD_TITLE to "Download Worker",
            KEY_DOWNLOAD_DESC to "Downlading Image is successfully"
        )

        val uploadData = workDataOf(
            KEY_UPLOAD_TITLE to "Upload Worker",
            KEY_UPLOAD_DESC to "Uploading Image is successfully"
        )

        val constraintUpload = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresCharging(true)
            .build()

        val constraintDownload = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.UNMETERED)
            .setRequiresStorageNotLow(true)
            .setRequiresBatteryNotLow(true)
            .build()

        val compressImage = OneTimeWorkRequestBuilder<CompressWorker>()
            .build()

        val filterImage = OneTimeWorkRequestBuilder<FilterWorker>()
            .build()

        val uploadImage = OneTimeWorkRequestBuilder<UploadWorker>()
            .setConstraints(constraintUpload)
            .setInputData(uploadData)
            .build()

        val downloadImage = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setConstraints(constraintDownload)
            .setInputData(downloadData)
            .setInitialDelay(3, TimeUnit.SECONDS)
            .build()

        val parallelWork = mutableListOf<OneTimeWorkRequest>()
        parallelWork.add(compressImage)
        parallelWork.add(filterImage)

        workManager
            .beginWith(parallelWork)
            .then(uploadImage)
            .then(downloadImage)
            .enqueue()

        workManager.getWorkInfoByIdLiveData(downloadImage.id)
            .observe(this) { workInfoDownload ->
                binding.textViewWorkState.text = workInfoDownload.state.name
            }
    }*/
}