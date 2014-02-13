package com.ivangusev.wtracker.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import com.ivangusev.wtracker.Preference;
import com.ivangusev.wtracker.PreferenceManager;
import com.ivangusev.wtracker.R;
import junit.framework.Assert;

/**
 * Created by ivan on 13.02.14.
 */
public class SplashActivity extends Activity implements Animation.AnimationListener {

    public static final String EXTRA_REDIRECT_IMMEDIATELY = "extra_redirect_immediately";

    private PreferenceManager mPreferenceManager;

    private View content;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        mPreferenceManager = PreferenceManager.getInstance(this);

        if(getIntent().hasExtra(EXTRA_REDIRECT_IMMEDIATELY)
                && getIntent().getBooleanExtra(EXTRA_REDIRECT_IMMEDIATELY, false)) {
            redirect();
            return;
        }
        content = findViewById(R.id.content);
        final Animation anim = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        Assert.assertNotNull(anim);

        anim.setAnimationListener(this);
        content.startAnimation(anim);
    }

    @Override
    public void onAnimationStart(Animation animation) {

    }

    @Override
    public void onAnimationEnd(Animation animation) {
        content.clearAnimation();
        redirect();
    }

    @Override
    public void onAnimationRepeat(Animation animation) {

    }

    private void redirect() {
        final Intent intent;
        if(mPreferenceManager.getBoolean(Preference.SERVICE_ACTIVE)) {
            intent = new Intent(this, MapActivity.class);
        } else {
            intent = new Intent(this, LoginActivity.class);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
    }
}
