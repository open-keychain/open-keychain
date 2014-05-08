package org.sufficientlysecure.keychain.ui.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.view.View;
import android.widget.TextView;

import org.sufficientlysecure.keychain.R;

/** This class extends AlertDiaog.Builder, styling the header using emphasis color.
 * Note that this class is a huge hack, because dialog boxes aren't easily stylable.
 * Also, the dialog NEEDS to be called with show() directly, not create(), otherwise
 * the order of internal operations will lead to a crash!
 */
public class CustomAlertDialogBuilder extends AlertDialog.Builder {

    public CustomAlertDialogBuilder(Activity activity) {
        super(activity);
    }

    @Override
    public AlertDialog show() {
        AlertDialog dialog = super.show();

        int dividerId = dialog.getContext().getResources().getIdentifier("android:id/titleDivider", null, null);
        View divider = dialog.findViewById(dividerId);
        if (divider != null) {
            divider.setBackgroundColor(dialog.getContext().getResources().getColor(R.color.emphasis));
        }

        int textViewId = dialog.getContext().getResources().getIdentifier("android:id/alertTitle", null, null);
        TextView tv = (TextView) dialog.findViewById(textViewId);
        if (tv != null) {
            tv.setTextColor(dialog.getContext().getResources().getColor(R.color.emphasis));
        }

        return dialog;
    }

}
