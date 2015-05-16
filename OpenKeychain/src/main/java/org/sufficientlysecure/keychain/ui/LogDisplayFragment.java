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

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Parcel;
import android.support.v4.app.ListFragment;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.operations.results.OperationResult;
import org.sufficientlysecure.keychain.operations.results.OperationResult.LogEntryParcel;
import org.sufficientlysecure.keychain.operations.results.OperationResult.LogLevel;
import org.sufficientlysecure.keychain.operations.results.OperationResult.SubLogEntryParcel;
import org.sufficientlysecure.keychain.util.FileHelper;
import org.sufficientlysecure.keychain.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

public class LogDisplayFragment extends ListFragment implements OnItemClickListener {

    LogAdapter mAdapter;

    OperationResult mResult;

    public static final String EXTRA_RESULT = "log";

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

        mAdapter = new LogAdapter(getActivity(), mResult.getLog());
        setListAdapter(mAdapter);

        getListView().setOnItemClickListener(this);

        getListView().setFastScrollEnabled(true);
        getListView().setDividerHeight(0);

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
                exportLog();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void exportLog() {
        showExportLogDialog(new File(Constants.Path.APP_DIR, "export.log"));
    }

    private void writeToLogFile(final OperationResult.OperationLog operationLog, final File f) {
        OperationResult.OperationLog currLog = new OperationResult.OperationLog();
        currLog.add(OperationResult.LogType.MSG_EXPORT_LOG, 0);

        boolean error = false;

        PrintWriter pw = null;
        try {
            pw = new PrintWriter(f);
            pw.print(getPrintableOperationLog(operationLog, ""));
            if (pw.checkError()) {//IOException
                Log.e(Constants.TAG, "Log Export I/O Exception " + f.getAbsolutePath());
                currLog.add(OperationResult.LogType.MSG_EXPORT_LOG_EXPORT_ERROR_WRITING, 1);
                error = true;
            }
        } catch (FileNotFoundException e) {
            Log.e(Constants.TAG, "File not found for exporting log " + f.getAbsolutePath());
            currLog.add(OperationResult.LogType.MSG_EXPORT_LOG_EXPORT_ERROR_FOPEN, 1);
            error = true;
        }
        if (pw != null) {
            pw.close();
            if (!error && pw.checkError()) {//check if it is only pw.close() which generated error
                currLog.add(OperationResult.LogType.MSG_EXPORT_LOG_EXPORT_ERROR_WRITING, 1);
                error = true;
            }
        }

        if (!error) {
            currLog.add(OperationResult.LogType.MSG_EXPORT_LOG_EXPORT_SUCCESS, 1);
        }

        int opResultCode = error ? OperationResult.RESULT_ERROR : OperationResult.RESULT_OK;
        OperationResult opResult = new LogExportResult(opResultCode, currLog);
        opResult.createNotify(getActivity()).show();
    }

    /**
     * returns an indented String of an entire OperationLog
     *
     * @param opLog       log to be converted to indented, printable format
     * @param basePadding padding to add at the start of all log entries, made for use with SubLogs
     * @return printable, indented version of passed operationLog
     */
    private String getPrintableOperationLog(OperationResult.OperationLog opLog, String basePadding) {
        String log = "";
        for (LogEntryParcel anOpLog : opLog) {
            log += getPrintableLogEntry(anOpLog, basePadding) + "\n";
        }
        log = log.substring(0, log.length() - 1);//gets rid of extra new line
        return log;
    }

