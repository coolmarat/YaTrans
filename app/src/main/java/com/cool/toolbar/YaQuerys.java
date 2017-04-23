package com.cool.toolbar;

import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;

import com.aranea_apps.android.libs.commons.NetworkUtil;
import com.aranea_apps.android.libs.commons.app.Commons;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

/**
 * Created by arov on 06.04.2017.
 */

// передаю сюда массив строк в таком порядке: [0] - srcWord, [1] - srcLng, [2] - destLng

public class YaQuerys extends AsyncTask<TranslateWord, Void, TranslateWord> {
    private String dictKey = "dict.1.1.20170403T080359Z.54b289663bd06c37.d81714882bcc049259c7b8daac03c5961870046e";
    private String transKey = "trnsl.1.1.20170404T110457Z.12e8cf365a2bf041.d562de9ed83ecb4dc99b86ee8df2e5214a86cf2e";
    private String transURL = "https://translate.yandex.net/api/v1.5/tr/translate?key=KEY_PATTERN&text=TEXT_PATTERN&lang=LNG_PATTERN";
    private String dictionaryURL = "https://dictionary.yandex.net/api/v1/dicservice/lookup?key=KEY_PATTERN&lang=LNG_PATTERN&text=TEXT_PATTERN&ui=ru&flags=2";
    private boolean useTranslator;
    private TranslateVariant translateVariant;
    private String lastSynPos;
    private boolean isMean;
    private TranslateWord translateWord;
    // надо отдельно на каждом уровне сохранять последний открытый тэг
    private String lastOpenTag; // последний пройденный тэг
    private String prevOpenTag; // предпоследний открыытый тэг, чтобы понять куда относится тэг text
    private String lastPosValue;


    private WeakReference<MainActivity> mainActivityWeakReference;

    public YaQuerys(MainActivity mainActivity) {
        mainActivityWeakReference = new WeakReference<MainActivity>(mainActivity);
    }

