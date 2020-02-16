package com.imagepicker.testing;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.net.Uri;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.JavaOnlyArray;
import com.facebook.react.bridge.JavaOnlyMap;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.common.build.ReactBuildConfig;
import com.imagepicker.ImagePickerModule;
import com.imagepicker.R;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;

import java.io.File;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

/**
 * Created by rusfearuth on 10.04.17.
 */

@RunWith(RobolectricTestRunner.class)
@SuppressStaticInitializationFor("com.facebook.react.common.build.ReactBuildConfig")
@PrepareForTest({Arguments.class})
@PowerMockIgnore({"org.mockito.*", "org.robolectric.*", "android.*", "jdk.internal.reflect.*"})
public class ImagePickerModuleTest
{
    private static final int DEFAULT_THEME = R.style.DefaultExplainingPermissionsTheme;

    @Rule
    public PowerMockRule rule = new PowerMockRule();

    private ActivityController<Activity> activityController;
    private Activity activity;
    private ReactApplicationContext reactContext;

    private TestableImagePickerModule module;

    @Before
    public void setUp() throws Exception
    {
        nativeMock();

        activityController = Robolectric.buildActivity(Activity.class);
        activity = activityController.create().start().resume().get();
        reactContext = mock(ReactApplicationContext.class);

        module = new TestableImagePickerModule(reactContext, DEFAULT_THEME);
        assertNotNull("Module was created", module);
        when(reactContext.getCurrentActivity()).thenReturn(activity);
    }



    @After
    public void tearDown()
    {
        activityController.pause().stop().destroy();
        activity = null;
    }


    @Test
    public void testCancelTakingPhoto()
    {
        final SampleCallback callback = new SampleCallback();
        module.setCallback(callback);
        module.setCameraCaptureUri(Uri.fromFile(new File("")));
        module.onActivityResult(activity, ImagePickerModule.REQUEST_LAUNCH_IMAGE_CAPTURE, Activity.RESULT_CANCELED, null);
        assertFalse("Camera's been launched", callback.hasError());
        assertTrue("User's cancelled of taking a photo", callback.didCancel());
    }

    private void nativeMock()
    {
        PowerMockito.mockStatic(Arguments.class);
        when(Arguments.createArray()).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                return new JavaOnlyArray();
            }
        });
        when(Arguments.createMap()).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                return new JavaOnlyMap();
            }
        });
    }
}
