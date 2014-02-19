package com.ivangusev.wtracker.service;

import android.location.Location;
import android.os.*;
import android.util.Log;
import android.util.SparseArray;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.model.LatLng;
import com.ivangusev.wtracker.Preference;
import com.ivangusev.wtracker.PreferenceManager;
import com.ivangusev.wtracker.R;
import de.tavendo.autobahn.WebSocketConnection;
import de.tavendo.autobahn.WebSocketException;
import de.tavendo.autobahn.WebSocketHandler;
import de.tavendo.autobahn.WebSocketOptions;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by ivan on 13.02.14.
 */
public class TrackerBinder extends Binder implements GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener, LocationListener, Handler.Callback {

    private static final String TAG = TrackerBinder.class.getName();

    private final static int UPDATE_INTERVAL_IN_MILLISECONDS = 10000;
    private final static int FAST_INTERVAL_CEILING_IN_MILLISECONDS = 30000;
    private static final int SOCKET_CONNECTION_TIMEOUT = 15000; //15 seconds
    private static final int SOCKET_RECEIVE_TIMEOUT = 10000; //10 seconds

    private static final String WEB_SERVER_URL = "ws://mini-mdt.wheely.com?username=%s&password=%s";

    private static final int MSG_WEB_SOCKET_EXC = 1;
    private static final int MSG_ON_CONNECTION_ESTABLISHED = 2;
    private static final int MSG_ON_CONNECTION_FAILED = 3;
    private static final int MSG_ON_UPDATE_POINTS = 4;
    private static final int MSG_ON_UPDATE_MY_LOCATION = 5;

    private final Handler mHandler;
    private final ExecutorService mExecutorService = Executors.newSingleThreadExecutor();

    private final TrackerService mService;
    private final SparseArray<Receiver> mReceivers = new SparseArray<Receiver>();
    private final SparseArray<LatLng> mPoints = new SparseArray<LatLng>();

    private final WebSocketConnection mConnection = new WebSocketConnection();

    private final LocationClient mLocationClient;
    private final LocationRequest mLocationRequest;

    private LatLng mLastLocation;
    private boolean mLocationRequested;
    private PreferenceManager mPreferenceManager;
    private boolean mSilentDisconnect;

    private ConnectionResult mConnectionResult = null;

    public TrackerBinder(TrackerService mService) {
        this.mService = mService;

        mHandler = new Handler(Looper.getMainLooper(), this);

        mPreferenceManager = PreferenceManager.getInstance(mService);

        mLocationRequest = LocationRequest.create();
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setFastestInterval(FAST_INTERVAL_CEILING_IN_MILLISECONDS);

        mLocationClient = new LocationClient(mService, this, this);
        mLocationClient.connect();
    }

    public void registerReceiver(int id, Receiver receiver) {
        mReceivers.put(id, receiver);
    }

    public void unregisterReceiver(int id) {
        mReceivers.delete(id);
    }

    public void notifyMyLocation() {
        mHandler.sendEmptyMessage(MSG_ON_UPDATE_MY_LOCATION);
    }

    public void notifyUpdatePoints() {
        mHandler.sendEmptyMessage(MSG_ON_UPDATE_POINTS);
    }

    public void connect(final CharSequence login, final CharSequence password) {
        mExecutorService.submit(new Runnable() {
            @Override
            public void run() {
                if (mConnection.isConnected()) return;
                final WebSocketOptions options = new WebSocketOptions();
                options.setSocketConnectTimeout(SOCKET_CONNECTION_TIMEOUT);
                options.setSocketReceiveTimeout(SOCKET_RECEIVE_TIMEOUT);
                try {
                    mConnection.connect(String.format(WEB_SERVER_URL, login, password), new DefaultWebSocketHandler(), options);
                } catch (WebSocketException e) {
                    Log.e(TAG, e.getMessage());
                    final Message msg = new Message();
                    msg.what = MSG_WEB_SOCKET_EXC;
                    msg.obj = e.getMessage();
                    mHandler.sendMessage(msg);
                }
            }
        });
    }

    public void reconnect() {
        connect(mPreferenceManager.getString(Preference.LOGIN), mPreferenceManager.getString(Preference.PASSWORD));
    }

    public void disconnect(boolean silent) {
        if (mConnection.isConnected()) {
            mConnection.disconnect();
            mSilentDisconnect = silent;
        }
    }

    public void startLocationSending() {
        if (mLocationClient.isConnecting()) {
            mLocationRequested = true;
            return;
        }

        if(!mLocationClient.isConnected()) {
            onGooglePlayServicesConnectionFailed();
            return;
        }

        startLocationUpdates();

        final Location lastLocation = mLocationClient.getLastLocation();
        if (lastLocation != null) onLocationChanged(lastLocation);
    }

    public void stopLocationSending() {
        if (!mLocationClient.isConnected()) return;

        stopLocationUpdates();
    }

