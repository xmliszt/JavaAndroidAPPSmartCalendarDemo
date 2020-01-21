package com.kronos.sutd;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.applandeo.materialcalendarview.CalendarView;
import com.applandeo.materialcalendarview.EventDay;
import com.applandeo.materialcalendarview.exceptions.OutOfDateRangeException;
import com.applandeo.materialcalendarview.listeners.OnDayClickListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import android.util.Log;
import java.util.Map;

import static android.content.Context.MODE_PRIVATE;


public class HomeFragment extends Fragment {

    private static final int PICK_IMAGE = 300;

    private Animation fab_open, fab_close, fab_clock, fab_anticlock;
    private photoCaptured pc = photoCaptured.getInstance();
    private LinearLayout entrySpace;
    private CalendarView calendar;

    // Read from master SP at onCreate and generate HashMap of the subSP content
    // "key": ID
    // "value": [displayedText, year, month, day]
    private Map<String, Map> entryContentMap = new HashMap<>();

    @Override
    public void onAttach(Context context){
        super.onAttach(context);
        Activity thisActivity;
        if (context instanceof MainActivity){
            thisActivity = (Activity) context;
        }
    }

    private static final int REQUEST_PICTURE_CAPTURE = 1;

    private String getImagePathFromSP(String spid){
        SharedPreferences sp = this.getActivity().getSharedPreferences(spid, MODE_PRIVATE);
        return sp.getString("imgPath", "");
    }

    private String getFromDateFromSP(String spid){
        SharedPreferences sp = this.getActivity().getSharedPreferences(spid, MODE_PRIVATE);
        return sp.getString("fromDate", "");
    }

    private Boolean isDraft(String spid){
        SharedPreferences sp = this.getActivity().getSharedPreferences(spid, MODE_PRIVATE);
        return sp.getBoolean("draft", true);
    }

    private String generateEventInfo(String spid) {
        SharedPreferences sp = getActivity().getSharedPreferences(spid, MODE_PRIVATE);
        final String title = sp.getString("title", "");
        String location = sp.getString("location","");
        String fromDate = sp.getString("fromDate", "");
        String toDate = sp.getString("toDate", "");
        String fromTime = sp.getString("fromTime", "");
        String toTime = sp.getString("toTime", "");
        return buildInfoDisplay(title, location, fromDate, toDate, fromTime, toTime);
    }

    private String buildInfoDisplay(String title, String location, String fromDate, String toDate, String fromTime, String toTime){
        return String.format("Title: %s\nLocation: %s\n%s - %s\n%s ~ %s", title, location, fromDate, toDate, fromTime, toTime);
    }

    private int getYearFromDate(String fromDate){
        String[] s = fromDate.split("-");
        String year = s[2].split(" ")[0];
        return Integer.parseInt(year);
    }

    private int getMonthFromDate(String fromDate){
        String[] s = fromDate.split("-");
        String monthInText = s[1];
        return new GalleryUtils(getContext()).getMonthNumFromText(monthInText.toLowerCase());
    }
    private int getDayFromDate(String fromDate){
        String[] s = fromDate.split("-");
        return Integer.parseInt(s[0]);
    }

    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        calendar = view.findViewById(R.id.calendar_view);
        entrySpace = view.findViewById(R.id.entrySpace);
        final TextView date_select = view.findViewById(R.id.selected_date);
        final CalendarView calendarView = view.findViewById(R.id.calendar_view);
        final FloatingActionButton fabcam = view.findViewById(R.id.floatingActionButtoncam);
        final FloatingActionButton fabgal = view.findViewById(R.id.floatingActionButtongal);
        final FloatingActionButton fabadd = view.findViewById(R.id.floatingActionButtonAdd);
        final FloatingActionButton fabaddEntry = view.findViewById(R.id.floatingActionButtonAddBlank);

        List<EventDay> events = new ArrayList<>();

        Calendar crrt = Calendar.getInstance();

        try {
            calendar.setDate(crrt);
        }
        catch (OutOfDateRangeException e) {
            Log.d("init", "date error");
        }

        // initialize ContentMap
        SharedPreferences master = getActivity().getSharedPreferences("master", MODE_PRIVATE);
        Map<String, ?> sps = master.getAll();
        for(Map.Entry<String, ?> entry: sps.entrySet()){
            String spid = entry.getKey();
            if(!isDraft(spid)){
                String eventInfoEntry = generateEventInfo(spid);
                String fromDate = getFromDateFromSP(spid);
                Map content = new HashMap();
                int c_year = getYearFromDate(fromDate);
                int c_month = getMonthFromDate(fromDate);
                int c_day = getDayFromDate(fromDate);
                content.put("info", eventInfoEntry);
                content.put("year", c_year);
                content.put("month", c_month);
                content.put("day", c_day);
                Log.i("myDebug", content.toString());
                entryContentMap.put(spid, content);
                Calendar toAddEvents = Calendar.getInstance();
                toAddEvents.set(c_year, c_month-1, c_day);
                events.add(new EventDay(toAddEvents, R.drawable.ic_event));
            }
        }

