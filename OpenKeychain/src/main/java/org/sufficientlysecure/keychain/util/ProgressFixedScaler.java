package org.sufficientlysecure.keychain.util;

import org.sufficientlysecure.keychain.pgp.Progressable;

/** This is a simple variant of ProgressScaler which shows a fixed progress message, ignoring
 * the provided ones.
 */
public class ProgressFixedScaler extends ProgressScaler {

    final int mResId;

    public ProgressFixedScaler(Progressable wrapped, int from, int to, int max, int resId) {
        super(wrapped, from, to, max);
        mResId = resId;
    }

    public void setProgress(int resourceId, int progress, int max) {
        if (mWrapped != null) {
            mWrapped.setProgress(mResId, mFrom + progress * (mTo - mFrom) / max, mMax);
        }
    }

    public void setProgress(String message, int progress, int max) {
        if (mWrapped != null) {
            mWrapped.setProgress(mResId, mFrom + progress * (mTo - mFrom) / max, mMax);
        }
    }

}
