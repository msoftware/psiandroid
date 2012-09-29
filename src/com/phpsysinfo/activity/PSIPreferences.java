package com.phpsysinfo.activity;

import com.phpsysinfo.R;

import android.os.Bundle;
import android.preference.PreferenceActivity;


public class PSIPreferences extends PreferenceActivity {

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.prefs);
    }
}
