package com.avtomagen.avtoSMS

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class LoggerViewModel(loggerDao: LoggerDao) : ViewModel() {
    val allLogger: LiveData<List<Logger>> = loggerDao.getAll()
}

class UserViewModelFactory(private val loggerDao: LoggerDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoggerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LoggerViewModel(loggerDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}