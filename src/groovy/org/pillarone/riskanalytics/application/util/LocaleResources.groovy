package org.pillarone.riskanalytics.application.util

import org.pillarone.riskanalytics.core.user.Person
import org.pillarone.riskanalytics.core.user.UserManagement
import org.pillarone.riskanalytics.core.user.UserSettings
import org.pillarone.riskanalytics.core.util.ResourceBundleRegistry
import com.canoo.ulc.community.locale.server.ULCClientLocaleSetter
import com.ulcjava.base.application.ClientContext
import java.text.DateFormat
import java.text.NumberFormat
import java.text.SimpleDateFormat
import org.pillarone.riskanalytics.application.UserContext
import org.pillarone.riskanalytics.application.ui.util.UIUtils
import org.pillarone.riskanalytics.application.util.prefs.UserPreferencesFactory
import org.pillarone.riskanalytics.application.util.prefs.UserPreferences

/**
 * This class provides properties from resource bundles using client-specific internationalization.
 * <p/>
 * Note: This class is not synchronized and should only be called from the ULC Thread.
 *
 * @author Dierk.Koenig@canoo.com
 */
class LocaleResources {

    private static final String BUNDLE_FILENAME = "org.pillarone.riskanalytics.application.applicationResources"
    private static boolean sTestMode
    private static final String LOCALE = "SESSION_LOCAL_LOCALE"

    static String getString(String key) {
        getBundle(BUNDLE_FILENAME).getString(key)
    }

    static ResourceBundle getBundle(String bundleFilename) {
        ResourceBundleFactory.getBundle(bundleFilename, getLocale())
    }

    static Set getBundles(String key) {
        def resourceBundle = []
        def resources = ResourceBundleRegistry.getBundles(key)
        for (String bundleName in resources) {
            resourceBundle << ResourceBundle.getBundle(bundleName, getLocale(), Thread.currentThread().contextClassLoader)
        }
        return resourceBundle
    }

    static Locale getLocale() {
        if (sTestMode) {
            return Locale.getDefault()
        }
        Locale locale = (Locale) UserContext.getAttribute(LOCALE)

        if (locale == null) {
            Person.withTransaction {e ->
                UserSettings userSettings = UserContext.getCurrentUser()?.settings
                UserPreferences preferences = UserPreferencesFactory.getUserPreferences()
                if (userSettings != null) {
                    locale = buildLocale(userSettings)
                    UserContext.setAttribute(LOCALE, locale)
                } else if (preferences.getLanguage() != null) {
                    locale = new Locale(preferences.getLanguage())
                    UserContext.setAttribute(LOCALE, locale)
                }
            }
            if (locale == null) {
                try {
                    locale = ClientContext.getLocale()
                    if (locale == null) {
                        locale = new Locale("en", "US")
                    }
                } catch (Exception e) {
                    locale = new Locale("en", "US")
                }

                UserContext.setAttribute(LOCALE, locale)
            }
            ULCClientLocaleSetter.setDefaultLocale(locale)
        }

        return locale
    }

    static Locale buildLocale(UserSettings userSettings) {
        String language = userSettings.language
        String country = ''
        if (language.contains('_')) {
            String[] parts = language.split('_')
            language = parts[0]
            country = parts[1]
        }
        return new Locale(language, country)
    }


    static NumberFormat getNumberFormat() {
        NumberFormat.getInstance(UIUtils.clientLocale)
    }


    static DateFormat getDateFormat() {
        SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.DEFAULT, SimpleDateFormat.SHORT, getLocale())
    }

    static void setTestMode() {
        sTestMode = true
    }

    static boolean getTestMode() {
        return sTestMode
    }

    static void clearTestMode() {
        sTestMode = false
    }

}
