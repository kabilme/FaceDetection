package com.kabil.facedetection;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;
import android.Manifest;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;


public class MainActivity extends AppCompatActivity {
    private static final int PICK_IMAGE_REQUEST = 1;

    private static final int REQUEST_STORAGE_PERMISSION = 1;
    Bitmap myBitmap;

    SparseArray<Face> faces;
    File pathToImageDirectory;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_STORAGE_PERMISSION);
        }
       createDCIMDir();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            try {
                myBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                // Do something with the bitmap object
                findFaces();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.exit:
                closeApp();
                break;
            case R.id.folder:
                onFolderPressed();
                break;
            case R.id.save:
                onSavePressed();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    protected void onFolderPressed() {

        try {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            startActivityForResult(intent, PICK_IMAGE_REQUEST);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void onSavePressed() {

        if (faces != null) {
            savePressed();
            // Display the number of faces detected
            Toast.makeText(getApplicationContext(),
                    "Number of faces Saved: " + faces.size(),
                    Toast.LENGTH_SHORT).show();
        }
    }
    protected void savePressed() {

        for (int i = 0; i < faces.size(); ++i) {
            Face face = faces.valueAt(i);
            // Crop the face from the original bitmap
            Bitmap faceBitmap = Bitmap.createBitmap(myBitmap,
                    (int) face.getPosition().x,
                    (int) face.getPosition().y,
                    (int) face.getWidth(),
                    (int) face.getHeight());

            // Save the Bitmap object as a JPG image file with a unique filename.
            String baseFilename = "face";
            String timestamp = Long.toString(System.currentTimeMillis());
            String filename = baseFilename + timestamp + ".jpg";
            try {
                File file = new File(pathToImageDirectory, filename);
                FileOutputStream fos = new FileOutputStream(file);
                faceBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                fos.flush();
                fos.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }
    protected void findFaces() {
        // Create a face detector
        FaceDetector detector = new FaceDetector.Builder(getApplicationContext())
                .setTrackingEnabled(false)
                .setLandmarkType(FaceDetector.ALL_LANDMARKS)
                .build();

        // Create a frame from the bitmap and run the face detector
        Frame frame = new Frame.Builder().setBitmap(myBitmap).build();
        faces = detector.detect(frame);

        // Display the number of faces detected
        Toast.makeText(getApplicationContext(),
                "Number of faces detected: " + faces.size(),
                Toast.LENGTH_SHORT).show();

        // Draw rectangles around each face
        Bitmap tempBitmap = Bitmap.createBitmap(myBitmap.getWidth(), myBitmap.getHeight(), Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(tempBitmap);
        canvas.drawBitmap(myBitmap, 0, 0, null);

        Paint paint = new Paint();
        paint.setColor(Color.GREEN);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5);

        for (int i = 0; i < faces.size(); ++i) {
            Face face = faces.valueAt(i);
            float x1 = face.getPosition().x;
            float y1 = face.getPosition().y;
            float x2 = x1 + face.getWidth();
            float y2 = y1 + face.getHeight();
            canvas.drawRect(x1, y1, x2, y2, paint);
        }

        ImageView imageView = findViewById(R.id.imageView);
        imageView.setImageDrawable(new BitmapDrawable(getResources(), tempBitmap));

        // Release the detector
        detector.release();
    }
    public void closeApp() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Are you sure you want to Exit ?").setCancelable(false).setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                finish();
            }
        }).setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
            }
        });
        AlertDialog alert = builder.create();
        alert.show();
    }
      private void createDCIMDir() {
        // Define the name of the new directory
        String newDirName = "FaceDetection";

        // Get the path to the DCIM directory
        String dcimPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath();

        // Create a File object for the new directory inside the DCIM directory
        pathToImageDirectory = new File(dcimPath + File.separator + newDirName);

        // Create the directory if it does not already exist
        if (!pathToImageDirectory.exists()) {
            boolean success = pathToImageDirectory.mkdirs();
            if (!success) {
                // Handle the error if the directory could not be created
                Toast.makeText(getApplicationContext(),
                        "Directory Not created: ",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }
    @Override
    public void onBackPressed() {
        closeApp();
    }
}

