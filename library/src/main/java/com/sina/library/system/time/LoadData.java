package com.sina.library.system.time;

import android.content.Context;

import java.io.IOException;
import java.io.InputStream;

public class LoadData {
    public static String GetAsset(String inFile, Context context) {
        String tContents = "";
        try {
            InputStream stream = context.getAssets().open(inFile);

            int size = stream.available();
            byte[] buffer = new byte[size];
            stream.read(buffer);
            stream.close();
            tContents = new String(buffer);
        } catch (IOException e) {
        }
        return tContents;
    }
}
