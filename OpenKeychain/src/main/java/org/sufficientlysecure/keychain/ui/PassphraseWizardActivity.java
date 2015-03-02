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

package org.sufficientlysecure.keychain.ui;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.sufficientlysecure.keychain.R;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class PassphraseWizardActivity extends FragmentActivity {
//public class PassphraseWizardActivity extends FragmentActivity implements LockPatternView.OnPatternListener {
    //create or authenticate
    public String selectedAction;
    //for lockpattern
    public static char[] pattern;
    private static String passphrase = "";
    //nfc string
    private static byte[] output = new byte[8];

    public static final String CREATE_METHOD = "create";
    public static final String AUTHENTICATION = "authenticate";

    NfcAdapter adapter;
    PendingIntent pendingIntent;
    IntentFilter writeTagFilters[];
    boolean writeMode;
    Tag myTag;
    boolean writeNFC = false;
    boolean readNFC = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getActionBar() != null) {
            getActionBar().setTitle(R.string.unlock_method);
        }

        selectedAction = getIntent().getAction();
        if (savedInstanceState == null) {
            SelectMethods selectMethods = new SelectMethods();
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.add(R.id.fragmentContainer, selectMethods).commit();
        }
        setContentView(R.layout.passphrase_wizard);

        adapter = NfcAdapter.getDefaultAdapter(this);
        if (adapter != null) {
            pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, PassphraseWizardActivity.class).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
            IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
            tagDetected.addCategory(Intent.CATEGORY_DEFAULT);
            writeTagFilters = new IntentFilter[]{tagDetected};
        }
    }

    public void noPassphrase(View view) {
        passphrase = "";
        Toast.makeText(this, R.string.no_passphrase_set, Toast.LENGTH_SHORT).show();
        this.finish();
    }

    public void passphrase(View view) {
        Passphrase passphrase = new Passphrase();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragmentContainer, passphrase).addToBackStack(null).commit();
    }

    public void startLockpattern(View view) {
        if (getActionBar() != null) {
            getActionBar().setTitle(R.string.draw_lockpattern);
        }
//        LockPatternFragmentOld lpf = LockPatternFragmentOld.newInstance(selectedAction);
//        LockPatternFragment lpf = LockPatternFragment.newInstance("asd");

//        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
//        transaction.replace(R.id.fragmentContainer, lpf).addToBackStack(null).commit();
    }

    public void cancel(View view) {
        this.finish();
    }

    public void savePassphrase(View view) {
        EditText passphrase = (EditText) findViewById(R.id.passphrase);
        passphrase.setError(null);
        String pw = passphrase.getText().toString();
        //check and save passphrase
        if (selectedAction.equals(CREATE_METHOD)) {
            EditText passphraseAgain = (EditText) findViewById(R.id.passphraseAgain);
            passphraseAgain.setError(null);
            String pwAgain = passphraseAgain.getText().toString();

            if (!TextUtils.isEmpty(pw)) {
                if (!TextUtils.isEmpty(pwAgain)) {
                    if (pw.equals(pwAgain)) {
                        PassphraseWizardActivity.passphrase = pw;
                        Toast.makeText(this, getString(R.string.passphrase_saved), Toast.LENGTH_SHORT).show();
                        this.finish();
                    } else {
                        passphrase.setError(getString(R.string.passphrase_invalid));
                        passphrase.requestFocus();
                    }
                } else {
                    passphraseAgain.setError(getString(R.string.missing_passphrase));
                    passphraseAgain.requestFocus();
                }
            } else {
                passphrase.setError(getString(R.string.missing_passphrase));
                passphrase.requestFocus();
            }
        }
        //check for right passphrase
        if (selectedAction.equals(AUTHENTICATION)) {
            if (pw.equals(PassphraseWizardActivity.passphrase)) {
                Toast.makeText(this, getString(R.string.unlocked), Toast.LENGTH_SHORT).show();
                this.finish();
            } else {
                passphrase.setError(getString(R.string.passphrase_invalid));
                passphrase.requestFocus();
            }
        }
    }

    public void NFC(View view) {
        if (adapter != null) {
            if (getActionBar() != null) {
                getActionBar().setTitle(R.string.nfc_title);
            }
            NFCFragment nfc = new NFCFragment();
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fragmentContainer, nfc).addToBackStack(null).commit();

            //if you want to create a new method or just authenticate
            if (CREATE_METHOD.equals(selectedAction)) {
                writeNFC = true;
            } else if (AUTHENTICATION.equals(selectedAction)) {
                readNFC = true;
            }

            if (!adapter.isEnabled()) {
                showAlertDialog(getString(R.string.enable_nfc), true);
            }
        } else {
            showAlertDialog(getString(R.string.no_nfc_support), false);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {
            myTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

            if (writeNFC && CREATE_METHOD.equals(selectedAction)) {
                //write new password on NFC tag
                try {
                    if (myTag != null) {
                        write(myTag);
                        writeNFC = false;   //just write once
                        Toast.makeText(this, R.string.nfc_write_succesful, Toast.LENGTH_SHORT).show();
                        //advance to lockpattern
//                        LockPatternFragmentOld lpf = LockPatternFragmentOld.newInstance(selectedAction);
//                        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
//                        transaction.replace(R.id.fragmentContainer, lpf).addToBackStack(null).commit();
                    }
                } catch (IOException | FormatException e) {
                    e.printStackTrace();
                }

            } else if (readNFC && AUTHENTICATION.equals(selectedAction)) {
                //read pw from NFC tag
                try {
                    if (myTag != null) {
                        //if tag detected, read tag
                        String pwtag = read(myTag);
                        if (output != null && pwtag.equals(output.toString())) {

                            //passwort matches, go to next view
                            Toast.makeText(this, R.string.passphrases_match + "!", Toast.LENGTH_SHORT).show();

//                            LockPatternFragmentOld lpf = LockPatternFragmentOld.newInstance(selectedAction);
//                            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
//                            transaction.replace(R.id.fragmentContainer, lpf).addToBackStack(null).commit();
                            readNFC = false;    //just once
                        } else {
                            //passwort doesnt match
                            TextView nfc = (TextView) findViewById(R.id.nfcText);
                            nfc.setText(R.string.nfc_wrong_tag);
                        }
                    }
                } catch (IOException | FormatException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void write(Tag tag) throws IOException, FormatException {
        //generate new random key and write them on the tag
        SecureRandom sr = new SecureRandom();
        sr.nextBytes(output);
        NdefRecord[] records = {createRecord(output.toString())};
        NdefMessage message = new NdefMessage(records);
        Ndef ndef = Ndef.get(tag);
        ndef.connect();
        ndef.writeNdefMessage(message);
        ndef.close();
    }

    private String read(Tag tag) throws IOException, FormatException {
        //read string from tag
        String password = null;
        Ndef ndef = Ndef.get(tag);
        ndef.connect();
        NdefMessage ndefMessage = ndef.getCachedNdefMessage();

        NdefRecord[] records = ndefMessage.getRecords();
        for (NdefRecord ndefRecord : records) {
            if (ndefRecord.getTnf() == NdefRecord.TNF_WELL_KNOWN && Arrays.equals(ndefRecord.getType(), NdefRecord.RTD_TEXT)) {
                try {
                    password = readText(ndefRecord);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        }
        ndef.close();
        return password;
    }

    private String readText(NdefRecord record) throws UnsupportedEncodingException {
        //low-level method for reading nfc
        byte[] payload = record.getPayload();
        String textEncoding = ((payload[0] & 128) == 0) ? "UTF-8" : "UTF-16";
        int languageCodeLength = payload[0] & 0063;
        return new String(payload, languageCodeLength + 1, payload.length - languageCodeLength - 1, textEncoding);
    }

    private NdefRecord createRecord(String text) throws UnsupportedEncodingException {
        //low-level method for writing nfc
        String lang = "en";
        byte[] textBytes = text.getBytes();
        byte[] langBytes = lang.getBytes("US-ASCII");
        int langLength = langBytes.length;
        int textLength = textBytes.length;
        byte[] payload = new byte[1 + langLength + textLength];

        // set status byte (see NDEF spec for actual bits)
        payload[0] = (byte) langLength;
        // copy langbytes and textbytes into payload
        System.arraycopy(langBytes, 0, payload, 1, langLength);
        System.arraycopy(textBytes, 0, payload, 1 + langLength, textLength);
        return new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, new byte[0], payload);
    }

    public void showAlertDialog(String message, boolean nfc) {
        //This method shows an AlertDialog
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Information").setMessage(message).setPositiveButton("Ok",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                    }
                }
        );
        if (nfc) {

            alert.setNeutralButton(R.string.nfc_settings,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialogInterface, int i) {
                            startActivity(new Intent(Settings.ACTION_NFC_SETTINGS));
                        }
                    }
            );
        }
        alert.show();
    }

    @Override
    public void onPause() {
        //pause this app and free nfc intent
        super.onPause();
        if (adapter != null) {
            WriteModeOff();
        }
    }

    @Override
    public void onResume() {
        //resume this app and get nfc intent
        super.onResume();
        if (adapter != null) {
            WriteModeOn();
        }
    }

    private void WriteModeOn() {
        //enable nfc for this view
        writeMode = true;
        adapter.enableForegroundDispatch(this, pendingIntent, writeTagFilters, null);
    }

    private void WriteModeOff() {
        //disable nfc for this view
        writeMode = false;
        adapter.disableForegroundDispatch(this);
    }

    public static class SelectMethods extends Fragment {
//        private OnFragmentInteractionListener mListener;

        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         */
        public SelectMethods() {

        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
        }

        @Override
        public void onResume() {
            super.onResume();
            if (getActivity().getActionBar() != null) {
                getActivity().getActionBar().setTitle(R.string.unlock_method);
            }
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            // Inflate the layout for this fragment
            return inflater.inflate(R.layout.passphrase_wizard_fragment_select_methods, container, false);
        }

//        @Override
//        public void onAttach(Activity activity) {
//            super.onAttach(activity);
//            try {
//                mListener = (OnFragmentInteractionListener) activity;
//            } catch (ClassCastException e) {
//                throw new ClassCastException(activity.toString()
//                        + " must implement OnFragmentInteractionListener");
//            }
//        }
//
//        @Override
//        public void onDetach() {
//            super.onDetach();
//            mListener = null;
//        }

        /**
         * This interface must be implemented by activities that contain this
         * fragment to allow an interaction in this fragment to be communicated
         * to the activity and potentially other fragments contained in that
         * activity.
         * <p/>
         * See the Android Training lesson <a href=
         * "http://developer.android.com/training/basics/fragments/communicating.html"
         * >Communicating with Other Fragments</a> for more information.
         */
//        public static interface OnFragmentInteractionListener {
//            public void onFragmentInteraction(Uri uri);
//        }

    }


    //    /**
//     * A simple {@link android.support.v4.app.Fragment} subclass.
//     * Activities that contain this fragment must implement the
//     * {@link com.haibison.android.lockpattern.Passphrase.OnFragmentInteractionListener} interface
//     * to handle interaction events.
//     */
    public static class Passphrase extends Fragment {

//        private OnFragmentInteractionListener mListener;

        public Passphrase() {
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            // Inflate the layout for this fragment
            View view = inflater.inflate(R.layout.passphrase_wizard_fragment_passphrase, container, false);
            EditText passphraseAgain = (EditText) view.findViewById(R.id.passphraseAgain);
            TextView passphraseText = (TextView) view.findViewById(R.id.passphraseText);
            TextView passphraseTextAgain = (TextView) view.findViewById(R.id.passphraseTextAgain);
            String selectedAction = getActivity().getIntent().getAction();
            if (selectedAction.equals(AUTHENTICATION)) {
                passphraseAgain.setVisibility(View.GONE);
                passphraseTextAgain.setVisibility(View.GONE);
                passphraseText.setText(R.string.enter_passphrase);
//                getActivity().getActionBar().setTitle(R.string.enter_passphrase);
            } else if (selectedAction.equals(CREATE_METHOD)) {
                passphraseAgain.setVisibility(View.VISIBLE);
                passphraseTextAgain.setVisibility(View.VISIBLE);
                passphraseText.setText(R.string.passphrase);
//                getActivity().getActionBar().setTitle(R.string.set_passphrase);
            }
            return view;
        }

//        @Override
//        public void onAttach(Activity activity) {
//            super.onAttach(activity);
//            try {
//                mListener = (OnFragmentInteractionListener) activity;
//            } catch (ClassCastException e) {
//                throw new ClassCastException(activity.toString()
//                        + " must implement OnFragmentInteractionListener");
//            }
//        }
//
//        @Override
//        public void onDetach() {
//            super.onDetach();
//            mListener = null;
//        }

//        /**
//         * This interface must be implemented by activities that contain this
//         * fragment to allow an interaction in this fragment to be communicated
//         * to the activity and potentially other fragments contained in that
//         * activity.
//         * <p/>
//         * See the Android Training lesson <a href=
//         * "http://developer.android.com/training/basics/fragments/communicating.html"
//         * >Communicating with Other Fragments</a> for more information.
//         */
//        public interface OnFragmentInteractionListener {
//            public void onFragmentInteraction(Uri uri);
//        }
    }


    /**
     * A simple {@link android.support.v4.app.Fragment} subclass.
     * Activities that contain this fragment must implement the
     * interface
     * to handle interaction events.
     * Use the  method to
     * create an instance of this fragment.
     */
    public static class NFCFragment extends Fragment {
        // TODO: Rename parameter arguments, choose names that match
        // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
        private static final String ARG_PARAM1 = "param1";
        private static final String ARG_PARAM2 = "param2";

        // TODO: Rename and change types of parameters
        private String mParam1;
        private String mParam2;

//        private OnFragmentInteractionListener mListener;

        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment SelectMethods.
         */
        // TODO: Rename and change types and number of parameters
        public static NFCFragment newInstance(String param1, String param2) {
            NFCFragment fragment = new NFCFragment();
            Bundle args = new Bundle();
            args.putString(ARG_PARAM1, param1);
            args.putString(ARG_PARAM2, param2);
            fragment.setArguments(args);
            return fragment;
        }

        public NFCFragment() {
            // Required empty public constructor
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            if (getArguments() != null) {
                mParam1 = getArguments().getString(ARG_PARAM1);
                mParam2 = getArguments().getString(ARG_PARAM2);
            }
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            // Inflate the layout for this fragment
            return inflater.inflate(R.layout.passphrase_wizard_fragment_nfc, container, false);
        }

//        // TODO: Rename method, update argument and hook method into UI event
//        public void onButtonPressed(Uri uri) {
//            if (mListener != null) {
//                mListener.onFragmentInteraction(uri);
//            }
//        }

//        @Override
//        public void onAttach(Activity activity) {
//            super.onAttach(activity);
//            try {
//                mListener = (OnFragmentInteractionListener) activity;
//            } catch (ClassCastException e) {
//                throw new ClassCastException(activity.toString()
//                        + " must implement OnFragmentInteractionListener");
//            }
//        }


//        @Override
//        public void onDetach() {
//            super.onDetach();
//            mListener = null;
//        }
    }

}
