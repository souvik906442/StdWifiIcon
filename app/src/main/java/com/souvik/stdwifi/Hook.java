package com.souvik.stdwifi;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.os.PowerManager;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.util.SparseIntArray;
import android.util.TypedValue;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * One UI 6.1 SystemUI: shows "4" / "5" badged Wi-Fi icons for Wi-Fi 4 (802.11n) and Wi-Fi 5 (802.11ac),
 * like Wi-Fi 6 already has.
 *
 * How: WifiInteractorImpl.wifiIconGroup picks an IconGroup from the Wi-Fi standard. For standard 4/5 it returns the
 * plain group. We return WIFI_ICON_6GE (std 4) or WIFI_ICON_7G (std 5) instead; those groups carry the drawable ids
 * stat_sys_wifi6e_signal_N / stat_sys_wifi7_signal_N. When SystemUI loads those drawables we hand back our own
 * "4"/"5" icons. (This tablet has no 6 GHz / Wi-Fi 7, so those ids are otherwise unused.)
 * Also: double tap on the status bar turns the screen off; Circle to Search unlock.
 */
public class Hook implements IXposedHookLoadPackage {
    private static final String SUI = "com.android.systemui";
    private static final String LAMBDA =
            "com.android.systemui.statusbar.pipeline.wifi.domain.interactor.WifiInteractorImpl$wifiIconGroup$1";
    private static final String SAM = "com.android.systemui.statusbar.pipeline.wifi.ui.util.SamsungWifiIcons";
    private static final String ICONS = "com.android.systemui.statusbar.connectivity.WifiIcons";

