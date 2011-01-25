package org.thialfihar.android.apg.utils;

import android.os.Bundle;

public interface ApgConInterface {
    public static interface OnCallFinishListener {
        public abstract void onCallFinish(Bundle result);
    }
}
