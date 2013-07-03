package com.example.iconarrstudie;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.WallpaperManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
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
import android.widget.RelativeLayout;

import java.io.ByteArrayOutputStream;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class Test3_pos extends Activity {
    // TAG für Logs
    private final static String TAG = Test3_pos.class.getSimpleName();

    private static int selected_screen;
    static boolean[][] input;
    static boolean[][] answer;

    @SuppressLint("NewApi")
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "Test3_pos started");
        setContentView(R.layout.activity_test3);
        super.onCreate(savedInstanceState);
        PackageManager pm = getPackageManager();

        // Wallpaper
        Log.d(TAG, "setting Wallpaper");
        final WallpaperManager wallpaperManager = WallpaperManager.getInstance(this);
        final Drawable wallpaperDrawable = wallpaperManager.getFastDrawable();
        RelativeLayout ll = (RelativeLayout) findViewById(R.id.main_layout);
        // Unterscheidung der verwendeten Methode nach Android-Version
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

        // Listen für zufällige Abfrage später
        List<Entry> entries = new LinkedList<Entry>();
        List<Entry> generated = new LinkedList<Entry>();
        List<Entry> correct_answers = new LinkedList<Entry>();

        // Arrays zum auswerten der Antworten
        input = new boolean[4][4];
        answer = new boolean[4][4];

        // boolean[][][] mit false initialisiern
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
                if (cv.getAsInteger("cellX") > 3 || cv.getAsInteger("cellY") > 3 || cv.getAsInteger("container") != (-100)) {
                    continue;
                }
                // Extrawurst für Trebuchet (title = null)
                if (cv.getAsString("title") != null) {
                    // Falls der Title in der launcher.db steht den Entry mit diesem erstellen
                    Entry temp = new Entry(
                            cv.getAsInteger("cellX"), cv.getAsInteger("cellY"), cv.getAsInteger("spanX"), cv.getAsInteger("spanY"), cv.getAsByteArray("icon"), cv.getAsInteger("itemType"), cv.getAsString("title"), cv.getAsString("intent"), cv.getAsInteger("container"));
                    entries.add(temp);
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
                    for (ResolveInfo res : info_list) {
                        Log.d(TAG, "label: " + res.loadLabel(pm));
                        if (res.loadLabel(pm) != null) {
                            appLabel = (String) res.loadLabel(pm);
                        }
                    }
                    if (appLabel != null) {
                        // Falls wir ein Label bekommen haben damit den Entry erstellen
                        Entry temp = new Entry(
                                cv.getAsInteger("cellX"), cv.getAsInteger("cellY"), cv.getAsInteger("spanX"), cv.getAsInteger("spanY"), cv.getAsByteArray("icon"), cv.getAsInteger("itemType"), appLabel, cv.getAsString("intent"), cv.getAsInteger("container"));
                        entries.add(temp);
                    }
                }
            }
        }

        // generated füllen
        int[] icon_library = {
                R.drawable.amazon, R.drawable.barcodescanner, R.drawable.chrome, R.drawable.dropbox, R.drawable.ebay, R.drawable.facebook, R.drawable.firefox, R.drawable.instagram, R.drawable.maps, R.drawable.messenger, R.drawable.oeffi, R.drawable.opera, R.drawable.shazam, R.drawable.skype, R.drawable.spotify, R.drawable.templerun, R.drawable.tuneinpro, R.drawable.twitter, R.drawable.whatsapp, R.drawable.youtube
        };

        String[] string_library = {
                "Amazon", "Barcode Scanner", "Chrome", "Dropbox", "eBay", "Facebook", "Firefox", "Instagram", "Maps", "Messenger", "Öffi Verbindungen", "Opera", "Shazam", "Skype", "Spotify", "Temple Run", "TuneIn Radio Pro", "Twitter", "WhatsApp", "YouTube"
        };

        // icons der Library in generated einfügen
        for(int i = 0; i < icon_library.length; i++){
            Bitmap bitmap = BitmapFactory.decodeResource(this.getResources(), icon_library[i]);
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            byte[] bitmapdata = stream.toByteArray();
            generated.add(new Entry(0, 0, 0, 0, bitmapdata, Entry.GENERATED, string_library[i], null, 0));
        }

        // vergleichen von entries mit bibliothek generated auf duplikate
        List<Entry> toRemove = new LinkedList<Entry>();
        for (Entry e : entries) {
            // falls entry ein Icon beschreibt
            if (e.getTag() == Entry.ICON || e.getTag() == getResources().getInteger(R.integer.ICON_TAG)) {
                // Zunächst: parsen aller Icons aus dem Intent falls sie noch nicht in der db sind
                if (e.getIcon() == null){
                    Intent temp_intent;
                    Bitmap bitmap = null;
                    // Intent parsen und bitmap daraus holen
                    try {
                        temp_intent = Intent.parseUri(e.getIntent(), 0);
                        bitmap = ((BitmapDrawable) pm.getActivityIcon(temp_intent)).getBitmap();
                    } catch (URISyntaxException e1) {
                        e1.printStackTrace();
                    } catch (PackageManager.NameNotFoundException e1) {
                        e1.printStackTrace();
                    }
                    // Zu png konvertieren
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                    // Schreiben des Icons in den Entry für spätere Verwendung
                    e.setIcon(stream.toByteArray());
                }
                // Prüfen ob icon in Bildbibliothek vorhanden
                for (int i = 0; i < generated.size(); i++) {
                    // falls vorhanden
                    if (e.getTitle().equals(generated.get(i).getTitle())) {
                        // aus bibliothek entfernen
                        generated.remove(generated.get(i));
                    }
                }
            }
            // falls entry ein Widget beschreibt
            if (e.getTag() == Entry.WIDGET || e.getTag() == getResources().getInteger(R.integer.WIDGET_TAG)) {
                // das Widget zum Entfernen markieren
                toRemove.add(e);
            }
        }

        // markierte Elemente löschen
        for(Entry e : toRemove){
            entries.remove(e);
        }

        // Zufallszahl für Anzahl der anzuzeigenden richtigen Lösungen
        int random;
        if(entries.size() < 1){
            random = 0;
        }
        else{
            // Zufallszahl zwischen 1 und Anzahl der Icons und Ordner auf Screen
            random = 1 + (int)(Math.random() * ((entries.size() - 1) + 1));
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
                if (temp.getTag() == Entry.FOLDER || temp.getTag() == getResources().getInteger(R.integer.FOLDER_TAG)){
                    Log.d(TAG, "drawing folder, title: " + temp.getTitle() + " to x: " + x + ", y: " + y);
                    imageArray[x][y][1].setImageResource(R.drawable.folder);
                    // falls an x,y in echt auch ein ordner liegt
                    for(Entry e : entries){
                        if((e.getTag() == Entry.FOLDER || e.getTag() == getResources().getInteger(R.integer.FOLDER_TAG)) && e.getX() == x && e.getY() == y){
                            answer[x][y] = true;
                        }
                    }
                }
                else{
                    Log.d(TAG, "drawing icon with tag " + temp.getTag() + " , title: " + temp.getTitle() + " to x: " + x + ", y: " + y);
                    Bitmap bmp = BitmapFactory.decodeByteArray(temp.getIcon(), 0, temp.getIcon().length);
                    imageArray[x][y][1].setImageBitmap(bmp);
                }
                imageArray[x][y][1].invalidate();
                entries.remove(temp);
            }
        }
        // correct answers zeichnen
        for (Entry e : correct_answers){
            Log.i(TAG, "number of correct answers: " + correct_answers.size());
            // falls ein icon gezeichnet werden soll
            if(e.getTag() == Entry.ICON || e.getTag() == getResources().getInteger(R.integer.ICON_TAG)){
                // Icon setzen (steht an dieser Stelle bereits im Entry
                Bitmap bmp = BitmapFactory.decodeByteArray(e.getIcon(), 0, e.getIcon().length);
                imageArray[e.getX()][e.getY()][1].setImageBitmap(bmp);
            }
            // falls ein Ordner gezeichnet werden soll
            if(e.getTag() == Entry.FOLDER || e.getTag() == getResources().getInteger(R.integer.FOLDER_TAG)){
                imageArray[e.getX()][e.getY()][1].setImageResource(R.drawable.folder);
            }
            imageArray[e.getX()][e.getY()][1].invalidate();
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
                        if(!input[finalX][finalY]){
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