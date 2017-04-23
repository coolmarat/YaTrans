package com.cool.toolbar;

import android.widget.Toast;

import com.aranea_apps.android.libs.commons.NetworkUtil;
import com.aranea_apps.android.libs.commons.app.Commons;

import ru.yandex.speechkit.Error;
import ru.yandex.speechkit.Synthesis;
import ru.yandex.speechkit.Vocalizer;
import ru.yandex.speechkit.VocalizerListener;

/**
 * Created by Cool on 23.04.2017.
 */

public class YaSpeaker implements VocalizerListener {
    private Vocalizer vocalizer;
    private static YaSpeaker instance;


    private void resetVocalizer() {
        if (vocalizer != null) {
            vocalizer.cancel();
            vocalizer = null;
        }
    }

    public void speak(String srctext){
        if (NetworkUtil.isNetworkAvailable()) {
            if (srctext.trim().equals("")) {
                speak("Пока что мне нечего сказать");
            } else {
                resetVocalizer();
                vocalizer = Vocalizer.createVocalizer(Vocalizer.Language.RUSSIAN, srctext, true, Vocalizer.Voice.ERMIL);
                // Set the listener.
                vocalizer.setListener(this);
                // Don't forget to call start.
                vocalizer.start();
            }
        } else {
            Toast.makeText(Commons.getContext(), "Нет подключения к интернету", Toast.LENGTH_LONG).show();
        }
    }

    public static YaSpeaker getInstance() {
        if (instance == null) {
            instance = new YaSpeaker();
        };
        return instance;
    }


    @Override
    public void onSynthesisBegin(Vocalizer vocalizer) {

    }

    @Override
    public void onSynthesisDone(Vocalizer vocalizer, Synthesis synthesis) {

    }

    @Override
    public void onPlayingBegin(Vocalizer vocalizer) {

    }

    @Override
    public void onPlayingDone(Vocalizer vocalizer) {

    }

    @Override
    public void onVocalizerError(Vocalizer vocalizer, Error error) {
        Toast.makeText(Commons.getContext(), "Произошла ошибка: " + error.toString(), Toast.LENGTH_LONG).show();
    }
}
