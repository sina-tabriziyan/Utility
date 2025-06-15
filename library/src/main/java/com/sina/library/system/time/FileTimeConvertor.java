package com.sina.library.system.time;

import java.util.ArrayList;

public class FileTimeConvertor {
    double grogorianEpoch = 1721425.5, islamicEpoch = 1948439.5;
    long fileTimeOneDayS = 10000000 * 60L;
    long fileTimeOneDayM = 10000000 * 60 * 60L;
    long fileTimeOneDayH = 24L * 60 * 60 * 10000000;

    int oneSecond = 10000000;
    long oneMinute = 10000000 * 60L;
    long oneHour = 10000000 * 60 * 60L;


    public ArrayList<Double> JdToDt(double jd, int calendarType) {
        ArrayList<Double> dt = JdToGregorian(jd);

        jd = GregorianToJd(dt.get(0), dt.get(1), dt.get(2));

        switch (calendarType) {
            case 0: {
                return JdToGregorian(jd);
            }
            case 1: {
                return JdToPersian(jd);
            }
            case 2: {
                return JdToIslamic(jd);
            }
            default:
                return dt;
        }
    }

    //     ********************* convert jd to miladi *********************
    public ArrayList<Double> JdToGregorian(double jd) {
        double wjd, depoch, quadricent, dqc, cent, dcent, quad, dquad,
                yindex, dyindex, year, yearday, leapadj, month, day;

        ArrayList<Double> date = new ArrayList<>();

        wjd = Math.floor(jd - 0.5) + 0.5;
        depoch = wjd - grogorianEpoch;
        quadricent = Math.floor(depoch / 146097);
        dqc = (depoch % 146097);
        cent = Math.floor(dqc / 36524);
        dcent = (dqc % 36524);
        quad = Math.floor(dcent / 1461);
        dquad = (dcent % 1461);
        yindex = Math.floor(dquad / 365);
        year = (quadricent * 400) + (cent * 100) + (quad * 4) + yindex;
        if (!((cent == 4) || (yindex == 4))) {
            year++;
        }

        yearday = wjd - GregorianToJd(year, 1, 1);
        leapadj = ((wjd < GregorianToJd(year, 3, 1)) ? 0
                :
                (LeapGregorian(year) ? 1 : 2)
        );
        month = Math.floor((((yearday + leapadj) * 12) + 373) / 367);
        day = (wjd - GregorianToJd(year, month, 1)) + 1;

        date.add(year);
        date.add(month);
        date.add(day);

        return date;
    }

    //     ********************* convert jd to shamsi *********************
    public ArrayList<Double> JdToPersian(double jd) {
        double year, month, day, depoch, cycle, cyear, ycycle,
                aux1, aux2, yday;
        ArrayList<Double> date = new ArrayList<>();

        jd = (Math.floor(jd) + 0.5);

        depoch = jd - PersianToJd(475, 1, 1);
        cycle = Math.floor(depoch / 1029983);
        cyear = (depoch % 1029983);
        if (cyear == 1029982) {
            ycycle = 2820;
        } else {
            aux1 = Math.floor(cyear / 366);
            aux2 = (cyear % 366);
            ycycle = Math.floor(((2134 * aux1) + (2816 * aux2) + 2815) / 1028522) +
                    aux1 + 1;
        }
        year = ycycle + (2820 * cycle) + 474;
        if (year <= 0) {
            year--;
        }
        yday = (jd - PersianToJd(year, 1, 1)) + 1;
        month = (yday <= 186) ? Math.ceil(yday / 31) : Math.ceil((yday - 6) / 30);
        day = (jd - PersianToJd(year, month, 1)) + 1;

        date.add(year);
        date.add(month);
        date.add(day);

        return date;
    }

    //     ********************* convert jd to qamari *********************
    public ArrayList<Double> JdToIslamic(double jd) {
        double year, month, day;

        ArrayList<Double> values = new ArrayList<>();

        jd = Math.floor(jd) + 0.5;
        year = Math.floor(((30 * (jd - islamicEpoch)) + 10646) / 10631);
        month = Math.min(12,
                Math.ceil((jd - (29 + IslamicToJd(year, 1, 1))) / 29.5) + 1);
        day = (jd - IslamicToJd(year, month, 1)) + 1;

        values.add(year);
        values.add(month);
        values.add(day);

        return values;
    }

    //     ********************* convert shamsi to jd *********************
    public double PersianToJd(double year, double month, double day) {
        double epbase, epyear;

        epbase = year - ((year >= 0) ? 474 : 473);
        epyear = 474 + (epbase % 2820);

        return day +
                ((month <= 7) ?
                        ((month - 1) * 31) :
                        (((month - 1) * 30) + 6)
                ) +
                Math.floor(((epyear * 682) - 110) / 2816) +
                (epyear - 1) * 365 +
                Math.floor(epbase / 2820) * 1029983 +
                (1948320.5 - 1);
    }

