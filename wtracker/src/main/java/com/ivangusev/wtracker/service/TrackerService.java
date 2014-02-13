package com.ivangusev.wtracker.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import com.ivangusev.wtracker.R;

/**
 * Created by ivan on 12.02.14.
 */
public class TrackerService extends Service {

    private static final String TAG = TrackerService.class.getName();

    public static final String EXTRA_CONNECT_FORCE = "extra_connect_force";

    private ForegroundCompatWrapper mForegroundCompatWrapper = new ForegroundCompatWrapper(this);
    private NotificationCompat.Builder mForegroundNotifBuilder;

    private TrackerBinder mBinder;

    @Override
    public void onCreate() {
        mForegroundCompatWrapper.onCreate();

        changeStatus(R.string.status_disconnected);
        mBinder = new TrackerBinder(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent.hasExtra(EXTRA_CONNECT_FORCE) && intent.getBooleanExtra(EXTRA_CONNECT_FORCE, false))
            mBinder.reconnect();
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

        final Intent intent = new Intent(getPackageName() + ".ACTION_FAKE");
        final PendingIntent pIntent = PendingIntent.getBroadcast(this, 0, intent, 0);
        mForegroundNotifBuilder.setContentIntent(pIntent);

        return mForegroundNotifBuilder.build();
    }
}
