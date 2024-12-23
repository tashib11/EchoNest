package echonest.sociogram.connectus;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.RelativeLayout;

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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
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

            String message= binding.messageEt.getText().toString();

            if(TextUtils.isEmpty(message)){

            }else{
                sendMessage(message);
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
        DatabaseReference databaseReference=FirebaseDatabase.getInstance().getReference();

        String timestamp= String.valueOf(System.currentTimeMillis());
        HashMap<String,Object> hashMap= new HashMap<>();
        hashMap.put("sender", myUid);
        hashMap.put("receiver", hisUid);
        hashMap.put("message", message);
        hashMap.put("timestamp", timestamp);
        hashMap.put("isSeen", false);
        hashMap.put("type","text");

        databaseReference.child("Chats").push().setValue(hashMap);
        //reset edittext after sendiung message
        binding.messageEt.setText("");

        // create chatlist node/child in firebase
        DatabaseReference chatRef1= FirebaseDatabase.getInstance().getReference("Chatlist").child(myUid).child(hisUid);
        chatRef1.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(!snapshot.exists()){
                    chatRef1.child("id").setValue(hisUid);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        DatabaseReference chatRef2= FirebaseDatabase.getInstance().getReference("Chatlist").child(hisUid).child(myUid);

        chatRef2.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(!snapshot.exists()){
                    chatRef2.child("id").setValue(myUid);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        // scroll to the bottom of the RecyclerView
        binding.chatRecyclerView.scrollToPosition(chatList.size() - 1);
    }
    private void sendImageMessage(Uri imageUri) throws IOException {
        String timeStamp = String.valueOf(System.currentTimeMillis());
        String fileNameAndPath = "ChatImages/" + timeStamp;

        // Create Bitmap from URI and compress it
        Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
        byte[] data = baos.toByteArray();

        // Add a temporary message to the UI with local image URI and uploading state
        ModelChat tempChat = new ModelChat("loading", hisUid, myUid, timeStamp, "image", false);
        tempChat.setLocalImageUri(imageUri.toString()); // Store the local image URI
        tempChat.setUploading(true); // Set the message to uploading
        tempChat.setUploadProgress(0); // Initialize progress to 0

        chatList.add(tempChat);
        adapterChat.notifyItemInserted(chatList.size() - 1);
        binding.chatRecyclerView.scrollToPosition(chatList.size() - 1);

        // Firebase Storage reference
        StorageReference ref = FirebaseStorage.getInstance().getReference().child(fileNameAndPath);
        ref.putBytes(data)
                .addOnProgressListener(taskSnapshot -> {
                    long bytesTransferred = taskSnapshot.getBytesTransferred();
                    long totalByteCount = taskSnapshot.getTotalByteCount();
                    int progress = (int) (100 * bytesTransferred / totalByteCount);

                    Log.d("UploadProgress", "Bytes transferred: " + bytesTransferred + "/" + totalByteCount + " (" + progress + "%)");

                    // Update the progress in the message
                    tempChat.setUploadProgress(progress);

                    // Notify the adapter to refresh the UI
                    int position = chatList.indexOf(tempChat);
                    if (position != -1) {
                        adapterChat.notifyItemChanged(position);
                    }
                })
                .addOnSuccessListener(taskSnapshot -> {
                    Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                    while (!uriTask.isSuccessful());

                    String downloadUri = uriTask.getResult().toString();
                    if (uriTask.isSuccessful()) {
                        // Replace temp message with uploaded URL
                        tempChat.setMessage(downloadUri);
                        tempChat.setUploading(false); // Mark as uploaded
                        tempChat.setUploadProgress(100); // Final progress

                        int position = chatList.indexOf(tempChat);
                        if (position != -1) {
                            adapterChat.notifyItemChanged(position);
                        }

                        // Save to Firebase Realtime Database
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
                })
                .addOnFailureListener(e -> {
                    // Handle upload failure
                    chatList.remove(tempChat);
                    adapterChat.notifyDataSetChanged();
                });


    }




    private void sendVideoMessage(Uri videoUri) {

        String timeStamp = String.valueOf(System.currentTimeMillis());
        String fileNameAndPath = "ChatVideos" + timeStamp;

        StorageReference ref = FirebaseStorage.getInstance().getReference().child(fileNameAndPath);
        ref.putFile(videoUri).addOnSuccessListener(taskSnapshot -> {

            Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
            while (!uriTask.isSuccessful()) ;
            String downloadUri = uriTask.getResult().toString();

            if (uriTask.isSuccessful()) {
                // Video uploaded successfully
                DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();

                HashMap<String, Object> hashMap = new HashMap<>();
                hashMap.put("sender", myUid);
                hashMap.put("receiver", hisUid);
                hashMap.put("message", downloadUri);
                hashMap.put("timestamp", timeStamp);
                hashMap.put("type", "video");
                hashMap.put("isSeen", false);
                databaseReference.child("Chats").push().setValue(hashMap);

                // Create the chatlist node/child in Firebase
                DatabaseReference chatRef1 = FirebaseDatabase.getInstance().getReference("Chatlist").child(myUid).child(hisUid);
                chatRef1.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) {
                            chatRef1.child("id").setValue(hisUid);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });

                DatabaseReference chatRef2 = FirebaseDatabase.getInstance().getReference("Chatlist").child(hisUid).child(myUid);
                chatRef2.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) {
                            chatRef2.child("id").setValue(myUid);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
            }
        }).addOnFailureListener(e -> {

            // Handle the failure to upload the video here
        });
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
                try {
                    sendImageMessage(image_rui);
                } catch (IOException e) {
                    e.printStackTrace();
                }
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