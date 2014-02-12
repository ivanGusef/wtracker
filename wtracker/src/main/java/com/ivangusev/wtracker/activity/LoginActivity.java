package com.ivangusev.wtracker.activity;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
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
import junit.framework.Assert;
import org.json.JSONObject;

/**
 * Created by ivan on 12.02.14.
 */
public class LoginActivity extends ActionBarActivity {

    private static final String TAG = LoginActivity.class.getName();

    private TextView mLoginInput;
    private TextView mPasswordInput;

    private TrackerService mService;
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = ((TrackerService.TrackerBinder) service).getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

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
        if (mService == null) {
            Toast.makeText(this, "Connecting to service. Wait please.", Toast.LENGTH_SHORT).show();
        } else {
            try {
                mService.login(mLoginInput.getText(), mPasswordInput.getText());
            } catch (WebSocketException e) {
                Log.e(TAG, e.getMessage());
            }
        }
    }

    public void send(View view) {
        final CharSequence lat = ((TextView) findViewById(R.id.lat_input)).getText();
        final CharSequence lon = ((TextView) findViewById(R.id.lon_input)).getText();
        JSONObject jsonObject = new JSONObject();
        try {
            Assert.assertNotNull(lat);
            jsonObject.put("lat", Double.valueOf(lat.toString()));
            Assert.assertNotNull(lon);
            jsonObject.put("lon", Double.valueOf(lon.toString()));
            mService.sendMessage(jsonObject.toString());
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }
}
