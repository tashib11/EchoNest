package echonest.sociogram.connectus;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
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