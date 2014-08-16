/*
 * Copyright (C) 2014 Dominik Schürmann <dominik@dominikschuermann.de>
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

package org.sufficientlysecure.keychain.ui.dialog;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.DialogFragment;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.DatePicker;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.util.Log;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class EditSubkeyExpiryDialogFragment extends DialogFragment {
    private static final String ARG_MESSENGER = "messenger";
    private static final String ARG_CREATION_DATE = "creation_date";
    private static final String ARG_EXPIRY_DATE = "expiry_date";

    public static final int MESSAGE_NEW_EXPIRY_DATE = 1;
    public static final int MESSAGE_CANCEL = 2;
    public static final String MESSAGE_DATA_EXPIRY_DATE = "expiry_date";

    private Messenger mMessenger;

    private DatePicker mDatePicker;

    /**
     * Creates new instance of this dialog fragment
     */
    public static EditSubkeyExpiryDialogFragment newInstance(Messenger messenger,
                                                             Long creationDate, Long expiryDate) {
        EditSubkeyExpiryDialogFragment frag = new EditSubkeyExpiryDialogFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_MESSENGER, messenger);
        args.putSerializable(ARG_CREATION_DATE, creationDate);
        args.putSerializable(ARG_EXPIRY_DATE, expiryDate);

        frag.setArguments(args);

        return frag;
    }

    /**
     * Creates dialog
     */
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Activity activity = getActivity();
        mMessenger = getArguments().getParcelable(ARG_MESSENGER);
        long creationDate = getArguments().getLong(ARG_CREATION_DATE);
        long expiryDate = getArguments().getLong(ARG_EXPIRY_DATE);

        Calendar creationCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        creationCal.setTime(new Date(creationDate * 1000));
        final Calendar expiryCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        expiryCal.setTime(new Date(expiryDate * 1000));

        // date picker works with default time zone, we need to convert from UTC to default timezone
        creationCal.setTimeZone(TimeZone.getDefault());
        expiryCal.setTimeZone(TimeZone.getDefault());

        // Explicitly not using DatePickerDialog here!
        // DatePickerDialog is difficult to customize and has many problems (see old git versions)
        CustomAlertDialogBuilder alert = new CustomAlertDialogBuilder(activity);

        alert.setTitle(R.string.expiry_date_dialog_title);

        LayoutInflater inflater = activity.getLayoutInflater();
        View view = inflater.inflate(R.layout.edit_subkey_expiry_dialog, null);
        alert.setView(view);

        mDatePicker = (DatePicker) view.findViewById(R.id.edit_subkey_expiry_date_picker);

        // set default date
        if (expiryDate == 0L) {
            // if key has no expiry, set it to creation date +1 day

            Calendar creationCalPlusOne = (Calendar) creationCal.clone();
            creationCalPlusOne.add(Calendar.DAY_OF_MONTH, 1);
            mDatePicker.init(
                    creationCalPlusOne.get(Calendar.YEAR),
                    creationCalPlusOne.get(Calendar.MONTH),
                    creationCalPlusOne.get(Calendar.DAY_OF_MONTH),
                    null
            );
        } else {
            // set date picker to current expiry date +1 day

            Calendar expiryCalPlusOne = (Calendar) expiryCal.clone();
            expiryCalPlusOne.add(Calendar.DAY_OF_MONTH, 1);
            mDatePicker.init(
                    expiryCalPlusOne.get(Calendar.YEAR),
                    expiryCalPlusOne.get(Calendar.MONTH),
                    expiryCalPlusOne.get(Calendar.DAY_OF_MONTH),
                    null
            );
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
            // will crash with IllegalArgumentException if we set a min date
            // that is before creation date
            if (expiryDate == 0L || creationCal.before(expiryCal)) {
                mDatePicker.setMinDate(creationCal.getTime().getTime()
                        + DateUtils.DAY_IN_MILLIS);
            } else {
                // set min to expiry date
                mDatePicker.setMinDate(expiryCal.getTime().getTime()
                        + DateUtils.DAY_IN_MILLIS);
            }
        }

        alert.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dismiss();

                Calendar selectedCal = Calendar.getInstance(TimeZone.getDefault());
                //noinspection ResourceType
                selectedCal.set(mDatePicker.getYear(), mDatePicker.getMonth(), mDatePicker.getDayOfMonth());
                // date picker uses default time zone, we need to convert to UTC
                selectedCal.setTimeZone(TimeZone.getTimeZone("UTC"));

                long numDays = (selectedCal.getTimeInMillis() / 86400000)
                        - (expiryCal.getTimeInMillis() / 86400000);
                if (numDays > 0) {
                    Bundle data = new Bundle();
                    data.putSerializable(MESSAGE_DATA_EXPIRY_DATE, selectedCal.getTime().getTime() / 1000);
                    sendMessageToHandler(MESSAGE_NEW_EXPIRY_DATE, data);
                }
            }
        });

        alert.setNeutralButton(R.string.btn_no_date, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dismiss();

                Bundle data = new Bundle();
                data.putSerializable(MESSAGE_DATA_EXPIRY_DATE, 0L);
                sendMessageToHandler(MESSAGE_NEW_EXPIRY_DATE, data);
            }
        });

        alert.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dismiss();
            }
        });

        return alert.show();
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);

        dismiss();
        sendMessageToHandler(MESSAGE_CANCEL, null);
    }

    /**
     * Send message back to handler which is initialized in a activity
     *
     * @param what Message integer you want to send
     */
    private void sendMessageToHandler(Integer what, Bundle data) {
        Message msg = Message.obtain();
        msg.what = what;
        if (data != null) {
            msg.setData(data);
        }

        try {
            mMessenger.send(msg);
        } catch (RemoteException e) {
            Log.w(Constants.TAG, "Exception sending message, Is handler present?", e);
        } catch (NullPointerException e) {
            Log.w(Constants.TAG, "Messenger is null!", e);
        }
    }
}
