package echonest.sociogram.connectus;

import static com.google.firebase.database.FirebaseDatabase.getInstance;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.connectus.R;
import com.example.connectus.databinding.ActivitySettingsBinding;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
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

import java.util.HashMap;

public class SettingsActivity extends AppCompatActivity {
    ActivitySettingsBinding binding;
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


    // Class-level declaration for ValueEventListener
    private ValueEventListener valueEventListener;

ProgressDialog progressDialog;
    protected void onCreate(Bundle savedInstanceState) {

        // Load dark mode preference
        SharedPreferences sharedPreferences = getSharedPreferences("AppSettings", MODE_PRIVATE);
        boolean isDarkMode = sharedPreferences.getBoolean("DarkMode", true);
        // Apply the appropriate theme
        if (isDarkMode) {
            setTheme(R.style.DarkTheme); // Make sure DarkTheme is defined in your styles.xml
        } else {
            setTheme(R.style.LightTheme); // Make sure LightTheme is defined in your styles.xml
        }

        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= 21) {
            Window window = this.getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            ((Window) window).setStatusBarColor(this.getResources().getColor(R.color.black));
        }

        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

//       FirebaseDatabase storage = FirebaseStorage.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseDatabase= getInstance();
         user=firebaseAuth.getCurrentUser();
        databaseReference=firebaseDatabase.getReference("Users");
         binding.emailTv.setText(user.getEmail());
        progressDialog=new ProgressDialog(SettingsActivity.this);
      storage = FirebaseStorage.getInstance();
storageReference = storage.getReference();//firebase storage reference
        //init arrays of permissions

        // Initialize UI elements based on dark mode
        binding.mainLayout.setBackgroundColor(isDarkMode
                ? ContextCompat.getColor(this, R.color.blacklight)
                : ContextCompat.getColor(this, R.color.white));

        binding.darkModeSwitch.setChecked(isDarkMode);
        binding.darkModeStatus.setText(isDarkMode ? "Enabled" : "Disabled");

        // Set listener for the dark mode switch
        binding.darkModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Update the shared preference
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("DarkMode", isChecked);
            editor.apply();

            // Restart the activity to apply the new theme
            recreate();
        });


        valueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String name = "" + ds.child("name").getValue();
                    String email = "" + ds.child("email").getValue();

                    String image = "" + ds.child("profilePhoto").getValue();
                    String cover = "" + ds.child("coverPhoto").getValue();
                    binding.nameTv.setText(name);
                    binding.emailTv.setText(email);

                    imageUrl = image;

                    if (!isFinishing() && !isDestroyed()) {
                        try {
                            Glide.with(SettingsActivity.this)
                                    .load(image)
                                    .placeholder(R.drawable.avatar)
                                    .into(binding.avatarIv);
                        } catch (Exception e) {
                            Glide.with(SettingsActivity.this)
                                    .load(R.drawable.avatar)
                                    .into(binding.avatarIv);
                        }

                        try {
                            Glide.with(SettingsActivity.this)
                                    .load(cover)
                                    .into(binding.coverIv);
                        } catch (Exception e) {
                            // Handle exception
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle errors
            }
        };

        Query query = databaseReference.orderByChild("email").equalTo(user.getEmail());
        query.addValueEventListener(valueEventListener);

        binding.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showEditProfileDialog();
            }
        });

