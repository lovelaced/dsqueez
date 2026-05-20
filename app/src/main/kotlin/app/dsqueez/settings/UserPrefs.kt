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
        val DEFAULT_RATIO = floatPreferencesKey("default_ratio")
    }

    /**
     * Your preferred squeeze factor. Used as the initial selection when a
     * photo has no EXIF lens hint. Set via long-press on a ratio chip.
     */
    val defaultRatio: Flow<Float> = context.dataStore.data.map { it[Keys.DEFAULT_RATIO] ?: 1.33f }

    suspend fun setDefaultRatio(value: Float) {
        context.dataStore.edit { it[Keys.DEFAULT_RATIO] = value }
    }
}
