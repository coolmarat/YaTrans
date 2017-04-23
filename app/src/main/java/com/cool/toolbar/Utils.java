package com.cool.toolbar;

import android.annotation.TargetApi;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.widget.Spinner;

import com.aranea_apps.android.libs.commons.app.Commons;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Cool on 11.04.2017.
 */

// вспомогательный класс, содержащий служебный процедуры
public class Utils {
    // проверим, предназначен ли переданный текст для переводчика или для словаря
    public static boolean isForTranslator(String src, String srcLng, String destLng) {
        if (src.length() > 100 ) {
            // если текст длинный - сразу в переводчик его (не в dictionary)
            return true;
        }
        String nonLetters = "[~`!@#$%^&*()_+-=;:'\\|,<.>/?\\s\\d]";
        Pattern pattern = Pattern.compile(nonLetters);
        Matcher matcher = pattern.matcher(src);

        boolean matcherRes, dictionaryDestRes;

        matcherRes = matcher.find();  // если нашелся какой-то символ специальный - то это для переводчика, а не словаря
        ArrayList<String> dictLangs = LangsDecode.getInstance().dictionaryDirections;
        dictionaryDestRes = dictLangs.contains(srcLng + "-" + destLng);

        if (dictionaryDestRes) {
            // если есть направление перевода для словаря, то смотреть есть ли спецсимволы и если есть, то это в переводчик
            return matcherRes;
        } else {
            // если текущее направление перевода отсутствует для словаря - значит только в переводчике работать
            return true;
        }
    }

    // список языков, на которые можно переводить с переданного параметром языка
    public static ArrayList<String> availableDestLngs(String srcLng) {
        List<String> destLngs = new ArrayList<String>();

        for (String element : LangsDecode.getInstance().translateDirections ){
            if (element.contains(srcLng + "-")){
                // так как пары язкоы разделены "-"
                destLngs.add(element.substring(element.indexOf("-") + 1));
            }
        }

        return (ArrayList<String>) destLngs;
    }

    // сформировать список полных названий языков из переданного списка сокращений
    public static ArrayList<String> shortLngListToDescList(ArrayList<String> shortLngsList){
        // сначала получить список расшифровок языков из аббревиатур
        // затем отсортировать его

        List<String> decoded = new ArrayList<String>();
        String decodedItem;
        String shortDestLng;
        for (String shortLng: shortLngsList) {
            shortDestLng = "";
            decodedItem = LangsDecode.getInstance().getLngDescription(shortLng);
            if (! decodedItem.equals("")) {
                decoded.add(decodedItem);
            }

        }

        Collections.sort(decoded);

        return (ArrayList<String>) decoded;
    }

    // список всех сокращений языков
    public static ArrayList<String> allShortLngs(){
        List<String> myList = new ArrayList<String>();
        LangsDecode myLangs = LangsDecode.getInstance();
        for (String lng: myLangs.langsDecode.keySet()){
            if (myLangs.isLngInTransAsSrc(lng)||myLangs.isLngInDictAsSrc(lng)) {
                myList.add(lng);
            }
        }

        return (ArrayList<String>) myList;
    }


