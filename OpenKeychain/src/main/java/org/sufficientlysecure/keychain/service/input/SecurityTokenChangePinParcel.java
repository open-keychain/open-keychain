package org.sufficientlysecure.keychain.service.input;


import android.os.Parcelable;

import com.google.auto.value.AutoValue;


@AutoValue
public abstract class SecurityTokenChangePinParcel implements Parcelable {
    public abstract String getAdminPin();
    public abstract String getNewPin();

    public static SecurityTokenChangePinParcel createSecurityTokenUnlock(String adminPin, String newPin) {
        return new AutoValue_SecurityTokenChangePinParcel(adminPin, newPin);
    }

}
