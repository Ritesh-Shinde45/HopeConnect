package com.ritesh.hoppeconnect;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ActivityLogAdapter extends RecyclerView.Adapter<ActivityLogAdapter.VH> {

    private final List<ActivityLogItem> items;

    public ActivityLogAdapter(List<ActivityLogItem> items) { this.items = items; }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
        return new VH(LayoutInflater.from(p.getContext())
                .inflate(R.layout.item_activity_log, p, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        ActivityLogItem item = items.get(pos);
        h.logText.setText(item.text);
        h.logTime.setText(item.time);
       
        int color;
        switch (item.type) {
            case "success": color = Color.parseColor("#4CAF50"); break;
            case "warning": color = Color.parseColor("#FFA726"); break;
            case "danger":  color = Color.parseColor("#EF5350"); break;
            default:        color = Color.parseColor("#42A5F5"); break;
        }
        h.dot.setBackgroundTintList(android.content.res.ColorStateList.valueOf(color));
    }

    @Override public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        View dot; TextView logText, logTime;
        VH(View v) {
            super(v);
            dot     = v.findViewById(R.id.logDot);
            logText = v.findViewById(R.id.logText);
            logTime = v.findViewById(R.id.logTime);
        }
    }
}