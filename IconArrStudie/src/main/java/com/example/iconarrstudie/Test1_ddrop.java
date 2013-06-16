package com.example.iconarrstudie;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.WallpaperManager;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.DragEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SlidingDrawer;
import android.widget.Toast;
import java.util.LinkedList;
import java.util.List;

public class Test1_ddrop extends Activity {
    // format für lastaction: {art / tag, x, y, spanx, spany, invoker_x, invoker_y)
    // invoker ist die view von der die aktion ausging
    private final static String TAG = Test1_ddrop.class.getSimpleName();

    // TAGS dienen dem schnellen Anpassen auf Launcher die abgeänderte IDs für die verschiedenen Elemente verwenden (z.B. HTC Sense)
    private final int FOLDER_TAG = 3;
    private final int WIDGET_TAG = 6;
    private final int ICON_TAG = 0;

    private int selected_screen;
    private static boolean[][] in_use;
    private static boolean[][] used;
    private static String[][] input;
    private static List<Integer[]> lastaction;
    private static List<Integer[]> modified;
    private static String[][] solution;
    private static SlidingDrawer drawer;

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

    @SuppressLint("NewApi")
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "Test1_ddrop started!");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test1);

        // Wallpaper
        Log.d(TAG, "setting Wallpaper");
        final WallpaperManager wallpaperManager = WallpaperManager.getInstance(this);
        final Drawable wallpaperDrawable = wallpaperManager.getFastDrawable();
        RelativeLayout ll = (RelativeLayout) findViewById(R.id.main_layout);

        int sdk = android.os.Build.VERSION.SDK_INT;
        if(sdk < android.os.Build.VERSION_CODES.JELLY_BEAN) {
            ll.setBackgroundDrawable(wallpaperDrawable);
        } else {
            ll.setBackground(wallpaperDrawable);
        }


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
        modified = new LinkedList<Integer[]>();

        // Arrays initialisieren und füllen
        in_use = new boolean[4][4];
        solution = new String[4][4];
        input = new String[4][4];
        used = new boolean[8][2];
        for(int x = 0; x < 4; x++){
            for(int y = 0; y < 4; y++){
                in_use[x][y] = false;
                solution[x][y] = "empty";
                input[x][y] = "empty";
            }
        }
        for(int x = 0; x < 8; x++){
            for(int y = 0; y < 2; y++){
                used[x][y] = false;
            }
        }

        drawer = (SlidingDrawer) findViewById(R.id.slidingDrawer);
        RelativeLayout content = (RelativeLayout) findViewById(R.id.content);
        content.setBackgroundColor(Color.argb(120,0,0,0));

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
                //Log.d(TAG, "item_type:" + values.get(ITEM_TYPE) + "title: " + values.get(TITLE) + " x: " + values.get(CELLX) + " y: " + values.get(CELLY));
            }
        }
        c.close();

        Log.i(TAG, "parsed " + iterator + " rows of launcher.db");

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
//                Log.d(TAG, "adding entry: \n" + temp.toString());
                entries.add(temp);
            }
        }

        // Views aus R holen und in imageArray speichern

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
                        if(e.getTag() == Entry.ICON || e.getTag() == ICON_TAG){
                            icon = BitmapFactory.decodeByteArray(e.getIcon(), 0, e.getIcon().length);
                        }
                        switch(action){
                            // falls Drag gerade beginnt mögliche Felder blau färben
                            case DragEvent.ACTION_DRAG_STARTED:
                                // Unterscheidung zwischen Icons und Widgets
                                // falls ein Icon
                                if(last[0] == Entry.ICON || last[0] == ICON_TAG || last[0] == Entry.FOLDER || last[0] == FOLDER_TAG){
                                    if (!in_use[finalX][finalY]){
                                        imageArray[finalX][finalY][0].setColorFilter(Color.rgb(51, 181, 229), PorterDuff.Mode.OVERLAY);
                                        imageArray[finalX][finalY][0].invalidate();
                                        return true;
                                    }
                                    else {
                                        imageArray[finalX][finalY][0].setColorFilter(Color.rgb(255, 68, 68), PorterDuff.Mode.OVERLAY);
                                        imageArray[finalX][finalY][0].invalidate();
                                        return false;
                                    }
                                }
                                // falls ein widget
                                if(last[0] == Entry.WIDGET || last[0] == WIDGET_TAG){
                                    // falls es überstehen würde
                                    if(finalX + last[3] > 4 || finalY + last[4] > 4){
                                        imageArray[finalX][finalY][0].setColorFilter(Color.rgb(255, 68, 68), PorterDuff.Mode.OVERLAY);
                                        imageArray[finalX][finalY][0].invalidate();
                                        return false;
                                    }
                                    // falls im bereich bereits etwas liegt
                                    boolean space_used = false;
                                    for(int startx = 0; startx < last[3]; startx++){
                                        for(int starty = 0; starty < last[4]; starty++){
                                            if(in_use[finalX + startx][finalY + starty]){
                                                space_used = true;
                                            }
                                        }
                                    }
                                    if(space_used){
                                        imageArray[finalX][finalY][0].setColorFilter(Color.rgb(255, 68, 68), PorterDuff.Mode.OVERLAY);
                                        imageArray[finalX][finalY][0].invalidate();
                                        return false;
                                    }
                                    else{
                                        imageArray[finalX][finalY][0].setColorFilter(Color.rgb(51, 181, 229), PorterDuff.Mode.OVERLAY);
                                        imageArray[finalX][finalY][0].invalidate();
                                        return true;
                                    }
                                }
                                return true;
                            // falls Element über einem möglichen Feld liegt dieses grün färben
                            // if unterscheidung nicht nötig da die View schon bei ACTION_DRAG_STARTED false returnt und nicht mehr auf weitere events listent
                            case DragEvent.ACTION_DRAG_ENTERED:
                                imageArray[finalX][finalY][0].setColorFilter(Color.rgb(154, 204, 0), PorterDuff.Mode.OVERLAY);
                                imageArray[finalX][finalY][0].invalidate();
                                return true;
                            // falls Element wieder herausgezogen wird wieder blau färben
                            case DragEvent.ACTION_DRAG_EXITED:
                                imageArray[finalX][finalY][0].setColorFilter(Color.rgb(51, 181, 229), PorterDuff.Mode.OVERLAY);
                                imageArray[finalX][finalY][0].invalidate();
                                return true;
                            // falls Element in einer View losgelassen wird, das Icon übernehmen und setzen, sowie in in_use eintragen
                            case DragEvent.ACTION_DROP:
                                // prüfen ob ein icon gedroppt wurde
                                if(last[0] == Entry.ICON || last[0] == ICON_TAG || last[0] == Entry.FOLDER || last[0] == FOLDER_TAG){
                                    Log.d(TAG, "icon dropped on x:" + finalX + " y:" + finalY);
                                    if(!in_use[finalX][finalY]){
                                        if(e.getTag() == Entry.FOLDER || e.getTag() == FOLDER_TAG){
                                            imageArray[finalX][finalY][1].setImageResource(R.drawable.folder);
                                            input[finalX][finalY] = "Folder";
                                            Integer[] modification = {Entry.FOLDER, finalX, finalY, 0, 0};
                                            modified.add(modification);
                                        }
                                        else{
                                            imageArray[finalX][finalY][1].setImageBitmap(icon);
                                            input[finalX][finalY] = e.getTitle();
                                            Integer[] modification = {last[0], finalX, finalY, 0, 0};
                                            modified.add(modification);

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
                                    Integer[] modification = {Entry.WIDGET, finalX, finalY, last[3], last[4]};
                                    modified.add(modification);
                                    for(int startx = 0; startx < last[3]; startx++){
                                        for(int starty = 0; starty < last[4]; starty++){
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
                                drawer.animateOpen();
                                return true;
                        }
                        return false;
                    }
                });
            }
        }

        // Abfragen und Zeichnen der Elemente im Dock
        int x = 0;
        int y = 4;

        // Listener für Elemente im Dock setzen
        for(final Entry e : entries){
            // flag zum Anzeigen von Änderungen
            boolean wrote = false;
            // falls e ein Icon ist und direkt auf dem Desktop liegt (nicht im Dock)
            if((e.getTag() == Entry.ICON || e.getTag() == ICON_TAG) && e.getContainer() == (-100)){
                Log.d(TAG, "found Icon " + "x: " + x + " y: " + y);
                Bitmap bmp = BitmapFactory.decodeByteArray(e.getIcon(), 0, e.getIcon().length);
                imageArray[x][y][1].setImageBitmap(bmp);

                // in solution eintragen
                solution[e.getX()][e.getY()] = e.getTitle();

                // longclicklistener setzen
                final int finalX = x;
                final int finalY = y;
                imageArray[x][y][1].setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View view) {
                        Log.i(TAG, "Beginning Drag on Icon " + e.getTitle());
                        Integer[] temp = new Integer[]{e.getTag(), e.getX(), e.getY(), e.getSpan_x(), e.getSpan_y(), finalX, finalY};
                        lastaction.add(temp);
                        ClipData.Item item = new ClipData.Item((CharSequence) view.getTag());
                        ClipData dragData = new ClipData((String) view.getTag(), new String[] {ClipDescription.MIMETYPE_TEXT_PLAIN}, item);
                        View.DragShadowBuilder myShadow = new View.DragShadowBuilder(view);
                        view.startDrag(dragData, myShadow, e, 0);
                        return true;
                    }
                });
                imageArray[x][y][1].setOnDragListener(new View.OnDragListener() {
                    @Override
                    public boolean onDrag(View view, DragEvent dragEvent) {
                        int event = dragEvent.getAction();
                        // falls der drag beendet wurde
                        if(event == DragEvent.ACTION_DRAG_STARTED){
                            drawer.animateClose();
                            return true;
                        }
                        if(event == DragEvent.ACTION_DRAG_ENDED &&
                                // und er erfolgreich war
                                dragEvent.getResult() &&
                                // und das tag 0 oder 1 war
                                ((lastaction.get(lastaction.size()-1)[0] == Entry.ICON) || (lastaction.get(lastaction.size()-1)[0] == ICON_TAG)) &&
                                // und x der lastaction = diesem x
                                (lastaction.get(lastaction.size()-1)[1] == e.getX()) &&
                                // und y der lastaction = diesem y
                                (lastaction.get(lastaction.size()-1)[2] == e.getY())){
                            // setze die View (unten) unsichtbar
                            view.setVisibility(View.INVISIBLE);
                            // setze Wert in used auf true
                            used[finalX][finalY - 4] = true;
                        }
                        return true;
                    }
                });
                wrote = true;
            }
            // falls e ein ORDNER ist und nicht im Dock sitzt
            if((e.getTag() == Entry.FOLDER || e.getTag() == FOLDER_TAG) && e.getContainer() == (-100)){
                Log.d(TAG, "found folder " + "x: " + x + " y: " + y);
                imageArray[x][y][1].setImageResource(R.drawable.folder);

                // in solution eintragen
                solution[e.getX()][e.getY()] = "Folder";

                final ClipData.Item[] item = new ClipData.Item[1];
                // longclicklistener setzen
                final int finalY1 = y;
                final int finalX1 = x;
                imageArray[x][y][1].setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View view) {
                        Log.i(TAG, "Beginning Drag on folder");
                        Integer[] temp = new Integer[]{e.getTag(), e.getX(), e.getY(), e.getSpan_x(), e.getSpan_y(), finalX1, finalY1};
                        lastaction.add(temp);
                        item[0] = new ClipData.Item((String) view.getTag());
                        ClipData dragData = new ClipData((String) view.getTag(), new String[] {ClipDescription.MIMETYPE_TEXT_PLAIN}, item[0]);
                        View.DragShadowBuilder myShadow = new View.DragShadowBuilder(view);
                        view.startDrag(dragData, myShadow, e, 0);
                        return true;
                    }
                });
                imageArray[x][y][1].setOnDragListener(new View.OnDragListener() {
                    @Override
                    public boolean onDrag(View view, DragEvent dragEvent) {
                        int event = dragEvent.getAction();
                        if(event == DragEvent.ACTION_DRAG_STARTED){
                            Log.d(TAG, "In ondraglistener folder, event = " + event);
                            drawer.animateClose();

                            return true;
                        }
                        // falls der drag beendet wurde
                        if(event == DragEvent.ACTION_DRAG_ENDED &&
                                // und er erfolgreich war
                                dragEvent.getResult() &&
                                // und das tag 2 (ein ordner) war
                                ((lastaction.get(lastaction.size()-1)[0] == Entry.FOLDER) || (lastaction.get(lastaction.size()-1)[0] == FOLDER_TAG)) &&
                                // und x der lastaction = diesem x
                                (lastaction.get(lastaction.size()-1)[1] == e.getX()) &&
                                // und y der lastaction = diesem y
                                (lastaction.get(lastaction.size()-1)[2] == e.getY())){
                            // setze die View (unten) unsichtbar
                            view.setVisibility(View.INVISIBLE);
                            // setze Wert in used auf true
                            used[finalX1][finalY1 - 4] = true;
                        }
                        return true;
                    }
                });
                wrote = true;
            }
            // falls e ein widget ist
            if(e.getTag() == Entry.WIDGET || e.getTag() == WIDGET_TAG){
                // in solution eintragen
                for(int xx = 0; xx < e.getSpan_x(); xx++){
                    for(int yy = 0; yy < e.getSpan_y(); yy++){
                        solution[e.getX() + xx][e.getY() + yy] = "Widget";
                    }
                }
                // Drawable setzen
                setWidgetDrawable(e.getSpan_x(), e.getSpan_y(), imageArray[x][y][1], e.getX(), e.getY(), x, y, e);
                wrote = true;
            }
            // counter MAGIC
            if(wrote){
                imageArray[x][y][1].invalidate();
                if(y == 4){
                    y++;
                }
                else{
                    x++;
                    y = 4;

                }
            }
        }

        for(int x2 = x; x2 < 8; x2++){
            if(x2 > x){
                for(int y2 = 0; y2 < 2; y2++){
                    used[x2][y2] = true;
                }
            }
            else{
                used[x2][y - 4] = true;
            }
        }

        // Buttonfunktionen festlegen
        Button confirm = (Button) findViewById(R.id.confirm);
        Button back = (Button) findViewById(R.id.back);
        Button clear = (Button) findViewById(R.id.clear);
        Button undo = (Button) findViewById(R.id.undo);
        Button handle = (Button) findViewById(R.id.handle);

        confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean temp = false;
                for(int x = 0; x < 8; x++){
                    for(int y = 0; y < 2; y++){
                        if(!used[x][y]){
                            temp = true;
                        }
                    }
                }
                if(temp){
                    Log.d(TAG, "invoked finish without distributing all elements");
                    Toast.makeText(getApplicationContext(), "Es sind noch nicht alle Elemente verteilt. ", Toast.LENGTH_LONG).show();
                    return;
                }
                //TODO: compare & save data
                Log.i(TAG, "result of test 2 for screen " + selected_screen + " :");
                Log.i(TAG, "value of input[][] after confirm: \n" +
                        input[0][0] + "\t" +  input[1][0] + "\t" +  input[2][0] + "\t" +  input[3][0] + "\n" +
                        input[0][1] + "\t" +  input[1][1] + "\t" +  input[2][1] + "\t" +  input[3][1] + "\n" +
                        input[0][2] + "\t" +  input[1][2] + "\t" +  input[2][2] + "\t" +  input[3][2] + "\n" +
                        input[0][3] + "\t" +  input[1][3] + "\t" +  input[2][3] + "\t" +  input[3][3]
                );
                Log.i(TAG, "value of solution[][] after parse: \n" +
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
        clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "clear button pressed");
                for(int x = 0; x < 4; x++){
                    for(int y = 0; y < 4; y++){
                        imageArray[x][y][1].setImageResource(R.drawable.contour);
                        imageArray[x][y][1].invalidate();
                        in_use[x][y] = false;
                        input[x][y] = "empty";
                    }
                }
                lastaction.clear();
                for(int x = 0; x < 4; x++){
                    for(int y = 4; y < 6; y++){
                        imageArray[x][y][1].setVisibility(View.VISIBLE);
                    }
                }
            }
        });
        undo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "undoing latest activity");
                if(lastaction.size() <= 0){
                    //TOAST
                    Toast.makeText(getApplicationContext(), R.string.no_more_elements, Toast.LENGTH_LONG).show();
                    return;
                }

                // letzen Eintrag in lastaction holen
                Integer[] last = lastaction.get(lastaction.size() - 1);

                Integer[] modification;
                if(modified.size() <=0){
                    modification = new Integer[]{null, null, null, null, null};
                }
                else{
                    modification = modified.get(modified.size() - 1);

                }

                // falls es ein Widget war
                if(last[0] == Entry.WIDGET && modification[0] == null){
                    // Sichtbarkeit im Dock wiederherstellen
                    imageArray[last[5]][last[6]][1].setVisibility(View.VISIBLE);
                    // setze Wert in used auf false
                    used[last[5]][last[6] - 4] = false;

                    // aus lastaction entfernen
                    lastaction.remove(last);
                    return;
                }
                if(last[0] == Entry.WIDGET){
                    Log.d(TAG, "found widget to clean");
                    for(int x = 0; x < last[3]; x++){
                        for(int y = 0; y < last[4]; y++){
                            // Grid säubern
                            Log.d(TAG, "resetting x:" + (last[1] + x) + ", y:" + (last[2] +y));
                            imageArray[modification[1] + x][modification[2] + y][1].setImageResource(R.drawable.contour);
                            imageArray[modification[1] + x][modification[2] + y][1].invalidate();
                            // Arrays bereinigen
                            in_use[modification[1] + x][modification[2] + y] = false;
                            input[modification[1] + x][modification[2] + y] = "empty";
                        }
                    }
                }
                else{
                    Log.d(TAG, "found something else to clean");
                    // Grid säubern
                    Log.d(TAG, "resetting x:" + last[1] + ", y:" + last[2]);
                    imageArray[modification[1]][modification[2]][1].setImageResource(R.drawable.contour);
                    imageArray[modification[1]][modification[2]][1].invalidate();
                    // Arrays bereinigen
                    in_use[modification[1]][modification[2]] = false;
                    input[modification[1]][modification[2]] = "empty";
                }

                // Sichtbarkeit im Dock wiederherstellen
                imageArray[last[5]][last[6]][1].setVisibility(View.VISIBLE);
                // setze Wert in used auf false
                used[last[5]][last[6] - 4] = false;

                // aus lastaction entfernen
                lastaction.remove(last);
                modified.remove(modification);
            }
        });
    }
    private void setWidgetDrawable(int span_x, int span_y, final ImageView iview, int x, int y, final int invoker_x, final int invoker_y,  final Entry e){
        final Integer[] temp = new Integer[7];
        temp[0] = Entry.WIDGET;
        temp[1] = x;
        temp[2] = y;
        temp[5] = invoker_x;
        temp[6] = invoker_y;
        switch(span_x){
            case 1:
                switch(span_y){
                    case 1:
                        temp[3] = 1;
                        temp[4] = 1;
                        iview.setImageResource(R.drawable.widget_1x1);
                        break;
                    case 2:
                        temp[3] = 1;
                        temp[4] = 2;
                        iview.setImageResource(R.drawable.widget_1x2);
                        break;
                    case 3:
                        temp[3] = 1;
                        temp[4] = 3;
                        iview.setImageResource(R.drawable.widget_1x3);
                        break;
                    case 4:
                        temp[3] = 1;
                        temp[4] = 4;
                        iview.setImageResource(R.drawable.widget_1x4);
                        break;
                }
                break;
            case 2:
                switch(span_y){
                    case 1:
                        temp[3] = 2;
                        temp[4] = 1;
                        iview.setImageResource(R.drawable.widget_2x1);
                        break;
                    case 2:
                        temp[3] = 2;
                        temp[4] = 2;
                        iview.setImageResource(R.drawable.widget_2x2);
                        break;
                    case 3:
                        temp[3] = 2;
                        temp[4] = 3;
                        iview.setImageResource(R.drawable.widget_2x3);
                        break;
                    case 4:
                        temp[3] = 2;
                        temp[4] = 4;
                        iview.setImageResource(R.drawable.widget_2x4);
                        break;
                }
                break;
            case 3:
                switch(span_y){
                    case 1:
                        temp[3] = 3;
                        temp[4] = 1;
                        iview.setImageResource(R.drawable.widget_3x1);
                        break;
                    case 2:
                        temp[3] = 3;
                        temp[4] = 2;
                        iview.setImageResource(R.drawable.widget_3x2);
                        break;
                    case 3:
                        temp[3] = 3;
                        temp[4] = 3;
                        iview.setImageResource(R.drawable.widget_3x3);
                        break;
                    case 4:
                        temp[3] = 3;
                        temp[4] = 4;
                        iview.setImageResource(R.drawable.widget_3x4);
                        break;
                }
                break;
            case 4:
                switch(span_y){
                    case 1:
                        temp[3] = 4;
                        temp[4] = 1;
                        iview.setImageResource(R.drawable.widget_4x1);
                        break;
                    case 2:
                        temp[3] = 4;
                        temp[4] = 2;
                        iview.setImageResource(R.drawable.widget_4x2);
                        break;
                    case 3:
                        temp[3] = 4;
                        temp[4] = 3;
                        iview.setImageResource(R.drawable.widget_4x3);
                        break;
                    case 4:
                        temp[3] = 4;
                        temp[4] = 4;
                        iview.setImageResource(R.drawable.widget_4x4);
                        break;
                }
                break;
        }
        iview.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                Log.i(TAG, "longpress on widget");
                lastaction.add(temp);
                ClipData.Item item = new ClipData.Item((CharSequence) view.getTag());
                ClipData dragData = new ClipData((String) view.getTag(), new String[] {ClipDescription.MIMETYPE_TEXT_PLAIN}, item);
                View.DragShadowBuilder myShadow = new View.DragShadowBuilder(view);
                view.startDrag(dragData, myShadow, e, 0);
                return true;
            }
        });
        iview.setOnDragListener(new View.OnDragListener() {
            @Override
            public boolean onDrag(View view, DragEvent dragEvent) {
                int event = dragEvent.getAction();
                if (event == DragEvent.ACTION_DRAG_STARTED) {
                    drawer.animateClose();
                    return true;
                }
                if (event == DragEvent.ACTION_DRAG_ENDED){
                    Log.d(TAG, "Drag event on Widget ended, result" + dragEvent.getResult() + " , lastaction:" + lastaction.get(lastaction.size()-1) .toString());
                }
                // falls der drag beendet wurde
                if (event == DragEvent.ACTION_DRAG_ENDED &&
                        // und er erfolgreich war
                        dragEvent.getResult() &&
                        // und das tag 4 (ein widget) war
                        ((lastaction.get(lastaction.size() - 1)[0] == Entry.WIDGET) || (lastaction.get(lastaction.size() - 1)[0] == WIDGET_TAG))&&
                        // und x der lastaction = diesem x
                        (lastaction.get(lastaction.size() - 1)[1] == e.getX()) &&
                        // und y der lastaction = diesem y
                        (lastaction.get(lastaction.size() - 1)[2] == e.getY())) {
                    // setze die View (unten) unsichtbar
                    Log.d(TAG, "setting view for x:" + e.getX() + ", y:" + e.getY() + " invisible");
                    view.setVisibility(View.INVISIBLE);
                    // setze Wert in used auf true
                    used[invoker_x][invoker_y - 4] = true;
                    return true;
                }
                return true;
            }
        });
    }
}