package com.ritesh.hoppeconnect;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class ReportAdapter extends RecyclerView.Adapter<ReportAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(ReportModel model);
    }

    public enum Mode { HORIZONTAL, VERTICAL }

    private final Context ctx;
    private final List<ReportModel> reports;
    private final OnItemClickListener listener;
    private final Mode mode;

    public ReportAdapter(Context context, List<ReportModel> reports,
                         OnItemClickListener listener) {
        this(context, reports, listener, Mode.VERTICAL);
    }

    public ReportAdapter(Context context, List<ReportModel> reports,
                         OnItemClickListener listener, Mode mode) {
        this.ctx      = context;
        this.reports  = reports;
        this.listener = listener;
        this.mode     = mode;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(ctx)
                .inflate(R.layout.item_case_card, parent, false);

        if (mode == Mode.HORIZONTAL) {
            int dp200 = (int) (200 * ctx.getResources().getDisplayMetrics().density);
            int dp12  = (int) (12  * ctx.getResources().getDisplayMetrics().density);
            RecyclerView.LayoutParams lp =
                    new RecyclerView.LayoutParams(dp200,
                            RecyclerView.LayoutParams.WRAP_CONTENT);
            lp.setMarginEnd(dp12);
            view.setLayoutParams(lp);
        }

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ReportModel model = reports.get(position);

        // Name
        holder.personName.setText(model.name != null ? model.name : "Unknown");

        // Age
        holder.personAge.setText(model.age > 0 ? model.age + " yrs" : "Age N/A");

        // Location / description
        holder.personLocation.setText(
                model.description != null && !model.description.isEmpty()
                        ? model.description : "No description");

        // Missing since
        if (model.createdAt > 0) {
            String dateStr = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                    .format(new Date(model.createdAt));
            holder.missingDate.setText("Missing since: " + dateStr);
        } else if (model.missingSince != null && !model.missingSince.isEmpty()) {
            holder.missingDate.setText("Missing since: " + model.missingSince);
        } else {
            holder.missingDate.setText("Missing since: Unknown");
        }

        // Status badge
        if (holder.statusBadge != null) {
            String status = model.status != null ? model.status.toLowerCase() : "active";
            if ("found".equals(status)) {
                holder.statusBadge.setText("Found");
                holder.statusBadge.setBackgroundResource(R.drawable.badge_found);
            } else {
                holder.statusBadge.setText("Missing");
                holder.statusBadge.setBackgroundResource(R.drawable.badge_missing);
            }
        }

        // Photo
        if (model.photoUrls != null && !model.photoUrls.isEmpty()) {
            Glide.with(ctx)
                    .load(model.photoUrls.get(0))
                    .placeholder(R.drawable.person_placeholder)
                    .error(R.drawable.person_placeholder)
                    .centerCrop()
                    .into(holder.personImage);
        } else {
            holder.personImage.setImageResource(R.drawable.person_placeholder);
        }

        // View Details button
        holder.btnViewDetails.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(model);
            } else {
                ReportModelCache.put(model);
                Intent intent = new Intent(ctx, MissedPersonDetailActivity.class);
                intent.putExtra(MissedPersonDetailActivity.EXTRA_REPORT_ID, model.id);
                ctx.startActivity(intent);
            }
        });

        // Card click
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(model);
        });

        // ── Chat button — start chat with the person who uploaded this report ──
        holder.chatBtn.setOnClickListener(v -> {
            SharedPreferences prefs =
                    ctx.getSharedPreferences("hoppe_prefs", Context.MODE_PRIVATE);
            String myUserId = prefs.getString("logged_in_user_id", null);

            if (myUserId == null) {
                Toast.makeText(ctx, "Please log in to chat", Toast.LENGTH_SHORT).show();
                return;
            }

            // userId field on the report = who uploaded it
            String reporterUserId = model.userId;
            if (reporterUserId == null || reporterUserId.isEmpty()) {
                Toast.makeText(ctx, "Reporter info unavailable", Toast.LENGTH_SHORT).show();
                return;
            }

            if (myUserId.equals(reporterUserId)) {
                Toast.makeText(ctx, "This is your own report", Toast.LENGTH_SHORT).show();
                return;
            }

            Toast.makeText(ctx, "Opening chat with reporter...", Toast.LENGTH_SHORT).show();
            startChatWithReporter(myUserId, reporterUserId, model.name);
        });

        // ── Call button ──
        holder.callBtn.setOnClickListener(v -> {
            String number = model.emergencyContact1;
            if (number == null || number.isEmpty()) number = model.contact;
            if (number != null && !number.isEmpty()) {
                Intent callIntent = new Intent(Intent.ACTION_DIAL,
                        Uri.parse("tel:" + number.replaceAll("[^0-9+]", "")));
                ctx.startActivity(callIntent);
            } else {
                Toast.makeText(ctx, "No emergency contact available",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Finds or creates a chat with the reporter, then opens ChatRoomActivity.
     */
    private void startChatWithReporter(String myUserId,
                                       String reporterUserId,
                                       String reportName) {
        new Thread(() -> {
            try {
                AppwriteService.init(ctx);
                android.app.Activity activity = getActivity();

                // Get my name from prefs
                SharedPreferences prefs =
                        ctx.getSharedPreferences("hoppe_prefs", Context.MODE_PRIVATE);
                String myName = prefs.getString("logged_in_name", "User");

                io.appwrite.services.Databases db = AppwriteService.getDatabases();

                // Try to get reporter's name from users collection
                String reporterName = "Reporter";
                try {
                    io.appwrite.models.Document<?> reporterDoc =
                            AppwriteHelper.getDocument(
                                    db,
                                    AppwriteService.DB_ID,
                                    AppwriteService.COL_USERS,
                                    reporterUserId);
                    @SuppressWarnings("unchecked")
                    Map<String, Object> rd =
                            (Map<String, Object>) reporterDoc.getData();
                    if (rd.get("name") != null)
                        reporterName = rd.get("name").toString();
                } catch (Exception ignored) {
                    // Use default "Reporter" if lookup fails
                }

                // Check for existing chat between these two users
                String chatId = null;
                try {
                    java.util.List<? extends io.appwrite.models.Document<?>> existingChats =
                            AppwriteHelper.getUserChats(db, myUserId).getDocuments();

                    for (io.appwrite.models.Document<?> c : existingChats) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> cd =
                                (Map<String, Object>) c.getData();
                        String p1 = cd.get("participant1") != null
                                ? cd.get("participant1").toString() : "";
                        String p2 = cd.get("participant2") != null
                                ? cd.get("participant2").toString() : "";
                        if ((p1.equals(myUserId)       && p2.equals(reporterUserId)) ||
                                (p1.equals(reporterUserId) && p2.equals(myUserId))) {
                            chatId = c.getId();
                            break;
                        }
                    }
                } catch (Exception ignored) {}

                // Create new chat if none exists
                if (chatId == null) {
                    chatId = UUID.randomUUID().toString()
                            .replace("-", "").substring(0, 20);

                    Map<String, Object> chatData = new HashMap<>();
                    chatData.put("participant1",     myUserId);
                    chatData.put("participant2",     reporterUserId);
                    chatData.put("participant1Name", myName);
                    chatData.put("participant2Name", reporterName);
                    chatData.put("participants",     myUserId + "," + reporterUserId);
                    chatData.put("lastMessage",
                            "Re: Missing case - " + (reportName != null ? reportName : ""));
                    chatData.put("lastMessageTime", "");

                    AppwriteHelper.createDocument(
                            db,
                            AppwriteService.DB_ID,
                            AppwriteService.COL_CHATS,
                            chatId,
                            chatData);
                }

                final String finalChatId     = chatId;
                final String finalReporterName = reporterName;
                final String finalReporterId   = reporterUserId;

                android.os.Handler handler =
                        new android.os.Handler(android.os.Looper.getMainLooper());
                handler.post(() -> {
                    Intent i = new Intent(ctx, ChatRoomActivity.class);
                    i.putExtra("chatId",      finalChatId);
                    i.putExtra("otherUserId", finalReporterId);
                    i.putExtra("otherName",   finalReporterName);
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    ctx.startActivity(i);
                });

            } catch (Exception e) {
                android.os.Handler handler =
                        new android.os.Handler(android.os.Looper.getMainLooper());
                handler.post(() ->
                        Toast.makeText(ctx,
                                "Chat error: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    /**
     * Helper to get Activity from Context for runOnUiThread.
     * Returns null safely if context is not an Activity.
     */
    private android.app.Activity getActivity() {
        if (ctx instanceof android.app.Activity) return (android.app.Activity) ctx;
        return null;
    }

    @Override
    public int getItemCount() { return reports.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ShapeableImageView personImage;
        TextView statusBadge, personAge, personName, personLocation, missingDate;
        Button btnViewDetails;
        ImageButton chatBtn, callBtn;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            personImage    = itemView.findViewById(R.id.personImage);
            statusBadge    = itemView.findViewById(R.id.statusBadge);
            personAge      = itemView.findViewById(R.id.personAge);
            personName     = itemView.findViewById(R.id.personName);
            personLocation = itemView.findViewById(R.id.personLocation);
            missingDate    = itemView.findViewById(R.id.missingDate);
            btnViewDetails = itemView.findViewById(R.id.btnViewDetails);
            chatBtn        = itemView.findViewById(R.id.chatBtn);
            callBtn        = itemView.findViewById(R.id.callBtn);
        }
    }
}