package com.ritesh.hoppeconnect;

import android.Manifest;
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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
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

    private static final int TYPE_TEXT     = 0;
    private static final int TYPE_IMAGE    = 1;
    private static final int TYPE_FILE     = 2;
    private static final int TYPE_LOCATION = 3;

    private static final String BUCKET_ID = AppwriteService.CHAT_BUCKET_ID;
    private static final String PREFS     = "hoppe_prefs";

    private ActivityChatRoomBinding binding;
    private MessageAdapter adapter;
    private final List<Message> messages = new ArrayList<>();

    private String myUserId, myName, chatId, otherName, otherUserId;
    private FusedLocationProviderClient fusedLocation;

    private final Handler pollHandler   = new Handler(Looper.getMainLooper());
    private final Runnable pollRunnable = new Runnable() {
        @Override public void run() { loadMessages(); pollHandler.postDelayed(this, 3000); }
    };

    private final ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) sendImageMessage(uri);
                }
            });

    private final ActivityResultLauncher<Intent> filePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) sendFileMessage(uri);
                }
            });

    private final ActivityResultLauncher<String[]> locationPermLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), grants -> {
                boolean ok = Boolean.TRUE.equals(grants.get(Manifest.permission.ACCESS_FINE_LOCATION));
                if (ok) sendLocationMessage();
                else Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatRoomBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // ✅ Fixed: static call, not .INSTANCE
        AppwriteService.init(getApplicationContext());
        fusedLocation = LocationServices.getFusedLocationProviderClient(this);

        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        myUserId    = prefs.getString("logged_in_user_id", "");
        myName      = prefs.getString("logged_in_name", "Me");
        chatId      = getIntent().getStringExtra("chatId");
        otherName   = getIntent().getStringExtra("otherName");
        otherUserId = getIntent().getStringExtra("otherUserId");

        binding.tvOtherName.setText(otherName);
        binding.ivBack.setOnClickListener(v -> finish());

        setupRecycler();
        setupInput();
        setupAttachmentPanel();
    }

    @Override protected void onResume() { super.onResume(); pollHandler.post(pollRunnable); }
    @Override protected void onPause()  { super.onPause();  pollHandler.removeCallbacks(pollRunnable); }

    private void setupRecycler() {
        adapter = new MessageAdapter(messages, myUserId);
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
                binding.btnSend.setVisibility(hasText ? View.VISIBLE : View.GONE);
                binding.btnAttach.setVisibility(hasText ? View.GONE : View.VISIBLE);
            }
        });

        binding.btnSend.setOnClickListener(v -> {
            String text = binding.etMessage.getText().toString().trim();
            if (!text.isEmpty()) {
                binding.etMessage.setText("");
                sendTextMessage(text);
            }
        });
    }

    private void setupAttachmentPanel() {
        binding.btnAttach.setOnClickListener(v -> {
            boolean visible = binding.attachPanel.getVisibility() == View.VISIBLE;
            binding.attachPanel.setVisibility(visible ? View.GONE : View.VISIBLE);
        });

        binding.btnSendImage.setOnClickListener(v -> {
            binding.attachPanel.setVisibility(View.GONE);
            Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            imagePickerLauncher.launch(i);
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
                        Manifest.permission.ACCESS_COARSE_LOCATION
                });
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
                String fileUrl = uploadFile(uri);
                final String name = getFileName(uri);
                runOnUiThread(() -> buildAndSendMessage(TYPE_IMAGE, null, fileUrl, name));
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this,
                        "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void sendFileMessage(Uri uri) {
        Toast.makeText(this, "Uploading file...", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            try {
                String fileUrl = uploadFile(uri);
                final String fileNameFinal = getFileName(uri);
                runOnUiThread(() -> buildAndSendMessage(TYPE_FILE, fileNameFinal, fileUrl, fileNameFinal));
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this,
                        "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void sendLocationMessage() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) return;

        fusedLocation.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                String locText = location.getLatitude() + "," + location.getLongitude();
                buildAndSendMessage(TYPE_LOCATION, locText, null, null);
            } else {
                Toast.makeText(this, "Could not get location", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void buildAndSendMessage(int type, String text, String fileUrl, String fileName) {
        final String msgId   = UUID.randomUUID().toString().replace("-", "").substring(0, 20);
        final String timeStr = new SimpleDateFormat("HH:mm", Locale.ROOT).format(new Date());

        Map<String, Object> msgData = new HashMap<>();
        msgData.put("chatId",     chatId);
        msgData.put("senderId",   myUserId);
        msgData.put("senderName", myName);
        msgData.put("type",       type);
        msgData.put("text",       text      != null ? text      : "");
        msgData.put("fileUrl",    fileUrl   != null ? fileUrl   : "");
        msgData.put("fileName",   fileName  != null ? fileName  : "");
        msgData.put("timestamp",  String.valueOf(System.currentTimeMillis()));
        msgData.put("timeStr",    timeStr);
        msgData.put("read",       false);

        // Optimistic UI update
        messages.add(new Message(msgId, myUserId, myName, type,
                text != null ? text : "",
                fileUrl != null ? fileUrl : "",
                fileName != null ? fileName : "",
                timeStr, true));
        adapter.notifyItemInserted(messages.size() - 1);
        binding.rvMessages.scrollToPosition(messages.size() - 1);

        final String finalText     = text;
        final String finalFileName = fileName;
        final int    finalType     = type;

        new Thread(() -> {
            try {
                // ✅ Fixed: static calls
                Databases db = AppwriteService.getDatabases();

                AppwriteHelper.createDocument(db, AppwriteService.DB_ID,
                        AppwriteService.COL_MSGS, msgId, msgData);

                String lastMessage =
                        finalType == TYPE_TEXT     ? (finalText != null ? finalText : "") :
                                finalType == TYPE_IMAGE    ? "📷 Photo" :
                                        finalType == TYPE_FILE     ? "📎 " + finalFileName :
                                                "📍 Location";

                Map<String, Object> chatUpdate = new HashMap<>();
                chatUpdate.put("lastMessage",     lastMessage);
                chatUpdate.put("lastMessageTime", timeStr);

                AppwriteHelper.updateDocument(db, AppwriteService.DB_ID,
                        AppwriteService.COL_CHATS, chatId, chatUpdate);

            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this,
                        "Send failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void loadMessages() {
        new Thread(() -> {
            try {
                // ✅ Fixed: static call
                Databases db = AppwriteService.getDatabases();
                List<? extends Document<?>> docs =
                        AppwriteHelper.getChatMessages(db, chatId).getDocuments();

                List<Message> fresh = new ArrayList<>();
                for (Document<?> doc : docs) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> d = (Map<String, Object>) doc.getData();
                    fresh.add(new Message(
                            doc.getId(),
                            strVal(d, "senderId"),
                            strVal(d, "senderName"),
                            d.get("type") != null ? Integer.parseInt(d.get("type").toString()) : 0,
                            strVal(d, "text"),
                            strVal(d, "fileUrl"),
                            strVal(d, "fileName"),
                            strVal(d, "timeStr"),
                            Boolean.parseBoolean(strVal(d, "read"))
                    ));
                }

                if (fresh.size() != messages.size()) {
                    runOnUiThread(() -> {
                        messages.clear();
                        messages.addAll(fresh);
                        adapter.notifyDataSetChanged();
                        if (!messages.isEmpty())
                            binding.rvMessages.scrollToPosition(messages.size() - 1);
                    });
                }
            } catch (Exception ignored) {}
        }).start();
    }

    private String uploadFile(Uri uri) throws Exception {
        String fileId    = UUID.randomUUID().toString().replace("-", "").substring(0, 20);
        String tempMime  = getContentResolver().getType(uri);
        String mimeType  = tempMime != null ? tempMime : "application/octet-stream";
        String fileName  = getFileName(uri);
        byte[] bytes     = readBytes(uri);

        // ✅ Fixed: static call
        Storage storage = AppwriteService.getStorage();

        io.appwrite.models.File uploaded = AppwriteHelper.uploadFileBlocking(
                storage, BUCKET_ID, fileId, bytes, fileName, mimeType);

        return AppwriteService.ENDPOINT
                + "/storage/buckets/" + BUCKET_ID
                + "/files/" + uploaded.getId()
                + "/view?project=" + AppwriteService.PROJECT_ID;
    }

    private byte[] readBytes(Uri uri) throws IOException {
        InputStream is = getContentResolver().openInputStream(uri);
        if (is == null) throw new IOException("Cannot open URI");
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int len;
        while ((len = is.read(buf)) != -1) bos.write(buf, 0, len);
        is.close();
        return bos.toByteArray();
    }

    private String getFileName(Uri uri) {
        String result = null;
        if ("content".equals(uri.getScheme())) {
            Cursor cursor = null;
            try {
                cursor = getContentResolver().query(uri,
                        new String[]{android.provider.OpenableColumns.DISPLAY_NAME},
                        null, null, null);
                if (cursor != null && cursor.moveToFirst()) result = cursor.getString(0);
            } catch (Exception ignored) {
            } finally { if (cursor != null) cursor.close(); }
        }
        if (result == null) {
            String path = uri.getPath();
            if (path == null) return "file";
            int idx = path.lastIndexOf('/');
            result = idx >= 0 ? path.substring(idx + 1) : path;
        }
        return result;
    }

    private static String strVal(Map<String, Object> m, String k) {
        Object v = m.get(k); return v != null ? v.toString() : "";
    }

    // ── Message model ─────────────────────────────────────────────────────────
    public static class Message {
        public final String id, senderId, senderName, text, fileUrl, fileName, timeStr;
        public final int type;
        public final boolean read;
        Message(String id, String senderId, String senderName, int type,
                String text, String fileUrl, String fileName, String timeStr, boolean read) {
            this.id = id; this.senderId = senderId; this.senderName = senderName;
            this.type = type; this.text = text; this.fileUrl = fileUrl;
            this.fileName = fileName; this.timeStr = timeStr; this.read = read;
        }
    }

    // ── MessageAdapter ────────────────────────────────────────────────────────
    static class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private static final int VIEW_SENT     = 0;
        private static final int VIEW_RECEIVED = 1;
        private final List<Message> list;
        private final String myUserId;

        MessageAdapter(List<Message> list, String myUserId) {
            this.list = list; this.myUserId = myUserId;
        }

        @Override
        public int getItemViewType(int pos) {
            return list.get(pos).senderId.equals(myUserId) ? VIEW_SENT : VIEW_RECEIVED;
        }

        @NonNull @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inf = LayoutInflater.from(parent.getContext());
            return new MsgVH(inf.inflate(
                    viewType == VIEW_SENT ? R.layout.item_message_sent : R.layout.item_message_received,
                    parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int pos) {
            Message msg = list.get(pos);
            MsgVH h = (MsgVH) holder;
            h.tvTime.setText(msg.timeStr);

            // Hide all first
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
                    Glide.with(h.ivImage.getContext()).load(msg.fileUrl).into(h.ivImage);
                    h.ivImage.setOnClickListener(v -> {
                        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(msg.fileUrl));
                        i.setDataAndType(Uri.parse(msg.fileUrl), "image/*");
                        v.getContext().startActivity(i);
                    });
                    break;
                case TYPE_FILE:
                    h.tvFile.setVisibility(View.VISIBLE);
                    h.tvFile.setText("📎 " + msg.fileName);
                    h.tvFile.setOnClickListener(v -> {
                        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(msg.fileUrl));
                        v.getContext().startActivity(i);
                    });
                    break;
                case TYPE_LOCATION:
                    h.tvLocation.setVisibility(View.VISIBLE);
                    h.tvLocation.setText("📍 Location");
                    h.tvLocation.setOnClickListener(v -> {
                        Uri geoUri = Uri.parse("geo:" + msg.text + "?q=" + msg.text);
                        v.getContext().startActivity(new Intent(Intent.ACTION_VIEW, geoUri));
                    });
                    break;
            }
        }

        @Override public int getItemCount() { return list.size(); }

        static class MsgVH extends RecyclerView.ViewHolder {
            TextView tvText, tvFile, tvLocation, tvTime;
            ImageView ivImage;
            MsgVH(@NonNull View v) {
                super(v);
                tvText     = v.findViewById(R.id.tvMessageText);
                tvFile     = v.findViewById(R.id.tvFileMessage);
                tvLocation = v.findViewById(R.id.tvLocationMessage);
                tvTime     = v.findViewById(R.id.tvMessageTime);
                ivImage    = v.findViewById(R.id.ivImageMessage);
            }
        }
    }
}