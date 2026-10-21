package com.gathin.tvhome;

import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.util.Log;

/** Returns to the selected Home after boot or Xiaomi's suspend-to-RAM wake broadcast. */
public class StartupReceiver extends BroadcastReceiver {
    private static final String TAG = "TvHomeStartup";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        Intent homeIntent = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
        ResolveInfo home = context.getPackageManager().resolveActivity(
                homeIntent, PackageManager.MATCH_DEFAULT_ONLY);
        String homePackage = home != null && home.activityInfo != null
                ? home.activityInfo.packageName : null;
        if (!StartupPolicy.shouldOpen(intent.getAction(), context.getPackageName(), homePackage)) return;
        try {
            context.startActivity(homeIntent.setClass(context, MainActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED));
            Log.i(TAG, "Opening selected Home after " + intent.getAction());
        } catch (ActivityNotFoundException | SecurityException e) {
            Log.w(TAG, "Could not open the selected Home", e);
        }
    }
}
