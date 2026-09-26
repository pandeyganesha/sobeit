package com.pandeyganesha.kaamsutra.data

import android.icu.util.Calendar

public fun calculateDelayUntil(hour: Int, minute: Int): Long {
    val now = Calendar.getInstance()
    val target = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        if (before(now)){
            add(Calendar.DAY_OF_YEAR, 1)
        }
    }
    return target.timeInMillis - now.timeInMillis
}