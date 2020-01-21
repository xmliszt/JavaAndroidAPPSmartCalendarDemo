package com.kronos.sutd;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import net.fortuna.ical4j.model.property.Description;

public class ViewEntry extends AppCompatActivity {

    private ImageButton editBtn;
    private ConstraintLayout entryView;
    private String spid;  // SharedPreferences ID
    private String toDisplay;

    //shared pref
    public void displayInfo(){
        toDisplay="";
        SharedPreferences mypref= getSharedPreferences(spid, 0);
        if (mypref!=null){
            String title = mypref.getString("title", "");
            String location = mypref.getString("location","");
            String fromTime = mypref.getString("fromTime", "");
            String toTime = mypref.getString("toTime", "");
            String desc=mypref.getString("description","");
            toDisplay+="Title:  "+title+"\n\n";
            toDisplay+="Location:  "+location+"\n\n";
            toDisplay+="Time:  "+fromTime+"-"+toTime+"\n\n";
            toDisplay+="Description:  "+desc;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        spid=getIntent().getStringExtra("id");
        setContentView(R.layout.activity_view_entry);
        displayInfo();
        TextView textView=findViewById(R.id.information);
        textView.setText(toDisplay);

        editBtn = findViewById(R.id.editButton);
        editBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SharedPreferences mypref= getSharedPreferences(spid, 0);
                String title = mypref.getString("title", "");
                String location = mypref.getString("location","");
                String fromDate = mypref.getString("fromDate", "");
                String toDate = mypref.getString("toDate", "");
                String fromTime = mypref.getString("fromTime", "");
                String toTime = mypref.getString("toTime", "");
                String desc= mypref.getString("description","");
                String imgPath = mypref.getString("imgPath", "");

                photoCaptured.getInstance().setImgPath(imgPath);
                Log.d("myDebug", "Path: "+imgPath);

                Intent toForm = new Intent(ViewEntry.this, Form.class);
                Bundle extras = new Bundle();
                extras.putString("id", spid);
                extras.putString("title", title);
                extras.putString("location", location);
                extras.putString("description", desc);
                extras.putString("fromDate", fromDate);
                extras.putString("toDate", toDate);
                extras.putString("fromTime", fromTime);
                extras.putString("toTime", toTime);
                extras.putString("imgPath", imgPath);
                toForm.putExtras(extras);
                startActivity(toForm);
                finish();
            }
        });

        entryView = findViewById(R.id.entryView);
        entryView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent backCld = new Intent(ViewEntry.this, MainActivity.class);
                startActivity(backCld);
                finish();
            }
        });
    }
}