        calendar.setEvents(events);

        // initialize entry display
        entrySpace.removeAllViews();
        int year = crrt.get(Calendar.YEAR);
        int month = crrt.get(Calendar.MONTH);
        int day = crrt.get(Calendar.DAY_OF_MONTH);

        for(String k: entryContentMap.keySet()){
            Map entry = entryContentMap.get(k);
            int y = (int) entry.get("year");
            int m = (int) entry.get("month");
            int d = (int) entry.get("day");
            Log.i("myDebug", String.format("What I get: %d-%d-%d", y, m, d));
            Log.i("myDebug", String.format("Selected date: %d-%d-%d", year, month, day));
            if (year==y && month+1==m && day==d){
                //display it!
                ConstraintLayout cl = generateConstraintLayout(entry, k);
                if (cl != null){
                    entrySpace.addView(cl);
                }
            }
        }
        date_select.setSingleLine(false);
        date_select.setText(day + "-" + (month + 1) + "-" + year + " \n" );

        fab_close = AnimationUtils.loadAnimation(getContext(), R.anim.fab_close);
        fab_open = AnimationUtils.loadAnimation(getContext(), R.anim.fab_open);
        fab_clock = AnimationUtils.loadAnimation(getContext(), R.anim.fab_clkwise);
        fab_anticlock = AnimationUtils.loadAnimation(getContext(), R.anim.fab_anticlkwise);


