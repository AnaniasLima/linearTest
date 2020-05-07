package com.example.lineartest

import android.graphics.Insets.add
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.lineartest.DataModel.MessagePool
import kotlinx.android.synthetic.main.activity_log.*

class LogActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log)

        val layoutManager = LinearLayoutManager(this)
        layoutManager.orientation = LinearLayoutManager.VERTICAL

        logRecyclerView.layoutManager = layoutManager

        val adapter = LogAdapter(this, MessagePool.logMessage)
        logRecyclerView.adapter = adapter

    }
}