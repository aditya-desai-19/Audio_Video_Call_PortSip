package com.example.call_task_fragmeny


public class Session {
    var bScreenShare: Boolean = false
    var bEarlyMedia = true
    var lineName: String? = null
    var state: CALL_STATE_FLAG? = CALL_STATE_FLAG.CLOSED
    var sessionID: Long = 0
    var remote: String? = null
    var hasVideo = false

    companion object {
        var INVALID_SESSION_ID = -1
        var displayName: String? = null
        var bScreenShare = false
        var bHold = false
        var bMute = false

    }


    fun IsIdle(): Boolean {
        return state == CALL_STATE_FLAG.FAILED || state == CALL_STATE_FLAG.CLOSED
    }

    public fun Session() {
        remote = null
        displayName = null
        hasVideo = false
        sessionID = INVALID_SESSION_ID.toLong()
        state = CALL_STATE_FLAG.CLOSED
    }

    fun Reset() {
        remote = null
        displayName = null
        hasVideo = false
        bScreenShare = false
        sessionID = INVALID_SESSION_ID.toLong()
        state = CALL_STATE_FLAG.CLOSED
        bEarlyMedia = false
    }

    enum class CALL_STATE_FLAG {
        INCOMING, TRYING, CONNECTED, FAILED, CLOSED
    }
}