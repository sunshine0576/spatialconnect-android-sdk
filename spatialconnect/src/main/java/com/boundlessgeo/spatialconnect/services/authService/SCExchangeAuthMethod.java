/**
 * Copyright 2017 Boundless, http://boundlessgeo.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License
 */
package com.boundlessgeo.spatialconnect.services.authService;

import android.content.Context;
import android.util.Base64;
import android.util.Log;

import com.boundlessgeo.spatialconnect.SpatialConnect;
import com.boundlessgeo.spatialconnect.scutilities.HttpHandler;
import com.boundlessgeo.spatialconnect.scutilities.SCCache;
import com.github.rtoshiro.secure.SecureSharedPreferences;

import org.joda.time.DateTime;
import org.joda.time.Seconds;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

import okhttp3.Response;

public class SCExchangeAuthMethod implements ISCAuth {

    private static String LOG_TAG = SCExchangeAuthMethod.class.getSimpleName();
    private static final String USERNAME = "username";
    private static final String PWD = "pwd";
    private static final String TOKEN = "token";
    private static final String REFRESH_TOKEN = "r_token";
    private static final String TOKEN_EXPIRATION = "t_expire";
    private static final String TOKEN_TIMESTAMP = "t_timestamp";

    private SecureSharedPreferences settings;
    private String serverUrl;
    private Context context;
    private String clientId;

    public SCExchangeAuthMethod(Context context, String serverUrl, String clientId) {
        this.context = context;
        this.settings = new SecureSharedPreferences(context);
        this.serverUrl = serverUrl;
        this.clientId = clientId;
    }

    @Override
    public boolean authFromCache() {
        Log.d(LOG_TAG, "authenticating from cache");
        String u = username();
        String p = getPassword();
        if (u != null && p != null) {
            SCCache cache = SpatialConnect.getInstance().getCache();
            int expire = cache.getIntValue(TOKEN_EXPIRATION);
            DateTime tokenTimestamp = new DateTime(cache.getStringValue(TOKEN_TIMESTAMP));
            DateTime tokenExpiration = tokenTimestamp.plusSeconds(expire);
            DateTime currentTimestamp = new DateTime();
            Seconds secondsBeforeExpiration = Seconds.secondsBetween(currentTimestamp, tokenExpiration);

            // current timestamp is later than token expiration or less than an 1 hour
            // before the token expires refresh token.
            if (currentTimestamp.isAfter(tokenExpiration) || secondsBeforeExpiration.getSeconds() < 3600) {
                return refreshToken();
            } else {
                if (settings.getString(TOKEN, null) != null) {
                    return true;
                } else {
                    return authenticate(u, p);
                }
            }
        } else {
            return false;
        }
    }

    @Override
    public boolean authenticate(String username, String pwd) {
        boolean authed = false;
        try {
            final String theUrl = String.format(Locale.US, "%s/o/token/", serverUrl);
            final String oauthCreds = String.format(Locale.US, "%s:", clientId);
            final String base64Encoded = Base64.encodeToString(oauthCreds.getBytes("UTF-8"), Base64.NO_WRAP);
            final String authHeader = String.format(Locale.US, "Basic %s", base64Encoded);
            Response response = HttpHandler.getInstance()
                    .postBlocking(theUrl,
                            String.format("grant_type=password&username=%s&password=%s",
                                    username, pwd), authHeader, HttpHandler.FORM);

            if (response.isSuccessful()) {
                JSONObject responseJson = new JSONObject(response.body().string());
                saveCredentials(username, pwd);
                saveAccessToken(responseJson.getString("access_token"));

                SCCache cache = SpatialConnect.getInstance().getCache();
                cache.setValue(responseJson.getString("refresh_token"), REFRESH_TOKEN);
                cache.setValue(responseJson.getInt("expires_in"), TOKEN_EXPIRATION);
                cache.setValue(new DateTime().toString(), TOKEN_TIMESTAMP);

                authed = true;
            } else {
                logout();
            }
        } catch (JSONException e) {
            Log.e(LOG_TAG,"JSON error trying to auth with exchange: " + e.getMessage());
        } catch (Exception e) {
            Log.e(LOG_TAG,"Error trying to auth with exchange: " + e.getMessage());
        }
        return authed;
    }

    @Override
    public void logout() {
        removeCredentials();
    }

    @Override
    public String xAccessToken() {
        return settings.getString(TOKEN, null);
    }

    @Override
    public String username() {
        return settings.getString(USERNAME, null);
    }

    public String getPassword() {
        return settings.getString(PWD, null);
    }

    private boolean refreshToken() {
        boolean authed = false;
        try {
            SCCache cache = SpatialConnect.getInstance().getCache();
            final String theUrl = String.format(Locale.US, "%s/o/token/", serverUrl);
            final String oauthCreds = String.format(Locale.US, "%s:", clientId);
            final String base64Encoded = Base64.encodeToString(oauthCreds.getBytes("UTF-8"), Base64.NO_WRAP);
            final String authHeader = String.format(Locale.US, "Basic %s", base64Encoded);
            Response response = HttpHandler.getInstance()
                    .postBlocking(theUrl,
                            String.format("grant_type=refresh_token&refresh_token=%s",
                                    cache.getStringValue(REFRESH_TOKEN)), authHeader, HttpHandler.FORM);

            JSONObject responseJson = new JSONObject(response.body().string());
            if (response.isSuccessful()) {
                saveAccessToken(responseJson.getString("access_token"));
                cache.setValue(responseJson.getString("refresh_token"), REFRESH_TOKEN);
                cache.setValue(responseJson.getInt("expires_in"), TOKEN_EXPIRATION);
                cache.setValue(new DateTime(), TOKEN_TIMESTAMP);
                authed = true;
            } else {
                if (responseJson.getString("error") != null) {
                    Log.e(LOG_TAG, "Error attempting to refresh token");
                }
                logout();
            }
        } catch (JSONException e) {
            Log.e(LOG_TAG,"JSON error trying to refresh token with exchange: " + e.getMessage());
        } catch (Exception e) {
            Log.e(LOG_TAG,"Error trying to refresh token with exchange: " + e.getMessage());
        }
        return authed;
    }

    private void saveCredentials(String username, String password) {
        SecureSharedPreferences.Editor editor = settings.edit();
        editor.putString(USERNAME, username);
        editor.putString(PWD, password);
        editor.commit();
    }

    private void saveAccessToken(String accessToken) {
        SecureSharedPreferences.Editor editor = settings.edit();
        editor.putString(TOKEN, accessToken);
        editor.commit();
    }

    private void removeCredentials() {
        SecureSharedPreferences.Editor editor = settings.edit();
        editor.remove(USERNAME);
        editor.remove(PWD);
        editor.remove(TOKEN);
        editor.commit();
    }
}