    //     ********************* convert shamsi to filetime *********************
    public long PersianToFileTime(double year, double month, double day) {
        double jd_date = PersianToJd(year, month, day);
        long filetime = (long) ((((jd_date - 2440587.5) * 86400) + 11644473600L) * 10000000);

        return filetime;
    }

    //     ********************* convert miladi to jd *********************
    public double GregorianToJd(double year, double month, double day) {
        return (grogorianEpoch - 1) +
                (365 * (year - 1)) +
                Math.floor((year - 1) / 4) +
                (-Math.floor((year - 1) / 100)) +
                Math.floor((year - 1) / 400) +
                Math.floor((((367 * month) - 362) / 12) +
                        ((month <= 2) ? 0 :
                                (LeapGregorian(year) ? -1 : -2)
                        ) +
                        day);
    }

    //     ********************* convert miladi to shamsi *********************
    public ArrayList<Double> GregorianToShamsi(int year, int month, int day) {
        double date = GregorianToJd(year, month, day);
        ArrayList<Double> persian = JdToPersian(date);
        return persian;
    }

    //     ********************* convert miladi to filetime *********************
    public long GregorianToFileTime(double year, double month, double day) {
        double jd_date = GregorianToJd(year, month, day);
        long filetime = (long) ((((jd_date - 2440587.5) * 86400) + 11644473600L) * 10000000);

        return filetime;
    }

    //     ********************* convert filetime to jd *********************
    public double FiletimeToJd(long filetime, long timezone) {

        long filetimeZone = filetime + timezone;
        double jd = (((filetimeZone / 10000000) - 11644473600L) / 86400) + 2440587.5;
        return jd;
    }

    //      ********************* convert qamari to jd *********************
    public double IslamicToJd(double year, double month, double day) {
        return (day +
                Math.ceil(29.5 * (month - 1)) +
                (year - 1) * 354 +
                Math.floor((3 + (11 * year)) / 30) +
                islamicEpoch) - 1;
    }

    //     ********************* check daylight range *********************
    public boolean CheckDayLight(String daylight_range, double jd, int calendar_type) {
        if (daylight_range == null || daylight_range.equals("null"))
            return false;
        String[] range = daylight_range.split(",");
        int toM = Integer.parseInt(range[2]);
        int fromM = Integer.parseInt(range[0]);
        int toD = Integer.parseInt(range[3]);
        int fromD = Integer.parseInt(range[1]);

        ArrayList<Double> dt = JdToDt(jd, calendar_type);
        if (toM >= dt.get(1) && dt.get(1) >= fromM && (fromM != dt.get(1) || dt.get(2) >= fromD) && (toM != dt.get(1) || toD >= dt.get(2))) {
            return true;
        } else {
            return false;
        }
    }

    //     ********************* get date and time *********************
    public ArrayList<String> Picker(long filetime, long timezone, String daylight_range, int calendar_type) {

        ArrayList<String> result = new ArrayList<>();

        double jd = FiletimeToJd(filetime, timezone);

        if (daylight_range != null) {
            boolean checkDayLight = CheckDayLight(daylight_range, jd, calendar_type);
            if (checkDayLight) {
                filetime = filetime + 36000000000L;
            }
        }
        ArrayList<Double> dt = JdToDt(jd, calendar_type);

        long[] filetime_mod_day = FiletimeModDay(filetime + timezone);


        String year = String.valueOf(Integer.valueOf(dt.get(0).intValue()));
        String month = String.valueOf(Integer.valueOf(dt.get(1).intValue()));
        String day = String.valueOf(Integer.valueOf(dt.get(2).intValue()));
        String hour = String.valueOf((int) (filetime_mod_day[0] / oneHour));
        String minute = String.valueOf((int) (filetime_mod_day[1] / oneMinute));
        String second = String.valueOf((int) (filetime_mod_day[2] / oneSecond));

        result.add(year);
        result.add(month.length() == 1 ? "0" + month : month);
        result.add(day.length() == 1 ? "0" + day : day);
        result.add(hour.length() == 1 ? "0" + hour : hour);
        result.add(minute.length() == 1 ? "0" + minute : minute);
        result.add(second.length() == 1 ? "0" + second : second);

        return result;
    }

    public long[] FiletimeModDay(long filetime) {
        long h = filetime % fileTimeOneDayH;
        long m = h % fileTimeOneDayM;
        long s = m % fileTimeOneDayS;
        long[] ss = {h, m, s};
        return ss;
    }

    //     *********************  leap year *********************
    public boolean LeapGregorian(double year) {
        return ((year % 4) == 0) &&
                (!(((year % 100) == 0) && ((year % 400) != 0)));
    }

    public boolean LeapPersian(String year) {
        double p = Double.valueOf(year) + 621;
        return ((p % 4) == 0) &&
                (!(((p % 100) == 0) && ((p % 400) != 0)));
    }
}
