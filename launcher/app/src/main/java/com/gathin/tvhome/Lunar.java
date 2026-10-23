package com.gathin.tvhome;

import java.time.LocalDate;

/**
 * Lunar (nongli) date, solar terms and the sexagenary (ganzhi) cycle: the 1900-2100 month table and the
 * Shouxing term formula of lunarDate.js v1.0.0 (dansion, 2011), ported to plain Java and upgraded with the
 * "小调整" correction table so every term day is exact across 1900-2100. No network, no ICU, cheap for a
 * 1 GB TV. The ganzhi year and zodiac turn on 立春 and the month pillar on each 节; the month table itself
 * is exact for 1900-01-31 through 2101-01-28.
 */
final class Lunar {

    /**
     * One entry per year 1900-2100. Bits 15..4: the twelve common months, set = 30 days, clear = 29.
     * Bits 3..0: the leap month number, 0 or 15 meaning none; a year's own leap length is read from the
     * next entry's low nibble (15 = 30 days, otherwise 29), exactly as the JS does.
     */
    private static final int[] YEARS = {
            0x4bd8, 0x4ae0, 0xa570, 0x54d5, 0xd260, 0xd950, 0x5554, 0x56af, 0x9ad0, 0x55d2,
            0x4ae0, 0xa5b6, 0xa4d0, 0xd250, 0xd255, 0xb54f, 0xd6a0, 0xada2, 0x95b0, 0x4977,
            0x497f, 0xa4b0, 0xb4b5, 0x6a50, 0x6d40, 0xab54, 0x2b6f, 0x9570, 0x52f2, 0x4970,
            0x6566, 0xd4a0, 0xea50, 0x6a95, 0x5adf, 0x2b60, 0x86e3, 0x92ef, 0xc8d7, 0xc95f,
            0xd4a0, 0xd8a6, 0xb55f, 0x56a0, 0xa5b4, 0x25df, 0x92d0, 0xd2b2, 0xa950, 0xb557,
            0x6ca0, 0xb550, 0x5355, 0x4daf, 0xa5b0, 0x4573, 0x52bf, 0xa9a8, 0xe950, 0x6aa0,
            0xaea6, 0xab50, 0x4b60, 0xaae4, 0xa570, 0x5260, 0xf263, 0xd950, 0x5b57, 0x56a0,
            0x96d0, 0x4dd5, 0x4ad0, 0xa4d0, 0xd4d4, 0xd250, 0xd558, 0xb540, 0xb6a0, 0x95a6,
            0x95bf, 0x49b0, 0xa974, 0xa4b0, 0xb27a, 0x6a50, 0x6d40, 0xaf46, 0xab60, 0x9570,
            0x4af5, 0x4970, 0x64b0, 0x74a3, 0xea50, 0x6b58, 0x5ac0, 0xab60, 0x96d5, 0x92e0,
            0xc960, 0xd954, 0xd4a0, 0xda50, 0x7552, 0x56a0, 0xabb7, 0x25d0, 0x92d0, 0xcab5,
            0xa950, 0xb4a0, 0xbaa4, 0xad50, 0x55d9, 0x4ba0, 0xa5b0, 0x5176, 0x52bf, 0xa930,
            0x7954, 0x6aa0, 0xad50, 0x5b52, 0x4b60, 0xa6e6, 0xa4e0, 0xd260, 0xea65, 0xd530,
            0x5aa0, 0x76a3, 0x96d0, 0x4afb, 0x4ad0, 0xa4d0, 0xd0b6, 0xd25f, 0xd520, 0xdd45,
            0xb5a0, 0x56d0, 0x55b2, 0x49b0, 0xa577, 0xa4b0, 0xaa50, 0xb255, 0x6d2f, 0xada0,
            0x4b63, 0x937f, 0x49f8, 0x4970, 0x64b0, 0x68a6, 0xea5f, 0x6b20, 0xa6c4, 0xaaef,
            0x92e0, 0xd2e3, 0xc960, 0xd557, 0xd4a0, 0xda50, 0x5d55, 0x56a0, 0xa6d0, 0x55d4,
            0x52d0, 0xa9b8, 0xa950, 0xb4a0, 0xb6a6, 0xad50, 0x55a0, 0xaba4, 0xa5b0, 0x52b0,
            0xb273, 0x6930, 0x7337, 0x6aa0, 0xad50, 0x4b55, 0x4b6f, 0xa570, 0x54e4, 0xd260,
            0xe968, 0xd520, 0xdaa0, 0x6aa6, 0x56df, 0x4ae0, 0xa9d4, 0xa4d0, 0xd150, 0xf252,
            0xd520};

