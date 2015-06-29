package org.sufficientlysecure.keychain.ui.keyunlock.dialogs;

import android.support.v4.app.DialogFragment;


/**
 * Base unlock fragment shared amongst unlock methods.
 */
public abstract class UnlockDialog extends DialogFragment {
    public static final String EXTRA_PARAM_OPERATION_TYPE = "EXTRA_PARAM_OPERATION_TYPE";
}
