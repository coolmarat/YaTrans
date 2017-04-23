package com.cool.toolbar;

import com.aranea_apps.android.libs.commons.app.CommonsApp;

/**
 * Created by arov on 06.04.2017.
 */

// чтобы иметь возможность получать контекст приложения использую сторонний компонент CommonsApp
// так же с его помощью узнаю состояние подключения к интернету
public class MyApp extends CommonsApp {

    @Override
    public void onCreate() {
        super.onCreate();
        LangsDecode.initInstance(this);
    }
}
