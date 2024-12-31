package echonest.sociogram.connectus;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.animation.ObjectAnimator;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import echonest.sociogram.connectus.Adapters.AdapterChat;
import echonest.sociogram.connectus.Models.ModelChat;
import com.example.connectus.R;
import com.example.connectus.databinding.ActivityChatDetailBinding;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;


public class ChatDetailActivity extends AppCompatActivity {
    private CustomItemAnimator itemAnimator;
    private ActivityChatDetailBinding binding;
    private FirebaseDatabase database;
    private FirebaseAuth firebaseAuth;
    private DatabaseReference usersDbRef;
    private String hisUid, myUid;
    private String hisImage;
    private ValueEventListener messagesListener;

    private AdapterChat adapterChat;
    private final List<ModelChat> chatList = new ArrayList<>();

    private static final int GALLERY_REQUEST_CODE = 400;
    private static final int VIDEO_REQUEST_CODE = 500;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStatusBarColor(R.color.black);

        binding = ActivityChatDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initializeChatRecyclerView();
        initializeTextWatcher();
        preloadDatabaseConnection();

        firebaseAuth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();

        Intent intent = getIntent();
        hisUid = intent.getStringExtra("hisUid");

        loadUserDetails();

        binding.sendbtn.setOnClickListener(v -> sendMessage());
        binding.attachBtn.setOnClickListener(v -> pickImageFromGallery());
        binding.attachBtnVideo.setOnClickListener(v -> pickVideoFromGallery());
        binding.backArrow.setOnClickListener(v -> finish());

