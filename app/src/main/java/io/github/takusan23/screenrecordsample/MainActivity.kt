package io.github.takusan23.screenrecordsample

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    //リクエストの結果
    val code = 512
    val permissionCode = 810
    lateinit var projectionManager: MediaProjectionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        projectionManager =
            getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        //画面録画をしてもいいか聞く
        rec_button.setOnClickListener {
            //その前にマイクへアクセスしていいか尋ねる
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), permissionCode)
            } else {
                //マイクの権限があるので画面録画リクエスト
                //ダイアログを出す
                startActivityForResult(projectionManager.createScreenCaptureIntent(), code)
            }
        }
        //停止ボタンで止められるように
        stop_button.setOnClickListener {
            val intent = Intent(this, ScreenRecordService::class.java)
            stopService(intent)
        }
    }

    //画面録画の合否を受け取る
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        //成功＋結果が画面録画の物か
        if (resultCode == Activity.RESULT_OK && requestCode == code) {
            //Service起動
            //Manifestに「android:foregroundServiceType="mediaProjection"」を付け足しておく
            val intent = Intent(this, ScreenRecordService::class.java)
            intent.putExtra("code", resultCode) //必要なのは結果。startActivityForResultのrequestCodeではない。
            intent.putExtra("data", data)
            //画面の大きさも一緒に入れる
            val metrics = resources.displayMetrics;
            intent.putExtra("height", metrics.heightPixels)
            intent.putExtra("width", metrics.widthPixels)
            intent.putExtra("dpi", metrics.densityDpi)

            startForegroundService(intent)
        }
    }

    //権限の結果受け取る
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == permissionCode) {
            //マイクへアクセス権げっと
            Toast.makeText(this, "権限が付与されました。", Toast.LENGTH_SHORT).show()
        }
    }

}
