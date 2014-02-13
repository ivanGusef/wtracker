package com.ivangusev.wtracker.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.ivangusev.wtracker.Preference;
import com.ivangusev.wtracker.PreferenceManager;
import com.ivangusev.wtracker.service.TrackerService;
import com.ivangusev.wtracker.utils.ConnectivityUtils;

/**
 * Created by ivan on 13.02.14.
 */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        final PreferenceManager manager = PreferenceManager.getInstance(context);
        if(!manager.getBoolean(Preference.SERVICE_ACTIVE)) return;

        if(ConnectivityUtils.isConnected(context)) {
            final Intent serviceIntent = new Intent(context, TrackerService.class);
            serviceIntent.putExtra(TrackerService.EXTRA_CONNECT_FORCE, true);
            context.startService(serviceIntent);
        }
    }
}
