package org.sufficientlysecure.keychain.ui.adapter;

import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.provider.KeychainContract.Certs;
import org.sufficientlysecure.keychain.service.SaveKeyringParcel;

import java.util.ArrayList;

public class UserIdsSelectableAdapter extends UserIdsAdapter implements AdapterView.OnItemClickListener  {

    private final ArrayList<Boolean> mCheckStates;

    public UserIdsSelectableAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);

        mCheckStates = new ArrayList<Boolean>();
    }

    @Override
    public Cursor swapCursor(Cursor newCursor) {
        if (mCheckStates != null) {
            mCheckStates.clear();
            if (newCursor != null) {
                int count = newCursor.getCount();
                mCheckStates.ensureCapacity(count);
                // initialize to true (use case knowledge: we usually want to sign all uids)
                for (int i = 0; i < count; i++) {
                    newCursor.moveToPosition(i);
                    int verified = newCursor.getInt(INDEX_VERIFIED);
                    mCheckStates.add(verified != Certs.VERIFIED_SECRET);
                }
            }
        }

        return super.swapCursor(newCursor);
    }

    @Override
    public void onItemClick(AdapterView<?> adapter, View view, int position, long id) {
        CheckBox box = ((CheckBox) view.findViewById(R.id.user_id_item_check_box));
        if (box != null) {
            box.toggle();
        }
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        super.bindView(view, context, cursor);

        final CheckBox vCheckBox = (CheckBox) view.findViewById(R.id.user_id_item_check_box);
        final int position = cursor.getPosition();
        vCheckBox.setOnCheckedChangeListener(null);
        vCheckBox.setChecked(mCheckStates.get(position));
        vCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                mCheckStates.set(position, b);
            }
        });
        vCheckBox.setClickable(false);
    }

    public ArrayList<String> getSelectedUserIds() {
        ArrayList<String> result = new ArrayList<>();
        for (int i = 0; i < mCheckStates.size(); i++) {
            if (mCheckStates.get(i)) {
                mCursor.moveToPosition(i);
                result.add(mCursor.getString(INDEX_USER_ID));
            }
        }
        return result;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View view = mInflater.inflate(R.layout.view_key_selectable_user_id_item, null);
        return view;
    }

}
