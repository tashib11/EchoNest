package echonest.sociogram.connectus;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import echonest.sociogram.connectus.Adapters.FragmentsAdapter;
import com.example.connectus.R;
import com.example.connectus.databinding.ActivityMainBinding;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;


public class MainActivity extends AppCompatActivity {
    DrawerLayout drawerLayout;
    NavigationView navigationView;
    BottomNavigationView bottomNavigationView;
    Toolbar toolbar;
    ActivityMainBinding binding;
    FirebaseAuth auth;
    GoogleSignInClient mGoogleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // for  status bar color
        Window window = this.getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        ((Window) window).setStatusBarColor(this.getResources().getColor(R.color.black));

//        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.black)));
        binding=ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        auth=FirebaseAuth.getInstance();

        drawerLayout=findViewById(R.id.drawerLayout);
        navigationView=findViewById((R.id.nav_drawer));
        bottomNavigationView=findViewById(R.id.bottom_navigation);
        toolbar=findViewById((R.id.toolbar));
        setSupportActionBar(toolbar);
        ActionBarDrawerToggle toggle =new ActionBarDrawerToggle(this,drawerLayout,toolbar,R.string.OpenDrawer,R.string.CloseDrawer);
                //activity ,drawer,toolbar and 2flex-open-close reference push kora lagbe in ActionBarDrawerToggle()
        //flex ar value int dite hobe but int ta string hishebe thakbe .so go vlues->string a jao
        drawerLayout.addDrawerListener(toggle);//for sliding , mane shob vabei drawer use kora jabe
        toggle.syncState();// open hole bolbe ji open vai , close hole state ta hobe close

         boolean isDarkMode=true;
        MenuItem darkModeMenuItem = navigationView.getMenu().findItem(R.id.darkModeSwitch);
        if (darkModeMenuItem != null) {
            View actionView = darkModeMenuItem.getActionView();
            if (actionView != null) {
                SwitchCompat darkModeSwitch = actionView.findViewById(R.id.nav_dark_mode_switch);
                if (darkModeSwitch != null) {
                    // Load dark mode preference
                    SharedPreferences sharedPreferences = getSharedPreferences("AppSettings", MODE_PRIVATE);
                     isDarkMode = sharedPreferences.getBoolean("DarkMode", true);
                    darkModeSwitch.setChecked(isDarkMode);

                    // Listen for switch changes
                    darkModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putBoolean("DarkMode", isChecked);
                        editor.apply();

                        AppCompatDelegate.setDefaultNightMode(isChecked
                                ? AppCompatDelegate.MODE_NIGHT_YES
                                : AppCompatDelegate.MODE_NIGHT_NO);
                        // Update NavigationView background color
                        int navBackgroundColor = ContextCompat.getColor(this,
                                isChecked ? R.color.colorPrimary : R.color.light_background);
                        navigationView.setBackgroundColor(navBackgroundColor);
                    });


                } else {
                    Log.e("MainActivity", "SwitchCompat not found in actionView");
                }
            } else {
                Log.e("MainActivity", "actionView not found for MenuItem");
            }
        } else {
            Log.e("MainActivity", "MenuItem darkModeSwitch not found");
        }


        if (isDarkMode) {
 binding.navDrawer.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimary));

        }else {
            binding.bottomAppBar.setBackgroundTint (ColorStateList.valueOf(ContextCompat.getColor(this, R.color.appbar)));
//            binding.chatLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.white));
            binding.bottomNavigation.setBackgroundColor(ContextCompat.getColor(this, R.color.appbar));
            binding.bottomNavigation.setItemTextColor(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.black)));

           binding.navDrawer.setBackgroundColor(ContextCompat.getColor(this, R.color.light_background));
            binding.navDrawer.setItemTextColor(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.black)));
        }


        getSupportActionBar().setTitle("Chats");

        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id= item.getItemId();
                if(id==R.id.logout){
             auth.signOut();
            Intent intent=new Intent(MainActivity.this,SignInActivity.class);
                startActivity(intent);
                }
                else if(id==R.id.settings){
                    Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                    startActivity(intent);
                }
                else if(id==R.id.deleteAccount){
                    Intent intent = new Intent(MainActivity.this, deleteAccountActivity.class);
                    startActivity(intent);
                }       else if(id==R.id.news){
                    Intent intent = new Intent(MainActivity.this, newsActivity.class);
                    startActivity(intent);
                }


                return true;
            }
        });
        binding.viewPager.setAdapter(new FragmentsAdapter(getSupportFragmentManager()));

        bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {

            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {

                int id= item.getItemId();
                if(id==R.id.bchat){
                    binding.viewPager.setCurrentItem(0);

                    getSupportActionBar().setTitle("Chats");

                    return  true;
                }
          else   if(id==R.id.bpeople){
                    binding.viewPager.setCurrentItem(1);
//                    getActionBar().setTitle("Chats");
                    getSupportActionBar().setTitle("People");

                    return  true;
                }
                return false;
            }
        });

    }




    public void onBackPressed() {
      if(drawerLayout.isDrawerOpen(GravityCompat.START)){
          drawerLayout.closeDrawer(GravityCompat.START);
          // if u pressed back button then drawer will be off
      }else {
//          super.onBackPressed();// if already drwer is off then by pressing backbutton ,app exit
          finishAffinity();
          finish();
      }

  }

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        getMenuInflater().inflate(R.menu.menu, menu);
//        return super.onCreateOptionsMenu(menu);
//    }


}