    /**
     * returns an indented String of a LogEntryParcel including any sub-logs it may contain
     *
     * @param entryParcel log entryParcel whose String representation is to be obtained
     * @return indented version of passed log entryParcel in a readable format
     */
    private String getPrintableLogEntry(OperationResult.LogEntryParcel entryParcel,
                                        String basePadding) {

        final String indent = "    ";//4 spaces = 1 Indent level

        String padding = basePadding;
        for (int i = 0; i < entryParcel.mIndent; i++) {
            padding += indent;
        }
        String logText = padding;

        switch (entryParcel.mType.mLevel) {
            case DEBUG:
                logText += "[DEBUG]";
                break;
            case INFO:
                logText += "[INFO]";
                break;
            case WARN:
                logText += "[WARN]";
                break;
            case ERROR:
                logText += "[ERROR]";
                break;
            case START:
                logText += "[START]";
                break;
            case OK:
                logText += "[OK]";
                break;
            case CANCELLED:
                logText += "[CANCELLED]";
                break;
        }

        // special case: first parameter may be a quantity
        if (entryParcel.mParameters != null && entryParcel.mParameters.length > 0
                && entryParcel.mParameters[0] instanceof Integer) {
            logText += getResources().getQuantityString(entryParcel.mType.getMsgId(),
                    (Integer) entryParcel.mParameters[0],
                    entryParcel.mParameters);
        } else {
            logText += getResources().getString(entryParcel.mType.getMsgId(),
                    entryParcel.mParameters);
        }

        if (entryParcel instanceof SubLogEntryParcel) {
            OperationResult subResult = ((SubLogEntryParcel) entryParcel).getSubResult();
            LogEntryParcel subEntry = subResult.getLog().getLast();
            if (subEntry != null) {
                //the first line of log of subResult is same as entryParcel, so replace logText
                logText = getPrintableOperationLog(subResult.getLog(), padding);
            }
        }

        return logText;
    }

    private void showExportLogDialog(final File exportFile) {

        String title = this.getString(R.string.title_export_log);

        String message = this.getString(R.string.specify_file_to_export_log_to);

        FileHelper.saveFile(new FileHelper.FileDialogCallback() {
            @Override
            public void onFileSelected(File file, boolean checked) {
                writeToLogFile(mResult.getLog(), file);
            }
        }, this.getActivity().getSupportFragmentManager(), title, message, exportFile, null);
    }

    private static class LogExportResult extends OperationResult {

        public static Creator<LogExportResult> CREATOR = new Creator<LogExportResult>() {
            public LogExportResult createFromParcel(final Parcel source) {
                return new LogExportResult(source);
            }

            public LogExportResult[] newArray(final int size) {
                return new LogExportResult[size];
            }
        };

        public LogExportResult(int result, OperationLog log) {
            super(result, log);
        }

        /**
         * trivial but necessary to implement the Parcelable protocol.
         */
        public LogExportResult(Parcel source) {
            super(source);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        LogEntryParcel parcel = mAdapter.getItem(position);
        if ( ! (parcel instanceof SubLogEntryParcel)) {
            return;
        }
        Intent intent = new Intent(
                getActivity(), LogDisplayActivity.class);
        intent.putExtra(LogDisplayFragment.EXTRA_RESULT, ((SubLogEntryParcel) parcel).getSubResult());
        startActivity(intent);
    }

    private class LogAdapter extends ArrayAdapter<LogEntryParcel> {

        private LayoutInflater mInflater;
        private int dipFactor;

        public LogAdapter(Context context, OperationResult.OperationLog log) {
            super(context, R.layout.log_display_item, log.toList());
            mInflater = LayoutInflater.from(getContext());
            dipFactor = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                    (float) 8, getResources().getDisplayMetrics());
        }