    private static final String[] MONTH_NAMES = {
            "正月", "二月", "三月", "四月", "五月", "六月",
            "七月", "八月", "九月", "十月", "冬月", "腊月"};
    private static final String[] DIGITS = {"零", "一", "二", "三", "四", "五", "六", "七", "八", "九", "十"};
    private static final String[] TENS = {"初", "十", "廿", "卅"};
    /** 天干 / 地支 / 生肖 tables for the ganzhi (sexagenary) line. */
    private static final String[] STEMS = {"甲", "乙", "丙", "丁", "戊", "己", "庚", "辛", "壬", "癸"};
    private static final String[] BRANCHES =
            {"子", "丑", "寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥"};
    private static final String[] ANIMALS =
            {"鼠", "牛", "虎", "兔", "龙", "蛇", "马", "羊", "猴", "鸡", "狗", "猪"};
    private static final String[] TERMS = {
            "小寒", "大寒", "立春", "雨水", "惊蛰", "春分", "清明", "谷雨",
            "立夏", "小满", "芒种", "夏至", "小暑", "大暑", "立秋", "处暑",
            "白露", "秋分", "寒露", "霜降", "立冬", "小雪", "大雪", "冬至"};
    /**
     * Shouxing term constants C for the [Y*D+C]-L formula, per century, indexed like TERMS (小寒..冬至).
     * From the maintained chinese-days table; its 立秋 20th-century value is corrected here to 8.35.
     */
    private static final double[] TERM_C_20 = {
            6.11, 20.84, 4.6295, 19.4599, 6.3926, 21.4155, 5.59, 20.888, 6.318, 21.86, 6.5, 22.2,
            7.928, 23.65, 8.35, 23.95, 8.44, 23.822, 9.098, 24.218, 8.218, 23.08, 7.9, 22.6};
    private static final double[] TERM_C_21 = {
            5.4055, 20.12, 3.87, 18.73, 5.63, 20.646, 4.81, 20.1, 5.52, 21.04, 5.678, 21.37,
            7.108, 22.83, 7.5, 23.13, 7.646, 23.042, 8.318, 23.438, 7.438, 22.36, 7.18, 21.94};
    /**
     * The "小调整" table: the handful of (year, term) pairs where the general formula is off by a day, as
     * {year, term index, day offset} (2026 雨水 -1, 2021 冬至 -1, ...). Keeps every term exact 1900-2100.
     */
    private static final int[][] TERM_DELTA = {
            {2026, 3, -1}, {2084, 5, 1}, {1911, 8, 1}, {2008, 9, 1}, {1902, 10, 1}, {1928, 11, 1},
            {1925, 12, 1}, {2016, 12, 1}, {1922, 13, 1}, {2002, 14, 1}, {1927, 16, 1}, {1942, 17, 1},
            {2089, 19, 1}, {2089, 20, 1}, {1978, 21, 1}, {1954, 22, 1}, {1918, 23, -1}, {2021, 23, -1},
            {1982, 0, 1}, {2019, 0, -1}, {2000, 1, 1}, {2082, 1, 1}};

    /**
     * Season palette for the solar terms (ARGB). The four initiating terms carry the colors and the rest
     * of their season inherits them: 立春 pink, 立夏 green, 立秋 orange, 立冬 blue.
     */
    static final int COLOR_SPRING = 0xFFFF2F92;   // 立春
    static final int COLOR_SUMMER = 0xFF00F900;   // 立夏
    static final int COLOR_AUTUMN = 0xFFFF9300;   // 立秋
    static final int COLOR_WINTER = 0xFF0096FF;   // 立冬

    /** Lunar new year 1900; the day count in the table starts here, as in the JS. */
    private static final long LUNAR_EPOCH_DAY = LocalDate.of(1900, 1, 31).toEpochDay();

