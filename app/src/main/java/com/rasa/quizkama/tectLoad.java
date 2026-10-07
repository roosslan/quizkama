package com.rasa.quizkama;

import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

public class tectLoad extends tectApp {
    private static final String TAG = "quizkama";

    public tectLoad(MainActivity c) {
        super(c);
    }

    /**
     * Загружает вопросы из файла, выбранного пользователем в системном диалоге.
     *
     * @param uri адрес файла (content://...)
     * @return число загруженных вопросов; 0, если загрузить не удалось (пользователь уже
     * получил сообщение о причине, а ранее загруженные вопросы остались нетронутыми).
     */
    public int ladeTest(Uri uri) {
        // Настройки перечитываем каждый раз: переключатель перемешивания мог измениться
        ta.funke.ConfLesen();

        List<tectFragen> loaded;
        int skipped;
        try (InputStream in = ta.getContentResolver().openInputStream(uri)) {
            if (in == null) {
                return fail(R.string.err_file_not_found, new FileNotFoundException(String.valueOf(uri)));
            }
            BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            Parser parser = new Parser(br);
            loaded = parser.parse();
            skipped = parser.getSkippedCount();
        } catch (FileNotFoundException e) {
            return fail(R.string.err_file_not_found, e);
        } catch (SecurityException e) {
            // доступ к файлу был отозван или не сохранился после перезапуска
            return fail(R.string.err_no_access, e);
        } catch (IOException | RuntimeException e) {
            return fail(R.string.err_file_read, e);
        }

        if (loaded.isEmpty()) {
            Toast.makeText(ta, R.string.err_no_questions, Toast.LENGTH_LONG).show();
            return 0;
        }

        int questionNumber = 0;
        for (tectFragen jedeFrage : loaded) {
            jedeFrage.checkBox = jedeFrage.Antworten.length() > 1;
            jedeFrage.frageNummer = ++questionNumber;
            Collections.shuffle(jedeFrage.Vorschlage);
        }
        if (ta.APP_PREFERENCES_SHUFFLEQ) {
            Collections.shuffle(loaded);
        }
        ta.fragenLs = loaded;
        if (skipped > 0) {
            Toast.makeText(ta, ta.getString(R.string.warn_questions_skipped, loaded.size(), skipped),
                    Toast.LENGTH_LONG).show();
        }
        return loaded.size();
    }

    private int fail(int messageId, Exception e) {
        Log.e(TAG, "Can not load question file", e);
        Toast.makeText(ta, messageId, Toast.LENGTH_LONG).show();
        return 0;
    }
}
