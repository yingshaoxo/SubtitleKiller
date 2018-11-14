package xyz.yingshaoxo.android.subtitlekiller

import android.content.BroadcastReceiver
import android.content.Context
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.content.Intent
import android.os.Handler
import android.widget.Toast
import com.linchaolong.android.floatingpermissioncompat.FloatingPermissionCompat
import kotlinx.android.synthetic.main.activity_main.*
import xyz.yingshaoxo.android.subtitlekiller.GlobalVariable.service_intent
import xyz.yingshaoxo.android.subtitlekiller.GlobalVariable.started


object GlobalVariable {
    lateinit var service_intent: Intent
    var started = false

    var handler = Handler()
    var kill_program_runable = Runnable {  }
}

class MainActivity : AppCompatActivity() {
    var allowed = false
    var could_do = false
    fun get_permission() {
        // 检测是否已授权悬浮窗权限（check floating window permission）
        val allowed = FloatingPermissionCompat.get().check(this)

        // 判断是否已经兼容当前 ROM（check if supported current ROM）
        val could_do = FloatingPermissionCompat.get().isSupported()

        // 打开授权界面（show the floating window permission activity）
        if (!allowed) {
            if (could_do) {
                FloatingPermissionCompat.get().apply(this);
            } else {
                this.finish()
            }
        }

        while (!FloatingPermissionCompat.get().check(this)) {
        }
    }

    fun start_service() {
        if (!started) {
            service_intent = Intent(this, FloatingAppService::class.java)
            startService(service_intent)
            started = true
        }
    }

    fun stop_service() {
        if (started) {
            stopService(service_intent)
            started = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        get_permission()

        textView.setOnLongClickListener {
            stop_service()
            finish()
            true
        }

        GlobalVariable.kill_program_runable = Runnable {
            stop_service()
            finish()
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()

        start_service()

        finish()
    }

    override fun onBackPressed() {
        super.onBackPressed()

        start_service()

        finish()
    }

}


class MyBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "kill_yourself") {
            GlobalVariable.handler.post(GlobalVariable.kill_program_runable)
        }
    }
}
