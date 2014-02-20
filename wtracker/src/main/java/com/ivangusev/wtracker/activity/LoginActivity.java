package com.ivangusev.wtracker.activity;

import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.ActionBarActivity;
import android.text.TextUtils;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
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

        mPreferenceManager = PreferenceManager.getInstance(this);

        mLoginInput = (TextView) findViewById(R.id.input_login);
        mPasswordInput = (TextView) findViewById(R.id.input_password);

        mLoginInput.setText(mPreferenceManager.getString(Preference.LOGIN));

        mPasswordInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (event == null) return false;
                if (event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN) {
                    signIn();
                    return true;
                }
                return false;
            }
        });

        mProgress = new ProgressDialog(this);
        mProgress.setMessage(getString(R.string.authenticating));
        mProgress.setCancelable(false);
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
        if (mProgress.isShowing()) mProgress.dismiss();
        super.onStop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_sign_in, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() != R.id.mi_sign_in) return false;
        signIn();
        return true;
    }

    @Override
    public void onConnectionEstablished() {

    }

    @Override
    public void onConnectionFailed(int code, String reason) {
        if (mProgress.isShowing()) mProgress.dismiss();
        switch (code) {
            case WebSocketHandler.CLOSE_CONNECTION_LOST:
                Toast.makeText(this, "Login or password is incorrect", Toast.LENGTH_LONG).show();
                break;
            case WebSocketHandler.CLOSE_INTERNAL_ERROR:
            case WebSocketHandler.CLOSE_PROTOCOL_ERROR:
            case WebSocketHandler.CLOSE_CANNOT_CONNECT:
                Toast.makeText(this, reason, Toast.LENGTH_LONG).show();
                break;

        }
    }

    @Override
    public void onUpdatePoints(SparseArray<LatLng> points) {
        if (mProgress.isShowing()) mProgress.dismiss();
        mPreferenceManager.save(Preference.SERVICE_ACTIVE, true);
        mPreferenceManager.save(Preference.LOGGED_IN, true);

        final Intent serviceIntent = new Intent(this, TrackerService.class);
        serviceIntent.putExtra(TrackerService.EXTRA_FOREGROUND, true);
        startService(serviceIntent);
        startActivity(new Intent(this, MapActivity.class));
        mBinder.unregisterReceiver(TAG.hashCode());
    }

    @Override
    public void onUpdateMyLocation(LatLng point) {

    }

    private void signIn() {
        if (mBinder == null) {
            Toast.makeText(this, R.string.wait_for_connection, Toast.LENGTH_SHORT).show();
            return;
        }
        if (!validate(mLoginInput, mPasswordInput)) {
            return;
        }
        final CharSequence login = mLoginInput.getText();
        final CharSequence password = mPasswordInput.getText();

        mPreferenceManager.save(Preference.LOGIN, String.valueOf(login));
        mPreferenceManager.save(Preference.PASSWORD, String.valueOf(password));

        mBinder.connect(login, password);
        mProgress.show();
    }

    private boolean validate(TextView... fields) {
        if (fields == null) return true;
        boolean valid = true;
        for (TextView field : fields) {
            if (field != null && !TextUtils.isGraphic(String.valueOf(field.getText()))) {
                field.setError("Required value");
                valid = false;
            }
        }
        return valid;
    }
}