    private Lunar() {
    }

    /**
     * The Home line for one Gregorian date, e.g. "霜降 九月(大) 十四" for 2026-10-23; the term only on its
     * own day. Null outside the table's range.
     */
    static String label(int year, int month, int day) {
        int[] lunar = toLunar(year, month, day);
        if (lunar == null) {
            return null;
        }
        int lunarYear = lunar[0];
        int lunarMonth = lunar[1];
        int lunarDay = lunar[2];
        boolean isLeap = lunar[3] == 1;
        StringBuilder line = new StringBuilder();
        String term = solarTerm(year, month, day);
        if (term != null) {
            line.append(term).append(' ');
        }
        line.append(isLeap ? "闰" : "").append(MONTH_NAMES[lunarMonth - 1])
                .append((isLeap ? leapDays(lunarYear) : monthDays(lunarYear, lunarMonth)) == 30 ? "(大)" : "(小)")
                .append(' ').append(dayName(lunarDay));
        return line.toString();
    }

    /**
     * Gregorian to lunar, the month-loop of lunarDate.js toLunarDate() line for line.
     * Returns {lunarYear, lunarMonth 1-12, lunarDay 1-30, 1 when the month is the leap one}; null outside
     * 1900-01-31 .. 2101-01-28.
     */
    static int[] toLunar(int year, int month, int day) {
        LocalDate date = LocalDate.of(year, month, day);
        if (date.isBefore(LocalDate.of(1900, 1, 31)) || date.isAfter(LocalDate.of(2101, 1, 28))) {
            return null;
        }
        int c = (int) (date.toEpochDay() - LUNAR_EPOCH_DAY);
        int b = 1900;
        int d = 0;
        for (; b < 2100 && c > 0; b++) {
            d = yearDays(b);
            c -= d;
        }
        if (c < 0) {
            c += d;
            b--;
        }
        int lunarYear = b;
        int leap = leapMonth(lunarYear);
        boolean isLeap = false;
        for (b = 1; b < 13 && c > 0; b++) {
            if (leap > 0 && b == leap + 1 && !isLeap) {
                b--;
                isLeap = true;
                d = leapDays(lunarYear);
            } else {
                d = monthDays(lunarYear, b);
            }
            if (isLeap && b == leap + 1) {
                isLeap = false;
            }
            c -= d;
        }
        if (c == 0 && leap > 0 && b == leap + 1) {
            if (isLeap) {
                isLeap = false;
            } else {
                isLeap = true;
                b--;
            }
        }
        if (c < 0) {
            c += d;
            b--;
        }
        return new int[]{lunarYear, b, c + 1, isLeap ? 1 : 0};
    }

    /** The solar term falling on this Gregorian day ("霜降"), or null. */
    static String solarTerm(int year, int month, int day) {
        for (int idx = (month - 1) * 2; idx <= (month - 1) * 2 + 1; idx++) {
            if (termDay(year, idx) == day) {
                return TERMS[idx];
            }
        }
        return null;
    }

    /**
     * ARGB color of a solar term, by the season it belongs to: the four initiating terms 立春/立夏/立秋/
     * 立冬 start the palette and the other two terms of each season share its color. 0 when the name is
     * not one of the 24 terms (so callers can treat "no term today" and "transparent" alike).
     */
    static int termColor(String term) {
        if (term == null) {
            return 0;
        }
        for (int idx = 0; idx < TERMS.length; idx++) {
            if (TERMS[idx].equals(term)) {
                // TERMS runs 小寒(0)..冬至(23); seasons begin at 立春(2), 立夏(8), 立秋(14), 立冬(20).
                if (idx >= 2 && idx < 8) return COLOR_SPRING;
                if (idx >= 8 && idx < 14) return COLOR_SUMMER;
                if (idx >= 14 && idx < 20) return COLOR_AUTUMN;
                return COLOR_WINTER;   // 立冬..冬至 plus the wrap-around 小寒, 大寒
            }
        }
        return 0;
    }

