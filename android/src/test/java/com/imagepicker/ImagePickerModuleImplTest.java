package com.imagepicker;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.robolectric.Shadows.shadowOf;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Looper;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.BridgeReactContext;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.JavaOnlyArray;
import com.facebook.react.bridge.JavaOnlyMap;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.LooperMode;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.PAUSED)
@Config(sdk = 35, shadows = ImagePickerModuleImplTest.ShadowArguments.class)
@SuppressWarnings("deprecation")
public class ImagePickerModuleImplTest {
    private static final Callback NOOP_CALLBACK = new Callback() {
        @Override
        public void invoke(Object... args) {
        }
    };

    private ExecutorService executor;
    private BridgeReactContext reactContext;
    private ImagePickerModuleImpl module;
    private RecordingActivity firstActivity;
    private RecordingActivity secondActivity;

    @Before
    public void setUp() {
        firstActivity = createActivity();
        secondActivity = createActivity();

        reactContext = new BridgeReactContext(RuntimeEnvironment.getApplication());
        reactContext.onHostResume(firstActivity);
        shadowOf(reactContext.getPackageManager()).setSystemFeature(PackageManager.FEATURE_CAMERA, true);

        module = new ImagePickerModuleImpl(reactContext);
        executor = Executors.newSingleThreadExecutor();
    }

    @After
    public void tearDown() {
        executor.shutdownNow();
    }

    @Test
    public void launchImageLibraryCalledOffMainStartsActivityOnMainLooper() throws Exception {
        launchImageLibraryOffMain();

        assertEquals(0, firstActivity.launchCount);

        shadowOf(Looper.getMainLooper()).idle();

        assertEquals(1, firstActivity.launchCount);
        assertEquals(ImagePickerModuleImpl.REQUEST_LAUNCH_LIBRARY, firstActivity.launchedRequestCode);
        assertSame(Looper.getMainLooper(), firstActivity.launchedOnLooper);
    }

    @Test
    public void launchImageLibraryUsesActivityCurrentWhenUiWorkRuns() throws Exception {
        launchImageLibraryOffMain();

        assertEquals(0, firstActivity.launchCount);

        reactContext.onHostResume(secondActivity);
        shadowOf(Looper.getMainLooper()).idle();

        assertEquals(0, firstActivity.launchCount);
        assertEquals(1, secondActivity.launchCount);
        assertEquals(ImagePickerModuleImpl.REQUEST_LAUNCH_LIBRARY, secondActivity.launchedRequestCode);
        assertSame(Looper.getMainLooper(), secondActivity.launchedOnLooper);
    }

    @Test
    public void launchCameraCalledOffMainStartsActivityOnMainLooper() throws Exception {
        executor.submit(() -> module.launchCamera(defaultOptions(), NOOP_CALLBACK)).get(5, SECONDS);

        assertEquals(0, firstActivity.launchCount);

        shadowOf(Looper.getMainLooper()).idle();

        assertEquals(1, firstActivity.launchCount);
        assertEquals(ImagePickerModuleImpl.REQUEST_LAUNCH_IMAGE_CAPTURE, firstActivity.launchedRequestCode);
        assertSame(Looper.getMainLooper(), firstActivity.launchedOnLooper);
    }

    @Test
    public void activityNotFoundReturnsOneErrorAndClearsMatchingCallback() throws Exception {
        ActivityNotFoundRecordingActivity activity = Robolectric
                .buildActivity(ActivityNotFoundRecordingActivity.class)
                .setup()
                .get();
        reactContext.onHostResume(activity);
        CapturingCallback callback = new CapturingCallback();
        module.callback = callback;

        executor.submit(() -> module.startActivityForResultOnUiThread(
                new Intent(),
                ImagePickerModuleImpl.REQUEST_LAUNCH_LIBRARY,
                callback
        )).get(5, SECONDS);

        assertEquals(0, callback.invocationCount);

        shadowOf(Looper.getMainLooper()).idle();

        assertEquals(1, callback.invocationCount);
        assertSame(Looper.getMainLooper(), callback.invokedOnLooper);
        ReadableMap response = (ReadableMap) callback.arguments[0];
        assertEquals("others", response.getString("errorCode"));
        assertEquals("No activity found", response.getString("errorMessage"));
        assertNull(module.callback);
    }

    @Test
    public void activityNotFoundDoesNotClearReplacementCallback() throws Exception {
        ActivityNotFoundRecordingActivity activity = Robolectric
                .buildActivity(ActivityNotFoundRecordingActivity.class)
                .setup()
                .get();
        reactContext.onHostResume(activity);
        CapturingCallback launchCallback = new CapturingCallback();
        Callback replacementCallback = NOOP_CALLBACK;
        module.callback = launchCallback;

        executor.submit(() -> module.startActivityForResultOnUiThread(
                new Intent(),
                ImagePickerModuleImpl.REQUEST_LAUNCH_LIBRARY,
                launchCallback
        )).get(5, SECONDS);
        module.callback = replacementCallback;

        shadowOf(Looper.getMainLooper()).idle();

        assertEquals(1, launchCallback.invocationCount);
        assertSame(replacementCallback, module.callback);
    }

    private void launchImageLibraryOffMain() throws Exception {
        executor.submit(() -> module.launchImageLibrary(defaultOptions(), NOOP_CALLBACK)).get(5, SECONDS);
    }

    private static RecordingActivity createActivity() {
        return Robolectric.buildActivity(RecordingActivity.class).setup().get();
    }

    private static ReadableMap defaultOptions() {
        return JavaOnlyMap.of(
                "mediaType", "photo",
                "restrictMimeTypes", JavaOnlyArray.of(),
                "videoQuality", "high",
                "quality", 1.0,
                "maxWidth", 0,
                "maxHeight", 0,
                "includeBase64", false,
                "cameraType", "back",
                "selectionLimit", 1,
                "saveToPhotos", false,
                "durationLimit", 0,
                "includeExtra", false,
                "presentationStyle", "pageSheet",
                "assetRepresentationMode", "auto"
        );
    }

    public static class RecordingActivity extends Activity {
        volatile int launchCount;
        volatile int launchedRequestCode = -1;
        volatile Looper launchedOnLooper;

        @Override
        public void startActivityForResult(Intent intent, int requestCode) {
            launchCount++;
            launchedRequestCode = requestCode;
            launchedOnLooper = Looper.myLooper();
        }
    }

    public static class ActivityNotFoundRecordingActivity extends Activity {
        @Override
        public void startActivityForResult(Intent intent, int requestCode) {
            throw new ActivityNotFoundException("No activity found");
        }
    }

    private static class CapturingCallback implements Callback {
        int invocationCount;
        Object[] arguments;
        Looper invokedOnLooper;

        @Override
        public void invoke(Object... args) {
            invocationCount++;
            arguments = args;
            invokedOnLooper = Looper.myLooper();
        }
    }

    @Implements(value = Arguments.class, isInAndroidSdk = false)
    public static class ShadowArguments {
        @Implementation
        public static WritableMap createMap() {
            return new JavaOnlyMap();
        }
    }
}
