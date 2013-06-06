package com.example.iconarrstudie;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
 * Created by john-louis on 17.05.13.
 */
public class Test3_pos extends Activity {
    private final static String TAG = Test3_pos.class.getSimpleName();
    private static int selected_screen;
    static boolean[][] input;
    static boolean[][] answer;
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
        Log.i(TAG, "Test3_pos started");
        setContentView(R.layout.activity_test3);
        super.onCreate(savedInstanceState);

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

        // Listen für zufällige Abfrage später
        List<Entry> entries = new LinkedList<Entry>();
        List<Entry> generated = new LinkedList<Entry>();
        List<Entry> correct_answers = new LinkedList<Entry>();

        final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        final List<ResolveInfo> pkgAppsList = this.getPackageManager().queryIntentActivities( mainIntent, 0);

        Drawable[] icon_library = new Drawable[pkgAppsList.size()];
        String[] string_library = new String[pkgAppsList.size()];

        int library_counter = 0;
        for(ResolveInfo r :  pkgAppsList){
            icon_library[library_counter] = r.loadIcon(this.getPackageManager());
            string_library[library_counter] = (String) r.loadLabel(this.getPackageManager());
            library_counter++;
        }

        // Arrays zum auswerten der Antworten
        input = new boolean[4][4];
        answer = new boolean[4][4];