    /**
     * Ganzhi (sexagenary) year, month and day indices (0-59) for a Gregorian date, ported from
     * lunarDate.js ganzhi(): the year and zodiac turn on 立春, the month on each 节, the day on the
     * continuous 60-cycle. Null outside the table range.
     */
    static int[] ganzhi(int year, int month, int day) {
        LocalDate date = LocalDate.of(year, month, day);
        if (date.isBefore(LocalDate.of(1900, 1, 31)) || date.isAfter(LocalDate.of(2101, 1, 28))) {
            return null;
        }
        int f = month - 1;
        int gy = f < 2 ? year - 1900 + 35 : year - 1900 + 36;
        int gm = (year - 1900) * 12 + f + 12;
        if (f == 1 && day >= termDay(year, 2)) {
            gy = year - 1900 + 36;                 // 立春 onward belongs to this Gregorian year's pillar
        }
        if (day + 1 >= termDay(year, f * 2)) {
            gm = (year - 1900) * 12 + f + 13;       // past the month's 节, the month pillar advances
        }
        long gd = date.toEpochDay() + 25577;
        return new int[]{(int) mod(gy, 60), (int) mod(gm, 60), (int) mod(gd, 60)};
    }

    /** Home's ganzhi line, e.g. "丙午 (马) 年 - 戊戌月 - 庚午日" for 2026-10-23; null outside the range. */
    static String ganzhiLine(int year, int month, int day) {
        int[] gz = ganzhi(year, month, day);
        if (gz == null) {
            return null;
        }
        return stemBranch(gz[0]) + " (" + ANIMALS[gz[0] % 12] + ") 年 - "
                + stemBranch(gz[1]) + "月 - " + stemBranch(gz[2]) + "日";
    }

    private static String stemBranch(int index) {
        return STEMS[index % 10] + BRANCHES[index % 12];
    }

    private static long mod(long value, long m) {
        return ((value % m) + m) % m;
    }

    /**
     * Day of the month on which term {@code idx} (0 = 小寒) of Gregorian {@code year} falls, by the Shouxing
     * formula [Y*D+C]-L plus the 小调整 table. Y is the last two year digits; the four Jan/Feb terms
     * (小寒/大寒/立春/雨水) use L=(Y-1)/4 and, in 2000, the 20th-century C.
     */
    private static int termDay(int year, int idx) {
        int y = year % 100;
        boolean use20 = year < 2000 || (year == 2000 && idx <= 3);
        double c = use20 ? TERM_C_20[idx] : TERM_C_21[idx];
        int l = idx <= 3 ? (y - 1) / 4 : y / 4;
        return (int) (y * 0.2422 + c) - l + termDelta(year, idx);
    }

    /** The 小调整 day offset for this (year, term); 0 when the general formula is already exact. */
    private static int termDelta(int year, int idx) {
        for (int[] e : TERM_DELTA) {
            if (e[0] == year && e[1] == idx) {
                return e[2];
            }
        }
        return 0;
    }

    private static String dayName(int lunarDay) {
        return TENS[lunarDay / 10] + (lunarDay % 10 == 0 ? "" : DIGITS[lunarDay % 10]);
    }

    /** Days in lunar year {@code y}: 12 common months plus its leap month when it has one. */
    private static int yearDays(int y) {
        int total = 348;
        for (int bit = 0x8000; bit > 8; bit >>= 1) {
            total += (entry(y) & bit) != 0 ? 1 : 0;
        }
        return leapMonth(y) != 0 ? total + leapDays(y) : total;
    }

    /** Which month is leap in lunar year {@code y}; 0 when there is none. */
    private static int leapMonth(int y) {
        int nibble = entry(y) & 15;
        return nibble == 15 ? 0 : nibble;
    }

    /** Days in the leap month of lunar year {@code y}, read from the next year's entry as the JS does. */
    private static int leapDays(int y) {
        if (y + 1 > 2100) {
            return 29;
        }
        return (entry(y + 1) & 15) == 15 ? 30 : 29;
    }

    /** Days in common lunar month {@code m} (1-12) of year {@code y}. */
    private static int monthDays(int y, int m) {
        return (entry(y) & (0x10000 >> m)) != 0 ? 30 : 29;
    }

    private static int entry(int y) {
        return YEARS[y - 1900];
    }
}
