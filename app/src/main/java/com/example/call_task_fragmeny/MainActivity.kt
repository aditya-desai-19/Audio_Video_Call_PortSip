package com.example.call_task_fragmeny

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.call_task_fragmeny.ui.LoginFragment

class MainActivity : AppCompatActivity() {
    var receiver: PortMessageReceiver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val fragment = LoginFragment()
        val fragmentManager = supportFragmentManager
        val trans = fragmentManager.beginTransaction()
        trans.add(R.id.container, fragment)
        trans.commit()
    }
}