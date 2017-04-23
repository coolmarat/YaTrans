package com.cool.toolbar;

import android.content.Context;
import android.content.res.XmlResourceParser;
import android.util.Log;

import org.xmlpull.v1.XmlPullParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by arov on 06.04.2017.
 */

// первоначально получил xml со всеми языками и их сокращенными названиями и положил эти файлы в ресурсы
// так же скачал все направления перевода через "Переводчик" и через "Словарь"
// есшли будет переводиться одно слово - порбую через словарь, если нет выбранного направления перевода в словаре
// то буду через переводчик, если более одного слова в тексте для перевода - в любом случае через переводчик сразу
public class LangsDecode {
    // чтобы получать название языка по сокращению
    public Map<String, String> langsDecode;  // key = "ru"   value = "Русский"
    // чтобы при выборе в spinner'е языка найти его сокращенное название
    public Map<String, String> langsEncode;  // key = "Русский"   value = "ru"
    // направления для перевода через "Переводчик". Строки вида "ru-en"
    public ArrayList<String> translateDirections;
    // направления для перевода через "Словарь"
    public ArrayList<String> dictionaryDirections;
    private static LangsDecode instance;
    private static Context cnt;

    private LangsDecode(){
        // здесь читаю из ресурсов списки языков
//        Log.d("dbg", "init Langs");
        String key = null;
        String value = null;

        // языки для перевода текста
        String[] trans = cnt.getResources().getStringArray(R.array.TrnsDrctns);
        if (trans.length > 0) {
            translateDirections = new ArrayList<String>();
            for (String element : trans) {
                translateDirections.add(element);
            }
        }

        // языки для получения синонимов для одного слова (из Dictionary)
        String[] dic_lng = cnt.getResources().getStringArray(R.array.dic_directions);
        if (dic_lng.length > 0) {
            dictionaryDirections = new ArrayList<String>();
            for (String element: dic_lng) {
                dictionaryDirections.add(element);
            }
        }

        langsDecode = null;
        langsEncode = null;
        XmlResourceParser parser = cnt.getResources().getXml(R.xml.langs_decode);

        // читаю из ресурсов XML список всех обозначений языков с расшифровкой
        // ru-Русский
        try {
            int eventType = parser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_DOCUMENT) {
                    Log.d("dbg","Start document langs_decode");
                } else if (eventType == XmlPullParser.START_TAG) {
                    if (parser.getName().equals("langs")) {
                        // в начале XML создаем сам хэшмэп
                        langsDecode =  new HashMap<String, String>();
                        langsEncode = new HashMap<String, String>();
                    } else if (parser.getName().equals("Item")) {
                        // если открвыающийся тэг это Итем - забираю из его атрибутов значения
                        key = parser.getAttributeValue(null, "key");
                        value = parser.getAttributeValue(null, "value");
                        if (null == key) {
                            parser.close();
                        }
                    }
                } else if (eventType == XmlPullParser.END_TAG) {
                    if (parser.getName().equals("Item")) {
                        langsDecode.put(key, value);
                        langsEncode.put(value, key);
                        key = null;
                        value = null;
                    }
                }
                eventType = parser.next();
            }
        } catch (Exception e) {

        }


    }

    public static void initInstance(Context c){

        if (instance == null) {
            cnt = c;
            instance = new LangsDecode();
        }
    }

    // для того чотбы из всевозможных языков отфильтровать те с которых можно переводить
    // нужны эти две функции
    public boolean isLngInTransAsSrc(String lng) {
        for (String currentPair: translateDirections){
            if (currentPair.contains(lng + "-")) return true;
        }
        return false;
    }

    public boolean isLngInDictAsSrc(String lng) {
        for (String currentPair: dictionaryDirections){
            if (currentPair.contains(lng + "-")) return true;
        }
        return false;
    }

    public static LangsDecode getInstance(){
        return instance;
    }

    // по краткому обозначению языка получить полное
    public String getLngDescription(String srcLng) {
        String desc = "";
        desc = langsDecode.get(srcLng);
        return desc;
    }

    // по полному обозначению языка получить краткое
    public String getShortLngByDesc(String desc) {
        String shortLng = langsEncode.get(desc);
        return shortLng;
    }
}
