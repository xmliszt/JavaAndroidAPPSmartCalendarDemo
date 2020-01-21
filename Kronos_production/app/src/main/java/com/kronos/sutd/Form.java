package com.kronos.sutd;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.joestelmach.natty.Parser;

import java.text.SimpleDateFormat;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Form extends AppCompatActivity {
    private static final int REQUEST_PICTURE_CAPTURE = 101;
    private photoCaptured pc = photoCaptured.getInstance();
    private Button saveButton;
    private Button confirmButton;
    private EditText entryTitle;
    private EditText locationBox;
    private EditText descriptionBox;
    private ImageView thumbnailImg;
    private Button fromDateBtn;
    private Button fromTimeBtn;
    private Button toDateBtn;
    private Button toTimeBtn;
    private ImageButton shareBtn;
    private String spid;

    private boolean hasPhoto;
    private boolean canSubmit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_form);
        canSubmit = true;

        // get this bundle from either ViewEntry or EditOCR
        Bundle extras = getIntent().getExtras();
        spid = (String) extras.get("id");
        String title = (String) extras.get("title");
        String fromDate = (String) extras.get("fromDate");
        String fromTime = (String) extras.get("fromTime");
        String toDate = (String) extras.get("toDate");
        String toTime = (String) extras.get("toTime");
        String des = (String) extras.get("description");
        String loc = (String) extras.get("location");
        String imgPath = (String) extras.get("imgPath");

        GalleryUtils galleryUtils = new GalleryUtils(Form.this);
        final Calendar cldr = Calendar.getInstance();
        final int year = cldr.get(Calendar.YEAR);
        final int month = cldr.get(Calendar.MONTH);
        final int dayNum = cldr.get(Calendar.DAY_OF_MONTH);
        final int day = cldr.get(Calendar.DAY_OF_WEEK);
        final int hour = cldr.get(Calendar.HOUR_OF_DAY);
        final int min = cldr.get(Calendar.MINUTE);

        saveButton = (Button) findViewById(R.id.saveBtn);
        confirmButton = (Button) findViewById(R.id.addCalendar);
        entryTitle = (EditText) findViewById(R.id.entryTitle);
        locationBox = (EditText) findViewById(R.id.locationBox);
        descriptionBox = (EditText) findViewById(R.id.descriptionBox);
        thumbnailImg = (ImageView) findViewById(R.id.thumbnail);
        fromDateBtn = (Button) findViewById(R.id.fromDateBtn);
        toDateBtn = (Button) findViewById(R.id.toDateBtn);
        fromTimeBtn = (Button) findViewById(R.id.fromTimeBtn);
        toTimeBtn = (Button) findViewById(R.id.toTimeBtn);
        shareBtn = (ImageButton) findViewById(R.id.shareButton);

        entryTitle.setText(title);

        Log.d("myDebug", String.format("fromDate: %s", fromDate));

        // if date time not existing, set current date and time dd:MMM:yyyy EE

        if (fromDate.length() == 0) {
            fromDateBtn.setText(dateBuilder(year, month, dayNum, day));
        } else { fromDateBtn.setText(fromDate); }

        if (toDate.length() == 0) {
            toDateBtn.setText(dateBuilder(year, month, dayNum, day));
        } else { toDateBtn.setText(toDate); }

        if (fromTime.length() == 0) {
            fromTimeBtn.setText(timeBuilder(hour, min));
        } else { fromTimeBtn.setText(fromTime);}

        if (toTime.length() == 0) {
            toTimeBtn.setText(timeBuilder(hour, min));
        } else { toTimeBtn.setText(toTime);}

        if (des != null){
            if (des.length() != 0) {descriptionBox.setSingleLine(false); descriptionBox.setText(des);}
        }

        if (loc != null){
            if (loc.length() != 0) {locationBox.setSingleLine(false); locationBox.setText(loc);}
        }

        Log.d("myDebug", "Image Path: "+imgPath+"Length of imgPath"+imgPath.length());
        // get the image from imgPath and convert to Bitmap
        if (imgPath.length() != 0){
            Bitmap bitmap = BitmapFactory.decodeFile(imgPath);
            try {
                hasPhoto = true;
                Bitmap processedBitmap = pc.processThumbnail(bitmap, imgPath);
                thumbnailImg.setImageBitmap(processedBitmap);
            } catch (IOException ioex){
                Toast.makeText(Form.this, ioex.getMessage(), Toast.LENGTH_SHORT).show();
                hasPhoto = false;
                thumbnailImg.setImageResource(R.drawable.placeholder);
            }
        } else if (photoCaptured.getInstance().getImgPath() != null && imgPath.length() == 0){
            try {
                hasPhoto = true;
                String ip = photoCaptured.getInstance().getImgPath();
                Bitmap processedBitmap = pc.processThumbnail(BitmapFactory.decodeFile(ip), ip);
                thumbnailImg.setImageBitmap(processedBitmap);
            } catch (IOException ioex){
                Toast.makeText(Form.this, ioex.getMessage(), Toast.LENGTH_SHORT).show();
                hasPhoto = false;
                thumbnailImg.setImageResource(R.drawable.placeholder);
            }
        } else if (photoCaptured.getInstance().getImgPath() != null && imgPath.length() != 0){
            try {
                hasPhoto = true;
                String ip = photoCaptured.getInstance().getImgPath();
                Bitmap processedBitmap = pc.processThumbnail(BitmapFactory.decodeFile(ip), ip);
                thumbnailImg.setImageBitmap(processedBitmap);
            } catch (IOException ioex){
                Toast.makeText(Form.this, ioex.getMessage(), Toast.LENGTH_SHORT).show();
                hasPhoto = false;
                thumbnailImg.setImageResource(R.drawable.placeholder);
            }
        }
        else {
            hasPhoto = false;
            thumbnailImg.setImageResource(R.drawable.placeholder);
        }
        // end of retrieving info

        //save info -> direct to HOME
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String xtitle = entryTitle.getText().toString();
                final String xloc = locationBox.getText().toString();
                final String xdes = descriptionBox.getText().toString();
                final String xfromDate = fromDateBtn.getText().toString();
                final String xtoDate = toDateBtn.getText().toString();
                final String xfromTime = fromTimeBtn.getText().toString();
                final String xtoTime = toTimeBtn.getText().toString();
                final String ximgPath = pc.getImgPath();
                if (xtitle.length() == 0){
                    Toast.makeText(Form.this, "Event title cannot be blanked!", Toast.LENGTH_SHORT).show();
                } else if (!canSubmit){
                    Toast.makeText(Form.this, "There is error in your dates!", Toast.LENGTH_SHORT).show();
                }
                else {
                    if (spid.length() == 0){
                        String crrtDateTime = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                        spid = "KRONOS_"+crrtDateTime;
                    }
                    saveSP(spid, xtitle, xfromDate, xtoDate, xfromTime, xtoTime, xdes, xloc, ximgPath,true);
                    Intent direct = new Intent(Form.this, MainActivity.class);
                    startActivity(direct);
                }
            }
        });

        //confirm info -> direct to HOME
        confirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String xtitle = entryTitle.getText().toString();
                final String xloc = locationBox.getText().toString();
                final String xdes = descriptionBox.getText().toString();
                final String xfromDate = fromDateBtn.getText().toString();
                final String xtoDate = toDateBtn.getText().toString();
                final String xfromTime = fromTimeBtn.getText().toString();
                final String xtoTime = toTimeBtn.getText().toString();
                final String ximgPath = pc.getImgPath();
                if (xtitle.length() == 0 ){
                    Toast.makeText(Form.this, "Event title cannot be blanked!", Toast.LENGTH_SHORT).show();
                } else if (!canSubmit){
                    Toast.makeText(Form.this, "There is error in your dates!", Toast.LENGTH_SHORT).show();
                }
                else {
                    if (spid.length() == 0){
                        spid = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                    }
                    saveSP(spid, xtitle, xfromDate, xtoDate, xfromTime, xtoTime, xdes, xloc, ximgPath, false);
                    Intent direct = new Intent(Form.this, MainActivity.class);
                    startActivity(direct);
                }
            }
        });

        //set from date
        fromDateBtn.setOnClickListener((View v) -> {
            String date = fromDateBtn.getText().toString();
            List<Date> parseDate = new Parser().parse(date).get(0).getDates();
            Date parsedDate = parseDate.get(0);
            cldr.setTime(parsedDate);
            int y = cldr.get(Calendar.YEAR);
            int m = cldr.get(Calendar.MONTH);
            int d = cldr.get(Calendar.DAY_OF_MONTH);
            final DatePickerDialog picker = new DatePickerDialog(this,
                    new DatePickerDialog.OnDateSetListener() {
                        @Override
                        public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                            Date date = new Date(year-1900, month, dayOfMonth);
                            String dayStr = new SimpleDateFormat("EE", Locale.ENGLISH).format(date);
                            String monthStr = new SimpleDateFormat("MMM", Locale.ENGLISH).format(date);
                            fromDateBtn.setText(String.format("%02d-%s-%04d %s", dayOfMonth, monthStr, year, dayStr));
                            String fromDateString = fromDateBtn.getText().toString();
                            String toDateString = toDateBtn.getText().toString();
                            try {
                                Date fromDate = new SimpleDateFormat("dd-MMM-yyyy EE").parse(fromDateString);
                                Date toDate = new SimpleDateFormat("dd-MMM-yyyy EE").parse(toDateString);
                                if (isBefore(toDate, fromDate)){
                                    canSubmit = false;
                                    Toast.makeText(Form.this, "To Date cannot be before From Date!", Toast.LENGTH_LONG).show();
                                    toDateBtn.setTextColor(getResources().getColor(R.color.error));
                                } else {
                                    canSubmit = true;
                                    toDateBtn.setTextColor(getResources().getColor(R.color.black));
                                }
                            } catch (ParseException parEx){
                                Toast.makeText(Form.this, "Unable to parse the date string into Date object. Fail to compare...", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }, y, m, d);
            picker.show();
        });

        //set from time
        fromTimeBtn.setOnClickListener((View v) -> {
            galleryUtils.saveTime(fromTimeBtn);
        });

        //set to date
        toDateBtn.setOnClickListener((View v) -> {
//            Calendar cldr = Calendar.getInstance();
//            int year = cldr.get(Calendar.YEAR);
//            int month = cldr.get(Calendar.MONTH);
//            int dayNum = cldr.get(Calendar.DAY_OF_MONTH);
            String date = toDateBtn.getText().toString();
            List<Date> parseDate = new Parser().parse(date).get(0).getDates();
            Date parsedDate = parseDate.get(0);
            cldr.setTime(parsedDate);
            int y = cldr.get(Calendar.YEAR);
            int m = cldr.get(Calendar.MONTH);
            int d = cldr.get(Calendar.DAY_OF_MONTH);
            final DatePickerDialog picker = new DatePickerDialog(this,
                    new DatePickerDialog.OnDateSetListener() {
                        @Override
                        public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                            Date date = new Date(year-1900, month, dayOfMonth);
                            String dayStr = new SimpleDateFormat("EE", Locale.ENGLISH).format(date);
                            String monthStr = new SimpleDateFormat("MMM", Locale.ENGLISH).format(date);
                            toDateBtn.setText(String.format("%02d-%s-%04d %s", dayOfMonth, monthStr, year, dayStr));
                            String fromDateString = fromDateBtn.getText().toString();
                            String toDateString = toDateBtn.getText().toString();
                            try {
                                Date fromDate = new SimpleDateFormat("dd-MMM-yyyy EE").parse(fromDateString);
                                Date toDate = new SimpleDateFormat("dd-MMM-yyyy EE").parse(toDateString);
                                if (isBefore(toDate, fromDate)){
                                    canSubmit = false;
                                    Toast.makeText(Form.this, "To Date cannot be before From Date!", Toast.LENGTH_LONG).show();
                                    toDateBtn.setTextColor(getResources().getColor(R.color.error));
                                } else {
                                    canSubmit = true;
                                    toDateBtn.setTextColor(getResources().getColor(R.color.black));
                                }
                            } catch (ParseException parEx){
                                Toast.makeText(Form.this, "Unable to parse the date string into Date object. Fail to compare...", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }, y, m, d);
            picker.show();
        });

        //set to time
        toTimeBtn.setOnClickListener((View v) -> {
            galleryUtils.saveTime(toTimeBtn);
        });

        shareBtn.setOnClickListener((View v) -> {
            final String xtitle = entryTitle.getText().toString();
            final String xlocation = locationBox.getText().toString();
            final String xdes = descriptionBox.getText().toString();
            final String xfromDate = fromDateBtn.getText().toString();
            final String xtoDate = toDateBtn.getText().toString();
            final String xfromTime = fromTimeBtn.getText().toString();
            final String xtoTime = toTimeBtn.getText().toString();


            final String fromDateTime = xfromDate + " " + xfromTime;
            final String toDateTime = xtoDate + " " + xtoTime;
            SimpleDateFormat formatter = new SimpleDateFormat("dd-MMM-yyyy EE hh:mm", Locale.ENGLISH);
            try{
                Date fromDT = formatter.parse(fromDateTime);
                Date toDT = formatter.parse(toDateTime);
                long fromTimeMilli = fromDT.getTime();
                long toTimeMilli = toDT.getTime();
                Intent intent = new Intent(Intent.ACTION_INSERT)
                        .setData(CalendarContract.Events.CONTENT_URI)
                        .putExtra(CalendarContract.Events.TITLE, xtitle)
                        .putExtra(CalendarContract.Events.EVENT_LOCATION, xlocation)
                        .putExtra(CalendarContract.Events.DESCRIPTION, xdes)
                        .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, fromTimeMilli)
                        .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, toTimeMilli);

                if (intent.resolveActivity(this.getPackageManager()) != null) {
                    this.startActivity(intent);
                }
            } catch (ParseException e){
                Log.e("myDebug", e.getMessage());
                e.printStackTrace();
            }
        });

        thumbnailImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!hasPhoto){
                    Log.i("myDebug", "Don't have photo, take a photo!");
                    take_photo(spid);
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PICTURE_CAPTURE && resultCode == RESULT_OK){
            SharedPreferences entrySP = getSharedPreferences(spid, MODE_PRIVATE);
            SharedPreferences.Editor ed = entrySP.edit();
            ed.putString("imgPath", pc.getImgPath());
            ed.apply();
            Form.this.recreate();
        }
    }

    private String dateBuilder(int year, int month, int dayOfWeek, int dayOfMonth){
        GalleryUtils gu = new GalleryUtils(Form.this);
        return String.format("%02d-%s-%d %s", dayOfMonth, gu.convertMonth(month), year, gu.convertDayOfWeek(dayOfWeek));
    }

    private String timeBuilder(int hour, int minute){
        return String.format("%02d:%02d", hour, minute);
    }

    private void take_photo(String spid) {
        pc.setContext(Form.this);
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (cameraIntent.resolveActivity(this.getPackageManager()) != null) {
            Log.d("myMessage","trying to capture photo");
            File pictureFile;
            try {
                pictureFile = pc.createImageFile();
                SharedPreferences entrySP = getSharedPreferences(spid, MODE_PRIVATE);
                Log.d("myDebug", "Current stored ImgPath in SP: "+entrySP.getString("imgPath", "no path"));
                SharedPreferences.Editor ed = entrySP.edit();
                ed.putString("imgPath", pictureFile.getAbsolutePath());
                Log.d("myDebug", "Now stored ImgPath in SP: "+entrySP.getString("imgPath", "no path"));
                ed.apply();
            } catch (IOException ex) {
                Toast.makeText(this,
                        "Photo file can't be created, please try again",
                        Toast.LENGTH_SHORT).show();
                return;
            } catch (NullPointerException exn){Toast.makeText(this, "Cannot get external files directory", Toast.LENGTH_SHORT).show(); return;}
            if (pictureFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.example.java_1d.fileprovider",
                        pictureFile);
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                this.startActivityForResult(cameraIntent, REQUEST_PICTURE_CAPTURE);
            }
        }
    }

    private Boolean isBefore(Date date1, Date date2){
        int res = date1.compareTo(date2);
        if (res < 0){
            return true;
        } else {
            return false;
        }
    }

    private void saveSP(String id, String title, String fromDate,
                        String toDate, String fromTime, String toTime,
                        String des, String loc, String imgPath, Boolean isDraft){
        SharedPreferences master = getSharedPreferences("master", MODE_PRIVATE);
        SharedPreferences.Editor masterEditor = master.edit();
        masterEditor.putString(id, id);
        masterEditor.apply();

        SharedPreferences entrySP = getSharedPreferences(id, MODE_PRIVATE);
        SharedPreferences.Editor editor = entrySP.edit();
        editor.putString("title", title);
        editor.putString("fromDate", fromDate);
        editor.putString("toDate", toDate);
        editor.putString("fromTime", fromTime);
        editor.putString("toTime", toTime);
        editor.putString("description", des);
        editor.putString("location", loc);
        editor.putBoolean("draft", isDraft);
        if (photoCaptured.getInstance().getImgPath() == null){
            editor.putString("imgPath", imgPath);
        } else {
            editor.putString("imgPath", photoCaptured.getInstance().getImgPath());
        }
        editor.apply();
    }
}
