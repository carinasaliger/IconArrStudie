package com.example.iconarrstudie;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.WallpaperManager;
import android.content.*;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.DragEvent;
import android.view.View;
import android.widget.*;

import java.io.ByteArrayOutputStream;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;

public class Test1_ddrop_alt extends Activity {
    // format für lastaction: {art / tag, x, y, spanx, spany, invoker_x, invoker_y)
    // invoker ist die view von der die aktion ausging
    private final static String TAG = Test1_ddrop_alt.class.getSimpleName();

    // density zum umrechnen von dp auf px

    private int selected_screen;
    private static boolean[][] in_use;
    private static String[][] input;
    private static List<Integer[]> lastaction;
    private static List<Integer[]> modified;
    private static String[][] solution;
    private static SlidingDrawer drawer;

    static final ImageView[][][] imageArray = new ImageView[8][6][2];

    @SuppressLint("NewApi")
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "Test1_ddrop_alt started!");
        super.onCreate(savedInstanceState);

        // Layout setzen
        setContentView(R.layout.activity_test1);

        // Wallpaper
        Log.d(TAG, "setting Wallpaper");
        final WallpaperManager wallpaperManager = WallpaperManager.getInstance(this);
        final Drawable wallpaperDrawable = wallpaperManager.getFastDrawable();
        RelativeLayout ll = (RelativeLayout) findViewById(R.id.main_layout);
        // Unterscheidung der verwendeten Methode nach Android-Version
        int sdk = Build.VERSION.SDK_INT;
        if(sdk < Build.VERSION_CODES.JELLY_BEAN) {
            // falls älter als Jelly Bean
            ll.setBackgroundDrawable(wallpaperDrawable);
        } else {
            // sonst die aktuelle Methode verwenden
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

        // Listen
        List<Entry> entries = new LinkedList<Entry>();
        lastaction = new LinkedList<Integer[]>();
        modified = new LinkedList<Integer[]>();

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

        drawer = (SlidingDrawer) findViewById(R.id.dock);
        RelativeLayout content = (RelativeLayout) findViewById(R.id.content);
        content.setBackgroundColor(Color.argb(120,0,0,0));

        // PackageInfos holen
        PackageManager pm = this.getPackageManager();

        // Auslesen und speichern der relevanten Werte in ContentValues
        ContentValues[] row_values = new ContentValues[c.getCount()];
        int iterator = 0;
        c.moveToFirst();
        do {
            if (c.getInt(c.getColumnIndex("screen")) == selected_screen) {
                ContentValues values = new ContentValues(10);
                values.put("title", c.getString(c.getColumnIndex("title")));
                values.put("intent", c.getString(c.getColumnIndex("intent")));
                values.put("itemType", c.getInt(c.getColumnIndex("itemType")));
                values.put("appWidgetId", c.getInt(c.getColumnIndex("appWidgetId")));
                values.put("cellX", c.getInt(c.getColumnIndex("cellX")));
                values.put("cellY", c.getInt(c.getColumnIndex("cellY")));
                values.put("spanX", c.getInt(c.getColumnIndex("spanX")));
                values.put("spanY", c.getInt(c.getColumnIndex("spanY")));
                values.put("icon", c.getBlob(c.getColumnIndex("icon")));
                values.put("container", c.getInt(c.getColumnIndex("container")));
                row_values[iterator++] = values;
            }
        } while (c.moveToNext());
        c.close();

        Log.i(TAG, "parsed " + iterator + " rows of launcher.db");

        // Füllen von entries anhand der ausgelesenen Werte
        for(ContentValues cv : row_values){
            if (cv != null){
                // Weglassen aller Elemente die nicht auf dem 4x4 Grid sind
                if (cv.getAsInteger("cellX") > 3 || cv.getAsInteger("cellY") > 3) {
                    continue;
                }
                // Extrawurst für Trebuchet
                if (cv.getAsString("title") != null && (cv.getAsInteger("itemType") != Entry.WIDGET || cv.getAsInteger("itemType") != getResources().getInteger(R.integer.WIDGET_TAG))) {
                    // Falls der Title in der launcher.db steht den Entry mit diesem erstellen
                    Entry temp = new Entry(
                            cv.getAsInteger("cellX"),
                            cv.getAsInteger("cellY"),
                            cv.getAsInteger("spanX"),
                            cv.getAsInteger("spanY"),
                            cv.getAsByteArray("icon"),
                            cv.getAsInteger("itemType"),
                            cv.getAsString("title"),
                            cv.getAsString("intent"),
                            cv.getAsInteger("container"));
                    entries.add(temp);
                    Log.d(TAG, "adding entry: " + temp.toString());
                } else if (cv.getAsInteger("itemType") == Entry.ICON ||
                        cv.getAsInteger("itemType") == Entry.FOLDER ||
                        cv.getAsInteger("itemType") == getResources().getInteger(R.integer.ICON_TAG) ||
                        cv.getAsInteger("itemType") == getResources().getInteger(R.integer.FOLDER_TAG)) {
                    // Sonst falls es ein Icon oder Ordner ist und title=null den Titel aus Intent holen
                    Intent temp_intent = null;
                    Log.d(TAG, "title is null, Intent: " + cv.getAsString("intent"));
                    try {
                        temp_intent = Intent.parseUri(cv.getAsString("intent"), 0);
                    } catch (URISyntaxException e1) {
                        e1.printStackTrace();
                    }
                    List<ResolveInfo> info_list = pm.queryIntentActivities(temp_intent, 0);
                    String appLabel = null;
                    for (ResolveInfo res : info_list){
                        Log.d(TAG, "label: " + res.loadLabel(pm));
                        if(res.loadLabel(pm) != null){
                            appLabel = (String) res.loadLabel(pm);
                        }
                    }
                    if(appLabel != null){
                        // Falls wir ein Label bekommen haben damit den Entry erstellen
                        Entry temp = new Entry(
                                cv.getAsInteger("cellX"),
                                cv.getAsInteger("cellY"),
                                cv.getAsInteger("spanX"),
                                cv.getAsInteger("spanY"),
                                cv.getAsByteArray("icon"),
                                cv.getAsInteger("itemType"),
                                appLabel,
                                cv.getAsString("intent"),
                                cv.getAsInteger("container"));
                        entries.add(temp);
                        Log.d(TAG, "adding entry: " + temp.toString());
                    }
                }
                else{
                    // hier dürften nur noch Widgets sein
                    Entry temp = new Entry(
                            cv.getAsInteger("cellX"),
                            cv.getAsInteger("cellY"),
                            cv.getAsInteger("spanX"),
                            cv.getAsInteger("spanY"),
                            cv.getAsByteArray("icon"),
                            cv.getAsInteger("itemType"),
                            cv.getAsString("title"),
                            cv.getAsString("intent"),
                            cv.getAsInteger("container"));
                    Log.d(TAG, "adding entry: " + temp.toString());
                    entries.add(temp);
                }
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
                        if(e.getTag() == Entry.ICON || e.getTag() == getResources().getInteger(R.integer.ICON_TAG)){
                            if(e.getIcon() != null){
                                icon = BitmapFactory.decodeByteArray(e.getIcon(), 0, e.getIcon().length);
                            }
                        }
                        switch(action){
                            // falls Drag gerade beginnt mögliche Felder blau färben
                            case DragEvent.ACTION_DRAG_STARTED:
                                // Unterscheidung zwischen Icons und Widgets
                                // falls ein Icon
                                if(last[0] == Entry.ICON || last[0] == getResources().getInteger(R.integer.ICON_TAG) || last[0] == Entry.FOLDER || last[0] == getResources().getInteger(R.integer.FOLDER_TAG)){
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
                                if(last[0] == Entry.WIDGET || last[0] == getResources().getInteger(R.integer.WIDGET_TAG)){
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
                                if(last[0] == Entry.ICON || last[0] == getResources().getInteger(R.integer.ICON_TAG) || last[0] == Entry.FOLDER || last[0] == getResources().getInteger(R.integer.FOLDER_TAG)){
                                    Log.d(TAG, "icon dropped on x:" + finalX + " y:" + finalY);
                                    if(!in_use[finalX][finalY]){
                                        if(e.getTag() == Entry.FOLDER || e.getTag() == getResources().getInteger(R.integer.FOLDER_TAG)){
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
                                        imageArray[last[5]][last[6]][1].setVisibility(View.INVISIBLE);
                                        imageArray[last[5]][last[6]][1].invalidate();
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
                                            // passendes icon setzen
                                            imageArray[finalX + startx][finalY + starty][1].setImageResource(R.drawable.widget_1x1);
                                            // colorfilter entfernen
                                            imageArray[finalX + startx][finalY + starty][0].clearColorFilter();
                                            // beide Imageviews neu zeichnen
                                            imageArray[finalX + startx][finalY + starty][0].invalidate();
                                            imageArray[finalX + startx][finalY + starty][1].invalidate();
                                            // In Input eintragen
                                            input[finalX + startx][finalY + starty] = "Widget";
                                            in_use[finalX + startx][finalY + starty] = true;

                                        }
                                    }
                                    imageArray[last[5]][last[6]][1].setVisibility(View.INVISIBLE);
                                    imageArray[last[5]][last[6]][1].invalidate();
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
            if((e.getTag() == Entry.ICON || e.getTag() == getResources().getInteger(R.integer.ICON_TAG)) && e.getContainer() == (-100)){
                Log.d(TAG, "found Icon " + "x: " + x + " y: " + y);
                if (e.getIcon() != null) {
                    // falls das Icon in der launcher.db steht
                    Bitmap bmp = BitmapFactory.decodeByteArray(e.getIcon(), 0, e.getIcon().length);
                    imageArray[x][y][1].setImageBitmap(bmp);
                    imageArray[x][y][1].invalidate();
                }
                // Falls dies nicht der Fall ist bild aus Intent holen
                else{
                    Intent i;
                    try {
                        // Parsen des Intents aus der launcher.db (sollte IMMER vorhanden sein)
                        i = Intent.parseUri(e.getIntent(), 0);
                        Log.d(TAG, "trying to set icon from intent");
                        // Icon in ImageView setzen
                        imageArray[x][y][1].setImageDrawable(pm.getActivityIcon(i));
                        // Zu PNG konvertieren
                        Bitmap bitmap = ((BitmapDrawable) pm.getActivityIcon(i)).getBitmap();
                        ByteArrayOutputStream stream = new ByteArrayOutputStream();
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                        // Im Entry das Icon setzen
                        e.setIcon(stream.toByteArray());
                    } catch (URISyntaxException e1) {
                        e1.printStackTrace();
                    } catch (PackageManager.NameNotFoundException e1) {
                        e1.printStackTrace();
                    }

                }

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
                        drawer.animateClose();
                        view.startDrag(dragData, myShadow, e, 0);
                        return true;
                    }
                });
                wrote = true;
            }
            // falls e ein ORDNER ist und nicht im Dock sitzt
            if((e.getTag() == Entry.FOLDER || e.getTag() == getResources().getInteger(R.integer.FOLDER_TAG)) && e.getContainer() == (-100)){
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
                        drawer.animateClose();
                        view.startDrag(dragData, myShadow, e, 0);
                        return true;
                    }
                });
                wrote = true;
            }
            // falls e ein widget ist
            if(e.getTag() == Entry.WIDGET || e.getTag() == getResources().getInteger(R.integer.WIDGET_TAG)){
                Log.d(TAG, "found Widget to draw");
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
        if (y == 4){
            for (int x_cont = x; x_cont < 8; x_cont++){
                for (int y_cont = y ; y_cont < 6; y_cont++){
                    imageArray[x_cont][y_cont][1].setVisibility(View.INVISIBLE);
                    imageArray[x_cont][y_cont][1].invalidate();
                }
            }
        }
        else{
            imageArray[x][y][1].setVisibility(View.INVISIBLE);
            imageArray[x][y][1].invalidate();
            y = 4;
            x++;
            for (int x_cont = x; x_cont < 8; x_cont++){
                for (int y_cont = y; y_cont < 6; y_cont++){
                    imageArray[x_cont][y_cont][1].setVisibility(View.INVISIBLE);
                    imageArray[x_cont][y_cont][1].invalidate();
                }
            }
        }

        // Buttonfunktionen festlegen
        Button confirm = (Button) findViewById(R.id.confirm);
        Button back = (Button) findViewById(R.id.back);
        Button clear = (Button) findViewById(R.id.clear);
        Button undo = (Button) findViewById(R.id.undo);

        confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean temp = false;
                for(int x = 0; x < 8; x++){
                    for(int y = 0; y < 2; y++){
//                        Log.d(TAG, "visibility x:" + x + " , y:" + y + " vis:" + imageArray[x][4 + y][1].getVisibility());
                        if(imageArray[x][4 + y][1].getVisibility() != View.INVISIBLE){
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
                drawer.animateClose();
                view.startDrag(dragData, myShadow, e, 0);
                return true;
            }
        });
    }
}