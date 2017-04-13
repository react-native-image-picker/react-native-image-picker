package com.imagepicker.testing.utils;

import android.net.Uri;

import com.imagepicker.utils.MediaUtils;
import com.imagepicker.utils.RealPathUtil;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.internal.SdkEnvironment;

import java.io.File;

import static junit.framework.Assert.assertNotNull;

/**
 * Created by rusfearuth on 13.04.17.
 */

@RunWith(RobolectricTestRunner.class)
public class RealPathUtilTest
{
    @Test
    public void testCompatUriFromFile()
    {
        File newFile = MediaUtils.createNewFile(RuntimeEnvironment.application);
        Uri uri = RealPathUtil.compatUriFromFile(RuntimeEnvironment.application, newFile);
        assertNotNull("Uri was created", uri);
    }
}
