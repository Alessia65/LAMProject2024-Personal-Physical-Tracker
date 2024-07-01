    package com.example.personalphysicaltracker.utils

    import android.Manifest
    import android.app.NotificationManager
    import android.app.PendingIntent
    import android.app.Service
    import android.content.Context
    import android.content.Intent
    import android.content.pm.PackageManager
    import android.os.Binder
    import android.os.IBinder
    import android.util.Log
    import androidx.core.app.ActivityCompat
    import androidx.core.app.NotificationCompat
    import androidx.core.app.NotificationManagerCompat
    import androidx.core.app.ServiceCompat.startForeground
    import com.example.personalphysicaltracker.MainActivity
    import com.example.personalphysicaltracker.R

    class NotificationService : Service() {

        inner class SimpleBinder : Binder() {
            fun getService(): NotificationService {
                return this@NotificationService
            }
        }

        private val binder = SimpleBinder()

        override fun onBind(intent: Intent?): IBinder {
            return binder
        }

        override fun onCreate() {
            super.onCreate()
        }


        fun showDailyReminderNotification(context: Context, title: String, message: String) {
            Log.d("SHOW DAILY NOTIFICATION","FROM NOTIFICATION SERVICE")

            val intent = Intent(context, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            val pendingIntent = PendingIntent.getActivity(context, Constants.REQUEST_CODE_DAILY_REMINDER, intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

            val notification = NotificationCompat.Builder(context, Constants.CHANNEL_DAILY_REMINDER_ID)
                .setSmallIcon(R.drawable.ic_notifications_black_24dp)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true) // Chiude la notifica quando viene cliccata
                .build()

            with(NotificationManagerCompat.from(context)) {
                if (ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return
                }
                notify(Constants.REQUEST_CODE_DAILY_REMINDER, notification)
            }
        }

        fun showStepsReminderNotification(context: Context, title: String, message: String) {
            Log.d("SHOW STEP NOTIFICATION","FROM NOTIFICATION SERVICE")

            val intent = Intent(context, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            val pendingIntent = PendingIntent.getActivity(context, Constants.REQUEST_CODE_STEPS_REMINDER, intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

            val notification = NotificationCompat.Builder(context, Constants.CHANNEL_STEPS_REMINDER_ID)
                .setSmallIcon(R.drawable.ic_notifications_black_24dp)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true) // Chiude la notifica quando viene cliccata
                .build()

            with(NotificationManagerCompat.from(context)) {
                if (ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return
                }
                notify(Constants.REQUEST_CODE_STEPS_REMINDER, notification)
            }
        }

        fun showActivityChangesNotification(context: Context, title: String, message: String){
            Log.d("SHOW ACTIVITY RECOGNITION NOTIFICATION","FROM NOTIFICATION SERVICE")

            val intent = Intent(context, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            val pendingIntent = PendingIntent.getActivity(context, Constants.REQUEST_CODE_ACTIVITY_RECOGNITION, intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

            val notification = NotificationCompat.Builder(context, Constants.CHANNEL_ACTIVITY_RECOGNITION_ID)
                .setSmallIcon(R.drawable.ic_notifications_black_24dp)
                .setContentTitle(title)
                .setContentText(message)
                .setSmallIcon(R.drawable.ic_notifications_black_24dp)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()

            with(NotificationManagerCompat.from(context)) {
                if (ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    Log.d("NOTIFICATION SERVICE","Permission not Granted")
                    return
                }
                notify(Constants.REQUEST_CODE_ACTIVITY_RECOGNITION, notification)
            }
        }

         fun createPermanentNotificationActivityRecognition(context: Context) {

            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent: PendingIntent = PendingIntent.getActivity(context, Constants.REQUEST_CODE_ACTIVITY_RECOGNITION, intent, PendingIntent.FLAG_IMMUTABLE)

            val notification = NotificationCompat.Builder(context, Constants.CHANNEL_ACTIVITY_RECOGNITION_ID)
                .setContentTitle("Activity Recognition On")
                .setContentText("Don't close the app.")
                .setSmallIcon(R.drawable.ic_notifications_black_24dp)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()

             with(NotificationManagerCompat.from(context)) {
                 if (ActivityCompat.checkSelfPermission(
                         context,
                         Manifest.permission.POST_NOTIFICATIONS
                     ) != PackageManager.PERMISSION_GRANTED
                 ) {
                     Log.d("NOTIFICATION SERVICE","Permission not Granted")
                     return
                 }
                 notify(Constants.REQUEST_CODE_ACTIVITY_RECOGNITION, notification)
             }
        }

        fun stopPermanentNotificationActivityRecognition(context: Context) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(Constants.REQUEST_CODE_ACTIVITY_RECOGNITION)
        }





    }
