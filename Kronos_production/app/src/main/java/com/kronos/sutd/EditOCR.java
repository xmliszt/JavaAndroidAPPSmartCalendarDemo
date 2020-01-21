package com.kronos.sutd;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.content.Context;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.common.AccountPicker;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.vision.v1.Vision;
import com.google.api.services.vision.v1.model.AnnotateImageRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesResponse;
import com.google.api.services.vision.v1.model.EntityAnnotation;
import com.google.api.services.vision.v1.model.Feature;
import com.google.api.services.vision.v1.model.Image;

import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;
import com.joestelmach.natty.DateGroup;
import com.joestelmach.natty.Parser;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.text.SimpleDateFormat;
import java.text.DateFormat;
import java.util.Locale;


public class EditOCR extends AppCompatActivity {
    private static final String TAG = "myDebug";
    static final int REQUEST_GALLERY_IMAGE = 100;
    static final int REQUEST_CODE_PICK_ACCOUNT = 101;
    static final int REQUEST_ACCOUNT_AUTHORIZATION = 102;
    static final int REQUEST_PERMISSIONS = 13;
    private Account mAccount;
    private ProgressDialog mProgressDialog;
    private static String accessToken;

    Button backBtn ;
    Button doneBtn ;
    CropImageView eventImg ;
    TextView testView;
    String image_path;
    String detectedTitle;
    String fromDate;
    String toDate;
    String fromTime;
    String toTime;
    Uri fileUri;
    String[] info = new String[2];


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_ocr);

        backBtn = findViewById(R.id.btnBack);
        doneBtn = findViewById(R.id.btnDone);
        eventImg = findViewById(R.id.imgEvent);
        testView = findViewById(R.id.testView);

        String imgPath = getIntent().getStringExtra("Image");
        //set selected image in imageView
        try {
            fileUri = Uri.fromFile(new File(imgPath));
            image_path = imgPath;
        } catch (Exception ecp){
            fileUri = Uri.parse(imgPath);
            image_path = new File(fileUri.getPath()).getPath();
        }
        Log.d("myDebug", "file URI: "+fileUri.toString());
        eventImg.setImageUriAsync(fileUri);

        // does OCR for overall image and stores string in info[0]
        FirebaseVisionImage image;
        try {
            image = FirebaseVisionImage.fromFilePath(EditOCR.this, fileUri);
            FirebaseVisionTextRecognizer detector =
                    FirebaseVision.getInstance().getOnDeviceTextRecognizer();

            detector.processImage(image)
                    .addOnSuccessListener(texts -> {
                        info[0] = processTextRecognitionResult(texts);
                        if (info[0].length() == 0){
                            fromDate = "";
                            testView.setText("Unable to identify date" + info[0]);
                        } else {
                            String dateTimetext = info[0];
                            String fromDate = getDateTime(dateTimetext).get(0);
                            testView.setText(fromDate);
                        }
                    })
                    .addOnFailureListener(e -> {
                        e.printStackTrace();
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }

        // request for account
        doneBtn.setOnClickListener((View v) -> {
            String dateTimetext = info[0];
            fromDate = getDateTime(dateTimetext).get(0);
            fromTime = getDateTime(dateTimetext).get(1);
            toDate = getDateTime(dateTimetext).get(2);
            toTime = getDateTime(dateTimetext).get(3);

            boolean connected = false;
            ConnectivityManager connectivityManager = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
            if(connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getState() == NetworkInfo.State.CONNECTED ||
                    connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState() == NetworkInfo.State.CONNECTED) {
                //we are connected to a network
                connected = true;
            }
            else {
                connected = false;
            }

            if (!connected) {
                Toast.makeText(EditOCR.this, "Please turn on your network in order to access the Google Cloud OCR feature!", Toast.LENGTH_LONG).show();
                Intent doneIntent = new Intent(EditOCR.this, Form.class);
                Bundle bundle = new Bundle();

                File photofile = new File(fileUri.toString());
                String idRaw = photofile.getName();
                String id = idRaw.substring(0, idRaw.lastIndexOf('.'));
                Log.d("myDebug", "SP id to be passed to Form page: "+id);

                bundle.putString("id", id);
                bundle.putString("title", "");
                bundle.putString("fromDate", fromDate);
                bundle.putString("fromTime", fromTime);
                bundle.putString("toDate", toDate);
                bundle.putString("toTime", toTime);
                bundle.putString("des", "");
                bundle.putString("loc", "");
                bundle.putString("imgPath", image_path);
                doneIntent.putExtras(bundle);
                startActivity(doneIntent);
            } else {
                ActivityCompat.requestPermissions(EditOCR.this, new String[]{Manifest.permission.GET_ACCOUNTS}, REQUEST_PERMISSIONS);
            }
        });

        backBtn.setOnClickListener((View v) ->{
            Intent goBack = new Intent(EditOCR.this, MainActivity.class);
            startActivity(goBack);
            finish();

        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_PERMISSIONS:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getAuthToken();
                } else {
                    Toast.makeText(this, "Permission Denied!", Toast.LENGTH_SHORT).show();
                }
        }
    }

    // method for getting text from OCR call
    private String processTextRecognitionResult(FirebaseVisionText texts) {
        String result = "";
        List<FirebaseVisionText.TextBlock> blocks = texts.getTextBlocks();
        if (blocks.size() == 0) {
            return result;
        }

        for (int i = 0; i < blocks.size(); i++) {
            List<FirebaseVisionText.Line> lines = blocks.get(i).getLines();
            for (int j = 0; j < lines.size(); j++) {
                List<FirebaseVisionText.Element> elements = lines.get(j).getElements();
                for (int k = 0; k < elements.size(); k++) {
                    result = result.concat(elements.get(k).getText()).concat(" ");
                }
            }
        }

        return result;
    }

    // method for parsing text
    public static ArrayList<String> getDateTime(String text){
        ArrayList<String> finalList = new ArrayList<>();
        List<DateGroup> dateGroups =new Parser().parse(text);
        String startDate = "";
        String endDate = "";
        String startTime = "";
        String endTime = "";
        String patternDate = "dd-MMM-yyyy EE";
        String patternTime = "HH:mm";
        DateFormat df1 = new SimpleDateFormat(patternDate);
        DateFormat df2 = new SimpleDateFormat(patternTime);
        Date eventStartDate;
        Date eventEndDate;

        if (dateGroups.size()==1){
            List<Date> dates = dateGroups.get(0).getDates();
            if (dates.size()==1){
                eventStartDate = dates.get(0);
                startTime = df2.format(eventStartDate);
                startDate = df1.format(eventStartDate);
            } else {
                eventStartDate = dates.get(0);
                startTime = df2.format(eventStartDate);
                startDate = df1.format(eventStartDate);

                eventEndDate = dates.get(1);
                endTime = df2.format(eventEndDate);
                endDate = df1.format(eventEndDate);
            }

        } else if (dateGroups.size()>=2) {
            eventStartDate = dateGroups.get(0).getDates().get(0);
            startTime = df2.format(eventStartDate);
            startDate = df1.format(eventStartDate);

            eventEndDate = dateGroups.get(1).getDates().get(0);
            endTime = df2.format(eventEndDate);
            endDate = df1.format(eventEndDate);
        }
        finalList.add(startDate);
        finalList.add(startTime);
        finalList.add(endDate);
        finalList.add(endTime);
        return finalList;

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_GALLERY_IMAGE && resultCode == RESULT_OK && data != null) {
            performCloudVisionRequest((Uri) data.getData());
        } else if (requestCode == REQUEST_CODE_PICK_ACCOUNT) {
            if (resultCode == RESULT_OK) {
                String email = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                AccountManager am = AccountManager.get(this);
                Account[] accounts = am.getAccountsByType(GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE);
                for (Account account : accounts) {
                    if (account.name.equals(email)) {
                        mAccount = account;
                        break;
                    }
                }
                getAuthToken();
            } else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, "No Account Selected", Toast.LENGTH_SHORT)
                        .show();
            }
        } else if (requestCode == REQUEST_ACCOUNT_AUTHORIZATION) {
            if (resultCode == RESULT_OK) {
                Bundle extra = data.getExtras();
                onTokenReceived(extra.getString("authtoken"));
            } else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, "Authorization Failed", Toast.LENGTH_SHORT)
                        .show();
            }
        }
    }

    public void performCloudVisionRequest(Uri uri) {
        if (uri != null) {
            try {
                Bitmap bitmap = resizeBitmap(
                        MediaStore.Images.Media.getBitmap(getContentResolver(), uri));
                callCloudVision(bitmap);
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            }
        }
    }

    Thread thread = new Thread();

    @SuppressLint("StaticFieldLeak")
    private void callCloudVision(final Bitmap bitmap) throws IOException {
//        mProgressDialog = ProgressDialog.show(this, null,"Asking best friend Google to help recognize the text...", true);

        new AsyncTask<Object, Void, BatchAnnotateImagesResponse>() {

//            @Override
//            protected void onPreExecute(){
//                mProgressDialog.setTitle("Google Cloud Vision");
//                mProgressDialog.setMessage("Calling best friend Google to recognize the text...");
//                mProgressDialog.show();
//            }

            @Override
            protected BatchAnnotateImagesResponse doInBackground(Object... params) {
                try {
                    GoogleCredential credential = new GoogleCredential().setAccessToken(accessToken);
                    HttpTransport httpTransport = AndroidHttp.newCompatibleTransport();
                    JsonFactory jsonFactory = GsonFactory.getDefaultInstance();

                    Vision.Builder builder = new Vision.Builder
                            (httpTransport, jsonFactory, credential);
                    Vision vision = builder.build();

                    List<Feature> featureList = new ArrayList<>();
                    Feature labelDetection = new Feature();
                    labelDetection.setType("LABEL_DETECTION");
                    labelDetection.setMaxResults(10);
                    featureList.add(labelDetection);

                    Feature textDetection = new Feature();
                    textDetection.setType("TEXT_DETECTION");
                    textDetection.setMaxResults(10);
                    featureList.add(textDetection);

                    List<AnnotateImageRequest> imageList = new ArrayList<>();
                    AnnotateImageRequest annotateImageRequest = new AnnotateImageRequest();
                    Image base64EncodedImage = getBase64EncodedJpeg(bitmap);
                    annotateImageRequest.setImage(base64EncodedImage);
                    annotateImageRequest.setFeatures(featureList);
                    imageList.add(annotateImageRequest);

                    BatchAnnotateImagesRequest batchAnnotateImagesRequest =
                            new BatchAnnotateImagesRequest();
                    batchAnnotateImagesRequest.setRequests(imageList);

                    Vision.Images.Annotate annotateRequest =
                            vision.images().annotate(batchAnnotateImagesRequest);
                    annotateRequest.setDisableGZipContent(true);
                    Log.d(TAG, "Sending request to Google Cloud");

                    BatchAnnotateImagesResponse response = annotateRequest.execute();
                    return response;

                } catch (GoogleJsonResponseException e) {
                    Log.e(TAG, "Request error: " + e.getContent());
                } catch (IOException e) {
                    Log.d(TAG, "Request error: " + e.getMessage());
                }
                return null;
            }

            protected void onPostExecute(BatchAnnotateImagesResponse response) {
//                mProgressDialog.dismiss();
                detectedTitle = getDetectedTexts(response);
                Intent doneIntent = new Intent(EditOCR.this, Form.class);
                Bundle bundle = new Bundle();

                File photofile = new File(fileUri.toString());
                String idRaw = photofile.getName();
                String id = idRaw.substring(0, idRaw.lastIndexOf('.'));
                Log.d("myDebug", "SP id to be passed to Form page: "+id);

                bundle.putString("id", id);
                bundle.putString("title", processDetectedText(detectedTitle));
                bundle.putString("fromDate", fromDate);
                bundle.putString("fromTime", fromTime);
                bundle.putString("toDate", toDate);
                bundle.putString("toTime", toTime);
                bundle.putString("des", "");
                bundle.putString("loc", "");
                bundle.putString("imgPath", image_path);
                Log.d("myDebug", "SP Image Path passed in: "+image_path);
                doneIntent.putExtras(bundle);
                startActivity(doneIntent);

            }

        }.execute();
    }

    private String getDetectedLabels(BatchAnnotateImagesResponse response){
        StringBuilder message = new StringBuilder("");
        List<EntityAnnotation> labels = response.getResponses().get(0).getLabelAnnotations();
        if (labels != null) {
            for (EntityAnnotation label : labels) {
                message.append(String.format(Locale.getDefault(), "%.3f: %s",
                        label.getScore(), label.getDescription()));
                message.append("\n");
            }
        } else {
            message.append("nothing\n");
        }

        return message.toString();
    }

    private String getDetectedTexts(BatchAnnotateImagesResponse response){
        StringBuilder message = new StringBuilder("");
        List<EntityAnnotation> texts = response.getResponses().get(0)
                .getTextAnnotations();
        if (texts != null) {
            for (EntityAnnotation text : texts) {
                message.append(String.format(Locale.getDefault(), "%s: %s",
                        text.getLocale(), text.getDescription()));
                message.append("\n");
            }
        } else {
            message.append("nothing\n");
        }

        return message.toString();
    }

    public Bitmap resizeBitmap(Bitmap bitmap) {

        int maxDimension = 1024;
        int originalWidth = bitmap.getWidth();
        int originalHeight = bitmap.getHeight();
        int resizedWidth = maxDimension;
        int resizedHeight = maxDimension;

        if (originalHeight > originalWidth) {
            resizedHeight = maxDimension;
            resizedWidth = (int) (resizedHeight * (float) originalWidth / (float) originalHeight);
        } else if (originalWidth > originalHeight) {
            resizedWidth = maxDimension;
            resizedHeight = (int) (resizedWidth * (float) originalHeight / (float) originalWidth);
        } else if (originalHeight == originalWidth) {
            resizedHeight = maxDimension;
            resizedWidth = maxDimension;
        }
        return Bitmap.createScaledBitmap(bitmap, resizedWidth, resizedHeight, false);
    }

    public Image getBase64EncodedJpeg(Bitmap bitmap) {
        Image image = new Image();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream);
        byte[] imageBytes = byteArrayOutputStream.toByteArray();
        image.encodeContent(imageBytes);
        return image;
    }

    private void pickUserAccount() {
        String[] accountTypes = new String[]{GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE};
        Intent intent = AccountPicker.newChooseAccountIntent(null, null,
                accountTypes, false, null, null, null, null);
        startActivityForResult(intent, REQUEST_CODE_PICK_ACCOUNT);
    }

    private void getAuthToken() {
        String SCOPE = "oauth2:https://www.googleapis.com/auth/cloud-platform";
        if (mAccount == null) {
            pickUserAccount();
        } else {
            new GetOAuthToken(EditOCR.this, mAccount, SCOPE, REQUEST_ACCOUNT_AUTHORIZATION)
                    .execute();
        }
    }

    public void onTokenReceived(String token){
        accessToken = token;
        Bitmap cropped = eventImg.getCroppedImage(500, 500);
        performCloudVisionRequest(getImageUri(EditOCR.this, cropped));
    }

    private Uri getImageUri(Context inContext, Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage, "Title", null);
        return Uri.parse(path);
    }

    private String processDetectedText(String t){
        String s1 = t.substring(3);
        String s2 = s1.replaceAll("\n", " ");
        String s3 = s2.substring(0, s2.indexOf("null"));
        Log.d("myDebug", "Final title I get: "+s3);
        return s3;
    }
}