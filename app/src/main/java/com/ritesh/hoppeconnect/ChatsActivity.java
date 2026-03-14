package com.ritesh.hoppeconnect;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ritesh.hoppeconnect.databinding.ActivityChatsBinding;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import io.appwrite.models.Document;
import io.appwrite.services.Databases;

public class ChatsActivity extends AppCompatActivity {

    private static final String PREFS = "hoppe_prefs";

    private ActivityChatsBinding binding;
    private ChatListAdapter adapter;
    private final List<ChatItem> allChats    = new ArrayList<>();
    private final List<ChatItem> filteredChats = new ArrayList<>();
    private String myUserId;
    private String myName;

    // Auto-refresh every 5 seconds
    private final Handler refreshHandler = new Handler(Looper.getMainLooper());
    private final Runnable refreshRunnable = new Runnable() {
        @Override public void run() {
            loadChats();
            refreshHandler.postDelayed(this, 5000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        AppwriteService.init(this);
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        myUserId = prefs.getString("logged_in_user_id", null);
        myName   = prefs.getString("logged_in_name", "Me");

        if (myUserId == null) { finish(); return; }

        setupRecycler();
        setupSearch();
        setupNewChat();
        loadChats();
    }

    @Override protected void onResume()  { super.onResume();  refreshHandler.post(refreshRunnable); }
    @Override protected void onPause()   { super.onPause();   refreshHandler.removeCallbacks(refreshRunnable); }

    private void setupRecycler() {
        adapter = new ChatListAdapter(filteredChats, chat -> {
            Intent i = new Intent(this, ChatRoomActivity.class);
            i.putExtra("chatId",       chat.chatId);
            i.putExtra("otherUserId",  chat.otherUserId);
            i.putExtra("otherName",    chat.otherName);
            startActivity(i);
        });
        binding.rvChatList.setLayoutManager(new LinearLayoutManager(this));
        binding.rvChatList.setAdapter(adapter);
    }

    private void setupSearch() {
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override public void afterTextChanged(Editable s) { filterChats(s.toString()); }
        });
    }

    private void setupNewChat() {
        binding.newChat.setOnClickListener(v -> showNewChatDialog());
    }

    private void filterChats(String query) {
        filteredChats.clear();
        if (query.isEmpty()) {
            filteredChats.addAll(allChats);
        } else {
            for (ChatItem c : allChats) {
                if (c.otherName.toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT)))
                    filteredChats.add(c);
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void loadChats() {
        new Thread(() -> {
            try {
                // ✅ Fixed: assign to db + use static call
                Databases db = AppwriteService.getDatabases();
                List<? extends Document<?>> docs =
                        AppwriteHelper.getUserChats(db, myUserId).getDocuments();

                List<ChatItem> items = new ArrayList<>();
                for (Document<?> doc : docs) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> data = (Map<String, Object>) doc.getData();

                    String p1 = (String) data.get("participant1");
                    String p2 = (String) data.get("participant2");
                    String otherId   = myUserId.equals(p1) ? p2 : p1;
                    String otherName = myUserId.equals(p1)
                            ? strVal(data, "participant2Name")
                            : strVal(data, "participant1Name");
                    String lastMsg  = strVal(data, "lastMessage");
                    String lastTime = strVal(data, "lastMessageTime");
                    int unread = data.get("unread_" + myUserId) != null
                            ? Integer.parseInt(data.get("unread_" + myUserId).toString()) : 0;

                    items.add(new ChatItem(doc.getId(), otherId, otherName, lastMsg, lastTime, unread));
                }

                runOnUiThread(() -> {
                    allChats.clear();
                    allChats.addAll(items);
                    filterChats(binding.etSearch.getText().toString());
                });

            } catch (Exception e) {
                // Silent refresh
            }
        }).start();
    }

