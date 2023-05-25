package com.example.call_task_fragmeny

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.Build
import android.os.IBinder
import android.preference.PreferenceManager
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.portsip.OnPortSIPEvent
import com.portsip.PortSipEnumDefine
import com.portsip.PortSipErrorcode
import com.portsip.PortSipSdk
import java.util.Random
import java.util.UUID


class PortSipService: Service() , OnPortSIPEvent, NetworkReceiver.NetWorkListener {

    private val channelID = "PortSipService"

    private val APPID = "com.portsip.sipsample"

    val REGISTER_CHANGE_ACTION = "PortSip.AndroidSample.Test.RegisterStatusChagnge"

    val CALL_CHANGE_ACTION = "PortSip.AndroidSample.Test.CallStatusChagnge"

    val PRESENCE_CHANGE_ACTION = "PortSip.AndroidSample.Test.PRESENCEStatusChagnge"

    val ACTION_SIP_AUDIODEVICE = "PortSip.AndroidSample.Test.AudioDeviceUpdate"

    val ACTION_SIP_REGIEST = "PortSip.AndroidSample.Test.REGIEST"

    val ACTION_SIP_UNREGIEST = "PortSip.AndroidSample.Test.UNREGIEST"

    val ACTION_SIP_REINIT = "PortSip.AndroidSample.Test.TrnsType"

    val ACTION_PUSH_TOKEN = "PortSip.AndroidSample.Test.PushToken"


    val EXTRA_PUSHTOKEN = "token"

    private var mNetWorkReceiver: NetworkReceiver? = null


    val USER_NAME = "user name"
    val USER_PWD = "user pwd"
    val SVR_HOST = "svr host"
    val SVR_PORT = "svr port"

    val USER_DOMAIN = "user domain"
    val USER_DISPALYNAME = "user dispalay"
    val USER_AUTHNAME = "user authname"
    val STUN_HOST = "stun host"
    val STUN_PORT = "stun port"

    val TRANS = "trans type"
    val SRTP = "srtp type"

    private var applicaton: MyApplication? = null

    private var mEngine: PortSipSdk? = null

    val INSTANCE_ID = "instanceid"

    private var pushToken: String? = null

    val channelId = "MyChannel"

    private val mNotificationManager: NotificationManager? = null


    companion object {

        private val SERVICE_NOTIFICATION = 31414

        val PENDINGCALL_NOTIFICATION: Int = PortSipService.SERVICE_NOTIFICATION + 1

        val REGISTER_CHANGE_ACTION = "PortSip.AndroidSample.Test.RegisterStatusChagnge"

        val CALL_CHANGE_ACTION = "PortSip.AndroidSample.Test.CallStatusChagnge"

        val PRESENCE_CHANGE_ACTION = "PortSip.AndroidSample.Test.PRESENCEStatusChagnge"

        val ACTION_SIP_AUDIODEVICE = "PortSip.AndroidSample.Test.AudioDeviceUpdate"

        val ACTION_SIP_REGIEST = "PortSip.AndroidSample.Test.REGIEST"

        val EXTRA_CALL_SEESIONID = "SessionID"

        val EXTRA_CALL_DESCRIPTION = "Description"

        val notificationId = 1

    }

    override fun onCreate() {
        super.onCreate()
        applicaton = applicationContext as MyApplication?
        mEngine = applicaton!!.mEngine

        initialSDK()

        registerReceiver()

    }

