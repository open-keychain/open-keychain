/*
 * Copyright (C) 2012-2015 Dominik Schürmann <dominik@dominikschuermann.de>
 * Copyright (C) 2015 Adithya Abraham Philip <adithyaphilip@gmail.com>
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

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.support.v4.app.Fragment;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.dialog.AddEditKeyserverDialogFragment;
import org.sufficientlysecure.keychain.ui.util.recyclerview.ItemTouchHelperAdapter;
import org.sufficientlysecure.keychain.ui.util.recyclerview.ItemTouchHelperViewHolder;
import org.sufficientlysecure.keychain.ui.util.recyclerview.ItemTouchHelperDragCallback;
import org.sufficientlysecure.keychain.ui.util.Notify;
import org.sufficientlysecure.keychain.ui.util.recyclerview.RecyclerItemClickListener;
import org.sufficientlysecure.keychain.util.Preferences;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SettingsKeyserverFragment extends Fragment implements RecyclerItemClickListener.OnItemClickListener {

    private static final String ARG_KEYSERVER_ARRAY = "arg_keyserver_array";
    private ItemTouchHelper mItemTouchHelper;

    private ArrayList<String> mKeyservers;
    private KeyserverListAdapter mAdapter;

    public static SettingsKeyserverFragment newInstance(String[] keyservers) {
        Bundle args = new Bundle();
        args.putStringArray(ARG_KEYSERVER_ARRAY, keyservers);

        SettingsKeyserverFragment fragment = new SettingsKeyserverFragment();
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {

        return inflater.inflate(R.layout.settings_keyserver_fragment, null);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String keyservers[] = getArguments().getStringArray(ARG_KEYSERVER_ARRAY);
        mKeyservers = new ArrayList<>(Arrays.asList(keyservers));

        mAdapter = new KeyserverListAdapter(mKeyservers);

        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.keyserver_recycler_view);
        // recyclerView.setHasFixedSize(true); // the size of the first item changes
        recyclerView.setAdapter(mAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));


        ItemTouchHelper.Callback callback = new ItemTouchHelperDragCallback(mAdapter);
        mItemTouchHelper = new ItemTouchHelper(callback);
        mItemTouchHelper.attachToRecyclerView(recyclerView);

        // for clicks
        recyclerView.addOnItemTouchListener(new RecyclerItemClickListener(getActivity(), this));

        // can't use item decoration because it doesn't move with drag and drop
        // recyclerView.addItemDecoration(new DividerItemDecoration(getActivity(), null));

        // We have a menu item to show in action bar.
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
        inflater.inflate(R.menu.keyserver_pref_menu, menu);

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.menu_add_keyserver:
                startAddKeyserverDialog();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void startAddKeyserverDialog() {
        // keyserver and position have no meaning
        startEditKeyserverDialog(AddEditKeyserverDialogFragment.DialogAction.ADD, null, -1);
    }

    private void startEditKeyserverDialog(AddEditKeyserverDialogFragment.DialogAction action,
                                          String keyserver, final int position) {
        Handler returnHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                Bundle data = message.getData();
                switch (message.what) {
                    case AddEditKeyserverDialogFragment.MESSAGE_OKAY: {
                        boolean deleted =
                                data.getBoolean(AddEditKeyserverDialogFragment.MESSAGE_KEYSERVER_DELETED
                                        , false);
                        if (deleted) {
                            Notify.create(getActivity(),
                                    getActivity().getString(
                                            R.string.keyserver_preference_deleted, mKeyservers.get(position)),
                                    Notify.Style.OK)
                                    .show();
                            deleteKeyserver(position);
                            return;
                        }
                        boolean verified =
                                data.getBoolean(AddEditKeyserverDialogFragment.MESSAGE_VERIFIED);
                        if (verified) {
                            Notify.create(getActivity(),
                                    R.string.add_keyserver_verified, Notify.Style.OK).show();
                        } else {
                            Notify.create(getActivity(),
                                    R.string.add_keyserver_without_verification,
                                    Notify.Style.WARN).show();
                        }
                        String keyserver = data.getString(
                                AddEditKeyserverDialogFragment.MESSAGE_KEYSERVER);

                        AddEditKeyserverDialogFragment.DialogAction dialogAction
                                = (AddEditKeyserverDialogFragment.DialogAction) data.getSerializable(
                                AddEditKeyserverDialogFragment.MESSAGE_DIALOG_ACTION);
                        switch (dialogAction) {
                            case ADD:
                                addKeyserver(keyserver);
                                break;
                            case EDIT:
                                editKeyserver(keyserver, position);
                                break;
                        }
                        break;
                    }
                    case AddEditKeyserverDialogFragment.MESSAGE_VERIFICATION_FAILED: {
                        AddEditKeyserverDialogFragment.FailureReason failureReason =
                                (AddEditKeyserverDialogFragment.FailureReason) data.getSerializable(
                                        AddEditKeyserverDialogFragment.MESSAGE_FAILURE_REASON);
                        switch (failureReason) {
                            case CONNECTION_FAILED: {
                                Notify.create(getActivity(),
                                        R.string.add_keyserver_connection_failed,
                                        Notify.Style.ERROR).show();
                                break;
                            }
                            case INVALID_URL: {
                                Notify.create(getActivity(),
                                        R.string.add_keyserver_invalid_url,
                                        Notify.Style.ERROR).show();
                                break;
                            }
                        }
                        break;
                    }
                }
            }
        };

        // Create a new Messenger for the communication back
        Messenger messenger = new Messenger(returnHandler);
        AddEditKeyserverDialogFragment dialogFragment = AddEditKeyserverDialogFragment
                .newInstance(messenger, action, keyserver, position);
        dialogFragment.show(getFragmentManager(), "addKeyserverDialog");
    }

    private void addKeyserver(String keyserver) {
        mKeyservers.add(keyserver);
        mAdapter.notifyItemInserted(mKeyservers.size() - 1);
        saveKeyserverList();
    }

    private void editKeyserver(String newKeyserver, int position) {
        mKeyservers.set(position, newKeyserver);
        mAdapter.notifyItemChanged(position);
        saveKeyserverList();
    }

    private void deleteKeyserver(int position) {
        if (mKeyservers.size() == 1) {
            Notify.create(getActivity(), R.string.keyserver_preference_cannot_delete_last,
                    Notify.Style.ERROR).show();
            return;
        }
        mKeyservers.remove(position);
        // we use this
        mAdapter.notifyItemRemoved(position);
        if (position == 0 && mKeyservers.size() > 0) {
            // if we deleted the first item, we need the adapter to redraw the new first item
            mAdapter.notifyItemChanged(0);
        }
        saveKeyserverList();
    }

    private void saveKeyserverList() {
        String servers[] = mKeyservers.toArray(new String[mKeyservers.size()]);
        Preferences.getPreferences(getActivity()).setKeyServers(servers);
    }

    @Override
    public void onItemClick(View view, int position) {
        startEditKeyserverDialog(AddEditKeyserverDialogFragment.DialogAction.EDIT,
                mKeyservers.get(position), position);
    }

    public class KeyserverListAdapter extends RecyclerView.Adapter<KeyserverListAdapter.ViewHolder>
            implements ItemTouchHelperAdapter {

        private final List<String> mKeyservers;

        public KeyserverListAdapter(List<String> keyservers) {
            mKeyservers = keyservers;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.settings_keyserver_item, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            holder.keyserverUrl.setText(mKeyservers.get(position));

            // Start a drag whenever the handle view it touched
            holder.dragHandleView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (MotionEventCompat.getActionMasked(event) == MotionEvent.ACTION_DOWN) {
                        mItemTouchHelper.startDrag(holder);
                    }
                    return false;
                }
            });

            selectUnselectKeyserver(holder, position);
        }

        private void selectUnselectKeyserver(ViewHolder holder, int position) {

            if (position == 0) {
                holder.showAsSelectedKeyserver();
            } else {
                holder.showAsUnselectedKeyserver();
            }
        }

        @Override
        public void onItemMove(RecyclerView.ViewHolder source, RecyclerView.ViewHolder target,
                               int fromPosition, int toPosition) {
            Collections.swap(mKeyservers, fromPosition, toPosition);
            saveKeyserverList();
            selectUnselectKeyserver((ViewHolder) target, fromPosition);
            // we don't want source to change color while dragging, therefore we just set
            // isSelectedKeyserver instead of selectUnselectKeyserver
            ((ViewHolder) source).isSelectedKeyserver = toPosition == 0;

            notifyItemMoved(fromPosition, toPosition);
        }

        @Override
        public int getItemCount() {
            return mKeyservers.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder implements
                ItemTouchHelperViewHolder {

            public final ViewGroup outerLayout;
            public final TextView selectedServerLabel;
            public final TextView keyserverUrl;
            public final ImageView dragHandleView;

            private boolean isSelectedKeyserver = false;

            public ViewHolder(View itemView) {
                super(itemView);
                outerLayout = (ViewGroup) itemView.findViewById(R.id.outer_layout);
                selectedServerLabel = (TextView) itemView.findViewById(
                        R.id.selected_keyserver_title);
                keyserverUrl = (TextView) itemView.findViewById(R.id.keyserver_tv);
                dragHandleView = (ImageView) itemView.findViewById(R.id.drag_handle);

                itemView.setClickable(true);
            }

            public void showAsSelectedKeyserver() {
                isSelectedKeyserver = true;
                selectedServerLabel.setVisibility(View.VISIBLE);
                outerLayout.setBackgroundColor(getResources().getColor(R.color.android_green_dark));
            }

            public void showAsUnselectedKeyserver() {
                isSelectedKeyserver = false;
                selectedServerLabel.setVisibility(View.GONE);
                outerLayout.setBackgroundColor(Color.WHITE);
            }

            @Override
            public void onItemSelected() {
                selectedServerLabel.setVisibility(View.GONE);
                itemView.setBackgroundColor(Color.LTGRAY);
            }

            @Override
            public void onItemClear() {
                if (isSelectedKeyserver) {
                    showAsSelectedKeyserver();
                } else {
                    showAsUnselectedKeyserver();
                }
            }
        }
    }
}