    @Override
    protected TranslateWord doInBackground(TranslateWord... params) {
        translateWord = params[0];  // основная структура для хранения структуры перевода
        String txt = null;
        try {
            // переведенный в УТФ-8 текст подставляю в запрос, но не пишу его в базу в таком виде, а пишу оригинал
            txt = URLEncoder.encode(params[0].getSrcWord().trim(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        String readyURL;
        boolean is_main_translate_set = false;

        // проверяю, передано ли одно слово или несколько
        // надо все не буквенные символы проверить как разделители
        useTranslator = Utils.isForTranslator(translateWord.getSrcWord(), params[0].getSrcLng(), params[0].getDestLng());
        String lng = params[0].getSrcLng() + "-" + params[0].getDestLng();

        // самое первое - проверяю наличие в БД введенного текста
        translateWord = Utils.getTranslateFromDB(translateWord.getSrcWord(), params[0].getSrcLng(), params[0].getDestLng());
        if (translateWord != null) {
            // слово нашлось в базе - выдать его наружу
            if (translateWord.getVariants().size()!=0) {
                return translateWord;
            } else translateWord = params[0];
        } else translateWord = params[0];  // основная структура для хранения перевода

        // если не нашли в базе - запрашиваем яндекс.переводчик
        // но сначала проверяем наличие интернета

        if (NetworkUtil.isNetworkAvailable()) {

            if (useTranslator) {
                // подставляю ключ от переводчика
                readyURL = transURL.replace("KEY_PATTERN", transKey);
            } else {
                // ключ от словаря
                readyURL = dictionaryURL.replace("KEY_PATTERN", dictKey);
            }

            readyURL = readyURL.replace("TEXT_PATTERN", txt);
            readyURL = readyURL.replace("LNG_PATTERN", lng);

            String LOG_TAG = "dbg";
            String tmp;

            try {
                URL url = new URL(readyURL);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.setReadTimeout(7000);
                urlConnection.connect();
                BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                StringBuilder stringBuilder = new StringBuilder();
                String xmlResponse = null;
                String xmlFinal;


                while ((xmlResponse = reader.readLine()) != null) {
                    stringBuilder.append(xmlResponse + "\n");
                }

                xmlFinal = stringBuilder.toString();

                XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                factory.setNamespaceAware(false);
                XmlPullParser xpp = factory.newPullParser();
                xpp.setInput(new StringReader(xmlFinal));


                // теперь надо непосредственно парсить

                // в зависимости от того запрос к переводчику или к словарю - по-разному обрабатывать ответы
                if (useTranslator) {
                    // если переводчик (фраза без частей речи и синонимов)
                    while (xpp.getEventType() != XmlPullParser.END_DOCUMENT) {
                        switch (xpp.getEventType()) {
                            // начало документа
                            case XmlPullParser.START_DOCUMENT:
                                Log.d(LOG_TAG, "START_DOCUMENT");
                                break;
                            // начало тэга
                            case XmlPullParser.START_TAG:

                                prevOpenTag = lastOpenTag;
                                lastOpenTag = xpp.getName();

                                Log.d(LOG_TAG, "START_TAG: name = " + xpp.getName()
                                        + ", depth = " + xpp.getDepth() + ", attrCount = "
                                        + xpp.getAttributeCount());
                                tmp = "";
                                for (int i = 0; i < xpp.getAttributeCount(); i++) {
                                    tmp = tmp + xpp.getAttributeName(i) + " = "
                                            + xpp.getAttributeValue(i) + ", ";
                                }
                                if (!TextUtils.isEmpty(tmp))
                                    Log.d(LOG_TAG, "Attributes: " + tmp);
                                break;
                            // конец тэга
                            case XmlPullParser.END_TAG:

                                if (xpp.getName().equals("text")) {
                                    // надо "занулить" все вспомогательные переменные
                                    lastPosValue = "";
                                    // и добавить заполненный вариант пеервода к слову
                                    translateWord.addVariant(translateVariant);
                                }

                                Log.d(LOG_TAG, "END_TAG: name = " + xpp.getName());
                                break;
                            // содержимое тэга
                            case XmlPullParser.TEXT:

                                translateVariant = new TranslateVariant(xpp.getText());

                                if (!is_main_translate_set) {
                                    is_main_translate_set = true;
                                    translateWord.setMainTranslate(xpp.getText());
                                }

                                Log.d(LOG_TAG, "text = " + xpp.getText());
                                break;

                            default:
                                break;
                        }
                        // следующий элемент
                        xpp.next();
                    }
                    Log.d(LOG_TAG, "END_DOCUMENT");
                } else {


                    // если словарь
                    // если словарь (синонимы и значения)
                    while (xpp.getEventType() != XmlPullParser.END_DOCUMENT) {
                        switch (xpp.getEventType()) {
                            // начало документа
                            case XmlPullParser.START_DOCUMENT:
                                Log.d(LOG_TAG, "START_DOCUMENT");
                                break;
                            // начало тэга
                            case XmlPullParser.START_TAG:

                                prevOpenTag = lastOpenTag;
                                lastOpenTag = xpp.getName();


                                Log.d(LOG_TAG, "START_TAG: name = " + xpp.getName()
                                        + ", depth = " + xpp.getDepth() + ", attrCount = "
                                        + xpp.getAttributeCount());
                                tmp = "";
                                for (int i = 0; i < xpp.getAttributeCount(); i++) {
                                    tmp = tmp + xpp.getAttributeName(i) + " = "
                                            + xpp.getAttributeValue(i) + ", ";

                                    if (xpp.getAttributeName(i).equals("pos")) {
                                        lastPosValue = xpp.getAttributeValue(i);
                                    }
                                }
                                if (!TextUtils.isEmpty(tmp))
                                    Log.d(LOG_TAG, "Attributes: " + tmp);
                                break;
                            // конец тэга
                            case XmlPullParser.END_TAG:
                                Log.d(LOG_TAG, "END_TAG: name = " + xpp.getName());

                                if (xpp.getName().equals("tr") && xpp.getDepth() == 3) {
                                    // надо "занулить" все вспомогательные переменные
                                    lastPosValue = "";
                                    // и добавить заполненный вариант пеервода к слову
                                    translateWord.addVariant(translateVariant);
                                }
                                break;
                            // содержимое тэга
                            case XmlPullParser.TEXT:
                                Log.d(LOG_TAG, "text = " + xpp.getText());

                                if (lastOpenTag.equals("text") && xpp.getDepth() == 4) {
                                    // если тэг text встретился на 4 уровне вложенности значит это основное значение для варианта перевода
                                    translateVariant = new TranslateVariant(xpp.getText());
                                    translateVariant.setPartOfSpeech(lastPosValue);

                                    if (!is_main_translate_set) {
                                        is_main_translate_set = true;
                                        translateWord.setMainTranslate(xpp.getText());
                                        translateWord.setMainPartOfSpeech(lastPosValue);
                                    }
                                }

                                if (lastOpenTag.equals("text") && (xpp.getDepth() == 5) && (prevOpenTag.equals("syn"))) {
                                    // сейчас просматривается значение синонима
                                    translateVariant.addSynonim(xpp.getText());
                                }

                                if (lastOpenTag.equals("text") && (xpp.getDepth() == 5) && (prevOpenTag.equals("mean"))) {
                                    // сейчас просматривается значение перевода
                                    translateVariant.addMean(xpp.getText());
                                }
                                break;

                            default:
                                break;
                        }
                        // следующий элемент
                        xpp.next();
                    }
                    Log.d(LOG_TAG, "END_DOCUMENT");
                }


            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (XmlPullParserException e) {
                e.printStackTrace();
            }


            if (translateWord.getVariants().size() > 0) {
                // если взяли перевод из сети, то пишем все это в локальную базу чтоб потом сразу доставать
                int id_word;
                id_word = Utils.insertTranslateToDB(Commons.getContext(), translateWord);
                translateWord.setId_word(id_word);
            }
                return translateWord;
        } else {
            // если нет ни в базе ни интернета нет - то null вернем
            return null;
        }
    }

    @Override
    protected void onPostExecute(TranslateWord translateWord) {

        if (translateWord != null) {
         //   if (translateWord.getVariants().size() > 0) {
                // тут же надо вызвать обновление интерфейса
                if (mainActivityWeakReference != null) {
                    MainActivity activity = mainActivityWeakReference.get();
                    if (activity != null) {
                        activity.updateUI(translateWord);
                    }
                }
        //    }
        } else {
            if (mainActivityWeakReference != null) {
                MainActivity activity = mainActivityWeakReference.get();
                if (activity != null) {
                    Log.d("dbg", "no inet message");
                    activity.noInetToast();
                }
            }
        }

        // для отладки

//        for (Object element : translateWord.getVariants()) {
//            TranslateVariant variant = (TranslateVariant) element;
//            Log.d("dbg", variant.getDestWord());
//            Log.d("dbg", variant.getAllSynonimString());
//            Log.d("dbg", variant.getAllMeanString());
//        }
    }
}
