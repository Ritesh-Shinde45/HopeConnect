package com.ritesh.hoppeconnect;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.ritesh.hoppeconnect.admin.AdminOverviewFragment;
import com.ritesh.hoppeconnect.admin.AdminApproveReportsFragment;
import com.ritesh.hoppeconnect.admin.AdminSpammedReportsFragment;
import com.ritesh.hoppeconnect.admin.AdminProfileFragment;


public class AdminDashboardActivity extends AppCompatActivity
        implements NavigationBarView.OnItemSelectedListener {

    private static final String PREFS = "hoppe_prefs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

       
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        if (!"admin".equals(prefs.getString("logged_in_role", ""))) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        BottomNavigationView bottomNav = findViewById(R.id.adminBottomNav);
        bottomNav.setOnItemSelectedListener(this);

       
        if (savedInstanceState == null) {
            loadFragment(new AdminOverviewFragment());
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        Fragment fragment = null;
        int id = item.getItemId();

        if (id == R.id.nav_overview) {
            fragment = new AdminOverviewFragment();
        } else if (id == R.id.nav_approve_reports) {
            fragment = new AdminApproveReportsFragment();
        } else if (id == R.id.nav_spammed_reports) {
            fragment = new AdminSpammedReportsFragment();
        } else if (id == R.id.nav_admin_profile) {
            fragment = new AdminProfileFragment();
        }

        if (fragment != null) {
            loadFragment(fragment);
            return true;
        }
        return false;
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.adminFragmentContainer, fragment)
                .commit();
    }
}