package echonest.sociogram.connectus;

import static com.google.firebase.database.FirebaseDatabase.getInstance;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.connectus.R;
import com.example.connectus.databinding.ActivityInboxDetailBinding;
import com.example.connectus.databinding.ActivitySettingsBinding;
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

public class inboxDetailActivity extends AppCompatActivity {
ActivityInboxDetailBinding binding;
    FirebaseAuth firebaseAuth;
    FirebaseUser user;
    FirebaseStorage storage;
    FirebaseDatabase firebaseDatabase;
    DatabaseReference databaseReference;
    StorageReference storageReference;
    //path where images of user profile and cover will be stored
    String storagePath="Users_Profile_Cover_Imgs/";
    Uri image_uri;
    String profileOrCover;
    private String imageUrl;
    //    permission constants
//    private  static  final  int CAMERA_REQUEST_CODE = 100;
//    private  static  final  int STORAGE_REQUEST_CODE = 200;
    private  static  final  int GALLERY_REQUEST_CODE = 300;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // to set up darkmode features
        SharedPreferences sharedPreferences = getSharedPreferences("AppSettings", MODE_PRIVATE);
        boolean isDarkMode = sharedPreferences.getBoolean("DarkMode", true);

        updateTheme(isDarkMode);

        // Listen for preference changes to apply theme dynamically
        sharedPreferences.registerOnSharedPreferenceChangeListener((sharedPrefs, key) -> {
            if ("DarkMode".equals(key)) {
                boolean newDarkModeState = sharedPrefs.getBoolean("DarkMode", true);
                updateTheme(newDarkModeState);
            }
        });




        // Set up layout
        binding = ActivityInboxDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.mainLayout.setBackgroundColor(isDarkMode
                ? ContextCompat.getColor(this, R.color.blacklight)
                : ContextCompat.getColor(this, R.color.white));

        // Initialize Firebase and other components
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance();
        user = firebaseAuth.getCurrentUser();
        databaseReference = firebaseDatabase.getReference("Users");
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();




        // Get hisUid from the Intent
        Intent intent = getIntent();
        String hisUid = intent.getStringExtra("hisUid");

        if (hisUid != null) {
            // Fetch data from Firebase for the provided hisUid
            DatabaseReference userRef = databaseReference.child(hisUid);
            userRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        // Fetch user data
                        String name = "" + snapshot.child("name").getValue();
                        String email = "" + snapshot.child("email").getValue();
                        String profession = "" + snapshot.child("profession").getValue();
                        String image = "" + snapshot.child("profilePhoto").getValue();
                        String cover = "" + snapshot.child("coverPhoto").getValue();

                        // Bind data to views
                        binding.nameTv.setText(name);
                        binding.emailTv.setText(email);
                        binding.professionTv.setText(profession);

                        // Load profile photo
                        try {
                            Glide.with(inboxDetailActivity.this)
                                    .load(image)
                                    .placeholder(R.drawable.avatar)
                                    .into(binding.avatarIv);
                        } catch (Exception e) {
                            Glide.with(inboxDetailActivity.this)
                                    .load(R.drawable.avatar)
                                    .into(binding.avatarIv);
                        }

                        // Load cover photo
                        try {
                            Glide.with(inboxDetailActivity.this)
                                    .load(cover)
                                    .into(binding.coverIv);
                        } catch (Exception e) {
                            Glide.with(inboxDetailActivity.this)
                                    .load(R.drawable.avatar)
                                    .into(binding.coverIv);
                        }
                    } else {
                        Toast.makeText(inboxDetailActivity.this, "User not found.", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(inboxDetailActivity.this, "Failed to fetch data: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Toast.makeText(this, "No user ID passed.", Toast.LENGTH_SHORT).show();
        }



    }

    private void updateTheme(boolean isDarkMode) {

    }

    @Override
    public void onBackPressed() {
       finish();
    }


}