package com.example.call_task_fragmeny.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat.startForegroundService
import com.example.call_task_fragmeny.PortMessageReceiver
import com.example.call_task_fragmeny.PortSipService
import com.example.call_task_fragmeny.R
import kotlin.system.exitProcess


class LoginFragment : Fragment() {
    var receiver: PortMessageReceiver? = null

    private val REQ_DANGERS_PERMISSION = 2

    private var etUsername: EditText? = null
    private var etPassword: EditText? = null
    private var etSipServer: EditText? = null
    private var etSipServerPort: EditText? = null
    private var transport = 0

    private var mPreferences: SharedPreferences? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_login, container, false)

        receiver = PortMessageReceiver()

        val filter = IntentFilter()
        filter.addAction(PortSipService.REGISTER_CHANGE_ACTION)
        filter.addAction(PortSipService.CALL_CHANGE_ACTION)
        filter.addAction(PortSipService.PRESENCE_CHANGE_ACTION)
        filter.addAction(PortSipService.ACTION_SIP_AUDIODEVICE)
        activity?.registerReceiver(receiver, filter)


        mPreferences = PreferenceManager.getDefaultSharedPreferences(activity)

        etUsername = view.findViewById(R.id.etusername)
        etPassword = view.findViewById(R.id.etpwd)
        etSipServer = view.findViewById(R.id.etsipsrv)
        etSipServerPort = view.findViewById(R.id.etsipport)

        LoadUserInfo()

        val loginbtn = view.findViewById<Button>(R.id.register)
        loginbtn.setOnClickListener {
            SaveUserInfo()
            val onLineIntent = Intent(activity, PortSipService::class.java)
            onLineIntent.action = PortSipService.ACTION_SIP_REGIEST
            activity?.startForegroundService(onLineIntent)
            Toast.makeText(activity, "Registering...", Toast.LENGTH_SHORT).show()
        }

        val callNav = view.findViewById<Button>(R.id.callNav)
        callNav.setOnClickListener {
            Toast.makeText(activity, "Redirecting to next fragment", Toast.LENGTH_SHORT).show()
            val newfragment = CallFragment()
            val fragmentManager = activity?.supportFragmentManager
            val trans = fragmentManager?.beginTransaction()
            trans?.replace(R.id.container, newfragment)
            trans?.commit()
        }

        return  view
    }

    override fun onResume() {
        super.onResume()
        requestPermissions(activity)
    }

    override fun onDestroy() {
        super.onDestroy()
        activity?.unregisterReceiver(receiver)
    }

    fun requestPermissions(activity: Activity?) {
        // Check if we have write permission
        if (PackageManager.PERMISSION_GRANTED != ActivityCompat.checkSelfPermission(
                requireActivity(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) || PackageManager.PERMISSION_GRANTED != activity?.let {
                ActivityCompat.checkSelfPermission(
                    it, Manifest.permission.CAMERA
                )
            } || PackageManager.PERMISSION_GRANTED != ActivityCompat.checkSelfPermission(
                activity, Manifest.permission.RECORD_AUDIO
            )
        ) {
            if (activity != null) {
                ActivityCompat.requestPermissions(
                    activity, arrayOf<String>(
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO
                    ),
                    REQ_DANGERS_PERMISSION
                )
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQ_DANGERS_PERMISSION -> {
                var i = 0
                for (result in grantResults) {
                    if (result != PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(
                            activity,
                            "you must grant the permission " + permissions[i],
                            Toast.LENGTH_SHORT
                        ).show()
                        i++
                        activity?.stopService(Intent(activity, PortSipService::class.java))
                        exitProcess(0)
                    }
                }
            }
        }
    }

    private fun LoadUserInfo() {
        etUsername!!.setText(mPreferences!!.getString(PortSipService().USER_NAME, null))
        etPassword!!.setText(mPreferences!!.getString(PortSipService().USER_PWD, null))
        etSipServer!!.setText(mPreferences!!.getString(PortSipService().SVR_HOST, null))
        etSipServerPort!!.setText(mPreferences!!.getString(PortSipService().SVR_PORT, "5060"))
    }

    private fun SaveUserInfo() {
        val editor = mPreferences!!.edit()
        editor.putString(PortSipService().USER_NAME, etUsername!!.text.toString())
        editor.putString(PortSipService().USER_PWD, etPassword!!.text.toString())
        editor.putString(PortSipService().SVR_HOST, etSipServer!!.text.toString())
        editor.putString(PortSipService().SVR_PORT, etSipServerPort!!.text.toString())
        editor.commit()
    }

}