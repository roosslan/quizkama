package com.rasa.quizkama;

import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
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
     * Loads the questions from the file chosen in the preferences (or Download/tect.txt).
     *
     * @return number of loaded questions; 0 if nothing could be loaded (the user has been
     * told why, and the previously loaded questions, if any, are left untouched).
     */
    public int ladeTest() {
        // Always re-read preferences: the file name may have just been changed by the
        // "Open..." dialog, after onResume() already ran.
        ta.funke.ConfLesen();

        File file;
        if ("none".equals(ta.APP_PREFERENCES_FNAME)) {
            File downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            file = new File(downloads, "tect.txt");
        } else {
            file = new File(ta.APP_PREFERENCES_FNAME);
        }

        List<tectFragen> loaded;
        int skipped;
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            Parser parser = new Parser(br);
            loaded = parser.parse();
            skipped = parser.getSkippedCount();
        } catch (FileNotFoundException e) {
            return fail(R.string.err_file_not_found, e);
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
