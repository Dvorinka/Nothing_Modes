package com.tdvorak.nothingmodes.data.prefs

import android.content.Context
import android.content.SharedPreferences
import com.tdvorak.nothingmodes.engine.model.EngineJson
import com.tdvorak.nothingmodes.engine.model.NotifyRule
import kotlinx.serialization.builtins.ListSerializer

/** Per-mode notification rules default. Empty means off by default. */
class NotificationPreferences(
    context: Context,
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getDefaultRules(): List<NotifyRule> {
        val raw = prefs.getString(KEY_DEFAULT_RULES, null) ?: return emptyList()
        return runCatching {
            EngineJson.json.decodeFromString(ListSerializer(NotifyRule.serializer()), raw)
        }.getOrDefault(emptyList())
    }

    fun setDefaultRules(rules: List<NotifyRule>) {
        prefs.edit()
            .putString(
                KEY_DEFAULT_RULES,
                EngineJson.json.encodeToString(ListSerializer(NotifyRule.serializer()), rules),
            )
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "notification_preferences"
        private const val KEY_DEFAULT_RULES = "default_notify_rules"
    }
}
