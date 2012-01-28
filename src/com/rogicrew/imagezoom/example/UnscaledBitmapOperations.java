package com.rogicrew.imagezoom.example;

import java.lang.reflect.Field;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.os.Build;

//almost copy paste from http://blog.tomgibara.com/post/190539066/android-unscaled-bitmaps
public abstract class UnscaledBitmapOperations {

    public static final UnscaledBitmapOperations instance;

    static {
    	int sdkVersion = Integer.parseInt(Build.VERSION.SDK);
        instance = sdkVersion < 4 ? new Old() : new New();
    }

    public static Bitmap loadFromResource(Resources resources, int resId, BitmapFactory.Options options) {
        return instance.load(resources, resId, options);
    }

    private static class Old extends UnscaledBitmapOperations {

        @Override
        Bitmap load(Resources resources, int resId, Options options) {
            return BitmapFactory.decodeResource(resources, resId, options);
        }

    }

    private static class New extends UnscaledBitmapOperations {

        @Override
        Bitmap load(Resources resources, int resId, Options options) {
            if (options == null) options = new BitmapFactory.Options();
            try {
                Class<?> c = options.getClass();
                Field isScaledField = c.getDeclaredField("isScaled");
                isScaledField.setBoolean(c, true);
            }
            catch (Throwable e) {
                System.err.println(e);
            }
            return BitmapFactory.decodeResource(resources, resId, options);
        }

    }

    abstract Bitmap load(Resources resources, int resId, BitmapFactory.Options options);

}