    // ищу в базе слово по переданному исходному тексту и направлению перевода
    // возвращаю готовую структуру многослойную свою с переводом, если такое найдено
    public static TranslateWord getTranslateFromDB(String srcText, String lngFrom, String lngTo){
        int id_word, id_translation;
        TranslateWord resultTW = new TranslateWord(srcText, lngFrom, lngTo);
        TranslateVariant translateVariant;
        boolean is_main_translate_set = false;
        String main_translate;
        String main_partOfSpeech;

        DBHelper dbHelper = new DBHelper(Commons.getContext());
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try {
            Cursor cursor = db.query(DBHelper.WORD_TABLE, new String[]{"id"}, "srctext = ? and srclng = ? and destlng = ?", new String[]{srcText, lngFrom, lngTo}, null, null, null);
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    // по id слова достаю всю остальную структуру перевода
                    id_word = cursor.getInt(cursor.getColumnIndex("id"));
                    resultTW.setId_word(id_word);
                    // вытаскиваю из базы все варианты перевода
                    Cursor cVariant = db.query(DBHelper.TRANSLATION_TABLE, new String[]{"id", "partofspeech", "desttext"}, "id_word = ?", new String[]{String.valueOf(id_word)}, null, null, null);
                    if (cVariant != null) {
                        if (cVariant.moveToFirst()) {
                            do {
                                if (!is_main_translate_set) {
                                    is_main_translate_set = true;
                                    main_translate = cVariant.getString(cVariant.getColumnIndex("desttext"));
                                    main_partOfSpeech = cVariant.getString(cVariant.getColumnIndex("partofspeech"));
                                    resultTW.setMainTranslate(main_translate);;
                                    resultTW.setMainPartOfSpeech(main_partOfSpeech);
                                }

                                translateVariant = new TranslateVariant(cVariant.getString(cVariant.getColumnIndex("desttext")));
                                translateVariant.setPartOfSpeech(cVariant.getString(cVariant.getColumnIndex("partofspeech")));
                                id_translation = cVariant.getInt(cVariant.getColumnIndex("id"));

                                // выбираю все синонимы текущего перевода
                                Cursor cSynonim = db.query(DBHelper.SYNONIM_TABLE, new String[]{"syntext"}, "id_translation = ?", new String[]{String.valueOf(id_translation)}, null, null, null);
                                if (cSynonim != null) {
                                    if (cSynonim.moveToFirst()) {
                                        do{
                                            translateVariant.addSynonim(cSynonim.getString(cSynonim.getColumnIndex("syntext")));
                                        } while (cSynonim.moveToNext());
                                    }
                                }

                                // выбираю все значения текущего перевода
                                Cursor cMean = db.query(DBHelper.MEAN_TABLE, new String[]{"meantext"}, "id_translation = ?", new String[]{String.valueOf(id_translation)}, null, null, null);
                                if (cMean != null) {
                                    if (cMean.moveToFirst()) {
                                        do{
                                            translateVariant.addMean(cMean.getString(cMean.getColumnIndex("meantext")));
                                        } while (cMean.moveToNext());
                                    }
                                }

                                // добавляю к структуре текущий заполненный перевод
                                resultTW.addVariant(translateVariant);
                            } while (cVariant.moveToNext());
                        }
                    }
                } else {
                    dbHelper.close();
                    return null;}

            }
        } catch (Exception e){
            e.printStackTrace();
            return  null;
        }


        dbHelper.close();
        return resultTW;
    }

    // вставляю переведенную структуру в базу, возвращаю id главного элемента
    public static int insertTranslateToDB(Context ctx, TranslateWord tw) {
        try {
            long id_word, id_translate;
            String curSynonim, curMean;
            ContentValues cv = new ContentValues();
            DBHelper dbHelper = new DBHelper(ctx);
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            cv.put("srctext", tw.getSrcWord());
            cv.put("srclng", tw.getSrcLng());
            cv.put("destlng", tw.getDestLng());
            cv.put("maintranslate", tw.getMainTranslate());
            if (tw.getMainPartOfSpeech() != null) {
                cv.put("main_partofspeech", tw.getMainPartOfSpeech());
            }

            id_word = db.insert("srcword", null, cv);

            for (Object curTranslation : tw.getVariants()) {
                TranslateVariant translateVariant = (TranslateVariant) curTranslation;
                cv = new ContentValues();
                cv.put("id_word", id_word);
                cv.put("partofspeech", translateVariant.getPartOfSpeech());
                cv.put("desttext", translateVariant.getDestWord());

                id_translate = db.insert("translation", null, cv);

                // пишу в базу все синонимы текущего перевода
                for (Object currrentSynonim : translateVariant.getSynonimList()) {
                    curSynonim = currrentSynonim.toString();
                    cv = new ContentValues();
                    cv.put("id_translation", id_translate);
                    cv.put("syntext", curSynonim);

                    db.insert("synonim", null, cv);


                }

                // пишу в базу все значения текущего перевода
                for (Object currrentMean : translateVariant.getMeanList()) {
                    curMean = currrentMean.toString();
                    cv = new ContentValues();
                    cv.put("id_translation", id_translate);
                    cv.put("meantext", curMean);

                    db.insert("mean", null, cv);
                }
            }

            dbHelper.close();
            return (int) id_word;
        } catch (Exception e) {
            Log.d("dbg", e.getMessage());
            e.printStackTrace();
            return -1;
        }
    }

    // получаю фильтрованный список для отображения в RecyclerView
    public static List<ItemWordModel> getDataFromDB(String filter, boolean showAll){
        List<ItemWordModel> modelList = new ArrayList<ItemWordModel>();
        String query;
        if (!filter.equals("")) {
            query = "select * from " + DBHelper.WORD_TABLE +
                    " where ((srctext like '%" + filter + "%') or (maintranslate like '%" + filter + "%'))";
        } else{
            query = "select * from " + DBHelper.WORD_TABLE + " where (1 = 1)";
        }

        // если показывать не все а только избранное, то добавляю условие
        if (!showAll) {
            query += " and (isfavorite = 1)";
        }


        DBHelper dbHelper = new DBHelper(Commons.getContext());
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        Cursor cursor = db.rawQuery(query, null);

        if (cursor.moveToFirst()) {
            do {
                ItemWordModel model = new ItemWordModel();
                model.setId(cursor.getInt(cursor.getColumnIndex("id")));
                model.setSrcWord(cursor.getString(cursor.getColumnIndex("srctext")));
                model.setTranslatedWord(cursor.getString(cursor.getColumnIndex("maintranslate")));
                model.setSrcShortLng(cursor.getString(cursor.getColumnIndex("srclng")));
                model.setDestShortLng(cursor.getString(cursor.getColumnIndex("destlng")));
                model.setIsFavorite(cursor.getInt(cursor.getColumnIndex("isfavorite")));

                modelList.add(model);
            } while (cursor.moveToNext());
        }

        cursor.close();
        dbHelper.close();

        Log.d("dbg", modelList.toString());
        return modelList;
    }

    // установить значени избранности для слова зная его id
    public static void changeWordFavorite(int id_word, int newFavValue){
        DBHelper dbHelper = new DBHelper(Commons.getContext());
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("isfavorite", newFavValue);
        db.update(DBHelper.WORD_TABLE,
                cv,
                "id = ?",
                new String[] {String.valueOf(id_word)});
        dbHelper.close();
    }

    // удалить слово из базы по id
    public static void deleteWordById(int id_word) {
        DBHelper dbHelper = new DBHelper(Commons.getContext());
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete(DBHelper.WORD_TABLE, "id = ?", new String[] {String.valueOf(id_word)});
        dbHelper.close();
    }

    // удаляю из текста все знаки препинания и длинные пробелы заменяю одинарными чтобы красиво отображалось
    public static String prepareTextToTranslate(String srcText) {
        // заменяю все знаки препинания на пробелы
        String tmp = srcText.replaceAll("[^\\p{L}\\p{Nd}]+", " ");
        // заменить подстроки только из пробелов на один пробел
        return tmp.trim().replaceAll(" +", " ");
    }

    // для формирования HTML цветного текста перевода
    public static String coloredText(String txt, String HTMLColorString){
        return "<font color='" + HTMLColorString + "'>" + txt + "</font>";
    }


}
