package com.canibal.firebaseapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

public class RegisterActivity extends AppCompatActivity {

    //views
    EditText mEmailEt, mPasswordEt;
    Button mRegisterBtn;
    TextView mHaveAccountTv;

    //progressbar to display while registering user
    ProgressDialog progressDialog;

    //Declare an instance of FirebaseAuth -- Declarar una instancia de FirebaseAuth
    private FirebaseAuth mAuth;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        //Actionbar and its title
        //Habilitar barra y titulo
        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle("Create Account");
        // Eenable back button
        //Boton de retroceso habilitar
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);

        //init -- inicializar
        mEmailEt = findViewById(R.id.emailEt);
        mPasswordEt = findViewById(R.id.passwordEt);
        mRegisterBtn = findViewById(R.id.registerBtn);
        mHaveAccountTv = findViewById(R.id.have_accountTv);

        //In the onCreate() method, initialize the FirebaseAuth instance. -- En el método onCreate (), inicialice la instancia de FirebaseAuth.
        mAuth = FirebaseAuth.getInstance();

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Registrering User...");

        //handle register button click -- haga clic en el botón de registro

        mRegisterBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //input Email, Password -- entrada de correo electrónico, contraseña
                String email = mEmailEt.getText().toString().trim();
                String password = mPasswordEt.getText().toString().trim();
                //validate -- validar
                if(!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    //set error and focuss to email edittext -- establecer error y enfoques al correo electrónico edittext
                    mEmailEt.setError("Invalid Email");
                    mEmailEt.setFocusable(true);
                }
                else if (password.length() < 6) {
                    //set error and focuss to password edittext -- establecer error y enfoques a la contraseña edittext
                    mPasswordEt.setError("Password length at least 6 characters");
                    mPasswordEt.setFocusable(true);
                }
                else {
                    registerUser(email, password); //register the user -- registra el usuario
                }

            }
        });
        //handle login textview click listener --manejar login textview click oyente
        mHaveAccountTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                finish();
            }
        });
    }

    private void registerUser(String email, String password) {
        //email and password pattern is valid, show progress dialog and start registering user
        //el patrón de correo electrónico y contraseña es válido, muestra el diálogo de progreso y comienza a registrar usuarios
        progressDialog.show();

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, dismiss dialog and start register activity --cerrar el diálogo y comenzar a registrar actividad
                            progressDialog.dismiss();
                            FirebaseUser user = mAuth.getCurrentUser();
                            //get user email and uid from auth -- obtener correo electrónico y usuario de autenticación
                            String email = user.getEmail();
                            String uid = user.getUid();
                            //when user is registered store user info in firebase realtime database too --cuando el usuario está registrado, almacena la información del usuario en la base de datos en tiempo real de Firebase también
                            //using HashMap --usando hashmap
                            HashMap<Object, String> hashMap = new HashMap<>();
                            //Put info in hashmap -- poner información en hashmap
                            hashMap.put("email", email);
                            hashMap.put("uid", uid);
                            hashMap.put("name", ""); // will add later  (e.g. edit profile) --agregará más tarde
                            hashMap.put("onlineStatus", "online"); // will add later  (e.g. edit profile) --agregará más tarde
                            hashMap.put("typingTo", "noOne"); // will add later  (e.g. edit profile) --agregará más tarde
                            hashMap.put("phone", "");// will add later  (e.g. edit profile) --agregará más tarde
                            hashMap.put("image", "");// will add later  (e.g. edit profile) --agregará más tarde
                            hashMap.put("cover", "");// will add later  (e.g. edit profile) --agregará más tarde

                            // firebase database instance -- instancia de base de datos firebase
                            FirebaseDatabase database = FirebaseDatabase.getInstance();
                            //path to store user data named -- "Users"ruta para almacenar datos de usuario llamados "Users"
                            DatabaseReference reference = database.getReference("Users");
                            //put data within hashmap in database -- poner datos dentro de hashmap en la base de datos
                            reference.child(uid).setValue(hashMap);


                            Toast.makeText(RegisterActivity.this, "Registered...\n"+user.getEmail(), Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(RegisterActivity.this, DashboardActivity.class));
                            finish();
                        } else {
                            // If sign in fails, display a message to the user. -- Mostrar un mensaje al usuario
                            progressDialog.dismiss();
                            Toast.makeText(RegisterActivity.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                        }

                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                //error, dismiss progress dialog and get and show the error message -- error, descartar el diálogo de progreso y obtener y mostrar el mensaje de error
                progressDialog.dismiss();
                Toast.makeText(RegisterActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();//go previous activity  -- ir a la actividad anterior
        return super.onSupportNavigateUp();
    }
}

