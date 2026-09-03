package com.tdvorak.nothingmodes.data

import androidx.room.TypeConverter
import com.tdvorak.nothingmodes.engine.model.AutomationStatus
import com.tdvorak.nothingmodes.engine.model.AutomationType

class Converters {

    @TypeConverter
    fun fromAutomationStatus(status: AutomationStatus): String = status.name

    @TypeConverter
    fun toAutomationStatus(value: String): AutomationStatus = AutomationStatus.valueOf(value)

    @TypeConverter
    fun fromAutomationType(type: AutomationType): String = type.name

    @TypeConverter
    fun toAutomationType(value: String): AutomationType = AutomationType.valueOf(value)
}
