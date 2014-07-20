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

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.helper.FileHelper;
import org.sufficientlysecure.keychain.helper.OtherHelper;
import org.sufficientlysecure.keychain.helper.Preferences;
import org.sufficientlysecure.keychain.provider.TemporaryStorageProvider;
import org.sufficientlysecure.keychain.util.Choice;

import java.io.File;
import java.util.List;

public class EncryptFileFragment extends Fragment implements EncryptActivityInterface.UpdateListener {
    public static final String ARG_URIS = "uris";

    private static final int REQUEST_CODE_INPUT = 0x00007003;
    private static final int REQUEST_CODE_OUTPUT = 0x00007007;

    private EncryptActivityInterface mEncryptInterface;

    // view
    private View mAddView;
    private View mShareFile;
    private View mEncryptFile;
    private SelectedFilesAdapter mAdapter = new SelectedFilesAdapter();

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mEncryptInterface = (EncryptActivityInterface) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement EncryptActivityInterface");
        }
    }

    /**
     * Inflate the layout for this fragment
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.encrypt_file_fragment, container, false);

        mEncryptFile = view.findViewById(R.id.action_encrypt_file);
        mEncryptFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                encryptClicked(false);
            }
        });
        mShareFile = view.findViewById(R.id.action_encrypt_share);
        mShareFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                encryptClicked(true);
            }
        });

        mAddView = inflater.inflate(R.layout.file_list_entry_add, null);
        mAddView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Constants.KITKAT) {
                    FileHelper.openDocument(EncryptFileFragment.this, "*/*", REQUEST_CODE_INPUT);
                } else {
                    FileHelper.openFile(EncryptFileFragment.this,
                            mEncryptInterface.getInputUris().isEmpty() ? null : mEncryptInterface.getInputUris().get(mEncryptInterface.getInputUris().size() - 1), "*/*", REQUEST_CODE_INPUT);
                }
            }
        });
        ListView listView = (ListView) view.findViewById(R.id.selected_files_list);
        listView.addFooterView(mAddView);
        listView.setAdapter(mAdapter);

        /*
        mFileCompression = (Spinner) view.findViewById(R.id.fileCompression);
        Choice[] choices = new Choice[]{
                new Choice(Constants.choice.compression.none, getString(R.string.choice_none) + " ("
                        + getString(R.string.compression_fast) + ")"),
                new Choice(Constants.choice.compression.zip, "ZIP ("
                        + getString(R.string.compression_fast) + ")"),
                new Choice(Constants.choice.compression.zlib, "ZLIB ("
                        + getString(R.string.compression_fast) + ")"),
                new Choice(Constants.choice.compression.bzip2, "BZIP2 ("
                        + getString(R.string.compression_very_slow) + ")"),
        };
        ArrayAdapter<Choice> adapter = new ArrayAdapter<Choice>(getActivity(),
                android.R.layout.simple_spinner_item, choices);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mFileCompression.setAdapter(adapter);


        int defaultFileCompression = Preferences.getPreferences(getActivity()).getDefaultFileCompression();
        for (int i = 0; i < choices.length; ++i) {
            if (choices[i].getId() == defaultFileCompression) {
                mFileCompression.setSelection(i);
                break;
            }
        }
        */

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        addInputUris(getArguments().<Uri>getParcelableArrayList(ARG_URIS));
    }

    private void addInputUris(List<Uri> uris) {
        if (uris != null) {
            for (Uri uri : uris) {
                addInputUri(uri);
            }
        }
    }

    private void addInputUri(Uri inputUri) {
        if (inputUri == null) {
            return;
        }

        mEncryptInterface.getInputUris().add(inputUri);
        mEncryptInterface.notifyUpdate();

        /**
         * We hide the encrypt to file button if multiple files are selected.
         *
         * With Android L it will be possible to select a target directory for multiple files, so we might want to
         * change this later
         */

        if (mEncryptInterface.getInputUris().size() > 1) {
            mEncryptFile.setVisibility(View.GONE);
        } else {
            mEncryptFile.setVisibility(View.VISIBLE);
        }
    }

    private void delInputUri(int position) {
        mEncryptInterface.getInputUris().remove(position);
        mEncryptInterface.notifyUpdate();

        if (mEncryptInterface.getInputUris().size() > 1) {
            mEncryptFile.setVisibility(View.GONE);
        } else {
            mEncryptFile.setVisibility(View.VISIBLE);
        }
    }

    private void showOutputFileDialog() {
        if (mEncryptInterface.getInputUris().size() > 1 || mEncryptInterface.getInputUris().isEmpty()) {
            throw new IllegalStateException();
        }
        Uri inputUri = mEncryptInterface.getInputUris().get(0);
        if (!Constants.KITKAT) {
            File file = new File(inputUri.getPath());
            File parentDir = file.exists() ? file.getParentFile() : Constants.Path.APP_DIR;
            String targetName = FileHelper.getFilename(getActivity(), inputUri) +
                    (mEncryptInterface.isUseArmor() ? ".asc" : ".gpg");
            File targetFile = new File(parentDir, targetName);
            FileHelper.saveFile(this, getString(R.string.title_encrypt_to_file),
                    getString(R.string.specify_file_to_encrypt_to), targetFile, REQUEST_CODE_OUTPUT);
        } else {
            FileHelper.saveDocument(this, "*/*", FileHelper.getFilename(getActivity(), inputUri) +
                    (mEncryptInterface.isUseArmor() ? ".asc" : ".gpg"), REQUEST_CODE_OUTPUT);
        }
    }

    private void encryptClicked(boolean share) {
        if (share) {
            mEncryptInterface.getOutputUris().clear();
            for (Uri uri : mEncryptInterface.getInputUris()) {
                String targetName = FileHelper.getFilename(getActivity(), uri) +
                        (mEncryptInterface.isUseArmor() ? ".asc" : ".gpg");
                mEncryptInterface.getOutputUris().add(TemporaryStorageProvider.createFile(getActivity(), targetName));
            }
            mEncryptInterface.startEncrypt(share);
        } else if (mEncryptInterface.getInputUris().size() == 1) {
            showOutputFileDialog();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_INPUT: {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    addInputUri(data.getData());
                }
                return;
            }
            case REQUEST_CODE_OUTPUT: {
                // This happens after output file was selected, so start our operation
                if (resultCode == Activity.RESULT_OK && data != null) {
                    mEncryptInterface.getOutputUris().add(data.getData());
                    mEncryptInterface.startEncrypt(false);
                }
                return;
            }

            default: {
                super.onActivityResult(requestCode, resultCode, data);

                break;
            }
        }
    }

    @Override
    public void onNotifyUpdate() {
        mAdapter.notifyDataSetChanged();
    }

    private class SelectedFilesAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return mEncryptInterface.getInputUris().size();
        }

        @Override
        public Object getItem(int position) {
            return mEncryptInterface.getInputUris().get(position);
        }

        @Override
        public long getItemId(int position) {
            return getItem(position).hashCode();
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            View view;
            if (convertView == null) {
                view = getActivity().getLayoutInflater().inflate(R.layout.file_list_entry, null);
            } else {
                view = convertView;
            }
            ((TextView) view.findViewById(R.id.filename)).setText(FileHelper.getFilename(getActivity(), mEncryptInterface.getInputUris().get(position)));
            long size = FileHelper.getFileSize(getActivity(), mEncryptInterface.getInputUris().get(position));
            if (size == -1) {
                ((TextView) view.findViewById(R.id.filesize)).setText("");
            } else {
                ((TextView) view.findViewById(R.id.filesize)).setText(FileHelper.readableFileSize(size));
            }
            view.findViewById(R.id.action_remove_file_from_list).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    delInputUri(position);
                }
            });
            int px = OtherHelper.dpToPx(getActivity(), 48);
            Bitmap bitmap = FileHelper.getThumbnail(getActivity(), mEncryptInterface.getInputUris().get(position), new Point(px, px));
            if (bitmap != null) {
                ((ImageView) view.findViewById(R.id.thumbnail)).setImageBitmap(bitmap);
            } else {
                ((ImageView) view.findViewById(R.id.thumbnail)).setImageResource(R.drawable.ic_doc_generic_am);
            }
            return view;
        }
    }
}
