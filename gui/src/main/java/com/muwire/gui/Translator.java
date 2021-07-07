package com.muwire.gui;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;

public class Translator {

    
    public static final Set<Locale> SUPPORTED_LOCALES = new LinkedHashSet<>();
    static {
        SUPPORTED_LOCALES.add(Locale.US);
        SUPPORTED_LOCALES.add(Locale.forLanguageTag("fr"));
        SUPPORTED_LOCALES.add(Locale.forLanguageTag("de"));
        SUPPORTED_LOCALES.add(Locale.forLanguageTag("cs"));
        SUPPORTED_LOCALES.add(Locale.forLanguageTag("ja"));
        SUPPORTED_LOCALES.add(Locale.forLanguageTag("tr"));
        SUPPORTED_LOCALES.add(Locale.forLanguageTag("sq"));
        // add more as they get translated
    }
    
    public static final List<LocaleWrapper> LOCALE_WRAPPERS = new ArrayList<>();
    static {
        for (Locale l : SUPPORTED_LOCALES)
            LOCALE_WRAPPERS.add(new LocaleWrapper(l));
    }
    
    private static Locale locale = Locale.US;
    private static ResourceBundle usBundle = ResourceBundle.getBundle("messages");
    private static ResourceBundle localeBundle;
    
    public synchronized static void setLocale(String code) {
        locale = Locale.forLanguageTag(code);
        localeBundle = ResourceBundle.getBundle("messages", locale);
    }
    
    public synchronized static String trans(String key) {
        if (localeBundle == null)
            throw new IllegalStateException("locale not initialized");
        try {
            return localeBundle.getString(key);
        } catch (MissingResourceException notTranslated) {
            return usBundle.getString(key);
        }
    }
    
    public synchronized static String trans(String key, Object... values) {
        if (localeBundle == null)
            throw new IllegalStateException("locale not initialized");
        String pattern;
        try {
            pattern = localeBundle.getString(key);
        } catch (MissingResourceException notTranslated) {
            pattern = usBundle.getString(key);
        }
        return MessageFormat.format(pattern, values);
    }
    
    public static class LocaleWrapper {
        private final Locale locale;
        LocaleWrapper(Locale locale) {
            this.locale = locale;
        }
        
        public String toString() {
            return locale.getDisplayLanguage(locale).toUpperCase(locale);
        }
    }

}