    private fun initialSDK() {
        mEngine!!.CreateCallManager(applicaton)
        mEngine!!.setOnPortSIPEvent(this)
        val dataPath = getExternalFilesDir(null)!!.absolutePath
        val certRoot = "$dataPath/certs"
        val rm = Random()
        val localPort = 5060 + rm.nextInt(60000)
        val transType = 0
        var result = mEngine!!.initialize(
            getTransType(transType), "0.0.0.0", localPort,
            PortSipEnumDefine.ENUM_LOG_LEVEL_DEBUG, dataPath,
            8, "PortSIP SDK for Android", 0, 0, certRoot, "", false, null
        )
        if (result != PortSipErrorcode.ECoreErrorNone) {
            print("initialize failure ErrorCode = $result")
            CallManager.Instance()!!.resetAll()
            return
        }
        result = mEngine!!.setLicenseKey("LicenseKey")
        if (result == PortSipErrorcode.ECoreWrongLicenseKey) {
            print("The wrong license key was detected, please check with sales@portsip.com or support@portsip.com")
            return
        } else if (result == PortSipErrorcode.ECoreTrialVersionLicenseKey) {
            Log.w(
                "Trial Version",
                "This trial version SDK just allows short conversation, you can't hearing anything after 2-3 minutes, contact us: sales@portsip.com to buy official version."
            )
            print("This Is Trial Version")
        }
        mEngine!!.setInstanceId(getInstanceID())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification: Notification = addNotification()
        startForeground(notificationId, notification)
        val result = super.onStartCommand(intent, flags, startId)
        if (intent != null) {
            if (ACTION_SIP_REGIEST == intent.action) {
                CallManager.Instance()!!.online = true
                if (CallManager.Instance()!!.regist) {
                    mEngine!!.refreshRegistration(0)
                } else {
                    registerToServer()
                }
            } else if (ACTION_SIP_UNREGIEST == intent.action) {
                CallManager.Instance()!!.online = false
                unregisterToServer()
            } else if (ACTION_SIP_REINIT == intent.action) {
                CallManager.Instance()!!.hangupAllCalls(mEngine!!)
                initialSDK()
            } else if (ACTION_PUSH_TOKEN == intent.action) {
                pushToken = intent.getStringExtra(EXTRA_PUSHTOKEN)
                refreshPushToken()
            }
        }
        return result
    }

    fun addNotification(): Notification {
        createNotificationChannel()

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.call)
            .setContentTitle("PortSip Running")
            .setContentText("The Service is running")

