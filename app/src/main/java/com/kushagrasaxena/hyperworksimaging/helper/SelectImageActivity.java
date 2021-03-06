package com.kushagrasaxena.hyperworksimaging.helper;

import android.content.Context;
import android.location.LocationManager;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;


        import android.content.Intent;
        import android.net.Uri;
        import android.os.Bundle;
        import android.os.Environment;
        import android.provider.MediaStore;
        import android.support.annotation.NonNull;
        import android.support.v4.content.FileProvider;
        import android.support.v7.app.ActionBarActivity;
        import android.view.View;
        import android.widget.TextView;
import android.widget.Toast;


import com.kushagrasaxena.hyperworksimaging.GPSTracker;
import com.kushagrasaxena.hyperworksimaging.R;

import java.io.File;
        import java.io.IOException;

// The activity for the user to select a image and to detect faces in the image.
public class SelectImageActivity extends ActionBarActivity {
    // Flag to indicate the request of the next task to be performed
    private static final int REQUEST_TAKE_PHOTO = 0;
    private static final int REQUEST_SELECT_IMAGE_IN_ALBUM = 1;

    // The URI of photo taken from gallery
    private Uri mUriPhotoTaken;

    // File of the photo taken with camera
    private File mFilePhotoTaken;
// taking location of image taken from camera
    double latitude=0.0;
    double longitude=0.0;
    GPSTracker gps;
    // When the activity is created, set all the member variables to initial state.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_image);
    }

    // Save the activity state when it's going to stop.
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable("ImageUri", mUriPhotoTaken);
    }

    // Recover the saved state when the activity is recreated.
    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mUriPhotoTaken = savedInstanceState.getParcelable("ImageUri");
    }

    // Deal with the result of selection of the photos and faces.
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_TAKE_PHOTO:

                if (resultCode == RESULT_OK) {

                    gps = new GPSTracker(SelectImageActivity.this);
                    if(gps.canGetLocation()&&isGpsEnabled())
                    {
                        latitude = gps.getLatitude();
                        longitude = gps.getLongitude();

                        // \n is for new line
                      //  Toast.makeText(getApplicationContext(), "Your Location is - \nLat: "
                          //      + latitude + "\nLong: " + longitude, Toast.LENGTH_LONG).show();
                    }
                    if(mFilePhotoTaken!=null) {
                        Intent intent = new Intent();

                        intent.setData(Uri.fromFile(mFilePhotoTaken));
                        intent.putExtra("latitude", latitude);
                        intent.putExtra("longitude", longitude);
                        setResult(RESULT_OK, intent);
                    }
                    else {
                        Toast.makeText(getApplicationContext(), "Retake Picture", Toast.LENGTH_LONG).show();
                    }
                    finish();
                }
                break;
            case REQUEST_SELECT_IMAGE_IN_ALBUM:
                if (resultCode == RESULT_OK) {
                    Uri imageUri;
                    if (data == null || data.getData() == null) {
                        imageUri = mUriPhotoTaken;
                    } else {
                        imageUri = data.getData();
                    }
                    Intent intent = new Intent();
                    intent.setData(imageUri);
                    //we don't know location of previously stored photo
                    intent.putExtra("latitude","Not Available");
                    intent.putExtra("longitude","Not Available");
                    setResult(RESULT_OK, intent);
                    finish();
                }

                break;
            default:
                break;
        }
    }

    // When the button of "Take a Photo with Camera" is pressed.
    public void takePhoto(View view) {
        //ask for gps inorder to get gps location
        if(!isGpsEnabled())
        {
            turnGPSOn();
        }
        if(isGpsEnabled())
        {




            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if(intent.resolveActivity(getPackageManager()) != null) {
                // Save the photo taken to a temporary file.
                File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                try {
                    mFilePhotoTaken = File.createTempFile(
                            "IMG_",  /* prefix */
                            ".jpg",         /* suffix */
                            storageDir      /* directory */
                    );

                    // Create the File where the photo should go
                    // Continue only if the File was successfully created
                    if (mFilePhotoTaken != null) {
                        mUriPhotoTaken = FileProvider.getUriForFile(this,
                                "com.kushagrasaxena.hyperworksimaging.fileprovider",
                                mFilePhotoTaken);
                        intent.putExtra(MediaStore.EXTRA_OUTPUT, mUriPhotoTaken);

                        // Finally start camera activity
                        startActivityForResult(intent, REQUEST_TAKE_PHOTO);
                    }
                } catch (IOException e) {
                    setInfo(e.getMessage());
                }
            }
        }

    }

    // When the button of "Select a Photo in Album" is pressed.
    public void selectImageInAlbum(View view) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("mFilePhotoTaken/*");
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, REQUEST_SELECT_IMAGE_IN_ALBUM);
        }
    }

    // Set the information panel on screen.
    private void setInfo(String info) {
        TextView textView = (TextView) findViewById(R.id.info);
        textView.setText(info);
    }
    private void turnGPSOn(){
        Toast.makeText(getApplicationContext(), "Turn Gps on to get location ", Toast.LENGTH_LONG).show();
       Intent intent1 = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        startActivity(intent1);


    }
    public boolean isGpsEnabled(){

       LocationManager locationManager = (LocationManager)getApplicationContext().getSystemService(Context.LOCATION_SERVICE);

       boolean GpsStatus = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
return GpsStatus;

    }

}
