package com.example.call_task_fragmeny

import android.app.Application
import com.portsip.PortSipSdk


class MyApplication: Application() {
    var mConference = false
    var mUseFrontCamera = false
    var mEngine: PortSipSdk? = null

    override fun onCreate() {
        super.onCreate()
        mEngine = PortSipSdk()
    }
}