package xyz.yingshaoxo.android.subtitlekiller

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Color
import android.os.Build
import android.os.IBinder
import android.support.annotation.RequiresApi
import java.util.*
import android.graphics.PixelFormat
import android.os.Environment
import android.view.*
import android.widget.Toast
import kotlinx.android.synthetic.main.floating_window.view.*
import java.io.File
import java.lang.Math.abs
import android.util.DisplayMetrics
import java.lang.Math.min
import android.app.PendingIntent
import android.app.Notification.EXTRA_NOTIFICATION_ID
import android.renderscript.Int2
import xyz.yingshaoxo.android.subtitlekiller.R.attr.icon
import kotlin.properties.Delegates


class FloatingAppService : Service() {
    // About windows
    companion object {
        lateinit var params: WindowManager.LayoutParams
        lateinit var myView: View
        lateinit var window_manager: WindowManager
        var initial_layout_x = 0
        var initial_layout_y = 0
        var min_height = 0
        var max_height = 0

    }
    fun getScreenWidth(): Int {
        return Resources.getSystem().getDisplayMetrics().widthPixels
    }

    fun getScreenHeight(): Int {
        return Resources.getSystem().getDisplayMetrics().heightPixels
    }

    fun get_max(): Int {
        var the_max = 0
        if (getScreenHeight() > getScreenWidth()) {
            the_max = getScreenHeight()
        } else {
            the_max = getScreenWidth()
        }
        return the_max
    }

    fun get_min_height(): Int {
        return (0.05 * get_max()).toInt()
    }

    fun get_max_height(): Int {
        return (0.6 * get_max()).toInt()
    }

    Companion.min_height = get_min_height()
    Companion.max_height = get_max_height()
    val max_value = get_max()


    // Input and output
    fun write_txt(file_name: String, text: String) {
        val dest = File(this.filesDir.toString() + file_name)
        dest.writeText(text)
    }

    fun read_txt(file_name: String): String {
        val dest = File(this.filesDir.toString() + file_name)
        if (dest.exists()) {
            return dest.readText()
        } else {
            return ""
        }
    }

    // About notifacation
    private val notificationId = Random().nextInt()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return Service.START_STICKY
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(): String{
        val channelId = "subtitlekiller"
        val channelName = "Subtitle Killer service"
        val chan = NotificationChannel(channelId,
                channelName, NotificationManager.IMPORTANCE_NONE)
        chan.lightColor = Color.BLUE
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(chan)
        return channelId
    }

    private fun startNotification() {
        val activityIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, activityIntent, 0)

        val kill_Intent = Intent(this, MyBroadcastReceiver::class.java).apply {
            action = "kill_yourself"
            putExtra(EXTRA_NOTIFICATION_ID, 0)
        }
        val kill_PendingIntent: PendingIntent =
                PendingIntent.getBroadcast(this, 0, kill_Intent, 0)

