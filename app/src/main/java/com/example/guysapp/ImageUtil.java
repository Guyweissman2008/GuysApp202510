package com.example.guysapp;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.VectorDrawable;
import android.util.Base64;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

/// Utility class for image operations
/// Contains methods for requesting permissions, converting images to base64 and vice versa
public class ImageUtil {

    // Image compression settings
    private static final int IMAGE_MAX_SIZE_PX = 450;
    private static final int JPEG_QUALITY = 30;
    private static final int MAX_IMAGE_BYTES = 500 * 1024;

    public static Bitmap downscaleBitmap(Bitmap original) {
        int width = original.getWidth();
        int height = original.getHeight();
        float ratio = Math.min((float) IMAGE_MAX_SIZE_PX / width, (float) IMAGE_MAX_SIZE_PX / height);
        return Bitmap.createScaledBitmap(original, Math.round(width * ratio), Math.round(height * ratio), true);
    }

    @Nullable
    public static List<Integer> processSelectedImage(Context context, Bitmap bitmap) {
        Bitmap downscaled = ImageUtil.downscaleBitmap(bitmap);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        downscaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, baos);
        byte[] bytes = baos.toByteArray();

        if (bytes.length > MAX_IMAGE_BYTES) {
            Toast.makeText(context, "Image is too large, please try another", Toast.LENGTH_LONG).show();
            return null;
        }

        List<Integer> imageDataList = new ArrayList<>();
        for (byte b : bytes) imageDataList.add(b & 0xFF);
        return imageDataList;
    }

}