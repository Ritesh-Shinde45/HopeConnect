package com.ritesh.hoppeconnect;

import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class AdminDashboardActivity extends AppCompatActivity {

    private static final String TAG = "AdminDashboardActivity";
    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().getDecorView()
                    .setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }
        AppwriteService.init(this);

        bottomNav = findViewById(R.id.admin_bottom_nav);
        if (bottomNav == null) {
            Log.e(TAG, "admin_bottom_nav not found — check activity_admin_dashboard.xml");
            return;
        }

        if (savedInstanceState == null) {
            loadFragment(new AdminOverviewFragment());
            bottomNav.setSelectedItemId(R.id.nav_overview);
        }

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment f = null;
            int id = item.getItemId();
            if      (id == R.id.nav_overview)        f = new AdminOverviewFragment();
            else if (id == R.id.nav_approve_reports) f = new AdminApproveReportsFragment();
            else if (id == R.id.nav_all_reports)     f = new AdminAllReportsFragment();
            else if (id == R.id.nav_manage_users)    f = new AdminManageUsersFragment();
            else if (id == R.id.nav_admin_settings)  f = new AdminSettingsFragment();

            if (f != null) { loadFragment(f); return true; }
            return false;
        });
    }

    private void loadFragment(Fragment f) {
        try {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.admin_fragment_container, f)
                    .commit();
        } catch (Exception e) {
            Log.e(TAG, "loadFragment error: " + e.getMessage(), e);
        }
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }
}