    private static final String[][] W4_P = {
        {"M10.02,7.647C10.231,7.86 10.231,8.204 10.019,8.416C9.913,8.521 9.774,8.574 9.635,8.574C9.496,8.574 9.356,8.521 9.25,8.415C8.403,7.567 6.926,7.567 6.079,8.415C5.868,8.628 5.523,8.628 5.31,8.417C5.098,8.205 5.098,7.861 5.31,7.648C6.568,6.389 8.761,6.389 10.02,7.647ZM7.664,4.104C8.538,4.104 9.381,4.3 10.151,4.659C10.147,4.734 10.13,4.805 10.129,4.882C10.129,5.299 10.222,5.677 10.368,6.027C9.577,5.486 8.644,5.191 7.665,5.191C6.38,5.191 5.173,5.692 4.264,6.601C4.053,6.813 3.709,6.814 3.495,6.601C3.283,6.389 3.283,6.045 3.495,5.832C4.609,4.718 6.089,4.104 7.664,4.104ZM7.665,1.685C8.84,1.685 9.975,1.934 11.018,2.394C10.779,2.715 10.577,3.028 10.447,3.325C9.577,2.963 8.636,2.772 7.665,2.772C5.733,2.772 3.919,3.524 2.553,4.89C2.447,4.996 2.308,5.048 2.169,5.048C2.03,5.048 1.891,4.995 1.785,4.889C1.573,4.677 1.573,4.333 1.785,4.12C3.355,2.551 5.444,1.685 7.665,1.685ZM7.665,9.247C7.285,9.247 6.978,9.555 6.978,9.934C6.978,10.313 7.286,10.621 7.665,10.621C8.045,10.621 8.352,10.313 8.352,9.934C8.352,9.555 8.045,9.247 7.665,9.247Z", "M13.95,1.5L13.95,4.8L14.6,4.8L14.6,5.65L13.95,5.65L13.95,6.65L13.0,6.65L13.0,5.65L11.1,5.65L11.1,4.9L13.2,1.5ZM13.0,3.35L12.0,4.8L13.0,4.8Z"},
        {"M10.02,7.647C10.231,7.86 10.231,8.204 10.019,8.416C9.913,8.521 9.774,8.574 9.635,8.574C9.496,8.574 9.356,8.521 9.25,8.415C8.403,7.567 6.926,7.567 6.079,8.415C5.868,8.628 5.523,8.628 5.31,8.417C5.098,8.205 5.098,7.861 5.31,7.648C6.568,6.389 8.761,6.389 10.02,7.647ZM7.664,4.104C8.538,4.104 9.381,4.3 10.151,4.659C10.147,4.734 10.13,4.805 10.129,4.882C10.129,5.299 10.222,5.677 10.368,6.027C9.577,5.486 8.644,5.191 7.665,5.191C6.38,5.191 5.173,5.692 4.264,6.601C4.053,6.813 3.709,6.814 3.495,6.601C3.283,6.389 3.283,6.045 3.495,5.832C4.609,4.718 6.089,4.104 7.664,4.104ZM7.665,1.685C8.84,1.685 9.975,1.934 11.018,2.394C10.779,2.715 10.577,3.028 10.447,3.325C9.577,2.963 8.636,2.772 7.665,2.772C5.733,2.772 3.919,3.524 2.553,4.89C2.447,4.996 2.308,5.048 2.169,5.048C2.03,5.048 1.891,4.995 1.785,4.889C1.573,4.677 1.573,4.333 1.785,4.12C3.355,2.551 5.444,1.685 7.665,1.685Z", "M7.665,9.247C7.285,9.247 6.978,9.555 6.978,9.934C6.978,10.313 7.286,10.621 7.665,10.621C8.045,10.621 8.352,10.313 8.352,9.934C8.352,9.555 8.045,9.247 7.665,9.247Z", "M13.95,1.5L13.95,4.8L14.6,4.8L14.6,5.65L13.95,5.65L13.95,6.65L13.0,6.65L13.0,5.65L11.1,5.65L11.1,4.9L13.2,1.5ZM13.0,3.35L12.0,4.8L13.0,4.8Z"},
        {"M5.31,7.648C5.098,7.861 5.098,8.205 5.31,8.417C5.523,8.628 5.868,8.628 6.079,8.415C6.926,7.567 8.403,7.567 9.25,8.415C9.356,8.521 9.496,8.574 9.635,8.574C9.774,8.574 9.913,8.521 10.019,8.416C10.231,8.204 10.231,7.86 10.02,7.647C8.761,6.389 6.568,6.389 5.31,7.648ZM7.665,9.247C7.285,9.247 6.978,9.555 6.978,9.934C6.978,10.313 7.286,10.621 7.665,10.621C8.045,10.621 8.352,10.313 8.352,9.934C8.352,9.555 8.045,9.247 7.665,9.247Z", "M7.664,4.104C8.538,4.104 9.381,4.3 10.151,4.659C10.147,4.734 10.13,4.805 10.129,4.882C10.129,5.299 10.222,5.677 10.368,6.027C9.577,5.486 8.644,5.191 7.665,5.191C6.38,5.191 5.173,5.692 4.264,6.601C4.053,6.813 3.709,6.814 3.495,6.601C3.283,6.389 3.283,6.045 3.495,5.832C4.609,4.718 6.089,4.104 7.664,4.104ZM7.665,1.685C8.84,1.685 9.975,1.934 11.018,2.394C10.779,2.715 10.577,3.028 10.447,3.325C9.577,2.963 8.636,2.772 7.665,2.772C5.733,2.772 3.919,3.524 2.553,4.89C2.447,4.996 2.308,5.048 2.169,5.048C2.03,5.048 1.891,4.995 1.785,4.889C1.573,4.677 1.573,4.333 1.785,4.12C3.355,2.551 5.444,1.685 7.665,1.685Z", "M13.95,1.5L13.95,4.8L14.6,4.8L14.6,5.65L13.95,5.65L13.95,6.65L13.0,6.65L13.0,5.65L11.1,5.65L11.1,4.9L13.2,1.5ZM13.0,3.35L12.0,4.8L13.0,4.8Z"},
        {"M5.31,7.648C6.568,6.389 8.761,6.389 10.02,7.647C10.231,7.86 10.231,8.204 10.019,8.416C9.913,8.521 9.774,8.574 9.635,8.574C9.496,8.574 9.356,8.521 9.25,8.415C8.403,7.567 6.926,7.567 6.079,8.415C5.868,8.628 5.523,8.628 5.31,8.417C5.098,8.205 5.098,7.861 5.31,7.648ZM7.664,4.104C8.538,4.104 9.381,4.3 10.151,4.659C10.147,4.734 10.13,4.805 10.129,4.882C10.129,5.299 10.222,5.677 10.368,6.027C9.577,5.486 8.644,5.191 7.665,5.191C6.38,5.191 5.173,5.692 4.264,6.601C4.053,6.813 3.709,6.814 3.495,6.601C3.283,6.389 3.283,6.045 3.495,5.832C4.609,4.718 6.089,4.104 7.664,4.104ZM7.665,9.247C7.285,9.247 6.978,9.555 6.978,9.934C6.978,10.313 7.286,10.621 7.665,10.621C8.045,10.621 8.352,10.313 8.352,9.934C8.352,9.555 8.045,9.247 7.665,9.247Z", "M1.785,4.12C1.573,4.333 1.573,4.677 1.785,4.889C1.891,4.995 2.03,5.048 2.169,5.048C2.308,5.048 2.447,4.996 2.553,4.89C3.919,3.524 5.733,2.772 7.665,2.772C8.636,2.772 9.577,2.963 10.447,3.325C10.577,3.028 10.779,2.715 11.018,2.394C9.975,1.934 8.84,1.685 7.665,1.685C5.444,1.685 3.355,2.551 1.785,4.12Z", "M13.95,1.5L13.95,4.8L14.6,4.8L14.6,5.65L13.95,5.65L13.95,6.65L13.0,6.65L13.0,5.65L11.1,5.65L11.1,4.9L13.2,1.5ZM13.0,3.35L12.0,4.8L13.0,4.8Z"},
        {"M10.02,7.647C10.231,7.86 10.231,8.204 10.019,8.416C9.913,8.521 9.774,8.574 9.635,8.574C9.496,8.574 9.356,8.521 9.25,8.415C8.403,7.567 6.926,7.567 6.079,8.415C5.868,8.628 5.523,8.628 5.31,8.417C5.098,8.205 5.098,7.861 5.31,7.648C6.568,6.389 8.761,6.389 10.02,7.647ZM7.664,4.104C8.538,4.104 9.381,4.3 10.151,4.659C10.147,4.734 10.13,4.805 10.129,4.882C10.129,5.299 10.222,5.677 10.368,6.027C9.577,5.486 8.644,5.191 7.665,5.191C6.38,5.191 5.173,5.692 4.264,6.601C4.053,6.813 3.709,6.814 3.495,6.601C3.283,6.389 3.283,6.045 3.495,5.832C4.609,4.718 6.089,4.104 7.664,4.104ZM7.665,1.685C8.84,1.685 9.975,1.934 11.018,2.394C10.779,2.715 10.577,3.028 10.447,3.325C9.577,2.963 8.636,2.772 7.665,2.772C5.733,2.772 3.919,3.524 2.553,4.89C2.447,4.996 2.308,5.048 2.169,5.048C2.03,5.048 1.891,4.995 1.785,4.889C1.573,4.677 1.573,4.333 1.785,4.12C3.355,2.551 5.444,1.685 7.665,1.685ZM7.665,9.247C7.285,9.247 6.978,9.555 6.978,9.934C6.978,10.313 7.286,10.621 7.665,10.621C8.045,10.621 8.352,10.313 8.352,9.934C8.352,9.555 8.045,9.247 7.665,9.247ZM13.95,1.5L13.95,4.8L14.6,4.8L14.6,5.65L13.95,5.65L13.95,6.65L13.0,6.65L13.0,5.65L11.1,5.65L11.1,4.9L13.2,1.5ZM13.0,3.35L12.0,4.8L13.0,4.8Z"},
    };
    private static final float[][] W4_A = {
        {0.3961f, 1.0f},
        {0.3961f, 1.0f, 1.0f},
        {1.0f, 0.3961f, 1.0f},
        {1.0f, 0.3961f, 1.0f},
        {1.0f},
    };
    private static final String[][] W5_P = {
        {"M10.02,7.647C10.231,7.86 10.231,8.204 10.019,8.416C9.913,8.521 9.774,8.574 9.635,8.574C9.496,8.574 9.356,8.521 9.25,8.415C8.403,7.567 6.926,7.567 6.079,8.415C5.868,8.628 5.523,8.628 5.31,8.417C5.098,8.205 5.098,7.861 5.31,7.648C6.568,6.389 8.761,6.389 10.02,7.647ZM7.664,4.104C8.538,4.104 9.381,4.3 10.151,4.659C10.147,4.734 10.13,4.805 10.129,4.882C10.129,5.299 10.222,5.677 10.368,6.027C9.577,5.486 8.644,5.191 7.665,5.191C6.38,5.191 5.173,5.692 4.264,6.601C4.053,6.813 3.709,6.814 3.495,6.601C3.283,6.389 3.283,6.045 3.495,5.832C4.609,4.718 6.089,4.104 7.664,4.104ZM7.665,1.685C8.84,1.685 9.975,1.934 11.018,2.394C10.779,2.715 10.577,3.028 10.447,3.325C9.577,2.963 8.636,2.772 7.665,2.772C5.733,2.772 3.919,3.524 2.553,4.89C2.447,4.996 2.308,5.048 2.169,5.048C2.03,5.048 1.891,4.995 1.785,4.889C1.573,4.677 1.573,4.333 1.785,4.12C3.355,2.551 5.444,1.685 7.665,1.685ZM7.665,9.247C7.285,9.247 6.978,9.555 6.978,9.934C6.978,10.313 7.286,10.621 7.665,10.621C8.045,10.621 8.352,10.313 8.352,9.934C8.352,9.555 8.045,9.247 7.665,9.247Z", "M11.4,1.5L14.4,1.5L14.4,2.35L12.3,2.35L12.2,3.45C12.5,3.3 12.8,3.25 13.1,3.25C14.1,3.25 14.65,3.85 14.65,4.9C14.65,5.9 14.0,6.65 12.9,6.65C12.0,6.65 11.35,6.2 11.2,5.35L12.2,5.2C12.3,5.6 12.55,5.8 12.9,5.8C13.35,5.8 13.65,5.45 13.65,4.9C13.65,4.35 13.4,4.05 12.95,4.05C12.65,4.05 12.4,4.2 12.3,4.4L11.4,4.3Z"},
        {"M10.02,7.647C10.231,7.86 10.231,8.204 10.019,8.416C9.913,8.521 9.774,8.574 9.635,8.574C9.496,8.574 9.356,8.521 9.25,8.415C8.403,7.567 6.926,7.567 6.079,8.415C5.868,8.628 5.523,8.628 5.31,8.417C5.098,8.205 5.098,7.861 5.31,7.648C6.568,6.389 8.761,6.389 10.02,7.647ZM7.664,4.104C8.538,4.104 9.381,4.3 10.151,4.659C10.147,4.734 10.13,4.805 10.129,4.882C10.129,5.299 10.222,5.677 10.368,6.027C9.577,5.486 8.644,5.191 7.665,5.191C6.38,5.191 5.173,5.692 4.264,6.601C4.053,6.813 3.709,6.814 3.495,6.601C3.283,6.389 3.283,6.045 3.495,5.832C4.609,4.718 6.089,4.104 7.664,4.104ZM7.665,1.685C8.84,1.685 9.975,1.934 11.018,2.394C10.779,2.715 10.577,3.028 10.447,3.325C9.577,2.963 8.636,2.772 7.665,2.772C5.733,2.772 3.919,3.524 2.553,4.89C2.447,4.996 2.308,5.048 2.169,5.048C2.03,5.048 1.891,4.995 1.785,4.889C1.573,4.677 1.573,4.333 1.785,4.12C3.355,2.551 5.444,1.685 7.665,1.685Z", "M7.665,9.247C7.285,9.247 6.978,9.555 6.978,9.934C6.978,10.313 7.286,10.621 7.665,10.621C8.045,10.621 8.352,10.313 8.352,9.934C8.352,9.555 8.045,9.247 7.665,9.247Z", "M11.4,1.5L14.4,1.5L14.4,2.35L12.3,2.35L12.2,3.45C12.5,3.3 12.8,3.25 13.1,3.25C14.1,3.25 14.65,3.85 14.65,4.9C14.65,5.9 14.0,6.65 12.9,6.65C12.0,6.65 11.35,6.2 11.2,5.35L12.2,5.2C12.3,5.6 12.55,5.8 12.9,5.8C13.35,5.8 13.65,5.45 13.65,4.9C13.65,4.35 13.4,4.05 12.95,4.05C12.65,4.05 12.4,4.2 12.3,4.4L11.4,4.3Z"},
        {"M5.31,7.648C5.098,7.861 5.098,8.205 5.31,8.417C5.523,8.628 5.868,8.628 6.079,8.415C6.926,7.567 8.403,7.567 9.25,8.415C9.356,8.521 9.496,8.574 9.635,8.574C9.774,8.574 9.913,8.521 10.019,8.416C10.231,8.204 10.231,7.86 10.02,7.647C8.761,6.389 6.568,6.389 5.31,7.648ZM7.665,9.247C7.285,9.247 6.978,9.555 6.978,9.934C6.978,10.313 7.286,10.621 7.665,10.621C8.045,10.621 8.352,10.313 8.352,9.934C8.352,9.555 8.045,9.247 7.665,9.247Z", "M7.664,4.104C8.538,4.104 9.381,4.3 10.151,4.659C10.147,4.734 10.13,4.805 10.129,4.882C10.129,5.299 10.222,5.677 10.368,6.027C9.577,5.486 8.644,5.191 7.665,5.191C6.38,5.191 5.173,5.692 4.264,6.601C4.053,6.813 3.709,6.814 3.495,6.601C3.283,6.389 3.283,6.045 3.495,5.832C4.609,4.718 6.089,4.104 7.664,4.104ZM7.665,1.685C8.84,1.685 9.975,1.934 11.018,2.394C10.779,2.715 10.577,3.028 10.447,3.325C9.577,2.963 8.636,2.772 7.665,2.772C5.733,2.772 3.919,3.524 2.553,4.89C2.447,4.996 2.308,5.048 2.169,5.048C2.03,5.048 1.891,4.995 1.785,4.889C1.573,4.677 1.573,4.333 1.785,4.12C3.355,2.551 5.444,1.685 7.665,1.685Z", "M11.4,1.5L14.4,1.5L14.4,2.35L12.3,2.35L12.2,3.45C12.5,3.3 12.8,3.25 13.1,3.25C14.1,3.25 14.65,3.85 14.65,4.9C14.65,5.9 14.0,6.65 12.9,6.65C12.0,6.65 11.35,6.2 11.2,5.35L12.2,5.2C12.3,5.6 12.55,5.8 12.9,5.8C13.35,5.8 13.65,5.45 13.65,4.9C13.65,4.35 13.4,4.05 12.95,4.05C12.65,4.05 12.4,4.2 12.3,4.4L11.4,4.3Z"},
        {"M5.31,7.648C6.568,6.389 8.761,6.389 10.02,7.647C10.231,7.86 10.231,8.204 10.019,8.416C9.913,8.521 9.774,8.574 9.635,8.574C9.496,8.574 9.356,8.521 9.25,8.415C8.403,7.567 6.926,7.567 6.079,8.415C5.868,8.628 5.523,8.628 5.31,8.417C5.098,8.205 5.098,7.861 5.31,7.648ZM7.664,4.104C8.538,4.104 9.381,4.3 10.151,4.659C10.147,4.734 10.13,4.805 10.129,4.882C10.129,5.299 10.222,5.677 10.368,6.027C9.577,5.486 8.644,5.191 7.665,5.191C6.38,5.191 5.173,5.692 4.264,6.601C4.053,6.813 3.709,6.814 3.495,6.601C3.283,6.389 3.283,6.045 3.495,5.832C4.609,4.718 6.089,4.104 7.664,4.104ZM7.665,9.247C7.285,9.247 6.978,9.555 6.978,9.934C6.978,10.313 7.286,10.621 7.665,10.621C8.045,10.621 8.352,10.313 8.352,9.934C8.352,9.555 8.045,9.247 7.665,9.247Z", "M1.785,4.12C1.573,4.333 1.573,4.677 1.785,4.889C1.891,4.995 2.03,5.048 2.169,5.048C2.308,5.048 2.447,4.996 2.553,4.89C3.919,3.524 5.733,2.772 7.665,2.772C8.636,2.772 9.577,2.963 10.447,3.325C10.577,3.028 10.779,2.715 11.018,2.394C9.975,1.934 8.84,1.685 7.665,1.685C5.444,1.685 3.355,2.551 1.785,4.12Z", "M11.4,1.5L14.4,1.5L14.4,2.35L12.3,2.35L12.2,3.45C12.5,3.3 12.8,3.25 13.1,3.25C14.1,3.25 14.65,3.85 14.65,4.9C14.65,5.9 14.0,6.65 12.9,6.65C12.0,6.65 11.35,6.2 11.2,5.35L12.2,5.2C12.3,5.6 12.55,5.8 12.9,5.8C13.35,5.8 13.65,5.45 13.65,4.9C13.65,4.35 13.4,4.05 12.95,4.05C12.65,4.05 12.4,4.2 12.3,4.4L11.4,4.3Z"},
        {"M10.02,7.647C10.231,7.86 10.231,8.204 10.019,8.416C9.913,8.521 9.774,8.574 9.635,8.574C9.496,8.574 9.356,8.521 9.25,8.415C8.403,7.567 6.926,7.567 6.079,8.415C5.868,8.628 5.523,8.628 5.31,8.417C5.098,8.205 5.098,7.861 5.31,7.648C6.568,6.389 8.761,6.389 10.02,7.647ZM7.664,4.104C8.538,4.104 9.381,4.3 10.151,4.659C10.147,4.734 10.13,4.805 10.129,4.882C10.129,5.299 10.222,5.677 10.368,6.027C9.577,5.486 8.644,5.191 7.665,5.191C6.38,5.191 5.173,5.692 4.264,6.601C4.053,6.813 3.709,6.814 3.495,6.601C3.283,6.389 3.283,6.045 3.495,5.832C4.609,4.718 6.089,4.104 7.664,4.104ZM7.665,1.685C8.84,1.685 9.975,1.934 11.018,2.394C10.779,2.715 10.577,3.028 10.447,3.325C9.577,2.963 8.636,2.772 7.665,2.772C5.733,2.772 3.919,3.524 2.553,4.89C2.447,4.996 2.308,5.048 2.169,5.048C2.03,5.048 1.891,4.995 1.785,4.889C1.573,4.677 1.573,4.333 1.785,4.12C3.355,2.551 5.444,1.685 7.665,1.685ZM7.665,9.247C7.285,9.247 6.978,9.555 6.978,9.934C6.978,10.313 7.286,10.621 7.665,10.621C8.045,10.621 8.352,10.313 8.352,9.934C8.352,9.555 8.045,9.247 7.665,9.247ZM11.4,1.5L14.4,1.5L14.4,2.35L12.3,2.35L12.2,3.45C12.5,3.3 12.8,3.25 13.1,3.25C14.1,3.25 14.65,3.85 14.65,4.9C14.65,5.9 14.0,6.65 12.9,6.65C12.0,6.65 11.35,6.2 11.2,5.35L12.2,5.2C12.3,5.6 12.55,5.8 12.9,5.8C13.35,5.8 13.65,5.45 13.65,4.9C13.65,4.35 13.4,4.05 12.95,4.05C12.65,4.05 12.4,4.2 12.3,4.4L11.4,4.3Z"},
    };
    private static final float[][] W5_A = {
        {0.3961f, 1.0f},
        {0.3961f, 1.0f, 1.0f},
        {1.0f, 0.3961f, 1.0f},
        {1.0f, 0.3961f, 1.0f},
        {1.0f},
    };

