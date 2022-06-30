package com.pesterenan.i18n;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class Bundle {
    private static final String BUNDLE_NAME; //$NON-NLS-1$

    private static final ResourceBundle RESOURCE_BUNDLE;

    static {
        BUNDLE_NAME = Bundle.class.getPackageName() + ".properties";
        RESOURCE_BUNDLE = ResourceBundle.getBundle("MechPesteBundle");
    }

    private Bundle() {
    }

    public static String getString(String key) {
        try {
            return RESOURCE_BUNDLE.getString(key);
        } catch (MissingResourceException e) {
            return '!' + key + '!';
        }
    }
}
