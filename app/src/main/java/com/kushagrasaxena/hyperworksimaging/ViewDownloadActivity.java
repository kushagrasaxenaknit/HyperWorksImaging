package com.kushagrasaxena.hyperworksimaging;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.firebase.ui.storage.images.FirebaseImageLoader;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;



public class ViewDownloadActivity extends AppCompatActivity implements View.OnClickListener {

    private Button buttonFetch;
    private Button buttonDownload;

    private ImageView imageView;

    private EditText editTextName;

    private Bitmap bitmap;

    ProgressDialog pd;
    private Uri file;
    ConnectionDetector cd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_download);


        buttonFetch = (Button) findViewById(R.id.buttonFetch);
        buttonDownload = (Button) findViewById(R.id.buttonDownload);

        editTextName = (EditText) findViewById(R.id.editText);

        imageView  = (ImageView) findViewById(R.id.imageView);

        buttonFetch.setOnClickListener(this);
        buttonDownload.setOnClickListener(this);
        cd = new ConnectionDetector(getApplicationContext());
    }

    @Override
    public void onClick(View view) {
        if(editTextName.getText().toString().trim().length()!=0) {
            if(cd.isConnectingToInternet()) {
                if(view == buttonFetch){
                    fetchImage();
                }else if(view == buttonDownload){
                    pd=new ProgressDialog(this);
                    pd.setProgress(100);;
                    pd.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                    pd.setCancelable(false);
                    pd.show();
                    downloadImage();
                }
            }

            else {
                Toast.makeText(getApplicationContext(), "No Internet Access", Toast.LENGTH_LONG).show();
            }

        }

        else {
            Toast.makeText(getApplicationContext(), "Enter image name", Toast.LENGTH_LONG).show();
        }

    }

    private void downloadImage() {

        FirebaseStorage storage=FirebaseStorage.getInstance();
        // Create a storage reference from our app
        StorageReference storageRef = storage.getReferenceFromUrl("gs://myapplication-41f31.appspot.com");
       // gs://myapplication-41f31.appspot.com/images
        storageRef.child("images/"+editTextName.getText().toString().trim()+".jpg").getBytes(Long.MAX_VALUE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
            @Override
            public void onSuccess(byte[] bytes) {
                // Use the bytes to display the image
                String path=Environment.getExternalStorageDirectory()+"/"+editTextName.getText().toString()+".jpg";
                try {
                    FileOutputStream fos=new FileOutputStream(path);
                    fos.write(bytes);
                    fos.close();
                    Toast.makeText(ViewDownloadActivity.this, "Success!!!", Toast.LENGTH_SHORT).show();

                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    Toast.makeText(ViewDownloadActivity.this, e.toString(), Toast.LENGTH_SHORT).show();
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(ViewDownloadActivity.this, e.toString(), Toast.LENGTH_SHORT).show();
                }
                pd.dismiss();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle any errors
                pd.dismiss();
                Toast.makeText(ViewDownloadActivity.this, exception.toString()+"!!!", Toast.LENGTH_SHORT).show();
            }
        });

    }

    private void fetchImage() {
        FirebaseStorage storage=FirebaseStorage.getInstance();

// Points to the root reference
        StorageReference storageRef = storage.getReferenceFromUrl("gs://myapplication-41f31.appspot.com");

        // Points to "images" Directory
        StorageReference imagesRef = storageRef.child("images");

        // Points to "images/space.jpg"
        // Note that you can use variables to create child values
        String fileName = "ditTextName.getText().toString().trim()"+".jpg";
        StorageReference spaceRef = imagesRef.child(fileName);

        // File path is "images/space.jpg"
        String path = spaceRef.getPath();

        // File name is "space.jpg"
        String name = spaceRef.getName();

        // Points to "images"
        imagesRef = spaceRef.getParent();

        Glide.with(ViewDownloadActivity.this /* context */)
                .using(new FirebaseImageLoader())
                .load(spaceRef)
                .into(imageView);
    }
}
