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
import xyz.yingshaoxo.android.subtitlekiller.R.attr.icon


class FloatingAppService : Service() {
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

    val min_height = get_min_height()
    val max_height = get_max_height()
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


    // About windows
    lateinit var myView: View
    lateinit var window_manager: WindowManager

    override fun onCreate() {
        super.onCreate()

        if (Build.VERSION.SDK_INT >= 26) {
            startNotification()
        }

        var params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        )

        // Check if we're running on Android 7.0 or lower
        if (Build.VERSION.SDK_INT < 26) {
            // Call some material design APIs here
            params = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT
            )
        }

        val last_y = read_txt("y.txt")
        if (last_y != "") {
            params.x = 0
            params.y = last_y.toInt()
            params.gravity = Gravity.BOTTOM or Gravity.CENTER
        } else {
            params.gravity = Gravity.BOTTOM or Gravity.CENTER
            params.x = 0
            params.y = 100
        }

        window_manager= getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        myView = inflater!!.inflate(R.layout.floating_window, null, true)

        var initial_layout_x = 0
        var initial_layout_y = 0
        var starting_touch_x: Float = 0.toFloat()
        var starting_touch_y: Float = 0.toFloat()
        var distance = 0
        //var toast = Toast.makeText(this, "hi", Toast.LENGTH_SHORT)

        val last_hight = read_txt("height.txt")
        if (last_hight != "") {
            params.height =  last_hight.toInt()
        } else {
            params.height = min_height
        }

        myView.button.setOnTouchListener { view, motionEvent ->
            //Toast.makeText(this, "(${getScreenHeight().toInt()}, ${getScreenWidth().toInt()})",Toast.LENGTH_SHORT).show()

            when (motionEvent.action) {
                MotionEvent.ACTION_DOWN -> {
                    initial_layout_x = params.x
                    initial_layout_y = params.y
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
                                params.height = (params.height - (starting_touch_x - motionEvent.rawX) * 0.1).toInt()
                            } else if (starting_touch_x < motionEvent.rawX) {
                                params.height = (params.height + (motionEvent.rawX - starting_touch_x) * 0.1).toInt()
                            }
                            if (params.height < min_height) {
                                params.height = min_height
                            }
                            if (params.height > max_height) {
                                params.height = max_height
                            }
                        }
                    } else if (up_or_down > left_or_right) {
                        // move box
                        //params.x = initial_layout_x + (motionEvent.getRawX() - starting_touch_y).toInt();
                        params.y = initial_layout_y - (motionEvent.rawY - starting_touch_y).toInt()
                    }

                    window_manager.updateViewLayout(myView, params)
                }
                MotionEvent.ACTION_UP -> {
                    distance = 0
                    write_txt("height.txt", params.height.toString())
                    write_txt("y.txt", params.y.toString())
                    params.width = max_value
                }
            }
            true
        }
        myView.button.setOnKeyListener { view, action, key_code ->  //Codes for Android TV Remote.
            if (action == 0) {
                when (key_code) {
                    19 -> params.y = initial_layout_y - 1  //Up
                    20 -> params.y = initial_layout_y + 1  //Down
                    21 -> params.x = initial_layout_x - 1  //Left
                    22 -> params.x = initial_layout_x + 1  //Right
                }
                window_manager.updateViewLayout(myView, params)
            }
        }

        // Add layout to window manager
        window_manager!!.addView(myView, params)
    }

    override fun onDestroy() {
        super.onDestroy()

        window_manager.removeViewImmediate(myView)
    }
}
