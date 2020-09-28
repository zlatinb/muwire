package com.muwire.gui;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class Translator {

    private static Locale locale = Locale.US;
    private static ResourceBundle usBundle = ResourceBundle.getBundle("messages");
    private static ResourceBundle localeBundle;
    
    public synchronized static void setLocale(String code) {
        locale = Locale.forLanguageTag(code);
        localeBundle = ResourceBundle.getBundle("messages", locale);
    }
    
    public synchronized static String getString(String key) {
        if (localeBundle == null)
            throw new IllegalStateException("locale not initialized");
        try {
            return localeBundle.getString(key);
        } catch (MissingResourceException notTranslated) {
            return usBundle.getString(key);
        }
    }

}
