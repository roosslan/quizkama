package com.rasa.quizkama;

import android.os.Bundle;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.content.pm.PackageManager;
import android.view.MenuInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.KeyEvent;
import android.view.ViewConfiguration;
import android.widget.CompoundButton;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.preference.PreferenceManager;
import java.util.ArrayList;
import java.util.List;
import android.graphics.Color;
import android.widget.Toast;
import android.content.SharedPreferences;
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
    public int LaufendeFrage = -1;
    public List<tectFragen> fragenLs = new ArrayList<tectFragen>();
    /** Check boxes / radio buttons of the current question; tag = choice letter. */
    public final List<CompoundButton> optionViews = new ArrayList<CompoundButton>();
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

    /** Uri выбранного файла с вопросами (строкой); null, если файл ещё не выбирали. */
    public String APP_PREFERENCES_URI;
    public boolean APP_PREFERENCES_SHUFFLEQ = true;
    private SharedPreferences mSettings;

    /**
     * Системный выбор файла (Storage Access Framework). Права на память не нужны: приложение
     * получает доступ только к тому файлу, который пользователь выбрал сам.
     */
    private final ActivityResultLauncher<String[]> filePicker = registerForActivityResult(
            new ActivityResultContracts.OpenDocument() {
                @Override
                public Intent createIntent(Context context, String[] input) {
                    Intent intent = super.createIntent(context, input);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    // Просим запомнить доступ к файлу, чтобы он пережил перезапуск приложения
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                            | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
                    return intent;
                }
            },
            uri -> onFilePicked(uri));


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // if вместо switch: в AGP 8 идентификаторы ресурсов не являются константами
        int id = item.getItemId();
        if (id == R.id.menu_open) {
            openFilePicker();
        } else if (id == R.id.menu_settings) {
            startActivity(new Intent(MainActivity.this, SettingsActivity.class));
        } else if (id == R.id.options_about) {
            showAbout();
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
                // Открываем сохранённый файл; если его нет или он недоступен, предлагаем выбрать
                funke.ConfLesen();
                if (APP_PREFERENCES_URI == null || !startQuiz(Uri.parse(APP_PREFERENCES_URI))) {
                    openFilePicker();
                }
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

    /** Окно «О программе»: версия берётся из манифеста (её задаёт app/build.gradle). */
    private void showAbout() {
        String version = "?";
        try {
            version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            // своё собственное приложение всегда найдётся; оставляем «?»
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.about_title)
                .setMessage(getString(R.string.about_message, version))
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void openFilePicker() {
        // На некоторых провайдерах .txt отдаётся как octet-stream, поэтому разрешаем оба типа
        filePicker.launch(new String[]{"text/plain", "application/octet-stream"});
    }

    /** Вызывается после выбора файла в системном диалоге; uri == null, если пользователь отменил выбор. */
    private void onFilePicked(Uri uri) {
        if (uri == null) return;
        try {
            getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (SecurityException e) {
            // Провайдер не поддерживает постоянный доступ: файл откроется сейчас,
            // а после перезапуска его придётся выбрать снова
        }
        if (startQuiz(uri)) {
            // Запоминаем файл только после успешной загрузки, чтобы не хранить негодный uri
            PreferenceManager.getDefaultSharedPreferences(this)
                    .edit().putString("tect_uri", uri.toString()).apply();
            APP_PREFERENCES_URI = uri.toString();
            Toast.makeText(this, displayName(uri), Toast.LENGTH_SHORT).show();
        }
    }

    /** Имя файла для показа пользователю; если узнать не удалось, возвращает сам uri. */
    private String displayName(Uri uri) {
        try (Cursor cursor = getContentResolver().query(uri,
                new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                String name = cursor.getString(0);
                if (name != null) return name;
            }
        } catch (RuntimeException e) {
            // не критично: имя нужно только для подсказки
        }
        return uri.toString();
    }

    /**
     * Загружает вопросы из файла и показывает первый.
     *
     * @return true, если вопросы загружены; при ошибке пользователь уже получил сообщение
     */
    private boolean startQuiz(Uri uri) {
        if (Laden.ladeTest(uri) == 0) return false;
        btnOpen.setVisibility(View.GONE);
        btnFwd.setEnabled(true);
        funke.AndereFrage(2);
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        funke.ConfLesen();
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
