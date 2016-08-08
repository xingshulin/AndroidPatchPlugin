package com.xingshulin.singularity.utils;

import android.util.Log;

import java.lang.reflect.Array;
import java.lang.reflect.Field;

public class ReflectionUtils {
    private static String TAG = ReflectionUtils.class.getSimpleName();

    private static Field getField(String name, Class<?> clazz) {
        try {
            Field field = clazz.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException e) {
            Log.w(TAG, e.getMessage(), e);
        }
        return null;
    }

    public static void setField(Object target, Class<?> clazz, String field, Object value) {
        try {
            getField(field, clazz).set(target, value);
        } catch (Exception e) {
            Log.w(TAG, e.getMessage(), e);
        }
    }

    public static Object getFieldValue(Object target, Class<?> clazz, String field) {
        try {
            return getField(field, clazz).get(target);
        } catch (Exception e) {
            Log.w(TAG, e.getMessage(), e);
        }
        return null;
    }

    public static Object getDexElements(Object dexListObject) {
        return getFieldValue(dexListObject, dexListObject.getClass(), "dexElements");
    }

    public static Object getPathList(ClassLoader classLoader) {
        try {
            return getFieldValue(classLoader, Class.forName("dalvik.system.BaseDexClassLoader"), "pathList");
        } catch (ClassNotFoundException e) {
            Log.w(TAG, e.getMessage(), e);
        }
        return null;
    }
}
