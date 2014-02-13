package com.ivangusev.wtracker.activity;

import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.ActionBarActivity;
import android.util.SparseArray;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.maps.model.LatLng;
import com.ivangusev.wtracker.Preference;
import com.ivangusev.wtracker.PreferenceManager;
import com.ivangusev.wtracker.R;
import com.ivangusev.wtracker.service.TrackerBinder;
import com.ivangusev.wtracker.service.TrackerService;
import de.tavendo.autobahn.WebSocketHandler;

/**
 * Created by ivan on 12.02.14.
 */
public class LoginActivity extends ActionBarActivity implements TrackerBinder.Receiver {

    private static final String TAG = LoginActivity.class.getName();

    private TextView mLoginInput;
    private TextView mPasswordInput;

    private ProgressDialog mProgress;

    private PreferenceManager mPreferenceManager;

    private TrackerBinder mBinder;
    private boolean mConnected;
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mBinder = (TrackerBinder) service;
            mBinder.registerReceiver(TAG.hashCode(), LoginActivity.this);

            mConnected = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mLoginInput = (TextView) findViewById(R.id.input_login);
        mPasswordInput = (TextView) findViewById(R.id.input_password);

        mPreferenceManager = PreferenceManager.getInstance(this);
        mLoginInput.setText(mPreferenceManager.getString(Preference.LOGIN));

        mProgress = new ProgressDialog(this);
        mProgress.setMessage(getString(R.string.authenticating));
    }

    @Override
    protected void onStart() {
        super.onStart();
        bindService(new Intent(this, TrackerService.class), mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        if (mConnected) {
            mBinder.unregisterReceiver(TAG.hashCode());
            unbindService(mConnection);
        }
        super.onStop();
    }

    public void signIn(View view) {
        if (mBinder == null) {
            Toast.makeText(this, "Connecting to service. Wait please.", Toast.LENGTH_SHORT).show();
        } else {
            final CharSequence login = mLoginInput.getText();
            final CharSequence password = mPasswordInput.getText();

            mPreferenceManager.save(Preference.LOGIN, String.valueOf(login));
            mPreferenceManager.save(Preference.PASSWORD, String.valueOf(password));

            mBinder.connect(login, password);
            mProgress.show();
        }
    }

    @Override
    public void onConnectionEstablished() {

    }

    @Override
    public void onConnectionFailed(int code, String reason) {
        mProgress.dismiss();
        switch (code) {
            case WebSocketHandler.CLOSE_CONNECTION_LOST:
                Toast.makeText(this, "Login or password is incorrect", Toast.LENGTH_SHORT).show();
                break;
            case WebSocketHandler.CLOSE_INTERNAL_ERROR:
            case WebSocketHandler.CLOSE_PROTOCOL_ERROR:
            case WebSocketHandler.CLOSE_CANNOT_CONNECT:
                Toast.makeText(this, reason, Toast.LENGTH_SHORT).show();
                break;

        }
    }

    @Override
    public void onUpdatePoints(SparseArray<LatLng> points) {
        mProgress.dismiss();
        mPreferenceManager.save(Preference.SERVICE_ACTIVE, true);
        startService(new Intent(this, TrackerService.class));
        final Intent intent = new Intent(this, MapActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        } else {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        }
        startActivity(intent);
        mBinder.unregisterReceiver(TAG.hashCode());
    }

    @Override
    public void onUpdateMyLocation(LatLng point) {

    }
}
