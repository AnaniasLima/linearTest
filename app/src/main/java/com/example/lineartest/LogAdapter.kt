package com.example.lineartest

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.lineartest.DataModel.LogMessage
import kotlinx.android.synthetic.main.list_item.view.*

class LogAdapter(val context : Context, val logMessages: List<LogMessage>) : RecyclerView.Adapter <LogAdapter.MyViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
       val view =  LayoutInflater.from(context).inflate(R.layout.list_item, parent, false)
        return MyViewHolder(view)
    }

    override fun getItemCount(): Int {
        return logMessages.size
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val logMessage = logMessages[position]
        holder.setData(logMessage, position)
    }

    inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var currentMsg: LogMessage? = null
        var currentPosition : Int = 0

        init {
            itemView.setOnClickListener {
                Toast.makeText(context, currentMsg!!.message + "Clicked.", Toast.LENGTH_SHORT).show()
            }
        }

        fun setData(logMessage : LogMessage?, pos: Int) {
            itemView.txvTitle.text = logMessage!!.message
            this.currentMsg = logMessage
            this.currentPosition = pos

        }


    }

    private fun add() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}