package com.yenaly.han1meviewer.ui.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.yenaly.han1meviewer.R

class DelayAdapter(private val ipList: List<String>) :
    RecyclerView.Adapter<DelayAdapter.DelayViewHolder>() {

    private val delayResults = mutableMapOf<String, Int>() // ip -> delay
    inner class DelayViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvIp: TextView = view.findViewById(R.id.tv_ip)
        val tvDelay: TextView = view.findViewById(R.id.tv_delay)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DelayViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_delay_result, parent, false)
        return DelayViewHolder(view)
    }

    override fun onBindViewHolder(holder: DelayViewHolder, position: Int) {
        val ip = ipList[position]
        holder.tvIp.text = ip
        val delay = delayResults[ip] ?: -1
        holder.tvDelay.text = if (delay >= 0) "$delay ms" else "Time out"

        val color = when {
            delay in 0 until 100 -> Color.GREEN
            delay in 100..500 -> Color.YELLOW
            else -> Color.RED
        }
        holder.tvDelay.setTextColor(color)
    }

    override fun getItemCount(): Int = ipList.size

    fun updateDelay(ip: String, delay: Int) {
        delayResults[ip] = delay
        val index = ipList.indexOf(ip)
        if (index >= 0) {
            notifyItemChanged(index)
        }
    }
}
