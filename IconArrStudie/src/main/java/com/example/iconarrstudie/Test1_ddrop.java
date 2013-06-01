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
import android.widget.Button;
import android.widget.ImageView;
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
    private static List<Integer[]> lastaction;
    private static String[][] solution;
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
    static final ImageView[][][] imageArray = new ImageView[8][6][2];

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
        lastaction = new LinkedList<Integer[]>();

        // Arrays initialisieren und füllen
        in_use = new boolean[4][4];
        solution = new String[4][4];
        input = new String[4][4];
        for(int x = 0; x < 4; x++){
            for(int y = 0; y < 4; y++){
                in_use[x][y] = false;
                solution[x][y] = "empty";
                input[x][y] = "empty";
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

        // onDragListener für Grid-Views setzen
        for(int x = 0; x < 4; x++){
            for(int y = 0; y < 4; y++){
                final int finalX = x;
                final int finalY = y;
                imageArray[x][y][1].setOnDragListener(new View.OnDragListener() {
                    @Override
                    public boolean onDrag(View view, DragEvent dragEvent) {
                        Integer[] last = lastaction.get(lastaction.size() - 1);
                        final int action = dragEvent.getAction();
                        // Drawable der auf die View gezogenen View holen
                        Entry e = (Entry) dragEvent.getLocalState();
                        Bitmap icon = null;
                        if(e.getTag() == 0 || e.getTag() == 1){
                            icon = BitmapFactory.decodeByteArray(e.getIcon(), 0, e.getIcon().length);
                        }
                        switch(action){
                                // falls Drag gerade beginnt mögliche Felder blau färben
                            case DragEvent.ACTION_DRAG_STARTED:
                                // Unterscheidung zwischen Icons und Widgets
                                // falls ein Icon
                                if(last[0] == 0 && last[1] == 0){
                                    if (!in_use[finalX][finalY]){
                                        imageArray[finalX][finalY][0].setColorFilter(Color.BLUE);
                                        imageArray[finalX][finalY][0].invalidate();
                                        return true;
                                    }
                                }
                                // falls ein widget
                                if(last[0] > 0 && last[1] > 0){
                                    // falls es überstehen würde
                                    if(finalX + last[0] > 4 || finalY + last[1] > 4){
                                        Log.d(TAG, "x: " + finalX + " y: " + finalY + " würde überstehen");
                                        imageArray[finalX][finalY][0].setColorFilter(Color.RED);
                                        imageArray[finalX][finalY][0].invalidate();
                                        return false;
                                    }
                                    // falls im bereich bereits etwa liegt
                                    boolean space_used = false;
                                    for(int startx = 0; startx < last[0]; startx++){
                                        for(int starty = 0; starty < last[1]; starty++){
                                            if(in_use[finalX + startx][finalY + starty]){
                                                Log.d(TAG, "checking in_use for x:" + (finalX + startx) + " y: " + (finalY + starty));
                                                space_used = true;
                                            }
                                        }
                                    }
                                    if(space_used){
                                        imageArray[finalX][finalY][0].setColorFilter(Color.RED);
                                        imageArray[finalX][finalY][0].invalidate();
                                        return false;
                                    }
                                    else{
                                        imageArray[finalX][finalY][0].setColorFilter(Color.BLUE);
                                        imageArray[finalX][finalY][0].invalidate();
                                        return true;
                                    }
                                }
                                return true;
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
                                // prüfen ob ein icon gedroppt wurde
                                if(last[0] == 0 && last[1] == 0){
                                    Log.d(TAG, "icon dropped on x:" + finalX + " y:" + finalY);
                                    if(!in_use[finalX][finalY]){
                                        if(e.getTag() == Entry.FOLDER){
                                            imageArray[finalX][finalY][1].setImageResource(R.drawable.portal_ring_inner_holo);
                                            input[finalX][finalY] = "Folder";

                                        }
                                        else{
                                            imageArray[finalX][finalY][1].setImageBitmap(icon);
                                            input[finalX][finalY] = e.getTitle();

                                        }
                                        imageArray[finalX][finalY][0].clearColorFilter();
                                        imageArray[finalX][finalY][0].invalidate();
                                        imageArray[finalX][finalY][1].invalidate();
                                        in_use[finalX][finalY] = true;
                                        return true;
                                    }
                                    else return false;
                                }
                                // falls ein Widget gedroppt wurde
                                // Bild der betroffenen Felder anpassen
                                // in_use anpassen
                                else{
                                    Log.d(TAG, "widget dropped on x:" + finalX + " y:" + finalY);
                                    Log.d(TAG, "starte schleife für last[0]=" + last[0] + " , last[1]=" + last[1] );
                                    for(int startx = 0; startx < last[0]; startx++){
                                        for(int starty = 0; starty < last[1]; starty++){
                                            Log.d(TAG, "setze feld x=" + (finalX + starty) + " , y=" + (finalY + starty) );
                                            imageArray[finalX + startx][finalY + starty][1].setImageResource(R.drawable.widget_1x1);
                                            imageArray[finalX + startx][finalY + starty][0].clearColorFilter();
                                            imageArray[finalX + startx][finalY + starty][0].invalidate();
                                            imageArray[finalX + startx][finalY + starty][1].invalidate();
                                            // In Input eintragen
                                            input[finalX + startx][finalY + starty] = "Widget";
                                            in_use[finalX + startx][finalY + starty] = true;
                                        }
                                    }
                                    return true;
                                }
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

        for(final Entry e : entries){
            Log.d(TAG, "in for loop for icons + folders");
            // flag zum Anzeigen von Änderungen
            boolean wrote = false;
            // falls e ein Icon ist und direkt auf dem Desktop liegt (nicht im Dock)
            if((e.getTag() == 0 || e.getTag() == 1) && e.getContainer() == (-100)){
                Log.d(TAG, "found Icon");
                Bitmap bmp = BitmapFactory.decodeByteArray(e.getIcon(), 0, e.getIcon().length);
                Log.d(TAG, "x: " + x + " y: " + y);
                imageArray[x][y][1].setImageBitmap(bmp);

                // in solution eintragen
                solution[e.getX()][e.getY()] = e.getTitle();

                // longclicklistener setzen
                imageArray[x][y][1].setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View view) {
                        Log.d(TAG, "longpress on Icon " + e.getTitle());
                        Integer[] temp = new Integer[]{0, 0};
                        lastaction.add(temp);
                        ClipData.Item item = new ClipData.Item((CharSequence) view.getTag());
                        ClipData dragData = new ClipData((String) view.getTag(), new String[] {ClipDescription.MIMETYPE_TEXT_PLAIN}, item);
                        View.DragShadowBuilder myShadow = new View.DragShadowBuilder(view);
                        view.startDrag(dragData, myShadow, e, 0);
                        view.setVisibility(View.INVISIBLE);
                        return true;
                    }
                });
                imageArray[x][y][1].invalidate();
                wrote = true;
            }
            // falls e ein ordner ist und nicht im Dock sitzt
            if(e.getTag() == Entry.FOLDER && e.getContainer() == (-100)){
                Log.d(TAG, "found folder");
                Log.d(TAG, "x: " + x + " y: " + y);
                imageArray[x][y][1].setImageResource(R.drawable.portal_ring_inner_holo);

                // in solution eintragen
                solution[e.getX()][e.getY()] = "Folder";

                // longclicklistener setzen
                imageArray[x][y][1].setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View view) {
                        Log.d(TAG, "Longpress on folder");
                        Integer[] temp = new Integer[]{0, 0};
                        lastaction.add(temp);
                        ClipData.Item item = new ClipData.Item((String) view.getTag());
                        ClipData dragData = new ClipData((String) view.getTag(), new String[] {ClipDescription.MIMETYPE_TEXT_PLAIN},item);
                        View.DragShadowBuilder myShadow = new View.DragShadowBuilder(view);
                        view.startDrag(dragData, myShadow, e, 0);
                        view.setVisibility(View.INVISIBLE);
                        return true;
                    }
                });
                imageArray[x][y][1].invalidate();
                wrote = true;
            }
            // falls e ein widget ist
            if(e.getTag() == Entry.WIDGET){
                Log.d(TAG, "found Widget, calling setWidgetDrawable with x: " + x + " ,y: " + y);
                // in solution eintragen
                for(int xx = e.getX(); xx < e.getSpan_x(); xx++){
                    for(int yy = e.getY(); yy < e.getSpan_y(); yy++){
                        solution[e.getX() + xx][e.getX() + yy] = "Widget";
                    }
                }
                // Drawable setzen
                setWidgetDrawable(e.getSpan_x(), e.getSpan_y(), imageArray[x][y][1], e);
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
                Log.d(TAG, "result of test 2 for screen " + selected_screen + " :");
                Log.d(TAG, "value of input[][] after confirm: \n" +
                        input[0][0] + "\t" +  input[1][0] + "\t" +  input[2][0] + "\t" +  input[3][0] + "\n" +
                        input[0][1] + "\t" +  input[1][1] + "\t" +  input[2][1] + "\t" +  input[3][1] + "\n" +
                        input[0][2] + "\t" +  input[1][2] + "\t" +  input[2][2] + "\t" +  input[3][2] + "\n" +
                        input[0][3] + "\t" +  input[1][3] + "\t" +  input[2][3] + "\t" +  input[3][3]
                );
                Log.d(TAG, "value of solution[][] after parse: \n" +
                        solution[0][0] + "\t" + solution[1][0] + "\t" + solution[2][0] + "\t" + solution[3][0] + "\n" +
                        solution[0][1] + "\t" + solution[1][1] + "\t" + solution[2][1] + "\t" + solution[3][1] + "\n" +
                        solution[0][2] + "\t" + solution[1][2] + "\t" + solution[2][2] + "\t" + solution[3][2] + "\n" +
                        solution[0][3] + "\t" + solution[1][3] + "\t" + solution[2][3] + "\t" + solution[3][3]
                );
                int errors = 0;
                for(int i = 0; i < 4; i++){
                    for(int j = 0; j < 4; j++){
                        if(!input[i][j].equals(solution[i][j])){
                            errors++;
                        }
                    }
                }
                Log.d(TAG, "number of errors: " + errors);
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
    private void setWidgetDrawable(int x, int y, final ImageView iview, final Entry e){
        final Integer[] temp = new Integer[2];
        switch(x){
            case 1:
                switch(y){
                    case 1:
                        temp[0] = 1;
                        temp[1] = 1;
                        iview.setImageResource(R.drawable.widget_1x1);
                        iview.invalidate();
                        break;
                    case 2:
                        temp[0] = 1;
                        temp[1] = 2;
                        iview.setImageResource(R.drawable.widget_1x2);
                        iview.invalidate();
                        break;
                    case 3:
                        temp[0] = 1;
                        temp[1] = 3;
                        iview.setImageResource(R.drawable.widget_1x3);
                        iview.invalidate();
                        break;
                    case 4:
                        temp[0] = 1;
                        temp[1] = 4;
                        iview.setImageResource(R.drawable.widget_1x4);
                        iview.invalidate();
                        break;
                }
                break;
            case 2:
                switch(y){
                    case 1:
                        temp[0] = 2;
                        temp[1] = 1;
                        iview.setImageResource(R.drawable.widget_2x1);
                        iview.invalidate();
                        break;
                    case 2:
                        temp[0] = 2;
                        temp[1] = 2;
                        iview.setImageResource(R.drawable.widget_2x2);
                        iview.invalidate();
                        break;
                    case 3:
                        temp[0] = 2;
                        temp[1] = 3;
                        iview.setImageResource(R.drawable.widget_2x3);
                        iview.invalidate();
                        break;
                    case 4:
                        temp[0] = 2;
                        temp[1] = 4;
                        iview.setImageResource(R.drawable.widget_2x4);
                        iview.invalidate();
                        break;
                }
                break;
            case 3:
                switch(y){
                    case 1:
                        temp[0] = 3;
                        temp[1] = 1;
                        iview.setImageResource(R.drawable.widget_3x1);
                        iview.invalidate();
                        break;
                    case 2:
                        temp[0] = 3;
                        temp[1] = 2;
                        iview.setImageResource(R.drawable.widget_3x2);
                        iview.invalidate();
                        break;
                    case 3:
                        temp[0] = 3;
                        temp[1] = 3;
                        iview.setImageResource(R.drawable.widget_3x3);
                        iview.invalidate();
                        break;
                    case 4:
                        temp[0] = 3;
                        temp[1] = 4;
                        iview.setImageResource(R.drawable.widget_3x4);
                        iview.invalidate();
                        break;
                }
                break;
            case 4:
                switch(y){
                    case 1:
                        temp[0] = 4;
                        temp[1] = 1;
                        iview.setImageResource(R.drawable.widget_4x1);
                        iview.invalidate();
                        break;
                    case 2:
                        temp[0] = 4;
                        temp[1] = 2;
                        iview.setImageResource(R.drawable.widget_4x2);
                        iview.invalidate();
                        break;
                    case 3:
                        temp[0] = 4;
                        temp[1] = 3;
                        iview.setImageResource(R.drawable.widget_4x3);
                        iview.invalidate();
                        break;
                    case 4:
                        temp[0] = 4;
                        temp[1] = 4;
                        iview.setImageResource(R.drawable.widget_4x4);
                        iview.invalidate();
                        break;
                }
                break;
        }
        iview.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                Log.d(TAG, "longpress on widget");
                Log.d(TAG, "adding x:" + temp[0] + " y:" + temp[1] + " to lastaction" );
                lastaction.add(temp);
                ClipData.Item item = new ClipData.Item((CharSequence) view.getTag());
                ClipData dragData = new ClipData((String) view.getTag(), new String[] {ClipDescription.MIMETYPE_TEXT_PLAIN}, item);
                View.DragShadowBuilder myShadow = new View.DragShadowBuilder(view);
                view.startDrag(dragData, myShadow, e, 0);
                view.setVisibility(View.INVISIBLE);
                return true;
            }
        });
    }
}