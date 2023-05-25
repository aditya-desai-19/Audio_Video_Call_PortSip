package com.example.call_task_fragmeny.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.call_task_fragmeny.CallManager
import com.example.call_task_fragmeny.MainActivity
import com.example.call_task_fragmeny.MyApplication
import com.example.call_task_fragmeny.PortMessageReceiver
import com.example.call_task_fragmeny.PortSipService
import com.example.call_task_fragmeny.R
import com.example.call_task_fragmeny.Ring
import com.example.call_task_fragmeny.Session
import com.portsip.PortSIPVideoRenderer
import com.portsip.PortSipErrorcode
import com.portsip.PortSipSdk


class CameraFragment : Fragment(), View.OnClickListener, PortMessageReceiver.BroadcastListener {
    var application: MyApplication? = null
    var activity: MainActivity? = null

    private var remoteRenderScreen: PortSIPVideoRenderer? = null
    private var localRenderScreen: PortSIPVideoRenderer? = null
    private var remoteRenderSmallScreen: PortSIPVideoRenderer? = null
    private var scalingType = PortSIPVideoRenderer.ScalingType.SCALE_ASPECT_BALANCED // SCALE_ASPECT_FIT or SCALE_ASPECT_FILL;

    private var imgSwitchCamera: ImageButton? = null
    private var imgScaleType: ImageButton? = null
    private var shareInSmall = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view =  inflater.inflate(R.layout.fragment_camera, container, false)

        activity = getActivity() as MainActivity?
        application = activity?.application as MyApplication?


        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val portSipSdk: PortSipSdk? = application!!.mEngine
        val currentLine = CallManager.Instance()!!.getCurrentSession()

        imgSwitchCamera = view.findViewById(R.id.ibcamera) as ImageButton
        imgScaleType = view.findViewById(R.id.ibscale) as ImageButton

        val hangUp = view.findViewById<Button>(R.id.hangUpBtnCam)
        hangUp.setOnClickListener {
            getActivity()?.let { it1 -> Ring.getInstance(it1)?.stop()}
            when (currentLine?.state) {
                Session.CALL_STATE_FLAG.INCOMING -> {
                    portSipSdk?.rejectCall(currentLine.sessionID, 486)
                    Toast.makeText(activity, "Rejected Call", Toast.LENGTH_SHORT).show()
                }

                Session.CALL_STATE_FLAG.CONNECTED, Session.CALL_STATE_FLAG.TRYING -> {
                    portSipSdk?.hangUp(currentLine.sessionID)
                    Toast.makeText(activity, "Hang Up", Toast.LENGTH_SHORT).show()
                }

                else -> {}
            }
            currentLine?.Reset()
        }

        imgSwitchCamera!!.setOnClickListener(this)
        imgScaleType!!.setOnClickListener(this)

