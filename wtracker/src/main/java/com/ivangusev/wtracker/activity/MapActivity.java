package com.ivangusev.wtracker.activity;

import android.app.Dialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.ivangusev.wtracker.R;
import com.ivangusev.wtracker.client.service.TrackerService;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by ivan on 12.02.14.
 */
public class MapActivity extends ActionBarActivity implements TrackerService.Receiver, Handler.Callback,
        GooglePlayServicesClient.ConnectionCallbacks, GooglePlayServicesClient.OnConnectionFailedListener,
        LocationListener {

    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
    private final static int UPDATE_INTERVAL_IN_MILLISECONDS = 5000;
    private final static int FAST_INTERVAL_CEILING_IN_MILLISECONDS = 10000;

    private static final String TAG = MapActivity.class.getName();
    private static final int MSG_POINTS_RECEIVED = 1;

    private LocationClient mLocationClient;
    private LocationRequest mLocationRequest;

    private TrackerService.TrackerBinder mBinder;
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mBinder = (TrackerService.TrackerBinder) service;
            mBinder.registerReceiver(TAG, MapActivity.this);

            mLocationClient.connect();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBinder.unregisterReceiver(TAG);

            if (mLocationClient.isConnected()) mLocationClient.removeLocationUpdates(MapActivity.this);
            mLocationClient.disconnect();
        }
    };

    private final SparseArray<Point> mPoints = new SparseArray<Point>();
    private final Handler mHandler = new Handler(this);

    private GoogleMap mGoogleMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        final SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map_fragment);
        mGoogleMap = mapFragment.getMap();

        mLocationRequest = LocationRequest.create();
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setFastestInterval(FAST_INTERVAL_CEILING_IN_MILLISECONDS);

        mLocationClient = new LocationClient(this, this, this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        //bindService(new Intent(this, TrackerService.class), mConnection, 0);
    }

    @Override
    protected void onStop() {
        unbindService(mConnection);
        super.onStop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_map, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() != R.id.mi_change_connection) return false;
        bindService(new Intent(this, TrackerService.class), mConnection, 0);
        return true;
    }

    @Override
    public void onOpen() {
        //nothing
    }

    @Override
    public void onClose(int code, String reason) {
        Toast.makeText(this, reason, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onMessage(String text) {
        try {
            final JSONArray jPoints = new JSONArray(text);
            final int jPointsLength = jPoints.length();

            JSONObject jPoint;
            Point point;
            LatLng position;
            int id;
            for (int i = 0; i < jPointsLength; i++) {
                jPoint = jPoints.getJSONObject(i);
                id = jPoint.getInt("id");
                position = new LatLng(jPoint.getDouble("lat"), jPoint.getDouble("lon"));
                if (mPoints.get(id) != null) {
                    point = mPoints.get(id);
                } else {
                    point = new Point();
                    mPoints.put(id, point);
                }
                point.position = position;
            }
            mHandler.sendEmptyMessage(MSG_POINTS_RECEIVED);
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage());
        }
    }

    @Override
    public boolean handleMessage(Message msg) {
        if (msg.what != MSG_POINTS_RECEIVED) return false;
        final int pointsCount = mPoints.size();
        for (int i = 0; i < pointsCount; i++) {
            addOrUpdatePoint(mPoints.get(mPoints.keyAt(i)));
        }
        return true;
    }

    @Override
    public void onConnected(Bundle bundle) {
        final Location lastLocation = mLocationClient.getLastLocation();
        if (lastLocation != null) onLocationChanged(lastLocation);

        mLocationClient.requestLocationUpdates(mLocationRequest, this);
    }

    @Override
    public void onDisconnected() {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if (connectionResult.hasResolution()) {
            try {
                connectionResult.startResolutionForResult(this, CONNECTION_FAILURE_RESOLUTION_REQUEST);
            } catch (IntentSender.SendIntentException e) {
                Log.e(TAG, e.getMessage());
            }
        } else {
            showErrorDialog(connectionResult.getErrorCode());
        }

    }

    @Override
    public void onLocationChanged(Location location) {
        try {
            final JSONObject jLocation = new JSONObject();
            jLocation.put("lat", location.getLatitude());
            jLocation.put("lon", location.getLongitude());
            mBinder.sendMessage(jLocation.toString());
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage());
        }
    }

    private void addOrUpdatePoint(Point point) {
        if (point.marker == null) {
            final MarkerOptions options = new MarkerOptions();
            options.position(point.position);
            options.icon(BitmapDescriptorFactory.defaultMarker());
            point.marker = mGoogleMap.addMarker(options);
        } else {
            point.marker.setPosition(point.position);
        }
    }

    private void showErrorDialog(int errorCode) {

        final Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(errorCode, this, CONNECTION_FAILURE_RESOLUTION_REQUEST);

        if (errorDialog != null) {
            final ErrorDialogFragment errorFragment = new ErrorDialogFragment();
            errorFragment.setDialog(errorDialog);
            errorFragment.show(getSupportFragmentManager(), TAG);
        }
    }

    static class Point {
        Marker marker;
        LatLng position;
    }

    static class ErrorDialogFragment extends DialogFragment {

        private Dialog mDialog;

        public ErrorDialogFragment() {
            super();
            mDialog = null;
        }

        public void setDialog(Dialog dialog) {
            mDialog = dialog;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return mDialog;
        }
    }
}
