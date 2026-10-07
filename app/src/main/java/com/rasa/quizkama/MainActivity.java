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
import android.widget.CompoundButton;
import android.widget.Button;
import android.widget.LinearLayout;
import android.content.pm.PackageManager;
import android.widget.TextView;
import android.preference.PreferenceManager;
import java.util.ArrayList;
import java.util.List;
import android.graphics.Color;
import android.widget.Toast;
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
    public List<tectFragen> fragenLs = new ArrayList<tectFragen>();
    /** Check boxes / radio buttons of the current question; tag = choice letter. */
    public final List<CompoundButton> optionViews = new ArrayList<CompoundButton>();
    private boolean pendingStart = false;
    /** Показан ли правильный ответ на текущий вопрос (сбрасывается при смене вопроса). */
    public boolean answerRevealed = false;

    /**
     * Состояние квиза, которое переживает поворот экрана. Хранится в памяти через
     * onRetainCustomNonConfigurationInstance(), а не в Bundle: список вопросов может быть
     * большим, а Bundle ограничен примерно 1 МБ. Ссылок на Activity здесь нет, утечки не будет.
     */
    private static final class SavedQuiz {
        List<tectFragen> fragen;
        int index;
        boolean answerRevealed;
        String checkedLetters; // буквы отмеченных вариантов, например "AC"
    }

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
                if (!hasStoragePermission()) {
                    requestStoragePermission();
                    break;
                }
                OpenFileDialog fileDialog = new OpenFileDialog(this)
                        .setFilter(".*\\.txt")
                        .setOpenDialogListener(new OpenFileDialog.OpenDialogListener() {
                            @Override
                            public void OnSelectedFile(String fileName) {
                                Toast.makeText(getApplicationContext(), fileName, Toast.LENGTH_LONG).show();
                                SharedPreferences.Editor editor = getSharedPreferences(
                                        getPackageName() + "_preferences", MODE_PRIVATE).edit();
                                editor.putString("tect_fname", fileName);
                                editor.apply();
                                startQuiz();
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
                showAnswer();
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
                if (!hasStoragePermission()) {
                    pendingStart = true; // continue in onRequestPermissionsResult()
                    requestStoragePermission();
                    return;
                }
                startQuiz();
            }
        });

        // После поворота экрана Activity создаётся заново: возвращаем вопросы и позицию
        Object saved = getLastCustomNonConfigurationInstance();
        if (saved instanceof SavedQuiz) {
            restoreQuiz((SavedQuiz) saved);
        }
    }

    /** Подсвечивает правильные варианты текущего вопроса. Ничего не делает, пока файл не загружен. */
    private void showAnswer() {
        if (LaufendeFrage < 0 || LaufendeFrage >= fragenLs.size()) return;
        tectFragen frage = fragenLs.get(LaufendeFrage);
        btnAugen.setText(frage.Antworten);
        for (CompoundButton option : optionViews) {
            if (frage.Antworten.contains((String) option.getTag())) {
                option.setTextColor(Color.BLUE);
            }
        }
        answerRevealed = true;
    }

    @Override
    public Object onRetainCustomNonConfigurationInstance() {
        if (fragenLs.isEmpty() || LaufendeFrage < 0) return null; // нечего сохранять
        SavedQuiz state = new SavedQuiz();
        state.fragen = fragenLs;
        state.index = LaufendeFrage;
        state.answerRevealed = answerRevealed;
        StringBuilder checked = new StringBuilder();
        for (CompoundButton option : optionViews) {
            if (option.isChecked()) {
                checked.append((String) option.getTag());
            }
        }
        state.checkedLetters = checked.toString();
        return state;
    }

    private void restoreQuiz(SavedQuiz state) {
        fragenLs = state.fragen;
        LaufendeFrage = state.index;
        btnOpen.setVisibility(View.GONE);
        btnFwd.setEnabled(true);
        funke.AndereFrage(3); // перерисовать текущий вопрос, не меняя номер
        for (CompoundButton option : optionViews) {
            if (state.checkedLetters.contains((String) option.getTag())) {
                option.setChecked(true);
            }
        }
        if (state.answerRevealed) {
            showAnswer();
        }
    }

    private boolean hasStoragePermission() {
        return checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestStoragePermission() {
        requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE) return;
        boolean granted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
        boolean start = pendingStart;
        pendingStart = false;
        if (!granted) {
            Toast.makeText(this, R.string.err_permission_denied, Toast.LENGTH_LONG).show();
        } else if (start) {
            startQuiz();
        }
    }

    /** Loads the question file and shows the first question; does nothing visible if loading fails. */
    private void startQuiz() {
        if (Laden.ladeTest() == 0) return;
        btnOpen.setVisibility(View.GONE);
        btnFwd.setEnabled(true);
        funke.AndereFrage(2);
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
