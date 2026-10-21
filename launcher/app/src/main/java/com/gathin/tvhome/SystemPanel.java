package com.gathin.tvhome;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/** Opens a configured, explicit vendor panel, which may be an activity or a service. */
final class SystemPanel {
    private SystemPanel() { }

    static boolean open(Context context, String component, String action) {
        ComponentName target = ComponentName.unflattenFromString(component);
        if (target == null) return false;
        Intent intent = new Intent(action).setComponent(target);
        try {
            if (context.getPackageManager().resolveActivity(intent, 0) != null) {
                context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                return true;
            }
            return context.startService(intent) != null;
        } catch (RuntimeException e) {
            Log.w("TvHome.Panel", "Could not open configured system panel", e);
            return false;
        }
    }
}
