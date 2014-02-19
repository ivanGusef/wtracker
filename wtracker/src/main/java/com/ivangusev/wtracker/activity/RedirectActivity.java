package com.ivangusev.wtracker.activity;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.ivangusev.wtracker.Preference;
import com.ivangusev.wtracker.PreferenceManager;
import com.ivangusev.wtracker.R;

/**
 * Created by ivan on 13.02.14.
 */
public class RedirectActivity extends Activity {

    public static final int RC_GPSERVICES_ERROR = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_redirect);

        final int playServicesAvailable = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);

        if (playServicesAvailable != ConnectionResult.SUCCESS) {
            final Dialog errDialog = GooglePlayServicesUtil.getErrorDialog(playServicesAvailable, this, RC_GPSERVICES_ERROR);
            errDialog.show();
        } else {
            final PreferenceManager mPreferenceManager = PreferenceManager.getInstance(this);

            final Intent intent;
            if (mPreferenceManager.getBoolean(Preference.LOGGED_IN)) {
                intent = new Intent(this, MapActivity.class);
            } else {
                intent = new Intent(this, LoginActivity.class);
            }
            startActivity(intent);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == RC_GPSERVICES_ERROR) {
            finish();
        }
    }
}
