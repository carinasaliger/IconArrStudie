package com.example.iconarrstudie;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.*;
import java.util.List;

public class MainActivity extends Activity {
    // TAG für Logs
    private static final String TAG = MainActivity.class.getSimpleName();
    // globale Variablen
    private static String AUTHORITY;
    private static String PACKAGE_NAME;
    private static int selected_screen;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.i(TAG, "onCreate()");

        // Views aus R holen und anpassen
        TextView debug_infos = (TextView) findViewById(R.id.text_debug);
        Button button_test1 = (Button) findViewById(R.id.button_ddrop);
        Button button_test2 = (Button) findViewById(R.id.button_pverbrauch);
        Button button_test3 = (Button) findViewById(R.id.button_pos);
        Button button_save = (Button) findViewById(R.id.button_save);

        // Spinner für Screenauswahl anpassen
        Spinner screen_auswahl = (Spinner) findViewById(R.id.spinner_screen);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.strings_screens, android.R.layout.simple_spinner_dropdown_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        screen_auswahl.setAdapter(adapter);
        screen_auswahl.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                selected_screen = Integer.parseInt(adapterView.getItemAtPosition(i).toString());
                Log.d(TAG, "changed selected screen to: " + (selected_screen));
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        // neue Datenbank erstellen
        final LauncherDBHandler dbHandler = new LauncherDBHandler(this);
        SQLiteDatabase database = dbHandler.getWritableDatabase();

        // ContentResolver für Debug Anzeige
        ContentResolver cr = this.getContentResolver();

        //test
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        ResolveInfo resolveInfo = getPackageManager().resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
        PACKAGE_NAME = resolveInfo.activityInfo.packageName;
        Log.d(TAG, "current home package = " + PACKAGE_NAME);

        // verantwortliche Authority finden und Cursor für Datenbank erstellen
        AUTHORITY = getAuthorityFromPermission(this, "launcher.permission.READ_SETTINGS");
        Log.d(TAG, "found AUTHORITY: " + AUTHORITY);
        final Uri content_uri = Uri.parse("content://" + AUTHORITY + "/favorites?notify=true");
        final Cursor c = cr.query(content_uri, null, null, null, null);
        if (c != null) {
            c.moveToFirst();
        }

        //debug_infos füllen
        debug_infos.setText(DatabaseUtils.dumpCursorToString(c));
        debug_infos.setMovementMethod(new ScrollingMovementMethod());


        //OnClickListener setzen
        button_test1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i1 = new Intent(MainActivity.this, Test1_ddrop.class);
                i1.putExtra("uri", content_uri.toString());
                i1.putExtra("screen", selected_screen);
                Log.i(TAG, "starting Test1_ddrop");
                startActivity(i1);
            }
        });

        button_test2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i2 = new Intent(MainActivity.this, Test2_pverbrauch.class);
                i2.putExtra("uri", content_uri.toString());
                i2.putExtra("screen", selected_screen);
                Log.i(TAG, "starting Test2_pverbrauch");
                startActivity(i2);
            }
        });

        button_test3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i3 = new Intent(MainActivity.this, Test3_pos.class);
                i3.putExtra("uri", content_uri.toString());
                i3.putExtra("screen", selected_screen);
                Log.i(TAG, "starting Test3_pos");
                startActivity(i3);
            }
        });

        button_save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "calling print / send ");
                Log.i(TAG, DatabaseUtils.dumpCursorToString(c));
                // neues Intent zum Teilen des Cursordumps
//                Intent i4 = new Intent(android.content.Intent.ACTION_SEND);
//                i4.setType("text/plain");
//                i4.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
//                i4.putExtra(Intent.EXTRA_SUBJECT, "launcher.db, Authority: " + AUTHORITY);
//                i4.putExtra(Intent.EXTRA_TEXT, DatabaseUtils.dumpCursorToString(c));
//                i4.putExtra(Intent.EXTRA_EMAIL, new String[]{"jlouisgao@gmail.com"});
//                startActivity(Intent.createChooser(i4, getString(R.string.choose_mail)));

            }
        });
    }

    //Methode zum Finden der Authority aus der Permission, übernommen aus http://stackoverflow.com/questions/8501306/android-shortcut-access-launcher-db
    private static String getAuthorityFromPermission(Context context, String permission){
        if (permission == null){
            return null;
        }
        List<PackageInfo> packs = context.getPackageManager().getInstalledPackages(PackageManager.GET_PROVIDERS);
        if (packs != null){
            for (PackageInfo pack : packs){
                ProviderInfo[] providers = pack.providers;
                if (providers != null){
                    for (ProviderInfo provider : providers){
//                        Log.d(TAG, "providername: " + provider.packageName + " , providerpermission: " + provider.readPermission);
//                        Log.d(TAG, "permission.equals(provider.readPermission):  " + permission.equals(provider.readPermission));
//                        Log.d(TAG, "provider.packageName.equals(PACKAGE_NAME): " + provider.packageName.equals(PACKAGE_NAME));
                        if (provider.readPermission != null) {
                            if(provider.readPermission.contains(permission) && provider.packageName.equals(PACKAGE_NAME)){
                                return provider.authority;
                            }
                        }
                        if (provider.writePermission != null) {
                            if(provider.writePermission.contains(permission) && provider.packageName.equals(PACKAGE_NAME)){
                                return provider.authority;
                            }
                        }
                    }
                }
            }
        }
        return "nothing found";
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
}
