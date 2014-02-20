package com.ivangusev.wtracker.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import com.ivangusev.wtracker.R;
import com.ivangusev.wtracker.activity.MapActivity;

/**
 * Created by ivan on 12.02.14.
 */
public class TrackerService extends Service {

    public static final String EXTRA_CONNECT_FORCE = "extra_connect_force";
    public static final String EXTRA_FOREGROUND = "extra_foreground";

    private ForegroundCompatWrapper mForegroundCompatWrapper = new ForegroundCompatWrapper(this);
    private NotificationCompat.Builder mForegroundNotifBuilder;

    private TrackerBinder mBinder;

    private boolean mForeground;
    private int mStatus;

    @Override
    public void onCreate() {
        mForegroundCompatWrapper.onCreate();

        mBinder = new TrackerBinder(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent.hasExtra(EXTRA_CONNECT_FORCE) && intent.getBooleanExtra(EXTRA_CONNECT_FORCE, false)) {
            mBinder.reconnect();
        }
        if (intent.hasExtra(EXTRA_FOREGROUND) && intent.getBooleanExtra(EXTRA_FOREGROUND, false)) {
            mForeground = true;
            changeStatus(mStatus);
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        mForegroundCompatWrapper.stopForegroundCompat(R.string.service_tracker_id);
        mBinder.disconnect(true);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public void changeStatus(int textRes) {
        if(!mForeground) {
            mStatus = textRes;
            return;
        }
        final Notification n = prepareNotification(textRes);
        mForegroundCompatWrapper.startForegroundCompat(R.string.service_tracker_id, n);
    }

    private Notification prepareNotification(int textRes) {
        if (mForegroundNotifBuilder != null) return mForegroundNotifBuilder.setContentText(getString(textRes)).build();
        mForegroundNotifBuilder = new NotificationCompat.Builder(this);
        mForegroundNotifBuilder.setContentTitle(getString(R.string.app_name));
        mForegroundNotifBuilder.setContentText(textRes != 0 ? getString(textRes) : "");
        mForegroundNotifBuilder.setOngoing(true);
        mForegroundNotifBuilder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher));
        mForegroundNotifBuilder.setSmallIcon(android.R.drawable.stat_notify_sync);

        final Intent intent = new Intent(this, MapActivity.class);
        final PendingIntent pIntent = PendingIntent.getActivity(this, 0, intent, 0);
        mForegroundNotifBuilder.setContentIntent(pIntent);

        return mForegroundNotifBuilder.build();
    }
}