        loadMessages();
    }

    private void setStatusBarColor(int colorId) {
        if (Build.VERSION.SDK_INT >= 21) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(getResources().getColor(colorId));
        }
    }

    private void preloadDatabaseConnection() {
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("Dummy");
        dbRef.setValue("init").addOnCompleteListener(task -> dbRef.removeValue());
    }

    private void initializeChatRecyclerView() {
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setStackFromEnd(true);

        adapterChat = new AdapterChat(this, chatList, hisImage);
        binding.chatRecyclerView.setAdapter(adapterChat);
        binding.chatRecyclerView.setHasFixedSize(true);
        binding.chatRecyclerView.setLayoutManager(linearLayoutManager);
        binding.chatRecyclerView.setNestedScrollingEnabled(false);

    }

    private void initializeTextWatcher() {
        binding.messageEt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    binding.attachmentLayout.setVisibility(View.GONE);
                    binding.likebtn.setVisibility(View.GONE);
                    binding.sendbtn.setVisibility(View.VISIBLE);
                } else {
                    binding.attachmentLayout.setVisibility(View.VISIBLE);
                    binding.likebtn.setVisibility(View.VISIBLE);
                    binding.sendbtn.setVisibility(View.GONE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void loadUserDetails() {
        usersDbRef = database.getReference("Users");
        Query userQuery = usersDbRef.orderByChild("userId").equalTo(hisUid);

        userQuery.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String name = "" + ds.child("name").getValue();
                    hisImage = "" + ds.child("profilePhoto").getValue();
                    String onlineStatus = "" + ds.child("onlineStatus").getValue();

                    binding.nameTv.setText(name);
                    updateUserStatus(onlineStatus);

                    Glide.with(ChatDetailActivity.this)
                            .load(hisImage)
                            .placeholder(R.drawable.avatar)
                            .into(binding.profileIv);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void updateUserStatus(String onlineStatus) {
        if (onlineStatus.equals("online")) {
            binding.userStatusTv.setText(onlineStatus);
        } else {
            try {
                Calendar cal = Calendar.getInstance(Locale.ENGLISH);
                cal.setTimeInMillis(Long.parseLong(onlineStatus));
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy hh:mm aa", Locale.ENGLISH);
                String dateTime = sdf.format(cal.getTime());
                binding.userStatusTv.setText("Last seen: " + dateTime);
            } catch (NumberFormatException e) {
                Log.e("TimeStamp Error", "Invalid timestamp format", e);
            }
        }
    }

    private void sendMessage() {
        String message = binding.messageEt.getText().toString().trim();
        if (!TextUtils.isEmpty(message)) {
            // Create a temporary message object
            String timestamp = String.valueOf(System.currentTimeMillis());
            ModelChat tempMessage = new ModelChat(message, hisUid, myUid, timestamp, "text", false, null);

            // Add the temporary message to the chat list
            chatList.add(tempMessage);

            // Notify the adapter on the main thread
            runOnUiThread(() -> {
                adapterChat.notifyItemInserted(chatList.size() - 1);
                binding.chatRecyclerView.scrollToPosition(chatList.size() - 1);
            });

            // Clear the input field immediately
            binding.messageEt.setText("");

            // Send the message to Firebase in the background
            DatabaseReference chatRef = FirebaseDatabase.getInstance().getReference("Chats");

            HashMap<String, Object> messageMap = new HashMap<>();
            messageMap.put("sender", myUid);
            messageMap.put("receiver", hisUid);
            messageMap.put("message", message);
            messageMap.put("timestamp", timestamp);
            messageMap.put("isSeen", false);
            messageMap.put("type", "text");

            chatRef.push().setValue(messageMap).addOnCompleteListener(task -> {
                if (!task.isSuccessful()) {
                    // Remove the temporary message if sending fails
                    chatList.remove(tempMessage);
                    runOnUiThread(() -> adapterChat.notifyDataSetChanged());
                    Toast.makeText(this, "Message sending failed", Toast.LENGTH_SHORT).show();
                } else {
                    // Add both users to the "Chatlist" node
                    DatabaseReference chatListRef1 = FirebaseDatabase.getInstance().getReference("Chatlist")
                            .child(myUid)
                            .child(hisUid);
                    chatListRef1.child("id").setValue(hisUid);

                    DatabaseReference chatListRef2 = FirebaseDatabase.getInstance().getReference("Chatlist")
                            .child(hisUid)
                            .child(myUid);
                    chatListRef2.child("id").setValue(myUid);
                }
            });
        } else {
            Toast.makeText(this, "Cannot send an empty message", Toast.LENGTH_SHORT).show();
        }
    }



    private void loadMessages() {
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("Chats");
        Query chatQuery = dbRef.orderByChild("timestamp").limitToLast(20);

        messagesListener = chatQuery.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                chatList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    ModelChat chat = ds.getValue(ModelChat.class);
                    if (chat != null && (
                            (chat.getReceiver().equals(myUid) && chat.getSender().equals(hisUid)) ||
                                    (chat.getReceiver().equals(hisUid) && chat.getSender().equals(myUid)))) {
                        chatList.add(chat);
                    }
                }
                adapterChat.notifyDataSetChanged();
                binding.chatRecyclerView.scrollToPosition(chatList.size() - 1);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ChatDetailActivity.this, "Failed to load messages.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("Chats");
        dbRef.removeEventListener(messagesListener);
    }

    private void pickImageFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, GALLERY_REQUEST_CODE);
    }

    private void pickVideoFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("video/*");
        startActivityForResult(intent, VIDEO_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            if (requestCode == GALLERY_REQUEST_CODE) {
                Uri imageUri = data.getData();
                sendImageMessage(imageUri);
            } else if (requestCode == VIDEO_REQUEST_CODE) {
                Uri videoUri = data.getData();
                sendVideoMessage(videoUri);
            }
        }
    }


    private void sendImageMessage(Uri imageUri) {
        String timeStamp = String.valueOf(System.currentTimeMillis());
        String fileNameAndPath = "ChatImages/" + timeStamp;

        // Declare tempChat inside the method
        ModelChat tempChat = new ModelChat("loading", hisUid, myUid, timeStamp, "image", false, null);
        tempChat.setLocalImageUri(imageUri.toString());
        tempChat.setUploading(true);
        tempChat.setUploadProgress(0);

        // Add tempChat to the chatList and update the adapter
        chatList.add(tempChat);
        runOnUiThread(() -> {
            adapterChat.notifyItemInserted(chatList.size() - 1);
            binding.chatRecyclerView.scrollToPosition(chatList.size() - 1);
        });

        new Thread(() -> {
            try {
                Bitmap bitmap = decodeSampledBitmapFromUri(imageUri, 800, 800);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 60, baos);
                byte[] data = baos.toByteArray();

                // Firebase Storage reference
                StorageReference ref = FirebaseStorage.getInstance().getReference().child(fileNameAndPath);
                ref.putBytes(data)
                        .addOnProgressListener(taskSnapshot -> {
                            // Update progress
                            int progress = (int) (100 * taskSnapshot.getBytesTransferred() / taskSnapshot.getTotalByteCount());
                            tempChat.setUploadProgress(progress);
                            runOnUiThread(() -> adapterChat.notifyItemChanged(chatList.indexOf(tempChat)));
                        })
                        .addOnSuccessListener(taskSnapshot -> {
                            // Get download URL and save to database
                            taskSnapshot.getStorage().getDownloadUrl().addOnSuccessListener(uri -> {
                                tempChat.setMessage(uri.toString());
                                tempChat.setUploading(false);
                                tempChat.setUploadProgress(100);

                                runOnUiThread(() -> adapterChat.notifyItemChanged(chatList.indexOf(tempChat)));
                                saveMessageToDatabase(uri.toString(), timeStamp);
                            });
                        })
                        .addOnFailureListener(e -> {
                            // Remove tempChat on failure
                            chatList.remove(tempChat);
                            runOnUiThread(adapterChat::notifyDataSetChanged);
                        });

            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }







    private Bitmap decodeSampledBitmapFromUri(Uri imageUri, int reqWidth, int reqHeight) throws IOException {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        InputStream input = getContentResolver().openInputStream(imageUri);
        BitmapFactory.decodeStream(input, null, options);
        input.close();

        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        options.inJustDecodeBounds = false;

        input = getContentResolver().openInputStream(imageUri);
        Bitmap sampledBitmap = BitmapFactory.decodeStream(input, null, options);
        input.close();

        return sampledBitmap;
    }

    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        int height = options.outHeight;
        int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    private void saveMessageToDatabase(String downloadUri, String timeStamp) {
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("sender", myUid);
        hashMap.put("receiver", hisUid);
        hashMap.put("message", downloadUri);
        hashMap.put("timestamp", timeStamp);
        hashMap.put("type", "image");
        hashMap.put("isSeen", false);
        databaseReference.child("Chats").push().setValue(hashMap);
    }


    private void sendVideoMessage(Uri videoUri) {
        String timeStamp = String.valueOf(System.currentTimeMillis());
        String filePath = "ChatVideos/" + timeStamp + ".mp4";

        // Add a temporary message to the UI with local video URI and uploading state
        ModelChat tempChat = new ModelChat("loading", hisUid, myUid, timeStamp, "video", false, null);
        tempChat.setLocalImageUri(videoUri.toString()); // Set the local video URI
        tempChat.setUploading(true); // Set as uploading
        tempChat.setUploadProgress(0); // Initialize progress to 0

        chatList.add(tempChat);
        adapterChat.notifyItemInserted(chatList.size() - 1);
        binding.chatRecyclerView.scrollToPosition(chatList.size() - 1);

        // Firebase Storage reference
        StorageReference videoRef = FirebaseStorage.getInstance().getReference().child(filePath);
        UploadTask uploadTask = videoRef.putFile(videoUri);

        uploadTask.addOnProgressListener(taskSnapshot -> {
            long bytesTransferred = taskSnapshot.getBytesTransferred();
            long totalByteCount = taskSnapshot.getTotalByteCount();
            int progress = (int) (100 * bytesTransferred / totalByteCount);

            Log.d("UploadProgress", "Bytes transferred: " + bytesTransferred + "/" + totalByteCount + " (" + progress + "%)");

            // Update progress in the temporary message
            tempChat.setUploadProgress(progress);

            int position = chatList.indexOf(tempChat);
            if (position != -1) {
                adapterChat.notifyItemChanged(position);
            }
        }).addOnSuccessListener(taskSnapshot -> {
            videoRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                // Update message with final URL
                tempChat.setMessage(downloadUri.toString());
                tempChat.setUploading(false); // Mark as uploaded
                tempChat.setUploadProgress(100); // Final progress

                int position = chatList.indexOf(tempChat);
                if (position != -1) {
                    adapterChat.notifyItemChanged(position);
                }

                // Save to Firebase Realtime Database
                sendVideoMessageToDatabase(downloadUri.toString());
            });
        }).addOnFailureListener(e -> {
            // Remove the message on failure
            chatList.remove(tempChat);
            adapterChat.notifyDataSetChanged();
            Toast.makeText(this, "Video upload failed.", Toast.LENGTH_SHORT).show();
        });
    }


    private void sendVideoMessageToDatabase(String videoUrl) {
        String timeStamp = String.valueOf(System.currentTimeMillis());
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Chats");

        HashMap<String, Object> messageMap = new HashMap<>();
        messageMap.put("sender", myUid);
        messageMap.put("receiver", hisUid);
        messageMap.put("message", videoUrl);
        messageMap.put("timestamp", timeStamp);
        messageMap.put("type", "video");
        messageMap.put("isSeen", false);

        databaseReference.push().setValue(messageMap);
    }


    @Override
    protected void onStart() {
        super.onStart();
        myUid = firebaseAuth.getCurrentUser().getUid();
    }

}
