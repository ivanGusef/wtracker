package com.ivangusev.wtracker.client.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import com.ivangusev.wtracker.R;
import com.ivangusev.wtracker.activity.MapActivity;
import de.tavendo.autobahn.WebSocketConnection;
import de.tavendo.autobahn.WebSocketException;
import de.tavendo.autobahn.WebSocketHandler;
import de.tavendo.autobahn.WebSocketOptions;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by ivan on 12.02.14.
 */
public class TrackerService extends Service {

    private static final String TAG = TrackerService.class.getName();

    private static final String WEB_SERVER_URL = "ws://mini-mdt.wheely.com?username=%s&password=%s";
    private static final int SOCKET_CONNECTION_TIMEOUT = 30000; //30 seconds
    private static final int SOCKET_RECEIVE_TIMEOUT = 10000; //10 seconds

    private ForegroundCompatWrapper mForegroundCompatWrapper = new ForegroundCompatWrapper(this);
    private NotificationCompat.Builder mForegroundNotifBuilder;

    private final TrackerBinder mBinder = new TrackerBinder();

    private final WebSocketConnection mConnection = new WebSocketConnection();

    @Override
    public void onCreate() {
        mForegroundCompatWrapper.onCreate();

        final Notification n = prepareNotification(R.string.status_connecting);
        mForegroundCompatWrapper.startForegroundCompat(R.string.service_tracker_id, n);
    }

    @Override
    public void onDestroy() {
        mForegroundCompatWrapper.stopForegroundCompat(R.string.service_tracker_id);
        if (mConnection.isConnected()) mConnection.disconnect();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public void login(CharSequence login, CharSequence password) throws WebSocketException {
        final WebSocketOptions options = new WebSocketOptions();
        options.setSocketConnectTimeout(SOCKET_CONNECTION_TIMEOUT);
        options.setSocketReceiveTimeout(SOCKET_RECEIVE_TIMEOUT);
        mConnection.connect(String.format(WEB_SERVER_URL, login, password), new DefaultWebSocketHandler(), options);
    }

    private Notification prepareNotification(int textRes) {
        if (mForegroundNotifBuilder != null) return mForegroundNotifBuilder.setContentText(getString(textRes)).build();
        mForegroundNotifBuilder = new NotificationCompat.Builder(this);
        mForegroundNotifBuilder.setContentTitle(getString(R.string.app_name));
        mForegroundNotifBuilder.setContentText(getString(textRes));
        mForegroundNotifBuilder.setOngoing(true);
        mForegroundNotifBuilder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher));
        mForegroundNotifBuilder.setSmallIcon(android.R.drawable.stat_notify_sync);

        final Intent intent = new Intent(this, MapActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        final PendingIntent pIntent = PendingIntent.getActivity(this, 0, intent, 0);
        mForegroundNotifBuilder.setContentIntent(pIntent);

        return mForegroundNotifBuilder.build();
    }

    class DefaultWebSocketHandler extends WebSocketHandler {

        @Override
        public void onOpen() {
            Log.d(TAG, "WebSocket opened");
            final Map<String, Receiver> receivers = mBinder.getReceivers();
            for (String id : receivers.keySet()) {
                receivers.get(id).onOpen();
            }
        }

        @Override
        public void onClose(int code, String reason) {
            Log.d(TAG, "WebSocket closed: code - " + code + ", reason: " + reason);
            final Map<String, Receiver> receivers = mBinder.getReceivers();
            for (String id : receivers.keySet()) {
                receivers.get(id).onClose(code, reason);
            }
        }

        @Override
        public void onTextMessage(String payload) {
            Log.d(TAG, "WebSocket received message: " + payload);
            final Map<String, Receiver> receivers = mBinder.getReceivers();
            for (String id : receivers.keySet()) {
                receivers.get(id).onMessage(payload);
            }
        }
    }

    public class TrackerBinder extends Binder {

        private final Map<String, Receiver> mReceivers = new HashMap<String, Receiver>();

        public void login(CharSequence login, CharSequence password) throws WebSocketException {
            final WebSocketOptions options = new WebSocketOptions();
            options.setSocketConnectTimeout(SOCKET_CONNECTION_TIMEOUT);
            options.setSocketReceiveTimeout(SOCKET_RECEIVE_TIMEOUT);
            mConnection.connect(String.format(WEB_SERVER_URL, login, password), new DefaultWebSocketHandler(), options);
        }

        public void sendMessage(String message) {
            if(mConnection.isConnected()) mConnection.sendTextMessage(message);
        }

        public void registerReceiver(String id, Receiver receiver) {
            mReceivers.put(id, receiver);
        }

        public void unregisterReceiver(String id) {
            mReceivers.remove(id);
        }

        public Map<String, Receiver> getReceivers() {
            return mReceivers;
        }
    }

    public static interface Receiver {
        void onOpen();
        void onClose(int code, String reason);
        void onMessage(String text);
    }
}
