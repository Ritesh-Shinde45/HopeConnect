package com.ritesh.hoppeconnect;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class AdminReportRowAdapter extends RecyclerView.Adapter<AdminReportRowAdapter.VH> {

    public interface Listener {
        void onApprove(AdminReportModel r, int pos);
        void onFlag(AdminReportModel r, int pos);
        void onClick(AdminReportModel r);
    }

    private final List<AdminReportModel> items;
    private final Listener listener;

    public AdminReportRowAdapter(List<AdminReportModel> items, Listener listener) {
        this.items    = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
        return new VH(LayoutInflater.from(p.getContext())
                .inflate(R.layout.item_admin_report_row, p, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        AdminReportModel r = items.get(pos);
        h.tvId.setText(String.valueOf(pos + 1));
        h.tvName.setText(r.name);
        h.tvLocation.setText(r.location);
        h.tvDate.setText(r.createdAt);
        h.tvStatus.setText(r.status.toUpperCase());

        int bg;
        switch (r.status) {
            case "active":   bg = Color.parseColor("#4CAF50"); break;
            case "found":    bg = Color.parseColor("#42A5F5"); break;
            case "fake":
            case "rejected": bg = Color.parseColor("#EF5350"); break;
            case "resolved": bg = Color.parseColor("#9C27B0"); break;
            default:         bg = Color.parseColor("#FFA726"); break;
        }
        h.tvStatus.setBackgroundTintList(ColorStateList.valueOf(bg));

        boolean isPending = "pending".equals(r.status);
        h.btnApprove.setVisibility(isPending ? View.VISIBLE : View.GONE);
        h.btnFlag.setVisibility(isPending ? View.VISIBLE : View.GONE);

        h.btnApprove.setOnClickListener(v -> listener.onApprove(r, h.getAdapterPosition()));
        h.btnFlag.setOnClickListener(v -> listener.onFlag(r, h.getAdapterPosition()));
        h.itemView.setOnClickListener(v -> listener.onClick(r));
    }

    @Override
    public int getItemCount() { return items.size(); }

    public void remove(int pos) {
        items.remove(pos);
        notifyItemRemoved(pos);
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvId, tvName, tvLocation, tvDate, tvStatus;
        Button btnApprove, btnFlag;

        VH(View v) {
            super(v);
            tvId       = v.findViewById(R.id.tv_row_id);
            tvName     = v.findViewById(R.id.tv_row_name);
            tvLocation = v.findViewById(R.id.tv_row_location);
            tvDate     = v.findViewById(R.id.tv_row_date);
            tvStatus   = v.findViewById(R.id.tv_row_status);
            btnApprove = v.findViewById(R.id.btn_row_approve);
            btnFlag    = v.findViewById(R.id.btn_row_flag);
        }
    }
}