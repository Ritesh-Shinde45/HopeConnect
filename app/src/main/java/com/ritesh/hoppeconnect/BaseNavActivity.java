package com.ritesh.hoppeconnect;

import android.content.Intent;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Base class for all bottom-nav tab activities (Explore, Chats, Profile).
 *
 * Back press behaviour:
 *   • If not on home tab → go to home (no back stack chain)
 *   • If already on home tab → double-press to exit
 *
 * MainActivity extends this too, but overrides the double-press logic itself.
 */
public abstract class BaseNavActivity extends AppCompatActivity {

    /** Override in subclasses that ARE the home tab (MainActivity). */
    protected boolean isHomeTab() { return false; }

    @Override
    protected void onStart() {
        super.onStart();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (!isHomeTab()) {
                    // Navigate to home, reuse existing instance
                    Intent intent = new Intent(BaseNavActivity.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    startActivity(intent);
                    finish();
                } else {
                    // Already home — do nothing here; MainActivity handles double-back
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }
}