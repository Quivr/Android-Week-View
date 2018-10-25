package com.alamkanak.weekview

import java.util.*

data class SimpleDate(val year: Int, val month: Int, val dayOfMonth: Int) {
    override fun toString(): String = "$year-$month-$dayOfMonth"

    constructor(cal: Calendar) : this(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
}
