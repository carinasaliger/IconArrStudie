package com.example.iconarrstudie;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

/**
 * Created by john-louis on 13.06.13.
 */
public class Pre_Test3 extends Activity {
    private final static String TAG = Pre_Test3.class.getSimpleName();
    private int selected_screen;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "pre_test3 started!");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pre_test3);

        // Wallpaper
        Log.d(TAG, "setting Wallpaper");
        getWindow().setBackgroundDrawable(getWallpaper());

        // content_uri aus MainActivity holen
        String newString = null;
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            newString = extras.getString("uri");
        }
        final Uri content_uri = Uri.parse(newString);
        Log.d(TAG, "received content_uri: " + content_uri);

        // Ausgewählten screen aus MainActivity holen
        if (extras != null) {
            selected_screen = extras.getInt("screen");
        }
        Log.d(TAG, "received screen: " + selected_screen);

        // Cursor aus content_uri erstellen
        ContentResolver cr = this.getContentResolver();
        Cursor c = cr.query(content_uri, null, null, null, null);

        Button start = (Button) findViewById(R.id.start);
        Button back = (Button) findViewById(R.id.back);
        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i1 = new Intent(Pre_Test3.this, Test3_pos.class);
                i1.putExtra("uri", content_uri.toString());
                i1.putExtra("screen", selected_screen);
                Log.i(TAG, "starting Test3_pos");
                startActivity(i1);
            }
        });

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "back button pressed");
                finish();
            }
        });

    }
}