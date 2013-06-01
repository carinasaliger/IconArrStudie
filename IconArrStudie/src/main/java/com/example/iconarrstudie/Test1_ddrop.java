package com.example.iconarrstudie;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.DragEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by john-louis on 17.05.13.
 */
public class Test1_ddrop extends Activity {
    private final static String TAG = Test1_ddrop.class.getSimpleName();
    private int selected_screen;
    private static boolean[][] in_use;
    private static String[][] input;
    private String[][] tags;
    static final String TITLE = "title";
    static final String ITEM_TYPE = "itemType";
    static final String SCREEN = "screen";
    static final String APPWIDGET_ID = "appWidgetId";
    static final String CELLX = "cellX";
    static final String CELLY = "cellY";
    static final String SPANX = "spanX";
    static final String SPANY = "spanY";
    static final String ICON = "icon";
    static final String CONTAINER = "container";

    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "Test1_ddrop started!");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test1);

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
        final int titleIndex = c.getColumnIndex(TITLE);
        final int itemTypeIndex = c.getColumnIndex(ITEM_TYPE);
        final int screenIndex = c.getColumnIndex(SCREEN);
        final int appWidgetIdIndex = c.getColumnIndex(APPWIDGET_ID);
        final int cellXIndex = c.getColumnIndex(CELLX);
        final int cellYIndex = c.getColumnIndex(CELLY);
        final int spanXIndex = c.getColumnIndex(SPANX);
        final int spanYIndex = c.getColumnIndex(SPANY);
        final int iconIndex = c.getColumnIndex(ICON);
        final int containerIndex = c.getColumnIndex(CONTAINER);

        // Listen
        List<Entry> entries = new LinkedList<Entry>();

        // booleans
        in_use = new boolean[4][4];
        for(int x = 0; x < 4; x++){
            for(int y = 0; y < 4; y++){
                in_use[x][y] = false;
            }
        }

        // Auslesen und speichern der relevanten Werte in ContentValues
        ContentValues[] row_values = new ContentValues[c.getCount()];
        int iterator = 0;
        c.moveToFirst();
        while(c.moveToNext()){
            if(c.getInt(screenIndex) == selected_screen){
                ContentValues values = new ContentValues(9);
                values.put(TITLE, c.getString(titleIndex));
                values.put(ITEM_TYPE, c.getInt(itemTypeIndex));
                values.put(APPWIDGET_ID, c.getInt(appWidgetIdIndex));
                values.put(CELLX, c.getInt(cellXIndex));
                values.put(CELLY, c.getInt(cellYIndex));
                values.put(SPANX, c.getInt(spanXIndex));
                values.put(SPANY, c.getInt(spanYIndex));
                values.put(ICON, c.getBlob(iconIndex));
                values.put(CONTAINER, c.getInt(containerIndex));
                row_values[iterator++] = values;
            }
        }
        c.close();

        Log.d(TAG, "parsed " + iterator + " rows of launcher.db");

        // Füllen von entries anhand der ausgelesenen Werte
        for(ContentValues cv : row_values){
            if (cv != null){
                // Weglassen aller Elemente die nicht auf dem 4x4 Grid sind
                if(cv.getAsInteger(CELLX) > 3 || cv.getAsInteger(CELLY) > 3){
                    continue;
                }
                Entry temp = new Entry(
                        cv.getAsInteger(CELLX),
                        cv.getAsInteger(CELLY),
                        cv.getAsInteger(SPANX),
                        cv.getAsInteger(SPANY),
                        cv.getAsByteArray(ICON),
                        cv.getAsInteger(ITEM_TYPE),
                        cv.getAsString(TITLE),
                        cv.getAsInteger(CONTAINER));
                Log.d(TAG, "adding entry: \n" + temp.toString());
                entries.add(temp);
            }
        }

        // Views aus R holen und in imageArray speichern

        final ScrollView scrollView = (ScrollView) findViewById(R.id.scrollView);
        Log.d(TAG, "creating and filling ImageView-Array");
        final ImageView[][][] imageArray= new ImageView[8][6][2];
        imageArray[0][0][0] = (ImageView) findViewById(R.id.row0_cell0_low);
        imageArray[1][0][0] = (ImageView) findViewById(R.id.row0_cell1_low);
        imageArray[2][0][0] = (ImageView) findViewById(R.id.row0_cell2_low);
        imageArray[3][0][0] = (ImageView) findViewById(R.id.row0_cell3_low);
        imageArray[0][1][0] = (ImageView) findViewById(R.id.row1_cell0_low);
        imageArray[1][1][0] = (ImageView) findViewById(R.id.row1_cell1_low);
        imageArray[2][1][0] = (ImageView) findViewById(R.id.row1_cell2_low);
        imageArray[3][1][0] = (ImageView) findViewById(R.id.row1_cell3_low);
        imageArray[0][2][0] = (ImageView) findViewById(R.id.row2_cell0_low);
        imageArray[1][2][0] = (ImageView) findViewById(R.id.row2_cell1_low);
        imageArray[2][2][0] = (ImageView) findViewById(R.id.row2_cell2_low);
        imageArray[3][2][0] = (ImageView) findViewById(R.id.row2_cell3_low);
        imageArray[0][3][0] = (ImageView) findViewById(R.id.row3_cell0_low);
        imageArray[1][3][0] = (ImageView) findViewById(R.id.row3_cell1_low);
        imageArray[2][3][0] = (ImageView) findViewById(R.id.row3_cell2_low);
        imageArray[3][3][0] = (ImageView) findViewById(R.id.row3_cell3_low);


        imageArray[0][0][1] = (ImageView) findViewById(R.id.row0_cell0_high);
        imageArray[1][0][1] = (ImageView) findViewById(R.id.row0_cell1_high);
        imageArray[2][0][1] = (ImageView) findViewById(R.id.row0_cell2_high);
        imageArray[3][0][1] = (ImageView) findViewById(R.id.row0_cell3_high);
        imageArray[0][1][1] = (ImageView) findViewById(R.id.row1_cell0_high);
        imageArray[1][1][1] = (ImageView) findViewById(R.id.row1_cell1_high);
        imageArray[2][1][1] = (ImageView) findViewById(R.id.row1_cell2_high);
        imageArray[3][1][1] = (ImageView) findViewById(R.id.row1_cell3_high);
        imageArray[0][2][1] = (ImageView) findViewById(R.id.row2_cell0_high);
        imageArray[1][2][1] = (ImageView) findViewById(R.id.row2_cell1_high);
        imageArray[2][2][1] = (ImageView) findViewById(R.id.row2_cell2_high);
        imageArray[3][2][1] = (ImageView) findViewById(R.id.row2_cell3_high);
        imageArray[0][3][1] = (ImageView) findViewById(R.id.row3_cell0_high);
        imageArray[1][3][1] = (ImageView) findViewById(R.id.row3_cell1_high);
        imageArray[2][3][1] = (ImageView) findViewById(R.id.row3_cell2_high);
        imageArray[3][3][1] = (ImageView) findViewById(R.id.row3_cell3_high);
        imageArray[0][4][1] = (ImageView) findViewById(R.id.row4_cell0_high);
        imageArray[1][4][1] = (ImageView) findViewById(R.id.row4_cell1_high);
        imageArray[2][4][1] = (ImageView) findViewById(R.id.row4_cell2_high);
        imageArray[3][4][1] = (ImageView) findViewById(R.id.row4_cell3_high);
        imageArray[4][4][1] = (ImageView) findViewById(R.id.row4_cell4_high);
        imageArray[5][4][1] = (ImageView) findViewById(R.id.row4_cell5_high);
        imageArray[6][4][1] = (ImageView) findViewById(R.id.row4_cell6_high);
        imageArray[7][4][1] = (ImageView) findViewById(R.id.row4_cell7_high);
        imageArray[0][5][1] = (ImageView) findViewById(R.id.row5_cell0_high);
        imageArray[1][5][1] = (ImageView) findViewById(R.id.row5_cell1_high);
        imageArray[2][5][1] = (ImageView) findViewById(R.id.row5_cell2_high);
        imageArray[3][5][1] = (ImageView) findViewById(R.id.row5_cell3_high);
        imageArray[4][5][1] = (ImageView) findViewById(R.id.row5_cell4_high);
        imageArray[5][5][1] = (ImageView) findViewById(R.id.row5_cell5_high);
        imageArray[6][5][1] = (ImageView) findViewById(R.id.row5_cell6_high);
        imageArray[7][5][1] = (ImageView) findViewById(R.id.row5_cell7_high);

        // onDragListener für Grid-Elemente setzen
        for(int x = 0; x < 4; x++){
            for(int y = 0; y < 4; y++){
                final int finalX = x;
                final int finalY = y;
                imageArray[x][y][1].setOnDragListener(new View.OnDragListener() {
                    @Override
                    public boolean onDrag(View view, DragEvent dragEvent) {
                        final int action = dragEvent.getAction();
                        // Drawable der auf die View gezogenen View holen
                        Drawable icon = (Drawable) dragEvent.getLocalState();
                        switch(action){
                            // falls Drag gerade beginnt mögliche Felder blau färben
                            case DragEvent.ACTION_DRAG_STARTED:
                                if (!in_use[finalX][finalY]){
                                    imageArray[finalX][finalY][0].setColorFilter(Color.BLUE);
                                    imageArray[finalX][finalY][0].invalidate();
                                    return true;
                                }
                                else return false;
                            // falls Element über einem möglichen Feld liegt dieses grün färben
                            // if unterscheidung nicht nötig da die View schon bei ACTION_DRAG_STARTED false returnt und nicht mehr auf weitere events listent
                            case DragEvent.ACTION_DRAG_ENTERED:
                                imageArray[finalX][finalY][0].setColorFilter(Color.GREEN);
                                imageArray[finalX][finalY][0].invalidate();
                                scrollView.smoothScrollTo(0, 0);
                                return true;
                            // falls Element wieder herausgezogen wird wieder blau färben
                            case DragEvent.ACTION_DRAG_EXITED:
                                imageArray[finalX][finalY][0].setColorFilter(Color.BLUE);
                                imageArray[finalX][finalY][0].invalidate();
                                return true;
                            // falls Element in einer View losgelassen wird, das Icon übernehmen und setzen, sowie in in_use eintragen
                            case DragEvent.ACTION_DROP:
                                if(!in_use[finalX][finalY]){
                                    imageArray[finalX][finalY][1].setImageDrawable(icon);
                                    imageArray[finalX][finalY][0].clearColorFilter();
                                    imageArray[finalX][finalY][0].invalidate();
                                    imageArray[finalX][finalY][1].invalidate();
                                    in_use[finalX][finalY] = true;
                                    return true;
                                }
                                else return false;
                            // falls Drag endet und Icon nicht in der View gedroppt wurde wieder zurück in Ausgangszustand
                            case DragEvent.ACTION_DRAG_ENDED:
                                imageArray[finalX][finalY][0].clearColorFilter();
                                imageArray[finalX][finalY][0].invalidate();
                                return true;
                        }
                        return false;
                    }
                });
            }
        }

        // Abfragen und Zeichnen der Elemente
        int x = 0;
        int y = 4;

        for(Entry e : entries){
            Log.d(TAG, "in for loop for icons + folders");
            boolean wrote = false;
            // falls e ein Icon ist und direkt auf dem Desktop liegt (nicht in der Startleiste)
            if((e.getTag() == 0 || e.getTag() == 1) && e.getContainer() == (-100)){
                Log.d(TAG, "found Icon");
                Bitmap bmp = BitmapFactory.decodeByteArray(e.getIcon(), 0, e.getIcon().length);
                Log.d(TAG, "x: " + x + " y: " + y);
                imageArray[x][y][1].setImageBitmap(bmp);
                // longclicklistener setzen
                final int finalX = x;
                final int finalY = y;
                imageArray[x][y][1].setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View view) {
                        Log.d(TAG, "LONGPRESS");
                        ClipData.Item item = new ClipData.Item((String) view.getTag());
                        ClipData dragData = new ClipData((String) view.getTag(), new String[] {ClipDescription.MIMETYPE_TEXT_PLAIN},item);
                        View.DragShadowBuilder myShadow = new View.DragShadowBuilder(view);
                        view.startDrag(dragData, myShadow, imageArray[finalX][finalY][1].getDrawable(), 0);
                        return true;
                    }
                });
                imageArray[x][y][1].invalidate();
                wrote = true;
            }
            // falls e ein ordner ist
            if(e.getTag() == Entry.FOLDER && e.getContainer() == (-100)){
                Log.d(TAG, "found folder");
                Log.d(TAG, "x: " + x + " y: " + y);
                imageArray[x][y][1].setImageResource(R.drawable.portal_ring_inner_holo);
                // longclicklistener setzen
                final int finalX1 = x;
                final int finalY1 = y;
                imageArray[x][y][1].setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View view) {
                        Log.d(TAG, "LONGPRESS");
                        ClipData.Item item = new ClipData.Item((String) view.getTag());
                        ClipData dragData = new ClipData((String) view.getTag(), new String[] {ClipDescription.MIMETYPE_TEXT_PLAIN},item);
                        View.DragShadowBuilder myShadow = new View.DragShadowBuilder(view);
                        view.startDrag(dragData, myShadow, imageArray[finalX1][finalY1][1].getDrawable(), 0);
                        return true;
                    }
                });
                imageArray[x][y][1].invalidate();
                wrote = true;
            }
            // falls e ein widget ist
            if(e.getTag() == Entry.WIDGET){
                Log.d(TAG, "found Widget");
                switch(e.getSpan_x()){
                    case 1:
                        switch(e.getSpan_y()){
                            case 1:
                                imageArray[x][y][1].setImageResource(R.drawable.widget_1x1);
                                imageArray[x][y][1].invalidate();
                                break;
                            case 2:
                                imageArray[x][y][1].setImageResource(R.drawable.widget_1x2);
                                imageArray[x][y][1].invalidate();
                                break;
                            case 3:
                                imageArray[x][y][1].setImageResource(R.drawable.widget_1x3);
                                imageArray[x][y][1].invalidate();
                                break;
                            case 4:
                                imageArray[x][y][1].setImageResource(R.drawable.widget_1x4);
                                imageArray[x][y][1].invalidate();
                                break;
                        }
                        break;
                    case 2:
                        switch(e.getSpan_y()){
                            case 1:
                                imageArray[x][y][1].setImageResource(R.drawable.widget_2x1);
                                imageArray[x][y][1].invalidate();
                                break;
                            case 2:
                                imageArray[x][y][1].setImageResource(R.drawable.widget_2x2);
                                imageArray[x][y][1].invalidate();
                                break;
                            case 3:
                                imageArray[x][y][1].setImageResource(R.drawable.widget_2x3);
                                imageArray[x][y][1].invalidate();
                                break;
                            case 4:
                                imageArray[x][y][1].setImageResource(R.drawable.widget_2x4);
                                imageArray[x][y][1].invalidate();
                                break;
                        }
                        break;
                    case 3:
                        switch(e.getSpan_y()){
                            case 1:
                                imageArray[x][y][1].setImageResource(R.drawable.widget_3x1);
                                imageArray[x][y][1].invalidate();
                                break;
                            case 2:
                                imageArray[x][y][1].setImageResource(R.drawable.widget_3x2);
                                imageArray[x][y][1].invalidate();
                                break;
                            case 3:
                                imageArray[x][y][1].setImageResource(R.drawable.widget_3x3);
                                imageArray[x][y][1].invalidate();
                                break;
                            case 4:
                                imageArray[x][y][1].setImageResource(R.drawable.widget_3x4);
                                imageArray[x][y][1].invalidate();
                                break;
                        }
                        break;
                    case 4:
                        switch(e.getSpan_y()){
                            case 1:
                                imageArray[x][y][1].setImageResource(R.drawable.widget_4x1);
                                imageArray[x][y][1].invalidate();
                                break;
                            case 2:
                                imageArray[x][y][1].setImageResource(R.drawable.widget_4x2);
                                imageArray[x][y][1].invalidate();
                                break;
                            case 3:
                                imageArray[x][y][1].setImageResource(R.drawable.widget_4x3);
                                imageArray[x][y][1].invalidate();
                                break;
                            case 4:
                                imageArray[x][y][1].setImageResource(R.drawable.widget_4x4);
                                imageArray[x][y][1].invalidate();
                                break;
                        }
                        break;
                }
                wrote = true;
            }
            // counter MAGIC
            if(wrote){
                if(y == 4){
                    y++;
                }
                else{
                    x++;
                    y = 4;

                }
            }
        }

        // Buttonfunktionen festlegen
        Button confirm = (Button) findViewById(R.id.confirm);
        Button back = (Button) findViewById(R.id.back);
        Button clear = (Button) findViewById(R.id.clear);
        confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //TODO: compare & save data
//                Log.d(TAG, "number of errors: " + errors);
                finish();
            }
        });
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "back button pressed");
                finish();
            }
        });
        clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "clear button pressed");
                for(int x = 0; x < 4; x++){
                    for(int y = 0; y < 4; y++){
                        imageArray[x][y][1].setImageResource(R.drawable.contour);
                        imageArray[x][y][1].invalidate();
                        in_use[x][y] = false;

                    }
                }
            }
        });
    }
}