package com.example.lineartest

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.list_item.view.*


class LogAdapter(private val context: Context, val myList: ArrayList<String>): RecyclerView.Adapter<LogAdapter.ViewHolder>() {
    var contaId=0
    override fun onCreateViewHolder(viewGroup: ViewGroup, position: Int): ViewHolder {
        val view: View = LayoutInflater.from(context).inflate(R.layout.list_item, viewGroup, false)
//        println("onCreateViewHolder position = $position")
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
//        println("getItemCount myList.count = ${myList.count()}")
        return myList.count()
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
//        println("onBindViewHolder position = $position - ${myList[position]}  id:${viewHolder.id}")
        viewHolder.bind(myList[position])
    }

    inner class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        var id=contaId++
        fun bind(myItem:String) {
//            println("bind myItem = $myItem")
            itemView.tv_title.text = myItem
        }

    }
}