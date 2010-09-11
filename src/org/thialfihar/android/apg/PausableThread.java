package org.thialfihar.android.apg;

public class PausableThread extends Thread {
    private boolean mPaused = false;

    public PausableThread(Runnable runnable) {
        super(runnable);
    }

    public void pause() {
        synchronized (this) {
            mPaused = true;
            while (mPaused) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    // ignore
                }
            }
        }
    }

    public void unpause() {
        synchronized (this) {
           mPaused = false;
           notify();
        }
    }

    public boolean isPaused() {
        synchronized (this) {
            return mPaused;
        }
    }
}
