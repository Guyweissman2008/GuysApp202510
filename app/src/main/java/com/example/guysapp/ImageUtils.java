package com.example.guysapp;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ImageUtils {

    // הפיכת Bitmap לרשימת אינטג'רים עבור Firestore
    public static List<Integer> processBitmapToIntegerList(Bitmap bitmap, int maxDimension) {
        Bitmap resizedBitmap = scaleBitmapDown(bitmap, maxDimension);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
        byte[] data = baos.toByteArray();

        List<Integer> imageData = new ArrayList<>();
        for (byte b : data) {
            imageData.add(b & 0xFF);
        }
        return imageData;
    }

    // טיפול ב-Uri (גלריה) והפיכתו לרשימת אינטג'רים
    public static List<Integer> processUriToIntegerList(ContentResolver resolver, Uri uri, int maxDimension) throws IOException {
        Bitmap originalBitmap = MediaStore.Images.Media.getBitmap(resolver, uri);
        return processBitmapToIntegerList(originalBitmap, maxDimension);
    }

    // הפיכת רשימת אינטג'רים (מ-Firestore) בחזרה ל-Bitmap לתצוגה
    public static Bitmap decodeFirestoreData(List<?> imageDataRaw) {
        if (imageDataRaw == null || imageDataRaw.isEmpty()) return null;

        byte[] bytes = new byte[imageDataRaw.size()];
        for (int i = 0; i < imageDataRaw.size(); i++) {
            Object o = imageDataRaw.get(i);
            int value = (o instanceof Number) ? ((Number) o).intValue() : 0;
            bytes[i] = (byte) (value & 0xFF);
        }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    // פונקציית העזר לכיווץ התמונה
    private static Bitmap scaleBitmapDown(Bitmap bitmap, int maxDimension) {
        int originalWidth = bitmap.getWidth();
        int originalHeight = bitmap.getHeight();
        int resizedWidth = maxDimension;
        int resizedHeight = maxDimension;

        if (originalHeight > originalWidth) {
            resizedHeight = maxDimension;
            resizedWidth = (int) (resizedHeight * (float) originalWidth / (float) originalHeight);
        } else if (originalWidth > originalHeight) {
            resizedWidth = maxDimension;
            resizedHeight = (int) (resizedWidth * (float) originalHeight / (float) originalWidth);
        }

        return Bitmap.createScaledBitmap(bitmap, resizedWidth, resizedHeight, false);
    }
}