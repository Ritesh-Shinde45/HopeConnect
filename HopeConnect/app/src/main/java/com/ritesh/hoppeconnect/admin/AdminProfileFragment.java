package com.ritesh.hoppeconnect.admin;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.*;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.ritesh.hoppeconnect.LoginActivity;
import com.ritesh.hoppeconnect.R;

import static android.content.Context.MODE_PRIVATE;

public class AdminProfileFragment extends Fragment {

    private static final String PREFS = "hoppe_prefs";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        SharedPreferences prefs = requireContext()
                .getSharedPreferences(PREFS, MODE_PRIVATE);

        TextView tvName = view.findViewById(R.id.tvAdminName);
        if (tvName != null) {
            tvName.setText("Admin: " + prefs.getString("logged_in_name", "Admin"));
        }

        Button btnLogout = view.findViewById(R.id.btnAdminLogout);
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> logout());
        }
    }

    private void logout() {
       
        requireContext().getSharedPreferences(PREFS, MODE_PRIVATE).edit().clear().apply();

        Intent i = new Intent(requireContext(), LoginActivity.class);
        i.putExtra("explicit_login", true);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
    }
}