package com.ivangusev.wtracker.activity;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.ActionBarActivity;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.ivangusev.wtracker.Preference;
import com.ivangusev.wtracker.PreferenceManager;
import com.ivangusev.wtracker.R;
import com.ivangusev.wtracker.service.TrackerBinder;
import com.ivangusev.wtracker.service.TrackerService;
import junit.framework.Assert;

/**
 * Created by ivan on 12.02.14.
 */
public class MapActivity extends ActionBarActivity implements TrackerBinder.Receiver {

    private static final String TAG = MapActivity.class.getName();
    private static final int DEFAULT_ZOOM = 12;

    private PreferenceManager mPreferenceManager;

    private TrackerBinder mBinder;
    private boolean mConnected;
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mBinder = (TrackerBinder) service;
            mBinder.registerReceiver(TAG.hashCode(), MapActivity.this);

            mBinder.notifyMyLocation();
            mBinder.notifyUpdatePoints();

            mConnected = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    };

    private final SparseArray<Point> mPoints = new SparseArray<Point>();

    private GoogleMap mGoogleMap;
    private Marker mMyLocation;
    private boolean mFirstStart = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        mPreferenceManager = PreferenceManager.getInstance(this);

        final SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map_fragment);
        mGoogleMap = mapFragment.getMap();
    }

    @Override
    protected void onStart() {
        super.onStart();
        bindService(new Intent(this, TrackerService.class), mConnection, 0);
    }

    @Override
    protected void onStop() {
        if (mConnected) {
            mBinder.unregisterReceiver(TAG.hashCode());
            unbindService(mConnection);
        }
        super.onStop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_map, menu);

        final MenuItem item = menu.findItem(R.id.mi_change_connection);
        Assert.assertNotNull(item);
        if (mPreferenceManager.getBoolean(Preference.SERVICE_ACTIVE)) {
            item.setIcon(R.drawable.ic_action_cancel);
            item.setTitle(R.string.stop_tracking);
        } else {
            item.setIcon(R.drawable.ic_action_import_export);
            item.setTitle(R.string.start_tracking);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.mi_change_connection) {
            if(mBinder == null) {
                Toast.makeText(this, R.string.wait_for_connection, Toast.LENGTH_SHORT).show();
                return true;
            }
            final boolean serviceActive = mPreferenceManager.getBoolean(Preference.SERVICE_ACTIVE);
            mPreferenceManager.save(Preference.SERVICE_ACTIVE, !serviceActive);

            if (serviceActive) mBinder.disconnect(false);
            else mBinder.reconnect();

            supportInvalidateOptionsMenu();
            return true;
        } else if (item.getItemId() == R.id.mi_sign_out) {
            final String login = mPreferenceManager.getString(Preference.LOGIN);
            mPreferenceManager.clear();
            mPreferenceManager.save(Preference.LOGIN, login);

            startActivity(new Intent(this, LoginActivity.class));
            stopService(new Intent(this, TrackerService.class));
            finish();
            return true;
        }
        return false;
    }

    @Override
    public void onConnectionEstablished() {

    }

    @Override
    public void onConnectionFailed(int code, String reason) {
        Toast.makeText(this, reason, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onUpdatePoints(SparseArray<LatLng> points) {
        final int pointsCount = points.size();

        Point point;
        int key;
        for (int i = 0; i < pointsCount; i++) {
            key = points.keyAt(i);
            point = mPoints.get(key);
            if (point == null) point = new Point(points.get(key));
            else point.position = points.get(key);
            mPoints.put(key, point);
            addOrUpdatePoint(point);
        }
    }

    @Override
    public void onUpdateMyLocation(LatLng point) {
        if (mMyLocation == null) {
            final MarkerOptions options = new MarkerOptions();
            options.position(point);
            options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
            mMyLocation = mGoogleMap.addMarker(options);
        } else {
            mMyLocation.setPosition(point);
        }
        if (mFirstStart) {
            mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(point, DEFAULT_ZOOM));
            mFirstStart = !mFirstStart;
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

    static class Point {
        Marker marker;
        LatLng position;

        Point(LatLng position) {
            this.position = position;
        }
    }
}
