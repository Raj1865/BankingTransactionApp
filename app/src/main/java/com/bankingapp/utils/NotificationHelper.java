package com.bankingapp.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.bankingapp.activities.DashboardActivity;

public class NotificationHelper {

    public static final String CHANNEL_TRANSACTIONS = "channel_transactions";
    public static final String CHANNEL_ALERTS       = "channel_alerts";

    private static final int NOTIF_ID_SEND    = 1001;
    private static final int NOTIF_ID_RECEIVE = 1002;
    private static final int NOTIF_ID_BILL    = 1003;
    private static final int NOTIF_ID_ALERT   = 2001;

    private final Context context;

    public NotificationHelper(Context context) {
        this.context = context.getApplicationContext();
        createChannels();
    }

    private void createChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = (NotificationManager)
                    context.getSystemService(Context.NOTIFICATION_SERVICE);

            Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            AudioAttributes audioAttrs = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();

            // Transaction channel (debits, credits, bill payments)
            NotificationChannel txChannel = new NotificationChannel(
                    CHANNEL_TRANSACTIONS,
                    "Transaction Alerts",
                    NotificationManager.IMPORTANCE_HIGH);
            txChannel.setDescription("Notifies you for every debit and credit.");
            txChannel.setSound(soundUri, audioAttrs);
            txChannel.enableVibration(true);
            nm.createNotificationChannel(txChannel);

            // Security alert channel (suspicious transactions)
            NotificationChannel alertChannel = new NotificationChannel(
                    CHANNEL_ALERTS,
                    "Security Alerts",
                    NotificationManager.IMPORTANCE_MAX);
            alertChannel.setDescription("Warns you about suspicious transactions.");
            alertChannel.setSound(soundUri, audioAttrs);
            alertChannel.enableVibration(true);
            alertChannel.setVibrationPattern(new long[]{0, 500, 200, 500});
            nm.createNotificationChannel(alertChannel);
        }
    }

    private PendingIntent getDashboardIntent() {
        Intent intent = new Intent(context, DashboardActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            flags |= PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getActivity(context, 0, intent, flags);
    }

    public void notifyDebit(String recipient, double amount) {
        showNotification(CHANNEL_TRANSACTIONS, NOTIF_ID_SEND,
                "Money Sent",
                String.format("₹%.2f sent to %s", amount, recipient));
    }

    public void notifyCredit(String sender, double amount) {
        showNotification(CHANNEL_TRANSACTIONS, NOTIF_ID_RECEIVE,
                "Money Received",
                String.format("₹%.2f received from %s", amount, sender));
    }

    public void notifyBillPaid(String category, double amount) {
        showNotification(CHANNEL_TRANSACTIONS, NOTIF_ID_BILL,
                "Bill Paid",
                String.format("%s bill of ₹%.2f paid successfully.", category, amount));
    }

    public void notifySuspicious(double amount) {
        showNotification(CHANNEL_ALERTS, NOTIF_ID_ALERT,
                "⚠ Suspicious Activity Detected",
                String.format("Large transfer of ₹%.2f flagged. Tap to review.", amount));
    }

    private void showNotification(String channel, int id,
                                  String title, String content) {
        Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context, channel)
                        .setSmallIcon(android.R.drawable.ic_dialog_info)
                        .setContentTitle(title)
                        .setContentText(content)
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(content))
                        .setAutoCancel(true)
                        .setContentIntent(getDashboardIntent())
                        .setSound(soundUri)
                        .setDefaults(NotificationCompat.DEFAULT_ALL)
                        .setPriority(channel.equals(CHANNEL_ALERTS)
                                ? NotificationCompat.PRIORITY_MAX
                                : NotificationCompat.PRIORITY_HIGH);

        NotificationManagerCompat.from(context).notify(id, builder.build());
    }
}