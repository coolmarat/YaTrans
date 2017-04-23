package com.cool.toolbar;

import java.util.ArrayList;

/**
 * Created by arov on 05.04.2017.
 */

public class TranslateWord {
    private String srcWord;
    private String srcLng;
    private String destLng;
    private String mainTranslate;
    private String mainPartOfSpeech;
    private ArrayList variants;
    private boolean isProcessed;
    private int id_word;

    public int getId_word() {
        return id_word;
    }

    public void setId_word(int id_word) {
        this.id_word = id_word;
    }

    public TranslateWord(String srcWord, String srcLng, String destLng){
        this.isProcessed = false;
        this.srcWord = srcWord;
        this.srcLng = srcLng;
        this.destLng = destLng;
        this.variants = new ArrayList();
        this.mainTranslate = null;
        this.mainPartOfSpeech = null;
        this.id_word = -1;
//        tryTranslate();
    }

    public String getMainTranslate() {
        return mainTranslate;
    }

    public void setMainTranslate(String mainTranslate) {
        this.mainTranslate = mainTranslate;
    }

    public String getMainPartOfSpeech() {
        return mainPartOfSpeech;
    }

    public void setMainPartOfSpeech(String mainPartOfSpeech) {
        this.mainPartOfSpeech = mainPartOfSpeech;
    }


    public void addVariant(TranslateVariant tv){
        this.variants.add(tv);
    }

    public boolean getIsProcessed(){
        return this.isProcessed;
    }

    public ArrayList getVariants(){
        return  variants;
    }

    public String getSrcWord(){
        return this.srcWord;
    }

    public String getSrcLng(){
        return this.srcLng;
    }

    public boolean setSrcLng(String srcShortLng){
        if (LangsDecode.getInstance().langsDecode.containsKey(srcShortLng)) {
            this.srcLng = srcShortLng;
            return true;
        } else {
            return false;
        }
    }

    public String getDestLng(){
        return this.destLng;
    }

    public boolean setDestLng(String destShortLng){
        if (LangsDecode.getInstance().langsDecode.containsKey(destShortLng)){
            this.destLng = destShortLng;
            return  true;
        } else {
            return false;
        }
    }

    // эта функция собирает все данные в удобочитаемый человеком вид
    // нужна для отправки текста в другие приложения по кнопке "Поделиться" во фрагменте с переводом
    @Override
    public String toString() {
        String result =  this.srcLng + "-" + this.destLng + "\n" + this.srcWord + "\n";
        TranslateVariant translateVariant;
        for (Object tv : getVariants()) {
            translateVariant = (TranslateVariant) tv;
            result += translateVariant.getAllSynonimString() + "\n" + translateVariant.getAllMeanString() + "\n";
        }

        return result;
    }

    // эта функция формирует HTML для отображения его разными цветами если это значение из словаря
    // и может иметь несколько вариантов перевода
    public String toHTML(){
        if (getVariants().size() == 0) {
            return "";
        }

        final String GRAY_COLOR = "#7F7F7F";
        final String BLUE_COLOR = "#6DB7D9";
        final String RED_COLOR = "#EF8550";
        String result = getMainTranslate();
        if (getMainPartOfSpeech() != null) {
            if (getMainPartOfSpeech().length() > 0) {
                result += " - " + getMainPartOfSpeech();
            }
        }

        ArrayList<String> allVariants = getVariants();
        // если переводилось словарем - надо много строк формировать
        // если не для переводчика а для словаря, то форматирую
        if (!Utils.isForTranslator(srcWord, srcLng, destLng)) {
            int counter = 1;
            TranslateVariant currentVariant;
            String allMean;
            result += "<br>";
            for (Object tv: allVariants) {
                currentVariant = (TranslateVariant) tv;
                result += "<br>";
                result += Utils.coloredText("" + counter + ". ", GRAY_COLOR);
                result += Utils.coloredText(currentVariant.getAllSynonimString(), BLUE_COLOR);

                allMean = currentVariant.getAllMeanString();
                if (allMean.length() > 0) {
                    result += "<br>";
                    result += Utils.coloredText("(" + allMean + ")", RED_COLOR);
                }

                counter++;
            }
        }

        return result;
    }
}
