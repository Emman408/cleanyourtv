package com.gathin.tvhome;

final class StartupPolicy {
    private StartupPolicy() { }

    static boolean shouldOpen(String action, String applicationPackage, String homePackage) {
        if (applicationPackage == null || applicationPackage.isEmpty()
                || !applicationPackage.equals(homePackage)) return false;
        return "android.intent.action.BOOT_COMPLETED".equals(action)
                || "mitv.action.STR_BOOT_COMPLETED".equals(action);
    }
}
