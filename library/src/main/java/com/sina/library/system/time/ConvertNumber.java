package com.sina.library.system.time;

public class ConvertNumber {

    public static String convertEnToFa(String faNumbers) {
        String[][] mChars = new String[][]{
                {"0", "۰"},
                {"1", "۱"},
                {"2", "۲"},
                {"3", "۳"},
                {"4", "۴"},
                {"5", "۵"},
                {"6", "۶"},
                {"7", "۷"},
                {"8", "۸"},
                {"9", "۹"}
        };

        for (String[] num : mChars) {
            faNumbers = faNumbers.replace(num[0], num[1]);
        }
        return faNumbers;
    }

    public static String convertFaToEn(String faNumbers) {
        String[][] mChars = new String[][]{
                {"۰","0"},
                {"۱","1"},
                {"۲","2"},
                {"۳","3"},
                {"۴","4"},
                {"۵","5"},
                {"۶","6"},
                {"۷","7"},
                {"۸","8"},
                {"۹","9"}
        };

        for (String[] num : mChars) {
            faNumbers = faNumbers.replace(num[0], num[1]);
        }
        return faNumbers;
    }
}
