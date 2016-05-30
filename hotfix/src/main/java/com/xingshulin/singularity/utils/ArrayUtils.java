package com.xingshulin.singularity.utils;

import java.lang.reflect.Array;

public class ArrayUtils {

    public static Object concat(Object firstArray, Object secondArray) {
        Class<?> localClass = firstArray.getClass().getComponentType();
        int firstArrayLength = Array.getLength(firstArray);
        int totalLength = firstArrayLength + Array.getLength(secondArray);
        Object result = Array.newInstance(localClass, totalLength);
        for (int i = 0; i < totalLength; i++) {
            if (i < firstArrayLength) {
                Array.set(result, i, Array.get(firstArray, i));
            } else {
                Array.set(result, i, Array.get(secondArray, i - firstArrayLength));
            }
        }
        return result;
    }
}
