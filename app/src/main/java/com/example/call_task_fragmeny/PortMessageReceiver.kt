package com.example.call_task_fragmeny


import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent


class PortMessageReceiver : BroadcastReceiver() {
    interface BroadcastListener {
        fun onBroadcastReceiver(intent: Intent?)
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (broadcastReceiver != null) {
            broadcastReceiver!!.onBroadcastReceiver(intent)
        }
    }

    var broadcastReceiver: BroadcastListener? = null
}


