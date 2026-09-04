package com.tdvorak.nothingmodes.data

import androidx.room.TypeConverter
import com.tdvorak.nothingmodes.engine.model.AutomationStatus
import com.tdvorak.nothingmodes.engine.model.AutomationType

class Converters {

    @TypeConverter
    fun fromAutomationStatus(status: AutomationStatus): String = status.name

    @TypeConverter
    fun toAutomationStatus(value: String): AutomationStatus =
        runCatching { AutomationStatus.valueOf(value) }.getOrDefault(AutomationStatus.NEEDS_REVIEW)

    @TypeConverter
    fun fromAutomationType(type: AutomationType): String = type.name

    @TypeConverter
    fun toAutomationType(value: String): AutomationType =
        runCatching { AutomationType.valueOf(value) }.getOrDefault(AutomationType.ROUTINE)
}
