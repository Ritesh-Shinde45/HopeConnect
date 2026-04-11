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
import java.util.List;
import java.util.Locale;

public class ReportAdapter extends RecyclerView.Adapter<ReportAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(ReportModel model);
    }

    
    public enum Mode { HORIZONTAL, VERTICAL }

    private final Context context;
    private final List<ReportModel> reports;
    private final OnItemClickListener listener;
    private final Mode mode;

    
    public ReportAdapter(Context context, List<ReportModel> reports,
                         OnItemClickListener listener) {
        this(context, reports, listener, Mode.VERTICAL);
    }

    public ReportAdapter(Context context, List<ReportModel> reports,
                         OnItemClickListener listener, Mode mode) {
        this.context  = context;
        this.reports  = reports;
        this.listener = listener;
        this.mode     = mode;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_case_card, parent, false);

       
        if (mode == Mode.HORIZONTAL) {
            int dp200 = (int) (200 * context.getResources().getDisplayMetrics().density);
            int dp12  = (int) (12  * context.getResources().getDisplayMetrics().density);
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

       
        holder.personName.setText(model.name != null ? model.name : "Unknown");

       
        holder.personAge.setText(model.age > 0 ? model.age + " yrs" : "Age N/A");

       
        holder.personLocation.setText(
                model.description != null && !model.description.isEmpty()
                        ? model.description : "No description");

       
        if (model.createdAt > 0) {
            String dateStr = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                    .format(new Date(model.createdAt));
            holder.missingDate.setText("Missing since: " + dateStr);
        } else if (model.missingSince != null && !model.missingSince.isEmpty()) {
            holder.missingDate.setText("Missing since: " + model.missingSince);
        } else {
            holder.missingDate.setText("Missing since: Unknown");
        }

       
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

       
        if (model.photoUrls != null && !model.photoUrls.isEmpty()) {
            Glide.with(context)
                    .load(model.photoUrls.get(0))
                    .placeholder(R.drawable.person_placeholder)
                    .error(R.drawable.person_placeholder)
                    .centerCrop()
                    .into(holder.personImage);
        } else {
            holder.personImage.setImageResource(R.drawable.person_placeholder);
        }

       
        holder.btnViewDetails.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(model);
            } else {
                ReportModelCache.put(model);
                Intent intent = new Intent(context, MissedPersonDetailActivity.class);
                intent.putExtra(MissedPersonDetailActivity.EXTRA_REPORT_ID, model.id);
                context.startActivity(intent);
            }
        });

       
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(model);
        });

       
        holder.chatBtn.setOnClickListener(v -> {
            SharedPreferences prefs =
                    context.getSharedPreferences("hoppe_prefs", Context.MODE_PRIVATE);
            String myUserId = prefs.getString("logged_in_user_id", null);
            if (myUserId == null) {
                Toast.makeText(context, "Please log in to chat", Toast.LENGTH_SHORT).show();
                return;
            }
            if (model.userId == null || model.userId.isEmpty()) {
                Toast.makeText(context, "Uploader info unavailable", Toast.LENGTH_SHORT).show();
                return;
            }
            if (myUserId.equals(model.userId)) {
                Toast.makeText(context, "This is your own report", Toast.LENGTH_SHORT).show();
                return;
            }
            ReportModelCache.put(model);
            startChatWithUploader(context, myUserId, model.userId, model.name);
        });

       
        holder.callBtn.setOnClickListener(v -> {
            String number = model.emergencyContact1;
            if (number == null || number.isEmpty()) number = model.contact;
            if (number != null && !number.isEmpty()) {
                Intent callIntent = new Intent(Intent.ACTION_DIAL,
                        Uri.parse("tel:" + number.replaceAll("[^0-9+]", "")));
                context.startActivity(callIntent);
            } else {
                Toast.makeText(context, "No emergency contact available",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

   

    private void startChatWithUploader(Context ctx, String myUserId,
                                       String otherUserId, String reportName) {
        Toast.makeText(ctx, "Opening chat...", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            try {
                AppwriteService.init(ctx);
                io.appwrite.services.Databases db = AppwriteService.getDatabases();

                java.util.List<? extends io.appwrite.models.Document<?>> existingChats =
                        AppwriteHelper.getUserChats(db, myUserId).getDocuments();

                String chatId = null;
                for (io.appwrite.models.Document<?> c : existingChats) {
                    @SuppressWarnings("unchecked")
                    java.util.Map<String, Object> cd =
                            (java.util.Map<String, Object>) c.getData();
                    String p1 = cd.get("participant1") != null
                            ? cd.get("participant1").toString() : "";
                    String p2 = cd.get("participant2") != null
                            ? cd.get("participant2").toString() : "";
                    if ((p1.equals(myUserId) && p2.equals(otherUserId)) ||
                            (p1.equals(otherUserId) && p2.equals(myUserId))) {
                        chatId = c.getId();
                        break;
                    }
                }

                if (chatId == null) {
                    chatId = java.util.UUID.randomUUID().toString()
                            .replace("-", "").substring(0, 20);
                    SharedPreferences prefs = ctx.getSharedPreferences(
                            "hoppe_prefs", Context.MODE_PRIVATE);
                    String myName = prefs.getString("logged_in_name", "User");

                    java.util.Map<String, Object> chatData = new java.util.HashMap<>();
                    chatData.put("participant1", myUserId);
                    chatData.put("participant2", otherUserId);
                    chatData.put("participant1Name", myName);
                    chatData.put("participant2Name", "Report Uploader");
                    chatData.put("participants", myUserId + "," + otherUserId);
                    chatData.put("lastMessage", "Re: Missing case - " + reportName);
                    chatData.put("lastMessageTime", "");
                    AppwriteHelper.createDocument(db, AppwriteService.DB_ID,
                            AppwriteService.COL_CHATS, chatId, chatData);
                }

                final String finalChatId = chatId;
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    Intent i = new Intent(ctx, ChatRoomActivity.class);
                    i.putExtra("chatId", finalChatId);
                    i.putExtra("otherUserId", otherUserId);
                    i.putExtra("otherName", "Report Uploader");
                    ctx.startActivity(i);
                });

            } catch (Exception e) {
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() ->
                        Toast.makeText(ctx, "Chat error: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
            }
        }).start();
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