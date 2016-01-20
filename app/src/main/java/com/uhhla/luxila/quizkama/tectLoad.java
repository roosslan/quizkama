package com.rasa.quizkama;

import android.os.Environment;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;

public class tectLoad extends tectApp {
    public tectLoad(MainActivity c) {
        super(c);
    }

    private int qCount = 0;
    private int qIndex = 0;

    public void ladeTest(){
        File file;
        if (ta.APP_PREFERENCES_FNAME != "none"){
            ta.funke.ConfLesen();
            file = new File(ta.APP_PREFERENCES_FNAME);
        }
        else
        {
            File sdcard = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            file = new File(sdcard,"tect.txt");
        }
        StringBuilder text = new StringBuilder();
        final Parser parser;
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            parser = new Parser(br);
            String line;

            ta.fragenLs = parser.parse();
            int questionNumber = 0;
            for (tectFragen jedeFrage : ta.fragenLs) {
                if (jedeFrage.Antworten.length() > 1) { jedeFrage.checkBox = true; }

                ++questionNumber;
//                System.out.println("Question #" + ++questionNumber + ": " + jedeFrage.frageText);
                jedeFrage.frageNummer = questionNumber;
                int choiceNumber = 0;
                for (String choice : jedeFrage.Vorschlage) {
//                    choice = ((char) ((int) 'A' + choiceNumber++) + ". " + choice);
                }
                Collections.shuffle(jedeFrage.Vorschlage);
                System.out.println("++ Correct answer(s): " + jedeFrage.Antworten);
            }
           br.close();
        }
        catch (FileNotFoundException e) {
            ta.mTextMessage.setText("File not found");
            Log.e("login activity", "File not found: " + e.toString());
            e.printStackTrace();
        }
        catch (IOException e) {
            ta.mTextMessage.setText("File access denied");
            Log.e("login activity", "Can not read file: " + e.toString());
            e.printStackTrace();
        }

        if (ta.APP_PREFERENCES_SHUFFLEQ) { Collections.shuffle(ta.fragenLs); }
    }
}
