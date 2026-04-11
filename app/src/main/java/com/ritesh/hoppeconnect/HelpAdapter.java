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
        h.tvWatcherName.setText("By: " + (m.watcherName != null
                ? m.watcherName : "Anonymous"));
        h.tvMessage.setText(m.message != null ? m.message : "");
        h.tvDate.setText(m.resolvedAt != null ? m.resolvedAt : "");
    }

    @Override public int getItemCount() { return list.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvReportName, tvWatcherName, tvMessage, tvDate;
        VH(@NonNull View v) {
            super(v);
            tvReportName  = v.findViewById(R.id.tvHelpReportName);
            tvWatcherName = v.findViewById(R.id.tvHelpWatcherName);
            tvMessage     = v.findViewById(R.id.tvHelpMessage);
            tvDate        = v.findViewById(R.id.tvHelpDate);
        }
    }
}