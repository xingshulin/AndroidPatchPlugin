package com.xingshulin.singularity.utils

import java.text.DateFormat
import java.text.SimpleDateFormat

class DateUtils {
    static String format(long timestamp) {
        Date date = new Date(timestamp);
        DateFormat formatter = new SimpleDateFormat("MM,dd HH:mm:ss");
        return formatter.format(date);
    }
}
