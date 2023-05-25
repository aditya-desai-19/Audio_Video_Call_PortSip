package com.example.call_task_fragmeny


import com.portsip.PortSIPVideoRenderer
import com.portsip.PortSipEnumDefine
import com.portsip.PortSipSdk


class CallManager {
    val maxLines = 10
    lateinit var sessions: Array<Session>
    var CurrentLine = 0
    public var regist = false
    var online = false
    var speakerOn = false
    var currentAudioDevcie = PortSipEnumDefine.AudioDevice.NONE
    var audioDeviceAvailable: MutableList<PortSipEnumDefine.AudioDevice> = ArrayList()

    init {
        CurrentLine = 0
        sessions = Array(maxLines) {Session()}
        for (i in sessions.indices) {
            sessions[i] = Session()
            sessions[i].lineName = "line - $i"
        }
    }

    companion object {
        private var mInstance: CallManager? = null
        private val locker = Any()
        fun Instance(): CallManager? {
            if (mInstance == null) {
                synchronized(locker) {
                    if (mInstance == null) {
                        mInstance = CallManager()
                    }
                }
            }
            return mInstance
        }
    }


    fun setSelectalbeAudioDevice(
        current: PortSipEnumDefine.AudioDevice,
        devices: Set<PortSipEnumDefine.AudioDevice>?
    ) {
        audioDeviceAvailable.clear()
        audioDeviceAvailable.addAll(devices!!)
        currentAudioDevcie = current
    }

//    fun getSelectalbeAudioDevice(): Set<PortSipEnumDefine.AudioDevice?>? {
//        val seletable: HashSet<*> = HashSet<PortSipEnumDefine.AudioDevice>()
//        seletable.addAll(audioDeviceAvailable)
//        return seletable
//    }

    fun setAudiodevice(portSipSdk: PortSipSdk, audioDevice: PortSipEnumDefine.AudioDevice) {
        currentAudioDevcie = audioDevice
        portSipSdk.setAudioDevice(currentAudioDevcie)
    }

    fun getCurrentAudioDevice(): PortSipEnumDefine.AudioDevice? {
        return currentAudioDevcie
    }


    fun hangupAllCalls(sdk: PortSipSdk) {
        for (session in sessions) {
            if (session.sessionID > Session.INVALID_SESSION_ID) {
                sdk.hangUp(session.sessionID)
            }
        }
    }

    fun hasActiveSession(): Boolean {
        for (session in sessions) {
            if (session.sessionID > Session.INVALID_SESSION_ID) {
                return true
            }
        }
        return false
    }

    fun findSessionBySessionID(SessionID: Long): Session? {
        for (session in sessions) {
            if (session.sessionID === SessionID) {
                return session
            }
        }
        return null
    }

    fun findIdleSession(): Session? {
        for (session in sessions) {
            if (session.IsIdle()) {
                session.Reset()
                return session
            }
        }
        return null
    }

    fun getCurrentSession(): Session? {
        return if (CurrentLine >= 0 && CurrentLine <= sessions.size) {
            sessions[CurrentLine]
        } else null
    }

    fun findSessionByIndex(index: Int): Session? {
        return if (index >= 0 && index <= sessions.size) {
            sessions[index]
        } else null
    }

    fun addActiveSessionToConfrence(sdk: PortSipSdk) {
        for (session in sessions) {
            if (session.state === Session.CALL_STATE_FLAG.CONNECTED) {
                sdk.setRemoteScreenWindow(session.sessionID, null)
                sdk.setRemoteVideoWindow(session.sessionID, null)
                sdk.joinToConference(session.sessionID)
                sdk.sendVideo(session.sessionID, true)
                sdk.unHold(session.sessionID)
            }
        }
    }

    fun setRemoteVideoWindow(sdk: PortSipSdk, sessionid: Long, renderer: PortSIPVideoRenderer?) {
        sdk.setConferenceVideoWindow(null)
        for (session in sessions) {
            if (session.state === Session.CALL_STATE_FLAG.CONNECTED && sessionid != session.sessionID) {
                sdk.setRemoteVideoWindow(session.sessionID, null)
            }
        }
        sdk.setRemoteVideoWindow(sessionid, renderer)
    }

    fun setShareVideoWindow(sdk: PortSipSdk, sessionid: Long, renderer: PortSIPVideoRenderer?) {
        sdk.setConferenceVideoWindow(null)
        for (session in sessions) {
            if (session.state === Session.CALL_STATE_FLAG.CONNECTED && sessionid != session.sessionID) {
                sdk.setRemoteScreenWindow(session.sessionID, null)
            }
        }
        sdk.setRemoteScreenWindow(sessionid, renderer)
    }


    fun setConferenceVideoWindow(sdk: PortSipSdk, renderer: PortSIPVideoRenderer?) {
        for (session in sessions) {
            if (session.state === Session.CALL_STATE_FLAG.CONNECTED) {
                sdk.setRemoteVideoWindow(session.sessionID, null)
                sdk.setRemoteScreenWindow(session.sessionID, null)
            }
        }
        sdk.setConferenceVideoWindow(renderer)
    }

    fun resetAll() {
        for (session in sessions) {
            session.Reset()
        }
    }

    fun findIncomingCall(): Session? {
        for (session in sessions) {
            if (session.sessionID != Session.INVALID_SESSION_ID.toLong() && session.state === Session.CALL_STATE_FLAG.INCOMING) {
                return session
            }
        }
        return null
    }
}


