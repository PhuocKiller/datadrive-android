/*
 * DataDrive - Android Client
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.onboarding

import android.util.Base64
import com.owncloud.android.MainApp
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Signs in to the fixed DataDrive server with a user name and password, without the browser login flow.
 * The password is exchanged for an app password, which is what the account stores.
 */
object DataDriveLogin {

    sealed class Result {
        data class Success(val loginName: String, val appPassword: String) : Result()
        object WrongCredentials : Result()
        object ServerUnreachable : Result()
    }

    private const val APP_PASSWORD_PATH = "/ocs/v2.php/core/getapppassword?format=json"
    private const val TIMEOUT_MS = 20_000
    private const val HTTP_OK = 200
    private const val HTTP_UNAUTHORIZED = 401
    private const val HTTP_FORBIDDEN = 403

    /** Blocking; call off the main thread. */
    @Suppress("TooGenericExceptionCaught")
    fun signIn(serverUrl: String, user: String, password: String): Result {
        val connection = try {
            URL(serverUrl.trimEnd('/') + APP_PASSWORD_PATH).openConnection() as HttpURLConnection
        } catch (e: IOException) {
            return Result.ServerUnreachable
        }

        return try {
            val basic = Base64.encodeToString("$user:$password".toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("Authorization", "Basic $basic")
            connection.setRequestProperty("OCS-APIRequest", "true")
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("User-Agent", MainApp.getUserAgent())

            when (connection.responseCode) {
                HTTP_OK -> {
                    val body = connection.inputStream.bufferedReader().use { it.readText() }
                    val appPassword = JSONObject(body).getJSONObject("ocs").getJSONObject("data")
                        .getString("apppassword")
                    Result.Success(user, appPassword)
                }
                // the server refuses to derive an app password from an app password: it is valid as is
                HTTP_FORBIDDEN -> Result.Success(user, password)
                HTTP_UNAUTHORIZED -> Result.WrongCredentials
                else -> Result.ServerUnreachable
            }
        } catch (e: Exception) {
            Result.ServerUnreachable
        } finally {
            connection.disconnect()
        }
    }
}
