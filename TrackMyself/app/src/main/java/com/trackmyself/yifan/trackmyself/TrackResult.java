package com.trackmyself.yifan.trackmyself;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.TextView;

public class TrackResult extends AppCompatActivity {
    //Keys for getting activity state from other activities
    private final static String KEY_START_TRACK_TIME = "start-track-time";
    private final static String KEY_END_TRACK_TIME = "end-track-time";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_track_result);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        showTrackResult(getIntent());

    }

    private void showTrackResult(Intent intent) {
        String startTrackTime = intent.getStringExtra(KEY_START_TRACK_TIME);
        String endTrackTime =intent.getStringExtra(KEY_END_TRACK_TIME);

        TextView textView = (TextView)findViewById(R.id.textView_time);
        textView.setText(startTrackTime + "\r\n" + endTrackTime);
    }
}
