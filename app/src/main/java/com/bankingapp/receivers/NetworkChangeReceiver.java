package com.bankingapp.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.widget.Toast;
import com.bankingapp.R;

/**
 * NetworkChangeReceiver — listens for connectivity changes.
 *
 * Registered in AndroidManifest.xml for:
 *   android.net.conn.CONNECTIVITY_CHANGE
 *
 * Syllabus: BroadcastReceiver — onReceive(), Context, Intent
 */
public class NetworkChangeReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        // Get connectivity service
        ConnectivityManager cm = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (cm == null) return;

        // Check active network
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = (activeNetwork != null && activeNetwork.isConnected());

        if (!isConnected) {
            // Device went OFFLINE — warn user
            Toast.makeText(context,
                    context.getString(R.string.receiver_offline_warning),
                    Toast.LENGTH_LONG).show();
        } else {
            // Device came back ONLINE
            Toast.makeText(context,
                    context.getString(R.string.receiver_online_restored),
                    Toast.LENGTH_SHORT).show();
        }
    }
}
