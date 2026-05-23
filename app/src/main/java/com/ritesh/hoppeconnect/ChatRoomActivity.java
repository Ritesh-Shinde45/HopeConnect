package com.ritesh.hoppeconnect;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsetsController;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.ritesh.hoppeconnect.databinding.ActivityChatRoomBinding;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
import io.appwrite.services.Storage;

public class ChatRoomActivity extends AppCompatActivity {

    private static final String TAG = "ChatRoomActivity";

    public static final int TYPE_TEXT     = 0;
    public static final int TYPE_IMAGE    = 1;
    public static final int TYPE_FILE     = 2;
    public static final int TYPE_LOCATION = 3;

    private static final String BUCKET_ID = AppwriteService.CHAT_BUCKET_ID;
    private static final String PREFS     = "hoppe_prefs";

    private ActivityChatRoomBinding binding;
    private MessageAdapter adapter;
    private final List<Message> messages = new ArrayList<>();

    private String myUserId, myName, chatId, otherName, otherUserId;
    private FusedLocationProviderClient fusedLocation;

    private final Handler  pollHandler  = new Handler(Looper.getMainLooper());
    private final Runnable pollRunnable = new Runnable() {
        @Override public void run() {
            loadMessages();
            pollHandler.postDelayed(this, 3000);
        }
    };

