package com.canibal.firebaseapp.adapters;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.canibal.firebaseapp.AddPostActivity;
import com.canibal.firebaseapp.PostDetailActivity;
import com.canibal.firebaseapp.PostLikedByActivity;
import com.canibal.firebaseapp.R;
import com.canibal.firebaseapp.ThereProfileActivity;
import com.canibal.firebaseapp.models.ModelPost;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class AdapterPosts extends RecyclerView.Adapter<AdapterPosts.MyHolder> {

    Context context;
    List<ModelPost>postList;

    String myUid;

    private DatabaseReference likesRef; // for likes database node
    private DatabaseReference postsRef; // reference of posts

    boolean mProcessLike = false;

    public AdapterPosts(Context context, List<ModelPost> postList) {
        this.context = context;
        this.postList = postList;
        myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        likesRef = FirebaseDatabase.getInstance().getReference().child("Likes");
        postsRef = FirebaseDatabase.getInstance().getReference().child("Posts");
    }

    @NonNull
    @Override
    public MyHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        //inflate layout row_post.xml
        View view = LayoutInflater.from(context).inflate(R.layout.row_posts, viewGroup, false);
        return new MyHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final MyHolder myHolder, final int i) {
        //get data
        //obtener datos
        final String uid = postList.get(i).getUid();
        String uEmail = postList.get(i).getuEmail();
        String uName = postList.get(i).getuName();
        String uDp = postList.get(i).getuDp();
        final String pId = postList.get(i).getpId();
        final String pTitle = postList.get(i).getpTitle();
        final String pDescription = postList.get(i).getpDescr();
        final String pImage = postList.get(i).getpImage();
        String pTimeStamp = postList.get(i).getpTime();
        String pLikes = postList.get(i).getpLikes(); //contenedor para el numero total de likes del post
        String pComments = postList.get(i).getpComments(); //contenedor para el numero total de likes del post

        //convert timestamp to dd/mm/yyy hh:mm am/pm
        //convierte la marca de tiempo a dd/mm/yyy hh:mm am/pm
        Calendar calendar = Calendar.getInstance(Locale.ENGLISH);
        calendar.setTimeInMillis(Long.parseLong(pTimeStamp));
        String pTime = DateFormat.format("dd/MM/yyyy hh:mm aa", calendar).toString();

        //set data
        myHolder.uNameTv.setText(uName);
        myHolder.pTimeTv.setText(pTime);
        myHolder.pTitleTv.setText(pTitle);
        myHolder.pDescriptionTv.setText(pDescription);
        myHolder.pLikesTv.setText(pLikes + "Likes"); //e.g. 100 likes
        myHolder.pCommentsTv.setText(pComments + "Comments"); //e.g. 100 likes
        //set likes for each post
        setLikes(myHolder, pId);

        //set user dp
        try {
            Picasso.get().load(uDp).placeholder(R.drawable.ic_default_img).into(myHolder.uPictureIv);
        }catch (Exception e){

        }

        //set post image
        //if there is no image i.e. pImage.equals("noImage") then hide ImageView
        if (pImage.equals("noImage")){
            //hide Image Biew
            myHolder.pImageIv.setVisibility(View.GONE);
        }
        else {
            //show Image Biew
            myHolder.pImageIv.setVisibility(View.VISIBLE);

            try {
                Picasso.get().load(pImage).into(myHolder.pImageIv);

            }catch (Exception e){

            }
        }


            //handle buttons clicks
        myHolder.moreBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showMoreOptions(myHolder.moreBtn, uid, myUid, pId, pImage);
            }
        });
        myHolder.likeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //get total number of likes for the post, whose like button clicked
                //if currently signed in user has not liket it before
                //increase value by 1, otherwise decrease value by 1

                // obtener el número total de Me gusta para la publicación, en cuyo botón Me gusta hizo clic
                // si el usuario que ha iniciado sesión actualmente no lo ha marcado antes
                // aumenta el valor en 1, de lo contrario disminuye el valor en 1
                final int pLikes = Integer.parseInt(postList.get(i).getpLikes());
                mProcessLike = true;
                //get id of the post clicked
                final String postIde = postList.get(i).getpId();
                likesRef.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (mProcessLike) {
                            if (dataSnapshot.child(postIde).hasChild(myUid)) {
                                //already liked, so remove like
                                postsRef.child(postIde).child("pLikes").setValue(""+(pLikes-1));
                                likesRef.child(postIde).child(myUid).removeValue();
                                mProcessLike = false;
                            }
                            else {
                                //no liked, like it
                                postsRef.child(postIde).child("pLikes").setValue(""+(pLikes+1));
                                likesRef.child(postIde).child(myUid).setValue("Liked"); //set any value
                                mProcessLike = false;

                                addToHisNotifications(""+uid, ""+pId, "Liked your post");
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
            }
        });
        myHolder.commentBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // start PostDetailActivity
                Intent intent = new Intent(context, PostDetailActivity.class);
                intent.putExtra("postId", pId); //will get detail of post using this id, its id of the post clicked
                context.startActivity(intent);


            }
        });
        myHolder.shareBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*some posts contains only text, and some contains image and text so, we will handle them both*/
                /* algunas publicaciones contienen solo texto, y algunas contienen imágenes y texto, así que las manejaremos ambas */
                //get image from imageView
                BitmapDrawable bitmapDrawable = (BitmapDrawable)myHolder.pImageIv.getDrawable();
                if (bitmapDrawable == null) {
                    //post without image
                    shareTextOnly(pTitle, pDescription);
                }
                else {
                    //post with image

                    //convert image to bitmap
                    Bitmap bitmap = bitmapDrawable.getBitmap();
                    shareImageAndText(pTitle, pDescription, bitmap);
                }
            }
        });
        myHolder.profileLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*click to go to ThereProfileActivity with uid, this uid is of clicked user
                * which will be used to show user specific data/post*/

                /* haga clic para ir a ThereProfileActivity con uid, este uid es de usuario cliqueado
                 * que se utilizará para mostrar datos / publicaciones específicos del usuario */

                Intent intent = new Intent(context, ThereProfileActivity.class);
                intent.putExtra("uid",uid);
                context.startActivity(intent);
            }
        });

        //click like count to start PostLikedByActivity, and pass the post id
        myHolder.pLikesTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, PostLikedByActivity.class);
                intent.putExtra("postId", pId);
                context.startActivity(intent);
            }
        });

    }


    private void addToHisNotifications(String hisUid, String pId, String notification){
        String pTimeStamp = ""+System.currentTimeMillis();

        HashMap<Object, String> hashMap = new HashMap<>();
        hashMap.put("pId", pId);
        hashMap.put("timestamp", pTimeStamp);
        hashMap.put("pUid", hisUid);
        hashMap.put("notification", notification);
        hashMap.put("sUid", myUid);

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(hisUid).child("Notifications").child(pTimeStamp).setValue(hashMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        //added successfully

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        //failed
                    }
                });
    }

    private void shareTextOnly(String pTitle, String pDescription) {
        //concatenate title and description to share
        String shareBody = pTitle + "\n" + pDescription;

        //share intent
        Intent sIntent = new Intent(Intent.ACTION_SEND);
        sIntent.setType("text/plain");
        sIntent.putExtra(Intent.EXTRA_SUBJECT, "Subject Here"); // en caso de que comparta a través de una aplicación de correo electrónico
        sIntent.putExtra(Intent.EXTRA_TEXT, shareBody); //texto para compartir
        context.startActivity(Intent.createChooser(sIntent, "Share Via")); //mensaje para mostrar en el diálogo de compartir


    }

    private void shareImageAndText(String pTitle, String pDescription, Bitmap bitmap) {
        //concatenate title and description to share
        String shareBody = pTitle + "\n" + pDescription;

        //first we will save this image in cache, get the saved image uri
        // primero guardaremos esta imagen en caché, obtendremos la uri de la imagen guardada
        Uri uri = saveImageToShare(bitmap);

        //share intent
        Intent sIntent = new Intent(Intent.ACTION_SEND);
        sIntent.putExtra(Intent.EXTRA_STREAM, uri);
        sIntent.putExtra(Intent.EXTRA_TEXT, shareBody);
        sIntent.putExtra(Intent.EXTRA_SUBJECT, "Subject Here");// en caso de que comparta a través de una aplicación de correo electrónico
        sIntent.setType("image/png");
        context.startActivity(Intent.createChooser(sIntent, "Share Via")); //mensaje para mostrar en el diálogo de compartir


    }

    private Uri saveImageToShare(Bitmap bitmap) {
        File imageFolder = new File(context.getCacheDir(), "images");
        Uri uri = null;
        try {
            imageFolder.mkdirs();//crear si no existe
            File file = new File(imageFolder, "shared_image.png");

            FileOutputStream stream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, stream);
            stream.flush();
            stream.close();
            uri = FileProvider.getUriForFile(context, "com.canibal.firebaseapp.fileprovider",
                    file);

        }
        catch (Exception e) {
            Toast.makeText(context, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        return uri;
    }

    //add a key named "pLikes" to each post and set its value to "0" manually in firebase
    // agrega una clave llamada "pLikes" a cada publicación y establece su valor en "0" manualmente en firebase

    private void setLikes(final MyHolder holder, final String postKey) {
        likesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.child(postKey).hasChild(myUid)) {
                    //user has liked this post
                    /*To indicate that the post is liked by this (SignedIn) user
                    * change drawable left icon of the button
                    *change text of like button from "like" to "liked" */

                    // al usuario le ha gustado esta publicación
                    /* Para indicar que la publicación le gusta a este usuario (SignedIn)
                     * Cambiar el icono izquierdo dibujable del botón
                     * cambia el texto del botón Me gusta de "Me gusta" a "Me gusta" */
                    holder.likeBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_liked, 0,0,0);
                    holder.likeBtn.setText("Liked");

                }
                else {
                    //user has not liked this post
                    /*To indicate that the post is not liked by this (SignedIn) user
                     * change drawable left icon of the button
                     *change text of like button from "liked" to "liked" */

                    // al usuario no le ha gustado esta publicación
                    /* Para indicar que la publicación no le gusta a este usuario (SignedIn)
                     * Cambiar el icono izquierdo dibujable del botón
                     * cambia el texto del botón Me gusta de "Me gusta" a "Me gusta" */
                    holder.likeBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_like_black, 0,0,0);
                    holder.likeBtn.setText("Like");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    private void showMoreOptions(ImageButton moreBtn, String uid, String myUid, final String pId, final String pImage) {
        //creating popup menu currently habing option delete, we will add more options later
        // creando un menú emergente que actualmente tiene la opción eliminar, agregaremos más opciones más tarde
        PopupMenu popupMenu = new PopupMenu(context, moreBtn, Gravity.END);

        //show delete option in only post(s) of currently signed-in user
        // muestra la opción de eliminación solo en las publicaciones del usuario que ha iniciado sesión actualmente
        if (uid.equals(myUid)){
            //add  items in menu
            popupMenu.getMenu().add(Menu.NONE, 0,  0, "Delete");
            popupMenu.getMenu().add(Menu.NONE, 1,  0, "Edit");
        }
        popupMenu.getMenu().add(Menu.NONE, 2, 0, "View Detail");


        //item click listener
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                int id = menuItem.getItemId();
                if (id == 0) {
                    //delete is clicked
                    beginDelete(pId, pImage);
                }
                else if (id == 1) {
                    //edit is clicked
                    //start AddPostActivity with key "editpost" and the id of the post clicked
                    // inicie AddPostActivity con la clave "editpost" y haga clic en la identificación de la publicación
                    Intent intent = new Intent(context, AddPostActivity.class);
                    intent.putExtra("key", "editPost");
                    intent.putExtra("editPostId", pId);
                    context.startActivity(intent);
                }
                else if (id == 2) {
                    // start PostDetailActivity
                    Intent intent = new Intent(context, PostDetailActivity.class);
                    intent.putExtra("postId", pId); //will get detail of post using this id, its id of the post clicked
                    context.startActivity(intent);
                }


                return false;
            }
        });
        //show menu
        popupMenu.show();

    }

    private void beginDelete(String pId, String pImage) {
        //post can be with or without image
        // la publicación puede ser con o sin imagen

        if (pImage.equals("noImage")) {
            //post is without image
            // la publicación no tiene imagen
            deleteWithoutImage(pId);

        }
        else {
            //post is with image
            //la publicacion tiene imagen
            deleteWithImage(pId, pImage);

        }
    }

    private void deleteWithImage(final String pId, String pImage) {
        //progres bar
        final ProgressDialog pd = new ProgressDialog(context);
        pd.setMessage("Deleting...");

        /*Steps:
        * 1) Delete image using url
        * 2)Delete from database using post id*/

        /*Pasos:
        * 1) Eliminar imagen usando url
        * 2) Eliminar de la base de datos usando el ID de publicación */

        StorageReference picRef = FirebaseStorage.getInstance().getReferenceFromUrl(pImage);
        picRef.delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        //image delete, now delete database
                        // eliminar imagen, ahora eliminar base de datos

                        Query fquery = FirebaseDatabase.getInstance().getReference("Posts").orderByChild("pId").equalTo(pId);
                        fquery.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                for (DataSnapshot ds: dataSnapshot.getChildren()) {
                                    ds.getRef().removeValue(); //remove values from firebase where pic matches -- elimina valores de firebase donde coincide la imagen
                                }
                                //deleted
                                Toast.makeText(context, "Deleted successfully", Toast.LENGTH_SHORT).show();
                                pd.dismiss();
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        });

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        //failed, can't go further
                        // falló, no puedo ir más allá
                        pd.dismiss();
                        Toast.makeText(context, ""+e.getMessage(), Toast.LENGTH_SHORT).show();

                    }
                });
    }

    private void deleteWithoutImage(String pId) {
        final ProgressDialog pd = new ProgressDialog(context);
        pd.setMessage("Deleting...");

        Query fquery = FirebaseDatabase.getInstance().getReference("Posts").orderByChild("pId").equalTo(pId);
        fquery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot ds: dataSnapshot.getChildren()) {
                    ds.getRef().removeValue(); //remove values from firebase where pic matches -- elimina valores de firebase donde coincide la imagen
                }
                //deleted
                Toast.makeText(context, "Deleted successfully", Toast.LENGTH_SHORT).show();
                pd.dismiss();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    // view holder class
    class MyHolder extends RecyclerView.ViewHolder{

        //views from row_post.xml
        ImageView uPictureIv, pImageIv;
        TextView uNameTv, pTimeTv, pTitleTv, pDescriptionTv, pLikesTv, pCommentsTv;
        ImageButton moreBtn;
        Button likeBtn, commentBtn, shareBtn;
        LinearLayout profileLayout;

        public MyHolder(@NonNull View itemView) {
            super(itemView);

            //init views
            uPictureIv = itemView.findViewById(R.id.uPictureIv);
            pImageIv = itemView.findViewById(R.id.pImageIv);
            uNameTv = itemView.findViewById(R.id.uNameTv);
            pTimeTv = itemView.findViewById(R.id.pTimeTv);
            pTitleTv = itemView.findViewById(R.id.pTitleTv);
            pDescriptionTv = itemView.findViewById(R.id.pDescriptionTv);
            pLikesTv = itemView.findViewById(R.id.pLikesTv);
            pCommentsTv = itemView.findViewById(R.id.pCommentsTv);
            moreBtn = itemView.findViewById(R.id.moreBtn);
            likeBtn = itemView.findViewById(R.id.likeBtn);
            commentBtn = itemView.findViewById(R.id.commentBtn);
            shareBtn = itemView.findViewById(R.id.shareBtn);
            profileLayout = itemView.findViewById(R.id.profileLayout);
        }
    }
}
