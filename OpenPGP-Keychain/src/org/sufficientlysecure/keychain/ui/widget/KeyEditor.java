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

import org.spongycastle.openpgp.PGPPublicKey;
import org.spongycastle.openpgp.PGPSecretKey;
import org.sufficientlysecure.keychain.Id;
import org.sufficientlysecure.keychain.pgp.PgpKeyHelper;
import org.sufficientlysecure.keychain.util.Choice;
import org.sufficientlysecure.keychain.R;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Vector;

public class KeyEditor extends LinearLayout implements Editor, OnClickListener {
    private PGPSecretKey mKey;

    private EditorListener mEditorListener = null;

    private boolean mIsMasterKey;
    ImageButton mDeleteButton;
    TextView mAlgorithm;
    TextView mKeyId;
    Spinner mUsage;
    TextView mCreationDate;
    Button mExpiryDateButton;
    GregorianCalendar mExpiryDate;

    private int mDatePickerResultCount = 0;
    private DatePickerDialog.OnDateSetListener mExpiryDateSetListener = new DatePickerDialog.OnDateSetListener() {
        public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
            if (mDatePickerResultCount++ == 0) // Note: Ignore results after the first one - android
                                               // sends multiples.
            {
                GregorianCalendar date = new GregorianCalendar(year, monthOfYear, dayOfMonth);
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
        mExpiryDateButton = (Button) findViewById(R.id.expiry);
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

        mDeleteButton = (ImageButton) findViewById(R.id.delete);
        mDeleteButton.setOnClickListener(this);

        setExpiryDate(null);

        mExpiryDateButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                GregorianCalendar date = mExpiryDate;
                if (date == null) {
                    date = new GregorianCalendar();
                }

                DatePickerDialog dialog = new DatePickerDialog(getContext(),
                        mExpiryDateSetListener, date.get(Calendar.YEAR), date.get(Calendar.MONTH),
                        date.get(Calendar.DAY_OF_MONTH));
                mDatePickerResultCount = 0;
                dialog.setCancelable(true);
                dialog.setButton(Dialog.BUTTON_NEGATIVE, getContext()
                        .getString(R.string.btn_no_date), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        if (mDatePickerResultCount++ == 0) // Note: Ignore results after the first
                                                           // one - android sends multiples.
                        {
                            setExpiryDate(null);
                        }
                    }
                });
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
        String keyId1Str = PgpKeyHelper.convertKeyIdToHex(key.getKeyID());
        String keyId2Str = PgpKeyHelper.convertKeyIdToHex(key.getKeyID() >> 32);
        mKeyId.setText(keyId1Str + " " + keyId2Str);

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

        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(PgpKeyHelper.getCreationDate(key));
        mCreationDate.setText(DateFormat.getDateInstance().format(cal.getTime()));
        cal = new GregorianCalendar();
        Date date = PgpKeyHelper.getExpiryDate(key);
        if (date == null) {
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

    private void setExpiryDate(GregorianCalendar date) {
        mExpiryDate = date;
        if (date == null) {
            mExpiryDateButton.setText(R.string.none);
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
