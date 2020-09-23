package com.canibal.firebaseapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.canibal.firebaseapp.notifications.Token;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;

public class DashboardActivity extends AppCompatActivity {

    //firebase auth
    FirebaseAuth firebaseAuth;

    ActionBar actionBar;

    String mUID;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        //Actionbar and its title
        //Habilitar barra y titulo
        actionBar= getSupportActionBar();

        //init  firebaseAuth -- inicializar  firebaseAuth
        firebaseAuth = FirebaseAuth.getInstance();

        //botton navigation -- navegación inferior
        BottomNavigationView navigationView = findViewById(R.id.navigation);
        navigationView.setOnNavigationItemSelectedListener(selectedListener);

        //Home fratment transaction (default) -- transacción en casa (por defecto al iniciar)
        actionBar.setTitle("Home"); // change actionbar title -- cambia el título de la barra de acción
        HomeFragment fragment1 = new HomeFragment();
        FragmentTransaction ft1 = getSupportFragmentManager().beginTransaction();
        ft1.replace(R.id.content, fragment1, "");
        ft1.commit();

        checkUserStatus();

    }

    @Override
    protected void onResume() {
        checkUserStatus();
        super.onResume();
    }

    public void updateToken(String token){
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Tokens");
        Token mToken = new Token(token);
        ref.child(mUID).setValue(mToken);
    }

    private BottomNavigationView.OnNavigationItemSelectedListener selectedListener =
            new BottomNavigationView.OnNavigationItemSelectedListener() {
                @Override
                public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                    //handle item clicks -- manejar clics de elementos
                    switch (menuItem.getItemId()) {
                        case R.id.nav_home:
                            //Home fratment transaction -- transacción en casa
                            actionBar.setTitle("Home"); // change actionbar title -- cambia el título de la barra de acción
                            HomeFragment fragment1 = new HomeFragment();
                            FragmentTransaction ft1 = getSupportFragmentManager().beginTransaction();
                            ft1.replace(R.id.content, fragment1, "");
                            ft1.commit();

                            return true;
                        case R.id.nav_profile:
                            //profile fratment transaction -- transacción en perfil
                            actionBar.setTitle("Profile"); // change actionbar title -- cambia el título de la barra de acción
                            ProfileFragment fragment2 = new ProfileFragment();
                            FragmentTransaction ft2 = getSupportFragmentManager().beginTransaction();
                            ft2.replace(R.id.content, fragment2, "");
                            ft2.commit();

                            return true;
                        case R.id.nav_users:
                            //users fratment transaction -- transacción en users

                            actionBar.setTitle("Users"); // change actionbar title -- cambia el título de la barra de acción
                            UsersFragment fragment3 = new UsersFragment();
                            FragmentTransaction ft3 = getSupportFragmentManager().beginTransaction();
                            ft3.replace(R.id.content, fragment3, "");
                            ft3.commit();

                            return true;
                        case R.id.nav_chat:
                            //users fratment transaction -- transacción en users

                            actionBar.setTitle("Chat"); // change actionbar title -- cambia el título de la barra de acción
                            ChatListFragment fragment4 = new ChatListFragment();
                            FragmentTransaction ft4 = getSupportFragmentManager().beginTransaction();
                            ft4.replace(R.id.content, fragment4, "");
                            ft4.commit();
                            return true;

                        case R.id.nav_notification:
                            //users fratment transaction -- transacción en users

                            actionBar.setTitle("Notifications"); // change actionbar title -- cambia el título de la barra de acción
                            NotificationsFragment fragment5 = new NotificationsFragment();
                            FragmentTransaction ft5 = getSupportFragmentManager().beginTransaction();
                            ft5.replace(R.id.content, fragment5, "");
                            ft5.commit();
                            return true;
                    }

                    return false;
                }
            };

    private void checkUserStatus(){
        //get current user -- obtener usuario actual
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if(user != null){
            //user is signed in stay here -- El usuario ha iniciado sesión aquí.
            // set email of logged in user -- configurar el correo electrónico del usuario conectado
            //mProfileTv.setText(user.getEmail());
            mUID = user.getUid();

            //save uid of currently signed in user in shared preferences
            // guardar el uid del usuario actualmente conectado en preferencias compartidas
            SharedPreferences sp = getSharedPreferences("SP_USER", MODE_PRIVATE);
            SharedPreferences.Editor editor = sp.edit();
            editor.putString("Current_USERID", mUID);
            editor.apply();

            //update token -- token de actualización
            updateToken(FirebaseInstanceId.getInstance().getToken());

        }else {
            //user not signed in, go to main activity -- usuario no ha iniciado sesión, vaya a la actividad principal
            startActivity(new Intent(DashboardActivity.this,  MainActivity.class));
            finish();
        }
    }

    ///////////////////3ra parte///////////////////////////
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    @Override
    protected void onStart() {
        //check on start app --comprobar en la aplicación de inicio
        checkUserStatus();
        super.onStart();
    }

}



