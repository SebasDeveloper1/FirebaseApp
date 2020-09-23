package com.canibal.firebaseapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.canibal.firebaseapp.adapters.AdapterChat;
import com.canibal.firebaseapp.adapters.AdapterUsers;
import com.canibal.firebaseapp.models.ModelChat;
import com.canibal.firebaseapp.models.ModelUser;
import com.canibal.firebaseapp.notifications.Data;
import com.canibal.firebaseapp.notifications.Sender;
import com.canibal.firebaseapp.notifications.Token;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
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
import com.google.gson.Gson;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ChatActivity extends AppCompatActivity {


    //views from xml -- vistas del xml
    Toolbar toolbar;
    RecyclerView recyclerView;
    ImageView profileIv, blockIv;
    TextView nameTv, userStatusTv;
    EditText messageEt;
    ImageButton sendBtn, attachBtn;

    //firebase auth
    FirebaseAuth firebaseAuth;

    FirebaseDatabase firebaseDatabase;
    DatabaseReference usersDbRef;
    //for checking if user has seen message or not
    // para verificar si el usuario ha visto el mensaje o no
    ValueEventListener seenListener;
    DatabaseReference userRefForSeen;

    List<ModelChat> chatList;
    AdapterChat adapterChat;


    String hisUid;
    String myUid;
    String hisImage;

    boolean isBlocked = false;

    //volley request queue for notification
    private RequestQueue requestQueue;

    private boolean notify = false;


    //permissions constants
    // permisos constantes
    private static  final int CAMERA_REQUEST_CODE = 100;
    private static  final int STORAGE_REQUEST_CODE = 200;

    //image pick constants
    private static  final int IMAGE_PICK_CAMERA_CODE = 300;
    private static  final int IMAGE_PICK_GALLERY_CODE = 400;

    //permisions array
    //conjunto de permisos
    String[] cameraPermissions;
    String[] storagePermissions;

    //image pick will be samed in this uri
    // la selección de imagen se mostrará en esta uri
    Uri image_rui = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        //init views -- inicializar vistas
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle("");

        recyclerView = findViewById(R.id.chat_recyclerView);
        profileIv = findViewById(R.id.profileIv);
        blockIv = findViewById(R.id.blockIv);
        nameTv = findViewById(R.id.nameTv);
        userStatusTv =  findViewById(R.id.userStatusTv);
        messageEt = findViewById(R.id.messageEt);
        sendBtn = findViewById(R.id.sendBtn);
        attachBtn = findViewById(R.id.attachBtn);

        //init permissions array
        //inicializar matriz de permisos
        cameraPermissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        storagePermissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};

        requestQueue = Volley.newRequestQueue(getApplicationContext());


        //Layout (LinearLayout) for ReciclerView -- Diseño (LinearLayout) para ReciclerView
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setStackFromEnd(true);
        //RecyclerView properties -- Propiedades de ReciclerView
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(linearLayoutManager);

        /*on clicking user from users list we have passed that user's UID using intent
        * so get that uid here to get the profile picture, name and start chat with that
        * user*/
        /*al hacer clic en el usuario de la lista de usuarios, hemos pasado el UID de ese usuario con la intención
         * así que obtén ese uid aquí para obtener la foto de perfil, el nombre y comenzar a chatear
         * usuario */
        Intent intent = getIntent();
        hisUid = intent.getStringExtra("hisUid");

        //firebase Auth instance
        firebaseAuth = FirebaseAuth.getInstance();

        firebaseDatabase = FirebaseDatabase.getInstance();
        usersDbRef = firebaseDatabase.getReference("Users");

        //search user to get that user's info -- buscar usuario para obtener la información de ese usuario
        Query userQuery = usersDbRef.orderByChild("uid").equalTo(hisUid);
        //get user picture and name -- obtener foto y nombre del usuario
        userQuery.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                //check until required info is received -- verificar hasta que se reciba la información requerida
                for (DataSnapshot ds: dataSnapshot.getChildren()) {
                    //get data  -- obtener datos
                    String name = "" + ds.child("name").getValue();
                    hisImage = "" + ds.child("image").getValue();
                    String typingStatus = "" + ds.child("typingTo").getValue();

                    //check typing status -- verificar el estado escribiendo
                    if (typingStatus.equals(myUid)) {
                        userStatusTv.setText("Typing...");
                    } else {

                        //get value of onlinestatus -- obtener el valor de onlinestatus
                        String onlineStatus = "" + ds.child("onlineStatus").getValue();
                        if (onlineStatus.equals("online")) {
                            userStatusTv.setText(onlineStatus);

                        } else {
                            //convert timestamp to proper time date  - convertir la fecha y hora a la fecha correcta
                            //convert time stamp to dd/mm/yyyy hh:mm am/pm
                            //convertir la marca de tiempo a dd/mm/aaaa hh:mm am/pm
                            Calendar cal = Calendar.getInstance(Locale.ENGLISH);
                            cal.setTimeInMillis(Long.parseLong(onlineStatus));
                            String dateTime = DateFormat.format("dd/MM/yyyy hh:mm aa", cal).toString();
                            userStatusTv.setText("Last seen at: " + dateTime);
                        }

                    }


                    //set data -- establecer datos
                    nameTv.setText(name);
                    try {
                        //image received, set it to imageView in toolbar -- imagen recibida, configúrela en imageView en la barra de herramientas
                        Picasso.get().load(hisImage).placeholder(R.drawable.ic_default_img_white).into(profileIv);
                    }
                    catch (Exception e){
                        //there is exeption getting picture, set default picture -- hay una excepción obteniendo imagen, establecer imagen predeterminada
                        Picasso.get().load(R.drawable.ic_default_img_white).into(profileIv);

                    }

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        //click button to send message -- haga clic para enviar mensaje
        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                notify = true;
                //get text from the edit text  -- obtener texto de EditText
                String message = messageEt.getText().toString().trim();
                //check if text is empty or not -- comprobar si el texto está vacío o no
                if (TextUtils.isEmpty(message)) {
                    //text empty -- texto vacio
                    Toast.makeText(ChatActivity.this, "Cannot send the empty message...", Toast.LENGTH_SHORT).show();
                }else {
                    //text not empty -- texto no vacio
                    sendMessage(message);
                }
                //reset EditText after sending message -- restablecer EditText después de enviar el mensaje
                messageEt.setText("");
            }
        });

        //click button to import image
        attachBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //show image pick dialog
                //muestra el cuadro de dialogo de seleccion de imagen
                showImagePickDialog();

            }
        });


        //check edit text change listener -- comprobar edit text de cambio de texto
        messageEt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().trim().length() ==0){
                    checkTypingStatus("noOne");
                }
                else {
                    checkTypingStatus(hisUid);//uid of receiver --  uid del reseptor
                }

            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        blockIv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isBlocked) {
                    unBlockUser();
                }
                else {
                    blockUser();
                }
            }
        });

        readMessages();

        checkIsBlocked();

        seenMessage();

    }

    private void checkIsBlocked() {
        //check if each user if is blocked or not
        //if uid of user exists in "BlockedUsers" the that user is blocked, otherwise not
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(firebaseAuth.getUid()).child("BlockedUsers").orderByChild("uid").equalTo(hisUid)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for (DataSnapshot ds: dataSnapshot.getChildren()){
                            if (ds.exists()){
                                blockIv.setImageResource(R.drawable.ic_blocked_red);
                                isBlocked = true;
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });

    }

    private void blockUser() {
        //block the user, by adding uid to current user's "BlockedUsers" node

        //put values in hasmap to put in db
        HashMap<String, String> hashMap = new HashMap<>();
        hashMap.put("uid", hisUid);

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(myUid).child("BlockedUsers").child(hisUid).setValue(hashMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        //blocked successfully
                        Toast.makeText(ChatActivity.this, "Blocked Successfully...", Toast.LENGTH_SHORT).show();

                        blockIv.setImageResource(R.drawable.ic_blocked_red);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        //failed to blocked
                        Toast.makeText(ChatActivity.this, "Failed: "+e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void unBlockUser() {
        //Unblock the user, by removing uid from current user's "BlockedUsers" node

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(myUid).child("BlockedUsers").orderByChild("uid").equalTo(hisUid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for (DataSnapshot ds: dataSnapshot.getChildren()){
                            if (ds.exists()){
                                //removed blocked user data from current user's BlockedUsers list
                                ds.getRef().removeValue()
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                //unblocked successfully
                                                Toast.makeText(ChatActivity.this, "Unblocked Successfully...", Toast.LENGTH_SHORT).show();
                                                blockIv.setImageResource(R.drawable.ic_unblocked_white);
                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                //failed to unblocked
                                                Toast.makeText(ChatActivity.this, "Failed: "+e.getMessage(), Toast.LENGTH_SHORT).show();

                                            }
                                        });
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });

    }



    private void showImagePickDialog() {
        //options (camera , gallery) to show in dialog
        // opciones (cámara, galería) para mostrar en el diálogo
        String[] options = {"Camera", "Gallery"};

        //dialog -- dialogo
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose Image from");

        //set options to dialog
        //establecer opciones para dialogo
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //item click handle
                //haga clic en el elemento del controlador
                if (which == 0){
                    //camera clicked
                    //hacer clicl en camara
                    if (!checkCameraPermission()){
                        requestCameraPermission();
                    }else {
                        pickFromCamera();
                    }

                }
                if (which == 1){
                    //gallery clicked
                    //hacer click en galeria
                    if (!checkStoragePermission()){
                        requestStoragePermission();
                    }else {
                        pickFromGallery();
                    }

                }
            }
        });
        //create and show dialog
        //crear y mostrar dialogo
        builder.create().show();
    }

    private void pickFromGallery() {
        //intent to pick image from gallery
        // intento de elegir una imagen de la galería
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, IMAGE_PICK_GALLERY_CODE);
    }

    private void pickFromCamera() {
        //intent to pick image from camera
        // intento de tomar una imagen de la cámara
        ContentValues cv = new ContentValues();
        cv.put(MediaStore.Images.Media.TITLE, "Temp Pick");
        cv.put(MediaStore.Images.Media.DESCRIPTION, "Temp Descr");
        image_rui = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cv);

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, image_rui);
        startActivityForResult(intent, IMAGE_PICK_CAMERA_CODE);

    }

    private boolean checkStoragePermission(){
        //check if storage permission is enable or not
        //return true if enable
        //return false if not enable

        // verifica si el permiso de almacenamiento está habilitado o no
        // devuelve verdadero si esta habilitado
        // devuelve falso si no está habilitado

        boolean result = ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);
        return  result;
    }

    private void requestStoragePermission(){
        //request runtime storage permission
        // solicitar permiso de almacenamiento en tiempo de ejecución
        ActivityCompat.requestPermissions(this, storagePermissions, STORAGE_REQUEST_CODE);
    }


    private boolean checkCameraPermission(){
        //check if camera permission is enable or not
        //return true if enable
        //return false if not enable

        // verifica si el permiso de camara está habilitado o no
        // devuelve verdadero si esta habilitado
        // devuelve falso si no está habilitado

        boolean result = ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA) == (PackageManager.PERMISSION_GRANTED);
        boolean result1 = ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);
        return  result && result1;
    }

    private void requestCameraPermission(){
        //request runtime camera permission
        // solicitar permiso de camara en tiempo de ejecución
        ActivityCompat.requestPermissions(this, cameraPermissions, CAMERA_REQUEST_CODE);
    }



    private void seenMessage() {
        userRefForSeen = FirebaseDatabase.getInstance().getReference("Chats");
        seenListener = userRefForSeen.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot ds: dataSnapshot.getChildren()){
                    ModelChat chat = ds.getValue(ModelChat.class);
                    if (chat.getReceiver().equals(myUid) && chat.getSender().equals(hisUid)) {
                        HashMap<String, Object> hasSeenHashMap = new HashMap<>();
                        hasSeenHashMap.put("isSeen", true);
                        ds.getRef().updateChildren(hasSeenHashMap);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void readMessages() {
        chatList = new ArrayList<>();
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("Chats");
        dbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                chatList.clear();
                for (DataSnapshot ds: dataSnapshot.getChildren()){
                    ModelChat chat = ds.getValue(ModelChat.class);
                    if (chat.getReceiver().equals(myUid) && chat.getSender().equals(hisUid) ||
                            chat.getReceiver().equals(hisUid) && chat.getSender().equals(myUid)) {
                        chatList.add(chat);
                    }
                    //adapter -- adaptador
                    adapterChat = new AdapterChat(ChatActivity.this, chatList, hisImage);
                    adapterChat.notifyDataSetChanged();
                    //set adapter to recyclerview -- configurar el adaptador para reciclar
                    recyclerView.setAdapter(adapterChat);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void sendMessage(final String message) {
        /*"chats" node will be created that will contain all chats
        * whenever user sends message it will create new child in "Chats" node and that child will contain
        * the followind key values
        * sender: UID of sender
        * receiver: UID if receiver
        * message:the actual message*/

        /* se creará el nodo "chats" que contendrá todos los chats
         * cada vez que el usuario envíe un mensaje, creará un nuevo hijo en el nodo "Chats" y ese hijo contendrá
        * los siguientes valores clave
        * remitente: UID del remitente
        * receptor: UID si el receptor
        * mensaje: el mensaje real */


        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();

        String timestamp = String.valueOf(System.currentTimeMillis());

        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("sender", myUid);
        hashMap.put("receiver", hisUid);
        hashMap.put("message", message);
        hashMap.put("timestamp", timestamp);
        hashMap.put("isSeen", false);
        hashMap.put("type", "text");
        databaseReference.child("Chats").push().setValue(hashMap);

        final DatabaseReference database = FirebaseDatabase.getInstance().getReference("Users").child(myUid);
        database.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                ModelUser user = dataSnapshot.getValue(ModelUser.class);

                if (notify){
                    senNotification(hisUid, user.getName(), message);
                }
                notify = false;
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        //create chatlist node/child in firebase database
        // crea un nodo / hijo de la lista de chat en la base de datos de Firebase
        final DatabaseReference chatRef1 = FirebaseDatabase.getInstance().getReference("Chatlist")
                .child(myUid)
                .child(hisUid);

        chatRef1.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists()) {
                    chatRef1.child("id").setValue(hisUid);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        final DatabaseReference chatRef2 = FirebaseDatabase.getInstance().getReference("Chatlist")
                .child(hisUid)
                .child(myUid);

        chatRef2.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists()) {
                    chatRef2.child("id").setValue(myUid);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    private void sendImageMessage(Uri image_rui) throws IOException {
        notify = true;

        //progress dialog
        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Sending Image...");
        progressDialog.show();


        final String timeStamp = ""+System.currentTimeMillis();

        String fileNameAndPath = "ChatImages/"+"post_"+timeStamp;

        /*Chats node will be create that will contain all images sent via chat*/

        //get bitmap from image uri
        Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), image_rui);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
        final byte[] data = baos.toByteArray(); //conver image to bytes
        StorageReference ref = FirebaseStorage.getInstance().getReference().child(fileNameAndPath);
        ref.putBytes(data)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        //image uploaded
                        progressDialog.dismiss();
                        //get url of uploaded image
                        Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                        while (!uriTask.isSuccessful());
                        String downloadUri = uriTask.getResult().toString();

                        if (uriTask.isSuccessful()){
                            //add image uri and other info to data
                            DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();

                            //setup required data
                            HashMap<String, Object> hashMap = new HashMap<>();
                            hashMap.put("sender", myUid);
                            hashMap.put("receiver", hisUid);
                            hashMap.put("message", downloadUri);
                            hashMap.put("timestamp", timeStamp);
                            hashMap.put("type", "image");
                            hashMap.put("isSeen", false);
                            //put this data to firebase
                            databaseReference.child("Chats").push().setValue(hashMap);

                            //send notification
                            DatabaseReference database = FirebaseDatabase.getInstance().getReference("Users").child(myUid);
                            database.addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    ModelUser user = dataSnapshot.getValue(ModelUser.class);

                                    if (notify) {
                                        senNotification(hisUid, user.getName(), "Sent you a photo...");
                                    }
                                    notify = false;
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                }
                            });


                            //create chatlist node/child in firebase database
                            // crea un nodo / hijo de la lista de chat en la base de datos de Firebase
                            final DatabaseReference chatRef1 = FirebaseDatabase.getInstance().getReference("Chatlist")
                                    .child(myUid)
                                    .child(hisUid);

                            chatRef1.addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    if (!dataSnapshot.exists()) {
                                        chatRef1.child("id").setValue(hisUid);
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                }
                            });

                            final DatabaseReference chatRef2 = FirebaseDatabase.getInstance().getReference("Chatlist")
                                    .child(hisUid)
                                    .child(myUid);

                            chatRef2.addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    if (!dataSnapshot.exists()) {
                                        chatRef2.child("id").setValue(myUid);
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                }
                            });


                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        //failed
                        progressDialog.dismiss();
                    }
                });



    }


    private void senNotification(final String hisUid, final String name, final String message) {
        DatabaseReference allTokens = FirebaseDatabase.getInstance().getReference("Tokens");
        Query query = allTokens.orderByKey().equalTo(hisUid);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot ds: dataSnapshot.getChildren()){
                    Token token = ds.getValue(Token.class);
                    Data data = new Data(
                            ""+myUid,
                            ""+name + ": " + message,
                            "New Message",
                            ""+hisUid,
                            "ChatNotification",
                            +R.drawable.ic_default_img);

                    Sender sender = new Sender(data, token.getToken());

                    //fcm json object request
                    try {
                        JSONObject senderJsonObj = new JSONObject(new Gson().toJson(sender));
                        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest("https://fcm.googleapis.com/fcm/send", senderJsonObj,
                                new Response.Listener<JSONObject>() {
                                    @Override
                                    public void onResponse(JSONObject response) {
                                        //response of the request
                                        Log.d("JSON_RESPONSE", "onResponse: "+response.toString());

                                    }
                                }, new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                Log.d("JSON_RESPONSE", "onResponse: "+error.toString());

                            }
                        }){
                            @Override
                            public Map<String, String> getHeaders() throws AuthFailureError {
                                //put params
                                Map<String, String> headers = new HashMap<>();
                                headers.put("Content-Type", "application/json");
                                headers.put("Authorization", "key=AAAAzUYnnOI:APA91bEeBlMjNd9EdUeSyKyRtWJJ4Ex6jqBkrdU6s46yf13quyDDa5wzQfRO-6RJTsy-c68kceB1cRQvWVFYUzIA0UQ49N8R9QS6lIR2xb3w6eoSuA8L118FUjcCzyFlqjCypOtm3Zef");
                                return headers;
                            }
                        };

                        //add this request to queue
                        requestQueue.add(jsonObjectRequest);

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

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
            myUid = user.getUid(); //currently signed in user's uid -- UID del usuario actualmente conectado

        }else {
            //user not signed in, go to main activity -- usuario no ha iniciado sesión, vaya a la actividad principal
            startActivity(new Intent(this,  MainActivity.class));
            finish();
        }
    }

    private void checkOnlineStatus(String status){
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("Users").child(myUid);
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("onlineStatus", status);
        //update value of onileStatus  of current user -- valor de actualización de onileStatus del usuario actual
        dbRef.updateChildren(hashMap);
    }

    private void checkTypingStatus(String typing){
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("Users").child(myUid);
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("typingTo", typing);
        //update value of onileStatus  of current user -- valor de actualización de onileStatus del usuario actual
        dbRef.updateChildren(hashMap);
    }

    @Override
    protected void onStart() {
        checkUserStatus();
        //set online -- establecer en linea
        checkOnlineStatus("online");
        super.onStart();
    }

    @Override
    protected void onPause() {
        super.onPause();
        //get timestamp -- obtener marca de tiempo
        String timestamp = String.valueOf(System.currentTimeMillis());
        //set ofline with last seen time stamp -- conjunto de línea con la última marca de tiempo vista
        checkOnlineStatus(timestamp);
        checkTypingStatus("noOne");
        userRefForSeen.removeEventListener(seenListener);
    }

    @Override
    protected void onResume() {
        //set online -- establecer en linea
        checkOnlineStatus("online");
        super.onResume();
    }

    //handle permissions results
    // manejar resultados de permisos
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        //this method is called when user press Allow or Deny from permissions request dialog
        //here we will handle permissions cases (allowed and denied)

        // se llama a este método cuando el usuario presiona Permitir o Denegar desde el diálogo de solicitud de permisos
        // aquí manejaremos los casos de permisos (permitidos y denegados)

        switch (requestCode){
            case CAMERA_REQUEST_CODE:{
                if (grantResults.length>0){
                    boolean cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean storageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                    if (cameraAccepted && storageAccepted){
                        //both permission are granted
                        // se otorgan ambos permisos
                        pickFromCamera();
                    }else {

                        //camera or gallery or both permissions were denied
                        // cámara o galería o ambos permisos fueron denegados
                        Toast.makeText(this, "Camera & Storage both permissions are neccessary...", Toast.LENGTH_SHORT).show();
                    }

                }else {

                }

            }
            break;
            case STORAGE_REQUEST_CODE:{
                if (grantResults.length>0){
                    boolean storageAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    if (storageAccepted){
                        //storage permission granted
                        //permiso de almacenamiento otorgado
                        pickFromGallery();
                    }else {

                        //camera or gallery or both permissions were denied
                        // cámara o galería o ambos permisos fueron denegados
                        Toast.makeText(this, "Storage permissions neccessary...", Toast.LENGTH_SHORT).show();
                    }
                }else {

                }
            }
            break;

        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        //this method will be called after picking image from camera or gallery
        // se llamará a este método después de seleccionar la imagen de la cámara o galería
        if (resultCode == RESULT_OK){

            if (requestCode == IMAGE_PICK_GALLERY_CODE){
                //Image is picked from gallery, get uri of image
                // La imagen se selecciona de la galería, obtén el uri de la imagen
                image_rui = data.getData();

                //use this image uri to upload to firebase storage
                try {
                    sendImageMessage(image_rui);
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
            else if (requestCode == IMAGE_PICK_CAMERA_CODE){
                //image is picked from camera, get uri of image
                // la imagen se toma de la cámara, obtén el uri de la imagen

                try {
                    sendImageMessage(image_rui);
                } catch (IOException e) {
                    e.printStackTrace();
                }


            }

        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        //hide searchview, add post, as we dont need it here -- oculta la vista de búsqueda, ya que no la necesitamos aquí
        menu.findItem(R.id.action_search).setVisible(false);
        menu.findItem(R.id.action_add_post).setVisible(false);

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
