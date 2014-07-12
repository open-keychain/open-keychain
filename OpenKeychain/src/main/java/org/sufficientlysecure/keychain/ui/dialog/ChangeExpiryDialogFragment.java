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

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.DialogFragment;
import android.text.format.DateUtils;
import android.widget.DatePicker;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.util.Log;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

public class ChangeExpiryDialogFragment extends DialogFragment {
    private static final String ARG_MESSENGER = "messenger";
    private static final String ARG_CREATION_DATE = "creation_date";
    private static final String ARG_EXPIRY_DATE = "expiry_date";

    public static final int MESSAGE_NEW_EXPIRY_DATE = 1;
    public static final String MESSAGE_DATA_EXPIRY_DATE = "expiry_date";

    private Messenger mMessenger;
    private Calendar mCreationCal;
    private Calendar mExpiryCal;

    private int mDatePickerResultCount = 0;
    private DatePickerDialog.OnDateSetListener mExpiryDateSetListener =
            new DatePickerDialog.OnDateSetListener() {
                public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                    // Note: Ignore results after the first one - android sends multiples.
                    if (mDatePickerResultCount++ == 0) {
                        Calendar selectedCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                        selectedCal.set(year, monthOfYear, dayOfMonth);
                        if (mExpiryCal != null) {
                            long numDays = (selectedCal.getTimeInMillis() / 86400000)
                                    - (mExpiryCal.getTimeInMillis() / 86400000);
                            if (numDays > 0) {
                                Bundle data = new Bundle();
                                data.putSerializable(MESSAGE_DATA_EXPIRY_DATE, selectedCal.getTime());
                                sendMessageToHandler(MESSAGE_NEW_EXPIRY_DATE, data);
                            }
                        } else {
                            Bundle data = new Bundle();
                            data.putSerializable(MESSAGE_DATA_EXPIRY_DATE, selectedCal.getTime());
                            sendMessageToHandler(MESSAGE_NEW_EXPIRY_DATE, data);
                        }
                    }
                }
            };

    public class ExpiryDatePickerDialog extends DatePickerDialog {

        public ExpiryDatePickerDialog(Context context, OnDateSetListener callBack,
                                      int year, int monthOfYear, int dayOfMonth) {
            super(context, callBack, year, monthOfYear, dayOfMonth);
        }

        // set permanent title
        public void setTitle(CharSequence title) {
            super.setTitle(getContext().getString(R.string.expiry_date_dialog_title));
        }
    }

    /**
     * Creates new instance of this dialog fragment
     */
    public static ChangeExpiryDialogFragment newInstance(Messenger messenger,
                                                         Date creationDate, Date expiryDate) {
        ChangeExpiryDialogFragment frag = new ChangeExpiryDialogFragment();
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
        mMessenger = getArguments().getParcelable(ARG_MESSENGER);
        Date creationDate = (Date) getArguments().getSerializable(ARG_CREATION_DATE);
        Date expiryDate = (Date) getArguments().getSerializable(ARG_EXPIRY_DATE);

        mCreationCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        mCreationCal.setTime(creationDate);
        mExpiryCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        mExpiryCal.setTime(expiryDate);

        /*
         * Using custom DatePickerDialog which overrides the setTitle because
         * the DatePickerDialog title is buggy (unix warparound bug).
         * See: https://code.google.com/p/android/issues/detail?id=49066
         */
        DatePickerDialog dialog = new ExpiryDatePickerDialog(getActivity(),
                mExpiryDateSetListener, mExpiryCal.get(Calendar.YEAR), mExpiryCal.get(Calendar.MONTH),
                mExpiryCal.get(Calendar.DAY_OF_MONTH));
        mDatePickerResultCount = 0;
        dialog.setCancelable(true);
        dialog.setButton(Dialog.BUTTON_NEGATIVE,
                getActivity().getString(R.string.btn_no_date),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // Note: Ignore results after the first one - android sends multiples.
                        if (mDatePickerResultCount++ == 0) {
                            // none expiry dates corresponds to a null message
                            Bundle data = new Bundle();
                            data.putSerializable(MESSAGE_DATA_EXPIRY_DATE, null);
                            sendMessageToHandler(MESSAGE_NEW_EXPIRY_DATE, data);
                        }
                    }
                }
        );

        // setCalendarViewShown() is supported from API 11 onwards.
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
            // Hide calendarView in tablets because of the unix warparound bug.
            dialog.getDatePicker().setCalendarViewShown(false);
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
            // will crash with IllegalArgumentException if we set a min date
            // that is not before expiry
            if (mCreationCal != null && mCreationCal.before(mExpiryCal)) {
                dialog.getDatePicker().setMinDate(mCreationCal.getTime().getTime()
                        + DateUtils.DAY_IN_MILLIS);
            } else {
                // When created date isn't available
                dialog.getDatePicker().setMinDate(mExpiryCal.getTime().getTime()
                        + DateUtils.DAY_IN_MILLIS);
            }
        }

        return dialog;
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
