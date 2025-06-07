package github.leavesczy.xlog.decode

import androidx.datastore.preferences.core.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File

/**
 * @Author: leavesCZY
 * @Date: 2024/6/6 15:47
 * @Desc:
 */
object DataStoreManager {

    private val dataStores = PreferenceDataStoreFactory.create {
        File("${System.getProperty("user.dir")}/config/compose-multiplatform-xlog-decode.preferences_pb")
    }

    private val PRIVATE_KEY = stringPreferencesKey("private_key")

    private val THEME = intPreferencesKey("theme")

    private val AUTO_OPEN_FILE_WHEN_PARSING_IS_SUCCESSFUL =
        booleanPreferencesKey("autoOpenFileWhenParsingIsSuccessful")

    fun privateKeyFlow(): Flow<String> {
        return dataStores.data.map { preferences ->
            preferences[PRIVATE_KEY] ?: ""
        }
    }

    fun themeFlow(): Flow<Int> {
        return dataStores.data.map { preferences ->
            preferences[THEME] ?: -1
        }
    }

    fun autoOpenFileWhenParsingIsSuccessful(): Flow<Boolean> {
        return dataStores.data.map { preferences ->
            preferences[AUTO_OPEN_FILE_WHEN_PARSING_IS_SUCCESSFUL] ?: true
        }
    }

    suspend fun updatePrivateKey(privateKey: String) {
        dataStores.edit { settings ->
            settings[PRIVATE_KEY] = privateKey
        }
    }

    suspend fun updateTheme(theme: Int) {
        dataStores.edit { settings ->
            settings[THEME] = theme
        }
    }

    suspend fun autoOpenFileWhenParsingIsSuccessful(autoOpen: Boolean) {
        dataStores.edit { settings ->
            settings[AUTO_OPEN_FILE_WHEN_PARSING_IS_SUCCESSFUL] = autoOpen
        }
    }

}