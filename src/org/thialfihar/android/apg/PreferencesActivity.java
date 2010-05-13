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

package org.thialfihar.android.apg;

import org.bouncycastle2.bcpg.HashAlgorithmTags;
import org.bouncycastle2.openpgp.PGPEncryptedData;
import org.thialfihar.android.apg.utils.Choice;

import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.AdapterView.OnItemSelectedListener;

public class PreferencesActivity extends BaseActivity {
    private Spinner mEncryptionAlgorithm = null;
    private Spinner mHashAlgorithm = null;
    private CheckBox mAsciiArmour = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.preferences);

        mEncryptionAlgorithm = (Spinner) findViewById(R.id.encryptionAlgorithm);
        mHashAlgorithm = (Spinner) findViewById(R.id.hashAlgorithm);
        mAsciiArmour = (CheckBox) findViewById(R.id.asciiArmour);

        mAsciiArmour.setChecked(getDefaultAsciiArmour());

        Choice choices[] = {
                new Choice(PGPEncryptedData.AES_128, "AES 128"),
                new Choice(PGPEncryptedData.AES_192, "AES 192"),
                new Choice(PGPEncryptedData.AES_256, "AES 256"),
                new Choice(PGPEncryptedData.BLOWFISH, "Blowfish"),
                new Choice(PGPEncryptedData.TWOFISH, "Twofish"),
                new Choice(PGPEncryptedData.CAST5, "CAST5"),
                new Choice(PGPEncryptedData.DES, "DES"),
                new Choice(PGPEncryptedData.TRIPLE_DES, "Triple DES"),
                new Choice(PGPEncryptedData.IDEA, "IDEA"),
        };
        ArrayAdapter<Choice> adapter =
                new ArrayAdapter<Choice>(this, android.R.layout.simple_spinner_item, choices);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mEncryptionAlgorithm.setAdapter(adapter);

        for (int i = 0; i < choices.length; ++i) {
            if (choices[i].getId() == getDefaultEncryptionAlgorithm()) {
                mEncryptionAlgorithm.setSelection(i);
                break;
            }
        }

        mEncryptionAlgorithm.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapter, View view, int index, long id) {
                setDefaultEncryptionAlgorithm(((Choice) mEncryptionAlgorithm.getSelectedItem()).getId());
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapter) {
                // nothing to do
            }
        });

        Choice choices2[] = {
                new Choice(HashAlgorithmTags.MD5, "MD5"),
                new Choice(HashAlgorithmTags.RIPEMD160, "RIPEMD160"),
                new Choice(HashAlgorithmTags.SHA1, "SHA1"),
                new Choice(HashAlgorithmTags.SHA224, "SHA224"),
                new Choice(HashAlgorithmTags.SHA256, "SHA256"),
                new Choice(HashAlgorithmTags.SHA384, "SHA384"),
                new Choice(HashAlgorithmTags.SHA512, "SHA512"),
        };
        adapter = new ArrayAdapter<Choice>(this, android.R.layout.simple_spinner_item, choices2);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mHashAlgorithm.setAdapter(adapter);

        for (int i = 0; i < choices2.length; ++i) {
            if (choices2[i].getId() == getDefaultHashAlgorithm()) {
                mHashAlgorithm.setSelection(i);
                break;
            }
        }

        mHashAlgorithm.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapter, View view, int index, long id) {
                setDefaultHashAlgorithm(((Choice) mHashAlgorithm.getSelectedItem()).getId());
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapter) {
                // nothing to do
            }
        });

        mAsciiArmour.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                setDefaultAsciiArmour(mAsciiArmour.isChecked());
            }
        });
    }
}

