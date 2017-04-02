package com.kushagrasaxena.hyperworksimaging;

import android.app.ProgressDialog;
import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;




    import android.content.Intent;
    import android.graphics.Bitmap;
    import android.net.Uri;
    import android.os.AsyncTask;
    import android.os.Bundle;
    import android.support.v7.app.ActionBarActivity;
    import android.util.Log;
    import android.view.Menu;
    import android.view.MenuItem;
    import android.view.View;
    import android.widget.Button;
    import android.widget.EditText;
    import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.gson.Gson;
import com.kushagrasaxena.hyperworksimaging.helper.ImageHelper;
import com.kushagrasaxena.hyperworksimaging.helper.SelectImageActivity;
import com.microsoft.projectoxford.vision.VisionServiceClient;
    import com.microsoft.projectoxford.vision.VisionServiceRestClient;
    import com.microsoft.projectoxford.vision.contract.AnalysisResult;
    import com.microsoft.projectoxford.vision.contract.Category;
    import com.microsoft.projectoxford.vision.contract.Face;
    import com.microsoft.projectoxford.vision.contract.Tag;
    import com.microsoft.projectoxford.vision.contract.Caption;
    import com.microsoft.projectoxford.vision.rest.VisionServiceException;



import java.io.ByteArrayInputStream;
    import java.io.ByteArrayOutputStream;
    import java.io.IOException;
import java.net.InetAddress;
import java.util.UUID;

public class DescribeActivity extends AppCompatActivity {

        // Flag to indicate which task is to be performed.
        private static final int REQUEST_SELECT_IMAGE = 0;

        // The button to select an image
        private Button mButtonSelectImage ,getLabelButton,uploadButton;

        // The URI of the image selected to detect.
        private Uri mImageUri;

        // The image selected to detect.
        private Bitmap mBitmap;

        // The edit to show status and result.
        private EditText mEditText,Imagename;

        private VisionServiceClient client;

