/*
 * Copyright (C) 2017 Schürmann & Breitmoser GbR
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

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.dialog.AddEditSmartPGPAuthorityDialogFragment;
import org.sufficientlysecure.keychain.ui.util.Notify;
import org.sufficientlysecure.keychain.ui.util.recyclerview.ItemTouchHelperAdapter;
import org.sufficientlysecure.keychain.ui.util.recyclerview.ItemTouchHelperDragCallback;
import org.sufficientlysecure.keychain.ui.util.recyclerview.ItemTouchHelperViewHolder;
import org.sufficientlysecure.keychain.ui.util.recyclerview.RecyclerItemClickListener;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;


public class SettingsSmartPGPAuthorityFragment extends Fragment implements RecyclerItemClickListener.OnItemClickListener {

    private ItemTouchHelper mItemTouchHelper;

    private ArrayList<String> mAuthorities;
    private AuthorityListAdapter mAdapter;

    public static SettingsSmartPGPAuthorityFragment newInstance(String[] authorities) {
        return new SettingsSmartPGPAuthorityFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {

        return inflater.inflate(R.layout.settings_smartpgp_authority_fragment, null);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        List<String> authorities = new LinkedList();

        try {
            final KeyStore ks = SettingsSmartPGPAuthoritiesActivity.readKeystore(getActivity());
            final Enumeration<String> it = ks.aliases();

            while (it.hasMoreElements()) {
                authorities.add(it.nextElement());
            }

        } catch (Exception e) {
        }

        mAuthorities = new ArrayList<>(authorities);
        mAdapter = new AuthorityListAdapter(mAuthorities);

        RecyclerView recyclerView = view.findViewById(R.id.smartpgp_authority_recycler_view);
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
        inflater.inflate(R.menu.smartpgp_authority_pref_menu, menu);

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.menu_add_smartpgp_authority:
                startAddAuthorityDialog();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void startAddAuthorityDialog() {
        startEditAuthorityDialog(AddEditSmartPGPAuthorityDialogFragment.Action.ADD, null, null, -1);
    }

    private void startEditAuthorityDialog(AddEditSmartPGPAuthorityDialogFragment.Action action,
                                          final String old_alias, final Uri uri, final int position) {
        Handler returnHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                Bundle data = message.getData();
                final String new_alias = data.getString(AddEditSmartPGPAuthorityDialogFragment.OUT_ALIAS);
                final int position = data.getInt(AddEditSmartPGPAuthorityDialogFragment.OUT_POSITION);
                final String uri = data.getString(AddEditSmartPGPAuthorityDialogFragment.OUT_URI);

                final AddEditSmartPGPAuthorityDialogFragment.Action action =
                        (AddEditSmartPGPAuthorityDialogFragment.Action)
                                data.getSerializable(AddEditSmartPGPAuthorityDialogFragment.OUT_ACTION);

                switch(action) {
                    case ADD:
                        if (editAuthority(old_alias, new_alias, position, uri)) {
                            Notify.create(getActivity(), "Authority " + new_alias + " added",
                                    Notify.LENGTH_SHORT, Notify.Style.OK).show();
                        }
                        break;

                    case EDIT:
                        if (editAuthority(old_alias, new_alias, position, uri)){
                            Notify.create(getActivity(), "Authority " + old_alias + " modified",
                                    Notify.LENGTH_SHORT, Notify.Style.OK).show();
                        }
                        break;

                    case DELETE:
                        if (deleteAuthority(position)) {
                            Notify.create(getActivity(), "Authority " + old_alias + " deleted",
                                    Notify.LENGTH_SHORT, Notify.Style.OK).show();
                        }
                        break;

                }
            }
        };

        // Create a new Messenger for the communication back
        Messenger messenger = new Messenger(returnHandler);
        AddEditSmartPGPAuthorityDialogFragment dialogFragment = AddEditSmartPGPAuthorityDialogFragment
                .newInstance(messenger, action, old_alias, uri, position);
        dialogFragment.show(getFragmentManager(), "addSmartPGPAuthorityDialog");
    }


    private boolean editAuthority(final String old_alias, final String new_alias, final int position, final String uri) {
        try {
            final KeyStore ks = SettingsSmartPGPAuthoritiesActivity.readKeystore(getContext());

            if (ks == null) {
                throw new KeyStoreException("no keystore found");
            }

            Certificate old_cert = null;
            if (old_alias != null) {
                old_cert = ks.getCertificate(old_alias);
                ks.deleteEntry(old_alias);
                mAuthorities.remove(old_alias);
                mAdapter.notifyItemRemoved(position);
            }

            Certificate new_cert = null;
            if (uri == null) {
                new_cert = old_cert;
            } else {
                final InputStream fis = getContext().getContentResolver().openInputStream(Uri.parse(uri));

                final CertificateFactory cf = CertificateFactory.getInstance("X.509");
                new_cert = cf.generateCertificate(fis);
                if (!(new_cert instanceof X509Certificate)) {
                    Notify.create(getActivity(), "Invalid certificate", Notify.LENGTH_LONG, Notify.Style.ERROR).show();
                    return false;
                }

                fis.close();
            }

            if (new_alias == null || new_cert == null) {
                Notify.create(getActivity(), "Missing alias or certificate", Notify.LENGTH_LONG, Notify.Style.ERROR).show();
                return false;
            }

            final X509Certificate x509cert = (X509Certificate)new_cert;

            x509cert.checkValidity();

            ks.setCertificateEntry(new_alias, x509cert);

            SettingsSmartPGPAuthoritiesActivity.writeKeystore(getContext(), ks);

            mAuthorities.add(new_alias);
            mAdapter.notifyItemInserted(mAuthorities.size() - 1);

            return true;

        } catch (IOException e) {
            Notify.create(getActivity(), "failed to open certificate (" + e.getMessage() + ")", Notify.LENGTH_LONG, Notify.Style.ERROR).show();
        } catch (CertificateException e) {
            Notify.create(getActivity(), "invalid certificate (" + e.getMessage() + ")", Notify.LENGTH_LONG, Notify.Style.ERROR).show();
        } catch (KeyStoreException e) {
            Notify.create(getActivity(), "invalid keystore (" + e.getMessage() + ")", Notify.LENGTH_LONG, Notify.Style.ERROR).show();
        }

        return false;
    }

    private boolean deleteAuthority(final int position) {
        try {
            final KeyStore ks = SettingsSmartPGPAuthoritiesActivity.readKeystore(getContext());

            if (ks == null) {
                return false;
            }

            ks.deleteEntry(mAuthorities.get(position));

            SettingsSmartPGPAuthoritiesActivity.writeKeystore(getContext(), ks);

            mAuthorities.remove(mAuthorities.get(position));
            mAdapter.notifyItemRemoved(position);

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void onItemClick(View view, int position) {
        startEditAuthorityDialog(AddEditSmartPGPAuthorityDialogFragment.Action.EDIT,
                                mAuthorities.get(position), null, position);
    }


    public class AuthorityListAdapter extends RecyclerView.Adapter<AuthorityListAdapter.ViewHolder>
            implements ItemTouchHelperAdapter {

        private final List<String> mAuthorities;

        public AuthorityListAdapter(List<String> authorities) {
            mAuthorities = authorities;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.settings_smartpgp_authority_item, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            holder.authorityName.setText(mAuthorities.get(position));
        }

        @Override
        public void onItemMove(RecyclerView.ViewHolder source, RecyclerView.ViewHolder target,
                               int fromPosition, int toPosition) {
            Collections.swap(mAuthorities, fromPosition, toPosition);
            notifyItemMoved(fromPosition, toPosition);
        }

        @Override
        public int getItemCount() {
            return mAuthorities.size();
        }


        public class ViewHolder extends RecyclerView.ViewHolder implements
                ItemTouchHelperViewHolder {

            public final ViewGroup outerLayout;
            public final TextView authorityName;

            public ViewHolder(View itemView) {
                super(itemView);
                outerLayout = itemView.findViewById(R.id.outer_layout);
                authorityName = itemView.findViewById(R.id.smartpgp_authority_tv);
                itemView.setClickable(true);
            }

            @Override
            public void onItemSelected() {
            }

            @Override
            public void onItemClear() {
            }
        }
    }
}
