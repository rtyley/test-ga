/*
 * Copyright 2012 GitHub Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.mobile.gauges.ui.airtraffic;

import static org.json.JSONObject.NULL;
import android.util.Log;

import com.emorym.android_pusher.PusherCallback;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Callback that delivers pushed air traffic events to an implemented {@link #onHit(Hit)} callback
 */
public abstract class AirTrafficPusherCallback extends PusherCallback {

    private static final String TAG = "ATPC";

    /**
     * Process hit
     *
     * @param hit
     */
    protected abstract void onHit(final Hit hit);

    /**
     * Get the value of the key as a {@link String}
     *
     * @param data
     * @param key
     * @return value, may be null
     * @throws JSONException
     */
    protected String getString(final JSONObject data, final String key) throws JSONException {
        final Object value = data.get(key);
        if (value != null && value != NULL)
            return value.toString();
        else
            return null;
    }

    @Override
    public void onEvent(final JSONObject eventData) {
        try {
            float longitude = (float) eventData.getDouble("longitude");
            float latitude = (float) eventData.getDouble("latitude");
            String id = getString(eventData, "site_id");
            String city = getString(eventData, "city");
            String region = getString(eventData, "region");
            String country = getString(eventData, "country");
            String title = getString(eventData, "title");
            onHit(new Hit(id, title, longitude, latitude, city, region, country));
        } catch (JSONException e) {
            Log.d(TAG, "JSON exception", e);
        }
    }
}
