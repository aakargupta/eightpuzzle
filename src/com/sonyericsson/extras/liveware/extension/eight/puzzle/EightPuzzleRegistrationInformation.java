/*
Copyright (c) 2012 Sony Ericsson Mobile Communications AB
Copyright (c) 2012 Sony Mobile Communications AB.

All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

* Redistributions of source code must retain the above copyright notice, this
  list of conditions and the following disclaimer.

* Redistributions in binary form must reproduce the above copyright notice,
  this list of conditions and the following disclaimer in the documentation
  and/or other materials provided with the distribution.

* Neither the name of the Sony Mobile Communications AB nor the names
  of its contributors may be used to endorse or promote products derived from
  this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package com.sonyericsson.extras.liveware.extension.eight.puzzle;

import com.sonyericsson.extras.liveware.aef.registration.Registration;
import com.sonyericsson.extras.liveware.extension.util.ExtensionUtils;
import com.sonyericsson.extras.liveware.extension.util.registration.RegistrationInformation;

import android.content.ContentValues;
import android.content.Context;

/**
 * Provides information needed during extension registration
 */
public class EightPuzzleRegistrationInformation extends RegistrationInformation {

    final Context mContext;

    /**
     * Create control registration object
     *
     * @param context The context
     */
    protected EightPuzzleRegistrationInformation(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("context == null");
        }
        mContext = context;
    }

    @Override
    public int getRequiredControlApiVersion() {
        return 1;
    }

    @Override
    public int getRequiredSensorApiVersion() {
        return 0;
    }

    @Override
    public int getRequiredNotificationApiVersion() {
        return 0;
    }

    @Override
    public int getRequiredWidgetApiVersion() {
        return 0;
    }

    /**
     * Get the extension registration information.
     *
     * @return The registration configuration.
     */
    @Override
    public ContentValues getExtensionRegistrationConfiguration() {
        String iconHostApp = ExtensionUtils.getUriString(mContext, R.drawable.icon);
        String iconExtension = ExtensionUtils
                .getUriString(mContext, R.drawable.eight_game_app_icon);

        ContentValues values = new ContentValues();

        values.put(Registration.ExtensionColumns.CONFIGURATION_TEXT,
                mContext.getString(R.string.preference_activity_title));
        values.put(Registration.ExtensionColumns.NAME, mContext.getString(R.string.extension_name));
        values.put(Registration.ExtensionColumns.EXTENSION_KEY,
                EightPuzzleExtensionService.EXTENSION_KEY);
        values.put(Registration.ExtensionColumns.HOST_APP_ICON_URI, iconHostApp);
        values.put(Registration.ExtensionColumns.EXTENSION_ICON_URI, iconExtension);
        values.put(Registration.ExtensionColumns.NOTIFICATION_API_VERSION,
                getRequiredNotificationApiVersion());
        values.put(Registration.ExtensionColumns.PACKAGE_NAME, mContext.getPackageName());

        return values;
    }

    @Override
    public boolean isDisplaySizeSupported(int width, int height) {
        return ((width == EightPuzzleControlSmartWatch.getSupportedControlWidth(mContext) && height == EightPuzzleControlSmartWatch
                .getSupportedControlHeight(mContext)));
    }

}
