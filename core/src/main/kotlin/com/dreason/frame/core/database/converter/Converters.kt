package com.dreason.frame.core.database.converter

import androidx.room.TypeConverter
import com.dreason.frame.core.model.ProxyProtocol
import com.dreason.frame.core.model.Route
import com.dreason.frame.core.model.RuleType

class Converters {

    @TypeConverter
    fun fromProxyProtocol(value: ProxyProtocol): String = value.name

    @TypeConverter
    fun toProxyProtocol(value: String): ProxyProtocol = ProxyProtocol.valueOf(value)

    @TypeConverter
    fun fromRoute(value: Route): String = value.name

    @TypeConverter
    fun toRoute(value: String): Route = Route.valueOf(value)

    @TypeConverter
    fun fromRuleType(value: RuleType): String = value.name

    @TypeConverter
    fun toRuleType(value: String): RuleType = RuleType.valueOf(value)
}
