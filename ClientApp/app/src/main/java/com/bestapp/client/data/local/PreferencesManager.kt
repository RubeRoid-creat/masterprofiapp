package com.bestapp.client.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "bestapp_client_prefs")

class PreferencesManager(
    private val context: Context
) {
    private val dataStore = context.dataStore

    companion object {
        private val TOKEN_KEY = stringPreferencesKey("auth_token")
        private val USER_ID_KEY = longPreferencesKey("user_id")
        private val USER_NAME_KEY = stringPreferencesKey("user_name")
        private val USER_EMAIL_KEY = stringPreferencesKey("user_email")
        private val USER_PHONE_KEY = stringPreferencesKey("user_phone")
    }

    val authToken: Flow<String?> = dataStore.data.map { prefs ->
        prefs[TOKEN_KEY]
    }

    val userId: Flow<Long?> = dataStore.data.map { prefs ->
        prefs[USER_ID_KEY]
    }

    val userName: Flow<String?> = dataStore.data.map { prefs ->
        prefs[USER_NAME_KEY]
    }

    val userEmail: Flow<String?> = dataStore.data.map { prefs ->
        prefs[USER_EMAIL_KEY]
    }

    val userPhone: Flow<String?> = dataStore.data.map { prefs ->
        prefs[USER_PHONE_KEY]
    }

    suspend fun saveAuthData(token: String, userId: Long, name: String, email: String, phone: String) {
        dataStore.edit { prefs ->
            prefs[TOKEN_KEY] = token
            prefs[USER_ID_KEY] = userId
            prefs[USER_NAME_KEY] = name
            prefs[USER_EMAIL_KEY] = email
            prefs[USER_PHONE_KEY] = phone
        }
    }

    suspend fun clearAuthData() {
        dataStore.edit { prefs ->
            prefs.remove(TOKEN_KEY)
            prefs.remove(USER_ID_KEY)
            prefs.remove(USER_NAME_KEY)
            prefs.remove(USER_EMAIL_KEY)
            prefs.remove(USER_PHONE_KEY)
        }
    }

    suspend fun isLoggedIn(): Boolean {
        val token = dataStore.data.map { prefs ->
            prefs[TOKEN_KEY]
        }.first()
        return !token.isNullOrEmpty()
    }
}


