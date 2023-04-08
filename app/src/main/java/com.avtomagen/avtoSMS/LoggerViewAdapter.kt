package com.avtomagen.avtoSMS

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class LoggerViewAdapter : RecyclerView.Adapter<LoggerViewAdapter.LoggerViewHolder>() {

    private var logger = emptyList<Logger>()

    inner class LoggerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val text: TextView = itemView.findViewById(R.id.textView)
        //val personAge: TextView = itemView.findViewById(R.id.person_age)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LoggerViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.person_item, parent, false)
        return LoggerViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: LoggerViewHolder, position: Int) {
        val currentLogger = logger[position]
        holder.text.text = currentLogger.text
        //holder.personAge.text = currentPerson.age.toString()
    }

    override fun getItemCount(): Int {
        return logger.size
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setLog(logger: List<Logger>) {
        this.logger = logger
        notifyDataSetChanged()
    }
}
