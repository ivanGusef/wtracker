package com.ivangusev.wtracker.service;

import android.location.Location;
import android.os.Binder;
import android.os.Bundle;
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

/**
 * Created by ivan on 13.02.14.
 */
public class TrackerBinder extends Binder implements GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener, LocationListener {

    private static final String TAG = TrackerBinder.class.getName();

    private final static int UPDATE_INTERVAL_IN_MILLISECONDS = 10000;
    private final static int FAST_INTERVAL_CEILING_IN_MILLISECONDS = 30000;
    private static final String WEB_SERVER_URL = "ws://mini-mdt.wheely.com?username=%s&password=%s";
    private static final int SOCKET_CONNECTION_TIMEOUT = 30000; //30 seconds
    private static final int SOCKET_RECEIVE_TIMEOUT = 10000; //10 seconds

    private final TrackerService mService;
    private final SparseArray<Receiver> mReceivers = new SparseArray<Receiver>();
    private final SparseArray<LatLng> mPoints = new SparseArray<LatLng>();
    private final WebSocketConnection mConnection = new WebSocketConnection();

    private final LocationClient mLocationClient;
    private final LocationRequest mLocationRequest;

    private LatLng mLastLocation;
    private boolean mLocationRequested;
    private PreferenceManager mPreferenceManager;

    public TrackerBinder(TrackerService mService) {
        this.mService = mService;

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
        if (mLastLocation == null) return;
        final int receiversCount = mReceivers.size();
        for (int i = 0; i < receiversCount; i++) {
            mReceivers.get(mReceivers.keyAt(i)).onUpdateMyLocation(mLastLocation);
        }
    }

    public void notifyUpdatePoints() {
        if (mPoints.size() == 0) return;
        final int receiversCount = mReceivers.size();
        for (int i = 0; i < receiversCount; i++) {
            mReceivers.get(mReceivers.keyAt(i)).onUpdatePoints(mPoints);
        }
    }

    public void connect(CharSequence login, CharSequence password) {
        if (mConnection.isConnected()) return;
        final WebSocketOptions options = new WebSocketOptions();
        options.setSocketConnectTimeout(SOCKET_CONNECTION_TIMEOUT);
        options.setSocketReceiveTimeout(SOCKET_RECEIVE_TIMEOUT);
        try {
            mConnection.connect(String.format(WEB_SERVER_URL, login, password), new DefaultWebSocketHandler(), options);
        } catch (WebSocketException e) {
            Log.e(TAG, e.getMessage());
            final int receiversCount = mReceivers.size();
            for (int i = 0; i < receiversCount; i++) {
                mReceivers.get(mReceivers.keyAt(i)).onConnectionFailed(-1, e.getMessage());
            }
        }
    }

    public void disconnect() {
        if (mConnection.isConnected()) mConnection.disconnect();
    }

    public void startLocationSending() {
        if (!mLocationClient.isConnected()) {
            mLocationRequested = true;
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
    public void onLocationChanged(Location location) {
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
    }

    private void startLocationUpdates() {
        mService.changeStatus(R.string.status_connected);
        mLocationClient.requestLocationUpdates(mLocationRequest, this);
    }

    private void stopLocationUpdates() {
        mService.changeStatus(R.string.status_disconnected);
        mLocationClient.removeLocationUpdates(this);
    }

    public void reconnect() {
        connect(mPreferenceManager.getString(Preference.LOGIN), mPreferenceManager.getString(Preference.PASSWORD));
    }

    class DefaultWebSocketHandler extends WebSocketHandler {

        @Override
        public void onOpen() {
            Log.d(TAG, "WebSocket opened");
            final int receiversCount = mReceivers.size();
            for (int i = 0; i < receiversCount; i++) {
                mReceivers.get(mReceivers.keyAt(i)).onConnectionEstablished();
            }
            startLocationSending();
        }

        @Override
        public void onClose(int code, String reason) {
            Log.d(TAG, "WebSocket closed {code: " + code + ", reason: " + reason + '}');
            final int receiversCount = mReceivers.size();
            for (int i = 0; i < receiversCount; i++) {
                mReceivers.get(mReceivers.keyAt(i)).onConnectionFailed(code, reason);
            }
            stopLocationSending();
        }

        @Override
        public void onTextMessage(String payload) {
            Log.d(TAG, "WebSocket received message {payload:" + payload + '}');
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
    }

    public static interface Receiver {
        void onConnectionEstablished();

        void onConnectionFailed(int code, String reason);

        void onUpdatePoints(SparseArray<LatLng> points);

        void onUpdateMyLocation(LatLng point);
    }
}
