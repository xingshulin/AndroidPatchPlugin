package com.xingshulin.singularity.utils

class MapUtils {
    static String merge(Map<String, String> dest, Map<String, String> source, String ...keys) {
        keys.each { key ->
            dest.put(key, source.get(key))
        }
    }
}
