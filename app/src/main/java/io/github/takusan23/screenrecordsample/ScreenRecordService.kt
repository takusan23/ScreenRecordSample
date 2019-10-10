package io.github.takusan23.screenrecordsample

import android.app.*
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.IBinder
import androidx.core.app.NotificationCompat
import java.io.File

class ScreenRecordService : Service() {

    //Intentに詰めたデータを受け取る
    var data: Intent? = null
    var code = Activity.RESULT_OK

    //画面録画で使う
    lateinit var mediaRecorder: MediaRecorder
    lateinit var projectionManager: MediaProjectionManager
    lateinit var projection: MediaProjection
    lateinit var virtualDisplay: VirtualDisplay

    //画面の大きさ
    //Pixel 3 XLだとなんかおかしくなる
    var height = 2800
    var width = 1400
    var dpi = 1000


    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        //データ受け取る
        data = intent?.getParcelableExtra("data")
        code = intent?.getIntExtra("code", Activity.RESULT_OK) ?: Activity.RESULT_OK

        //画面の大きさ
        //   height = intent?.getIntExtra("height", 1000) ?: 1000
        //   width = intent?.getIntExtra("width", 1000) ?: 1000
        dpi = intent?.getIntExtra("dpi", 1000) ?: 1000

        //通知を出す。
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        //通知チャンネル
        val channelID = "rec_notify"
        //通知チャンネルが存在しないときは登録する
        if (notificationManager.getNotificationChannel(channelID) == null) {
            val channel =
                NotificationChannel(channelID, "録画サービス起動中通知", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }
        //通知作成
        val notification = Notification.Builder(applicationContext, channelID)
            .setContentText("録画中です。")
            .setContentTitle("画面録画")
            .setSmallIcon(R.drawable.ic_cast_black_24dp)    //アイコンはベクターアセットから
            .build()

        startForeground(1, notification)

        //録画開始
        startRec()

        return START_NOT_STICKY
    }

    //Service終了と同時に録画終了
    override fun onDestroy() {
        super.onDestroy()
        stopRec()
    }

    //録画開始
    fun startRec() {
        if (data != null) {
            projectionManager =
                getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            //codeはActivity.RESULT_OKとかが入る。
            projection =
                projectionManager.getMediaProjection(code, data!!)

            mediaRecorder = MediaRecorder()
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE)
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            mediaRecorder.setVideoEncodingBitRate(1080 * 10000) //1080は512ぐらいにしといたほうが小さくできる
            mediaRecorder.setVideoFrameRate(30)
            mediaRecorder.setVideoSize(width, height)
            mediaRecorder.setAudioSamplingRate(44100)
            mediaRecorder.setOutputFile(getFilePath())
            mediaRecorder.prepare()

            virtualDisplay = projection.createVirtualDisplay(
                "recode",
                width,
                height,
                dpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mediaRecorder.surface,
                null,
                null
            )

            //開始
            mediaRecorder.start()
        }
    }

    //録画止める
    fun stopRec() {
        mediaRecorder.stop()
        mediaRecorder.release()
        virtualDisplay.release()
        projection.stop()
    }

    //保存先取得。今回は対象範囲別ストレージに保存する
    fun getFilePath(): File {
        //ScopedStorageで作られるサンドボックスへのぱす
        val scopedStoragePath = getExternalFilesDir(null)
        //写真ファイル作成
        val file = File("${scopedStoragePath?.path}/${System.currentTimeMillis()}.mp4")
        return file
    }

}