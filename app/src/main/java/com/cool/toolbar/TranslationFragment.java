package com.cool.toolbar;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.media.Image;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.aranea_apps.android.libs.commons.NetworkUtil;
import com.aranea_apps.android.libs.commons.app.Commons;

import org.greenrobot.eventbus.EventBus;
import org.w3c.dom.Text;

import java.util.Timer;
import java.util.TimerTask;

import ru.yandex.speechkit.Recognizer;
import ru.yandex.speechkit.SpeechKit;
import ru.yandex.speechkit.gui.RecognizerActivity;

/**
 * Created by Cool on 12.04.2017.
 */

public class TranslationFragment extends Fragment {

    private TextWatcher textWatcher;
    private ImageButton btnSrcSpeak, btnSrcMic, btnSrcClear, btnDestSpeak, btnShare, btnFavorite;
    private TextView destOutText;
    private EditText srcText;
    private TranslateWord currentWord = new TranslateWord("", "ru", "en");
    private String inputText;
    final Handler handler = new Handler();
    private String HTMLDestText;
    private static final String YA_SPEECH_API_KEY = "069b6659-984b-4c5f-880e-aaedcfd84102";
    private static final int REQUEST_CODE = 31;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        Log.d("dbg", "Trans onCreate");
        super.onCreate(savedInstanceState);
//        this.setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        try {
            Log.d("dbg", "Trans onCreateView");
            View view = inflater.inflate(R.layout.fragment_translate, container, false);
            return view;
        } catch (Exception e) {
            Log.d("dbg", "transFrag onCreateView error: " + e.getMessage());
            return  null;
        }
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        try {
            super.onViewCreated(view, savedInstanceState);
            initView(view);
            // инициализация SpeechKit
            SpeechKit.getInstance().configure(Commons.getContext(), YA_SPEECH_API_KEY);
        } catch (Exception e) {
            Log.d("dbg", "transFrag onViewCreated error: " + e.getMessage());
        }
    }

    // нахожу все элементы интерфейса
    // и задаю обработчики
    private void initView(View view) {
        try {
            if (srcText == null) {
                Log.d("dbg", "srcText = null");
                srcText = (EditText) view.findViewById(R.id.etSrcText);
//            srcText.setText(currentWord.getSrcWord());
            }

            if (destOutText == null) {
                destOutText = (TextView) view.findViewById(R.id.tvOutText);
                destOutText.setMovementMethod(new ScrollingMovementMethod());

//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//                destOutText.setText(Html.fromHtml(HTMLDestText, Html.FROM_HTML_MODE_COMPACT));
//            } else {
//                destOutText.setText(Html.fromHtml(HTMLDestText));
//            }
            }

            if (btnSrcSpeak == null) {
                btnSrcSpeak = (ImageButton) view.findViewById(R.id.btnSrcSpeak);
                btnSrcSpeak.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
//                        Toast.makeText(Commons.getContext(), "SPEAK", Toast.LENGTH_SHORT).show();
                        YaSpeaker.getInstance().speak(srcText.getText().toString());
                    }
                });
            }

            if (btnSrcMic == null) {
                btnSrcMic = (ImageButton) view.findViewById(R.id.btnSrcMic);
                btnSrcMic.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (NetworkUtil.isNetworkAvailable()) {
                            // To start recognition create an Intent with required extras.
                            Intent intent = new Intent(getActivity(), RecognizerActivity.class);
                            // Specify the model for better results.
                            intent.putExtra(RecognizerActivity.EXTRA_MODEL, Recognizer.Model.QUERIES);
                            // Specify the language.
                            intent.putExtra(RecognizerActivity.EXTRA_LANGUAGE, Recognizer.Language.RUSSIAN);
                            // To get recognition results use startActivityForResult(),
                            // also don't forget to override onActivityResult().
                            startActivityForResult(intent, REQUEST_CODE);
                        } else {
                            Toast.makeText(Commons.getContext(), "Нет подключения к интернету", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }

            if (btnSrcClear == null) {
                btnSrcClear = (ImageButton) view.findViewById(R.id.btnClear);
                btnSrcClear.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        clearInput();
                    }
                });
            }

            if (btnDestSpeak == null) {
                btnDestSpeak = (ImageButton) view.findViewById(R.id.btnDestSpeak);
                btnDestSpeak.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (currentWord != null) {
                            if (currentWord.getSrcWord() != null) {
                                YaSpeaker.getInstance().speak(currentWord.getMainTranslate());
                                return;
                            }
                        }
                        // тут пасхалка. Если текст пуст - телефон сообщит об этом голосом
                        YaSpeaker.getInstance().speak("");
                    }
                });
            }

            if (btnShare == null) {
                btnShare = (ImageButton) view.findViewById(R.id.btnShare);
                btnShare.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (currentWord != null) {
                            Intent sendIntent = new Intent();
                            sendIntent.setAction(Intent.ACTION_SEND);
                            sendIntent.putExtra(Intent.EXTRA_TEXT, currentWord.toString());
                            sendIntent.setType("text/plain");
                            startActivity(Intent.createChooser(sendIntent, "Послать перевод..."));
                        }
                    }
                });
            }

            // эта кнопка именно для добавления в избранное
            // если текущее отображаемое слово уже в избранном - нажатие на кнопку ничего не изменит.
            // чтобы убрать из избранного надо снять звездочку в списке в истории
            if (btnFavorite == null) {
                btnFavorite = (ImageButton) view.findViewById(R.id.btnDestFavorite);
                btnFavorite.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (currentWord != null) {
                            if (currentWord.getId_word() >= 0){
                                try {
                                    Utils.changeWordFavorite(currentWord.getId_word(), 1);
                                    Toast.makeText(Commons.getContext(), "Добавлено в избранное", Toast.LENGTH_SHORT).show();
                                } catch (Exception e) {
                                    Log.d("dbg", e.getMessage());
                                }
                            }
                        }
                    }
                });
            }

            // компонент, определяющий когда пользователь закончил ввод
            // и что нужно начать запрашивать перевод фразы введенной
            if (textWatcher == null) {
                textWatcher = new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                    }

                    private Timer timer = new Timer();
                    private final long DELAY = 1000; // milliseconds

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        // ничего не делать если текст пустой и если текст совпадает с srcText текущего currentWord (когда он только передался как переведенный, чтоб заново не запрашивать его же)
                        if (timer != null) {
                            timer.cancel();
                        }

                        inputText = "" + s;


                        Log.d("dbg", "onTextChanged: " + s);
                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                        Log.d("dbg", "inputText: " + inputText);
                        Log.d("dbg", "Editable s: " + s.toString());

                        boolean canProcess = true;
                        canProcess = canProcess && !inputText.equals("");
                        if (currentWord == null) {
                            Log.d("dbg", "currentWord is null");
                            canProcess = canProcess && true;
                        } else {
                            Log.d("dbg", currentWord.getSrcWord());
                            if (currentWord.getSrcWord() == null) {
                                Log.d("dbg", "srcWord is null");
                                canProcess = canProcess && true;
                            } else {
                                Log.d("dbg", "currentWord.getSrcWord(): " + currentWord.getSrcWord());
                                Log.d("dbg", "input after utils: " + Utils.prepareTextToTranslate(inputText));
                                if (currentWord.getSrcWord().equals(Utils.prepareTextToTranslate(inputText))) {

                                    // если текущая фраза отличается от выведенной сейчас на экран только знаками препинания - ничего не делаем
                                    canProcess = false;
                                } else canProcess = canProcess && true;
                            }
                        }

                        if (canProcess) {
                            timer = new Timer();
                            TimerTask mTimerTask = new TimerTask() {
                                @Override
                                public void run() {
                                    Log.d("dbg", "Send to EBUS");

                                    if (inputText.length() < 999) {
                                        // через EventBus посылаю сообщение в MainActivity
                                        EventBus.getDefault().post(new EBusMessage(EBusMessage.EVENTBUS_FROM_TRANSLATE_TO_MAIN, null, Utils.prepareTextToTranslate(inputText)));
                                    } else {
                                        EventBus.getDefault().post(new EBusMessage(EBusMessage.EVENTBUS_TEXT_TOO_LONG,null, null));
                                    }

                                }
                            };
                            timer.schedule(mTimerTask, DELAY);
                        }
                    }
                };
            }

            srcText.addTextChangedListener(textWatcher);
            // заполняю текстовые элементы интерфейса
            fillData();
        }catch (Exception e) {
            Log.d("dbg", "transFrag init error: " + e.getMessage());
        }
    }

    // очищаю текстовые элементы и переменную currentWord
    // она нужна для сравнения введенного текста с предыдущим.
    private void clearInput() {
        srcText.setText("");
        destOutText.setText("");
        currentWord = null;
    }

    public void updateTranslateUI(TranslateWord tw){
        try {
        currentWord = tw;
        if(currentWord != null) {
            HTMLDestText = tw.toHTML();
        }
        // если фрагмент уже виден в момент вызова этого метода, то надо тут заполнять его данными
        fillData();
        } catch (Exception e){
            Log.d("dbg", "transFrag updateTranslateUI error: " + e.getMessage());
        }
    }

    // заношу данные в интерфейс
    private void fillData(){
        try {
            if (srcText != null) {
                if (currentWord != null) {
                    if (currentWord.getSrcWord() != null) {
                        srcText.setText(currentWord.getSrcWord());
                    }
                }
            }

            if (destOutText != null && HTMLDestText != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    destOutText.setText(Html.fromHtml(HTMLDestText, Html.FROM_HTML_MODE_COMPACT));
                } else {
                    destOutText.setText(Html.fromHtml(HTMLDestText));
                }
            }
        } catch (Exception e){
            Log.d("dbg", "transFrag fillData error: " + e.getMessage());
        }
    }


    // обрабатываю голосовой ввод
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, requestCode, data);
        if (requestCode == REQUEST_CODE) {
            if (resultCode == RecognizerActivity.RESULT_OK && data != null) {
                final String result = data.getStringExtra(RecognizerActivity.EXTRA_RESULT);
                clearInput();
                srcText.setText(result);
            } else if (resultCode == RecognizerActivity.RESULT_ERROR) {
                String error = ((ru.yandex.speechkit.Error) data.getSerializableExtra(RecognizerActivity.EXTRA_ERROR)).getString();

                Toast.makeText(Commons.getContext(), error, Toast.LENGTH_LONG).show();
            }
        }
    }

}
