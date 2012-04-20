package org.thialfihar.android.apg.ui;

import org.thialfihar.android.apg.Constants;
import org.thialfihar.android.apg.Id;

import android.content.Context;
import android.os.Bundle;
import android.os.Message;

/**
 * Extends the standard ProgressDialog by new methods for setting progress
 *
 */
public class ProgressDialog extends android.app.ProgressDialog{
    
   Context mContext;

    public ProgressDialog(Context context) {
        super(context);
        mContext = context;
    }
    
    public void setProgress(int resourceId, int progress, int max) {
        setProgress(mContext.getString(resourceId), progress, max);
    }

    public void setProgress(int progress, int max) {
        this.setP
    }

    public void setProgress(String message, int progress, int max) {
        Message msg = new Message();
        Bundle data = new Bundle();
        data.putInt(Constants.extras.STATUS, Id.message.progress_update);
        data.putString(Constants.extras.MESSAGE, message);
        data.putInt(Constants.extras.PROGRESS, progress);
        data.putInt(Constants.extras.PROGRESS_MAX, max);
        msg.setData(data);
        mHandler.sendMessage(msg);
    }

}