        private class ItemHolder {
            final View mSecond;
            final TextView mText, mSecondText;
            final ImageView mImg, mSecondImg, mSub;
            public ItemHolder(TextView text, ImageView image, ImageView sub, View second, TextView secondText, ImageView secondImg) {
                mText = text;
                mImg = image;
                mSub = sub;
                mSecond = second;
                mSecondText = secondText;
                mSecondImg = secondImg;
            }
        }
        // Check if convertView.setPadding is redundant
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LogEntryParcel entry = getItem(position);
            ItemHolder ih;
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.log_display_item, parent, false);
                ih = new ItemHolder(
                        (TextView) convertView.findViewById(R.id.log_text),
                        (ImageView) convertView.findViewById(R.id.log_img),
                        (ImageView) convertView.findViewById(R.id.log_sub),
                        convertView.findViewById(R.id.log_second),
                        (TextView) convertView.findViewById(R.id.log_second_text),
                        (ImageView) convertView.findViewById(R.id.log_second_img)
                );
                convertView.setTag(ih);
            } else {
                ih = (ItemHolder) convertView.getTag();
            }

            if (entry instanceof SubLogEntryParcel) {
                ih.mSub.setVisibility(View.VISIBLE);
                convertView.setClickable(false);
                convertView.setPadding((entry.mIndent) * dipFactor, 0, 0, 0);

                OperationResult result = ((SubLogEntryParcel) entry).getSubResult();
                LogEntryParcel subEntry = result.getLog().getLast();
                if (subEntry != null) {
                    ih.mSecond.setVisibility(View.VISIBLE);
                    // special case: first parameter may be a quantity
                    if (subEntry.mParameters != null && subEntry.mParameters.length > 0
                            && subEntry.mParameters[0] instanceof Integer) {
                        ih.mSecondText.setText(getResources().getQuantityString(subEntry.mType.getMsgId(),
                                (Integer) subEntry.mParameters[0],
                                subEntry.mParameters));
                    } else {
                        ih.mSecondText.setText(getResources().getString(subEntry.mType.getMsgId(),
                                subEntry.mParameters));
                    }
                    ih.mSecondText.setTextColor(subEntry.mType.mLevel == LogLevel.DEBUG ? Color.GRAY : Color.BLACK);
                    switch (subEntry.mType.mLevel) {
                        case DEBUG: ih.mSecondImg.setBackgroundColor(Color.GRAY); break;
                        case INFO: ih.mSecondImg.setBackgroundColor(Color.BLACK); break;
                        case WARN: ih.mSecondImg.setBackgroundColor(getResources().getColor(R.color.android_orange_light)); break;
                        case ERROR: ih.mSecondImg.setBackgroundColor(getResources().getColor(R.color.android_red_light)); break;
                        case START: ih.mSecondImg.setBackgroundColor(Color.BLACK); break;
                        case OK: ih.mSecondImg.setBackgroundColor(getResources().getColor(R.color.android_green_light)); break;
                        case CANCELLED: ih.mSecondImg.setBackgroundColor(getResources().getColor(R.color.android_red_light)); break;
                    }
                } else {
                    ih.mSecond.setVisibility(View.GONE);
                }

            } else {
                ih.mSub.setVisibility(View.GONE);
                ih.mSecond.setVisibility(View.GONE);
                convertView.setClickable(true);
            }

            // special case: first parameter may be a quantity
            if (entry.mParameters != null && entry.mParameters.length > 0
                    && entry.mParameters[0] instanceof Integer) {
                ih.mText.setText(getResources().getQuantityString(entry.mType.getMsgId(),
                        (Integer) entry.mParameters[0],
                        entry.mParameters));
            } else {
                ih.mText.setText(getResources().getString(entry.mType.getMsgId(),
                        entry.mParameters));
            }
            convertView.setPadding((entry.mIndent) * dipFactor, 0, 0, 0);
            ih.mText.setTextColor(entry.mType.mLevel == LogLevel.DEBUG ? Color.GRAY : Color.BLACK);
            switch (entry.mType.mLevel) {
                case DEBUG: ih.mImg.setBackgroundColor(Color.GRAY); break;
                case INFO: ih.mImg.setBackgroundColor(Color.BLACK); break;
                case WARN: ih.mImg.setBackgroundColor(getResources().getColor(R.color.android_orange_light)); break;
                case ERROR: ih.mImg.setBackgroundColor(getResources().getColor(R.color.android_red_light)); break;
                case START: ih.mImg.setBackgroundColor(Color.BLACK); break;
                case OK: ih.mImg.setBackgroundColor(getResources().getColor(R.color.android_green_light)); break;
                case CANCELLED: ih.mImg.setBackgroundColor(getResources().getColor(R.color.android_red_light)); break;
            }

            return convertView;
        }

    }
}
