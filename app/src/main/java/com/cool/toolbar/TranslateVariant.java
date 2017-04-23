package com.cool.toolbar;

import java.util.ArrayList;

/**
 * Created by arov on 05.04.2017.
 */

// ответы к словарю структурирую таким образом, что каждый отдельный вариант перевода храню в отдельном экземпляре этого класса
// у каждого варианта может быть несколько синонимов , а так же несколько значений. все это храню здесь
public class TranslateVariant {
    private ArrayList synonim;
    private ArrayList mean;
    private String destWord;
    private String partOfSpeech;

    public TranslateVariant(String mainValue){
        this.destWord = mainValue;
        synonim = new ArrayList();
        mean = new ArrayList();
    }

    public void addSynonim(String syn){
        this.synonim.add(syn);
    }

    public void addMean(String mean){
        this.mean.add(mean);
    }

    public void setPartOfSpeech(String pos){
        this.partOfSpeech = pos;
    }

    public String getPartOfSpeech() {
        return this.partOfSpeech;
    }

    public ArrayList getSynonimList(){
        return this.synonim;
    }

    public int synonimCount(){
        return this.synonim.size();
    }

    public ArrayList getMeanList(){
        return this.mean;
    }

    public int meanCount(){
        return this.mean.size();
    }

    public String getDestWord(){
        return destWord;
    }

    public String getAllSynonimString(){
        String tmp = destWord;
        if (synonimCount() > 0) {

            for (Object element : this.synonim) {
                tmp = tmp  + ", " + element.toString();
            }

        //    tmp = tmp.substring(0, tmp.length() - 2);
        }

        return tmp;
    }

    public String getAllMeanString(){
        String tmp = "";
        if (meanCount() > 0) {

            for (Object element : this.mean) {
                tmp = tmp + element.toString() + ", ";
            }

            tmp = tmp.substring(0, tmp.length() - 2);
        }

        return tmp;
    }
}
