package net.lightningsdk.LightningJavaClient;

import android.util.Log;
import static net.lightningsdk.LightningJavaClient.Lightning.DEBUG;

/**
 * !!
 *
 * @author Daniel Behrman macdabby@gmail.com
 */
public class AppLog {

    private static final String TAG = "Lightning";

    public static void v(String msg) {
        if (DEBUG) {
            Log.v(TAG, msg);
        }
    }

    public static void d(String msg) {
        if (DEBUG) {
            Log.d(TAG, msg);
        }
    }

    public static void w(String msg) {
        if (DEBUG) {
            Log.w(TAG, msg);
        }
    }

    public static void e(String msg) {
        e(msg, null);
    }

    public static void e(String msg, Throwable e) {
        if (DEBUG) {
            Log.e(TAG, msg, e);
        }
    }

    public static void i(String msg) {
        if (DEBUG) {
            Log.i(TAG, msg);
        }
    }

    public static void wtf(String msg) {
        if (DEBUG) {
            Log.wtf(TAG, msg);
        }
    }

}
