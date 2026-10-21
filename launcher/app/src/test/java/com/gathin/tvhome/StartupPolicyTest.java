package com.gathin.tvhome;

import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class StartupPolicyTest {
    @Test public void resumesChosenCompatibleHomeAfterXiaomiStandby() {
        assertTrue(StartupPolicy.shouldOpen("mitv.action.STR_BOOT_COMPLETED",
                "com.ts.launcher", "com.ts.launcher"));
    }

    @Test public void opensChosenHomeAfterSystemBoot() {
        assertTrue(StartupPolicy.shouldOpen("android.intent.action.BOOT_COMPLETED",
                "com.ts.launcher", "com.ts.launcher"));
    }

    @Test public void alsoSupportsTheStandardApplicationIdentity() {
        assertTrue(StartupPolicy.shouldOpen("android.intent.action.BOOT_COMPLETED",
                "com.gathin.tvhome", "com.gathin.tvhome"));
    }

    @Test public void respectsTheUsersSwitchToAnotherHome() {
        assertFalse(StartupPolicy.shouldOpen("mitv.action.STR_BOOT_COMPLETED",
                "com.ts.launcher", "com.mitv.tvhome"));
        assertFalse(StartupPolicy.shouldOpen("android.intent.action.BOOT_COMPLETED",
                "com.ts.launcher", "com.mitv.tvhome"));
    }

    @Test public void doesNotReplaceTheHomeChooserOrAnUnresolvedHome() {
        assertFalse(StartupPolicy.shouldOpen("mitv.action.STR_BOOT_COMPLETED",
                "com.ts.launcher", "android"));
        assertFalse(StartupPolicy.shouldOpen("mitv.action.STR_BOOT_COMPLETED",
                "com.ts.launcher", null));
    }

    @Test public void ignoresOrdinaryScreenOnAndUnrelatedBroadcasts() {
        assertFalse(StartupPolicy.shouldOpen("android.intent.action.SCREEN_ON",
                "com.ts.launcher", "com.ts.launcher"));
        assertFalse(StartupPolicy.shouldOpen("android.net.conn.CONNECTIVITY_CHANGE",
                "com.ts.launcher", "com.ts.launcher"));
    }

    @Test public void ignoresMissingActionAndApplicationIdentity() {
        assertFalse(StartupPolicy.shouldOpen(null, "com.ts.launcher", "com.ts.launcher"));
        assertFalse(StartupPolicy.shouldOpen("android.intent.action.BOOT_COMPLETED", null, null));
        assertFalse(StartupPolicy.shouldOpen("android.intent.action.BOOT_COMPLETED", "", ""));
    }
}
