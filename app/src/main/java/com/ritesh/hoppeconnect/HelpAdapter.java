package com.ritesh.hoppeconnect;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class HelpAdapter extends RecyclerView.Adapter<HelpAdapter.VH> {

    private final List<HelpModel> list;

    public HelpAdapter(List<HelpModel> list) { this.list = list; }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_help, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        HelpModel m = list.get(pos);

        h.tvReportName.setText(m.reportName != null ? m.reportName : "Unknown");
        h.tvWatcherName.setText("Spotted by: " + (m.watcherName != null ? m.watcherName : "Anonymous"));
        h.tvMessage.setText(m.message != null ? m.message : "");
        h.tvReportId.setText(m.reportId != null && !m.reportId.isEmpty()
                ? "#" + m.reportId.substring(0, Math.min(8, m.reportId.length())).toUpperCase()
                : "—");

        h.tvDate.setText(m.resolvedAt != null && !m.resolvedAt.isEmpty()
                ? m.resolvedAt.substring(0, Math.min(10, m.resolvedAt.length()))
                : "Pending");

        boolean confirmed = m.resolvedAt != null && !m.resolvedAt.isEmpty();
        if (confirmed) {
            h.tvStatus.setText("✓ Confirmed");
            h.tvStatus.setTextColor(0xFF2E7D32);
            h.tvStatus.setBackgroundResource(R.drawable.bg_badge_confirmed);
            h.viewAccentBar.setBackgroundColor(0xFF1A1F3C);
            h.viewMsgAccent.setBackgroundColor(0xFF1A1F3C);
        } else {
            h.tvStatus.setText("● Pending");
            h.tvStatus.setTextColor(0xFFF57F17);
            h.tvStatus.setBackgroundResource(R.drawable.bg_badge_pending);
            h.viewAccentBar.setBackgroundColor(0xFFB8861B);
            h.viewMsgAccent.setBackgroundColor(0xFFB8861B);
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvReportName, tvWatcherName, tvMessage, tvDate, tvReportId, tvStatus;
        View viewAccentBar, viewMsgAccent;

        VH(@NonNull View v) {
            super(v);
            tvReportName  = v.findViewById(R.id.tvHelpReportName);
            tvWatcherName = v.findViewById(R.id.tvHelpWatcherName);
            tvMessage     = v.findViewById(R.id.tvHelpMessage);
            tvDate        = v.findViewById(R.id.tvHelpDate);
            tvReportId    = v.findViewById(R.id.tvHelpReportId);
            tvStatus      = v.findViewById(R.id.tvHelpStatus);
            viewAccentBar = v.findViewById(R.id.viewAccentBar);
            viewMsgAccent = v.findViewById(R.id.viewMsgAccent);
        }
    }
}