package com.imagepicker;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class UtilsTest {
    @Test
    public void samplesLargeImagesWithoutDecodingBelowTheTargetSize() {
        assertEquals(2, Utils.calculateInSampleSize(4000, 3000, 1024, 768));
        assertEquals(4, Utils.calculateInSampleSize(8000, 6000, 1024, 768));
    }

    @Test
    public void keepsOriginalDecodeSizeWhenSamplingWouldUndershoot() {
        assertEquals(1, Utils.calculateInSampleSize(1024, 768, 1024, 768));
        assertEquals(1, Utils.calculateInSampleSize(1600, 1200, 1024, 768));
    }
}
