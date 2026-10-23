package com.gathin.tvhome;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class HomeKeyServiceTest {

    @Test
    public void redirectsOnlyWhenSetAndJustBooted() {
        assertTrue(HomeKeyService.redirectAtBoot(true, 50_000L));
        assertFalse(HomeKeyService.redirectAtBoot(false, 50_000L));
        assertFalse(HomeKeyService.redirectAtBoot(true, HomeKeyService.BOOT_WINDOW_MS));
        assertFalse(HomeKeyService.redirectAtBoot(true, 3_600_000L));
    }
}