    private final ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            Uri uri = result.getData().getData();
                            if (uri != null) sendImageMessage(uri);
                        }
                    });

    private final ActivityResultLauncher<Intent> filePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            Uri uri = result.getData().getData();
                            if (uri != null) sendFileMessage(uri);
                        }
                    });

    private final ActivityResultLauncher<String[]> locationPermLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(),
                    grants -> {
                        boolean ok = Boolean.TRUE.equals(
                                grants.get(Manifest.permission.ACCESS_FINE_LOCATION));
                        if (ok) sendLocationMessage();
                        else Toast.makeText(this,
                                "Location permission denied", Toast.LENGTH_SHORT).show();
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatRoomBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        AppwriteService.init(getApplicationContext());
        fusedLocation = LocationServices.getFusedLocationProviderClient(this);

        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        myUserId    = prefs.getString("logged_in_user_id", "");
        myName      = prefs.getString("logged_in_name",    "Me");
        chatId      = getIntent().getStringExtra("chatId");
        otherName   = getIntent().getStringExtra("otherName");
        otherUserId = getIntent().getStringExtra("otherUserId");

        if (chatId == null || chatId.isEmpty()) {
            Toast.makeText(this, "Invalid chat", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        binding.tvOtherName.setText(otherName != null && !otherName.isEmpty() ? otherName : "User");
        binding.tvOtherUsername.setText("loading...");
        binding.tvAvatar.setVisibility(View.VISIBLE);
        binding.ivAvatar.setVisibility(View.GONE);
        binding.ivBack.setOnClickListener(v -> finish());

        loadOtherUserProfile();
        setupRecycler();
        setupInput();
        setupAttachmentPanel();

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            getWindow().getInsetsController().setSystemBarsAppearance(
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS);
        }
    }

    @Override protected void onResume() { super.onResume(); pollHandler.post(pollRunnable); }
    @Override protected void onPause()  { super.onPause();  pollHandler.removeCallbacks(pollRunnable); }

    private void loadOtherUserProfile() {
        if (otherUserId == null || otherUserId.isEmpty()) return;
        new Thread(() -> {
            try {
                Databases db = AppwriteService.getDatabases();
                Document<?> doc = AppwriteHelper.getDocument(
                        db, AppwriteService.DB_ID, AppwriteService.COL_USERS, otherUserId);

                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) doc.getData();

                String name     = strVal(data, "name");
                String username = strVal(data, "username");
                String photoId  = strVal(data, "photoId");
                if (name.isEmpty()) name = username.isEmpty() ? "User" : username;

                final String fn = name, fu = username, fp = photoId;
                runOnUiThread(() -> {
                    binding.tvOtherName.setText(fn);
                    binding.tvOtherUsername.setText(fu.isEmpty() ? "tap to view info" : "@" + fu);
                    if (!fp.isEmpty()) {
                        binding.ivAvatar.setVisibility(View.VISIBLE);
                        binding.tvAvatar.setVisibility(View.GONE);
                        RelativeLayout.LayoutParams p =
                                (RelativeLayout.LayoutParams) binding.nameBlock.getLayoutParams();
                        p.removeRule(RelativeLayout.END_OF);
                        p.addRule(RelativeLayout.END_OF, R.id.ivAvatar);
                        binding.nameBlock.setLayoutParams(p);
                        String photoUrl = AppwriteService.ENDPOINT
                                + "/storage/buckets/" + AppwriteService.USERS_BUCKET_ID
                                + "/files/" + fp + "/view?project=" + AppwriteService.PROJECT_ID;
                        Glide.with(this).load(photoUrl)
                                .placeholder(R.drawable.person_placeholder)
                                .error(R.drawable.person_placeholder)
                                .circleCrop().into(binding.ivAvatar);
                    } else {
                        String ini = !fn.isEmpty()
                                ? String.valueOf(fn.charAt(0)).toUpperCase(Locale.ROOT) : "?";
                        binding.tvAvatar.setText(ini);
                        binding.tvAvatar.setVisibility(View.VISIBLE);
                        binding.ivAvatar.setVisibility(View.GONE);
                        RelativeLayout.LayoutParams p =
                                (RelativeLayout.LayoutParams) binding.nameBlock.getLayoutParams();
                        p.removeRule(RelativeLayout.END_OF);
                        p.addRule(RelativeLayout.END_OF, R.id.tvAvatar);
                        binding.nameBlock.setLayoutParams(p);
                    }
                });
            } catch (Exception e) {
                Log.w(TAG, "loadOtherUserProfile failed: " + e.getMessage());
                runOnUiThread(() -> binding.tvOtherUsername.setText("tap to view info"));
            }
        }).start();
    }

    private void setupRecycler() {
        adapter = new MessageAdapter(messages, myUserId, this);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setStackFromEnd(true);
        binding.rvMessages.setLayoutManager(llm);
        binding.rvMessages.setAdapter(adapter);
    }

    private void setupInput() {
        binding.etMessage.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {
                boolean hasText = !s.toString().trim().isEmpty();
                binding.btnSend.setVisibility(hasText   ? View.VISIBLE : View.GONE);
                binding.btnAttach.setVisibility(hasText ? View.GONE    : View.VISIBLE);
            }
        });
        binding.btnSend.setOnClickListener(v -> {
            String text = binding.etMessage.getText().toString().trim();
            if (!text.isEmpty()) { binding.etMessage.setText(""); sendTextMessage(text); }
        });
        binding.etMessage.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND) {
                String text = binding.etMessage.getText().toString().trim();
                if (!text.isEmpty()) { binding.etMessage.setText(""); sendTextMessage(text); }
                return true;
            }
            return false;
        });
    }

    private void setupAttachmentPanel() {
        binding.btnAttach.setOnClickListener(v -> {
            boolean shown = binding.attachPanel.getVisibility() == View.VISIBLE;
            binding.attachPanel.setVisibility(shown ? View.GONE : View.VISIBLE);
        });
        binding.btnSendImage.setOnClickListener(v -> {
            binding.attachPanel.setVisibility(View.GONE);
            imagePickerLauncher.launch(new Intent(Intent.ACTION_PICK,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI));
        });
        binding.btnSendFile.setOnClickListener(v -> {
            binding.attachPanel.setVisibility(View.GONE);
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.setType("*/*");
            filePickerLauncher.launch(i);
        });
        binding.btnSendLocation.setOnClickListener(v -> {
            binding.attachPanel.setVisibility(View.GONE);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                sendLocationMessage();
            } else {
                locationPermLauncher.launch(new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION});
            }
        });
    }

    private void sendTextMessage(String text) {
        buildAndSendMessage(TYPE_TEXT, text, null, null);
    }

    private void sendImageMessage(Uri uri) {
        Toast.makeText(this, "Uploading image...", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            try {
                String[] res = uploadFile(uri);
                runOnUiThread(() -> buildAndSendMessage(TYPE_IMAGE, null, res[0], res[1]));
            } catch (Exception e) {
                Log.e(TAG, "Image upload failed", e);
                runOnUiThread(() -> Toast.makeText(this,
                        "Image upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void sendFileMessage(Uri uri) {
        Toast.makeText(this, "Uploading file...", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            try {
                String[] res = uploadFile(uri);
                runOnUiThread(() -> buildAndSendMessage(TYPE_FILE, res[1], res[0], res[1]));
            } catch (Exception e) {
                Log.e(TAG, "File upload failed", e);
                runOnUiThread(() -> Toast.makeText(this,
                        "File upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void sendLocationMessage() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) return;
        fusedLocation.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                buildAndSendMessage(TYPE_LOCATION,
                        location.getLatitude() + "," + location.getLongitude(), null, null);
            } else {
                Toast.makeText(this, "Could not get location", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void buildAndSendMessage(int type, String text, String fileUrl, String fileName) {
        final String msgId   = generateAppwriteId();
        final String timeStr = new SimpleDateFormat("hh:mm a", Locale.ROOT).format(new Date());
        final String safeText     = text     != null ? text     : "";
        final String safeFileUrl  = fileUrl  != null ? fileUrl  : "";
        final String safeFileName = fileName != null ? fileName : "";

        Map<String, Object> msgData = new HashMap<>();
        msgData.put("chatId",     chatId);
        msgData.put("senderId",   myUserId);
        msgData.put("senderName", myName);
        msgData.put("type",       type);
        msgData.put("text",       safeText);
        msgData.put("timestamp",  String.valueOf(System.currentTimeMillis()));
        msgData.put("timeStr",    timeStr);
        msgData.put("read",       false);
        msgData.put("delivered",  false);
        msgData.put("deletedFor", new ArrayList<String>());

        if (!safeFileUrl.isEmpty())  msgData.put("fileUrl",  safeFileUrl);
        if (!safeFileName.isEmpty()) msgData.put("fileName", safeFileName);

        messages.add(new Message(msgId, myUserId, myName, type,
                safeText, safeFileUrl, safeFileName, timeStr, false, false,
                new ArrayList<>()));
        adapter.notifyItemInserted(messages.size() - 1);
        binding.rvMessages.scrollToPosition(messages.size() - 1);

        new Thread(() -> {
            try {
                Databases db = AppwriteService.getDatabases();
                AppwriteHelper.createDocument(db, AppwriteService.DB_ID,
                        AppwriteService.COL_MSGS, msgId, msgData);

                Map<String, Object> delivered = new HashMap<>();
                delivered.put("delivered", true);
                AppwriteHelper.updateDocument(db, AppwriteService.DB_ID,
                        AppwriteService.COL_MSGS, msgId, delivered);

                String preview =
                        type == TYPE_TEXT     ? safeText     :
                                type == TYPE_IMAGE    ? "Photo"      :
                                        type == TYPE_FILE     ? safeFileName :
                                                "Location";

                Map<String, Object> chatUpdate = new HashMap<>();
                chatUpdate.put("lastMessage",     preview);
                chatUpdate.put("lastMessageTime", timeStr);
                AppwriteHelper.updateDocument(db, AppwriteService.DB_ID,
                        AppwriteService.COL_CHATS, chatId, chatUpdate);

                runOnUiThread(() -> {
                    for (int i = 0; i < messages.size(); i++) {
                        if (messages.get(i).id.equals(msgId)) {
                            Message old = messages.get(i);
                            messages.set(i, new Message(old.id, old.senderId, old.senderName,
                                    old.type, old.text, old.fileUrl, old.fileName,
                                    old.timeStr, true, false, old.deletedFor));
                            adapter.notifyItemChanged(i);
                            break;
                        }
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "Send failed", e);
                runOnUiThread(() -> Toast.makeText(this,
                        "Send failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void loadMessages() {
        new Thread(() -> {
            try {
                Databases db = AppwriteService.getDatabases();
                List<? extends Document<?>> docs =
                        AppwriteHelper.getChatMessages(db, chatId).getDocuments();

                List<Message> fresh = new ArrayList<>();
                for (Document<?> doc : docs) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> d = (Map<String, Object>) doc.getData();

                    List<String> deletedFor = new ArrayList<>();
                    Object df = d.get("deletedFor");
                    if (df instanceof List) {
                        for (Object o : (List<?>) df) deletedFor.add(o.toString());
                    }

                    if (deletedFor.contains(myUserId)) continue;

                    fresh.add(new Message(
                            doc.getId(),
                            strVal(d, "senderId"),
                            strVal(d, "senderName"),
                            d.get("type") != null ? parseInt(d.get("type").toString()) : 0,
                            strVal(d, "text"),
                            strVal(d, "fileUrl"),
                            strVal(d, "fileName"),
                            strVal(d, "timeStr"),
                            Boolean.parseBoolean(strVal(d, "delivered")),
                            Boolean.parseBoolean(strVal(d, "read")),
                            deletedFor
                    ));
                }

                markIncomingAsRead(db, docs);

                boolean changed = fresh.size() != messages.size();
                if (!changed) {
                    for (int i = 0; i < fresh.size(); i++) {
                        Message f = fresh.get(i);
                        Message m = messages.get(i);
                        if (!f.id.equals(m.id) || f.delivered != m.delivered || f.read != m.read) {
                            changed = true;
                            break;
                        }
                    }
                }

                if (changed) {
                    final List<Message> finalFresh = fresh;
                    runOnUiThread(() -> {
                        messages.clear();
                        messages.addAll(finalFresh);
                        adapter.notifyDataSetChanged();
                        if (!messages.isEmpty())
                            binding.rvMessages.scrollToPosition(messages.size() - 1);
                    });
                }
            } catch (Exception e) {
                Log.w(TAG, "loadMessages failed: " + e.getMessage());
            }
        }).start();
    }

    private void markIncomingAsRead(Databases db, List<? extends Document<?>> docs) {
        for (Document<?> doc : docs) {
            @SuppressWarnings("unchecked")
            Map<String, Object> d = (Map<String, Object>) doc.getData();
            String senderId = strVal(d, "senderId");
            boolean alreadyRead = Boolean.parseBoolean(strVal(d, "read"));
            if (!senderId.equals(myUserId) && !alreadyRead) {
                try {
                    Map<String, Object> update = new HashMap<>();
                    update.put("read", true);
                    AppwriteHelper.updateDocument(db, AppwriteService.DB_ID,
                            AppwriteService.COL_MSGS, doc.getId(), update);
                } catch (Exception ignored) {}
            }
        }
    }

    void deleteMessageForMe(Message msg, int position) {
        new Thread(() -> {
            try {
                Databases db = AppwriteService.getDatabases();
                @SuppressWarnings("unchecked")
                Document<?> doc = AppwriteHelper.getDocument(db, AppwriteService.DB_ID,
                        AppwriteService.COL_MSGS, msg.id);
                @SuppressWarnings("unchecked")
                Map<String, Object> d = (Map<String, Object>) doc.getData();

                List<String> deletedFor = new ArrayList<>();
                Object df = d.get("deletedFor");
                if (df instanceof List) {
                    for (Object o : (List<?>) df) deletedFor.add(o.toString());
                }
                if (!deletedFor.contains(myUserId)) deletedFor.add(myUserId);

                Map<String, Object> update = new HashMap<>();
                update.put("deletedFor", deletedFor);
                AppwriteHelper.updateDocument(db, AppwriteService.DB_ID,
                        AppwriteService.COL_MSGS, msg.id, update);

                runOnUiThread(() -> {
                    messages.remove(position);
                    adapter.notifyItemRemoved(position);
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this,
                        "Delete failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    void deleteMessageForAll(Message msg, int position) {
        new AlertDialog.Builder(this)
                .setTitle("Delete for everyone?")
                .setMessage("This message will be deleted for all participants.")
                .setPositiveButton("Delete", (dialog, which) -> new Thread(() -> {
                    try {
                        Databases db = AppwriteService.getDatabases();
                        Map<String, Object> update = new HashMap<>();
                        update.put("text",      "This message was deleted");
                        update.put("type",      TYPE_TEXT);
                        update.put("fileUrl",   "");
                        update.put("fileName",  "");
                        update.put("deletedAll", true);
                        AppwriteHelper.updateDocument(db, AppwriteService.DB_ID,
                                AppwriteService.COL_MSGS, msg.id, update);
                        runOnUiThread(() -> {
                            messages.remove(position);
                            adapter.notifyItemRemoved(position);
                        });
                    } catch (Exception e) {
                        runOnUiThread(() -> Toast.makeText(this,
                                "Delete failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    }
                }).start())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private String[] uploadFile(Uri uri) throws Exception {
        String fileId   = generateAppwriteId();
        String mimeType = getContentResolver().getType(uri);
        if (mimeType == null) mimeType = "application/octet-stream";
        String fileName = getFileName(uri);
        byte[] bytes    = readBytes(uri);
        Storage storage = AppwriteService.getStorage();
        io.appwrite.models.File uploaded = AppwriteHelper.uploadFileBlocking(
                storage, BUCKET_ID, fileId, bytes, fileName, mimeType);
        String fileUrl = AppwriteService.ENDPOINT
                + "/storage/buckets/" + BUCKET_ID
                + "/files/" + uploaded.getId()
                + "/view?project=" + AppwriteService.PROJECT_ID;
        return new String[]{ fileUrl, fileName };
    }

    private byte[] readBytes(Uri uri) throws IOException {
        InputStream is = getContentResolver().openInputStream(uri);
        if (is == null) throw new IOException("Cannot open URI");
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int len;
        while ((len = is.read(buf)) != -1) bos.write(buf, 0, len);
        is.close();
        return bos.toByteArray();
    }

    private String getFileName(Uri uri) {
        String result = null;
        if ("content".equals(uri.getScheme())) {
            try (Cursor cursor = getContentResolver().query(uri,
                    new String[]{android.provider.OpenableColumns.DISPLAY_NAME},
                    null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) result = cursor.getString(0);
            } catch (Exception ignored) {}
        }
        if (result == null) {
            String path = uri.getPath();
            if (path == null) return "file";
            int idx = path.lastIndexOf('/');
            result = idx >= 0 ? path.substring(idx + 1) : path;
        }
        return result;
    }

    private static String generateAppwriteId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private static String strVal(Map<String, Object> m, String k) {
        Object v = m.get(k); return v != null ? v.toString() : "";
    }

    private static int parseInt(String s) {
        try { return Integer.parseInt(s); } catch (Exception e) { return 0; }
    }

    public static class Message {
        public final String id, senderId, senderName, text, fileUrl, fileName, timeStr;
        public final int type;
        public final boolean delivered, read;
        public final List<String> deletedFor;

        Message(String id, String senderId, String senderName, int type,
                String text, String fileUrl, String fileName, String timeStr,
                boolean delivered, boolean read, List<String> deletedFor) {
            this.id         = id;
            this.senderId   = senderId;
            this.senderName = senderName;
            this.type       = type;
            this.text       = text;
            this.fileUrl    = fileUrl;
            this.fileName   = fileName;
            this.timeStr    = timeStr;
            this.delivered  = delivered;
            this.read       = read;
            this.deletedFor = deletedFor != null ? deletedFor : new ArrayList<>();
        }
    }

    static class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private static final int VIEW_SENT     = 0;
        private static final int VIEW_RECEIVED = 1;

        private final List<Message>      list;
        private final String             myUserId;
        private final ChatRoomActivity   activity;

        MessageAdapter(List<Message> list, String myUserId, ChatRoomActivity activity) {
            this.list     = list;
            this.myUserId = myUserId;
            this.activity = activity;
        }

        @Override
        public int getItemViewType(int pos) {
            return list.get(pos).senderId.equals(myUserId) ? VIEW_SENT : VIEW_RECEIVED;
        }

        @NonNull @Override
        public RecyclerView.ViewHolder onCreateViewHolder(
                @NonNull ViewGroup parent, int viewType) {
            LayoutInflater inf = LayoutInflater.from(parent.getContext());
            return new MsgVH(inf.inflate(
                    viewType == VIEW_SENT
                            ? R.layout.item_message_sent
                            : R.layout.item_message_received,
                    parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int pos) {
            Message msg = list.get(pos);
            MsgVH   h   = (MsgVH) holder;
            boolean isSent = msg.senderId.equals(myUserId);

            h.tvTime.setText(msg.timeStr);
            h.tvText.setVisibility(View.GONE);
            h.ivImage.setVisibility(View.GONE);
            h.tvFile.setVisibility(View.GONE);
            h.tvLocation.setVisibility(View.GONE);

            switch (msg.type) {
                case TYPE_TEXT:
                    h.tvText.setVisibility(View.VISIBLE);
                    h.tvText.setText(msg.text);
                    break;
                case TYPE_IMAGE:
                    h.ivImage.setVisibility(View.VISIBLE);
                    Glide.with(h.ivImage.getContext())
                            .load(msg.fileUrl)
                            .placeholder(R.drawable.person_placeholder)
                            .into(h.ivImage);
                    h.ivImage.setOnClickListener(v -> {
                        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(msg.fileUrl));
                        i.setDataAndType(Uri.parse(msg.fileUrl), "image/*");
                        v.getContext().startActivity(i);
                    });
                    break;
                case TYPE_FILE:
                    h.tvFile.setVisibility(View.VISIBLE);
                    h.tvFile.setText("File: " + msg.fileName);
                    h.tvFile.setOnClickListener(v -> {
                        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(msg.fileUrl));
                        v.getContext().startActivity(i);
                    });
                    break;
                case TYPE_LOCATION:
                    h.tvLocation.setVisibility(View.VISIBLE);
                    h.tvLocation.setText("Tap to open location");
                    h.tvLocation.setOnClickListener(v -> {
                        Uri geo = Uri.parse("geo:" + msg.text + "?q=" + msg.text);
                        v.getContext().startActivity(new Intent(Intent.ACTION_VIEW, geo));
                    });
                    break;
            }

            if (h.tvTick != null) {
                if (isSent) {
                    h.tvTick.setVisibility(View.VISIBLE);
                    if (msg.read) {
                        h.tvTick.setText("✓✓");
                        h.tvTick.setTextColor(0xFF34B7F1);
                    } else if (msg.delivered) {
                        h.tvTick.setText("✓✓");
                        h.tvTick.setTextColor(0xFFAAAAAA);
                    } else {
                        h.tvTick.setText("✓");
                        h.tvTick.setTextColor(0xFFAAAAAA);
                    }
                } else {
                    h.tvTick.setVisibility(View.GONE);
                }
            }

            View bubble = h.itemView.findViewWithTag("bubble");
            if (bubble == null) bubble = h.itemView;
            final View finalBubble = bubble;

            finalBubble.setOnLongClickListener(v -> {
                showMessageMenu(v, msg, pos, isSent);
                return true;
            });
        }

        private void showMessageMenu(View anchor, Message msg, int pos, boolean isSent) {
            PopupMenu popup = new PopupMenu(anchor.getContext(), anchor);

            if (msg.type == TYPE_TEXT) {
                popup.getMenu().add(0, 1, 0, "Copy");
            }
            popup.getMenu().add(0, 2, 1, "Delete for me");
            if (isSent) {
                popup.getMenu().add(0, 3, 2, "Delete for everyone");
            }
            popup.getMenu().add(0, 4, 3, "Forward");
            if (msg.type == TYPE_TEXT) {
                popup.getMenu().add(0, 5, 4, "Select text");
            }

            popup.setOnMenuItemClickListener(item -> {
                switch (item.getItemId()) {
                    case 1:
                        ClipboardManager cm = (ClipboardManager)
                                anchor.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                        cm.setPrimaryClip(ClipData.newPlainText("message", msg.text));
                        Toast.makeText(anchor.getContext(), "Copied", Toast.LENGTH_SHORT).show();
                        return true;
                    case 2:
                        activity.deleteMessageForMe(msg, pos);
                        return true;
                    case 3:
                        activity.deleteMessageForAll(msg, pos);
                        return true;
                    case 4:
                        Intent share = new Intent(Intent.ACTION_SEND);
                        share.setType("text/plain");
                        share.putExtra(Intent.EXTRA_TEXT,
                                msg.type == TYPE_TEXT ? msg.text : msg.fileUrl);
                        anchor.getContext().startActivity(
                                Intent.createChooser(share, "Forward via"));
                        return true;
                    case 5:
                        android.text.ClipboardManager oldCm =
                                (android.text.ClipboardManager)
                                        anchor.getContext().getSystemService(
                                                Context.CLIPBOARD_SERVICE);
                        Toast.makeText(anchor.getContext(),
                                "Long-press text to select", Toast.LENGTH_SHORT).show();
                        return true;
                }
                return false;
            });
            popup.show();
        }

        @Override public int getItemCount() { return list.size(); }

        static class MsgVH extends RecyclerView.ViewHolder {
            TextView  tvText, tvFile, tvLocation, tvTime, tvTick;
            ImageView ivImage;

            MsgVH(@NonNull View v) {
                super(v);
                tvText     = v.findViewById(R.id.tvMessageText);
                tvFile     = v.findViewById(R.id.tvFileMessage);
                tvLocation = v.findViewById(R.id.tvLocationMessage);
                tvTime     = v.findViewById(R.id.tvMessageTime);
                tvTick     = v.findViewById(R.id.tvMessageTick);
                ivImage    = v.findViewById(R.id.ivImageMessage);
            }
        }
    }
}