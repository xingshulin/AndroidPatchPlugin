package com.xingshulin.singularity.utils

class MapUtils {
    static void merge(Map<String, String> dest, Map<String, String> source, String ...keys) {
        keys.each { key ->
            dest.put(key, source.get(key))
        }
    }

    static void copy(Map<String, String> dest, Map<String, String> source) {
        source.keySet().each { key ->
            dest.put(key, source.get(key))
        }
    }

    static void nullSafePut(Map<String, String> options, String key, String value) {
     if (value == null || key == null) return
     if (!options.containsKey(key)) {
         options[key] = value
     }
 }
}
