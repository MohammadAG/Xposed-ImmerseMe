package com.mohammadag.immerseme;

import android.app.Activity;
import android.os.Handler;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class ImmerseMe implements IXposedHookZygoteInit, IXposedHookLoadPackage {

	@Override
	public void initZygote(StartupParam startupParam) throws Throwable {
		XposedHelpers.findAndHookMethod(Activity.class, "onResume", new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				Activity activity = (Activity) param.thisObject;
				Window window = activity.getWindow();
				window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
				View decorView = window.getDecorView();
				decorView.setSystemUiVisibility(
						View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
						| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
						| View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
			}
		});
	}

	@Override
	public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
		if (!lpparam.packageName.equals("android"))
			return;

		XposedHelpers.findAndHookMethod("com.android.internal.policy.impl.ImmersiveModeConfirmation", lpparam.classLoader,
				"immersiveModeChanged", String.class, boolean.class, new XC_MethodReplacement() {

			@Override
			protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
				final Object thisObject = param.thisObject;
				final String pkg = (String) param.args[0];
				boolean isImmersiveMode = (Boolean) param.args[1];
				final Object confirmedPackages =  XposedHelpers.getObjectField(param.thisObject, "mConfirmedPackages");
				boolean packageConfirmed = (Boolean) XposedHelpers.callMethod(confirmedPackages, "contains", pkg);
				Handler handler = (Handler) XposedHelpers.getObjectField(param.thisObject, "mHandler");
				if (isImmersiveMode) {
					XposedHelpers.setObjectField(param.thisObject, "mLastPackage", pkg);
					if (!packageConfirmed) {
						handler.post(new Runnable() {
							@Override
							public void run() {
								XposedHelpers.callMethod(confirmedPackages, "add", pkg);
								XposedHelpers.callMethod(thisObject, "saveSetting");
							}
						});
					}
				} else {
					XposedHelpers.setObjectField(param.thisObject, "mLastPackage", null);
				}
				return null;
			}
		});
	}
}
