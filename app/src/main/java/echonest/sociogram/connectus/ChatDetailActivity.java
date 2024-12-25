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
    ActivityChatDetailBinding binding;
    FirebaseDatabase database;
    FirebaseAuth firebaseAuth;
    DatabaseReference usersDbRef;
    String hisUid,myUid;
    String hisImage;
    //for checking if user has seen message or not
    ValueEventListener seenListener;
    DatabaseReference userRefForSeen;
    AdapterChat adapterChat;
    List<ModelChat> chatList;


    private  static  final  int GALLERY_REQUEST_CODE = 400;
    private static final int VIDEO_REQUEST_CODE = 500;

    Uri image_rui=null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= 21) {
            Window window = this.getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            ((Window) window).setStatusBarColor(this.getResources().getColor(R.color.black));
        }
//        getSupportActionBar().hide();
        binding=ActivityChatDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
// layout for Recycler view
        LinearLayoutManager linearLayoutManager= new LinearLayoutManager(this);
        linearLayoutManager.setStackFromEnd(true);

        // Add TextWatcher to EditText
        binding.messageEt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // No action needed
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Show or hide the attachmentLayout based on input
                if (s.length() > 0) {
                    binding.attachmentLayout.setVisibility(View.GONE); // Hide when text is entered
                    binding.likebtn.setVisibility(View.GONE);  // Hide the like button
                    binding.sendbtn.setVisibility(View.VISIBLE);  // Show the send button

                } else {
                    binding.attachmentLayout.setVisibility(View.VISIBLE); // Show when EditText is empty
                    binding.likebtn.setVisibility(View.VISIBLE);  // Show the like button
                    binding.sendbtn.setVisibility(View.GONE);  // Hide the send button

                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                // No action needed
            }
        });





        //to ensure adapter chat is not null
        chatList = new ArrayList<>();
        adapterChat = new AdapterChat(ChatDetailActivity.this, chatList, hisImage);
        binding.chatRecyclerView.setAdapter(adapterChat);

        binding.chatRecyclerView.setHasFixedSize(true);
        binding.chatRecyclerView.setLayoutManager(linearLayoutManager);


        binding.getRoot().getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                Rect r = new Rect();
                binding.getRoot().getWindowVisibleDisplayFrame(r);
                int screenHeight = binding.getRoot().getRootView().getHeight();
                int keypadHeight = screenHeight - r.bottom;

                if (keypadHeight > screenHeight * 0.15) { // 0.15 ratio is perhaps enough to determine keypad height.
                    // Keyboard is opened
                    // Adjust your chat layout here
                    RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) binding.chatRecyclerView.getLayoutParams();
                    layoutParams.setMargins(layoutParams.leftMargin, layoutParams.topMargin, layoutParams.rightMargin, keypadHeight/30);
                    binding.chatRecyclerView.setLayoutParams(layoutParams);
                } else {
                    // Keyboard is closed
                    // Reset your chat layout here
                    RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) binding.chatRecyclerView.getLayoutParams();
                    layoutParams.setMargins(layoutParams.leftMargin, layoutParams.topMargin, layoutParams.rightMargin, 0);
                    binding.chatRecyclerView.setLayoutParams(layoutParams);
                }
            }
        });


        Intent intent=getIntent();
        hisUid= intent.getStringExtra("hisUid");
        firebaseAuth=FirebaseAuth.getInstance();
        database= FirebaseDatabase.getInstance();

        usersDbRef=database.getReference("Users");

        //search user to get that user;s info
        Query userQuery= usersDbRef.orderByChild("userId").equalTo(hisUid);
        //get user picture and name
        userQuery.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                //check untill required info is received
                for(DataSnapshot ds: snapshot.getChildren()){
                    //get data
                    String name=""+ds.child("name").getValue();
                    hisImage=""+ds.child("profilePhoto").getValue();
//get value of online status
                    String  onlineStatus=""+ds.child("onlineStatus").getValue();
                    if(onlineStatus.equals("online")){
                        binding.userStatusTv.setText(onlineStatus);


                    }else{
                        //convert timestamp to proper time date
                        try {
                            // Convert timestamp to dd//mm//yyyy hh:mm am/pm
                            Calendar cal = Calendar.getInstance(Locale.ENGLISH);
                            cal.setTimeInMillis(Long.parseLong(onlineStatus));

                            SimpleDateFormat sdf = new SimpleDateFormat("dd//MM//yyyy hh:mm aa", Locale.ENGLISH);
                            String dateTime = sdf.format(cal.getTime());

                            // Set data
                            binding.userStatusTv.setText("Last seen: "+dateTime);

                        } catch (NumberFormatException e) {
                            Log.e("TimeStamp Error", "Invalid timestamp format", e);
                        }
                    }
                    //set data
                    binding.nameTv.setText(name);

                    if (!isFinishing()) {
                        try {
                            Glide.with(ChatDetailActivity.this)
                                    .load(hisImage)
                                    .placeholder(R.drawable.avatar)
                                    .into(binding.profileIv);
                        } catch (Exception e) {
                            Glide.with(ChatDetailActivity.this)
                                    .load(R.drawable.avatar)
                                    .into(binding.profileIv);
                        }
                    }}
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });









        binding.sendbtn.setOnClickListener(view -> {

            // Get the input text and trim it to remove trailing spaces/newlines
            String message = binding.messageEt.getText().toString().trim();

            // Check if the message is not empty after trimming
            if (!TextUtils.isEmpty(message)) {
                sendMessage(message);
            } else {
                // Optionally show a message if the input is empty
                Toast.makeText(ChatDetailActivity.this, "Cannot send an empty message", Toast.LENGTH_SHORT).show();
            }
        });


        binding.attachBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pickImageFromeGallery();
            }
        });

        binding.attachBtnVideo.setOnClickListener(view -> {
            pickVideoFromGallery();

        });
        binding.backArrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent=new Intent(ChatDetailActivity.this,MainActivity.class);
                startActivity(intent);
            }
        });

        readMessages();