        private double latitude=0.0;
        private double longitude=0.0;
        private ProgressDialog progress;
        private String label="hello";
    private String name="";
//for firebase
    private StorageReference mStorageRef;
    // Connection detector class
    ConnectionDetector cd;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_describe);

            if (client==null){
                client = new VisionServiceRestClient(getString(R.string.subscription_key));
            }
            startService(new Intent(DescribeActivity.this, GPSTracker.class));
            mButtonSelectImage = (Button)findViewById(R.id.buttonSelectImage);
            getLabelButton=(Button)findViewById(R.id.label);
            uploadButton=(Button)findViewById(R.id.upload);
            mEditText = (EditText)findViewById(R.id.editTextResult);
            Imagename=(EditText)findViewById(R.id.nameImage);
            mStorageRef = FirebaseStorage.getInstance().getReference();
            cd = new ConnectionDetector(getApplicationContext());
        }



        public void doDescribe() {
            mButtonSelectImage.setEnabled(false);
            uploadButton.setEnabled(false);
            mEditText.setText("Describing...");
            progress = ProgressDialog.show(DescribeActivity.this, "Loading", "Getting label for image");
            try {
                new doRequest().execute();
            } catch (Exception e)
            {
                mEditText.setText("Error encountered. Exception is: " + e.toString());
            }
        }

        // Called when the "Select Image" button is clicked.
        public void selectImage(View view) {
            mEditText.setText("");
            getLabelButton.setEnabled(false);
            Intent intent;
            intent = new Intent(DescribeActivity.this, SelectImageActivity.class);
            startActivityForResult(intent, REQUEST_SELECT_IMAGE);
        }
        // Called when the "Upload Image" button is clicked.
        public void uploadImage(View view) {
            if(Imagename.getText().toString().trim().length()!=0) {
                if(cd.isConnectingToInternet()) {
                    uploadFile();
                }

                else {
                    Toast.makeText(getApplicationContext(), "No Internet Access", Toast.LENGTH_LONG).show();
                }
            }

            else {
                Toast.makeText(getApplicationContext(), "Enter image name", Toast.LENGTH_LONG).show();
            }

        }

        // Called when the "GET LABEL" button is clicked.
        public void getLabel(View view) {

            if(cd.isConnectingToInternet()) {
                doDescribe();
            }

            else {
                Toast.makeText(getApplicationContext(), "No Internet Access", Toast.LENGTH_LONG).show();
            }
        }
    // Called when the "Retrieve Image" button is clicked.
    public void downloadImage(View view) {

        if(cd.isConnectingToInternet()) {
            Intent intent = new Intent(this, ViewDownloadActivity.class);
            startActivity(intent);
        }

        else {
            Toast.makeText(getApplicationContext(), "No Internet Access", Toast.LENGTH_LONG).show();
        }
    }
        // Called when image selection is done.
        @Override
        protected void onActivityResult(int requestCode, int resultCode, Intent data) {
            Log.d("DescribeActivity", "onActivityResult");
            switch (requestCode) {
                case REQUEST_SELECT_IMAGE:
                    if(resultCode == RESULT_OK) {
                        // If image is selected successfully, set the image URI and bitmap.
                        mImageUri = data.getData();
                        latitude=data.getDoubleExtra("latitude",0.0);
                        longitude=data.getDoubleExtra("longitude",0.0);
                        Toast.makeText(getApplicationContext(), " Location is - \nLat: "
                                + latitude + "\nLong: " + longitude, Toast.LENGTH_LONG).show();
                        mBitmap = ImageHelper.loadSizeLimitedBitmapFromUri(
                                mImageUri, getContentResolver());
                        if (mBitmap != null) {
                            // Show the image on screen.
                            ImageView imageView = (ImageView) findViewById(R.id.selectedImage);
                            imageView.setImageBitmap(mBitmap);

                            // Add detection log.
                            Log.d("DescribeActivity", "Image: " + mImageUri + " resized to " + mBitmap.getWidth()
                                    + "x" + mBitmap.getHeight());

                          //  doDescribe();
                            getLabelButton.setEnabled(true);
                        }
                    }
                    break;
                default:
                    break;
            }
        }


        private String process() throws VisionServiceException, IOException {
            Gson gson = new Gson();

            // Put the image into an input stream for detection.
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            mBitmap.compress(Bitmap.CompressFormat.JPEG, 100, output);
            ByteArrayInputStream inputStream = new ByteArrayInputStream(output.toByteArray());

            AnalysisResult v = this.client.describe(inputStream, 1);

            String result = gson.toJson(v);
            Log.d("result", result);

            return result;
        }

        private class doRequest extends AsyncTask<String, String, String> {
            // Store error message
            private Exception e = null;

            public doRequest() {
            }

            @Override
            protected String doInBackground(String... args) {
                try {
                    return process();
                } catch (Exception e) {
                    this.e = e;    // Store error
                }

                return null;
            }

            @Override
            protected void onPostExecute(String data) {
                super.onPostExecute(data);
                // Display based on error existence
                if (progress != null)
                    progress.dismiss();
                mEditText.setText("");
                if (e != null) {
                    mEditText.setText("Error: " + e.getMessage());
                    this.e = null;
                } else {
                    Gson gson = new Gson();
                    AnalysisResult result = gson.fromJson(data, AnalysisResult.class);

                    for (Caption caption: result.description.captions) {
                        label=caption.text;
                        mEditText.append( caption.text  + "\n");
                    }
                   /* mEditText.append("Image format: " + result.metadata.format + "\n");
                    mEditText.append("Image width: " + result.metadata.width + ", height:" + result.metadata.height + "\n");
                    mEditText.append("\n");

                    for (Caption caption: result.description.captions) {
                        label=caption.text;
                        mEditText.append("Caption: " + caption.text + ", confidence: " + caption.confidence + "\n");
                    }
                    mEditText.append("\n");

                    for (String tag: result.description.tags) {
                        mEditText.append("Tag: " + tag + "\n");
                    }
                    mEditText.append("\n");

                    mEditText.append("\n--- Raw Data ---\n\n");
                    mEditText.append(data);
                    mEditText.setSelection(0);*/
                    label=mEditText.getText().toString();
                }

                mButtonSelectImage.setEnabled(true);
                uploadButton.setEnabled(true);
            }
        }
    //this method will upload the file
    private void uploadFile() {
        //if there is a file to upload
        Uri filePath= mImageUri;
        if (filePath != null) {
            //displaying a progress dialog while upload is going on
            final ProgressDialog progressDialog = new ProgressDialog(this);
            progressDialog.setTitle("Uploading");
            progressDialog.show();

String timestamp=Imagename.getText().toString().trim();
            StorageReference riversRef = mStorageRef.child("images/"+timestamp+".jpg");
            // Create file metadata including the content type
            StorageMetadata metadata = new StorageMetadata.Builder()
                    .setContentType("image/jpg")
                    .setCustomMetadata("latitude", latitude+"")
                    .setCustomMetadata("longitude", longitude+"")
                    .setCustomMetadata("label", label)
                    .build();


            riversRef.putFile(filePath,metadata)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            //if the upload is successfull
                            //hiding the progress dialog
                            progressDialog.dismiss();

                            //and displaying a success toast
                            Toast.makeText(getApplicationContext(), "File Uploaded ", Toast.LENGTH_LONG).show();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception exception) {
                            //if the upload is not successfull
                            //hiding the progress dialog
                            progressDialog.dismiss();

                            //and displaying error message
                            Toast.makeText(getApplicationContext(), exception.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    })
                    .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                            //calculating progress percentage
                            double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();

                            //displaying percentage in progress dialog
                            progressDialog.setMessage("Uploaded " + ((int) progress) + "%...");
                        }
                    });
        }
        //if there is not any file
        else {
            //you can display an error toast
            Toast.makeText(getApplicationContext(), "upload error ", Toast.LENGTH_LONG).show();
        }
    }


}

