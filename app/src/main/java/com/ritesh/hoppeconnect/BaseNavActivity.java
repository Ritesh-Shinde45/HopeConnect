package com.ritesh.hoppeconnect;

import android.content.Intent;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;


public abstract class BaseNavActivity extends AppCompatActivity {

    
    protected boolean isHomeTab() { return false; }

    @Override
    protected void onStart() {
        super.onStart();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (!isHomeTab()) {
                   
                    Intent intent = new Intent(BaseNavActivity.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    startActivity(intent);
                    finish();
                } else {
                   
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }
}