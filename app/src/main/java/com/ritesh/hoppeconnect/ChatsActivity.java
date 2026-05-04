package com.ritesh.hoppeconnect;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ritesh.hoppeconnect.databinding.ActivityChatsBinding;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import io.appwrite.models.Document;
import io.appwrite.services.Databases;

public class ChatsActivity extends AppCompatActivity {

    private static final String TAG   = "ChatsActivity";
    private static final String PREFS = "hoppe_prefs";

    private ActivityChatsBinding binding;
    private ChatListAdapter adapter;

    private final List<ChatItem> allChats      = new ArrayList<>();
    private final List<ChatItem> filteredChats = new ArrayList<>();

    private String myUserId;
    private String myName;

    private final Handler  refreshHandler  = new Handler(Looper.getMainLooper());
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
        myName   = prefs.getString("logged_in_name",    "Me");

        if (myUserId == null) { finish(); return; }

        setupRecycler();
        setupSearch();
        setupBottomNav();

        binding.newChat.setOnClickListener(v -> showNewChatDialog());

        if (binding.ivBack != null)
            binding.ivBack.setOnClickListener(v -> goHome());

        handleDirectOpen();
        loadChats();
    }

   
    @Override
    public void onBackPressed() {
        goHome();
    }

    private void goHome() {
        Intent i = new Intent(this, MainActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(i);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshHandler.post(refreshRunnable);
        if (binding.bottomNav != null)
            binding.bottomNav.setSelectedItemId(R.id.nav_chat);
    }

    @Override
    protected void onPause() {
        super.onPause();
        refreshHandler.removeCallbacks(refreshRunnable);
    }

   
    private void setupRecycler() {
        adapter = new ChatListAdapter(filteredChats,
                chat -> openChatRoom(chat.chatId, chat.otherUserId, chat.otherName));
        binding.rvChatList.setLayoutManager(new LinearLayoutManager(this));
        binding.rvChatList.setAdapter(adapter);
    }

   
    private void setupSearch() {
        binding.etSearch.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        binding.etSearch.setSingleLine(true);

        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                filterChats(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        binding.etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                filterChats(binding.etSearch.getText().toString());
                android.view.inputmethod.InputMethodManager imm =
                        (android.view.inputmethod.InputMethodManager)
                                getSystemService(INPUT_METHOD_SERVICE);
                if (imm != null) imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                return true;
            }
            return false;
        });
    }

    private void filterChats(String query) {
        filteredChats.clear();
        if (query.trim().isEmpty()) {
            filteredChats.addAll(allChats);
        } else {
            String lq = query.toLowerCase(Locale.ROOT).trim();
            for (ChatItem c : allChats) {
                if (c.otherName.toLowerCase(Locale.ROOT).contains(lq) ||
                        c.lastMessage.toLowerCase(Locale.ROOT).contains(lq)) {
                    filteredChats.add(c);
                }
            }
        }
        adapter.notifyDataSetChanged();
        binding.tvEmptyChats.setVisibility(filteredChats.isEmpty() ? View.VISIBLE : View.GONE);
    }

   
    private void setupBottomNav() {
        if (binding.bottomNav == null) return;
        binding.bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if      (id == R.id.nav_home)    { goHome(); return true; }
            else if (id == R.id.nav_explore) { startActivity(new Intent(this, ExploreActivity.class)); return true; }
            else if (id == R.id.nav_chat)    { return true; }
            else if (id == R.id.nav_profile) { startActivity(new Intent(this, ProfileActivity.class)); return true; }
            return false;
        });
    }

   
    private void loadChats() {
        new Thread(() -> {
            try {
                Databases db = AppwriteService.getDatabases();
                List<? extends Document<?>> docs =
                        AppwriteHelper.getUserChats(db, myUserId).getDocuments();

                List<ChatItem> items = new ArrayList<>();
                for (Document<?> doc : docs) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> data = (Map<String, Object>) doc.getData();

                    String p1        = strVal(data, "participant1");
                    String p2        = strVal(data, "participant2");
                    String otherId   = myUserId.equals(p1) ? p2 : p1;
                    String otherName = myUserId.equals(p1)
                            ? strVal(data, "participant2Name")
                            : strVal(data, "participant1Name");
                    String lastMsg   = strVal(data, "lastMessage");
                    String lastTime  = strVal(data, "lastMessageTime");
                    int unread = data.get("unread_" + myUserId) != null
                            ? parseInt(data.get("unread_" + myUserId).toString()) : 0;

                   
                    String updatedAt = doc.getUpdatedAt() != null
                            ? doc.getUpdatedAt() : doc.getCreatedAt();

                    items.add(new ChatItem(
                            doc.getId(), otherId, otherName,
                            lastMsg, lastTime, unread, updatedAt));
                }

               
                Collections.sort(items, (a, b) ->
                        b.updatedAt.compareTo(a.updatedAt));

                runOnUiThread(() -> {
                    allChats.clear();
                    allChats.addAll(items);
                    filterChats(binding.etSearch.getText().toString());
                });

            } catch (Exception e) {
                Log.w(TAG, "loadChats error: " + e.getMessage());
            }
        }).start();
    }

   
    private void showNewChatDialog() {
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(48, 32, 48, 16);

        TextView hint = new TextView(this);
        hint.setText("Search by name or username");
        hint.setTextSize(12);
        hint.setTextColor(0xFF888888);
        hint.setPadding(0, 0, 0, 8);

        AutoCompleteTextView autoComplete = new AutoCompleteTextView(this);
        autoComplete.setHint("Type a name or username...");
        autoComplete.setThreshold(2);
        autoComplete.setSingleLine(true);
        autoComplete.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        autoComplete.setPadding(24, 24, 24, 24);
        autoComplete.setBackgroundResource(android.R.drawable.edit_text);

        layout.addView(hint);
        layout.addView(autoComplete);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("New Chat")
                .setView(layout)
                .setPositiveButton("Start Chat", null)
                .setNegativeButton("Cancel", null)
                .create();

        ArrayAdapter<String> suggestAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, new ArrayList<>());
        autoComplete.setAdapter(suggestAdapter);

        Map<String, String> nameToId   = new HashMap<>();
        Map<String, String> nameToFull = new HashMap<>();

        autoComplete.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim();
                if (query.length() < 2) return;
                new Thread(() -> {
                    try {
                        Databases db = AppwriteService.getDatabases();
                        List<? extends Document<?>> docs =
                                AppwriteHelper.listAllDocuments(
                                        db, AppwriteService.DB_ID,
                                        AppwriteService.COL_USERS).getDocuments();

                        List<String> suggestions = new ArrayList<>();
                        nameToId.clear(); nameToFull.clear();

                        for (Document<?> doc : docs) {
                            if (doc.getId().equals(myUserId)) continue;
                            @SuppressWarnings("unchecked")
                            Map<String, Object> data = (Map<String, Object>) doc.getData();
                            String name     = strVal(data, "name");
                            String username = strVal(data, "username");
                            String display  = name.isEmpty() ? username : name;
                            String lq       = query.toLowerCase(Locale.ROOT);
                            if (name.toLowerCase().contains(lq) ||
                                    username.toLowerCase().contains(lq)) {
                                String label = display + (username.isEmpty() ? "" : "  @" + username);
                                suggestions.add(label);
                                nameToId.put(label, doc.getId());
                                nameToFull.put(label, display);
                            }
                        }
                        runOnUiThread(() -> {
                            suggestAdapter.clear();
                            suggestAdapter.addAll(suggestions);
                            suggestAdapter.notifyDataSetChanged();
                            if (!suggestions.isEmpty()) autoComplete.showDropDown();
                        });
                    } catch (Exception e) {
                        Log.w(TAG, "suggestion error: " + e.getMessage());
                    }
                }).start();
            }
        });

        autoComplete.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                String input = autoComplete.getText().toString().trim();
                if (!input.isEmpty()) { dialog.dismiss(); startChatFromInput(input, nameToId, nameToFull); }
                return true;
            }
            return false;
        });

        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String input = autoComplete.getText().toString().trim();
            if (input.isEmpty()) { autoComplete.setError("Enter a name or username"); return; }
            dialog.dismiss();
            startChatFromInput(input, nameToId, nameToFull);
        });
    }

    private void startChatFromInput(String input,
                                    Map<String, String> nameToId,
                                    Map<String, String> nameToFull) {
        if (nameToId.containsKey(input)) {
            findUserByIdAndStartChat(nameToId.get(input), nameToFull.get(input));
        } else {
            findUserAndStartChat(input);
        }
    }

    public static void openChatWithUserId(
            android.content.Context ctx, String targetUserId, String targetName) {
        Intent i = new Intent(ctx, ChatsActivity.class);
        i.putExtra("direct_user_id",   targetUserId);
        i.putExtra("direct_user_name", targetName);
        ctx.startActivity(i);
    }

    private void handleDirectOpen() {
        String directId   = getIntent().getStringExtra("direct_user_id");
        String directName = getIntent().getStringExtra("direct_user_name");
        if (directId != null && !directId.isEmpty())
            findUserByIdAndStartChat(directId, directName != null ? directName : "User");
    }

    private void findUserByIdAndStartChat(String targetId, String targetName) {
        if (targetId.equals(myUserId)) {
            Toast.makeText(this, "Cannot chat with yourself", Toast.LENGTH_SHORT).show();
            return;
        }
        new Thread(() -> {
            try {
                Databases db = AppwriteService.getDatabases();
                String chatId = findOrCreateChat(db, targetId, targetName);
                runOnUiThread(() -> openChatRoom(chatId, targetId, targetName));
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this,
                        "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void findUserAndStartChat(String usernameOrName) {
        new Thread(() -> {
            try {
                Databases db = AppwriteService.getDatabases();
                List<? extends Document<?>> docs =
                        AppwriteHelper.findUserByField(
                                db, AppwriteService.COL_USERS, "username", usernameOrName
                        ).getDocuments();
                if (docs.isEmpty())
                    docs = AppwriteHelper.findUserByField(
                            db, AppwriteService.COL_USERS, "name", usernameOrName
                    ).getDocuments();

                if (docs.isEmpty()) {
                    runOnUiThread(() -> Toast.makeText(this,
                            "User \"" + usernameOrName + "\" not found", Toast.LENGTH_SHORT).show());
                    return;
                }
                Document<?> userDoc = docs.get(0);
                String otherId = userDoc.getId();
                @SuppressWarnings("unchecked")
                Map<String, Object> ud = (Map<String, Object>) userDoc.getData();
                String otherName = strVal(ud, "name");
                if (otherName.isEmpty()) otherName = strVal(ud, "username");

                if (otherId.equals(myUserId)) {
                    runOnUiThread(() -> Toast.makeText(this,
                            "Cannot chat with yourself", Toast.LENGTH_SHORT).show());
                    return;
                }
                String chatId    = findOrCreateChat(db, otherId, otherName);
                String fChatId   = chatId;
                String fName     = otherName;
                String fId       = otherId;
                runOnUiThread(() -> openChatRoom(fChatId, fId, fName));

            } catch (Exception e) {
                Log.e(TAG, "findUserAndStartChat error", e);
                runOnUiThread(() -> Toast.makeText(this,
                        "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private String findOrCreateChat(Databases db, String otherId, String otherName)
            throws Exception {
        List<? extends Document<?>> existingChats =
                AppwriteHelper.getUserChats(db, myUserId).getDocuments();
        for (Document<?> c : existingChats) {
            @SuppressWarnings("unchecked")
            Map<String, Object> cd = (Map<String, Object>) c.getData();
            String p1 = strVal(cd, "participant1");
            String p2 = strVal(cd, "participant2");
            if ((p1.equals(myUserId) && p2.equals(otherId)) ||
                    (p1.equals(otherId)  && p2.equals(myUserId)))
                return c.getId();
        }
        String chatId = UUID.randomUUID().toString().replace("-", "").substring(0, 20);
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
        return chatId;
    }

    private void openChatRoom(String chatId, String otherUserId, String otherName) {
        Intent i = new Intent(this, ChatRoomActivity.class);
        i.putExtra("chatId",      chatId);
        i.putExtra("otherUserId", otherUserId);
        i.putExtra("otherName",   otherName);
        startActivity(i);
    }

    private static String strVal(Map<String, Object> m, String k) {
        Object v = m.get(k); return v != null ? v.toString() : "";
    }
    private static int parseInt(String s) {
        try { return Integer.parseInt(s); } catch (Exception e) { return 0; }
    }

   
    public static class ChatItem {
        public final String chatId, otherUserId, otherName, lastMessage, lastTime, updatedAt;
        public final int unreadCount;

        ChatItem(String chatId, String otherUserId, String otherName,
                 String lastMessage, String lastTime, int unreadCount, String updatedAt) {
            this.chatId      = chatId;
            this.otherUserId = otherUserId;
            this.otherName   = otherName;
            this.lastMessage = lastMessage;
            this.lastTime    = lastTime;
            this.unreadCount = unreadCount;
            this.updatedAt   = updatedAt != null ? updatedAt : "";
        }
    }

    interface OnChatClickListener { void onChatClick(ChatItem chat); }

    static class ChatListAdapter extends RecyclerView.Adapter<ChatListAdapter.VH> {

        private final List<ChatItem>      list;
        private final OnChatClickListener listener;

        ChatListAdapter(List<ChatItem> list, OnChatClickListener listener) {
            this.list     = list;
            this.listener = listener;
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
            String initial = c.otherName.isEmpty() ? "?"
                    : String.valueOf(c.otherName.charAt(0)).toUpperCase(Locale.ROOT);
            h.tvAvatar.setText(initial);
            h.tvName.setText(c.otherName.isEmpty() ? "Unknown" : c.otherName);
            h.tvLastMsg.setText(c.lastMessage.isEmpty()
                    ? "Tap to start chatting" : c.lastMessage);
            h.tvTime.setText(c.lastTime);

            if (c.unreadCount > 0) {
                h.tvUnread.setVisibility(View.VISIBLE);
                h.tvUnread.setText(c.unreadCount > 99 ? "99+" : String.valueOf(c.unreadCount));
            } else {
                h.tvUnread.setVisibility(View.GONE);
            }
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