/*
 * Copyright (C) 2015 Dominik Schürmann <dominik@dominikschuermann.de>
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


import java.security.SecureRandom;
import java.util.Random;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import org.sufficientlysecure.keychain.R;


public class BackupCodeDisplayFragment extends Fragment {

    public static final String ARG_BACKUP_CODE = "backup_code";

    private String mBackupCode;

    private TextView vBackupCode;
    private Button vOkButton;

    public static BackupCodeDisplayFragment newInstance() {
        return new BackupCodeDisplayFragment();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.backup_code_display_fragment, container, false);

        vBackupCode = (TextView) view.findViewById(R.id.backup_code);
        vOkButton = (Button) view.findViewById(R.id.button_ok);

        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (savedInstanceState == null) {
            mBackupCode = generateRandomCode();
        } else {
            mBackupCode = savedInstanceState.getString(ARG_BACKUP_CODE);
        }

        vBackupCode.setText(mBackupCode);

        vOkButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                moveToCodeEntryFragment();
            }
        });

    }

    private void moveToCodeEntryFragment() {
        Fragment frag = BackupCodeEntryFragment.newInstance(mBackupCode);
        getFragmentManager().beginTransaction()
                .addToBackStack("backup_code_display")
                .replace(R.id.content_frame, frag)
                .commit();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(ARG_BACKUP_CODE, mBackupCode);
    }

    @NonNull
    private static String generateRandomCode() {

        Random r = new SecureRandom();

        // simple generation of a 20 character backup code
        StringBuilder code = new StringBuilder(28);
        for (int i = 0; i < 24; i++) {
            if (i == 6 || i == 12 || i == 18) {
                code.append('-');
            }
            code.append((char) ('A' + r.nextInt(26)));
        }

        return code.toString();

    }

}
