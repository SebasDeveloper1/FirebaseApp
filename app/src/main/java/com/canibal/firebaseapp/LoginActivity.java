package com.canibal.firebaseapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

public class LoginActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 100;
    GoogleSignInClient mGoogleSignInClient;

    //views -- vistas
    EditText mEmailEt,mPasswordEt;
    TextView notHaveAnccountTv,mRecoverPassTv;
    Button mLoginBtn;
    SignInButton mGoogleLoginBtn;

    //Declare an instance of FirebaseAuth
    private FirebaseAuth mAuth;

    //progress dialog -- diálogo de progreso
    ProgressDialog pd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        //Actionbar and its title
        //Habilitar barra y titulo
        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle("Login");
        // Eenable back button
        //Boton de retroceso habilitar
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);

        //before mAuth
        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this,gso);

        //In the onCreate() method, initialize the FirebaseAuth instance. --En el método onCreate (), inicialice la instancia de FirebaseAuth.
        mAuth = FirebaseAuth.getInstance();

        //init --- Inicializar
        mEmailEt = findViewById(R.id.emailEt);
        mPasswordEt = findViewById(R.id.passwordEt);
        notHaveAnccountTv = findViewById(R.id.nothave_accountTv);
        mRecoverPassTv = findViewById(R.id.recoverpasswordTv);
        mLoginBtn = findViewById(R.id.loginBtn);
        mGoogleLoginBtn = findViewById(R.id.googleLoginBtn);

        // login button click --haga clic en el botón de inicio de sesión
        mLoginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //input data --datos de entrada
                String email = mEmailEt.getText().toString();
                String passw = mPasswordEt.getText().toString().trim();
                if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
                    //invalid email paatern set error -- error de configuración de paatern de correo electrónico no válido
                    mEmailEt.setError("Invalid Email");
                    mEmailEt.setFocusable(true);

                }else {
                    //valid email pattern -- patrón de correo electrónico válido
                    logiUser(email,passw);
                }

            }
        });
        //not have acoount textview click --  haga click en el textview de no tiene cuenta
        notHaveAnccountTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginActivity.this,RegisterActivity.class));
            }
        });
        //////////////paso 3 boton de recovery///////////////////////ambios en el xml solo al final y variables al inicio de este

        //Recover pass textview clik -- Recuperar paso textview clik
        mRecoverPassTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showRecoverPasswordDialog();
            }
        });

        //handle google login btn click --  manejar el inicio de sesión de google btn clic
        mGoogleLoginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //begin google login progress -- comenzar el progreso de inicio de sesión de google
                Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                startActivityForResult(signInIntent, RC_SIGN_IN);

            }
        });

        //init progress dialog  -- inicializar el dialogo de progreso
        pd = new ProgressDialog(this);
    }

    private void showRecoverPasswordDialog() {
        //AlertDialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Recover Password");


        //set layout linearlayout
        LinearLayout linearLayout = new LinearLayout(this);
        // views to set in dialog
        final EditText emailEt = new EditText(this);
        emailEt.setHint("Email");
        emailEt.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);

        /*sets the min width of a EditView to fit a text of n 'M' letters regardless of the actual text extension and text size.*/
        /*establece el ancho mínimo de un EditView para que se ajuste a un texto de n letras 'M' independientemente de la extensión de texto real y el tamaño del texto.*/
        emailEt.setMinEms(16);

        linearLayout.addView(emailEt);
        linearLayout.setPadding(10,10,10,10);

        builder.setView(linearLayout);

        //Buttons recovery
        builder.setPositiveButton("Recover", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //input email
                String email = emailEt.getText().toString().trim();
                beginRecovery(email);

            }
        });

        //Buttons Cancel
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //dismiss dialog --cerrar diálogo
                dialog.dismiss();

            }
        });

        //show dialog
        builder.create().show();

    }

    private void beginRecovery(String email) {
        mAuth.setLanguageCode("es");
        // show progress dialog  -- mostrar el dialogo de progreso
        pd.setMessage("Sending email...");
        pd.show();
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                pd.dismiss();
                if(task.isSuccessful()){
                    Toast.makeText(LoginActivity.this, "Email sent", Toast.LENGTH_SHORT).show();
                }
                else {
                    Toast.makeText(LoginActivity.this, "Failed...", Toast.LENGTH_SHORT).show();
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                pd.dismiss();
                //get and show proper error message --- obtener y mostrar el mensaje de error adecuado
                Toast.makeText(LoginActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void logiUser(String email, String passw) {
        // show progress dialog  -- mostrar el dialogo de progreso
        pd.setMessage("Logging In...");
        pd.show();
        mAuth.signInWithEmailAndPassword(email, passw)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // dismiss progress dialog -- descartar el diálogo de progreso
                            pd.dismiss();

                            // Sign in success, update UI with the signed-in user's information
                            FirebaseUser user = mAuth.getCurrentUser();
                            //user is logged in, so start LoginActivity -- usuario ha iniciado sesión, así que inicie LoginActivity
                            startActivity(new Intent(LoginActivity.this, DashboardActivity.class));
                            finish();

                        } else {
                            // dismiss progress dialog -- descartar el diálogo de progreso
                            pd.dismiss();

                            // If sign in fails, display a message to the user.
                            Toast.makeText(LoginActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        }

                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                // dismiss progress dialog -- descartar el diálogo de progreso
                pd.dismiss();

                //error,  get and show error message -- error, obtener y mostrar mensaje de error
                Toast.makeText(LoginActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();//go previous activity  -- ir a la actividad anterior
        return super.onSupportNavigateUp();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
                // Google Sign In failed, update UI appropriately
                Toast.makeText(this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                // ...
            }
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {

        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information

                            FirebaseUser user = mAuth.getCurrentUser();

                            //if user is signing if first time then get and show user info from google account
                            if (task.getResult().getAdditionalUserInfo().isNewUser()) {

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

                            }
                            //show user email in toast -- mostrar el correo electrónico del usuario en un Toast
                            Toast.makeText(LoginActivity.this, ""+user.getEmail(), Toast.LENGTH_SHORT).show();
                            //go to profile activity sfter logged in -- ir a la actividad de perfil después de iniciar sesión
                            startActivity(new Intent(LoginActivity.this, DashboardActivity.class));
                            finish();
                            //updateUI(user);
                        } else {
                            // If sign in fails, display a message to the user.
                            Toast.makeText(LoginActivity.this, "Login Failed...", Toast.LENGTH_SHORT).show();
                            //updateUI(null);
                        }

                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                //get and show error message --obtener y mostrar mensaje de error
                Toast.makeText(LoginActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}

