package com.canibal.firebaseapp;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.ContextCompat;
import androidx.core.view.MenuItemCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.canibal.firebaseapp.adapters.AdapterPosts;
import com.canibal.firebaseapp.models.ModelPost;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
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
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static android.app.Activity.RESULT_OK;
import static com.google.firebase.storage.FirebaseStorage.getInstance;


/**
 * A simple {@link Fragment} subclass.
 */
public class ProfileFragment extends Fragment {

    //Firebase
    FirebaseAuth firebaseAuth;
    FirebaseUser user;
    FirebaseDatabase firebaseDatabase;
    DatabaseReference databaseReference;

    //storage
    StorageReference storageReference;

    //path where images of user profileand cover will be stored -- ruta donde se almacenarán las imágenes del perfil del usuario y la portada
    String storagePath = "Users_Profile_Cover_Imgs/";

    // views from xml -- vistas de xml
    ImageView avatarIv,coverIv;
    TextView nameTv, emailTv, phoneTv;
    FloatingActionButton fab;
    RecyclerView postsRecyclerView;

    //progress dialog ---  dialogo de progreso
    ProgressDialog pd;

    //permissions constants -- permisos constantes
    private static final int CAMERA_REQUEST_CODE = 100;
    private static final int STORAGE_REQUEST_CODE = 200;
    private static final int IMAGE_PICK_GALLERY_CODE = 300;
    private static final int IMAGE_PICK_CAMERA_CODE = 400;
    //Arrays of permissions to be requested -- Matrices de permisos a solicitar
    String cameraPermissions[];
    String storagePermissions[];

    List<ModelPost> postList;
    AdapterPosts adapterPosts;
    String uid;


    //uri if picked image -- uri si se selecciona la imagen
    Uri image_uri;

    //for checking profile or cover photo -- para verificar el perfil o la foto de portada
    String profileOrCoverPhoto;


    public ProfileFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        //init firebase
        firebaseAuth = FirebaseAuth.getInstance();
        user = firebaseAuth.getCurrentUser();
        firebaseDatabase = FirebaseDatabase.getInstance();
        databaseReference = firebaseDatabase.getReference("Users");
        storageReference = getInstance().getReference(); //firebase storage reference -- referencia de almacenamiento