//        seenMessage();

    }

    private void pickVideoFromGallery() {
        Intent videoIntent = new Intent(Intent.ACTION_PICK);
        videoIntent.setType("video/*");
        startActivityForResult(videoIntent, VIDEO_REQUEST_CODE);
    }

    private void pickImageFromeGallery() {
        Intent igallery= new Intent(Intent.ACTION_PICK);
//        igallery.setData(MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        igallery.setType("image/*");
        startActivityForResult(igallery,GALLERY_REQUEST_CODE);

    }




    private void readMessages() {
        chatList = new ArrayList<>();
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("Chats");
        dbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                chatList.clear();
                for(DataSnapshot ds: snapshot.getChildren()){
                    ModelChat chat=ds.getValue(ModelChat.class);
                    if(chat.getReceiver().equals(myUid) && chat.getSender().equals(hisUid) ||
                            chat.getReceiver().equals(hisUid) && chat.getSender().equals(myUid)){
                        chatList.add(chat);
                    }
                    adapterChat=new AdapterChat(ChatDetailActivity.this,chatList,hisImage);
                    //set adapter to recyclerview
                    binding.chatRecyclerView.setAdapter(adapterChat);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }





    private void sendMessage(String message) {
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
        String timestamp = String.valueOf(System.currentTimeMillis());

        HashMap<String, Object> messageMap = new HashMap<>();
        messageMap.put("sender", myUid);
        messageMap.put("receiver", hisUid);
        messageMap.put("message", message);
        messageMap.put("timestamp", timestamp);
        messageMap.put("isSeen", false);
        messageMap.put("type", "text");

        HashMap<String, Object> updates = new HashMap<>();
        String messageKey = databaseReference.child("Chats").push().getKey();
        updates.put("Chats/" + messageKey, messageMap);
        updates.put("Chatlist/" + myUid + "/" + hisUid + "/id", hisUid);
        updates.put("Chatlist/" + hisUid + "/" + myUid + "/id", myUid);

        databaseReference.updateChildren(updates);
        binding.messageEt.setText(""); // Reset input

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





    private  void checkUserStatus(){
        FirebaseUser user= firebaseAuth.getCurrentUser();
        if(user!=null){
//user is signed in
            //set email of logged in user
            //mprofileTv.setText(user.getEmail());
            myUid=user.getUid();
        }else{
            startActivity(new Intent(this, SignInActivity.class));
            finish();
        }
    }

    private void  checkOnlineStatus(String status){
        DatabaseReference dbRef= FirebaseDatabase.getInstance().getReference("Users").child(myUid);
        HashMap<String,Object> hashMap= new HashMap<>();
        hashMap.put("onlineStatus",status);
        dbRef.updateChildren(hashMap);
    }

    @Override
    protected void onStart() {
        checkUserStatus();
        //set online
        checkOnlineStatus("online");
        super.onStart();
    }

    @Override
    protected void onPause() {
        super.onPause();
        //get timestamp
        String timestamp= String.valueOf(System.currentTimeMillis());
        //set offiline with last seen time stamp
        checkOnlineStatus(timestamp);
//        userRefForSeen.removeEventListener(seenListener);
    }

    @Override
    protected void onResume() {
        //set online
        checkOnlineStatus("online");
        super.onResume();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if(resultCode==RESULT_OK){
            if(requestCode==GALLERY_REQUEST_CODE){
                image_rui=data.getData();

                    sendImageMessage(image_rui);

            } else if (requestCode == VIDEO_REQUEST_CODE) {
                Uri videoUri = data.getData();
                try {
                    sendVideoMessage(videoUri);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu,menu);
        //hide searchview
        menu.findItem(R.id.action_search).setVisible(false);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        int id =item.getItemId();
//        if(id==R.id.logout) {
//            firebaseAuth.signOut();
//            checkUserStatus();
//        }
        return super.onOptionsItemSelected(item);
    }
}