/*
 * Copyright (C) 2014 Dominik Schürmann <dominik@dominikschuermann.de>
 * Copyright (C) 2014 Vincent Breitmoser <v.breitmoser@mugenguild.com>
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

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.operations.results.OperationResult;
import org.sufficientlysecure.keychain.operations.results.OperationResult.SubLogEntryParcel;
import org.sufficientlysecure.keychain.provider.TemporaryFileProvider;
import org.sufficientlysecure.keychain.ui.dialog.ShareLogDialogFragment;
import org.sufficientlysecure.keychain.ui.util.Notify;
import org.sufficientlysecure.keychain.ui.util.Notify.Style;
import org.sufficientlysecure.keychain.ui.base.RecyclerFragment;
import org.sufficientlysecure.keychain.ui.util.recyclerview.item.LogDummyItem;
import org.sufficientlysecure.keychain.ui.util.recyclerview.item.LogHeaderItem;
import org.sufficientlysecure.keychain.ui.util.recyclerview.item.LogItem;
import org.sufficientlysecure.keychain.ui.util.recyclerview.item.RegularLogHeaderItem;
import org.sufficientlysecure.keychain.ui.util.recyclerview.item.RegularLogItem;
import org.sufficientlysecure.keychain.ui.util.recyclerview.item.SublogHeaderItem;
import org.sufficientlysecure.keychain.ui.util.recyclerview.item.SublogItem;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import eu.davidea.flexibleadapter.FlexibleAdapter;
import eu.davidea.flexibleadapter.common.SmoothScrollLinearLayoutManager;


public class LogDisplayFragment extends RecyclerFragment<FlexibleAdapter<LogItem>>
        implements FlexibleAdapter.OnItemClickListener {
    private OperationResult mResult;

    public static final String EXTRA_RESULT = "log";
    private Uri mLogTempFile;

    private List<LogItem> items = new ArrayList<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Intent intent = getActivity().getIntent();
        if (intent == null) {
            getActivity().finish();
            return;
        }

        if (savedInstanceState != null) {
            mResult = savedInstanceState.getParcelable(EXTRA_RESULT);
        } else {
            mResult = intent.getParcelableExtra(EXTRA_RESULT);
        }

        if (mResult == null) {
            getActivity().finish();
            return;
        }

        final int ENTRY_TYPE_REGULAR = 0;
        final int ENTRY_TYPE_SUBLOG = 1;
        final int LOG_ENTRY_ITEM_INDENT = 2;

        items.clear();

        LogHeaderItem lastHeader = null;
        for (OperationResult.LogEntryParcel parcel : mResult.getLog()) {
            if (parcel.mIndent < LOG_ENTRY_ITEM_INDENT) {
                if (parcel instanceof SubLogEntryParcel) {
                    lastHeader = new SublogHeaderItem(parcel);
                } else {
                    lastHeader = new RegularLogHeaderItem(parcel);
                }
                items.add(new LogDummyItem(lastHeader));
            } else {
                if (parcel instanceof SubLogEntryParcel) {
                    items.add(new SublogItem(lastHeader, parcel));
                } else {
                    items.add(new RegularLogItem(lastHeader, parcel));
                }
            }
        }

        List<LogItem> itemList = new ArrayList<>(items);

        FlexibleAdapter<LogItem> adapter = new FlexibleAdapter<>(itemList);
        adapter.setDisplayHeadersAtStartUp(true)
                .setStickyHeaders(true, getHeaderContainerWithPadding());
        adapter.addListener(this);
        setAdapter(adapter);

        setLayoutManager(new SmoothScrollLinearLayoutManager(getContext()));
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // need to parcel this again, logs are only single-instance parcelable
        outState.putParcelable(EXTRA_RESULT, mResult);
    }

    @Override
    public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
        inflater.inflate(R.menu.log_display, menu);

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_log_display_export_log:
                shareLog();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void shareLog() {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }

        String log = mResult.getLog().getPrintableOperationLog(getResources(), 0);

        // if there is no log temp file yet, create one
        if (mLogTempFile == null) {
            mLogTempFile = TemporaryFileProvider.createFile(getActivity(), "openkeychain_log.txt", "text/plain");
            try {
                OutputStream outputStream = activity.getContentResolver().openOutputStream(mLogTempFile);
                outputStream.write(log.getBytes());
            } catch (IOException | NullPointerException e) {
                Notify.create(activity, R.string.error_log_share_internal, Style.ERROR).show();
                return;
            }
        }


        ShareLogDialogFragment shareLogDialog = ShareLogDialogFragment.newInstance(mLogTempFile);
        shareLogDialog.show(getActivity().getSupportFragmentManager(), "shareLogDialog");
    }

    /* @Override
    public void onSubEntryClicked(SubLogEntryParcel subLogEntryParcel) {
        Intent intent = new Intent(getActivity(), LogDisplayActivity.class);
        intent.putExtra(LogDisplayFragment.EXTRA_RESULT, subLogEntryParcel.getSubResult());
        startActivity(intent);
    } */

    @Override
    public boolean onItemClick(int position) {
        LogItem item = getAdapter().getItem(position);
        if (item instanceof SublogItem) {
            Intent intent = new Intent(getActivity(), LogDisplayActivity.class);
            intent.putExtra(LogDisplayFragment.EXTRA_RESULT,
                    ((SubLogEntryParcel) item.getEntry()).getSubResult());
            startActivity(intent);
            return true;
        }
        return false;
    }

    /* static class MyNestedLogAdapter extends FlexibleAdapter<LogAbstractItem> {

        MyNestedLogAdapter(List<LogAbstractItem> logAbstractItems) {
            super(logAbstractItems);
        }


        public void setLog(OperationResult.OperationLog log) {
            List<OperationResult.LogEntryParcel> list = log.toList();

            if (mLogEntries != null) {
                mLogEntries.clear();
            } else {
                mLogEntries = new ArrayList<>(list.size());
            }

            int lastSection = 0;
            for (int i = 0; i < list.size(); i++) {
                OperationResult.LogEntryParcel parcel = list.get(i);
                if(parcel.mIndent < LOG_ENTRY_ITEM_INDENT) {
                    lastSection = i;
                }

                mLogEntries.add(new Pair<>(parcel, lastSection));
            }

            notifyDataSetChanged();
        }
    } */
}