        // boolean[][][] mit false initialisiern
        for(int x = 0; x < 4; x++){
            for(int y = 0; y < 4; y++){
                answer[x][y] = false;
                input[x][y] = false;
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

        Log.i(TAG, "parsed " + iterator + " rows of launcher.db");

        // Füllen von entries anhand der ausgelesenen Werte
        for(ContentValues cv : row_values){
            if (cv != null){
                if(cv.getAsInteger(CELLX) > 3 || cv.getAsInteger(CELLY) > 3 || cv.getAsInteger(CONTAINER) != (-100)){
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
                entries.add(temp);
            }
        }

        for(int i = 0; i < icon_library.length; i++){
            Bitmap bitmap = ((BitmapDrawable) icon_library[i]).getBitmap();
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            byte[] bitmapdata = stream.toByteArray();
            generated.add(new Entry(0, 0, 0, 0, bitmapdata, Entry.GENERATED, string_library[i], 0));
        }

        // vergleichen von entries mit bibliothek generated auf duplikate
        List<Entry> toRemove = new LinkedList<Entry>();
        for(int j = 0; j<entries.size(); j++){
            Entry e = entries.get(j);
            int x = e.getX();
            int y = e.getY();
            // falls entry ein Icon beschreibt
            if(e.getTag() == 1 || e.getTag() == 0){
                // falls icon in Bildbibliothek vorhanden
                for(int i = 0; i < generated.size(); i++){
                    if(e.getTitle().equals(generated.get(i).getTitle())){
                        // aus bibliothek entfernen
                        generated.remove(generated.get(i));
                    }
                }
            }
            // falls entry ein Widget beschreibt
            if(e.getTag() == Entry.WIDGET){
                // das Widget entfernen
                toRemove.add(e);
            }
        }
        for(Entry e : toRemove){
            entries.remove(e);
        }

        // prüfen ob genug entries vorhanden sind
        if(entries.size() < 2){
            Toast.makeText(getApplicationContext(), R.string.not_enough_elements, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Zufallszahl zwischen 1 und 4
        int random;
        if(iterator < 4){
            random = 1 + (int)(Math.random() * ((iterator - 1) + 1));
        }
        else{
            random = 1 + (int)(Math.random() * ((4 - 1) + 1));
        }
        Log.d(TAG, "random = " + random);

        // für zufallszahl einträge aus entries auswählen und in correct_answers eintragen
        for(int asd = 0; asd < random; asd++){
            Entry temp = returnRandomElement(entries);
            correct_answers.add(temp);
            entries.remove(temp);
        }

        // entries und generated joinen, sollte danach größer als 16 sein
        entries.addAll(generated);

        // Views aus R holen und in imageArray speichern
        Log.d(TAG, "creating and filling ImageView-Array");
        final ImageView[][][] imageArray= new ImageView[4][4][2];
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

        // zufällig zeichnen
        for(int x = 0; x < 4; x++){
            for(int y = 0; y < 4; y++){
                Entry temp = returnRandomElement(entries);
                // falls temp ein Ordner ist
                if (temp.getTag() == Entry.FOLDER){
                    Log.d(TAG, "drawing folder, title: " + temp.getTitle() + " to x: " + x + ", y: " + y);
                    imageArray[x][y][1].setImageResource(R.drawable.folder);
                    imageArray[x][y][1].invalidate();
                    entries.remove(temp);
                    // falls an x,y in echt auch ein ordner liegt
                    for(Entry e : entries){
                        if(e.getTag() == Entry.FOLDER && e.getX() == x && e.getY() == y){
                            answer[x][y] = true;
                        }
                    }
                }
                else{
                    Log.d(TAG, "drawing icon with tag " + temp.getTag() + " , title: " + temp.getTitle() + " to x: " + x + ", y: " + y);
                    Bitmap bmp = BitmapFactory.decodeByteArray(temp.getIcon(), 0, temp.getIcon().length);
                    imageArray[x][y][1].setImageBitmap(bmp);
                    imageArray[x][y][1].invalidate();
                    entries.remove(temp);
                }
            }
        }
        // correct answers zeichnen
        for (Entry e : correct_answers){
            Log.i(TAG, "number of correct answers: " + correct_answers.size());
            if(e.getTag() == Entry.ICON || e.getTag() == 0){
                Bitmap bmp = BitmapFactory.decodeByteArray(e.getIcon(), 0, e.getIcon().length);
                imageArray[e.getX()][e.getY()][1].setImageBitmap(bmp);
                imageArray[e.getX()][e.getY()][1].invalidate();
            }
            if(e.getTag() == Entry.FOLDER){
                imageArray[e.getX()][e.getY()][1].setImageResource(R.drawable.folder);
                imageArray[e.getX()][e.getY()][1].invalidate();
            }
            answer[e.getX()][e.getY()] = true;
        }

        // OnClickListener setzen
        for(int x = 0; x < 4; x++){
            for(int y = 0; y < 4; y++){
                final int finalX = x;
                final int finalY = y;
                imageArray[x][y][1].setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if(imageArray[finalX][finalY][0].getColorFilter() == null){
                            Log.d(TAG, "registered FIRST keypress on: x= " + String.valueOf(finalX) + "\t" + "y= " + String.valueOf(finalY));
                            // Farbe auf blau setzen
                            imageArray[finalX][finalY][0].setColorFilter(Color.rgb(51, 181, 229));
                            input[finalX][finalY] = true;
                        }
                        else{
                            Log.d(TAG, "registered SECOND keypress on: x= " + String.valueOf(finalX) + "\t" + "y= " + String.valueOf(finalY));
                            imageArray[finalX][finalY][0].setColorFilter(null);
                            input[finalX][finalY] = false;
                        }
                    }
                });
            }
        }

        Button confirm = (Button) findViewById(R.id.confirm);
        Button back = (Button) findViewById(R.id.back);
        confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //TODO: compare & save data
                Log.i(TAG, "result of test 3 for screen " + selected_screen + " :");
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

    // übernommen aus http://stackoverflow.com/questions/124671/picking-a-random-element-from-a-set
    private Entry returnRandomElement(List<Entry> input){
        Log.d(TAG, "returnRandomElement called with inputlist size: " + input.size());
        int size = input.size();
        int item = new Random().nextInt(size);
        int i = 0;
        for(Entry e : input){
            if (i == item)
                return e;
            i = i + 1;
        }
        return null;
    }
}