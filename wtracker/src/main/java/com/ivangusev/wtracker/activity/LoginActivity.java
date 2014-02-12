package com.ivangusev.wtracker.activity;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import com.ivangusev.wtracker.R;
import com.ivangusev.wtracker.client.service.TrackerService;
import de.tavendo.autobahn.WebSocketException;

/**
 * Created by ivan on 12.02.14.
 */
public class LoginActivity extends ActionBarActivity implements TrackerService.Receiver {

    private static final String TAG = LoginActivity.class.getName();

    private TextView mLoginInput;
    private TextView mPasswordInput;

    private TrackerService.TrackerBinder mBinder;
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mBinder = (TrackerService.TrackerBinder) service;
            mBinder.registerReceiver(TAG, LoginActivity.this);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBinder.unregisterReceiver(TAG);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        startService(new Intent(this, TrackerService.class));

        mLoginInput = (TextView) findViewById(R.id.input_login);
        mPasswordInput = (TextView) findViewById(R.id.input_password);
    }

    @Override
    protected void onStart() {
        super.onStart();
        bindService(new Intent(this, TrackerService.class), mConnection, 0);
    }

    @Override
    protected void onStop() {
        unbindService(mConnection);
        super.onStop();
    }

    public void signIn(View view) {
        if (mBinder == null) {
            Toast.makeText(this, "Connecting to service. Wait please.", Toast.LENGTH_SHORT).show();
        } else {
            try {
                mBinder.login(mLoginInput.getText(), mPasswordInput.getText());
            } catch (WebSocketException e) {
                Log.e(TAG, e.getMessage());
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onOpen() {
        final Intent intent = new Intent(this, MapActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        } else {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        }
        startActivity(intent);
    }

    @Override
    public void onClose(int code, String reason) {
        Toast.makeText(this, reason, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onMessage(String text) {
        //nothing
    }
}
