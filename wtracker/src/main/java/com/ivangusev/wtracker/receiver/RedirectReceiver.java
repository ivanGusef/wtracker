package com.ivangusev.wtracker.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.ivangusev.wtracker.Preference;
import com.ivangusev.wtracker.PreferenceManager;
import com.ivangusev.wtracker.activity.LoginActivity;
import com.ivangusev.wtracker.activity.MapActivity;

/**
 * Created by ivan on 19.02.14.
 */
public class RedirectReceiver extends BroadcastReceiver {

    public static final String ACTION_REDIRECT = "com.ivangusev.wracker.action.REDIRECT";

    @Override
    public void onReceive(Context context, Intent intent) {
        final PreferenceManager mPreferenceManager = PreferenceManager.getInstance(context);
        final Intent activityIntent;
        if (mPreferenceManager.getBoolean(Preference.LOGGED_IN)) {
            activityIntent = new Intent(context, LoginActivity.class);
        } else {
            activityIntent = new Intent(context, MapActivity.class);
        }
        activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(activityIntent);
    }
}
