/*
 * Copyright (C) 2010 Thialfihar <thi@thialfihar.org>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.sufficientlysecure.keychain.ui.widget;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.Vector;

import org.spongycastle.openpgp.PGPPublicKey;
import org.spongycastle.openpgp.PGPSecretKey;
import org.sufficientlysecure.keychain.Id;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.pgp.PgpKeyHelper;
import org.sufficientlysecure.keychain.util.Choice;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.format.DateUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.beardedhen.androidbootstrap.BootstrapButton;

public class KeyEditor extends LinearLayout implements Editor, OnClickListener {
    private PGPSecretKey mKey;

    private EditorListener mEditorListener = null;

    private boolean mIsMasterKey;
    BootstrapButton mDeleteButton;
    TextView mAlgorithm;
    TextView mKeyId;
    Spinner mUsage;
    TextView mCreationDate;
    BootstrapButton mExpiryDateButton;
    GregorianCalendar mCreatedDate;
    GregorianCalendar mExpiryDate;

    private int mDatePickerResultCount = 0;
    private DatePickerDialog.OnDateSetListener mExpiryDateSetListener = new DatePickerDialog.OnDateSetListener() {
        public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
            // Note: Ignore results after the first one - android sends multiples.
            if (mDatePickerResultCount++ == 0) {
                GregorianCalendar date = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
                date.set(year, monthOfYear, dayOfMonth);
                setExpiryDate(date);
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
        mKeyId = (TextView) findViewById(R.id.keyId);
        mCreationDate = (TextView) findViewById(R.id.creation);
        mExpiryDateButton = (BootstrapButton) findViewById(R.id.expiry);
        mUsage = (Spinner) findViewById(R.id.usage);
        Choice choices[] = {
                new Choice(Id.choice.usage.sign_only, getResources().getString(
                        R.string.choice_sign_only)),
                new Choice(Id.choice.usage.encrypt_only, getResources().getString(
                        R.string.choice_encrypt_only)),
                new Choice(Id.choice.usage.sign_and_encrypt, getResources().getString(
                        R.string.choice_sign_and_encrypt)), };
        ArrayAdapter<Choice> adapter = new ArrayAdapter<Choice>(getContext(),
                android.R.layout.simple_spinner_item, choices);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mUsage.setAdapter(adapter);

        mDeleteButton = (BootstrapButton) findViewById(R.id.delete);
        mDeleteButton.setOnClickListener(this);

        setExpiryDate(null);

        mExpiryDateButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                GregorianCalendar date = mExpiryDate;
                if (date == null) {
                    date = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
                }
                /*
                 * Using custom DatePickerDialog which overrides the setTitle because 
                 * the DatePickerDialog title is buggy (unix warparound bug).
                 * See: https://code.google.com/p/android/issues/detail?id=49066
                 */
                DatePickerDialog dialog = new ExpiryDatePickerDialog(getContext(),
                        mExpiryDateSetListener, date.get(Calendar.YEAR), date.get(Calendar.MONTH),
                        date.get(Calendar.DAY_OF_MONTH));
                mDatePickerResultCount = 0;
                dialog.setCancelable(true);
                dialog.setButton(Dialog.BUTTON_NEGATIVE,
                        getContext().getString(R.string.btn_no_date),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // Note: Ignore results after the first one - android sends multiples.
                                if (mDatePickerResultCount++ == 0) {
                                    setExpiryDate(null);
                                }
                            }
                        });

                // setCalendarViewShown() is supported from API 11 onwards.
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB)
                    // Hide calendarView in tablets because of the unix warparound bug.
                    dialog.getDatePicker().setCalendarViewShown(false);

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
                    if ( dialog != null && mCreatedDate != null ) {
                        dialog.getDatePicker().setMinDate(mCreatedDate.getTime().getTime()+ DateUtils.DAY_IN_MILLIS);
                    } else {
                        //When created date isn't available
                        dialog.getDatePicker().setMinDate(date.getTime().getTime()+ DateUtils.DAY_IN_MILLIS);
                    }
                }

                dialog.show();
            }
        });

        super.onFinishInflate();
    }

    public void setCanEdit(boolean bCanEdit) {
        if (!bCanEdit) {
            mDeleteButton.setVisibility(View.INVISIBLE);
            mUsage.setEnabled(false);
            mExpiryDateButton.setEnabled(false);
        }
    }

    public void setValue(PGPSecretKey key, boolean isMasterKey, int usage) {
        mKey = key;

        mIsMasterKey = isMasterKey;
        if (mIsMasterKey) {
            mDeleteButton.setVisibility(View.INVISIBLE);
        }

        mAlgorithm.setText(PgpKeyHelper.getAlgorithmInfo(key));
        String keyIdStr = PgpKeyHelper.convertKeyIdToHex(key.getKeyID());
        mKeyId.setText(keyIdStr);

        Vector<Choice> choices = new Vector<Choice>();
        boolean isElGamalKey = (key.getPublicKey().getAlgorithm() == PGPPublicKey.ELGAMAL_ENCRYPT);
        boolean isDSAKey = (key.getPublicKey().getAlgorithm() == PGPPublicKey.DSA);
        if (!isElGamalKey) {
            choices.add(new Choice(Id.choice.usage.sign_only, getResources().getString(
                    R.string.choice_sign_only)));
        }
        if (!mIsMasterKey && !isDSAKey) {
            choices.add(new Choice(Id.choice.usage.encrypt_only, getResources().getString(
                    R.string.choice_encrypt_only)));
        }
        if (!isElGamalKey && !isDSAKey) {
            choices.add(new Choice(Id.choice.usage.sign_and_encrypt, getResources().getString(
                    R.string.choice_sign_and_encrypt)));
        }

        ArrayAdapter<Choice> adapter = new ArrayAdapter<Choice>(getContext(),
                android.R.layout.simple_spinner_item, choices);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mUsage.setAdapter(adapter);

        // Set value in choice dropdown to key
        int selectId = 0;
        if (PgpKeyHelper.isEncryptionKey(key)) {
            if (PgpKeyHelper.isSigningKey(key)) {
                selectId = Id.choice.usage.sign_and_encrypt;
            } else {
                selectId = Id.choice.usage.encrypt_only;
            }
        } else {
            // set usage if it is predefined
            if (usage != -1) {
                selectId = usage;
            } else {
                selectId = Id.choice.usage.sign_only;
            }

        }

        for (int i = 0; i < choices.size(); ++i) {
            if (choices.get(i).getId() == selectId) {
                mUsage.setSelection(i);
                break;
            }
        }

        GregorianCalendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        cal.setTime(PgpKeyHelper.getCreationDate(key));
        setCreatedDate(cal);
        cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        Date expiryDate = PgpKeyHelper.getExpiryDate(key);
        if (expiryDate == null) {
            setExpiryDate(null);
        } else {
            cal.setTime(PgpKeyHelper.getExpiryDate(key));
            setExpiryDate(cal);
        }

    }

    public PGPSecretKey getValue() {
        return mKey;
    }

    public void onClick(View v) {
        final ViewGroup parent = (ViewGroup) getParent();
        if (v == mDeleteButton) {
            parent.removeView(this);
            if (mEditorListener != null) {
                mEditorListener.onDeleted(this);
            }
        }
    }

    public void setEditorListener(EditorListener listener) {
        mEditorListener = listener;
    }

    private void setCreatedDate(GregorianCalendar date) {
        mCreatedDate = date;
        if (date == null) {
            mCreationDate.setText(getContext().getString(R.string.none));
        } else {
            mCreationDate.setText(DateFormat.getDateInstance().format(date.getTime()));
        }
    }

    private void setExpiryDate(GregorianCalendar date) {
        mExpiryDate = date;
        if (date == null) {
            mExpiryDateButton.setText(getContext().getString(R.string.none));
        } else {
            mExpiryDateButton.setText(DateFormat.getDateInstance().format(date.getTime()));
        }
    }

    public GregorianCalendar getExpiryDate() {
        return mExpiryDate;
    }

    public int getUsage() {
        return ((Choice) mUsage.getSelectedItem()).getId();
    }

}

class ExpiryDatePickerDialog extends DatePickerDialog {

    public ExpiryDatePickerDialog(Context context, OnDateSetListener callBack, int year, int monthOfYear, int dayOfMonth) {
        super(context, callBack, year, monthOfYear, dayOfMonth);
    }
    //Set permanent title.
    public void setTitle(CharSequence title) {
        super.setTitle(getContext().getString(R.string.expiry_date_dialog_title));
    }
}
