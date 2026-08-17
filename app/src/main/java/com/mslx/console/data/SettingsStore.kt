package com.mslx.console.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

/** 一个 Daemon 连接配置。 */
data class DaemonConfig(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val baseUrl: String = "",
    val apiKey: String = "",
)

enum class ThemeMode { DYNAMIC, SEED }

/** 应用全局设置(主题 + 多 Daemon + 引导状态)。 */
data class AppSettings(
    val daemons: List<DaemonConfig> = emptyList(),
    val activeDaemonId: String? = null,
    val themeMode: ThemeMode = ThemeMode.SEED,
    val seedColor: Long = 0xFF00838F,
    val onboarded: Boolean = false,
) {
    val activeDaemon: DaemonConfig?
        get() = daemons.firstOrNull { it.id == activeDaemonId }
}

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

class SettingsStore(private val context: Context) {

    private val gson = Gson()
    private val mutex = Mutex()

    private object Keys {
        val DAEMONS = stringPreferencesKey("daemons")
        val ACTIVE_DAEMON = stringPreferencesKey("active_daemon")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val SEED_COLOR = longPreferencesKey("seed_color")
        val ONBOARDED = booleanPreferencesKey("onboarded")
    }

    val settingsFlow: Flow<AppSettings> = context.settingsDataStore.data.map { prefs ->
        AppSettings(
            daemons = prefs[Keys.DAEMONS]?.let(::decodeDaemons) ?: emptyList(),
            activeDaemonId = prefs[Keys.ACTIVE_DAEMON]?.takeIf { it.isNotBlank() },
            themeMode = if (prefs[Keys.THEME_MODE] == "dynamic") ThemeMode.DYNAMIC else ThemeMode.SEED,
            seedColor = prefs[Keys.SEED_COLOR] ?: 0xFF00838F,
            onboarded = prefs[Keys.ONBOARDED] ?: false,
        )
    }

    suspend fun update(transform: (AppSettings) -> AppSettings) = mutex.withLock {
        val next = transform(settingsFlow.first())
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.DAEMONS] = gson.toJson(next.daemons)
            prefs[Keys.ACTIVE_DAEMON] = next.activeDaemonId ?: ""
            prefs[Keys.THEME_MODE] = if (next.themeMode == ThemeMode.SEED) "seed" else "dynamic"
            prefs[Keys.SEED_COLOR] = next.seedColor
            prefs[Keys.ONBOARDED] = next.onboarded
        }
    }

    /** 新增或更新一个 Daemon 并设为当前激活项。 */
    suspend fun upsertDaemon(config: DaemonConfig) = update { s ->
        val exists = s.daemons.any { it.id == config.id }
        val daemons = if (exists) {
            s.daemons.map { if (it.id == config.id) config else it }
        } else {
            s.daemons + config
        }
        s.copy(daemons = daemons, activeDaemonId = config.id)
    }

    suspend fun removeDaemon(id: String) = update { s ->
        val daemons = s.daemons.filter { it.id != id }
        val active = if (s.activeDaemonId == id) daemons.firstOrNull()?.id else s.activeDaemonId
        s.copy(daemons = daemons, activeDaemonId = active)
    }

    suspend fun setActiveDaemon(id: String) = update { it.copy(activeDaemonId = id) }

    suspend fun setTheme(mode: ThemeMode, seedColor: Long) =
        update { it.copy(themeMode = mode, seedColor = seedColor) }

    suspend fun markOnboarded() = update { it.copy(onboarded = true) }

    private fun decodeDaemons(json: String): List<DaemonConfig> = runCatching {
        gson.fromJson<List<DaemonConfig>>(json, object : TypeToken<List<DaemonConfig>>() {}.type)
    }.getOrDefault(emptyList())
}