    @Override
    public void onLocationChanged(final Location location) {
        mExecutorService.submit(new Runnable() {
            @Override
            public void run() {
                mLastLocation = new LatLng(location.getLatitude(), location.getLongitude());
                try {
                    final JSONObject jLocation = new JSONObject();
                    jLocation.put("lat", mLastLocation.latitude);
                    jLocation.put("lon", mLastLocation.longitude);
                    mConnection.sendTextMessage(jLocation.toString());
                } catch (JSONException e) {
                    Log.e(TAG, e.getMessage());
                }
                notifyMyLocation();
            }
        });
    }

    @Override
    public void onConnected(Bundle bundle) {
        if (mLocationRequested) startLocationUpdates();
    }

    @Override
    public void onDisconnected() {
        Log.e(TAG, "Location Client disconnected");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.e(TAG, "Connection failed {errorCode:" + connectionResult.getErrorCode() + '}');
        mConnectionResult = connectionResult;
        if(mConnection.isConnected()) {
            onGooglePlayServicesConnectionFailed();
        }
    }

    private void startLocationUpdates() {
        mService.changeStatus(R.string.status_connected);
        mLocationClient.requestLocationUpdates(mLocationRequest, this);
    }

    private void stopLocationUpdates() {
        mService.changeStatus(R.string.status_disconnected);
        mLocationClient.removeLocationUpdates(this);
    }

    private void onGooglePlayServicesConnectionFailed() {
        if(mConnectionResult == null) return;
        final Message msg = new Message();
        msg.what = MSG_ON_CONNECTION_FAILED;
        msg.obj = mService.getString(R.string.e_google_play_services_conn_failed, mConnectionResult.getErrorCode());
        mHandler.sendMessage(msg);
        mConnection.disconnect();
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case MSG_WEB_SOCKET_EXC:
                int receiversCount = mReceivers.size();
                for (int i = 0; i < receiversCount; i++) {
                    mReceivers.get(mReceivers.keyAt(i)).onConnectionFailed(-1, String.valueOf(msg.obj));
                }
                return true;
            case MSG_ON_CONNECTION_ESTABLISHED:
                receiversCount = mReceivers.size();
                for (int i = 0; i < receiversCount; i++) {
                    mReceivers.get(mReceivers.keyAt(i)).onConnectionEstablished();
                }
                return true;
            case MSG_ON_CONNECTION_FAILED:
                receiversCount = mReceivers.size();
                for (int i = 0; i < receiversCount; i++) {
                    mReceivers.get(mReceivers.keyAt(i)).onConnectionFailed(msg.arg1, String.valueOf(msg.obj));
                }
                return true;
            case MSG_ON_UPDATE_POINTS:
                if (mPoints.size() == 0) return true;
                receiversCount = mReceivers.size();
                for (int i = 0; i < receiversCount; i++) {
                    mReceivers.get(mReceivers.keyAt(i)).onUpdatePoints(mPoints);
                }
                return true;
            case MSG_ON_UPDATE_MY_LOCATION:
                if (mLastLocation == null) return true;
                receiversCount = mReceivers.size();
                for (int i = 0; i < receiversCount; i++) {
                    mReceivers.get(mReceivers.keyAt(i)).onUpdateMyLocation(mLastLocation);
                }
                return true;
            default:
                return false;
        }
    }

    class DefaultWebSocketHandler extends WebSocketHandler {

        @Override
        public void onOpen() {
            Log.d(TAG, "WebSocket opened");
            mHandler.sendEmptyMessage(MSG_ON_CONNECTION_ESTABLISHED);
            startLocationSending();
        }

        @Override
        public void onClose(int code, String reason) {
            Log.d(TAG, "WebSocket closed {code: " + code + ", reason: " + reason + '}');
            if (!mSilentDisconnect) {
                final Message msg = new Message();
                msg.what = MSG_ON_CONNECTION_FAILED;
                msg.arg1 = code;
                msg.obj = reason;
                mHandler.sendMessage(msg);
            } else {
                mSilentDisconnect = false;
            }
            stopLocationSending();
        }

        @Override
        public void onTextMessage(final String payload) {
            Log.d(TAG, "WebSocket received message {payload:" + payload + '}');
            mExecutorService.submit(new Runnable() {
                @Override
                public void run() {
                    if (payload == null) return;
                    try {
                        final JSONArray jPoints = new JSONArray(payload);
                        final int jPointsLength = jPoints.length();

                        JSONObject jPoint;
                        LatLng position;
                        int id;
                        for (int i = 0; i < jPointsLength; i++) {
                            jPoint = jPoints.getJSONObject(i);
                            id = jPoint.getInt("id");
                            position = new LatLng(jPoint.getDouble("lat"), jPoint.getDouble("lon"));
                            mPoints.put(id, position);
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, e.getMessage());
                    }
                    notifyUpdatePoints();
                }
            });
        }
    }

    public static interface Receiver {
        void onConnectionEstablished();

        void onConnectionFailed(int code, String reason);

        void onUpdatePoints(SparseArray<LatLng> points);

        void onUpdateMyLocation(LatLng point);
    }
}
