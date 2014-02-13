package com.ivangusev.wtracker;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by ivan on 13.02.14.
 */
public class PreferenceManager {

    public static final String PREFERENCES_NAME = "wheely_preferences";

    private static PreferenceManager sInstance;

    public static PreferenceManager getInstance(Context context) {
        if (sInstance == null) sInstance = new PreferenceManager(context.getApplicationContext());
        return sInstance;
    }

    private final Context mContext;
    private final SharedPreferences mSharedPreferences;

    private PreferenceManager(Context mContext) {
        this.mContext = mContext;
        this.mSharedPreferences = mContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
    }

    public void save(String key, String value) {
        edit().putString(key, value).commit();
    }

    public void save(String key, int value) {
        edit().putInt(key, value).commit();
    }

    public void save(String key, float value) {
        edit().putFloat(key, value).commit();
    }

    public void save(String key, boolean value) {
        edit().putBoolean(key, value).commit();
    }

    public String getString(String key, String defValue) {
        return mSharedPreferences.getString(key, defValue);
    }

    public int getInt(String key, int defValue) {
        return mSharedPreferences.getInt(key, defValue);
    }

    public float getFloat(String key, float defValue) {
        return mSharedPreferences.getFloat(key, defValue);
    }

    public boolean getBoolean(String key, boolean defValue) {
        return mSharedPreferences.getBoolean(key, defValue);
    }

    public String getString(String key) {
        return mSharedPreferences.getString(key, "");
    }

    public int getInt(String key) {
        return mSharedPreferences.getInt(key, 0);
    }

    public float getFloat(String key) {
        return mSharedPreferences.getFloat(key, 0.0f);
    }

    public boolean getBoolean(String key) {
        return mSharedPreferences.getBoolean(key, false);
    }

    public void clear() {
        mSharedPreferences.edit().clear().commit();
    }

    public SharedPreferences.Editor edit() {
        return mSharedPreferences.edit();
    }
}
