/*
 * Copyright (C) 2014 Dominik Schürmann <dominik@dominikschuermann.de>
 * Copyright (C) 2010-2014 Thialfihar <thi@thialfihar.org>
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

package org.sufficientlysecure.keychain.ui.widget;

import android.annotation.TargetApi;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.format.DateUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Button;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.pgp.PgpKeyHelper;
import org.sufficientlysecure.keychain.pgp.UncachedSecretKey;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

public class KeyEditor extends LinearLayout implements Editor, OnClickListener {
    private UncachedSecretKey mKey;

    private EditorListener mEditorListener = null;

    private boolean mIsMasterKey;
    ImageButton mDeleteButton;
    TextView mAlgorithm;
    TextView mKeyId;
    TextView mCreationDate;
    Button mExpiryDateButton;
    Calendar mCreatedDate;
    Calendar mExpiryDate;
    Calendar mOriginalExpiryDate = null;
    CheckBox mChkCertify;
    CheckBox mChkSign;
    CheckBox mChkEncrypt;
    CheckBox mChkAuthenticate;
    int mUsage;
    int mOriginalUsage;
    boolean mIsNewKey;

    private CheckBox.OnCheckedChangeListener mCheckChanged = new CheckBox.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (mEditorListener != null) {
                mEditorListener.onEdited();
            }
        }
    };


    private int mDatePickerResultCount = 0;
    private DatePickerDialog.OnDateSetListener mExpiryDateSetListener =
            new DatePickerDialog.OnDateSetListener() {
                public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                    // Note: Ignore results after the first one - android sends multiples.
                    if (mDatePickerResultCount++ == 0) {
                        GregorianCalendar date = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
                        date.set(year, monthOfYear, dayOfMonth);
                        if (mOriginalExpiryDate != null) {
                            long numDays = (date.getTimeInMillis() / 86400000) -
                                    (mOriginalExpiryDate.getTimeInMillis() / 86400000);
                            if (numDays == 0) {
                                setExpiryDate(mOriginalExpiryDate);
                            } else {
                                setExpiryDate(date);
                            }
                        } else {
                            setExpiryDate(date);
                        }
                        if (mEditorListener != null) {
                            mEditorListener.onEdited();
                        }
                    }
                }
            };

    public KeyEditor(Context context) {
        super(context);
    }

    public KeyEditor(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        setDrawingCacheEnabled(true);
        setAlwaysDrawnWithCacheEnabled(true);

        mAlgorithm = (TextView) findViewById(R.id.algorithm);
        mKeyId = (TextView) findViewById(R.id.subkey_item_key_id);
        mCreationDate = (TextView) findViewById(R.id.creation);
        mExpiryDateButton = (Button) findViewById(R.id.expiry);

        mDeleteButton = (ImageButton) findViewById(R.id.delete);
        mDeleteButton.setOnClickListener(this);
        mChkCertify = (CheckBox) findViewById(R.id.chkCertify);
        mChkCertify.setOnCheckedChangeListener(mCheckChanged);
        mChkSign = (CheckBox) findViewById(R.id.chkSign);
        mChkSign.setOnCheckedChangeListener(mCheckChanged);
        mChkEncrypt = (CheckBox) findViewById(R.id.chkEncrypt);
        mChkEncrypt.setOnCheckedChangeListener(mCheckChanged);
        mChkAuthenticate = (CheckBox) findViewById(R.id.chkAuthenticate);
        mChkAuthenticate.setOnCheckedChangeListener(mCheckChanged);

        setExpiryDate(null);

        mExpiryDateButton.setOnClickListener(new OnClickListener() {
            @TargetApi(11)
            public void onClick(View v) {
                Calendar expiryDate = mExpiryDate;
                if (expiryDate == null) {
                    expiryDate = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                }
                /*
                 * Using custom DatePickerDialog which overrides the setTitle because
                 * the DatePickerDialog title is buggy (unix warparound bug).
                 * See: https://code.google.com/p/android/issues/detail?id=49066
                 */
                DatePickerDialog dialog = new ExpiryDatePickerDialog(getContext(),
                        mExpiryDateSetListener, expiryDate.get(Calendar.YEAR), expiryDate.get(Calendar.MONTH),
                        expiryDate.get(Calendar.DAY_OF_MONTH));
                mDatePickerResultCount = 0;
                dialog.setCancelable(true);
                dialog.setButton(Dialog.BUTTON_NEGATIVE,
                        getContext().getString(R.string.btn_no_date),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // Note: Ignore results after the first one - android sends multiples.
                                if (mDatePickerResultCount++ == 0) {
                                    setExpiryDate(null);
                                    if (mEditorListener != null) {
                                        mEditorListener.onEdited();
                                    }
                                }
                            }
                        });

                // setCalendarViewShown() is supported from API 11 onwards.
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
                    // Hide calendarView in tablets because of the unix warparound bug.
                    dialog.getDatePicker().setCalendarViewShown(false);
                }
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {

                    // will crash with IllegalArgumentException if we set a min date
                    // that is not before expiry
                    if (mCreatedDate != null && mCreatedDate.before(expiryDate)) {
                        dialog.getDatePicker()
                                .setMinDate(
                                        mCreatedDate.getTime().getTime() + DateUtils.DAY_IN_MILLIS);
                    } else {
                        // When created date isn't available
                        dialog.getDatePicker().setMinDate(expiryDate.getTime().getTime() + DateUtils.DAY_IN_MILLIS);
                    }
                }

                dialog.show();
            }
        });

        super.onFinishInflate();
    }

    public void setCanBeEdited(boolean canBeEdited) {
        if (!canBeEdited) {
            mDeleteButton.setVisibility(View.INVISIBLE);
            mExpiryDateButton.setEnabled(false);
            mChkSign.setEnabled(false); //certify is always disabled
            mChkEncrypt.setEnabled(false);
            mChkAuthenticate.setEnabled(false);
        }
    }

    public void setValue(UncachedSecretKey key, boolean isMasterKey, int usage, boolean isNewKey) {
        mKey = key;

        mIsMasterKey = isMasterKey;
        if (mIsMasterKey) {
            mDeleteButton.setVisibility(View.INVISIBLE);
        }

        mAlgorithm.setText(PgpKeyHelper.getAlgorithmInfo(getContext(), key.getAlgorithm()));
        String keyIdStr = PgpKeyHelper.convertKeyIdToHex(key.getKeyId());
        mKeyId.setText(keyIdStr);

        boolean isElGamalKey = (key.isElGamalEncrypt());
        boolean isDSAKey = (key.isDSA());
        if (isElGamalKey) {
            mChkSign.setVisibility(View.INVISIBLE);
            TableLayout table = (TableLayout) findViewById(R.id.table_keylayout);
            TableRow row = (TableRow) findViewById(R.id.row_sign);
            table.removeView(row);
        }
        if (isDSAKey) {
            mChkEncrypt.setVisibility(View.INVISIBLE);
            TableLayout table = (TableLayout) findViewById(R.id.table_keylayout);
            TableRow row = (TableRow) findViewById(R.id.row_encrypt);
            table.removeView(row);
        }
        if (!mIsMasterKey) {
            mChkCertify.setVisibility(View.INVISIBLE);
            TableLayout table = (TableLayout) findViewById(R.id.table_keylayout);
            TableRow row = (TableRow) findViewById(R.id.row_certify);
            table.removeView(row);
        } else {
            TextView mLabelUsage2 = (TextView) findViewById(R.id.label_usage2);
            mLabelUsage2.setVisibility(View.INVISIBLE);
        }

        mIsNewKey = isNewKey;
        if (isNewKey) {
            mUsage = usage;
            mChkCertify.setChecked(
                    (usage & UncachedSecretKey.CERTIFY_OTHER) == UncachedSecretKey.CERTIFY_OTHER);
            mChkSign.setChecked(
                    (usage & UncachedSecretKey.SIGN_DATA) == UncachedSecretKey.SIGN_DATA);
            mChkEncrypt.setChecked(
                    ((usage & UncachedSecretKey.ENCRYPT_COMMS) == UncachedSecretKey.ENCRYPT_COMMS) ||
                    ((usage & UncachedSecretKey.ENCRYPT_STORAGE) == UncachedSecretKey.ENCRYPT_STORAGE));
            mChkAuthenticate.setChecked(
                    (usage & UncachedSecretKey.AUTHENTICATION) == UncachedSecretKey.AUTHENTICATION);
        } else {
            mUsage = key.getKeyUsage();
            mOriginalUsage = mUsage;
            if (key.isMasterKey()) {
                mChkCertify.setChecked(key.canCertify());
            }
            mChkSign.setChecked(key.canSign());
            mChkEncrypt.setChecked(key.canEncrypt());
            mChkAuthenticate.setChecked(key.canAuthenticate());
        }

        {
            Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            cal.setTime(key.getCreationTime());
            setCreatedDate(cal);
        }

        Date expiryDate = key.getExpiryTime();
        if (expiryDate == null) {
            setExpiryDate(null);
        } else {
            Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            cal.setTime(expiryDate);
            setExpiryDate(cal);
            mOriginalExpiryDate = cal;
        }

    }

    public UncachedSecretKey getValue() {
        return mKey;
    }

    public void onClick(View v) {
        final ViewGroup parent = (ViewGroup) getParent();
        if (v == mDeleteButton) {
            parent.removeView(this);
            if (mEditorListener != null) {
                mEditorListener.onDeleted(this, mIsNewKey);
            }
        }
    }

    public void setEditorListener(EditorListener listener) {
        mEditorListener = listener;
    }

    private void setCreatedDate(Calendar date) {
        mCreatedDate = date;
        if (date == null) {
            mCreationDate.setText(getContext().getString(R.string.none));
        } else {
            mCreationDate.setText(DateFormat.getDateInstance().format(date.getTime()));
        }
    }

    private void setExpiryDate(Calendar date) {
        mExpiryDate = date;
        if (date == null) {
            mExpiryDateButton.setText(getContext().getString(R.string.none));
        } else {
            mExpiryDateButton.setText(DateFormat.getDateInstance().format(date.getTime()));
        }
    }

    public Calendar getExpiryDate() {
        return mExpiryDate;
    }

    public int getUsage() {
        mUsage = (mUsage & ~UncachedSecretKey.CERTIFY_OTHER) |
                (mChkCertify.isChecked() ? UncachedSecretKey.CERTIFY_OTHER : 0);
        mUsage = (mUsage & ~UncachedSecretKey.SIGN_DATA) |
                (mChkSign.isChecked() ? UncachedSecretKey.SIGN_DATA : 0);
        mUsage = (mUsage & ~UncachedSecretKey.ENCRYPT_COMMS) |
                (mChkEncrypt.isChecked() ? UncachedSecretKey.ENCRYPT_COMMS : 0);
        mUsage = (mUsage & ~UncachedSecretKey.ENCRYPT_STORAGE) |
                (mChkEncrypt.isChecked() ? UncachedSecretKey.ENCRYPT_STORAGE : 0);
        mUsage = (mUsage & ~UncachedSecretKey.AUTHENTICATION) |
                (mChkAuthenticate.isChecked() ? UncachedSecretKey.AUTHENTICATION : 0);

        return mUsage;
    }

    public boolean needsSaving() {
        if (mIsNewKey) {
            return true;
        }

        boolean retval = (getUsage() != mOriginalUsage);

        boolean dateChanged;
        boolean mOEDNull = (mOriginalExpiryDate == null);
        boolean mEDNull = (mExpiryDate == null);
        if (mOEDNull != mEDNull) {
            dateChanged = true;
        } else {
            if (mOEDNull) {
                //both null, no change
                dateChanged = false;
            } else {
                dateChanged = ((mExpiryDate.compareTo(mOriginalExpiryDate)) != 0);
            }
        }
        retval |= dateChanged;

        return retval;
    }

    public boolean getIsNewKey() {
        return mIsNewKey;
    }
}

class ExpiryDatePickerDialog extends DatePickerDialog {

    public ExpiryDatePickerDialog(Context context, OnDateSetListener callBack,
                                  int year, int monthOfYear, int dayOfMonth) {
        super(context, callBack, year, monthOfYear, dayOfMonth);
    }

    //Set permanent title.
    public void setTitle(CharSequence title) {
        super.setTitle(getContext().getString(R.string.expiry_date_dialog_title));
    }
}
