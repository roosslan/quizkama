package com.rasa.quizkama;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import java.util.ArrayList;

public class tectSharedFuncts extends tectApp {
    public tectSharedFuncts(MainActivity c) {
        super(c);
    }

    private static final String HEX_DIGITS = "0123456789ABCDEF";

    public static int CharToDec(String hex) {
        char[] sources = hex.toCharArray();
        int dec = 0;
        for (int i = 0; i < sources.length; i++) {
            int digit = HEX_DIGITS.indexOf(Character.toUpperCase(sources[i]));
            dec += digit * Math.pow(16, (sources.length - (i + 1)));
        }
        return dec;
    }
    public boolean ConfLesen(){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ta);
        ta.APP_PREFERENCES_SHUFFLEQ = preferences.getBoolean("shuffle_q", true);
        ta.APP_PREFERENCES_FNAME = preferences.getString("tect_fname", "none");
//        Toast.makeText(MainActivity.this, APP_PREFERENCES_FNAME, Toast.LENGTH_SHORT).show();
        return false;
    }

    public void AndereFrage(int Wohin){ // Wohin 0 - назад, 1 - вперед, 2 - в начало
        if (Wohin == 0 & ta.LaufendeFrage < 1) return;
        if (Wohin == 1 & ta.LaufendeFrage != -1) {
            if (Wohin == 1 & ta.LaufendeFrage == ta.fragenLs.size()-1) return;
        }
        ta.lLv.removeAllViews();
        switch (Wohin){
            case 0: ta.LaufendeFrage--; break;
            case 1: ta.LaufendeFrage++; break;
            case 2: ta.LaufendeFrage = 0; break;
        }
        ta.mTextMessage.setText(ta.fragenLs.get(ta.LaufendeFrage).frageText);
        ArrayList<String> varianIntern = ta.fragenLs.get(ta.LaufendeFrage).Vorschlage;
        RadioGroup wrG = new RadioGroup(ta.getApplicationContext());

        for(String va : varianIntern){
            if (ta.fragenLs.get(ta.LaufendeFrage).checkBox){
                ta.wB = new CheckBox(ta.getApplicationContext());
                ta.wB.setId(CharToDec(va.substring(0,1))); // Id = буква в начале ответа (hex To dec)
                ta.wB.setText(va);
                //wB.setText(va.substring(3));
                ta.lLv.addView(ta.wB);
            }
            else {
                ta.rB = new RadioButton(ta.getApplicationContext());
                ta.rB.setId(CharToDec(va.substring(0,1))); // Id = буква в начале ответа (hex To dec)
                ta.rB.setText(va);
                //rB.setText(va.substring(3));
                wrG.addView(ta.rB);
            }
        }  if (!ta.fragenLs.get(ta.LaufendeFrage).checkBox){ta.lLv.addView(wrG);}

        ta.btnAugen.setText(Integer.toString(ta.LaufendeFrage));
        ta.btnAugen.setEnabled(true);
        ta.btnBack.setEnabled(true);

        //if (LaufendeFrage == fragenLs.size()){ btnFwd.setEnabled(false); }
    }
}
