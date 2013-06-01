package com.example.iconarrstudie;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

/**
 * Created by john-louis on 17.05.13.
 */
public class Test2_pverbrauch extends Activity {
    // Tag für Logs
    private final static String TAG = Test2_pverbrauch.class.getSimpleName();

    private static int selected_screen;
    static final String ITEM_TYPE = "itemType";
    static final String SCREEN = "screen";
    static final String APPWIDGET_ID = "appWidgetId";
    static final String CELLX = "cellX";
    static final String CELLY = "cellY";
    static final String SPANX = "spanX";
    static final String SPANY = "spanY";
    static boolean[][] input;
    static boolean[][] answer;

    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "Test2_pverbrauch started");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test2);

        // Wallpaper
        Log.d(TAG, "setting Wallpaper");
//        final WallpaperManager wallpaperManager = WallpaperManager.getInstance(this);
//        final Drawable wallpaperDrawable = wallpaperManager.getDrawable();
        getWindow().setBackgroundDrawable(getWallpaper());

        // content_uri aus MainActivity holen
        String newString = null;
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            newString = extras.getString("uri");
        }
        Uri content_uri = Uri.parse(newString);
        Log.d(TAG, "received content_uri: " + content_uri);

        // Ausgewählten screen aus MainActivity holen
        if (extras != null) {
            selected_screen = extras.getInt("screen");
        }
        Log.d(TAG, "received screen: " + selected_screen);

        // Cursor aus content_uri erstellen
        ContentResolver cr = this.getContentResolver();
        Cursor c = cr.query(content_uri, null, null, null, null);

        // Indizes
        final int itemTypeIndex = c.getColumnIndex(ITEM_TYPE);
        final int screenIndex = c.getColumnIndex(SCREEN);
        final int appWidgetIdIndex = c.getColumnIndex(APPWIDGET_ID);
        final int cellXIndex = c.getColumnIndex(CELLX);
        final int cellYIndex = c.getColumnIndex(CELLY);
        final int spanXIndex = c.getColumnIndex(SPANX);
        final int spanYIndex = c.getColumnIndex(SPANY);

        ContentValues[] row_values = new ContentValues[c.getCount()];

        // Arrays zum auswerten der Antworten
        input = new boolean[4][4];
        answer = new boolean[4][4];

        // boolean[][] mit false initialisiern
        for(int x = 0; x < 4; x++){
            for(int y = 0; y < 4; y++){
                answer[x][y] = false;
                input[x][y] = false;
            }
        }

        // Auslesen und speichern der relevanten Werte in ContentValues
        int iterator = 0;
        c.moveToFirst();
        while(c.moveToNext()){
            if(c.getInt(screenIndex) == selected_screen){
                ContentValues values = new ContentValues(6);
                values.put(ITEM_TYPE, c.getInt(itemTypeIndex));
                values.put(APPWIDGET_ID, c.getInt(appWidgetIdIndex));
                values.put(CELLX, c.getInt(cellXIndex));
                values.put(CELLY, c.getInt(cellYIndex));
                values.put(SPANX, c.getInt(spanXIndex));
                values.put(SPANY, c.getInt(spanYIndex));
                row_values[iterator++] = values;
            }
        }

        Log.i(TAG, "parsed " + iterator + " rows of launcher.db");

        // Füllen des boolean[][] anhand der ausgelesenen Werte
        for(ContentValues cv : row_values){
            if (cv != null){
                // Prüfen ob die Zeile ein Widget ist
                if(cv.getAsInteger(ITEM_TYPE) == 4 && cv.getAsInteger(APPWIDGET_ID) != -1){
//                    Log.d(TAG, "found fitting content value: \n" +
//                            "ITEM_TYPE: " + cv.getAsInteger(ITEM_TYPE) + "\n" +
//                            "APPWIDGET_ID: " + cv.getAsInteger(APPWIDGET_ID) + "\n" +
//                            "CELLX: " + cv.getAsInteger(CELLX) + "\n" +
//                            "CELLY: " + cv.getAsInteger(CELLY) + "\n" +
//                            "SPANX: " + cv.getAsInteger(SPANX) + "\n" +
//                            "SPANY: " + cv.getAsInteger(SPANY) + "\n"
//                    );
                    int cell_x = cv.getAsInteger(CELLX);
                    int cell_y = cv.getAsInteger(CELLY);
                    for(int span_x = 0; span_x < cv.getAsInteger(SPANX); span_x++){
                        for (int span_y = 0; span_y < cv.getAsInteger(SPANY); span_y++){
                            answer[cell_x + span_x][cell_y + span_y] = true;
                        }
                    }
                }
            }
        }

        Log.i(TAG, "value of answer[][] after parse: \n" +
                answer[0][0] + "\t" + answer[1][0] + "\t" + answer[2][0] + "\t" + answer[3][0] + "\n" +
                answer[0][1] + "\t" + answer[1][1] + "\t" + answer[2][1] + "\t" + answer[3][1] + "\n" +
                answer[0][2] + "\t" + answer[1][2] + "\t" + answer[2][2] + "\t" + answer[3][2] + "\n" +
                answer[0][3] + "\t" + answer[1][3] + "\t" + answer[2][3] + "\t" + answer[3][3]
        );

        // Views aus R holen und in imageArray speichern
        Log.d(TAG, "creating and filling ImageView-Array");
        final ImageView[][] imageArray= new ImageView[4][4];
        imageArray[0][0] = (ImageView) findViewById(R.id.row0_cell0);
        imageArray[1][0] = (ImageView) findViewById(R.id.row0_cell1);
        imageArray[2][0] = (ImageView) findViewById(R.id.row0_cell2);
        imageArray[3][0] = (ImageView) findViewById(R.id.row0_cell3);
        imageArray[0][1] = (ImageView) findViewById(R.id.row1_cell0);
        imageArray[1][1] = (ImageView) findViewById(R.id.row1_cell1);
        imageArray[2][1] = (ImageView) findViewById(R.id.row1_cell2);
        imageArray[3][1] = (ImageView) findViewById(R.id.row1_cell3);
        imageArray[0][2] = (ImageView) findViewById(R.id.row2_cell0);
        imageArray[1][2] = (ImageView) findViewById(R.id.row2_cell1);
        imageArray[2][2] = (ImageView) findViewById(R.id.row2_cell2);
        imageArray[3][2] = (ImageView) findViewById(R.id.row2_cell3);
        imageArray[0][3] = (ImageView) findViewById(R.id.row3_cell0);
        imageArray[1][3] = (ImageView) findViewById(R.id.row3_cell1);
        imageArray[2][3] = (ImageView) findViewById(R.id.row3_cell2);
        imageArray[3][3] = (ImageView) findViewById(R.id.row3_cell3);

        // Listener für Imageviews
        Log.d(TAG, "beginning init of views");
        // Iterieren über Array
        for(int x = 0; x <=3; x++){
            for (int y = 0; y <= 3; y++){
                final int finalX = x;
                final int finalY = y;
                imageArray[x][y].setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // erster klick
                        if(imageArray[finalX][finalY].getColorFilter() == null){
                            Log.d(TAG, "registered FIRST keypress on: x= " + String.valueOf(finalX) + "\t" + "y= " + String.valueOf(finalY));
                            // Farbe auf blau setzen
                            imageArray[finalX][finalY].setColorFilter(Color.rgb(51, 181, 229));
                            input[finalX][finalY] = true;
                        }
                        // zweiter klick
                        else{
                            Log.d(TAG, "registered SECOND keypress on: x= " + String.valueOf(finalX) + "\t" + "y= " + String.valueOf(finalY));
                            imageArray[finalX][finalY].setColorFilter(null);
                            input[finalX][finalY] = false;
                        }
                    }
                });
            }
        }

        // Restliche Buttons, Zurück und Bestätigen
        Button confirm = (Button) findViewById(R.id.confirm);
        Button back = (Button) findViewById(R.id.back);
        confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //TODO: compare & save data
                Log.i(TAG, "result of test 2 for screen " + selected_screen + " :");
                Log.i(TAG, "value of input[][] after confirm: \n" +
                        input[0][0] + "\t" +  input[1][0] + "\t" +  input[2][0] + "\t" +  input[3][0] + "\n" +
                        input[0][1] + "\t" +  input[1][1] + "\t" +  input[2][1] + "\t" +  input[3][1] + "\n" +
                        input[0][2] + "\t" +  input[1][2] + "\t" +  input[2][2] + "\t" +  input[3][2] + "\n" +
                        input[0][3] + "\t" +  input[1][3] + "\t" +  input[2][3] + "\t" +  input[3][3]
                );
                Log.i(TAG, "value of answer[][] after parse: \n" +
                        answer[0][0] + "\t" + answer[1][0] + "\t" + answer[2][0] + "\t" + answer[3][0] + "\n" +
                        answer[0][1] + "\t" + answer[1][1] + "\t" + answer[2][1] + "\t" + answer[3][1] + "\n" +
                        answer[0][2] + "\t" + answer[1][2] + "\t" + answer[2][2] + "\t" + answer[3][2] + "\n" +
                        answer[0][3] + "\t" + answer[1][3] + "\t" + answer[2][3] + "\t" + answer[3][3]
                );
                int errors = 0;
                for(int i = 0; i < 4; i++){
                    for(int j = 0; j < 4; j++){
                        if(input[i][j] != answer[i][j]){
                            errors++;
                        }
                    }
                }
                Log.i(TAG, "number of errors: " + errors);
                finish();
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