        //init arrays of the permissions --  inicializar matrices de permisos
        cameraPermissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        storagePermissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};

        //init views -- inicializar las vistas
        avatarIv = view.findViewById(R.id.avatarIv);
        coverIv = view.findViewById(R.id.coverIv);
        nameTv = view.findViewById(R.id.nameTv);
        emailTv = view.findViewById(R.id.emailTv);
        phoneTv = view.findViewById(R.id.phoneTv);
        fab = view.findViewById(R.id.fab);
        postsRecyclerView = view.findViewById(R.id.recyclerview_posts);


        //init progress dialog  --  inicializar dialogo de progreso
        pd = new ProgressDialog(getActivity());

        /*we have to get info of currently signed in user. we can get it using user's email or uid
        I'm gonna retrieve user detail using email -- tenemos que obtener información del usuario
        actualmente conectado. podemos obtenerlo usando el correo electrónico o el uid del usuario
        Voy a recuperar los detalles del usuario usando el correo electrónico*/

        /*By using orderByChild query we will show the detail from a node
        whose key named email has value equal to currently signed in email.
        It will search all nodes, where the key matches it will get its detail

        Al usar la consulta orderByChild mostraremos los detalles de un nodo
        cuya clave llamada correo electrónico tiene un valor igual al correo
        electrónico actualmente registrado.
        Buscará todos los nodos, donde la clave coincida obtendrá su detalle*/

        Query query= databaseReference.orderByChild("email").equalTo(user.getEmail());
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

        //fab button click -- clic en el botón fab
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showEditProfileDialog();
            }
        });

        postList = new ArrayList<>();

        checkUserStatus();
        loadMyPosts();


        return view;
    }

    private void loadMyPosts() {
        //linear layout for recyclerview
        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
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
                    adapterPosts = new AdapterPosts(getActivity(), postList);
                    //set this adapter to recyclerview
                    postsRecyclerView.setAdapter(adapterPosts);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                //in case of error se elimino es toast para correcto funcionamiento de cierre de secion
            }
        });
    }

    private void searchMyPosts(final String searchQuery) {
        //linear layout for recyclerview
        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
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
                    adapterPosts = new AdapterPosts(getActivity(), postList);
                    //set this adapter to recyclerview
                    postsRecyclerView.setAdapter(adapterPosts);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(getActivity(), ""+databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean checkStoragePermission(){
        //check if storage permissions is enabled or not -- verifica si los permisos de almacenamiento están habilitados o no
        //return true if enable -- devuelve verdadero si habilita
        //return false if not enabled -- devuelve falso si no está habilitado

        boolean result = ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == (PackageManager.PERMISSION_GRANTED);
        return result;
    }

    private void requestStoragePermission(){
        //request runtime storage permission -- solicitar permiso de almacenamiento en tiempo de ejecución
        requestPermissions(storagePermissions, STORAGE_REQUEST_CODE);
    }

    private boolean checkCameraPermission(){
        //check if storage permissions is enabled or not -- verifica si los permisos de almacenamiento están habilitados o no
        //return true if enable -- devuelve verdadero si habilita
        //return false if not enabled -- devuelve falso si no está habilitado
        boolean result = ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA)
                == (PackageManager.PERMISSION_GRANTED);

        boolean result1 = ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == (PackageManager.PERMISSION_GRANTED);
        return result && result1;
    }

    private void requestCameraPermission(){
        //request runtime storage permission -- solicitar permiso de almacenamiento en tiempo de ejecución
        requestPermissions(cameraPermissions, CAMERA_REQUEST_CODE);
    }

    private void showEditProfileDialog() {
        /*show dialog containig options
        * 1) Edit profile picture
        * 2) Edit cover photo
        * 3) Edit name
        * 4) Edit phone
        * 5) Change Password*/

        /* Mostrar diálogo que contiene opciones
                * 1) Editar foto de perfil
        * 2) Editar foto de portada
        * 3) Editar nombre
        * 4) Editar teléfono
        * 5) Cambiar contraseña*/

        //options to show dialog -- opciones para mostrar el diálogo
        String options[] = {"Editar foto de perfil", "Editar foto de portada", "Editar nombre", "Editar teléfono", "Cambiar Contraseña"};
        // alert dialog -- diálogo de alerta
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        //set title -- establecer título
        builder.setTitle("Choose Action");
        //set items to dialog -- establecer elementos para dialogar
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //handle dialog item clics -- manejar clics de elementos de diálogo
                if (which == 0){
                    // edit profile clicked
                    pd.setMessage("Updating profile picture");
                    profileOrCoverPhoto = "image"; // i.e. changing profile picture, make sure to assign same value -- es decir, cambiar la imagen de perfil, asegúrese de asignar el mismo valor
                    showImagePicDialog();

                }else if (which == 1){
                    //edit cover clicked
                    pd.setMessage("Updating cover photo");
                    profileOrCoverPhoto = "cover"; // i.e. changing cover photo, make sure to assign same value -- es decir, cambiar la foto de portada, asegúrese de asignar el mismo valor
                    showImagePicDialog();

                }else if (which == 2){
                    //edit name clicked
                    pd.setMessage("Updating Name");
                    //calling method and pass key "name" as parameter to update it's value in database
                    //método de llamada y clave de paso "nombre" como parámetro para actualizar su valor en la base de datos
                    showNamePhoneUpdateDialog("name");

                }else if (which == 3){
                    //edit phone clicked
                    pd.setMessage("Updating phone");
                    showNamePhoneUpdateDialog("phone");

                }else if (which == 4){
                //edit phone clicked
                pd.setMessage("Changing Password");
                showChangePasswordDialog();

            }
            }
        });
        //create and show dialog -- crear y mostrar diálogo
        builder.create().show();
    }

    private void showChangePasswordDialog() {
        //password change dialog with custom layout having  currentPassword, newPassword and update button

        //inflate layout for dialog
        View view = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_update_password, null);
        final EditText passwordEt = view.findViewById(R.id.passwordEt);
        final EditText newPasswordEt = view.findViewById(R.id.newPasswordEt);
        final Button updatePasswordBtn = view.findViewById(R.id.updatePasswordBtn);

        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(view); //set view to dialog

        final AlertDialog dialog = builder.create();
        dialog.show();

        updatePasswordBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //validate data
                String oldPassword = passwordEt.getText().toString().trim();
                String newPassword =newPasswordEt.getText().toString().trim();
                if (TextUtils.isEmpty(oldPassword)){
                    Toast.makeText(getActivity(), "Enter your to current password...", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (newPassword.length()<6){
                    Toast.makeText(getActivity(), "Password length must atleast 6 characters...", Toast.LENGTH_SHORT).show();
                    return;
                }

                dialog.dismiss();
                updatePassword(oldPassword, newPassword);
            }
        });
    }

    private void updatePassword(String oldPassword, final String newPassword) {
        pd.show();

        //get current user
        final FirebaseUser user = firebaseAuth.getCurrentUser();

        //before changing password re-authenticate th user
        AuthCredential authCredential = EmailAuthProvider.getCredential(user.getEmail(), oldPassword);
        user.reauthenticate(authCredential)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        //successfully authenticated, begin update

                        user.updatePassword(newPassword)
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        //password update
                                        pd.dismiss();
                                        Toast.makeText(getActivity(), "Password Updated...", Toast.LENGTH_SHORT).show();
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        //failed updating password, show reason
                                        pd.dismiss();
                                        Toast.makeText(getActivity(), ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        //authentication failed, show reason
                        pd.dismiss();
                        Toast.makeText(getActivity(), ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });

    }

    private void showNamePhoneUpdateDialog(final String key) {
        /*parameter "key" will contain value:
            either "name" which is key in user's database which is used to update user's name
            or "phone" which is key in user's database which is used to update user's phone*/

        /* parámetro "clave" contendrá el valor:
            ya sea "nombre", que es clave en la base de datos del usuario que se utiliza para actualizar el nombre del usuario
            o "teléfono" que es clave en la base de datos del usuario que se utiliza para actualizar el teléfono del usuario */

        //custom dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Update " + key); //  e.g. update name OR update phone -- nombre de actualización O teléfono de actualización
        //set layout of dialog -- establecer diseño de diálogo
        LinearLayout linearLayout = new LinearLayout(getActivity());
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setPadding(10,10,10,10);
        //add edit text - agregar texto de edición
        final EditText editText = new EditText(getActivity());
        editText.setHint("Enter "+ key); //hint e.g. Edit name OR Edit phone -- pista por ej. Editar nombre O Editar teléfono
        linearLayout.addView(editText);

        builder.setView(linearLayout);

        // add button in dialog to update -- botón agregar en el diálogo para actualizar
        builder.setPositiveButton("Update", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //input text from edit text -- ingresar texto desde editar texto
                final String value =editText.getText().toString().trim();
                //validate if user has entered something or not -- validar si el usuario ha ingresado algo o no
                if (!TextUtils.isEmpty(value)){
                    pd.show();
                    HashMap<String, Object> result = new HashMap<>();
                    result.put(key, value);

                    databaseReference.child(user.getUid()).updateChildren(result)
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    //updated, dismiss progress -- actualizada despedir progreso
                                    pd.dismiss();
                                    Toast.makeText(getActivity(), "Updated...", Toast.LENGTH_SHORT).show();

                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    //failed, dismiss progress, get and show error message -- falló, descartar progreso, obtener y mostrar mensaje de error
                                    pd.dismiss();
                                    Toast.makeText(getActivity(), ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });

                    //if user edit his name, also change it from hist posts
                    // si el usuario edita su nombre, también cámbielo de las publicaciones históricas
                    if (key.equals("name")) {
                        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Posts");
                        Query query = ref.orderByChild("uid").equalTo(uid);
                        query.addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                for (DataSnapshot ds: dataSnapshot.getChildren()) {
                                    String child = ds.getKey();
                                    dataSnapshot.getRef().child(child).child("uName").setValue(value);
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        });

                        //update name in current users comments on posts
                        // actualizar nombre en comentarios de usuarios actuales en publicaciones
                        ref.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                for (DataSnapshot ds: dataSnapshot.getChildren()) {
                                    String child = ds.getKey();
                                    if (dataSnapshot.child(child).hasChild("Comments")) {
                                        String child1 = ""+dataSnapshot.child(child).getKey();
                                        Query child2 = FirebaseDatabase.getInstance().getReference("Posts").child(child1).child("Comments").orderByChild("uid").equalTo(uid);
                                        child2.addValueEventListener(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                for (DataSnapshot ds: dataSnapshot.getChildren()) {
                                                    String child = ds.getKey();
                                                    dataSnapshot.getRef().child(child).child("uName").setValue(value);

                                                }
                                            }

                                            @Override
                                            public void onCancelled(@NonNull DatabaseError databaseError) {

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
                }
                else{
                    Toast.makeText(getActivity(), "Please enter "+ key, Toast.LENGTH_SHORT).show();

                }
            }
        });

        //add button in dialog to cancel -- botón agregar en el diálogo para cancelar
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        //create and show dialog -- crear y mostrar dialog
        builder.create().show();

    }

    private void showImagePicDialog() {
        //show dialog containig options camera and gallery to pick the image -- Mostrar cuadro de diálogo que contiene opciones de cámara y galería para elegir la imagen

        String options[] = {"Cámara", "Galería"};
        // alert dialog -- diálogo de alerta
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        //set title -- establecer título
        builder.setTitle("Pick Image From");
        //set items to dialog -- establecer elementos para dialogar
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //handle dialog item clics -- manejar clics de elementos de diálogo
                if (which == 0){
                    // camera clicked

                    if (!checkCameraPermission()){
                        requestCameraPermission();
                    }
                    else {
                        pickFromCamera();
                    }


                }else if (which == 1){
                    //gallery clicked

                    if (!checkStoragePermission()){
                        requestStoragePermission();
                    }
                    else {
                        pickFromGallery();
                    }

                }
            }
        });
        //create and show dialog -- crear y mostrar diálogo
        builder.create().show();

        /* for picking image from:
        * camera [ camera and storage permission required]
        * gallery [storage permission required]*/

        /* para elegir una imagen de:
        * cámara [se requiere permiso de cámara y almacenamiento]
        * galería [se requiere permiso de almacenamiento] */
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        /*this metod called when user press allow or Deny from permission request dialog
        * here we will handle permission cases (allowed & denied) */

        /*Este método se llama cuando el usuario presiona permitir o denegar desde el diálogo de solicitud de permiso
        * aquí manejaremos casos de permisos (permitidos y denegados)*/

        switch (requestCode){
            case CAMERA_REQUEST_CODE:{

                //picking from camera, frist check if camera and storage permissions allowed or not  -- seleccionando desde la cámara, primero verifique si los permisos de cámara y almacenamiento están permitidos o no
                if (grantResults.length >0) {
                    boolean cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean writeStorageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                    if (cameraAccepted && writeStorageAccepted){
                        //permissions enables -- permisos habilitados
                        pickFromCamera();
                    }
                    else {
                        //permissions denied --  permisos denegados
                        Toast.makeText(getActivity(), "Habilite la cámara y el permiso de almacenamiento", Toast.LENGTH_SHORT).show();
                    }
                }

            }
            break;
            case STORAGE_REQUEST_CODE:{

                //picking from gallery, frist check if storage permissions allowed or not  -- seleccionando desde la galeria, primero verifique si los permisos están permitidos o no
                if (grantResults.length >0) {
                    boolean writeStorageAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    if (writeStorageAccepted){
                        //permissions enables -- permisos habilitados
                        pickFromGallery();
                    }
                    else {
                        //permissions denied --  permisos denegados
                        Toast.makeText(getActivity(), "Habilite el permiso de almacenamiento", Toast.LENGTH_SHORT).show();
                    }
                }

            }
            break;
        }

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        /*This method will be called after picking image from Camera or Gallery*/
        /* Se llamará a este método después de seleccionar la imagen de la Cámara o Galería */
        if (resultCode == RESULT_OK) {

            if (requestCode == IMAGE_PICK_GALLERY_CODE){
                //image is picked from gallery, get ui of image
                //la imagen se selecciona de la galería, obtenga la interfaz de usuario de la imagen
                image_uri = data.getData();

                uploadProfileCoverPhoto(image_uri);
            }
            if (requestCode == IMAGE_PICK_CAMERA_CODE){
                //image is picked from camera, get ui of image
                //la imagen se selecciona de la camara, obtenga la interfaz de usuario de la imagen

                uploadProfileCoverPhoto(image_uri);
            }
        }


        super.onActivityResult(requestCode, resultCode, data);
    }

    private void uploadProfileCoverPhoto(final Uri uri) {
        //show progress  -- mostrar progreso
        pd.show();

        /*Instead of creating separate function for profile picture and cover photo
        * i'm doing work for both in same function
        *
        * to add check ill add a string variable and assing it value "image" when user clicks
        * "Edit profile pic", and assing it value "cover" when user clicks "Edit cover photo"
        * Here: image is the key in each user containig url of user's profile picture
        *       cover is athe key in each user containig url or user's cover photo */

        /* En lugar de crear una función separada para la foto de perfil y la foto de portada
         * Estoy trabajando para ambos en la misma función
         *
         * para agregar cheque, agregaré una variable de cadena y le asignaremos el valor "imagen" cuando el usuario haga clic
         * "Editar foto de perfil" y asignarle el valor "portada" cuando el usuario hace clic en "Editar foto de portada"
         * Aquí: la imagen es la clave en cada usuario que contiene la URL de la imagen de perfil del usuario
         * la portada es la clave en cada usuario que contiene una url o foto de portada del usuario */

         /*the parameter "image_uri" contains the uri of image picked either from camera or gallery
         * we will use UID of the currently signed in user as name of the images so there will be only one image
         * profile and one image for cover for each user*/

        /* usaremos el UID del usuario actualmente registrado como nombre de las imágenes, por lo que solo habrá una imagen
         * perfil y una imagen de portada para cada usuario */

        //path and name of image to be stored in firebase storage -- ruta y nombre de la imagen que se almacenará en el almacenamiento de Firebase
        //e.g. Users_profile_cover_imgs/image_e12f3456f789.jpg
        //e.g. Users_profile_cover_imgs/cover_c123n4567g89.jpg
        String filePathAndName = storagePath+ ""+ profileOrCoverPhoto + "_"+ user.getUid();

        StorageReference storageReference2nd = storageReference.child(filePathAndName);
        storageReference2nd.putFile(uri)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        //image is uploaded to storage, new get it's url and store in user's database
                        // la imagen se carga en el almacenamiento, se obtiene su URL y se almacena en la base de datos del usuario
                        Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                        while (!uriTask.isSuccessful());
                        final Uri downloadUri = uriTask.getResult();

                        //check if image is uploaded or not and url is received -- //compruebe si la imagen está cargada o no y se recibe la URL
                        if (uriTask.isSuccessful()){
                            //image uploaded -- imagen cargada
                            //add/update url in user's database -- agregar / actualizar url en la base de datos del usuario
                            HashMap<String, Object> results = new HashMap<>();
                            /*First parameter is  profileOrCoverPhoto that has value "image" or "cover"
                                which are keys in user's database where urlof image will be saved in one
                                of them
                                Second parameter contains the url of the image stored in firebase storade, this
                                url will be saved as value against key "image" or "cover*/
                            results.put(profileOrCoverPhoto, downloadUri.toString());

                            databaseReference.child(user.getUid()).updateChildren(results)
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            //url in database of user is added successfully -- url en la base de datos del usuario se agrega correctamente
                                            //dismiss progress bar --descartar la barra de progreso
                                            pd.dismiss();
                                            Toast.makeText(getActivity(), "Image Updated...", Toast.LENGTH_SHORT).show();

                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            //error adding url in database or user -- error al agregar la URL en la base de datos de usuario
                                            //dismiss progress bar --descartar la barra de progreso
                                            pd.dismiss();
                                            Toast.makeText(getActivity(), "Error Updating Image...", Toast.LENGTH_SHORT).show();

                                        }
                                    });

                            //if user edit his name, also change it from hist posts
                            // si el usuario edita su nombre, también cámbielo de las publicaciones históricas
                            if (profileOrCoverPhoto.equals("image")) {
                                DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Posts");
                                Query query = ref.orderByChild("uid").equalTo(uid);
                                query.addValueEventListener(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        for (DataSnapshot ds: dataSnapshot.getChildren()) {
                                            String child = ds.getKey();
                                            dataSnapshot.getRef().child(child).child("uDp").setValue(downloadUri.toString());
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                    }
                                });

                                //update user image in curent users comments on posts
                                // actualiza la imagen del usuario en los comentarios de los usuarios actuales sobre publicaciones
                                //update name in current users comments on posts
                                // actualizar nombre en comentarios de usuarios actuales en publicaciones
                                ref.addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        for (DataSnapshot ds: dataSnapshot.getChildren()) {
                                            String child = ds.getKey();
                                            if (dataSnapshot.child(child).hasChild("Comments")) {
                                                String child1 = ""+dataSnapshot.child(child).getKey();
                                                Query child2 = FirebaseDatabase.getInstance().getReference("Posts").child(child1).child("Comments").orderByChild("uid").equalTo(uid);
                                                child2.addValueEventListener(new ValueEventListener() {
                                                    @Override
                                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                        for (DataSnapshot ds: dataSnapshot.getChildren()) {
                                                            String child = ds.getKey();
                                                            dataSnapshot.getRef().child(child).child("uDp").setValue(downloadUri.toString());

                                                        }
                                                    }

                                                    @Override
                                                    public void onCancelled(@NonNull DatabaseError databaseError) {

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

                        }else {
                            // error
                            pd.dismiss();
                            Toast.makeText(getActivity(), "Some error occured", Toast.LENGTH_SHORT).show();

                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        //there were some error(s),get and show error message, dimiss progress dialog
                        //hubo algunos errores, obtener y mostrar mensaje de error, diálogo de progreso de dimiss
                        pd.dismiss();
                        Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });

    }

    private void pickFromCamera() {
        // intent of picking image from device camera -- intención de elegir la imagen de la cámara del dispositivo
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "Temp Pic");
        values.put(MediaStore.Images.Media.DESCRIPTION, "Temp Description");
        //put image uri -- poner imagen uri
        image_uri = getActivity().getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        // intent to start -- intención de iniciar la cámara
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, image_uri);
        startActivityForResult(cameraIntent, IMAGE_PICK_CAMERA_CODE);

    }

    private void pickFromGallery() {
        //pick from gallery -- elegir de la galeria
        Intent galleryIntent = new Intent(Intent.ACTION_PICK);
        galleryIntent.setType("image/*");
        startActivityForResult(galleryIntent, IMAGE_PICK_GALLERY_CODE);

    }

    private void checkUserStatus(){
        //get current user -- obtener usuario actual
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if(user != null){
            //user is signed in stay here -- El usuario ha iniciado sesión aquí.
            // set email of logged in user -- configurar el correo electrónico del usuario conectado
            //mProfileTv.setText(user.getEmail());
            uid = user.getUid();

        }else {
            //user not signed in, go to main activity -- usuario no ha iniciado sesión, vaya a la actividad principal
            startActivity(new Intent(getActivity(),  MainActivity.class));
            getActivity().finish();
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        setHasOptionsMenu(true);//to show menu options in fragment
        super.onCreate(savedInstanceState);
    }

    /*inflate options menu -- menú de opciones de inflado*/
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // inflating menu -- menú inflado
        inflater.inflate(R.menu.menu_main, menu);
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
                    searchMyPosts(s);

                }else {
                    //search text empty, get all users -- Buscar texto vacío, obtener todos los usuarios
                    loadMyPosts();
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                //called whenever user press any single letter
                //if search query is not empty the search
                if (!TextUtils.isEmpty(s.trim())) {
                    //search text contains text, search it
                    searchMyPosts(s);

                }else {
                    //search text empty, get all users
                    loadMyPosts();
                }
                return false;
            }
        });

        super.onCreateOptionsMenu(menu, inflater);
    }

    /*handle menu item clicks -- manejar clics de elementos de menú*/

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        //get item id
        int id = item.getItemId();
        if(id == R.id.action_logout){
            firebaseAuth.signOut();
            checkUserStatus();
        }
        else if(id == R.id.action_add_post){
            startActivity(new Intent(getActivity(), AddPostActivity.class));
        }
        else if (id==R.id.action_settings) {
            //go to SettingsActivity
            startActivity(new Intent(getActivity(), SettingsActivity.class));
        }

        return super.onOptionsItemSelected(item);

    }

}
