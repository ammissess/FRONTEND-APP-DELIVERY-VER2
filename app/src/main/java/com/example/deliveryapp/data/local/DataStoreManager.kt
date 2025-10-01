package com.example.deliveryapp.data.local

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.deliveryapp.utils.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private const val TAG = "DataStoreManager"

// Đảm bảo đây là top-level property để DataStore được khởi tạo đúng cách
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = Constants.PREFS_NAME)

class DataStoreManager(private val context: Context) {
    companion object {
        val ACCESS_TOKEN_KEY = stringPreferencesKey(Constants.KEY_ACCESS_TOKEN)
        val REFRESH_TOKEN_KEY = stringPreferencesKey(Constants.KEY_REFRESH_TOKEN)
    }

    val accessToken: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[ACCESS_TOKEN_KEY]
    }

    val refreshToken: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[REFRESH_TOKEN_KEY]
    }

    suspend fun saveTokens(access: String, refresh: String) {
        Log.d(TAG, "Saving tokens - Access: ${access.take(10)}..., Refresh: ${refresh.take(10)}...")
        context.dataStore.edit { prefs ->
            prefs[ACCESS_TOKEN_KEY] = access
            prefs[REFRESH_TOKEN_KEY] = refresh
        }

        // Verify tokens were saved
        val savedAccess = context.dataStore.data.first()[ACCESS_TOKEN_KEY]
        val savedRefresh = context.dataStore.data.first()[REFRESH_TOKEN_KEY]
        Log.d(TAG, "Saved tokens - Access: ${savedAccess?.take(10)}..., Refresh: ${savedRefresh?.take(10)}...")
    }

    suspend fun clearTokens() {
        Log.d(TAG, "Clearing all tokens")
        context.dataStore.edit { prefs ->
            prefs.remove(ACCESS_TOKEN_KEY)
            prefs.remove(REFRESH_TOKEN_KEY)
        }
    }
}