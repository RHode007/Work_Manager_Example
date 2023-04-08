package com.avtomagen.avtoSMS

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil.setContentView
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.room.Room
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.avtomagen.avtoSMS.databinding.ActivityMainBinding
import com.avtomagen.avtoSMS.databinding.DebugBinding


class DebugActivity : AppCompatActivity(){
    private lateinit var bindingMainActivity: ActivityMainBinding
    private lateinit var bindingDebugActivity: DebugBinding
    private lateinit var loggerViewModel: LoggerViewModel
    private var adapterRecyclerView = LoggerViewAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindingDebugActivity = setContentView(this, R.layout.debug)

        setLogger()

        bindingDebugActivity.buttonUpdateLog.setOnClickListener {
            updateLogger()
        }
        bindingDebugActivity.buttonClearDB.setOnClickListener {
            clearLogger()
        }
        val user: List<User> = getUser(0)
        user.forEach{item ->
            bindingDebugActivity.startLogger.isChecked = item.logging == 1
        }

        bindingDebugActivity.startLogger.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                enableLogger(0)
            } else {
                disableLogger(0)
            }
        }
    }

    fun goToMain(view: View?) {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }

    private fun getUser(id: Int): List<User> {
        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "AvtoDB"
        ).allowMainThreadQueries().build()
        val user = db.userDao().getAll()
        db.close()
        return user
    }

    private fun enableLogger(id: Int) {
        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "AvtoDB"
        ).allowMainThreadQueries().build()
        db.userDao().updateLogging(id, 1)
        db.close()
    }

    private fun disableLogger(id: Int) {
        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "AvtoDB"
        ).allowMainThreadQueries().build()
        db.userDao().updateLogging(id, 0)
        db.close()
    }

    private fun setLogger(){
        val recyclerView = bindingDebugActivity.recyclerView
        setContentView(bindingDebugActivity.root)
        val layoutManager = LinearLayoutManager(this)
        layoutManager.reverseLayout = true
        layoutManager.stackFromEnd = true
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = adapterRecyclerView

        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "AvtoDB"
        ).allowMainThreadQueries().build()
        val loggerDao = db.loggerDao()
        loggerViewModel = ViewModelProvider(this, UserViewModelFactory(loggerDao))[LoggerViewModel::class.java]
        loggerViewModel.allLogger.observe(this) { log ->
            adapterRecyclerView.setLog(log)
        }
    }

    private fun updateLogger(){
        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "AvtoDB"
        ).allowMainThreadQueries().build()
        val loggerDao = db.loggerDao()
        loggerDao.getAll().observe(this) { updatedLog ->
            adapterRecyclerView.setLog(updatedLog)
        }
    }

    private fun clearLogger(){
        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "AvtoDB"
        ).allowMainThreadQueries().build()
        val loggerDao = db.loggerDao()
        loggerDao.deleteAll()
        loggerDao.getAll().observe(this) { updatedLog ->
            adapterRecyclerView.setLog(updatedLog)
        }
    }

}