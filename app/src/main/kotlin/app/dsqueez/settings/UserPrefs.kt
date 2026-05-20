package app.dsqueez.settings

import android.content.Context
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "user_prefs")

class UserPrefs(private val context: Context) {

    private object Keys {
        val LAST_RATIO = floatPreferencesKey("last_ratio")
    }

    val lastRatio: Flow<Float> = context.dataStore.data.map { it[Keys.LAST_RATIO] ?: 1.33f }

    suspend fun setLastRatio(value: Float) {
        context.dataStore.edit { it[Keys.LAST_RATIO] = value }
    }
}
