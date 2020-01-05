package com.example.objectdetectedyazlab;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.entity.StringEntity;

public class MainActivity extends AppCompatActivity {
    private AsyncHttpClient asyncHttpClient;
    private byte[] byteArrayPhoto = null;
    private ImageView image;
    public EditText width, height;
    private static final int CAMERA_REQUEST = 1888;
    private static final int MY_CAMERA_PERMISSION_CODE = 100;
    private final String endpoint = "https://runner-server.herokuapp.com/image";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        image = findViewById(R.id.imageView);
        width = findViewById(R.id.width);
        height = findViewById(R.id.height);
    }

    public void sendImage(View view) {
        asyncHttpClient = new AsyncHttpClient();
        RequestParams requestParams = new RequestParams();
        if (byteArrayPhoto != null) {
            String encodedPhotoStr = Base64.encodeToString(byteArrayPhoto, Base64.DEFAULT);
            requestParams.add("imageData", encodedPhotoStr);
            asyncHttpClient.post(endpoint, requestParams, new AsyncHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                    String response = new String(responseBody);
                    Log.i("RESPINFO", response);
                    JSONObject jsonObject = null;
                    try {
                        jsonObject = new JSONObject(response);
                        String img = jsonObject.getString("image");
                        Log.i("IMAGE", img);
                        byte[] decodedString = Base64.decode(img, Base64.DEFAULT);
                        Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                        image.setImageBitmap(decodedByte);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {

                }
            });
        }
    }

    public void chooseImage(View view) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        try {
            startActivityForResult(
                    Intent.createChooser(intent, "Select a File to Upload"),
                    0);
        } catch (android.content.ActivityNotFoundException ex) {
            // Potentially direct the user to the Market with a Dialog
            Toast.makeText(this, "Please install a File Manager.",
                    Toast.LENGTH_SHORT).show();
        }
    }

    public void openCamera(View view) {
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, MY_CAMERA_PERMISSION_CODE);
        } else {
            Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
            startActivityForResult(cameraIntent, CAMERA_REQUEST);
        }


    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_CAMERA_PERMISSION_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "camera permission granted", Toast.LENGTH_LONG).show();
                Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(cameraIntent, CAMERA_REQUEST);
            } else {
                Toast.makeText(this, "camera permission denied", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case 0:
                if (resultCode == RESULT_OK) {
                    // Get the Uri of the selected file
                    Uri uri = data.getData();

                    image.setImageURI(uri);
                    try {
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                        ByteArrayOutputStream stream = new ByteArrayOutputStream();
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                        byteArrayPhoto = stream.toByteArray();
                        bitmap.recycle();
                        //Log.d("FILEBYTEARR", new String(byteArrayPhoto));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                break;
            case CAMERA_REQUEST:
                if (resultCode == Activity.RESULT_OK) {
                    Bitmap photo = (Bitmap) data.getExtras().get("data");
                    image.setImageBitmap(photo);
                    try {
                        String encodedImage = encodeImage(photo);
                        byteArrayPhoto = encodedImage.getBytes(StandardCharsets.UTF_8);
                        //Log.i("CAMERA STRING",byteArrayPhoto.toString());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private String encodeImage(Bitmap bm) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] b = baos.toByteArray();
        String encImage = Base64.encodeToString(b, Base64.DEFAULT);
        return encImage;
    }

    public void resizeImage(View view) {
        asyncHttpClient = new AsyncHttpClient();
        asyncHttpClient.setTimeout(1000 * 60);
        if (byteArrayPhoto != null) {
            String encodedPhotoStr = Base64.encodeToString(byteArrayPhoto, Base64.DEFAULT);
            JSONObject jsonParams = new JSONObject();
            try {
                jsonParams.put("resim", encodedPhotoStr);
                jsonParams.put("genislik", Integer.parseInt(width.getText().toString()));
                jsonParams.put("uzunluk", Integer.parseInt(height.getText().toString()));
                StringEntity entity = new StringEntity(jsonParams.toString());
                asyncHttpClient.post(getApplicationContext(), "https://mysterious-thicket-43904.herokuapp.com/scale", entity, "application/json", new AsyncHttpResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                        String response = new String(responseBody);
                        Log.i("RESULT", "RESIZED!");
                        Log.i("RESPINFO", response);
                        try {
                            byte[] decodedString = Base64.decode(response, Base64.DEFAULT);
                            Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                            image.setImageBitmap(decodedByte);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {

                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
