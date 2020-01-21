package com.kronos.sutd;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.fragment.app.Fragment;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import static android.content.Context.MODE_PRIVATE;

public class DraftsFragment extends Fragment {

    private LinearLayout drafts_space;
    private ImageButton toSPBtn;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_drafts, container, false);
        super.onCreate(savedInstanceState);
        drafts_space = view.findViewById(R.id.draft_space);

        // get the master SP and loop through its contents to generate entry
        Map<String, ?> draftsMap = this.getActivity().getSharedPreferences("master", MODE_PRIVATE).getAll();

        int index = 0;
        for (Map.Entry<String, ?> entry: draftsMap.entrySet()){
            String spid = entry.getKey();
            Log.d("myLog", "SPID: "+spid);
            ConstraintLayout draftEntry = generateDraftEntry(spid);
            if (draftEntry != null){
                drafts_space.addView(draftEntry, index);
                index ++;
            }
        }
        return view;
    }

    /**
     * programmatically create a ConstraintLayout that contains all contents of one draft entry,
     * using the SharedPreferences ID (spid) that is passed in to locate the specific SP contents
     * @param spid SharedPreferences ID
     * @return A ConstraintLayout that can be added to the LinearLayout to display the draft entry
     */
    private ConstraintLayout generateDraftEntry(final String spid){

        // getting all contents
        SharedPreferences sp = this.getActivity().getSharedPreferences(spid, MODE_PRIVATE);
        final String title = sp.getString("title", "");
        String location = sp.getString("location","");
        String fromDate = sp.getString("fromDate", "");
        String toDate = sp.getString("toDate", "");
        String fromTime = sp.getString("fromTime", "");
        String toTime = sp.getString("toTime", "");
        String imgPath = sp.getString("imgPath", "");
        Boolean isDraft = sp.getBoolean("draft", true);

        if(!isDraft){
            return null;
        } else {
            ConstraintLayout constraintLayout = new ConstraintLayout(getActivity());
            constraintLayout.setLayoutParams(new ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.WRAP_CONTENT));

            TextView infoDisplay = new TextView(getActivity());
            ImageView thumbnail = new ImageView(getActivity());

            // configure constraints
            ConstraintLayout.LayoutParams infoLayout = new ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_CONSTRAINT, ConstraintLayout.LayoutParams.MATCH_CONSTRAINT);
            infoLayout.setMargins(16, 16, 16, 16);
            infoDisplay.setLayoutParams(infoLayout);

            ConstraintLayout.LayoutParams thumbnailLayout = new ConstraintLayout.LayoutParams(300,300);
            thumbnailLayout.setMargins(16,16,16,16);
            thumbnail.setLayoutParams(thumbnailLayout);

            // set IDs
            constraintLayout.setId(View.generateViewId());
            infoDisplay.setId(View.generateViewId());
            thumbnail.setId(View.generateViewId());

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

            // set contents
            String info = buildInfoDisplay(title, location, fromDate, toDate, fromTime, toTime);
            infoDisplay.setText(info);
            Log.i("myLog", info);

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

            // set clickable event
            constraintLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.i("myLog", String.format("Direct to entry editing page with ID <%s> ", spid));
                    Intent intent = new Intent(getContext(), ViewEntry.class);
                    intent.putExtra("id", spid);
                    getActivity().startActivity(intent);
                }
            });


            return constraintLayout;
        }
    }

    /**
     * Generate the info string to be displayed in the draft entry list
     * @param title title of entry
     * @param location location of entry
     * @param fromDate fromDate string
     * @param toDate toDate string
     * @param fromTime fromTime string
     * @param toTime toTime string
     * @return info string to be displayed
     */
    private String buildInfoDisplay(String title, String location, String fromDate, String toDate, String fromTime, String toTime){
        return String.format("%s\n%s\n%s - %s\n%s ~ %s", title, location, fromDate, toDate, fromTime, toTime);
    }

}
