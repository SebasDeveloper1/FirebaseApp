package com.canibal.firebaseapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.MenuItemCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.canibal.firebaseapp.adapters.AdapterPosts;
import com.canibal.firebaseapp.models.ModelPost;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class ThereProfileActivity extends AppCompatActivity {

    FirebaseAuth firebaseAuth;

    // views from xml -- vistas de xml
    ImageView avatarIv,coverIv;
    TextView nameTv, emailTv, phoneTv;
    RecyclerView postsRecyclerView;

    List<ModelPost> postList;
    AdapterPosts adapterPosts;
    String uid;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_there_profile);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle("Profile");
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        //init views -- inicializar las vistas
        avatarIv = findViewById(R.id.avatarIv);
        coverIv = findViewById(R.id.coverIv);
        nameTv = findViewById(R.id.nameTv);
        emailTv = findViewById(R.id.emailTv);
        phoneTv = findViewById(R.id.phoneTv);
        postsRecyclerView = findViewById(R.id.recyclerview_posts);

        firebaseAuth = FirebaseAuth.getInstance();

        //get  uid clicket user retrieve his posts
        Intent intent = getIntent();
        uid = intent.getStringExtra("uid");


        Query query= FirebaseDatabase.getInstance().getReference("Users").orderByChild("uid").equalTo(uid);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                //checkc until required data get -- checkc hasta que se obtengan los datos requeridos
                for(DataSnapshot ds: dataSnapshot.getChildren()) {
                    //get data -- obtener datos
                    String name = ""+ ds.child("name").getValue();
                    String email = ""+ ds.child("email").getValue();
                    String phone = ""+ ds.child("phone").getValue();
                    String image = ""+ ds.child("image").getValue();
                    String cover = ""+ ds.child("cover").getValue();



                    //set data -- establecer datos
                    nameTv.setText(name);
                    emailTv.setText(email);
                    phoneTv.setText(phone);
                    try {
                        //if image is received then set -- si se recibe la imagen, establezca
                        Picasso.get().load(image).into(avatarIv);

                    }catch (Exception e) {
                        //if there is any exeption while getting image then set default -- si hay alguna excepción al obtener la imagen, configure el valor predeterminado

                        Picasso.get().load(R.drawable.ic_default_img_white).into(avatarIv);

                    }

                    try {
                        //if image is received then set -- si se recibe la imagen, establezca
                        Picasso.get().load(cover).into(coverIv);

                    }catch (Exception e) {
                        //if there is any exeption while getting image then set default -- si hay alguna excepción al obtener la imagen, configure el valor predeterminado


                    }

                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        postList = new ArrayList<>();

        checkUserStatus();
        loadHistPosts();

    }

    private void loadHistPosts() {
        //linear layout for recyclerview
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        //show newest post firs, for this load from last
        // muestra las últimas publicaciones, para esta carga desde el último
        layoutManager.setStackFromEnd(true);
        layoutManager.setReverseLayout(true);
        //set this layout to recyclerview  -- establece este diseño para recyclerview
        postsRecyclerView.setLayoutManager(layoutManager);

        //init post list
        DatabaseReference ref  = FirebaseDatabase.getInstance().getReference("Posts");
        //query to load  posts
        /*whenever user publishes a post the uid of this  user is also saved as info of post
         * so we're retrieving posts having uid equals to uid of current user */
        /* cada vez que el usuario publica una publicación, el uid de este usuario también se guarda como información de la publicación
         * así que estamos recuperando publicaciones que tienen uid igual a uid del usuario actual */
        Query query = ref.orderByChild("uid").equalTo(uid);
        //get all data from this ref
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                postList.clear();
                for (DataSnapshot ds: dataSnapshot.getChildren()) {
                    ModelPost myPosts = ds.getValue(ModelPost.class);

                    //add  to list
                    postList.add(myPosts);

                    //adapter
                    adapterPosts = new AdapterPosts(ThereProfileActivity.this, postList);
                    //set this adapter to recyclerview
                    postsRecyclerView.setAdapter(adapterPosts);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });

    }

    private void searchHistPosts(final String searchQuery) {
        //linear layout for recyclerview
        LinearLayoutManager layoutManager = new LinearLayoutManager(ThereProfileActivity.this);
        //show newest post firs, for this load from last
        // muestra las últimas publicaciones, para esta carga desde el último
        layoutManager.setStackFromEnd(true);
        layoutManager.setReverseLayout(true);
        //set this layout to recyclerview  -- establece este diseño para recyclerview
        postsRecyclerView.setLayoutManager(layoutManager);

        //init post list
        DatabaseReference ref  = FirebaseDatabase.getInstance().getReference("Posts");
        //query to load  posts
        /*whenever user publishes a post the uid of this  user is also saved as info of post
         * so we're retrieving posts having uid equals to uid of current user */
        /* cada vez que el usuario publica una publicación, el uid de este usuario también se guarda como información de la publicación
         * así que estamos recuperando publicaciones que tienen uid igual a uid del usuario actual */
        Query query = ref.orderByChild("uid").equalTo(uid);
        //get all data from this ref
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                postList.clear();
                for (DataSnapshot ds: dataSnapshot.getChildren()) {
                    ModelPost myPosts = ds.getValue(ModelPost.class);

                    if (myPosts.getpTitle().toLowerCase().contains(searchQuery.toLowerCase()) ||
                            myPosts.getpDescr().toLowerCase().contains(searchQuery.toLowerCase())) {
                        //add  to list
                        postList.add(myPosts);
                    }

                    //adapter
                    adapterPosts = new AdapterPosts(ThereProfileActivity.this, postList);
                    //set this adapter to recyclerview
                    postsRecyclerView.setAdapter(adapterPosts);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(ThereProfileActivity.this, ""+databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

    }

    private void checkUserStatus(){
        //get current user -- obtener usuario actual
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if(user != null){
            //user is signed in stay here -- El usuario ha iniciado sesión aquí.
            // set email of logged in user -- configurar el correo electrónico del usuario conectado
            //mProfileTv.setText(user.getEmail());
        }
        else {
            //user not signed in, go to main activity -- usuario no ha iniciado sesión, vaya a la actividad principal
            startActivity(new Intent(this,  MainActivity.class));
            finish();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return super.onSupportNavigateUp();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        menu.findItem(R.id.action_add_post).setVisible(false); //hide add post from this activity--ocultar agregar publicación de esta actividad

        //searchView
        MenuItem item = menu.findItem(R.id.action_search);
        //v7 searchview ot search user specific posts
        android.widget.SearchView searchView = (android.widget.SearchView) MenuItemCompat.getActionView(item);
        //search listener
        searchView.setOnQueryTextListener(new android.widget.SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                //called when user press search button from keyboard
                //if search query is not empty the search

                // se llama cuando el usuario presiona el botón de búsqueda desde el teclado
                // si la consulta de búsqueda no está vacía, la búsqueda
                if (!TextUtils.isEmpty(s.trim())) {
                    //search text contains text, search it -- buscar texto contiene texto, búscalo
                    searchHistPosts(s);

                }else {
                    //search text empty, get all users -- Buscar texto vacío, obtener todos los usuarios
                    loadHistPosts();
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                //called whenever user press any single letter
                //if search query is not empty the search
                if (!TextUtils.isEmpty(s.trim())) {
                    //search text contains text, search it
                    searchHistPosts(s);

                }else {
                    //search text empty, get all users
                    loadHistPosts();
                }
                return false;
            }
        });

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if(id == R.id.action_logout){
            firebaseAuth.signOut();
            checkUserStatus();
        }

        return super.onOptionsItemSelected(item);
    }
}
