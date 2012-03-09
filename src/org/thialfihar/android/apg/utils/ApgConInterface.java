package org.thialfihar.android.apg.utils;

public interface ApgConInterface {
    public static interface OnCallFinishListener {
        public abstract void onCallFinish(android.os.Bundle result);
    }
}
