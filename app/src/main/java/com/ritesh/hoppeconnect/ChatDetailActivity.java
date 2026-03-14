package com.ritesh.hoppeconnect;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

/**
 * ChatDetailActivity is superseded by ChatRoomActivity.
 * This stub redirects to ChatRoomActivity if called directly.
 */
public class ChatDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Forward all extras to ChatRoomActivity
        Intent i = new Intent(this, ChatRoomActivity.class);
        if (getIntent().getExtras() != null) {
            i.putExtras(getIntent().getExtras());
        }
        startActivity(i);
        finish();
    }
}