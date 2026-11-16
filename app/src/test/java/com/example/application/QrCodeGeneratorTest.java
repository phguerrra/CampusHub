package com.example.application;

import static org.junit.Assert.assertNull;

import android.graphics.Bitmap;

import org.junit.Test;

public class QrCodeGeneratorTest {

    @Test
    public void generateQrCode_nullOrEmptyContent_returnsNull() throws Exception {
        assertNull(QrCodeGenerator.generateQrCode(null, 200));
        assertNull(QrCodeGenerator.generateQrCode("   ", 200));
    }
}
