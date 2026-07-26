package github.leavesczy.xlog.decode.logic

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File

object DataStoreManager {

    private const val PREFERENCES_DIR_NAME = ".compose-multiplatform-xlog-decode"

    private const val PREFERENCES_FILE_NAME = "compose-multiplatform-xlog-decode.preferences_pb"

    private val dataStore = PreferenceDataStoreFactory.create {
        File(
            System.getProperty("user.home"),
            "$PREFERENCES_DIR_NAME/$PREFERENCES_FILE_NAME"
        )
    }

    private val PRIVATE_KEY = stringPreferencesKey("private_key")

    private val THEME_ID = intPreferencesKey("theme")

    private val AUTO_OPEN_ON_SUCCESS =
        booleanPreferencesKey("autoOpenFileWhenParsingIsSuccessful")

    fun privateKeyFlow(): Flow<String> {
        return dataStore.data.map { preferences ->
            preferences[PRIVATE_KEY].orEmpty()
        }
    }

    fun themeIdFlow(): Flow<Int> {
        return dataStore.data.map { preferences ->
            preferences[THEME_ID] ?: -1
        }
    }

    fun autoOpenOnSuccessFlow(): Flow<Boolean> {
        return dataStore.data.map { preferences ->
            preferences[AUTO_OPEN_ON_SUCCESS] ?: true
        }
    }

    suspend fun updatePrivateKey(privateKey: String) {
        dataStore.edit { preferences ->
            preferences[PRIVATE_KEY] = privateKey
        }
    }

    suspend fun updateThemeId(themeId: Int) {
        dataStore.edit { preferences ->
            preferences[THEME_ID] = themeId
        }
    }

    suspend fun updateAutoOpenOnSuccess(autoOpen: Boolean) {
        dataStore.edit { preferences ->
            preferences[AUTO_OPEN_ON_SUCCESS] = autoOpen
        }
    }

}