    // ── New Chat Dialog ───────────────────────────────────────────────────────
    private void showNewChatDialog() {
        android.widget.EditText input = new android.widget.EditText(this);
        input.setHint("Enter username to chat with");

        new AlertDialog.Builder(this)
                .setTitle("New Chat")
                .setView(input)
                .setPositiveButton("Start", (dialog, which) -> {
                    String username = input.getText().toString().trim();
                    if (!username.isEmpty()) findUserAndStartChat(username);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void findUserAndStartChat(String username) {
        new Thread(() -> {
            try {
                Databases db = AppwriteService.getDatabases();
                List<? extends Document<?>> docs =
                        AppwriteHelper.findUserByField(db, "username", username).getDocuments();

                if (docs.isEmpty()) {
                    runOnUiThread(() -> Toast.makeText(this,
                            "User not found", Toast.LENGTH_SHORT).show());
                    return;
                }

                Document<?> userDoc = docs.get(0);
                String otherId   = userDoc.getId();
                @SuppressWarnings("unchecked")
                Map<String, Object> ud = (Map<String, Object>) userDoc.getData();
                String otherName = strVal(ud, "name");

                if (otherId.equals(myUserId)) {
                    runOnUiThread(() -> Toast.makeText(this,
                            "Cannot chat with yourself", Toast.LENGTH_SHORT).show());
                    return;
                }

                // Check if chat already exists
                List<? extends Document<?>> existingChats =
                        AppwriteHelper.getUserChats(db, myUserId).getDocuments();

                String chatId = null;
                for (Document<?> c : existingChats) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> cd = (Map<String, Object>) c.getData();
                    String p1 = strVal(cd, "participant1");
                    String p2 = strVal(cd, "participant2");
                    if ((p1.equals(myUserId) && p2.equals(otherId)) ||
                            (p1.equals(otherId)  && p2.equals(myUserId))) {
                        chatId = c.getId();
                        break;
                    }
                }

                // Create new chat if none found
                if (chatId == null) {
                    chatId = UUID.randomUUID().toString().replace("-", "").substring(0, 20);
                    Map<String, Object> chatData = new HashMap<>();
                    chatData.put("participant1",     myUserId);
                    chatData.put("participant2",     otherId);
                    chatData.put("participant1Name", myName);
                    chatData.put("participant2Name", otherName);
                    chatData.put("participants",     myUserId + "," + otherId);
                    chatData.put("lastMessage",      "");
                    chatData.put("lastMessageTime",  "");
                    AppwriteHelper.createDocument(db, AppwriteService.DB_ID,
                            AppwriteService.COL_CHATS, chatId, chatData);
                }

                final String finalChatId   = chatId;
                final String finalOtherName = otherName;
                final String finalOtherId   = otherId;

                runOnUiThread(() -> {
                    Intent i = new Intent(this, ChatRoomActivity.class);
                    i.putExtra("chatId",       finalChatId);
                    i.putExtra("otherUserId",  finalOtherId);
                    i.putExtra("otherName",    finalOtherName);
                    startActivity(i);
                });

            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this,
                        "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private static String strVal(Map<String, Object> m, String k) {
        Object v = m.get(k); return v != null ? v.toString() : "";
    }

    // ── Data model ────────────────────────────────────────────────────────────
    public static class ChatItem {
        public final String chatId, otherUserId, otherName, lastMessage, lastTime;
        public final int unreadCount;
        ChatItem(String chatId, String otherUserId, String otherName,
                 String lastMessage, String lastTime, int unreadCount) {
            this.chatId      = chatId;
            this.otherUserId = otherUserId;
            this.otherName   = otherName;
            this.lastMessage = lastMessage;
            this.lastTime    = lastTime;
            this.unreadCount = unreadCount;
        }
    }

    // ── RecyclerView Adapter ──────────────────────────────────────────────────
    interface OnChatClickListener { void onChatClick(ChatItem chat); }

    static class ChatListAdapter extends RecyclerView.Adapter<ChatListAdapter.VH> {
        private final List<ChatItem> list;
        private final OnChatClickListener listener;
        ChatListAdapter(List<ChatItem> list, OnChatClickListener listener) {
            this.list = list; this.listener = listener;
        }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_chat_preview, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            ChatItem c = list.get(pos);
            h.tvName.setText(c.otherName.isEmpty() ? "Unknown" : c.otherName);
            h.tvLastMsg.setText(c.lastMessage.isEmpty() ? "No messages yet" : c.lastMessage);
            h.tvTime.setText(c.lastTime);
            if (c.unreadCount > 0) {
                h.tvUnread.setVisibility(View.VISIBLE);
                h.tvUnread.setText(String.valueOf(c.unreadCount));
            } else {
                h.tvUnread.setVisibility(View.GONE);
            }
            // Avatar initials
            String initial = c.otherName.isEmpty() ? "?" :
                    String.valueOf(c.otherName.charAt(0)).toUpperCase(Locale.ROOT);
            h.tvAvatar.setText(initial);
            h.itemView.setOnClickListener(v -> listener.onChatClick(c));
        }

        @Override public int getItemCount() { return list.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvAvatar, tvName, tvLastMsg, tvTime, tvUnread;
            VH(@NonNull View v) {
                super(v);
                tvAvatar  = v.findViewById(R.id.tvAvatar);
                tvName    = v.findViewById(R.id.tvName);
                tvLastMsg = v.findViewById(R.id.tvLastMessage);
                tvTime    = v.findViewById(R.id.tvTime);
                tvUnread  = v.findViewById(R.id.tvUnreadCount);
            }
        }
    }
}