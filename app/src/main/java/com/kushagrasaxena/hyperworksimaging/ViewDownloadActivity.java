package com.kushagrasaxena.hyperworksimaging;

import android.os.AsyncTask;
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
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.firebase.ui.storage.images.FirebaseImageLoader;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;

import org.w3c.dom.Text;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;


public class ViewDownloadActivity extends AppCompatActivity implements View.OnClickListener {

    ProgressDialog pd;
    ConnectionDetector cd;
    private Button buttonFetch;
    private Button buttonDownload;
    private ImageView imageView;
    private EditText editTextName;
    private TextView textView;
    private ProgressDialog progress;
    private Bitmap bitmap;
    private Uri file;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_download);


        buttonFetch = (Button) findViewById(R.id.buttonFetch);
        buttonDownload = (Button) findViewById(R.id.buttonDownload);

        editTextName = (EditText) findViewById(R.id.editText);
        textView = (TextView) findViewById(R.id.textView);

        imageView = (ImageView) findViewById(R.id.imageView);

        buttonFetch.setOnClickListener(this);
        buttonDownload.setOnClickListener(this);
        cd = new ConnectionDetector(getApplicationContext());


    }

    @Override
    public void onClick(View view) {
        if (editTextName.getText().toString().trim().length() != 0) {
            if (cd.isConnectingToInternet()) {
                if (view == buttonFetch) {
                    pd = new ProgressDialog(this);
                    pd.setProgress(100);
                    ;
                    pd.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                    pd.setCancelable(false);
                    pd.setMessage("Fetching");
                    pd.show();
                    fetchImage();

                } else if (view == buttonDownload) {
                    pd = new ProgressDialog(this);
                    pd.setProgress(100);
                    ;
                    pd.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                    pd.setCancelable(false);
                    pd.setMessage("Downloading");
                    pd.show();
                    downloadImage();
                }
            } else {
                Toast.makeText(getApplicationContext(), "No Internet Access", Toast.LENGTH_LONG).show();
            }

        } else {
            Toast.makeText(getApplicationContext(), "Enter image name", Toast.LENGTH_LONG).show();
        }

    }

    private void downloadImage() {

        FirebaseStorage storage = FirebaseStorage.getInstance();
        // Create a storage reference from our app
        StorageReference storageRef = storage.getReferenceFromUrl("gs://myapplication-41f31.appspot.com");
        // gs://myapplication-41f31.appspot.com/images
        storageRef.child("images/" + editTextName.getText().toString().trim() + ".jpg").getBytes(Long.MAX_VALUE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
            @Override
            public void onSuccess(byte[] bytes) {
                // Use the bytes to display the image
                String path = Environment.getExternalStorageDirectory() + "/" + editTextName.getText().toString() + ".jpg";
                try {
                    FileOutputStream fos = new FileOutputStream(path);
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
                Toast.makeText(ViewDownloadActivity.this, exception.toString() + "!!!", Toast.LENGTH_SHORT).show();
            }
        });

    }

    private void fetchImage() {


        FirebaseStorage storage = FirebaseStorage.getInstance();


        String fileName = editTextName.getText().toString().trim() + ".jpg";


        // Create a storage reference from our app
        StorageReference storageRef1 = storage.getReference();

// Create a reference with an initial file path and name
        StorageReference pathReference = storageRef1.child("images/" + fileName);

        pathReference.getMetadata().addOnSuccessListener(new OnSuccessListener<StorageMetadata>() {
            @Override
            public void onSuccess(StorageMetadata storageMetadata) {
                // Metadata now contains the metadata for 'images/xyz.jpg'
                String latitude = storageMetadata.getCustomMetadata("latitude");
                String longitude = storageMetadata.getCustomMetadata("longitude");
                String label = storageMetadata.getCustomMetadata("label");
                textView.setText("label = " + label + "\n" + "latitude=" + latitude + ",longitude=" + longitude);


            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Uh-oh, an error occurred!
            }
        });

        Glide.with(ViewDownloadActivity.this /* context */)
                .using(new FirebaseImageLoader())
                .load(pathReference)
                .into(imageView);

        pd.dismiss();
    }

}
