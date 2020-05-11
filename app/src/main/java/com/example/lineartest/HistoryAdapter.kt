package com.example.lineartest

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.list_item.view.*


class HistoryAdapter(private val context: Context, val list: ArrayList<String>): RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {
    var contaId=0
    override fun onCreateViewHolder(viewGroup: ViewGroup, position: Int): ViewHolder {
        val view: View = LayoutInflater.from(context).inflate(R.layout.list_item, viewGroup, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return list.count()
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        viewHolder.bind(list[position])
    }

    inner class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        var id=contaId++
        fun bind(myItem:String) {
            itemView.tv_title.text = myItem
        }

    }
}