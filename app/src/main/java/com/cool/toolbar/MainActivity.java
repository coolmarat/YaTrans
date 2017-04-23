package com.cool.toolbar;

import android.app.FragmentTransaction;
import android.os.PersistableBundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;


import com.aranea_apps.android.libs.commons.app.Commons;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;

import ru.yandex.speechkit.SpeechKit;

public class MainActivity extends AppCompatActivity 
implements UpdateUIInterface {


    ImageButton btnSwapLanguages;

    private Spinner spinnerSrc, spinnerDest;
    private BottomNavigationView navigation;
    private Toolbar toolbar;
    private EditText etSearch;
    private ImageButton btnDelete;
    ArrayList<String> srcLngsForSpinner;
    ArrayList<String> destLngsForSpinner;
    ArrayList<String> srcLngsShort, destLngsShort;
    public String srcSelectedShortLng, destSelectedShortLng, prevSrcLng, prevDestLng;
    private ArrayAdapter<String> adapterSrc;
    private ArrayAdapter<String> adapterDest;
    private TranslateWord currentWord;
    private boolean justCreated = true;
    private boolean showAllWords;
    private String filter = "";
    private HistoryFragment currentFragment;
    private TextWatcher searchWatcher;


// для реакции на нажатие кнопок навигации внизу экрана
    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_translate:
                   setInterfaceTranslate();
                    break;
                case R.id.navigation_history:
                    setInterfaceHistory();
                    break;
                case R.id.navigation_favorites:
                    setInterfaceFavorites();
                    break;
            }

            return true;
        }

    };

    private void setInterfaceTranslate() {
        try {
            FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
            fragmentTransaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
//        fragmentTransaction.setCustomAnimations(R.animator.slide_in_left, R.animator.slide_in_right);

            TranslationFragment translationFragment = new TranslationFragment();
            fragmentTransaction.replace(R.id.container, translationFragment);
            fragmentTransaction.commit();

//            translationFragment.updateTranslateUI(currentWord);
            // выставляю видимость элементов в верхнем Toolbar
            spinnerSrc.setVisibility(View.VISIBLE);
            spinnerDest.setVisibility(View.VISIBLE);
            btnSwapLanguages.setVisibility(View.VISIBLE);

            etSearch.setVisibility(View.INVISIBLE);
            btnDelete.setVisibility(View.INVISIBLE);

            if (currentWord != null) {
                //  в уже созданный существующий фрагмент пробрасываю данные, которые он прогрузит при отрисовке
                translationFragment.updateTranslateUI(currentWord);
            }
        }catch (Exception e){
            Log.d("dbg", "main setInterfaceTranslate error: " + e.getMessage());
        }
    }

    // аналогично обрабатываю показ дргих "вкладок", кнопок внизу экрана 3
    // но по факту разных фрагментов используется 2: для показа интерфейса перевода
    // и для показа истории переводов. но история переводов может отображаться вся
    // либо только избранные записи, поэтому история может отображаться одним фрагментом
    // но с разными настройками
    private void setInterfaceHistory() {
        showAllWords = true;
        HistoryFragment historyFragment = new HistoryFragment();
        currentFragment = historyFragment;
        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
        fragmentTransaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        fragmentTransaction.replace(R.id.container, historyFragment);
//        fragmentTransaction.addToBackStack("History");
        fragmentTransaction.commit();
        historyFragment.updateUI(filter, true);

        spinnerSrc.setVisibility(View.INVISIBLE);
        spinnerDest.setVisibility(View.INVISIBLE);
        btnSwapLanguages.setVisibility(View.INVISIBLE);

        etSearch.setVisibility(View.VISIBLE);
        btnDelete.setVisibility(View.VISIBLE);
    }

    private void setInterfaceFavorites() {
        showAllWords = false;
        HistoryFragment favoritesFragment = new HistoryFragment();
        currentFragment = favoritesFragment;
        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
        fragmentTransaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        fragmentTransaction.replace(R.id.container, favoritesFragment);
//        fragmentTransaction.addToBackStack("Favorites");
        fragmentTransaction.commit();
        favoritesFragment.updateUI(filter, false);

        spinnerSrc.setVisibility(View.INVISIBLE);
        spinnerDest.setVisibility(View.INVISIBLE);
        btnSwapLanguages.setVisibility(View.INVISIBLE);

        etSearch.setVisibility(View.VISIBLE);
        btnDelete.setVisibility(View.VISIBLE);
    }

    // заполнить список выбора языков на которые можно переводить
    // учитывается, что не на каждый язык можно перевести с любого языка
    // передается внутрь процедуры язык исходного текста и список
    // заполняется только поддерживаемыми для перевода языками
    private void fillDestSpinner(String shortSrcLng) {
        try {
            destLngsShort = Utils.availableDestLngs(shortSrcLng);
            destLngsForSpinner = Utils.shortLngListToDescList(destLngsShort);
            adapterDest = new ArrayAdapter<String>(MainActivity.this,
                    R.layout.list_item, destLngsForSpinner);
            adapterDest.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerDest.setAdapter(adapterDest);

            prevDestLng = destSelectedShortLng;
            // если в новом списке есть значение, которое было выбрано до изменения списка - то выставляю его
            if (destLngsForSpinner.indexOf(LangsDecode.getInstance().getLngDescription(destSelectedShortLng)) >= 0) {
                spinnerDest.setSelection(destLngsForSpinner.indexOf(LangsDecode.getInstance().getLngDescription(destSelectedShortLng)));
            } else {
                spinnerDest.setSelection(0);
                // по полному названию языка нахожу сокращение
                destSelectedShortLng = LangsDecode.getInstance().langsEncode.get(destLngsShort.get(0));
            }
        } catch (Exception e) {
            Log.d("dbg", "main fillDestSpinner error: " + e.getMessage());
        }

    }

    // установить язык исходного текста в Spinner
    // нужно для обмена значений по кнопке со стрелками,
    // а так же когда слово загружается из БД, то языки выставляются такие
    // которые были при первоначальном формировании перевода через интернет
    private String setSrcSpinnerShortLng(String shortLng){
        try {
            prevSrcLng = srcSelectedShortLng;
            if (srcLngsForSpinner.indexOf(LangsDecode.getInstance().getLngDescription(shortLng)) >= 0) {
                spinnerSrc.setSelection(srcLngsForSpinner.indexOf(LangsDecode.getInstance().getLngDescription(shortLng)));
                srcSelectedShortLng = shortLng;
            } else {
                spinnerSrc.setSelection(0);
                srcSelectedShortLng = LangsDecode.getInstance().langsEncode.get(srcLngsForSpinner.get(0));
            }
            return LangsDecode.getInstance().getShortLngByDesc(spinnerSrc.getSelectedItem().toString());
        } catch (Exception e){
            Log.d("dbg", "main setSrcSpinnerShortLng error: " + e.getMessage());
            return  null;
        }
    }

    // аналогичное для списка с языками  на которые переводить
    private String setDestSpinnerShortLng(String shortLng){
        try {
            prevDestLng = destSelectedShortLng;
            if (destLngsForSpinner.indexOf(LangsDecode.getInstance().getLngDescription(shortLng)) >= 0) {
                spinnerDest.setSelection(destLngsForSpinner.indexOf(LangsDecode.getInstance().getLngDescription(shortLng)));
                destSelectedShortLng = shortLng;
            } else {
                spinnerDest.setSelection(0);
                destSelectedShortLng = LangsDecode.getInstance().langsEncode.get(destLngsForSpinner.get(0));
            }
            return LangsDecode.getInstance().getShortLngByDesc(spinnerDest.getSelectedItem().toString());
        } catch (Exception e) {
            Log.d("dbg", "main setDestSpinnerShortLng error: " + e.getMessage());
            return null;
        }
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_main);

            // создаю объект для работы с языками
            LangsDecode.initInstance(this);

            toolbar = (Toolbar) findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);

            spinnerSrc = (Spinner) findViewById(R.id.spinner);
            spinnerDest = (Spinner) findViewById(R.id.spinner2);

            btnSwapLanguages = (ImageButton) findViewById(R.id.btnSwapLngs);


            btnSwapLanguages.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String tmpSrcLng = srcSelectedShortLng;
                    srcSelectedShortLng = setSrcSpinnerShortLng(destSelectedShortLng);
                    fillDestSpinner(srcSelectedShortLng);
                    destSelectedShortLng = setDestSpinnerShortLng(tmpSrcLng);
                    // когда изменили язык назначения - надо вновь перевести введенный текст
                    if (currentWord != null) {
                        performTranslate(currentWord.getSrcWord(), srcSelectedShortLng, destSelectedShortLng);
                    }
                }
            });

            etSearch = (EditText) findViewById(R.id.etSearch);
            searchWatcher = new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable s) {
                    filter = s.toString();
                    currentFragment.updateUI(filter, showAllWords);
                }
            };
            etSearch.addTextChangedListener(searchWatcher);

            // кнопка удаления текста из фильтра по истории
            btnDelete = (ImageButton) findViewById(R.id.btnDelete);
            btnDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    etSearch.setText("");
                }
            });

            navigation = (BottomNavigationView) findViewById(R.id.navigation);
            navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

            srcLngsShort = Utils.allShortLngs();
            srcLngsForSpinner = Utils.shortLngListToDescList(srcLngsShort);

            destLngsShort = Utils.availableDestLngs("be");
            destLngsForSpinner = Utils.shortLngListToDescList(destLngsShort);

            adapterSrc = new ArrayAdapter<String>(this, R.layout.list_item, srcLngsForSpinner);
            adapterSrc.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerSrc.setAdapter(adapterSrc);

            adapterDest = new ArrayAdapter<String>(this, R.layout.list_item, destLngsForSpinner);
            adapterDest.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerDest.setAdapter(adapterDest);

            // сразу после заполнения списков сохраняю текущие значения выбранных языков
            srcSelectedShortLng = LangsDecode.getInstance().getShortLngByDesc(spinnerSrc.getSelectedItem().toString());
            destSelectedShortLng = LangsDecode.getInstance().getShortLngByDesc(spinnerDest.getSelectedItem().toString());

            spinnerSrc.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    prevSrcLng = srcSelectedShortLng;
                    srcSelectedShortLng = LangsDecode.getInstance().getShortLngByDesc(spinnerSrc.getSelectedItem().toString());

                    fillDestSpinner(srcSelectedShortLng);
                    prevDestLng = destSelectedShortLng;
                    destSelectedShortLng = LangsDecode.getInstance().getShortLngByDesc(spinnerDest.getSelectedItem().toString());

                    // когда изменили язык исходный - надо вновь перевести введенный текст
                    if (currentWord != null&&(!destSelectedShortLng.equals(prevDestLng)||!srcSelectedShortLng.equals(prevSrcLng))) {
                        performTranslate(currentWord.getSrcWord(), srcSelectedShortLng, destSelectedShortLng);
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });

            spinnerDest.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    prevDestLng = destSelectedShortLng;
                    destSelectedShortLng = LangsDecode.getInstance().getShortLngByDesc(spinnerDest.getSelectedItem().toString());
                    // когда изменили язык назначения - надо вновь перевести введенный текст
                    if (currentWord != null&&!destSelectedShortLng.equals(prevDestLng)) {
                        performTranslate(currentWord.getSrcWord(), srcSelectedShortLng, destSelectedShortLng);
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });

            // при первоначальном запуске устанавливаю языки в списках выбора
            setSrcSpinnerShortLng("ru");
            setDestSpinnerShortLng("en");

            setInterfaceTranslate();
        } catch (Exception e) {
            Log.d("dbg", "main onCreate error: " + e.getMessage());
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        EventBus.getDefault().unregister(this);
    }

    // запуск АсинкТаска для осуществления перевода
    public void performTranslate(String srcText, String srcLng, String destLng){
        if (srcText != null) {
            TranslateWord tw = new TranslateWord(srcText, srcLng, destLng);
            currentWord = tw;
            YaQuerys myAsyncQuery = new YaQuerys(this);
            myAsyncQuery.execute(tw);
        }
    }

    // эту функцию вызывать в onPostExecute того AsyncTask который всю работу делает основную
    void updateUI(TranslateWord tw){
        try {
            currentWord = tw;
            //выставить языки из tw в toolbar
            setSrcSpinnerShortLng(tw.getSrcLng());
            // заполнить заново языки на которые можно переводить с текущего
            fillDestSpinner(srcSelectedShortLng);
            // затем установить из слова
            setDestSpinnerShortLng(tw.getDestLng());

            navigation.setSelectedItemId(R.id.navigation_translate);
        } catch (Exception e) {
            Log.d("dbg", "main updateUI error: " + e.getMessage());
        }
    }

    // здесь обрабатываю сообщения из других частей программы
    @Subscribe
    public void onEvent(EBusMessage msg){
        // приходит из фрагмента с интерфейсом перевода
        // если пользователь изменил текст в поле ввода и уже секунду ждет (закончил ввод)
        // сюда передаю введенный текст и запускаю непосредственно перевод
        if (msg.id.equals(EBusMessage.EVENTBUS_FROM_TRANSLATE_TO_MAIN)){
            if (msg.txt != null) {
                performTranslate(msg.txt, srcSelectedShortLng, destSelectedShortLng);
            }
        }

        // приходит из нажатия на элемент RecyclerView , чтобы отобразить во фрагменте с переводом выбранное слово
        // варианты перевода не запрашиваются вновь через интернет, а читаются из БД
        if (msg.id.equals(EBusMessage.EVENTBUS_SHOW_COMPLETE_TRANSLATE)) {
            if (msg.tw != null) {
                updateUI(msg.tw);
            }
        }

        // если введенный текст слишком длинный придем сюда
        if (msg.id.equals(EBusMessage.EVENTBUS_TEXT_TOO_LONG)) {
            Toast.makeText(this, "Текст слишком длинный и не будет переведен", Toast.LENGTH_LONG).show();
        }

        // если в RecyclerView нажали добавить в избранное или удалили элемент - придем сюда и обновим список
        if (msg.id.equals(EBusMessage.EVENTBUS_UPDATE_HISTORY)) {
            if (currentFragment != null) {
//                int visiblePos = ((LinearLayoutManager) currentFragment.allRecyclerView.getLayoutManager())
                currentFragment.updateUI(filter, currentFragment.modeShowAllWords);
            }
        }
    }

    // при попытке осуществить перевод без подключения к интернету, в этом случае если слово отсутствует в базе
    // АсинкТаск вызовет через EventBus эту функцию
    public void noInetToast(){
        Toast.makeText(this, "Отсутствует подключение к сети. Перевод невозможен", Toast.LENGTH_LONG).show();
    }

    @Override
    public void updateTranslateUI(TranslateWord tw) {
        updateUI(tw);
    }

}