        return builder.build()
    }

    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                channelId,
                "PortSip Running",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            manager.createNotificationChannel(channel)
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver()
    }

    fun registerToServer() {
        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        val rm = Random()
        val srtpType = preferences.getInt(SRTP, 0)
        val userName = preferences.getString(USER_NAME, "")
        val password = preferences.getString(USER_PWD, "")
        val displayName = preferences.getString(USER_DISPALYNAME, "")
        val authName = preferences.getString(USER_AUTHNAME, "")
        val userDomain = preferences.getString(USER_DOMAIN, "")
        val sipServer = preferences.getString(SVR_HOST, "")
        val serverPort = preferences.getString(SVR_PORT, "5060")
        val stunServer = preferences.getString(STUN_HOST, "")
        val stunPort = preferences.getString(STUN_PORT, "3478")
        val sipServerPort = serverPort!!.toInt()
        val stunServerPort = stunPort!!.toInt()
        if (TextUtils.isEmpty(userName)) {
            print("Please enter user name!")
            return
        }
        if (TextUtils.isEmpty(password)) {
            print("Please enter password!")
            return
        }
        if (TextUtils.isEmpty(sipServer)) {
            print("Please enter SIP Server!")
            return
        }
        if (TextUtils.isEmpty(serverPort)) {
            print("Please enter Server Port!")
            return
        }
        mEngine!!.removeUser()
        var result = mEngine!!.setUser(
            userName, displayName, authName, password,
            userDomain, sipServer, sipServerPort, stunServer, stunServerPort, null, 5060
        )
        if (result != PortSipErrorcode.ECoreErrorNone) {
            print("setUser failure ErrorCode = $result")
            CallManager.Instance()!!.resetAll()
            return
        }
        mEngine!!.enableAudioManager(true)
        mEngine!!.setAudioDevice(PortSipEnumDefine.AudioDevice.SPEAKER_PHONE)
        mEngine!!.setVideoDeviceId(1)
        mEngine!!.setSrtpPolicy(srtpType)
        ConfigPreferences(this, preferences, mEngine!!)
        mEngine!!.enable3GppTags(false)
        if (!TextUtils.isEmpty(pushToken)) {
            val pushMessage =
                "device-os=android;device-uid=$pushToken;allow-call-push=true;allow-message-push=true;app-id=$APPID"
            mEngine!!.addSipMessageHeader(-1, "REGISTER", 1, "portsip-push", pushMessage)
            //new version
            mEngine!!.addSipMessageHeader(-1, "REGISTER", 1, "x-p-push", pushMessage)
        }
        result = mEngine!!.registerServer(90, 0)
        if (result != PortSipErrorcode.ECoreErrorNone) {
            print("registerServer failure ErrorCode =$result")
            mEngine!!.unRegisterServer()
            CallManager.Instance()!!.resetAll()
        }
    }

    fun ConfigPreferences(context: Context, preferences: SharedPreferences, sdk: PortSipSdk) {
        sdk.clearAudioCodec()
        if (preferences.getBoolean(context.getString(R.string.MEDIA_G722), false)) {
            sdk.addAudioCodec(PortSipEnumDefine.ENUM_AUDIOCODEC_G722)
        }
        if (preferences.getBoolean(context.getString(R.string.MEDIA_PCMA), true)) {
            sdk.addAudioCodec(PortSipEnumDefine.ENUM_AUDIOCODEC_PCMA)
        }
        if (preferences.getBoolean(context.getString(R.string.MEDIA_PCMU), true)) {
            sdk.addAudioCodec(PortSipEnumDefine.ENUM_AUDIOCODEC_PCMU)
        }
        if (preferences.getBoolean(context.getString(R.string.MEDIA_G729), true)) {
            sdk.addAudioCodec(PortSipEnumDefine.ENUM_AUDIOCODEC_G729)
        }
        if (preferences.getBoolean(context.getString(R.string.MEDIA_GSM), false)) {
            sdk.addAudioCodec(PortSipEnumDefine.ENUM_AUDIOCODEC_GSM)
        }
        if (preferences.getBoolean(context.getString(R.string.MEDIA_ILBC), false)) {
            sdk.addAudioCodec(PortSipEnumDefine.ENUM_AUDIOCODEC_ILBC)
        }
        if (preferences.getBoolean(context.getString(R.string.MEDIA_AMR), false)) {
            sdk.addAudioCodec(PortSipEnumDefine.ENUM_AUDIOCODEC_AMR)
        }
        if (preferences.getBoolean(context.getString(R.string.MEDIA_AMRWB), false)) {
            sdk.addAudioCodec(PortSipEnumDefine.ENUM_AUDIOCODEC_AMRWB)
        }
        if (preferences.getBoolean(context.getString(R.string.MEDIA_SPEEX), false)) {
            sdk.addAudioCodec(PortSipEnumDefine.ENUM_AUDIOCODEC_SPEEX)
        }
        if (preferences.getBoolean(context.getString(R.string.MEDIA_SPEEXWB), false)) {
            sdk.addAudioCodec(PortSipEnumDefine.ENUM_AUDIOCODEC_SPEEXWB)
        }
        if (preferences.getBoolean(context.getString(R.string.MEDIA_ISACWB), false)) {
            sdk.addAudioCodec(PortSipEnumDefine.ENUM_AUDIOCODEC_ISACWB)
        }
        if (preferences.getBoolean(context.getString(R.string.MEDIA_ISACSWB), false)) {
            sdk.addAudioCodec(PortSipEnumDefine.ENUM_AUDIOCODEC_ISACSWB)
        }
        //        if (preferences.getBoolean(context.getString(R.string.MEDIA_G7221), false)) {
//            sdk.addAudioCodec(PortSipEnumDefine.ENUM_AUDIOCODEC_G7221);
//        }
        if (preferences.getBoolean(context.getString(R.string.MEDIA_OPUS), false)) {
            sdk.addAudioCodec(PortSipEnumDefine.ENUM_AUDIOCODEC_OPUS)
        }
        sdk.clearVideoCodec()
        if (preferences.getBoolean(context.getString(R.string.MEDIA_H264), true)) {
            sdk.addVideoCodec(PortSipEnumDefine.ENUM_VIDEOCODEC_H264)
        }
        if (preferences.getBoolean(context.getString(R.string.MEDIA_VP8), true)) {
            sdk.addVideoCodec(PortSipEnumDefine.ENUM_VIDEOCODEC_VP8)
        }
        if (preferences.getBoolean(context.getString(R.string.MEDIA_VP9), true)) {
            sdk.addVideoCodec(PortSipEnumDefine.ENUM_VIDEOCODEC_VP9)
        }
        sdk.setVideoNackStatus(preferences.getBoolean(context.getString(R.string.VIDEO_NACK), true))
        sdk.enableAEC(preferences.getBoolean(context.getString(R.string.MEDIA_AEC), true))
        sdk.enableAGC(preferences.getBoolean(context.getString(R.string.MEDIA_AGC), true))
        sdk.enableCNG(preferences.getBoolean(context.getString(R.string.MEDIA_CNG), true))
        sdk.enableVAD(preferences.getBoolean(context.getString(R.string.MEDIA_VAD), true))
        sdk.enableANS(preferences.getBoolean(context.getString(R.string.MEDIA_ANS), false))
        val foward = preferences.getBoolean(context.getString(R.string.str_fwopenkey), false)
        val fowardBusy = preferences.getBoolean(context.getString(R.string.str_fwbusykey), false)
        val fowardto = preferences.getString(context.getString(R.string.str_fwtokey), null)
        if (foward && !TextUtils.isEmpty(fowardto)) {
            sdk.enableCallForward(fowardBusy, fowardto)
        }
        if (preferences.getBoolean(context.getString(R.string.str_pracktitle), false)) {
            sdk.setReliableProvisional(2)
        } else {
            sdk.setReliableProvisional(0)
        }
        val resolution = preferences.getString(context.getString(R.string.str_resolution), "CIF")
        var width = 352
        var height = 288
        if (resolution == "QCIF") {
            width = 176
            height = 144
        } else if (resolution == "CIF") {
            width = 352
            height = 288
        } else if (resolution == "VGA") {
            width = 640
            height = 480
        } else if (resolution == "720P") {
            width = 1280
            height = 720
        } else if (resolution == "1080P") {
            width = 1920
            height = 1080
        }
        sdk.setVideoResolution(width, height)
    }

    fun unregisterToServer() {
        mEngine!!.unRegisterServer()
        CallManager.Instance()!!.regist = false
    }

    private fun refreshPushToken() {
        if (!TextUtils.isEmpty(pushToken)) {
            val pushMessage =
                "device-os=android;device-uid=$pushToken;allow-call-push=true;allow-message-push=true;app-id=$APPID"
            //old version
            mEngine!!.addSipMessageHeader(-1, "REGISTER", 1, "portsip-push", pushMessage)
            //new version
            mEngine!!.addSipMessageHeader(-1, "REGISTER", 1, "x-p-push", pushMessage)
            mEngine!!.refreshRegistration(0)
        }
    }

    private fun getTransType(select: Int): Int {
        when (select) {
            0 -> return PortSipEnumDefine.ENUM_TRANSPORT_UDP
            1 -> return PortSipEnumDefine.ENUM_TRANSPORT_TLS
            2 -> return PortSipEnumDefine.ENUM_TRANSPORT_TCP
            3 -> return PortSipEnumDefine.ENUM_TRANSPORT_PERS_UDP
            4 -> return PortSipEnumDefine.ENUM_TRANSPORT_PERS_TCP
        }
        return PortSipEnumDefine.ENUM_TRANSPORT_UDP
    }

    fun getInstanceID(): String? {
        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        var insanceid = preferences.getString(INSTANCE_ID, "")
        if (TextUtils.isEmpty(insanceid)) {
            insanceid = UUID.randomUUID().toString()
            preferences.edit().putString(INSTANCE_ID, insanceid).commit()
        }
        return insanceid
    }


    private fun registerReceiver() {
        val filter = IntentFilter()
        filter.addAction("android.net.conn.CONNECTIVITY_CHANGE")
        mNetWorkReceiver = NetworkReceiver()
        mNetWorkReceiver!!.setListener(this)
        registerReceiver(mNetWorkReceiver, filter)
    }

    private fun unregisterReceiver() {
        if (mNetWorkReceiver != null) {
            unregisterReceiver(mNetWorkReceiver)
        }
    }

    fun sendPortSipMessage(message: String?, broadIntent: Intent?) {
        val intent = Intent(this, MainActivity::class.java)
        val contentIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        val builder: Notification.Builder
        builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, channelID)
        } else {
            Notification.Builder(this)
        }
        builder.setSmallIcon(R.drawable.send)
            .setContentTitle("Sip Notify")
            .setContentText(message)
            .setContentIntent(contentIntent)
            .build() // getNotification()
        mNotificationManager?.notify(1, builder.build())
        sendBroadcast(broadIntent)
    }

    override fun onRegisterSuccess(statusText: String?, statusCode: Int, sipMessage: String?) {
        Toast.makeText(this, "Registration Successfull", Toast.LENGTH_SHORT).show()

    }

    override fun onRegisterFailure(p0: String?, p1: Int, p2: String?) {
        Toast.makeText(this, "Registration failure", Toast.LENGTH_SHORT).show()
    }

    override fun onInviteIncoming(
        p0: Long,
        p1: String?,
        p2: String?,
        p3: String?,
        p4: String?,
        p5: String?,
        p6: String?,
        p7: Boolean,
        p8: Boolean,
        p9: String?
    ) {
        TODO("Not yet implemented")
    }

    override fun onInviteTrying(p0: Long) {
        Toast.makeText(this, "Calling entered mobile number", Toast.LENGTH_SHORT).show()
    }

    override fun onInviteSessionProgress(
        sessionId: Long,
        audioCodecNames: String?,
        videoCodecNames: String?,
        existsEarlyMedia: Boolean,
        existsAudio: Boolean,
        existsVideo: Boolean,
        sipMessage: String?
    ) {
        val session = CallManager.Instance()!!.findSessionBySessionID(sessionId)
        if (session != null) {
            session.bEarlyMedia = existsEarlyMedia
        }
    }

    override fun onInviteRinging(
        sessionId: Long,
        statusText: String?,
        statusCode: Int,
        sipMessage: String?
    ) {
        val session = CallManager.Instance()!!.findSessionBySessionID(sessionId)
        if (session != null && !session.bEarlyMedia) {
            Ring.getInstance(this)?.startRingBackTone()
        }
    }

    override fun onInviteAnswered(
        sessionId: Long,
        callerDisplayName: String?,
        caller: String?,
        calleeDisplayName: String?,
        callee: String?,
        audioCodecNames: String?,
        videoCodecNames: String?,
        existsAudio: Boolean,
        existsVideo: Boolean,
        sipMessage: String?
    ) {
        val session = CallManager.Instance()!!.findSessionBySessionID(sessionId)
        if (session != null) {
            session.state = Session.CALL_STATE_FLAG.CONNECTED
            session.hasVideo = existsVideo
            val broadIntent = Intent(PortSipService.CALL_CHANGE_ACTION)
            broadIntent.putExtra(EXTRA_CALL_SEESIONID, sessionId)
            val description = session.lineName + " onInviteAnswered"
            broadIntent.putExtra(EXTRA_CALL_DESCRIPTION, description)
            sendPortSipMessage(description, broadIntent)
        }
        Ring.getInstance(this)?.stopRingBackTone()
    }

    override fun onInviteFailure(sessionId: Long, reason: String?, code: Int, sipMessage: String?) {
        val session = CallManager.Instance()!!.findSessionBySessionID(sessionId)
        if (session != null) {
            session.state = Session.CALL_STATE_FLAG.FAILED
            session.sessionID = sessionId
            val broadIntent = Intent(PortSipService.CALL_CHANGE_ACTION)
            broadIntent.putExtra(EXTRA_CALL_SEESIONID, sessionId)
            val description = session.lineName + " onInviteFailure"
            broadIntent.putExtra(EXTRA_CALL_DESCRIPTION, description)
//            sendPortSipMessage(description, broadIntent)
            Toast.makeText(this, "Reached on Invite failure", Toast.LENGTH_SHORT).show()
        }
        Ring.getInstance(this)?.stopRingBackTone()
    }


    override fun onInviteUpdated(
        sessionId: Long,
        audioCodecs: String?,
        videoCodecs: String?,
        screenCodecs: String?,
        existsAudio: Boolean,
        existsVideo: Boolean,
        existsScreen: Boolean,
        sipMessage: String?
    ) {
        val session = CallManager.Instance()!!.findSessionBySessionID(sessionId)
        if (session != null) {
            session.state = Session.CALL_STATE_FLAG.CONNECTED
            session.hasVideo = existsVideo
            session.bScreenShare = existsScreen
            val broadIntent = Intent(PortSipService.CALL_CHANGE_ACTION)
            broadIntent.putExtra(EXTRA_CALL_SEESIONID, sessionId)
            val description = session.lineName + " OnInviteUpdated"
            broadIntent.putExtra(EXTRA_CALL_DESCRIPTION, description)
            sendPortSipMessage(description, broadIntent)
        }
    }

    override fun onInviteConnected(sessionId: Long) {
        val session = CallManager.Instance()!!.findSessionBySessionID(sessionId)
        if (session != null) {
            session.state = Session.CALL_STATE_FLAG.CONNECTED
            session.sessionID = sessionId
            if (applicaton?.mConference == true) {
                applicaton!!.mEngine!!.joinToConference(session.sessionID)
                applicaton!!.mEngine!!.sendVideo(session.sessionID, true)
            }
            val broadIntent = Intent(PortSipService.CALL_CHANGE_ACTION)
            broadIntent.putExtra(EXTRA_CALL_SEESIONID, sessionId)
            val description = session.lineName + " OnInviteConnected"
            broadIntent.putExtra(EXTRA_CALL_DESCRIPTION, description)
            sendPortSipMessage(description, broadIntent)
        }
    }

    override fun onInviteBeginingForward(p0: String?) {
        TODO("Not yet implemented")
    }

    override fun onInviteClosed(sessionId: Long, sipMessage: String?) {
        val session = CallManager.Instance()!!.findSessionBySessionID(sessionId)
        if (session != null) {
            session.state = Session.CALL_STATE_FLAG.CLOSED
            session.sessionID = sessionId
            val broadIntent = Intent(PortSipService.CALL_CHANGE_ACTION)
            broadIntent.putExtra(EXTRA_CALL_SEESIONID, sessionId)
            val description = session.lineName + " OnInviteClosed"
            broadIntent.putExtra(EXTRA_CALL_DESCRIPTION, description)
            sendPortSipMessage(description, broadIntent)
        }
        Ring.getInstance(this)!!.stopRingTone()
        mNotificationManager!!.cancel(PortSipService.PENDINGCALL_NOTIFICATION)
    }

    override fun onDialogStateUpdated(p0: String?, p1: String?, p2: String?, p3: String?) {
        TODO("Not yet implemented")
    }

    override fun onRemoteHold(p0: Long) {
        TODO("Not yet implemented")
    }

    override fun onRemoteUnHold(p0: Long, p1: String?, p2: String?, p3: Boolean, p4: Boolean) {
        TODO("Not yet implemented")
    }

    override fun onReceivedRefer(p0: Long, p1: Long, p2: String?, p3: String?, p4: String?) {
        TODO("Not yet implemented")
    }

    override fun onReferAccepted(p0: Long) {
        TODO("Not yet implemented")
    }

    override fun onReferRejected(p0: Long, p1: String?, p2: Int) {
        TODO("Not yet implemented")
    }

    override fun onTransferTrying(p0: Long) {
        TODO("Not yet implemented")
    }

    override fun onTransferRinging(p0: Long) {
        TODO("Not yet implemented")
    }

    override fun onACTVTransferSuccess(p0: Long) {
        TODO("Not yet implemented")
    }

    override fun onACTVTransferFailure(p0: Long, p1: String?, p2: Int) {
        TODO("Not yet implemented")
    }

    override fun onReceivedSignaling(p0: Long, p1: String?) {
        TODO("Not yet implemented")
    }

    override fun onSendingSignaling(p0: Long, p1: String?) {
        TODO("Not yet implemented")
    }

    override fun onWaitingVoiceMessage(p0: String?, p1: Int, p2: Int, p3: Int, p4: Int) {
        TODO("Not yet implemented")
    }

    override fun onWaitingFaxMessage(p0: String?, p1: Int, p2: Int, p3: Int, p4: Int) {
        TODO("Not yet implemented")
    }

    override fun onRecvDtmfTone(p0: Long, p1: Int) {
        TODO("Not yet implemented")
    }

    override fun onRecvOptions(p0: String?) {
        TODO("Not yet implemented")
    }

    override fun onRecvInfo(p0: String?) {
        TODO("Not yet implemented")
    }

    override fun onRecvNotifyOfSubscription(p0: Long, p1: String?, p2: ByteArray?, p3: Int) {
        TODO("Not yet implemented")
    }

    override fun onPresenceRecvSubscribe(p0: Long, p1: String?, p2: String?, p3: String?) {
        TODO("Not yet implemented")
    }

    override fun onPresenceOnline(p0: String?, p1: String?, p2: String?) {
        TODO("Not yet implemented")
    }

    override fun onPresenceOffline(p0: String?, p1: String?) {
        TODO("Not yet implemented")
    }

    override fun onRecvMessage(p0: Long, p1: String?, p2: String?, p3: ByteArray?, p4: Int) {
        TODO("Not yet implemented")
    }

    override fun onRecvOutOfDialogMessage(
        p0: String?,
        p1: String?,
        p2: String?,
        p3: String?,
        p4: String?,
        p5: String?,
        p6: ByteArray?,
        p7: Int,
        p8: String?
    ) {
        TODO("Not yet implemented")
    }

    override fun onSendMessageSuccess(p0: Long, p1: Long, p2: String?) {
        TODO("Not yet implemented")
    }

    override fun onSendMessageFailure(p0: Long, p1: Long, p2: String?, p3: Int, p4: String?) {
        TODO("Not yet implemented")
    }

    override fun onSendOutOfDialogMessageSuccess(
        p0: Long,
        p1: String?,
        p2: String?,
        p3: String?,
        p4: String?,
        p5: String?
    ) {
        TODO("Not yet implemented")
    }

    override fun onSendOutOfDialogMessageFailure(
        p0: Long,
        p1: String?,
        p2: String?,
        p3: String?,
        p4: String?,
        p5: String?,
        p6: Int,
        p7: String?
    ) {
        TODO("Not yet implemented")
    }

    override fun onSubscriptionFailure(p0: Long, p1: Int) {
        TODO("Not yet implemented")
    }

    override fun onSubscriptionTerminated(p0: Long) {
        TODO("Not yet implemented")
    }

    override fun onPlayAudioFileFinished(p0: Long, p1: String?) {
        TODO("Not yet implemented")
    }

    override fun onPlayVideoFileFinished(p0: Long) {
        TODO("Not yet implemented")
    }

    override fun onAudioDeviceChanged(
        audioDevice: PortSipEnumDefine.AudioDevice?,
        set: Set<PortSipEnumDefine.AudioDevice?>?
    ) {
        CallManager.Instance()!!.setSelectalbeAudioDevice(audioDevice!!,
            set as Set<PortSipEnumDefine.AudioDevice>?
        )
        val intent = Intent()
        intent.action = PortSipService.ACTION_SIP_AUDIODEVICE
        sendBroadcast(intent)
    }

    override fun onRTPPacketCallback(p0: Long, p1: Int, p2: Int, p3: ByteArray?, p4: Int) {
        TODO("Not yet implemented")
    }

    override fun onAudioRawCallback(p0: Long, p1: Int, p2: ByteArray?, p3: Int, p4: Int) {
        TODO("Not yet implemented")
    }

    override fun onVideoRawCallback(p0: Long, p1: Int, p2: Int, p3: Int, p4: ByteArray?, p5: Int) {
        TODO("Not yet implemented")
    }

    override fun onBind(intent: Intent?): IBinder? {
        TODO("Not yet implemented")
    }

    override fun onNetworkChange(netMobile: Int) {
        if (netMobile == -1) {
            //invaluable
        } else {
            if (CallManager.Instance()!!.online) {
                applicaton!!.mEngine!!.refreshRegistration(0)
            } else {
                //
            }
        }
    }
}
