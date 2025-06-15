package com.sina.library.system.time;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.DisplayMetrics;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Locale;

public class LanguageSetting {

    public static String GetLanguage(String language, Context context) {
        String selectLanguage = "";
        try {
            final ArrayList<String> values = new ArrayList<String>();
            String object = "{ langs:" + LoadData.GetAsset("config_file", context) + "}";
            JSONObject jsonObject = new JSONObject(object);
            JSONArray langs = jsonObject.getJSONArray("langs");
            for (int j = 0; j < langs.length(); j++) {
                values.add(langs.getString(j));
            }

            for (int a = 0; a < values.size(); a++) {
                String[] s = values.get(a).split("-");
                if (s[2].equalsIgnoreCase(language)) {
                    selectLanguage = s[1];
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return selectLanguage;
    }

    public static void SetLanguage(String language, Context context) {
        Locale myLocale = new Locale(language);
        Resources res = context.getResources();
        DisplayMetrics dm = res.getDisplayMetrics();
        Configuration conf = res.getConfiguration();
        conf.locale = myLocale;
        res.updateConfiguration(conf, dm);
    }
}
