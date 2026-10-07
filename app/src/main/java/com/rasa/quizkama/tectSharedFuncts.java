package com.rasa.quizkama;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import java.util.ArrayList;

public class tectSharedFuncts extends tectApp {
    public tectSharedFuncts(MainActivity c) {
        super(c);
    }

    public boolean ConfLesen(){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ta);
        ta.APP_PREFERENCES_SHUFFLEQ = preferences.getBoolean("shuffle_q", true);
        ta.APP_PREFERENCES_FNAME = preferences.getString("tect_fname", "none");
        return false;
    }

    public void AndereFrage(int Wohin){ // Wohin 0 - назад, 1 - вперед, 2 - в начало, 3 - перерисовать текущий
        if (ta.fragenLs.isEmpty()) return; // nothing loaded yet (e.g. volume key / swipe at startup)
        if (Wohin == 0 && ta.LaufendeFrage < 1) return;
        if (Wohin == 1 && ta.LaufendeFrage >= ta.fragenLs.size() - 1) return;
        if (Wohin == 3 && (ta.LaufendeFrage < 0 || ta.LaufendeFrage >= ta.fragenLs.size())) return;

        ta.lLv.removeAllViews();
        ta.answerRevealed = false;
        ta.optionViews.clear();
        switch (Wohin){
            case 0: ta.LaufendeFrage--; break;
            case 1: ta.LaufendeFrage++; break;
            case 2: ta.LaufendeFrage = 0; break;
            case 3: break; // номер вопроса не меняется
        }
        tectFragen frage = ta.fragenLs.get(ta.LaufendeFrage);
        ta.mTextMessage.setText(frage.frageText);
        ArrayList<String> varianIntern = frage.Vorschlage;
        RadioGroup wrG = new RadioGroup(ta);

        for(String va : varianIntern){
            CompoundButton option = frage.checkBox ? new CheckBox(ta) : new RadioButton(ta);
            option.setTag(va.substring(0, 1)); // choice letter, used to highlight the correct answer
            option.setText(va);
            ta.optionViews.add(option);
            if (frage.checkBox) {
                ta.lLv.addView(option);
            } else {
                wrG.addView(option);
            }
        }
        if (!frage.checkBox) {
            ta.lLv.addView(wrG);
        }

        ta.btnAugen.setText(Integer.toString(ta.LaufendeFrage));
        ta.btnAugen.setEnabled(true);
        ta.btnBack.setEnabled(true);
    }
}
