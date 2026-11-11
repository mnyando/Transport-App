package com.example.transportapp;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Chat for one bus, shared by the parents of children on that bus, the driver and the school.
 * Messages are stored in {@code busGroups/{vehicle}/messages}, so they survive closing the
 * screen, everyone in the group sees them live, and the school keeps the history.
 *
 * Extras: {@code vehicle} (required), {@code senderRole} ("parent", "driver" or "school"),
 * {@code senderName} (shown above each message).
 */
public class ChatActivity extends AppCompatActivity {

    private static final String TAG = "ChatActivity";
    private static final int MAX_MESSAGES = 200;

    private RecyclerView chatRecyclerView;
    private ChatAdapter chatAdapter;
    private final List<ChatAdapter.Message> messageList = new ArrayList<>();
    private EditText messageInput;
    private Button sendButton;

    private FirebaseFirestore db;
    private ListenerRegistration listener;
    private String vehicle, senderRole, senderName, myUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        vehicle = getIntent().getStringExtra("vehicle");
        if (user == null || BusGroups.docId(vehicle).isEmpty()) {
            Toast.makeText(this, "Chat is not available for this bus yet.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        myUid = user.getUid();
        senderRole = getIntent().getStringExtra("senderRole");
        senderName = getIntent().getStringExtra("senderName");
        if (senderRole == null) senderRole = "parent";
        if (senderName == null || senderName.isEmpty()) {
            senderName = user.getDisplayName() != null ? user.getDisplayName() : capitalise(senderRole);
        }
        setTitle("Bus " + vehicle + " chat");

        db = FirebaseFirestore.getInstance();
        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        messageInput = findViewById(R.id.messageInput);
        sendButton = findViewById(R.id.sendButton);

        chatAdapter = new ChatAdapter(messageList, myUid);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        chatRecyclerView.setLayoutManager(layoutManager);
        chatRecyclerView.setAdapter(chatAdapter);

        sendButton.setOnClickListener(v -> sendMessage());
        listenForMessages();
    }

    private void listenForMessages() {
        listener = db.collection(BusGroups.COLLECTION).document(BusGroups.docId(vehicle))
                .collection(BusGroups.MESSAGES)
                .orderBy("sentAtMillis", Query.Direction.ASCENDING)
                .limitToLast(MAX_MESSAGES)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Could not load messages", e);
                        Toast.makeText(this, "Could not load messages for this bus.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (snapshots == null) return;
                    messageList.clear();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        messageList.add(new ChatAdapter.Message(
                                doc.getString("senderUid"),
                                doc.getString("senderName"),
                                doc.getString("senderRole"),
                                doc.getString("text")));
                    }
                    chatAdapter.notifyDataSetChanged();
                    if (!messageList.isEmpty()) {
                        chatRecyclerView.scrollToPosition(messageList.size() - 1);
                    }
                });
    }

    private void sendMessage() {
        String text = messageInput.getText().toString().trim();
        if (text.isEmpty()) return;
        if (text.length() > 1000) text = text.substring(0, 1000);

        Map<String, Object> message = new HashMap<>();
        message.put("senderUid", myUid);
        message.put("senderName", senderName);
        message.put("senderRole", senderRole);
        message.put("text", text);
        message.put("sentAtMillis", System.currentTimeMillis());
        message.put("sentAt", FieldValue.serverTimestamp());

        sendButton.setEnabled(false);
        db.collection(BusGroups.COLLECTION).document(BusGroups.docId(vehicle))
                .collection(BusGroups.MESSAGES).add(message)
                .addOnSuccessListener(ref -> messageInput.setText(""))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Could not send message", e);
                    Toast.makeText(this, "Message not sent. Check your connection and try again.", Toast.LENGTH_SHORT).show();
                })
                .addOnCompleteListener(t -> sendButton.setEnabled(true));
    }

    private static String capitalise(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (listener != null) listener.remove();
    }
}
