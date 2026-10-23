package com.gathin.tvhome;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

/** Locks the lunarDate.js port: the Home line format, spring festivals, leap months, terms, table edges. */
public class LunarTest {

    @Test
    public void homeLineMatchesTheRequestedFormat() {
        // The example from the request:霜降 day of 2026, lunar 九月十四, a 30-day (大) month.
        assertEquals("霜降 九月(大) 十四", Lunar.label(2026, 10, 23));
    }

    @Test
    public void springFestivalIsFirstDayOfFirstMonth() {
        assertEquals("正月(小) 初一", Lunar.label(2023, 1, 22));
        assertEquals("正月(大) 初一", Lunar.label(2025, 1, 29));
        assertEquals("正月(大) 初一", Lunar.label(2026, 2, 17));
    }

    @Test
    public void leapMonthIsPrefixedAndNamed() {
        // 2023 had a leap month 2; the JS threw on this branch, the port resolves it.
        int[] lunar = Lunar.toLunar(2023, 4, 19);
        assertArrayEquals(new int[]{2023, 2, 29, 1}, lunar);
        assertEquals("闰二月(小) 廿九", Lunar.label(2023, 4, 19));
    }

    @Test
    public void termShowsOnlyOnItsOwnDay() {
        assertEquals("谷雨 三月(小) 初一", Lunar.label(2023, 4, 20));
        assertEquals("立春 腊月(大) 廿五", Lunar.label(2024, 2, 4));
        assertEquals("冬至 冬月(大) 廿一", Lunar.label(2024, 12, 21));
        // The day before霜降 carries no term.
        assertEquals("九月(大) 十三", Lunar.label(2026, 10, 22));
    }

    @Test
    public void tableCoversFromLunarNewYear1900ToItsUpperEdge() {
        assertEquals("正月(小) 初二", Lunar.label(1900, 2, 1));
        assertEquals("腊月(小) 廿九", Lunar.label(2101, 1, 28));
        // One day past the last covered date falls off the table.
        assertNull(Lunar.label(2101, 1, 29));
        assertNull(Lunar.toLunar(1900, 1, 30));
    }

    @Test
    public void initiatingTermsCarryTheFourSeasonColors() {
        assertEquals(0xFFFF2F92, Lunar.termColor("立春"));
        assertEquals(0xFF00F900, Lunar.termColor("立夏"));
        assertEquals(0xFFFF9300, Lunar.termColor("立秋"));
        assertEquals(0xFF0096FF, Lunar.termColor("立冬"));
    }

    @Test
    public void eachSeasonInheritsItsInitiatorColor() {
        int spring = Lunar.termColor("立春");
        int summer = Lunar.termColor("立夏");
        int autumn = Lunar.termColor("立秋");
        int winter = Lunar.termColor("立冬");
        // 雨水..谷雨 are spring; 小满..大暑 summer; 处暑..霜降 autumn.
        assertEquals(spring, Lunar.termColor("谷雨"));
        assertEquals(spring, Lunar.termColor("惊蛰"));
        assertEquals(summer, Lunar.termColor("夏至"));
        assertEquals(summer, Lunar.termColor("小满"));
        assertEquals(autumn, Lunar.termColor("霜降"));
        assertEquals(autumn, Lunar.termColor("白露"));
        // Winter wraps the year boundary: 小雪..冬至 and 小寒, 大寒 all share 立冬's blue.
        assertEquals(winter, Lunar.termColor("冬至"));
        assertEquals(winter, Lunar.termColor("小寒"));
        assertEquals(winter, Lunar.termColor("大寒"));
    }

    @Test
    public void nonTermNamesAreTransparent() {
        assertEquals(0, Lunar.termColor("九月"));
        assertEquals(0, Lunar.termColor(null));
    }

    @Test
    public void ganzhiLineMatchesTheRequestedFormat() {
        // The example from the request, and the year/zodiac for 2026.
        assertEquals("丙午 (马) 年 - 戊戌月 - 庚午日", Lunar.ganzhiLine(2026, 10, 23));
        assertEquals("甲辰 (龙) 年 - 丙寅月 - 甲辰日", Lunar.ganzhiLine(2024, 2, 10));
    }

    @Test
    public void ganzhiYearAndZodiacTurnOnLichun() {
        // 2026 立春 falls on Feb 4: Feb 3 is still 乙巳蛇, Feb 4 onward is 丙午马.
        assertEquals("乙巳 (蛇) 年", Lunar.ganzhiLine(2026, 2, 3).substring(0, 8));
        assertEquals("丙午 (马) 年", Lunar.ganzhiLine(2026, 2, 4).substring(0, 8));
    }

    @Test
    public void minorAdjustmentKeepsEdgeTermDaysExact() {
        // 2026 雨水 needs the 小调整 -1 (formula alone gives Feb 19); 2021 冬至 needs -1 (Dec 22 -> 21).
        assertEquals("雨水", Lunar.solarTerm(2026, 2, 18));
        assertNull(Lunar.solarTerm(2026, 2, 19));
        assertEquals("冬至", Lunar.solarTerm(2021, 12, 21));
        assertNull(Lunar.solarTerm(2021, 12, 22));
    }
}
