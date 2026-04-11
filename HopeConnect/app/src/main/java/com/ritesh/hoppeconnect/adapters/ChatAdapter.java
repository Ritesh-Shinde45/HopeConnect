package com.ritesh.hoppeconnect.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ritesh.hoppeconnect.R;
import com.ritesh.hoppeconnect.models.ChatMessage;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_SENT     = 1;
    private static final int TYPE_RECEIVED = 2;

    private final List<ChatMessage> messages;

    public ChatAdapter(List<ChatMessage> messages) {
        this.messages = messages;
    }

    @Override
    public int getItemViewType(int position) {
        return messages.get(position).isMine ? TYPE_SENT : TYPE_RECEIVED;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inf = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_SENT) {
            View v = inf.inflate(R.layout.item_message_sent, parent, false);
            return new SentViewHolder(v);
        } else {
            View v = inf.inflate(R.layout.item_message_received, parent, false);
            return new ReceivedViewHolder(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage msg = messages.get(position);
        if (holder instanceof SentViewHolder) {
            ((SentViewHolder) holder).text.setText(msg.text);
        } else if (holder instanceof ReceivedViewHolder) {
            ((ReceivedViewHolder) holder).text.setText(msg.text);
        }
    }

    @Override
    public int getItemCount() { return messages.size(); }

    static class SentViewHolder extends RecyclerView.ViewHolder {
        TextView text;
        SentViewHolder(View v) {
            super(v);
           
            text = v.findViewById(R.id.tvMessageText);
        }
    }

    static class ReceivedViewHolder extends RecyclerView.ViewHolder {
        TextView text;
        ReceivedViewHolder(View v) {
            super(v);
           
            text = v.findViewById(R.id.tvMessageText);
        }
    }
}