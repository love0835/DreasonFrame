package com.dreason.frame.core.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.dreason.frame.core.model.Route
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "dreason_preferences")

@Singleton
class AppPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val CHINA_SERVER_ID = longPreferencesKey("china_server_id")
        val TAIWAN_SERVER_ID = longPreferencesKey("taiwan_server_id")
        val DEFAULT_ROUTE = stringPreferencesKey("default_route")
        val DNS_SERVER = stringPreferencesKey("dns_server")
        val FALLBACK_DNS_SERVER = stringPreferencesKey("fallback_dns_server")
        val AUTO_START = booleanPreferencesKey("auto_start")
        val NOTIFICATION_ENABLED = booleanPreferencesKey("notification_enabled")
        val LOG_RETENTION_DAYS = intPreferencesKey("log_retention_days")
        val IPV6_ENABLED = booleanPreferencesKey("ipv6_enabled")
        val RULES_IMPORTED = booleanPreferencesKey("rules_imported")
    }

    val chinaServerId: Flow<Long?> = context.dataStore.data.map { it[Keys.CHINA_SERVER_ID] }
    val taiwanServerId: Flow<Long?> = context.dataStore.data.map { it[Keys.TAIWAN_SERVER_ID] }
    val defaultRoute: Flow<Route> = context.dataStore.data.map {
        it[Keys.DEFAULT_ROUTE]?.let { name -> Route.valueOf(name) } ?: Route.DIRECT
    }
    val dnsServer: Flow<String> = context.dataStore.data.map { it[Keys.DNS_SERVER] ?: "223.5.5.5" }
    val fallbackDnsServer: Flow<String> = context.dataStore.data.map { it[Keys.FALLBACK_DNS_SERVER] ?: "8.8.8.8" }
    val autoStart: Flow<Boolean> = context.dataStore.data.map { it[Keys.AUTO_START] ?: false }
    val notificationEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.NOTIFICATION_ENABLED] ?: true }
    val logRetentionDays: Flow<Int> = context.dataStore.data.map { it[Keys.LOG_RETENTION_DAYS] ?: 7 }
    val ipv6Enabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.IPV6_ENABLED] ?: true }
    val rulesImported: Flow<Boolean> = context.dataStore.data.map { it[Keys.RULES_IMPORTED] ?: false }

    suspend fun setChinaServerId(id: Long?) {
        context.dataStore.edit { prefs ->
            if (id != null) prefs[Keys.CHINA_SERVER_ID] = id
            else prefs.remove(Keys.CHINA_SERVER_ID)
        }
    }

    suspend fun setTaiwanServerId(id: Long?) {
        context.dataStore.edit { prefs ->
            if (id != null) prefs[Keys.TAIWAN_SERVER_ID] = id
            else prefs.remove(Keys.TAIWAN_SERVER_ID)
        }
    }

    suspend fun setDefaultRoute(route: Route) {
        context.dataStore.edit { it[Keys.DEFAULT_ROUTE] = route.name }
    }

    suspend fun setDnsServer(server: String) {
        context.dataStore.edit { it[Keys.DNS_SERVER] = server }
    }

    suspend fun setRulesImported(imported: Boolean) {
        context.dataStore.edit { it[Keys.RULES_IMPORTED] = imported }
    }
}
