package com.example.call_task_fragmeny.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.call_task_fragmeny.CallManager
import com.example.call_task_fragmeny.MyApplication
import com.example.call_task_fragmeny.R
import com.example.call_task_fragmeny.Ring
import com.example.call_task_fragmeny.Session
import com.portsip.PortSipSdk

class CallFragment : Fragment() {
    var application: MyApplication? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_call, container, false)

        val mobileNumber = view.findViewById<EditText>(R.id.mobileNumber)
        val callBtn = view.findViewById<Button>(R.id.callBtn)
        val hangUpBtn = view.findViewById<Button>(R.id.hangUpBtn)
        val videoCallBtn = view.findViewById<Button>(R.id.videoCallBtn)
        val cameraBtn = view.findViewById<Button>(R.id.cameraBtn)

        application = activity?.applicationContext as MyApplication?
        val portSipSdk: PortSipSdk? = application!!.mEngine
        val currentLine = CallManager.Instance()!!.getCurrentSession()

        callBtn.setOnClickListener {

            if (application?.mEngine == null) return@setOnClickListener

            val destination = mobileNumber.text.toString()

            if (destination.isEmpty()) {
                Log.d("Invalid", "Enter valid mobile number")
                return@setOnClickListener
            }

            if (currentLine != null) {
                if (!currentLine.IsIdle()) {
                    Log.d("idle", "current line is empty")
                    return@setOnClickListener
                }
            }

            // Ensure that we have been added one audio codec at least
            if (portSipSdk != null) {
                if (portSipSdk.isAudioCodecEmpty) {
                    Log.d("Audio Codec", "Audiocodec is empty")
                    return@setOnClickListener
                }
            }

            // Usually for 3PCC need to make call without SDP
            val sessionId: Long? = portSipSdk?.call(destination,true,false)

            if (sessionId != null) {
                if (sessionId <= 0) {
                    Log.d("Fail", "Call failure")
                    return@setOnClickListener
                } else {
                    hangUpBtn.visibility = View.VISIBLE
                    videoCallBtn.visibility = View.INVISIBLE
                }
            }

            currentLine?.remote = destination
            if (sessionId != null) {
                currentLine?.sessionID = sessionId.toLong()
                currentLine!!.state = Session.CALL_STATE_FLAG.TRYING
                currentLine.hasVideo = false
                Toast.makeText(activity, "Calling", Toast.LENGTH_SHORT).show()
            }
        }

        videoCallBtn.setOnClickListener {
            if (application?.mEngine == null) return@setOnClickListener

            val destination = mobileNumber.text.toString()

            if (destination.isEmpty()) {
                Toast.makeText(activity, "Enter valid mobile number", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (currentLine != null) {
                if (!currentLine.IsIdle()) {
                    Toast.makeText(activity, "Current line is not empty", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }

            // Ensure that we have been added one audio codec at least
            if (portSipSdk != null) {
                if (portSipSdk.isAudioCodecEmpty) {
                    Toast.makeText(activity, "Audio codec is empty", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }

            // Usually for 3PCC need to make call without SDP
            val sessionId: Long? = portSipSdk?.call(destination,true,true)

            if (sessionId != null) {
                if (sessionId <= 0) {
                    Toast.makeText(activity, "Call failure", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                } else {
                    cameraBtn.visibility = View.VISIBLE
                    hangUpBtn.visibility = View.VISIBLE
                    callBtn.visibility = View.INVISIBLE
                }
            }

            currentLine?.remote = destination
            if (sessionId != null) {
                currentLine?.sessionID = sessionId.toLong()
                currentLine!!.state = Session.CALL_STATE_FLAG.TRYING
                currentLine.hasVideo = true
                Toast.makeText(activity, "Calling", Toast.LENGTH_SHORT).show()
            }
        }

        hangUpBtn.setOnClickListener {
            activity?.let { it1 -> Ring.getInstance(it1.applicationContext)?.stop() }
            when (currentLine!!.state) {
                Session.CALL_STATE_FLAG.INCOMING -> {
                    portSipSdk!!.rejectCall(currentLine.sessionID, 486)
                    Toast.makeText(activity, "Rejected Call", Toast.LENGTH_SHORT).show()
                }

                Session.CALL_STATE_FLAG.CONNECTED, Session.CALL_STATE_FLAG.TRYING -> {
                    portSipSdk!!.hangUp(currentLine.sessionID)
                    Toast.makeText(activity, "Hang Up", Toast.LENGTH_SHORT).show()
                }

                else -> {}
            }
            currentLine.Reset()
            hangUpBtn.visibility = View.INVISIBLE
            Ring().stopRingTone()
        }

        cameraBtn.setOnClickListener {
            val fragment = CameraFragment()
            val fragmentManager = activity?.supportFragmentManager
            val trans = fragmentManager?.beginTransaction()
            trans?.replace(R.id.container, fragment)
            trans?.commit()
        }

        return  view
    }

}