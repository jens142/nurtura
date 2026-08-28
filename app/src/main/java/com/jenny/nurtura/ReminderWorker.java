package com.jenny.nurtura;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class ReminderWorker extends Worker {

    public static final String KEY_TITLE = "reminderTitle";
    public static final String KEY_MESSAGE = "reminderMessage";
    public static final String KEY_NOTIFICATION_ID = "notificationId";

    private static final String CHANNEL_ID = "nurtura_baby_reminders";

    public ReminderWorker(
            @NonNull Context context,
            @NonNull WorkerParameters workerParameters
    ) {
        super(context, workerParameters);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();

        String title = getInputData().getString(KEY_TITLE);
        String message = getInputData().getString(KEY_MESSAGE);
        int notificationId = getInputData().getInt(
                KEY_NOTIFICATION_ID,
                (int) System.currentTimeMillis()
        );

        if (title == null || title.trim().isEmpty()
                || message == null || message.trim().isEmpty()) {
            return Result.failure();
        }

        showNotification(context, title, message, notificationId);
        return Result.success();
    }

    public static void showNotification(
            Context context,
            String title,
            String message,
            int notificationId
    ) {
        createNotificationChannel(context);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
        ) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        Intent openIntent = new Intent(
                context,
                BabyHealthScheduleActivity.class
        );
        openIntent.addFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP
        );

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
                        | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder notification =
                new NotificationCompat.Builder(context, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_nurtura_notification)
                        .setContentTitle(title)
                        .setContentText(message)
                        .setStyle(
                                new NotificationCompat.BigTextStyle()
                                        .bigText(message)
                        )
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setCategory(NotificationCompat.CATEGORY_REMINDER)
                        .setContentIntent(pendingIntent)
                        .setAutoCancel(true);

        try {
            NotificationManagerCompat.from(context).notify(
                    notificationId,
                    notification.build()
            );
        } catch (SecurityException ignored) {
            // Notification permission can be changed at any time.
        }
    }

    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.baby_reminder_channel_name),
                NotificationManager.IMPORTANCE_HIGH
        );

        channel.setDescription(
                context.getString(
                        R.string.baby_reminder_channel_description
                )
        );

        NotificationManager notificationManager =
                context.getSystemService(NotificationManager.class);

        if (notificationManager != null) {
            notificationManager.createNotificationChannel(channel);
        }
    }
}
