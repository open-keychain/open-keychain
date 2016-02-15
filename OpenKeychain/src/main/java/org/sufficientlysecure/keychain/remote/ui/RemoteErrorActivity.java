/*
 * Copyright (C) 2016 Dominik Schürmann <dominik@dominikschuermann.de>
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

package org.sufficientlysecure.keychain.remote.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.TextView;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.base.BaseActivity;

public class RemoteErrorActivity extends BaseActivity {

    public static final String EXTRA_ERROR_MESSAGE = "error_message";

    public static final String EXTRA_DATA = "data";

    @Override
    protected void initLayout() {
        setContentView(R.layout.api_remote_error_message);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String errorMessage = getIntent().getStringExtra(EXTRA_ERROR_MESSAGE);

        Spannable redErrorMessage = new SpannableString(errorMessage);
        redErrorMessage.setSpan(new ForegroundColorSpan(Color.RED), 0, errorMessage.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        initToolbar();

        // Inflate a "Done" custom action bar view
        setFullScreenDialogClose(
                new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        RemoteErrorActivity.this.setResult(RESULT_CANCELED);
                        RemoteErrorActivity.this.finish();
                    }
                }
        );

        // set text on view
        TextView textView = (TextView) findViewById(R.id.api_app_error_message_text);
        textView.setText(redErrorMessage);
    }

}
