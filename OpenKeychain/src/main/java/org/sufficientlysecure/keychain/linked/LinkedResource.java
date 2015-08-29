package org.sufficientlysecure.keychain.linked;

import java.net.URI;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.DrawableRes;
import android.support.annotation.StringRes;

public abstract class LinkedResource {

    public abstract URI toUri();

    public abstract @DrawableRes int getDisplayIcon();
    public abstract @StringRes int getVerifiedText(boolean isSecret);
    public abstract String getDisplayTitle(Context context);
    public abstract String getDisplayComment(Context context);
    public boolean isViewable() {
        return false;
    }
    public Intent getViewIntent() {
        return null;
    }

}
