package com.souvik.stdwifi;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class Hook implements IXposedHookLoadPackage {
    private static final String PKG = "com.android.systemui";
    private static final String LAMBDA =
            "com.android.systemui.statusbar.pipeline.wifi.domain.interactor.WifiInteractorImpl$wifiIconGroup$1";
    private static final String SAM = "com.android.systemui.statusbar.pipeline.wifi.ui.util.SamsungWifiIcons";
    private static final String ICONS = "com.android.systemui.statusbar.connectivity.WifiIcons";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lp) {
        if (!PKG.equals(lp.packageName)) return;
        XposedBridge.log("StdWifiIcon: systemui loaded");
        try {
            final ClassLoader cl = lp.classLoader;
            final Class<?> sam = XposedHelpers.findClass(SAM, cl);
            final Class<?> icons = XposedHelpers.findClass(ICONS, cl);
            final Object g6 = XposedHelpers.getStaticObjectField(sam, "WIFI_ICON_6G");
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
                            if (std == 6 || std == 8) {
                                XposedHelpers.setIntField(m, "wifiStandard", 5);
                                XposedBridge.log("StdWifiIcon: standard " + std + " -> 5");
                            }
                        }
                    } catch (Throwable t) {
                        XposedBridge.log("StdWifiIcon: before failed " + t);
                    }
                }

                @Override
                protected void afterHookedMethod(MethodHookParam p) {
                    Object r = p.getResult();
                    if (r != null && (r == g6 || r == g6e || r == g7)) {
                        p.setResult(standard);
                        XposedBridge.log("StdWifiIcon: group replaced");
                    }
                }
            });
            XposedBridge.log("StdWifiIcon: hooked");
        } catch (Throwable t) {
            XposedBridge.log("StdWifiIcon: failed " + t);
        }
    }
                }
