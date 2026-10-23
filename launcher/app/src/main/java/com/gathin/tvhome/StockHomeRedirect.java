package com.gathin.tvhome;

import java.util.Set;

/**
 * stock_home_redirect, decided from window state changes alone: HomeKeyService feeds it the package and class
 * of every window that comes forward and carries out the Step it returns. Kept free of Android classes so it
 * can be tested on the JVM.
 *
 * The stock launcher's home screen is sent back to this launcher a moment after it appears, unless another
 * stock launcher screen comes up on top in that moment (Google TV's profile chooser and quick settings open
 * over its home screen). Google TV also shows voice search results in its home screen, opened through a
 * gateway activity; from that gateway until another app comes forward ("results mode") the home screen is
 * left alone. Pressing Home on the results brings no new window, so HomeKeyService watches the Home key
 * itself while results mode lasts (onHomeKey). Google TV focuses the whole results panel rather than its
 * first card, so a Step can also ask for one Down once the results have settled.
 *
 * Window sequences, from an onn 4K Streaming Box on Google TV 14 (launcherx):
 *   Home press         FrameLayout, HomeActivity (~15 ms later), HomeActivity
 *   voice results      GatewayActivity, FrameLayout, HomeActivity, HomeActivity (~200 ms later)
 *   profile button     FrameLayout, HomeActivity, ProfileChooserActivity (~150 ms later)
 */
final class StockHomeRedirect {

    /** Wait before redirecting, so a screen opening over the home screen (profile chooser) can cancel it. */
    static final long REDIRECT_DELAY_MS = 250;
    /** The results screen reports itself during this long after the gateway; none of it is a Home press. */
    static final long RESULTS_SETTLE_MS = 1_500;
    /** A Home press shows a frame and then the home screen within this long. */
    static final long HOME_PRESS_PAIR_MS = 150;
    /** One Down this long after the results screen's second report, the sign that it takes keys. */
    static final long FOCUS_DELAY_MS = 150;
    /** ...or this long after the first report, when no second one comes. */
    static final long FOCUS_FALLBACK_MS = 900;

    /** Windows that come and go during a voice search without ending results mode. */
    static final String ASSISTANT = "com.google.android.katniss";
    static final String SYSTEM_UI = "com.android.systemui";

    /** What HomeKeyService does about one window. -1: nothing to schedule. */
    static final class Step {
        boolean cancelRedirect;
        long redirectIn = -1;
        /** Replaces any Down already scheduled. */
        long focusIn = -1;
        /** Results mode switched on or off, so key filtering has to follow it. */
        boolean resultsChanged;
    }

    private final String stockPackage;
    private final String ownPackage;
    private final Set<String> homeActivities;
    private boolean results;
    private long resultsAt = -1;
    private long frameAt = -1;
    private int resultsReports;

    StockHomeRedirect(String stockPackage, String ownPackage, Set<String> homeActivities) {
        this.stockPackage = stockPackage;
        this.ownPackage = ownPackage;
        this.homeActivities = homeActivities;
    }

    boolean inResults() {
        return results;
    }

    Step onWindow(String pkg, String cls, long now) {
        Step step = new Step();
        if (pkg == null || cls == null) return step;
        if (!pkg.equals(stockPackage)) {
            // Another app came forward (a voice result playing, anything opened from the results): results mode
            // is over. The Assistant's overlay, the system UI (mic indicator, volume) and this launcher don't count.
            if (results && !pkg.equals(ASSISTANT) && !pkg.equals(SYSTEM_UI) && !pkg.equals(ownPackage)) {
                results = false;
                step.resultsChanged = true;
            }
            return step;
        }
        if (homeActivities.contains(cls)) {
            if (results) {
                boolean homePress = now - resultsAt > RESULTS_SETTLE_MS && frameAt >= 0 && now - frameAt < HOME_PRESS_PAIR_MS;
                if (!homePress) {
                    resultsReports++;
                    if (resultsReports <= 2) step.focusIn = resultsReports == 1 ? FOCUS_FALLBACK_MS : FOCUS_DELAY_MS;
                    return step;
                }
                results = false;
                step.resultsChanged = true;
            }
            step.cancelRedirect = true;
            step.redirectIn = REDIRECT_DELAY_MS;
        } else if (cls.contains("Gateway") || cls.endsWith("LauncherDeepLink")) {
            step.cancelRedirect = true;
            step.resultsChanged = !results;
            results = true;
            resultsAt = now;
            resultsReports = 0;
        } else if (cls.endsWith("Activity")) {
            // Another stock launcher screen over its home screen: the home screen under it is not a Home press.
            step.cancelRedirect = true;
        } else {
            frameAt = now;
        }
        return step;
    }

    /** Home pressed while the results are up. True: results mode is over, go Home. */
    boolean onHomeKey() {
        if (!results) return false;
        results = false;
        return true;
    }
}