        val notification = Notification.Builder(this, createNotificationChannel())
                .setContentIntent(kill_PendingIntent)
                .setSmallIcon(R.mipmap.ic_launcher)
                //.setContentTitle(FloatingAppService::class.simpleName)
                .setContentText(getString(R.string.notification_string))
                /*
                .addAction(R.drawable.notification_icon_background, "STOP",
                        kill_PendingIntent)
                */
                .build()
        startForeground(notificationId, notification)
    }

    override fun onCreate() {
        super.onCreate()

        if (Build.VERSION.SDK_INT >= 26) {
            startNotification()
        }

        Companion.params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        )

        // Check if we're running on Android 7.0 or lower
        if (Build.VERSION.SDK_INT < 26) {
            // Call some material design APIs here
            Companion.params = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT
            )
        }

        val last_y = read_txt("y.txt")
        if (last_y != "") {
            Companion.params.x = 0
            Companion.params.y = last_y.toInt()
            Companion.params.gravity = Gravity.BOTTOM or Gravity.CENTER
        } else {
            Companion.params.gravity = Gravity.BOTTOM or Gravity.CENTER
            Companion.params.x = 0
            Companion.params.y = 100
        }

        Companion.window_manager= getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        Companion.myView = inflater!!.inflate(R.layout.floating_window, null, true)

        Companion.initial_layout_x = 0
        Companion.initial_layout_y = 0
        var starting_touch_x: Float = 0.toFloat()
        var starting_touch_y: Float = 0.toFloat()
        var distance = 0
        //var toast = Toast.makeText(this, "hi", Toast.LENGTH_SHORT)

        val last_hight = read_txt("height.txt")
        if (last_hight != "") {
            Companion.params.height =  last_hight.toInt()
        } else {
            Companion.params.height = Companion.min_height
        }

        Companion.myView.button.setOnTouchListener { view, motionEvent ->
            //Toast.makeText(this, "(${getScreenHeight().toInt()}, ${getScreenWidth().toInt()})",Toast.LENGTH_SHORT).show()

            when (motionEvent.action) {
                MotionEvent.ACTION_DOWN -> {
                    Companion.initial_layout_x = Companion.params.x
                    Companion.initial_layout_y = Companion.params.y
                    starting_touch_x = motionEvent.rawX
                    starting_touch_y = motionEvent.rawY
                }
                MotionEvent.ACTION_MOVE -> {
                    val left_or_right = abs(starting_touch_x - motionEvent.rawX)
                    val up_or_down = abs(starting_touch_y - motionEvent.rawY)
                    if (left_or_right > up_or_down) {
                        distance += 1
                        if (distance > 25) {
                            // resize box
                            if (starting_touch_x > motionEvent.rawX) {
                                Companion.params.height = (Companion.params.height - (starting_touch_x - motionEvent.rawX) * 0.1).toInt()
                            } else if (starting_touch_x < motionEvent.rawX) {
                                Companion.params.height = (Companion.params.height + (motionEvent.rawX - starting_touch_x) * 0.1).toInt()
                            }
                            if (Companion.params.height < Companion.min_height) {
                                Companion.params.height = Companion.min_height
                            }
                            if (Companion.params.height > Companion.max_height) {
                                Companion.params.height = Companion.max_height
                            }
                        }
                    } else if (up_or_down > left_or_right) {
                        // move box
                        //params.x = initial_layout_x + (motionEvent.getRawX() - starting_touch_y).toInt();
                        Companion.params.y = Companion.initial_layout_y - (motionEvent.rawY - starting_touch_y).toInt()
                    }

                    Companion.window_manager.updateViewLayout(Companion.myView, Companion.params)
                }
                MotionEvent.ACTION_UP -> {
                    distance = 0
                    write_txt("height.txt", Companion.params.height.toString())
                    write_txt("y.txt", Companion.params.y.toString())
                    Companion.params.width = max_value
                }
            }
            true
        }

        //Codes for Android TV Remote.
        Companion.myView.button.setOnKeyListener { _, code, event ->
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                when (code) {
                    KeyEvent.KEYCODE_DPAD_UP -> Companion.params.y = Companion.initial_layout_y - 1  //Up
                    KeyEvent.KEYCODE_DPAD_DOWN -> Companion.params.y = Companion.initial_layout_y + 1 //Down
                    KeyEvent.KEYCODE_DPAD_LEFT -> Companion.params.height -= 1  //Left
                    KeyEvent.KEYCODE_DPAD_RIGHT -> Companion.params.height += 1  //Right
                }
                if (Companion.params.height < Companion.min_height) {
                    Companion.params.height = Companion.min_height
                }
                if (Companion.params.height > Companion.max_height) {
                    Companion.params.height = Companion.max_height
                }
                Companion.window_manager.updateViewLayout(Companion.myView, Companion.params)
            }
            true
        }

        // Add layout to window manager
        Companion.window_manager.addView(Companion.myView, Companion.params)
    }

    override fun onDestroy() {
        super.onDestroy()

        Companion.window_manager.removeViewImmediate(Companion.myView)
    }
}
