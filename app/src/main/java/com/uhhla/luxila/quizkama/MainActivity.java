package com.rasa.quizkama;

import android.os.Bundle;
import android.preference.Preference;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.KeyEvent;
import android.view.ViewConfiguration;
import android.widget.CheckBox;
import android.widget.Button;
import android.widget.LinearLayout;
import android.content.pm.PackageManager;
import android.widget.TextView;
import android.preference.PreferenceManager;
import java.util.List;
import java.util.LinkedList;
import android.graphics.Color;
import android.widget.Toast;
import android.widget.RadioButton;
import android.content.SharedPreferences;
import android.Manifest;
import android.view.View;
import android.view.View.OnClickListener;

abstract class tectApp {
    protected MainActivity ta;
    public tectApp(MainActivity c){
        ta = c;
    }
}

public class MainActivity extends AppCompatActivity {

    tectSharedFuncts funke;
    tectLoad Laden;

    public MainActivity() {
        funke = new tectSharedFuncts(this);
        Laden = new tectLoad(this);
    }

    public TextView mTextMessage;
    public LinearLayout lLv;
    public Button btnOpen, btnBack, btnAugen, btnFwd;
    static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 1;
    public int LaufendeFrage = -1;
    public List<tectFragen> fragenLs = new LinkedList<tectFragen>();
    public CheckBox wB;
    public RadioButton rB;

    public String APP_PREFERENCES_FNAME;
    public boolean APP_PREFERENCES_SHUFFLEQ = true;
    private SharedPreferences mSettings;


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Preference pref = null;
        switch(item.getItemId())
        {
            case R.id.menu_open:
                OpenFileDialog fileDialog = new OpenFileDialog(this)
                        .setFilter(".*\\.txt")
                        .setOpenDialogListener(new OpenFileDialog.OpenDialogListener() {
                            @Override
                            public void OnSelectedFile(String fileName) {
                                Toast.makeText(getApplicationContext(), fileName, Toast.LENGTH_LONG).show();
                                SharedPreferences.Editor editor = getSharedPreferences(
                                        getPackageName() + "_preferences", MODE_PRIVATE).edit();
                                editor.putString("tect_fname", fileName);
                                editor.commit();
                                Laden.ladeTest();
                                funke.AndereFrage(2);
                            }
                        });
                fileDialog.show();
                break;
            case R.id.menu_settings:
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                break;
            case R.id.options_about:
                break;
        }
        return true;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        LinearLayout swL = (LinearLayout) findViewById(R.id.lLv);
        swL.setOnTouchListener(new OnSwipeTouchListener(MainActivity.this){
            public void onSwipeTop() {
                //Toast.makeText(MainActivity.this, "top", Toast.LENGTH_SHORT).show();
                btnAugen.callOnClick();
            }
            public void onSwipeRight() {
                Toast.makeText(MainActivity.this, "right", Toast.LENGTH_SHORT).show();
                funke.AndereFrage(0);
            }
            public void onSwipeLeft() {
                Toast.makeText(MainActivity.this, "left", Toast.LENGTH_SHORT).show();
                funke.AndereFrage(1);
            }
            public void onSwipeBottom() {
                Toast.makeText(MainActivity.this, "bottom", Toast.LENGTH_SHORT).show();
            }
        });

        mTextMessage = (TextView) findViewById(R.id.message);
        lLv = (LinearLayout) findViewById(R.id.lLv);
        btnOpen = (Button) findViewById(R.id.btnOpen);
        btnBack = (Button) findViewById(R.id.btnBack);
        btnAugen = (Button) findViewById(R.id.btnAugen);
        btnFwd = (Button) findViewById(R.id.btnFwd);


        btnAugen.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                btnAugen.setText(fragenLs.get(LaufendeFrage).Antworten);
                if (fragenLs.get(LaufendeFrage).checkBox){
                    for(int w=0;w<fragenLs.get(LaufendeFrage).Antworten.length();w++)
                    {
                        wB = (CheckBox) findViewById(funke.CharToDec(fragenLs.get(LaufendeFrage).Antworten.substring(w,w+1)));
                        wB.setTextColor(Color.BLUE);
                    }
                }
                else {
                    rB = (RadioButton) findViewById(funke.CharToDec(fragenLs.get(LaufendeFrage).Antworten.substring(0,1)));
                    rB.setTextColor(Color.BLUE);
                }
            }
        });

        btnFwd.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) { funke.AndereFrage(1);
            }
        });

        btnBack.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                funke.AndereFrage(0);
            }
        });

        btnOpen.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {

                    // Should we show an explanation?
                    //if (shouldShowRequestPermissionRationale(
                    //        Manifest.permission.READ_EXTERNAL_STORAGE)) {
                        // Explain to the user why we need to read the contacts
                    //}

                    requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                            MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);

                    // MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE is an
                    // app-defined int constant that should be quite unique

                   // return;
                }
                Laden.ladeTest();
                mTextMessage.setText(Integer.toString(fragenLs.size()));
                btnOpen.setVisibility(View.GONE);
                btnFwd.setEnabled(true);
                funke.AndereFrage(1);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        //PreferenceManager preferenceManager = getPreferenceManager();
        //if (preferenceManager.getSharedPreferences().getBoolean("pref_sync", true)){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        APP_PREFERENCES_SHUFFLEQ = preferences.getBoolean("shuffle_q", true);
        APP_PREFERENCES_FNAME = preferences.getString("tect_fname", "none");
        Toast.makeText(MainActivity.this, APP_PREFERENCES_FNAME, Toast.LENGTH_SHORT).show();
        // if (mSettings.contains(APP_PREFERENCES_COUNTER)) {
            // Получаем число из настроек
            //mCounter = mSettings.getInt(APP_PREFERENCES_COUNTER, 0);
            // Выводим на экран данные из настроек
            //mInfoTextView.setText("Я насчитал "
              //      + mCounter + " ворон");
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int action = event.getAction();
        int keyCode = event.getKeyCode();
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
                if (action == KeyEvent.ACTION_UP) {
                    if (event.getEventTime() - event.getDownTime() > ViewConfiguration.getLongPressTimeout()) {
                        Toast.makeText(MainActivity.this, "Long Prev", Toast.LENGTH_SHORT).show();
                    } else {
                        funke.AndereFrage(0);
                    }
                }
                return true;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                if (action == KeyEvent.ACTION_UP) {
                    if (event.getEventTime() - event.getDownTime() > ViewConfiguration.getLongPressTimeout()) {
                        Toast.makeText(MainActivity.this, "Long Next", Toast.LENGTH_SHORT).show();
                    } else {
                        funke.AndereFrage(1);
                    }
                }
                return true;
            default:
                return super.dispatchKeyEvent(event);
        }
    }


}
