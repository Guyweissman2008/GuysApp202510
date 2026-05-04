package com.example.guysapp;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Log;
import androidx.annotation.Nullable;
import com.google.firebase.firestore.Blob;
import java.io.ByteArrayOutputStream;
import java.util.List;
public class ImageHelper {
    public static final boolean SMALL_IMAGE = true;
    public static final boolean NORMAL_IMAGE = false;
    private static final int PROFILE_IMAGE_SIZE = 300;
    private static final int RECIPE_IMAGE_SIZE = 450;
    private static final int JPEG_QUALITY = 40;
    private static final int MAX_IMAGE_BYTES = 1000 * 1024; // 1MB
    public static Bitmap loadBitmapFromUri(Context context, Uri uri) {
        if (uri == null) return null;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                return ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.getContentResolver(), uri));
            }
            return MediaStore.Images.Media.getBitmap(context.getContentResolver(), uri);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    public static Bitmap scaleForProfile(Bitmap bitmap) {
        return scaleBitmapDown(bitmap, PROFILE_IMAGE_SIZE);
    }
    public static Bitmap scaleForRecipe(Bitmap bitmap) {
        return scaleBitmapDown(bitmap, RECIPE_IMAGE_SIZE);
    }
    private static Bitmap scaleBitmapDown(Bitmap bitmap, int maxDimension) {
        if (bitmap == null) return null;
        int originalWidth = bitmap.getWidth();
        int originalHeight = bitmap.getHeight();
        if (originalWidth <= maxDimension && originalHeight <= maxDimension) return bitmap;
        int newWidth = maxDimension;
        int newHeight = maxDimension;
        if (originalHeight > originalWidth) {
            newWidth = (int) (maxDimension * ((float) originalWidth / (float) originalHeight));
        } else {
            newHeight = (int) (maxDimension * ((float) originalHeight / (float) originalWidth));
        }
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
    }
    @Nullable
    public static Blob bitmapToBlob(Context context, Bitmap bitmap, boolean isSmall) {
        if (bitmap == null) return null;
        int size = RECIPE_IMAGE_SIZE;
        if (isSmall)
            size = PROFILE_IMAGE_SIZE;
        Bitmap resizedBitmap = scaleBitmapDown(bitmap, size);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, baos);
        byte[] data = baos.toByteArray();
        if (resizedBitmap != bitmap) {
            resizedBitmap.recycle();
        }
        if (data.length > MAX_IMAGE_BYTES) {
            return null;
        }
        return Blob.fromBytes(data);
    }
    public static Bitmap decodeBlobToBitmap(Blob blob) {
        if (blob == null) return null;
        byte[] bytes = blob.toBytes();
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }
    public static Uri createImageUri(Context context, String title, String description) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, title);
        values.put(MediaStore.Images.Media.DESCRIPTION, description);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        return context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
    }
    public static Bitmap decodeFirestoreData(List<?> list) {
        if (list == null || list.isEmpty()) return null;

        try {
            byte[] bytes = new byte[list.size()];
            for (int i = 0; i < list.size(); i++) {
                bytes[i] = ((Number) list.get(i)).byteValue();
            }
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        } catch (Exception e) {
            Log.e("ImageHelper", "Error decoding legacy list", e);
            return null;
        }
    }
    public static byte[] listToByteArray(List<?> list) {
        if (list == null || list.isEmpty()) return null;
        byte[] bytes = new byte[list.size()];
        for (int i = 0; i < list.size(); i++) {
            Object value = list.get(i);
            // Firestore ׳׳¢׳™׳×׳™׳ ׳׳—׳–׳™׳¨ Long, ׳׳›׳ ׳ ׳׳™׳¨ ׳‘׳–׳”׳™׳¨׳•׳×
            int intValue = 0;
            if (value instanceof Number) {
                intValue = ((Number) value).intValue();
            }
            bytes[i] = (byte) intValue;
        }
        return bytes;
    }
}