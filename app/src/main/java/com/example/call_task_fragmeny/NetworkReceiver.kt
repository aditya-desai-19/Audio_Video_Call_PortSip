package com.example.call_task_fragmeny

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Build

class NetworkReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        try {
            if (intent.action == ConnectivityManager.CONNECTIVITY_ACTION && null != context) {
                val netWorkState = getNetWorkState(context)
                // 当网络发生变化，判断当前网络状态，并通过NetEvent回调当前网络状态
                if (mListener != null) {
                    mListener!!.onNetworkChange(netWorkState)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // 自定义接口
    interface NetWorkListener {
        fun onNetworkChange(netMobile: Int)
    }

    private fun getNetWorkState(context: Context): Int {
        // 得到连接管理器对象
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            val connectivityManager = context
                .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetworkInfo = connectivityManager
                .activeNetworkInfo
            if (activeNetworkInfo != null && activeNetworkInfo.isConnected) {
                if (activeNetworkInfo.type == ConnectivityManager.TYPE_WIFI) {
                    return ConnectivityManager.TYPE_WIFI
                } else if (activeNetworkInfo.type == ConnectivityManager.TYPE_WIFI) {
                    return ConnectivityManager.TYPE_WIFI
                }
            } else {
                return -1
            }
        } else {
            val connMgr =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

            //获取所有网络连接的信息
            val networks = connMgr.allNetworks
            //通过循环将网络信息逐个取出来
            for (i in networks.indices) {
                //获取ConnectivityManager对象对应的NetworkInfo对象
                val networkInfo = connMgr.getNetworkInfo(networks[i])
                if (networkInfo!!.isConnected) {
                    return if (networkInfo.type == ConnectivityManager.TYPE_MOBILE) {
                        ConnectivityManager.TYPE_MOBILE
                    } else {
                        ConnectivityManager.TYPE_WIFI
                    }
                }
            }
        }
        return -1
    }

    private var mListener: NetWorkListener? = null
    fun setListener(listener: NetWorkListener?) {
        mListener = listener
    }

}

