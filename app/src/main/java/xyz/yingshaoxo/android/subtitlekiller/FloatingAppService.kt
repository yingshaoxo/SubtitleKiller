package xyz.yingshaoxo.android.subtitlekiller

import android.app.*
import android.content.Context
import android.content.Intent
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




class FloatingAppService : Service() {
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
        val notification = Notification.Builder(this, createNotificationChannel())
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.mipmap.ic_launcher)
                //.setContentTitle(FloatingAppService::class.simpleName)
                //.setContentText("Service is running.")
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

        // Check if we're running on Android 5.0 or higher
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
        //var toast = Toast.makeText(this, "hi", Toast.LENGTH_SHORT)

        val last_hight = read_txt("height.txt")
        if (last_hight != "") {
            params.height =  last_hight.toInt()
        } else {
            params.height = 100
        }
        myView.button.setOnTouchListener { view, motionEvent ->
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
                        // resize box
                        if (starting_touch_x > motionEvent.rawX) {
                            params.height = (params.height - (starting_touch_x - motionEvent.rawX) * 0.1).toInt()
                        } else if (starting_touch_x < motionEvent.rawX) {
                            params.height = (params.height + (motionEvent.rawX - starting_touch_x) * 0.1).toInt()
                        }
                        if (params.height < 100) {
                            params.height = 100
                        }
                        if (params.height > 800) {
                            params.height = 800
                        }
                    } else if (up_or_down > left_or_right) {
                        // move box
                        //params.x = initial_layout_x + (motionEvent.getRawX() - starting_touch_y).toInt();
                        params.y = initial_layout_y - (motionEvent.rawY - starting_touch_y).toInt()
                    }

                    window_manager.updateViewLayout(myView, params)
                }
                MotionEvent.ACTION_UP -> {
                    write_txt("height.txt", params.height.toString())
                    write_txt("y.txt", params.y.toString())
                    params.width = 1920
                    /*
                    toast.cancel()
                    toast = Toast.makeText(this, "(${starting_touch_x.toInt()}, ${starting_touch_y.toInt()})",Toast.LENGTH_SHORT)
                    toast.show()
                    */
                }
            }
            true
        }

        // Add layout to window manager
        window_manager!!.addView(myView, params)
    }

    override fun onDestroy() {
        super.onDestroy()

        window_manager.removeViewImmediate(myView)
    }
}
