package com.imagepicker;

import com.facebook.react.ReactPackage;
import com.facebook.react.bridge.JavaScriptModule;
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.uimanager.ViewManager;

import android.app.Activity;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ImagePickerPackage implements ReactPackage {
  private final Activity mMainActivity;

  public ImagePickerPackage(Activity mainActivity) {
    this.mMainActivity = mainActivity;
  }

  @Override
  public List<NativeModule> createNativeModules(ReactApplicationContext reactContext) {
    return Arrays.<NativeModule>asList(new ImagePickerModule(reactContext, mMainActivity));
  }

  @Override
  public List<Class<? extends JavaScriptModule>> createJSModules() {
    return Collections.emptyList();
  }

  @Override
  public List<ViewManager> createViewManagers(ReactApplicationContext reactContext) {
    return Collections.emptyList();
  }
}