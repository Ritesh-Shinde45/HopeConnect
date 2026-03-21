package com.ritesh.hoppeconnect;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class AdminUserAdapter extends RecyclerView.Adapter<AdminUserAdapter.VH> {

    public interface Listener {
        void onSuspend(UserModel u, int pos);
        void onActivate(UserModel u, int pos);
    }

    private final List<UserModel> items;
    private final Listener listener;

    public AdminUserAdapter(List<UserModel> items, Listener listener) {
        this.items = items; this.listener = listener;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
        return new VH(LayoutInflater.from(p.getContext())
                .inflate(R.layout.item_admin_user_row, p, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        UserModel u = items.get(pos);
        h.tvName.setText(u.name);
        h.tvEmail.setText(u.email);
        h.tvReportCount.setText(u.reportCount + " report(s)  •  Joined " + u.joinDate);

        boolean suspended = "suspended".equals(u.status);
        h.tvStatus.setText(suspended ? "Suspended" : "Active");
        int statusColor = suspended ? Color.parseColor("#EF5350") : Color.parseColor("#4CAF50");
        h.tvStatus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(statusColor));
        h.tvStatus.setTextColor(Color.WHITE);

        if (suspended) {
            h.btnAction.setText("Activate");
            h.btnAction.setStrokeColor(android.content.res.ColorStateList.valueOf(Color.parseColor("#4CAF50")));
            h.btnAction.setTextColor(Color.parseColor("#4CAF50"));
            h.btnAction.setOnClickListener(v -> listener.onActivate(u, h.getAdapterPosition()));
        } else {
            h.btnAction.setText("Suspend");
            h.btnAction.setStrokeColor(android.content.res.ColorStateList.valueOf(Color.parseColor("#EF5350")));
            h.btnAction.setTextColor(Color.parseColor("#EF5350"));
            h.btnAction.setOnClickListener(v -> listener.onSuspend(u, h.getAdapterPosition()));
        }
    }

    @Override public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvEmail, tvReportCount, tvStatus;
        com.google.android.material.button.MaterialButton btnAction;
        VH(View v) {
            super(v);
            tvName        = v.findViewById(R.id.tv_user_name);
            tvEmail       = v.findViewById(R.id.tv_user_email);
            tvReportCount = v.findViewById(R.id.tv_user_report_count);
            tvStatus      = v.findViewById(R.id.tv_user_status);
            btnAction     = v.findViewById(R.id.btn_user_action);
        }
    }
}