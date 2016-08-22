package com.example.roman.test;

import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.example.roman.test.utilities.Functions;

import javax.inject.Inject;

public class MessageActivity extends AppCompatActivity {
    @Inject
    SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ((TaxiApp) getApplication()).getNetComponent().inject(this);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            Functions.setWholeTheme(this, prefs);
        }

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_message);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.messages_container, MessageFragment.newInstance())
                    .commit();
        }
    }
}