        localRenderScreen = view.findViewById(R.id.local_video_view) as PortSIPVideoRenderer
        remoteRenderScreen = view.findViewById(R.id.remote_video_view) as PortSIPVideoRenderer
        remoteRenderSmallScreen = view.findViewById(R.id.share_video_view) as PortSIPVideoRenderer
        remoteRenderSmallScreen!!.setOnClickListener(this)
        scalingType = PortSIPVideoRenderer.ScalingType.SCALE_ASPECT_FIT
        remoteRenderScreen!!.setScalingType(scalingType)
        activity?.receiver?.broadcastReceiver = this
    }

    override fun onDestroyView() {
        super.onDestroyView()
        val portSipLib = application!!.mEngine
        if (localRenderScreen != null) {
            portSipLib?.displayLocalVideo(false, false, null)
            localRenderScreen!!.release()
        }

        application!!.mEngine?.let { CallManager.Instance()?.setRemoteVideoWindow(it, -1, null) } //set

        if (remoteRenderScreen != null) {
            remoteRenderScreen!!.release()
        }

        application!!.mEngine?.let { CallManager.Instance()?.setShareVideoWindow(it, -1, null) } //set

        remoteRenderSmallScreen?.release()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (hidden) {
            localRenderScreen!!.visibility = View.INVISIBLE
            remoteRenderSmallScreen?.visibility = View.INVISIBLE
            stopVideo(application!!.mEngine)
        } else {
            application!!.mEngine?.let { updateVideo(it) }
            activity?.receiver?.broadcastReceiver = this
            localRenderScreen!!.visibility = View.VISIBLE
        }
    }

    override fun onClick(v: View?) {
        val portSipLib = application?.mEngine
        when (v!!.id) {
            R.id.ibcamera -> {
                application?.mUseFrontCamera = !application?.mUseFrontCamera!!
                if (portSipLib != null) {
                    SetCamera(portSipLib, application!!.mUseFrontCamera)
                }
            }

            R.id.share_video_view -> {
                shareInSmall = !shareInSmall
                if (portSipLib != null) {
                    updateVideo(portSipLib)
                }
            }

            R.id.ibscale -> {
                scalingType =
                    if (scalingType == PortSIPVideoRenderer.ScalingType.SCALE_ASPECT_FIT) {
                        imgScaleType!!.setImageResource(R.drawable.aspect_fill)
                        PortSIPVideoRenderer.ScalingType.SCALE_ASPECT_FILL
                    } else if (scalingType == PortSIPVideoRenderer.ScalingType.SCALE_ASPECT_FILL) {
                        imgScaleType!!.setImageResource(R.drawable.aspect_balanced)
                        PortSIPVideoRenderer.ScalingType.SCALE_ASPECT_BALANCED
                    } else {
                        imgScaleType!!.setImageResource(R.drawable.aspect_fit)
                        PortSIPVideoRenderer.ScalingType.SCALE_ASPECT_FIT
                    }
                localRenderScreen!!.setScalingType(scalingType)
                remoteRenderScreen!!.setScalingType(scalingType)
                if (portSipLib != null) {
                    updateVideo(portSipLib)
                }
            }
        }
    }

    private fun SetCamera(portSipLib: PortSipSdk, userFront: Boolean) {
        if (userFront) {
            portSipLib.setVideoDeviceId(1)
        } else {
            portSipLib.setVideoDeviceId(0)
        }
    }

    private fun stopVideo(portSipLib: PortSipSdk?) {
        val cur = CallManager.Instance()!!.getCurrentSession()
        if (portSipLib != null) {
            portSipLib.displayLocalVideo(false, false, null)
            CallManager.Instance()!!.setRemoteVideoWindow(portSipLib, cur!!.sessionID, null)
            CallManager.Instance()!!.setConferenceVideoWindow(portSipLib, null)
        }
    }

    fun updateVideo(portSipLib: PortSipSdk) {
        Log.d("Update", "updated video ")
        val callManager = CallManager.Instance()
        val cur: Session? = CallManager.Instance()!!.getCurrentSession()
        if (application!!.mConference) {
            remoteRenderScreen!!.visibility = View.VISIBLE
            callManager!!.setConferenceVideoWindow(portSipLib, remoteRenderScreen)
        } else {
            if (cur != null && !cur.IsIdle() && cur.sessionID.toInt() != PortSipErrorcode.INVALID_SESSION_ID) {
                if (cur.hasVideo) {
                    localRenderScreen!!.visibility = View.VISIBLE
                    remoteRenderScreen!!.visibility = View.VISIBLE
                    if (cur.bScreenShare) {
                        remoteRenderSmallScreen?.visibility = View.VISIBLE
                        callManager!!.setRemoteVideoWindow(portSipLib, cur.sessionID, null)
                        callManager.setShareVideoWindow(portSipLib, cur.sessionID, null)
                        if (shareInSmall) {
                            callManager.setRemoteVideoWindow(
                                portSipLib,
                                cur.sessionID,
                                remoteRenderScreen
                            )
                            callManager.setShareVideoWindow(
                                portSipLib,
                                cur.sessionID,
                                remoteRenderSmallScreen
                            )
                            //callManager.se(portSipLib,cur.sessionID, remoteRenderScreen);
                        } else {
                            callManager.setRemoteVideoWindow(
                                portSipLib,
                                cur.sessionID,
                                remoteRenderSmallScreen
                            )
                            callManager.setShareVideoWindow(
                                portSipLib,
                                cur.sessionID,
                                remoteRenderScreen
                            )
                        }
                    } else {
                        remoteRenderSmallScreen?.setVisibility(View.GONE)
                        callManager!!.setShareVideoWindow(portSipLib, cur.sessionID, null)
                        callManager.setRemoteVideoWindow(
                            portSipLib,
                            cur.sessionID,
                            remoteRenderScreen
                        )
                    }
                    portSipLib.displayLocalVideo(
                        true,
                        true,
                        localRenderScreen
                    ) // display Local video
                    portSipLib.sendVideo(cur.sessionID, true)
                } else {
                    remoteRenderSmallScreen?.setVisibility(View.GONE)
                    localRenderScreen!!.visibility = View.GONE
                    remoteRenderScreen!!.visibility = View.VISIBLE
                    portSipLib.displayLocalVideo(false, false, null)
                    callManager!!.setRemoteVideoWindow(portSipLib, cur.sessionID, null)
                    if (cur.bScreenShare) {
                        callManager.setShareVideoWindow(
                            portSipLib,
                            cur.sessionID,
                            remoteRenderScreen
                        )
                    }
                }
            } else {
                remoteRenderSmallScreen?.setVisibility(View.GONE)
                portSipLib.displayLocalVideo(false, false, null)
                if (cur != null) {
                    callManager!!.setRemoteVideoWindow(portSipLib, cur.sessionID, null)
                }
            }
        }
    }


    override fun onBroadcastReceiver(intent: Intent?) {
        val action = if (intent == null) "" else intent.action!!
        if (PortSipService.CALL_CHANGE_ACTION == action) {
            val sessionId = intent!!.getLongExtra(
                PortSipService.EXTRA_CALL_SEESIONID,
                Session.INVALID_SESSION_ID.toLong()
            )
            val status = intent.getStringExtra(PortSipService.EXTRA_CALL_DESCRIPTION)
            val session = CallManager.Instance()!!.findSessionBySessionID(sessionId)
            if (session != null) {
                when (session.state) {
                    Session.CALL_STATE_FLAG.INCOMING -> {}
                    Session.CALL_STATE_FLAG.TRYING -> {}
                    Session.CALL_STATE_FLAG.CONNECTED, Session.CALL_STATE_FLAG.FAILED, Session.CALL_STATE_FLAG.CLOSED -> updateVideo(application!!.mEngine!!)
                    else -> {}
                }
            }
        }
    }
}