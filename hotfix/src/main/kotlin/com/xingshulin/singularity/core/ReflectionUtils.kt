package com.xingshulin.singularity.core

import java.lang.reflect.Field

fun getDexElements(dexListObject: Any): Array<*> {
    return getFieldValue(dexListObject, dexListObject.javaClass, "dexElements") as Array<*>
}

fun getPathList(baseDexClassLoader: ClassLoader): Any {
    return getFieldValue(baseDexClassLoader, Class.forName("dalvik.system.BaseDexClassLoader"), "pathList")
}

fun getPathClassLoader() = object {}.javaClass.classLoader

fun getFieldValue(target: Any, javaClass: Class<*>, field: String): Any {
    val declaredField = getField(field, javaClass)
    return declaredField.get(target)
}

fun setField(target: Any, javaClass: Class<*>, field: String, value: Any) {
    getField(field, javaClass).set(target, value)
}

fun getField(field: String, javaClass: Class<*>): Field {
    val declaredField = javaClass.getDeclaredField(field)
    declaredField.isAccessible = true
    return declaredField
}