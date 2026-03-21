package com.ritesh.hoppeconnect;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.imageview.ShapeableImageView;

import java.util.List;
import java.util.Map;

import io.appwrite.models.Document;
import io.appwrite.services.Databases;

import static android.content.Context.MODE_PRIVATE;

public class AdminSettingsFragment extends Fragment {

    private static final String TAG   = "AdminSettingsFrag";
    private static final String PREFS = "hoppe_prefs";

    private ShapeableImageView ivProfile;
    private TextView txtName, txtEmail, txtRole, txtVersion;
    private SwitchCompat swApproval, swFace, swPublic, swNgo, swEmail, swSpam;
    private Button btnChangePassword, btnLogout;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup c, @Nullable Bundle s) {
        return inf.inflate(R.layout.admin_settings, c, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        super.onViewCreated(v, s);

        ivProfile          = v.findViewById(R.id.adminProfileImage);
        txtName            = v.findViewById(R.id.txtAdminName);
        txtEmail           = v.findViewById(R.id.txtAdminEmail);
        txtRole            = v.findViewById(R.id.txtAdminRole);
        txtVersion         = v.findViewById(R.id.txtAppVersion);
        swApproval         = v.findViewById(R.id.switch_approval_required);
        swFace             = v.findViewById(R.id.switch_face_recognition);
        swPublic           = v.findViewById(R.id.switch_public_reports);
        swNgo              = v.findViewById(R.id.switch_ngo_integration);
        swEmail            = v.findViewById(R.id.switch_email_alerts);
        swSpam             = v.findViewById(R.id.switch_auto_flag_spam);
        btnChangePassword  = v.findViewById(R.id.btnChangePassword);
        btnLogout          = v.findViewById(R.id.btnLogout);

        // App version
        try {
            String ver = requireContext().getPackageManager()
                    .getPackageInfo(requireContext().getPackageName(), 0).versionName;
            txtVersion.setText("v" + ver);
        } catch (Exception e) { txtVersion.setText("v1.0"); }

        // Load admin profile
        loadAdminProfile();

        // Persist switch states via SharedPreferences
        SharedPreferences prefs = requireContext().getSharedPreferences("admin_settings", MODE_PRIVATE);
        swApproval.setChecked(prefs.getBoolean("approval_required",  true));
        swFace.setChecked(prefs.getBoolean("face_recognition",       true));
        swPublic.setChecked(prefs.getBoolean("public_reports",       false));
        swNgo.setChecked(prefs.getBoolean("ngo_integration",         false));
        swEmail.setChecked(prefs.getBoolean("email_alerts",          true));
        swSpam.setChecked(prefs.getBoolean("auto_flag_spam",         false));

        View.OnClickListener savePref = x -> {
            prefs.edit()
                    .putBoolean("approval_required", swApproval.isChecked())
                    .putBoolean("face_recognition",  swFace.isChecked())
                    .putBoolean("public_reports",    swPublic.isChecked())
                    .putBoolean("ngo_integration",   swNgo.isChecked())
                    .putBoolean("email_alerts",      swEmail.isChecked())
                    .putBoolean("auto_flag_spam",    swSpam.isChecked())
                    .apply();
        };
        swApproval.setOnClickListener(savePref);
        swFace.setOnClickListener(savePref);
        swPublic.setOnClickListener(savePref);
        swNgo.setOnClickListener(savePref);
        swEmail.setOnClickListener(savePref);
        swSpam.setOnClickListener(savePref);

        btnChangePassword.setOnClickListener(x ->
                startActivity(new Intent(getContext(), ChangePasswordActivity.class)));

        btnLogout.setOnClickListener(x -> confirmLogout());
    }

    private void loadAdminProfile() {
        // Pull from prefs first for instant display
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS, MODE_PRIVATE);
        String cachedName = prefs.getString("logged_in_name", "Admin");
        txtName.setText(cachedName);
        txtEmail.setText(AppwriteService.ADMIN_EMAIL);
        txtRole.setText("Administrator");

        // Then refresh from DB
        new Thread(() -> {
            try {
                Databases db = AppwriteService.getDatabases();
                List<? extends Document<?>> docs =
                        AppwriteHelper.listAllDocuments(db, AppwriteService.DB_ID, AppwriteService.COL_ADMINS)
                                .getDocuments();
                if (docs.isEmpty() || getActivity() == null) return;

                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) docs.get(0).getData();
                String name  = data.get("name")  != null ? data.get("name").toString()  : "Admin";
                String email = data.get("email") != null ? data.get("email").toString() : AppwriteService.ADMIN_EMAIL;

                getActivity().runOnUiThread(() -> {
                    txtName.setText(name);
                    txtEmail.setText(email);
                });
            } catch (Exception e) {
                Log.e(TAG, "loadAdminProfile error", e);
            }
        }).start();
    }

    private void confirmLogout() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (d, w) -> performLogout())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void performLogout() {
        // Clear local session
        requireContext().getSharedPreferences(PREFS, MODE_PRIVATE).edit().clear().apply();

        // Sign out of Appwrite session via helper (handles coroutine from Java)
        new Thread(() -> {
            try {
                AppwriteHelper.deleteCurrentSession(AppwriteService.getAccount());
            } catch (Exception e) {
                Log.w(TAG, "Session delete failed: " + e.getMessage());
            }
        }).start();

        Intent i = new Intent(getContext(), LoginActivity.class);
        i.putExtra("explicit_login", true);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
    }
}