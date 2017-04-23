package com.cool.toolbar;

/**
 * Created by Cool on 21.04.2017.
 */

// для посылки сообщений из разных частей программы в MainActivity
public class EBusMessage {
    public String id;
    public TranslateWord tw;
    public String txt;
    public static final String EVENTBUS_FROM_TRANSLATE_TO_MAIN = "TRANSLATE_TO_MAIN";
    public static final String EVENTBUS_UPDATE_HISTORY = "UPDATE_HISTORY";
    public static final String EVENTBUS_SHOW_COMPLETE_TRANSLATE = "SHOW_COMPLETE_TRANSLATE";
    public static final String EVENTBUS_TEXT_TOO_LONG = "TEXT_TOO_LONG";


    public EBusMessage(String id, TranslateWord translateWord, String txt){
        this.id = id;
        this.tw = translateWord;
        this.txt = txt;
    }
}
