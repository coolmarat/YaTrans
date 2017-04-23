package com.cool.toolbar;

/**
 * Created by Cool on 22.04.2017.
 */

// для представления совмещенных из нескольких таблиц БД данных
public class ItemWordModel {
    private int id;
    private String srcWord;
    private String translatedWord;
    private String srcShortLng;
    private String destShortLng;
    private int isFavorite;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getSrcWord() {
        return srcWord;
    }

    public void setSrcWord(String srcWord) {
        this.srcWord = srcWord;
    }

    public String getTranslatedWord() {
        return translatedWord;
    }

    public void setTranslatedWord(String translatedWord) {
        this.translatedWord = translatedWord;
    }

    public String getSrcShortLng() {
        return srcShortLng;
    }

    public void setSrcShortLng(String srcShortLng) {
        this.srcShortLng = srcShortLng;
    }

    public String getDestShortLng() {
        return destShortLng;
    }

    public void setDestShortLng(String destShortLng) {
        this.destShortLng = destShortLng;
    }

    public int getIsFavorite() {
        return isFavorite;
    }

    public void setIsFavorite(int isFavorite) {
        this.isFavorite = isFavorite;
    }
}