        fabadd.setOnClickListener(new View.OnClickListener() {
            boolean isOpen = false;

            @Override
            public void onClick(View v) {
                if (isOpen) {
                    fabgal.startAnimation(fab_close);
                    fabcam.startAnimation(fab_close);
                    fabaddEntry.startAnimation(fab_close);
                    fabadd.startAnimation(fab_anticlock);
                    fabgal.setClickable(false);
                    fabcam.setClickable(false);
                    fabaddEntry.setClickable(false);
                    isOpen = false;
                } else {
                    fabgal.startAnimation(fab_open);
                    fabcam.startAnimation(fab_open);
                    fabaddEntry.startAnimation(fab_open);
                    fabadd.startAnimation(fab_clock);
                    fabgal.setClickable(true);
                    fabcam.setClickable(true);
                    fabaddEntry.setClickable(true);
                    isOpen = true;
                }
            }
        });
        fabgal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                getActivity().startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE);
            }
        });
        fabcam.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                take_photo();
            }
        });
        fabaddEntry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent goToForm = new Intent(getContext(), Form.class);
                Bundle extras = new Bundle();
                extras.putString("id", "");
                extras.putString("title", "");
                extras.putString("location", "");
                extras.putString("description", "");
                extras.putString("fromDate", "");
                extras.putString("toDate", "");
                extras.putString("fromTime", "");
                extras.putString("toTime", "");
                extras.putString("imgPath", "");
                goToForm.putExtras(extras);
                getActivity().startActivity(goToForm);
            }
        });



        calendarView.setOnDayClickListener(new OnDayClickListener() {
            @Override
            public void onDayClick(EventDay eventDay) {
                int year = eventDay.getCalendar().get(1);
                int month = eventDay.getCalendar().get(2);
                int dayOfMonth = eventDay.getCalendar().get(5);
                entrySpace.removeAllViews();
                // loop through the contentMap and find that matched entry to display
                for(String k: entryContentMap.keySet()){
                    Map entry = entryContentMap.get(k);
                    int y = (int) entry.get("year");
                    int m = (int) entry.get("month");
                    int d = (int) entry.get("day");
                    Log.i("myDebug", String.format("What I get: %d-%d-%d", y, m, d));
//                    Log.i("myDebug", String.format("Selected date: %d-%d-%d", year, month, dayOfMonth));
                    if (year==y && month+1==m && dayOfMonth==d){
                        //display it!
                        ConstraintLayout cl = generateConstraintLayout(entry, k);
                        if (cl != null){
                            entrySpace.addView(cl);
                        }
                    }
                }
                date_select.setSingleLine(false);
                date_select.setText(dayOfMonth + "-" + (month + 1) + "-" + year + " \n" );
            }
        });
        return view;
    }

    public void take_photo() {
        pc.setContext(getContext());
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (cameraIntent.resolveActivity(getActivity().getPackageManager()) != null) {
            Log.d("myDebug","trying to capture photo");
            File pictureFile;
            try {
                pictureFile = pc.createImageFile();
            } catch (IOException ex) {
                Toast.makeText(getContext(),
                        "Photo file can't be created, please try again",
                        Toast.LENGTH_SHORT).show();
                return;
            } catch (NullPointerException exn){Toast.makeText(getContext(), "Cannot get external files directory", Toast.LENGTH_SHORT).show(); return;}
            if (pictureFile != null) {
                Uri photoURI = FileProvider.getUriForFile(getActivity(),
                        "com.kronos.sutd.fileprovider",
                        pictureFile);
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                getActivity().startActivityForResult(cameraIntent, REQUEST_PICTURE_CAPTURE);
            }
        }
    }

    /**
     *
     * @param entry EntryMap for one entry
     * @param k Entry SharedPreferences ID
     * @return ConstraintLayout of the entry content
     */
    private ConstraintLayout generateConstraintLayout(Map entry, String k){
        ConstraintLayout constraintLayout = new ConstraintLayout(getActivity());
        constraintLayout.setLayoutParams(new ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.WRAP_CONTENT));

        TextView infoDisplay = new TextView(getActivity());
        ImageView thumbnail = new ImageView(getActivity());

        // configure constraints
        ConstraintLayout.LayoutParams infoLayout = new ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_CONSTRAINT, ConstraintLayout.LayoutParams.WRAP_CONTENT);
        infoLayout.setMargins(32, 16, 16, 16);
        infoDisplay.setLayoutParams(infoLayout);

        ConstraintLayout.LayoutParams thumbnailLayout = new ConstraintLayout.LayoutParams(300,300);
        thumbnailLayout.setMargins(16,16,16,16);
        thumbnail.setLayoutParams(thumbnailLayout);

        // set IDs
        constraintLayout.setId(View.generateViewId());
        infoDisplay.setId(View.generateViewId());
        thumbnail.setId(View.generateViewId());

        // set contents
        String info = (String) entry.get("info");
        infoDisplay.setText(info);
        Log.i("myLog", info);

        String imgPath = getImagePathFromSP(k);
        Log.i("myDebug", "Entry Image: "+imgPath);
        if (imgPath == ""){
            thumbnail.setImageResource(R.drawable.placeholder);
        } else {
            File imgFile = new File(imgPath);
            Bitmap bitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
            try {
                bitmap = photoCaptured.getInstance().processThumbnail(bitmap, imgPath);
            } catch (IOException ioex2){
                Toast.makeText(getContext(), "Ooops... Something went wrong with the photos...", Toast.LENGTH_SHORT).show();
            }
            thumbnail.setImageBitmap(bitmap);
        }
        constraintLayout.addView(thumbnail, 0);
        constraintLayout.addView(infoDisplay, 1);

        ConstraintSet constraintSet = new ConstraintSet();
        constraintSet.clone(constraintLayout);
        constraintSet.connect(thumbnail.getId(), ConstraintSet.TOP, constraintLayout.getId(), ConstraintSet.TOP);
        constraintSet.connect(thumbnail.getId(),ConstraintSet.BOTTOM, constraintLayout.getId(), ConstraintSet.BOTTOM);
        constraintSet.connect(thumbnail.getId(), ConstraintSet.LEFT, constraintLayout.getId(), ConstraintSet.LEFT);
        constraintSet.connect(thumbnail.getId(), ConstraintSet.RIGHT, infoDisplay.getId(), ConstraintSet.LEFT);
        constraintSet.connect(infoDisplay.getId(), ConstraintSet.TOP, thumbnail.getId(), ConstraintSet.TOP);
        constraintSet.connect(infoDisplay.getId(), ConstraintSet.BOTTOM, thumbnail.getId(), ConstraintSet.BOTTOM);
        constraintSet.connect(infoDisplay.getId(), ConstraintSet.RIGHT, constraintLayout.getId(), ConstraintSet.RIGHT);
        constraintSet.connect(infoDisplay.getId(), ConstraintSet.LEFT, thumbnail.getId(), ConstraintSet.RIGHT);
        constraintSet.applyTo(constraintLayout);

        // TODO: on-click bring up a list of
        constraintLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i("myLog", String.format("Direct to entry editing page with ID <%s> ", k));
                Intent intent = new Intent(getContext(), ViewEntry.class);
                intent.putExtra("id", k);
                getActivity().startActivity(intent);
            }
        });

        return constraintLayout;
    }


}