binding.aboutTxt.setOnClickListener(view -> {
    Intent intent= new Intent(SettingsActivity.this,aboutActivity.class);
    startActivity(intent);
});


    }


    private void showEditProfileDialog() {
        String[] options ={"Edit Profile Picture", "Edit Cover Photo ","Edit Name", "Change password"};

        AlertDialog.Builder builder= new AlertDialog.Builder(this);
        builder.setTitle("Choose Action");
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if(i==0){
                    progressDialog.setMessage("Updating Profile Picture");
                    profileOrCover="profilePhoto";//changinf profile picture ,make sure to assign same value
                    pickImageFromeGallery();
                }else  if(i==1){
                    progressDialog.setMessage("Updating Cover Picture");
                    profileOrCover="coverPhoto";
                    pickImageFromeGallery();

                }else  if(i==2){
                    progressDialog.setMessage("Updating Name");
                    showNamePhoneUpdateDialog("name");

                }else  if(i==3){
                    progressDialog.setMessage("Updating Password");
                    showChangePasswordDialog();
                }
            }
        });
        builder.create().show();
    }

    private void showChangePasswordDialog() {
View view = LayoutInflater.from(this).inflate(R.layout.dialog_update_password, null);
EditText passwordEt= view.findViewById(R.id.etPassword);
EditText newPasswordEt= view.findViewById(R.id.newPasswordet);
Button updatePasswordBtn = view.findViewById(R.id.updatePasswordBtn);
       final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(view);

        AlertDialog dialog = builder.create();
        dialog.show();
        updatePasswordBtn.setOnClickListener(view1 -> {
            String oldPasword= passwordEt.getText().toString();
            String newPasword= newPasswordEt.getText().toString();
            if(TextUtils.isEmpty(oldPasword)){
                Toast.makeText(this, "Enter your current Password", Toast.LENGTH_LONG).show();
                return;
            }
            if(newPasword.length()<6){
                Toast.makeText(this, "Password length must be atleast 6 characters", Toast.LENGTH_SHORT).show();
                return;
            }
            dialog.dismiss();
            updatePassword(oldPasword,newPasword);
        });

    }

    private void updatePassword(String oldPassword, String newPassword) {
        progressDialog.show();
        FirebaseUser user = firebaseAuth.getCurrentUser();

        if (user == null || user.getEmail() == null) {
            progressDialog.dismiss();
            Toast.makeText(SettingsActivity.this, "User not authenticated. Please log in again.", Toast.LENGTH_SHORT).show();
            return;
        }

        AuthCredential authCredential = EmailAuthProvider.getCredential(user.getEmail(), oldPassword);
        user.reauthenticate(authCredential).addOnSuccessListener(unused -> {
            // Successfully authenticated, begin update
            user.updatePassword(newPassword).addOnSuccessListener(unused1 -> {
                // Password updated in Firebase Authentication
                DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users").child(user.getUid());
                userRef.child("password").setValue(newPassword).addOnCompleteListener(task -> {
                    progressDialog.dismiss();
                    if (task.isSuccessful()) {
                        Toast.makeText(SettingsActivity.this, "Password updated successfully.", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(SettingsActivity.this, "Password update failed in database.", Toast.LENGTH_SHORT).show();
                    }
                });
            }).addOnFailureListener(e -> {
                progressDialog.dismiss();
                Toast.makeText(SettingsActivity.this, "Failed to update password: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
        }).addOnFailureListener(e -> {
            progressDialog.dismiss();
            Toast.makeText(SettingsActivity.this, "Wrong current password: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }


    private void showNamePhoneUpdateDialog(String key) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Update "+key);
        //set layout of dialog

        LinearLayout linearLayout= new LinearLayout(this);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setPadding(10,10,10,10);
        // add editext
        EditText editText= new EditText(this);
        editText.setHint("Enter "+key);
        linearLayout.addView(editText);
        builder.setView(linearLayout);

        builder.setPositiveButton("Update", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                String value= editText.getText().toString().trim();


                if(!TextUtils.isEmpty(value)){
                    progressDialog.show();
                    HashMap<String,Object> result= new HashMap<>();
                    result.put(key,value);

                    databaseReference.child(user.getUid()).updateChildren(result)
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void unused) {
                                    progressDialog.dismiss();
                                    Toast.makeText(SettingsActivity.this, "Updated", Toast.LENGTH_SHORT).show();
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    progressDialog.dismiss();
                                    Toast.makeText(SettingsActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });
                }else{
                    Toast.makeText(SettingsActivity.this, "Please Enter "+key, Toast.LENGTH_SHORT).show();

                }
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
            progressDialog.dismiss();
            }
        });
        builder.create().show();
    }

    private void pickImageFromeGallery() {
        Intent igallery= new Intent(Intent.ACTION_PICK);
//        igallery.setData(MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        igallery.setType("image/*");
        startActivityForResult(igallery,GALLERY_REQUEST_CODE);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if(resultCode==RESULT_OK){
            if(requestCode==GALLERY_REQUEST_CODE){
                image_uri=data.getData();
                uploadProfileCoverPhoto(image_uri);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);

    }


    private void uploadProfileCoverPhoto(final Uri uri) {
        progressDialog.show();
        String filePathAndName = storagePath + ""+ profileOrCover +" "+user.getUid();
        StorageReference storageReference2nd=storageReference.child(filePathAndName);
        storageReference2nd.putFile(uri)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        //image is uploaded to storage , now get it's url and store in user's database
                        Task<Uri> uriTask= taskSnapshot.getStorage().getDownloadUrl();
                        while(!uriTask.isSuccessful());
                        Uri downloadUri= uriTask.getResult();
// check if image is uploaded or not amd url is received
                        if(uriTask.isSuccessful()){
                            //image uploaded
                            // add update url in users database
                            HashMap<String,Object>results= new HashMap<>();
                            results.put(profileOrCover,downloadUri.toString());
                            databaseReference.child(user.getUid()).updateChildren(results).addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void unused) {
                                    //url in database of user is added successfully
                                    progressDialog.dismiss();
                                    Toast.makeText(SettingsActivity.this, "Image Updated", Toast.LENGTH_SHORT).show();
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    progressDialog.dismiss();
                                    Toast.makeText(SettingsActivity.this, "Image Updating failed", Toast.LENGTH_SHORT).show();

                                }
                            });
                        }else{
                            progressDialog.dismiss();
                            Toast.makeText(SettingsActivity.this, "Error occured", Toast.LENGTH_SHORT).show();
                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        progressDialog.dismiss();
                        Toast.makeText(SettingsActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();

                    }
                });

    }
    @Override
    public void onBackPressed() {
        if (imageUrl == null || imageUrl.isEmpty()) {
            Toast.makeText(SettingsActivity.this, "Please, set your profile pic", Toast.LENGTH_LONG).show();
        } else {
            Intent intent = new Intent(SettingsActivity.this, MainActivity.class);
            startActivity(intent);
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (databaseReference != null && valueEventListener != null) {
            databaseReference.removeEventListener(valueEventListener);
        }
    }


}