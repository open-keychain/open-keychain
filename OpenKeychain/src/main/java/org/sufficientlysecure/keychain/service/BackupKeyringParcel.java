/*
 * Copyright (C) 2017 Schürmann & Breitmoser GbR
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.sufficientlysecure.keychain.service;


import java.util.List;

import android.net.Uri;
import android.os.Parcelable;
import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;


@AutoValue
public abstract class BackupKeyringParcel implements Parcelable {
    @Nullable
    @SuppressWarnings("mutable")
    public abstract long[] getMasterKeyIds();
    public abstract boolean getExportSecret();
    public abstract boolean getExportPublic();
    public abstract boolean getIsEncrypted();
    public abstract boolean getEnableAsciiArmorOutput();
    @Nullable
    public abstract Uri getOutputUri();
    @Nullable
    public abstract List<String> getExtraHeaders();

    public static BackupKeyringParcel create(long[] masterKeyIds, boolean exportSecret,
            boolean isEncrypted, boolean enableAsciiArmorOutput, Uri outputUri) {
        return new AutoValue_BackupKeyringParcel(
                masterKeyIds, exportSecret, true, isEncrypted, enableAsciiArmorOutput, outputUri, null);
    }

    public static BackupKeyringParcel create(long[] masterKeyIds, boolean exportSecret,
            boolean isEncrypted, boolean enableAsciiArmorOutput, Uri outputUri, List<String> extraHeaders) {
        return new AutoValue_BackupKeyringParcel(
                masterKeyIds, exportSecret, true, isEncrypted, enableAsciiArmorOutput, outputUri, extraHeaders);
    }

    public static BackupKeyringParcel createExportAutocryptSetupMessage(long[] masterKeyIds,
            List<String> extraHeaders) {
        return new AutoValue_BackupKeyringParcel(
                masterKeyIds, true, false, true, true, null, extraHeaders);
    }
}