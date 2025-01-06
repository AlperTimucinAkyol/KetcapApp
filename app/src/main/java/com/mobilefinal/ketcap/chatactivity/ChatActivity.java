package com.mobilefinal.ketcap.chatactivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.mobilefinal.ketcap.R;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class ChatActivity extends AppCompatActivity {
    String receiverId,receiverName,senderRoom,receiverRoom;
    DatabaseReference dbReferenceSender,dbReferenceReceiver,userReference;
    ImageView sendBtn;
    EditText messageText;
    RecyclerView recyclerView;
    MessageAdapter messageAdapter;
    Long timeStamp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        userReference = FirebaseDatabase.getInstance().getReference("users");
        receiverId = getIntent().getStringExtra("id");
        receiverName = getIntent().getStringExtra("name");

        getSupportActionBar().setTitle(receiverName);
        if (receiverId != null) {
            senderRoom = FirebaseAuth.getInstance().getUid() +receiverId;
            receiverRoom = receiverId + FirebaseAuth.getInstance().getUid();
        }
        sendBtn = findViewById(R.id.sendMessageIcon);
        messageAdapter = new MessageAdapter(this);
        recyclerView = findViewById(R.id.chatrecycler);
        messageText = findViewById(R.id.messageEdit);

        recyclerView.setAdapter(messageAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        dbReferenceSender = FirebaseDatabase.getInstance().getReference("chats").child(senderRoom);
        dbReferenceReceiver = FirebaseDatabase.getInstance().getReference("chats").child(receiverRoom);

        dbReferenceSender.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<MessageModel> messages = new ArrayList<>();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    MessageModel messageModel = dataSnapshot.getValue(MessageModel.class);
                    messages.add(messageModel);
                }
                messages.sort((m1, m2) -> Long.compare(m1.getTimeStamp(), m2.getTimeStamp()));
                messageAdapter.clear();
                for (MessageModel message : messages) {
                    messageAdapter.add(message);
                }
                messageAdapter.notifyDataSetChanged();
                if (messages.size() > 0) {
                    recyclerView.scrollToPosition(messages.size() - 1);
                }
            }


            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String message = messageText.getText().toString();
                if (message.trim().length()>0) {
                    SendMessage(message);
                }else{
                    Toast.makeText(ChatActivity.this, "Message cannot be empty", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void SendMessage(String message) {
        String messageId = UUID.randomUUID().toString();
        MessageModel messageModel = new MessageModel(messageId, FirebaseAuth.getInstance().getUid(), message);
        messageModel.setTimeStamp(new Date().getTime());
        messageAdapter.add(messageModel);

        dbReferenceSender.child(messageId).setValue(messageModel)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(ChatActivity.this, "Failed to send message", Toast.LENGTH_SHORT).show();
                    }
                });
        dbReferenceReceiver.child(messageId).setValue(messageModel);

        recyclerView.scrollToPosition(messageAdapter.getItemCount() - 1);
         messageText.setText("");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.logout) {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(ChatActivity.this, SigninActivity.class));
            finish();
            return true;
        }
        return false;
    }
}