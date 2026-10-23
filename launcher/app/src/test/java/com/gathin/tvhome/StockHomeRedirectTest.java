package com.gathin.tvhome;

import java.util.Collections;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Window sequences as logged on an onn 4K Streaming Box (Google TV 14, launcherx), with their timings. */
public class StockHomeRedirectTest {
    private static final String LX = "com.google.android.apps.tv.launcherx";
    private static final String HOME = LX + ".home.HomeActivity";
    private static final String FRAME = "android.widget.FrameLayout";
    private static final String GATEWAY = "com.google.apps.tiktok.nav.gateway.GatewayActivity";
    private static final String CHOOSER = LX + ".profile.chooser.ProfileChooserActivity";
    private static final String DASHBOARD = LX + ".dashboard.DashboardActivity";
    private static final String OWN = "com.gathin.tvhome";
    private static final String YOUTUBE = "com.google.android.youtube.tv";

    private final StockHomeRedirect redirect = new StockHomeRedirect(LX, OWN, Collections.singleton(HOME));

    private StockHomeRedirect.Step stock(String cls, long at) {
        return redirect.onWindow(LX, cls, at);
    }

    @Test public void homePressRedirectsAfterTheDelay() {
        assertEquals(-1, stock(FRAME, 1_000).redirectIn);
        StockHomeRedirect.Step home = stock(HOME, 1_016);
        assertTrue(home.cancelRedirect);
        assertEquals(StockHomeRedirect.REDIRECT_DELAY_MS, home.redirectIn);
        // The second report restarts the wait rather than adding a second redirect.
        StockHomeRedirect.Step again = stock(HOME, 1_120);
        assertTrue(again.cancelRedirect);
        assertEquals(StockHomeRedirect.REDIRECT_DELAY_MS, again.redirectIn);
    }

    @Test public void profileChooserOverTheHomeScreenCancelsTheRedirect() {
        stock(FRAME, 1_000);
        stock(HOME, 1_013);
        StockHomeRedirect.Step chooser = stock(CHOOSER, 1_175);
        assertTrue(chooser.cancelRedirect);
        assertEquals(-1, chooser.redirectIn);
        assertTrue(stock(DASHBOARD, 2_000).cancelRedirect);
    }

    @Test public void voiceResultsAreLeftAloneAndFocused() {
        StockHomeRedirect.Step gateway = stock(GATEWAY, 1_000);
        assertTrue(gateway.cancelRedirect);
        assertTrue(gateway.resultsChanged);
        assertTrue(redirect.inResults());
        stock(FRAME, 1_135);
        StockHomeRedirect.Step first = stock(HOME, 1_146);
        assertEquals(-1, first.redirectIn);
        assertEquals(StockHomeRedirect.FOCUS_FALLBACK_MS, first.focusIn);
        StockHomeRedirect.Step second = stock(HOME, 1_350);
        assertEquals(-1, second.redirectIn);
        assertEquals(StockHomeRedirect.FOCUS_DELAY_MS, second.focusIn);
        // Later reports while browsing the results neither redirect nor move focus again.
        StockHomeRedirect.Step third = stock(HOME, 5_000);
        assertEquals(-1, third.redirectIn);
        assertEquals(-1, third.focusIn);
    }

    @Test public void theAssistantAndSystemUiDoNotEndResults() {
        stock(GATEWAY, 1_000);
        assertFalse(redirect.onWindow("com.google.android.katniss", FRAME, 1_100).resultsChanged);
        assertFalse(redirect.onWindow("com.android.systemui", "android.widget.LinearLayout", 1_200).resultsChanged);
        assertFalse(redirect.onWindow(OWN, OWN + ".MainActivity", 1_300).resultsChanged);
        assertTrue(redirect.inResults());
    }

    @Test public void playingAResultEndsResultsSoBackRedirects() {
        stock(GATEWAY, 1_000);
        stock(HOME, 1_150);
        StockHomeRedirect.Step youtube = redirect.onWindow(YOUTUBE, YOUTUBE + ".MainActivity", 9_000);
        assertTrue(youtube.resultsChanged);
        assertFalse(redirect.inResults());
        stock(FRAME, 20_000);
        assertEquals(StockHomeRedirect.REDIRECT_DELAY_MS, stock(HOME, 20_015).redirectIn);
    }

    @Test public void homePressOnTheResultsIsRecognised() {
        stock(GATEWAY, 1_000);
        stock(FRAME, 1_130);
        stock(HOME, 1_150);
        // A frame then the home screen within a few ms, after the results settled: a Home press.
        stock(FRAME, 8_000);
        StockHomeRedirect.Step home = stock(HOME, 8_015);
        assertTrue(home.resultsChanged);
        assertEquals(StockHomeRedirect.REDIRECT_DELAY_MS, home.redirectIn);
        assertFalse(redirect.inResults());
    }

    @Test public void homeKeyEndsResultsOnlyWhileTheyAreUp() {
        assertFalse(redirect.onHomeKey());
        stock(GATEWAY, 1_000);
        assertTrue(redirect.onHomeKey());
        assertFalse(redirect.inResults());
        assertFalse(redirect.onHomeKey());
    }

    @Test public void otherAppsAndMissingNamesAreIgnored() {
        StockHomeRedirect.Step other = redirect.onWindow(YOUTUBE, YOUTUBE + ".MainActivity", 1_000);
        assertFalse(other.cancelRedirect);
        assertEquals(-1, other.redirectIn);
        assertEquals(-1, redirect.onWindow(null, HOME, 1_000).redirectIn);
        assertEquals(-1, redirect.onWindow(LX, null, 1_000).redirectIn);
    }
}