    // resource id -> encoded (std*10 + level), or -1 when not ours
    private static final SparseIntArray CACHE = new SparseIntArray();

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lp) {
        if (!SUI.equals(lp.packageName)) return;
        hookCts(lp.classLoader);
        final ClassLoader cl = lp.classLoader;
        final Class<?> sam = XposedHelpers.findClassIfExists(SAM, cl);
        if (sam == null) return; // secondary classloader
        try {
            final Class<?> icons = XposedHelpers.findClass(ICONS, cl);
            final Object g6e = XposedHelpers.getStaticObjectField(sam, "WIFI_ICON_6GE");
            final Object g7 = XposedHelpers.getStaticObjectField(sam, "WIFI_ICON_7G");
            final Object standard = XposedHelpers.getStaticObjectField(icons, "UNMERGED_WIFI");

            XposedHelpers.findAndHookMethod(LAMBDA, cl, "invokeSuspend", Object.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam p) {
                    try {
                        Object m = XposedHelpers.getObjectField(p.thisObject, "L$0");
                        if (m != null && m.getClass().getName().endsWith("WifiNetworkModel$Active")) {
                            int std = XposedHelpers.getIntField(m, "wifiStandard");
                            p.setObjectExtra("std", Integer.valueOf(std));
                            XposedBridge.log("StdWifiIcon: wifiStandard=" + std);
                        }
                    } catch (Throwable t) {
                        XposedBridge.log("StdWifiIcon: before failed " + t);
                    }
                }

                @Override
                protected void afterHookedMethod(MethodHookParam p) {
                    Object std = p.getObjectExtra("std");
                    if (std == null || p.getResult() != standard) return;
                    int s = ((Integer) std).intValue();
                    if (s == 4) p.setResult(g6e);
                    else if (s == 5) p.setResult(g7);
                }
            });

            XposedHelpers.findAndHookMethod("android.content.res.ResourcesImpl", null, "loadDrawable",
                    Resources.class, TypedValue.class, int.class, int.class, Resources.Theme.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam p) {
                            try {
                                Resources res = (Resources) p.args[0];
                                int id = ((Integer) p.args[2]).intValue();
                                int code = lookup(res, id);
                                if (code < 0) return;
                                int std = code / 10, level = code % 10;
                                p.setResult(new BadgeIcon(std == 4 ? W4_P[level] : W5_P[level],
                                        std == 4 ? W4_A[level] : W5_A[level],
                                        res.getDisplayMetrics().density));
                            } catch (Throwable t) {
                                XposedBridge.log("StdWifiIcon: drawable failed " + t);
                            }
                        }
                    });
            XposedBridge.log("StdWifiIcon: hooked OK");
        } catch (Throwable t) {
            XposedBridge.log("StdWifiIcon: failed " + t);
        }
        hookDoubleTapSleep(cl);
    }

    // ---- Circle to Search unlock (AssistStateManager.isCsHelperAvailable -> true) ----
    private static final String CTS_TARGET = "com.android.systemui.plugins.omni.AssistStateManager";
    private static volatile boolean ctsHooked = false;
    private static volatile boolean ctsLoaderHooked = false;

    private static void hookCts(ClassLoader cl) {
        if (ctsHooked) return;
        try {
            hookCtsClass(XposedHelpers.findClass(CTS_TARGET, cl));
        } catch (Throwable t) {
            XposedBridge.log("StdWifiIcon: CTS direct lookup failed, waiting for loadClass");
        }
        if (ctsLoaderHooked || ctsHooked) return;
        try {
            XposedHelpers.findAndHookMethod(ClassLoader.class, "loadClass", String.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (ctsHooked) return;
                    Object r = param.getResult();
                    if (r instanceof Class && CTS_TARGET.equals(((Class<?>) r).getName())) hookCtsClass((Class<?>) r);
                }
            });
            ctsLoaderHooked = true;
        } catch (Throwable t) {
            XposedBridge.log("StdWifiIcon: CTS loadClass hook failed " + t);
        }
    }

    private static synchronized void hookCtsClass(Class<?> c) {
        if (ctsHooked) return;
        int n = 0;
        for (Method m : c.getDeclaredMethods()) {
            if (!m.getName().equals("isCsHelperAvailable")) continue;
            final Class<?> rt = m.getReturnType();
            XposedBridge.hookMethod(m, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (rt == Boolean.TYPE || rt == Boolean.class) param.setResult(Boolean.TRUE);
                    else if (rt == Optional.class) param.setResult(Optional.of(Boolean.TRUE));
                }
            });
            XposedBridge.log("StdWifiIcon: CTS hooked " + c.getName() + "#" + m.getName());
            n++;
        }
        ctsHooked = n > 0;
    }

    private static long lastDown = 0L;

    /** Double tap on the status bar -> turn the screen off (two ACTION_DOWN events 40..300 ms apart). */
    private static void hookDoubleTapSleep(final ClassLoader cl) {
        try {
            XposedHelpers.findAndHookMethod("com.android.systemui.statusbar.phone.PhoneStatusBarView", cl,
                    "onTouchEvent", MotionEvent.class, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam p) {
                            try {
                                MotionEvent e = (MotionEvent) p.args[0];
                                if (e.getAction() != MotionEvent.ACTION_DOWN) return;
                                long now = System.nanoTime();
                                long dt = now - lastDown;
                                if (dt >= 40000000L && dt <= 300000000L) {
                                    lastDown = 0L;
                                    Context ctx = ((View) p.thisObject).getContext();
                                    PowerManager pm = (PowerManager) ctx.getSystemService(Context.POWER_SERVICE);
                                    XposedHelpers.callMethod(pm, "goToSleep", SystemClock.uptimeMillis());
                                    p.setResult(Boolean.TRUE);
                                } else {
                                    lastDown = now;
                                }
                            } catch (Throwable t) {
                                XposedBridge.log("StdWifiIcon: tap failed " + t);
                            }
                        }
                    });
            XposedBridge.log("StdWifiIcon: double-tap hooked");
        } catch (Throwable t) {
            XposedBridge.log("StdWifiIcon: double-tap hook failed " + t);
        }
    }

    private static int lookup(Resources res, int id) {
        if ((id >>> 24) != 0x7f) return -1;
        int c = CACHE.get(id, -2);
        if (c != -2) return c;
        c = -1;
        try {
            if (SUI.equals(res.getResourcePackageName(id))) {
                String n = res.getResourceEntryName(id);
                if (n.startsWith("stat_sys_wifi6e_signal_")) c = 40 + (n.charAt(n.length() - 1) - '0');
                else if (n.startsWith("stat_sys_wifi7_signal_")) c = 50 + (n.charAt(n.length() - 1) - '0');
                if (c >= 0 && (c % 10) > 4) c = -1;
            }
        } catch (Throwable ignored) {
        }
        CACHE.put(id, c);
        return c;
    }

    /** Draws vector paths (M/L/C/H/V/Z, absolute) scaled from a 17x14 viewport; honours tint / colour filter. */
    static final class BadgeIcon extends Drawable {
        private static final Pattern TOK = Pattern.compile("[MLCHVZ]|-?\\d*\\.?\\d+");
        private final Path[] paths;
        private final Paint[] paints;
        private final int w, h;
        private ColorStateList tint;
        private PorterDuff.Mode mode = PorterDuff.Mode.SRC_IN;
        private ColorFilter filter;

        BadgeIcon(String[] data, float[] alpha, float density) {
            paths = new Path[data.length];
            paints = new Paint[data.length];
            for (int i = 0; i < data.length; i++) {
                paths[i] = parse(data[i]);
                Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
                p.setStyle(Paint.Style.FILL);
                p.setColor(0xFF000000);
                p.setAlpha(Math.round(alpha[i] * 255f));
                paints[i] = p;
            }
            w = Math.round(17f * density);
            h = Math.round(14f * density);
        }

        private static Path parse(String d) {
            Path path = new Path();
            ArrayList<String> t = new ArrayList<>();
            Matcher m = TOK.matcher(d);
            while (m.find()) t.add(m.group());
            int i = 0;
            float cx = 0, cy = 0;
            char cmd = 'M';
            while (i < t.size()) {
                String s = t.get(i);
                if (Character.isLetter(s.charAt(0))) { cmd = s.charAt(0); i++; if (cmd == 'Z') { path.close(); } continue; }
                switch (cmd) {
                    case 'M': cx = f(t, i); cy = f(t, i + 1); i += 2; path.moveTo(cx, cy); cmd = 'L'; break;
                    case 'L': cx = f(t, i); cy = f(t, i + 1); i += 2; path.lineTo(cx, cy); break;
                    case 'H': cx = f(t, i); i += 1; path.lineTo(cx, cy); break;
                    case 'V': cy = f(t, i); i += 1; path.lineTo(cx, cy); break;
                    case 'C':
                        path.cubicTo(f(t, i), f(t, i + 1), f(t, i + 2), f(t, i + 3), f(t, i + 4), f(t, i + 5));
                        cx = f(t, i + 4); cy = f(t, i + 5); i += 6; break;
                    default: i++;
                }
            }
            return path;
        }

        private static float f(ArrayList<String> t, int i) { return Float.parseFloat(t.get(i)); }

        @Override public void draw(Canvas c) {
            android.graphics.Rect b = getBounds();
            if (b.isEmpty()) return;
            c.save();
            c.translate(b.left, b.top);
            c.scale(b.width() / 17f, b.height() / 14f);
            for (int i = 0; i < paths.length; i++) {
                paints[i].setColorFilter(filter);
                c.drawPath(paths[i], paints[i]);
            }
            c.restore();
        }

        @Override public void setAlpha(int a) { /* alpha is baked per path; status bar uses tint */ }
        @Override public void setColorFilter(ColorFilter cf) { filter = cf; invalidateSelf(); }
        @Override public void setTintList(ColorStateList t) { tint = t; update(); }
        @Override public void setTintMode(PorterDuff.Mode m) { mode = m; update(); }
        @Override public boolean isStateful() { return tint != null && tint.isStateful(); }
        @Override protected boolean onStateChange(int[] s) { update(); return true; }
        private void update() {
            filter = tint == null ? null : new PorterDuffColorFilter(tint.getColorForState(getState(), tint.getDefaultColor()), mode);
            invalidateSelf();
        }
        @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
        @Override public int getIntrinsicWidth() { return w; }
        @Override public int getIntrinsicHeight() { return h; }
    }
}
