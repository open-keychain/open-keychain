/*
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

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.beardedhen.androidbootstrap.BootstrapButton;

import org.sufficientlysecure.keychain.R;

public class KeyServerEditor extends LinearLayout implements Editor, OnClickListener {
    private EditorListener mEditorListener = null;

    BootstrapButton mDeleteButton;
    TextView mServer;

    public KeyServerEditor(Context context) {
        super(context);
    }

    public KeyServerEditor(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        setDrawingCacheEnabled(true);
        setAlwaysDrawnWithCacheEnabled(true);

        mServer = (TextView) findViewById(R.id.server);

        mDeleteButton = (BootstrapButton) findViewById(R.id.delete);
        mDeleteButton.setOnClickListener(this);

        super.onFinishInflate();
    }

    public void setValue(String value) {
        mServer.setText(value);
    }

    public String getValue() {
        return mServer.getText().toString().trim();
    }

    public void onClick(View v) {
        final ViewGroup parent = (ViewGroup) getParent();
        if (v == mDeleteButton) {
            parent.removeView(this);
            if (mEditorListener != null) {
                mEditorListener.onDeleted(this, false);
            }
        }
    }

    @Override
    public boolean needsSaving() {
        return false;
    }

    public void setEditorListener(EditorListener listener) {
        mEditorListener = listener;
    }
}
