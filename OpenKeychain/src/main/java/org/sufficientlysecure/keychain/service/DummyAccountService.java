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

package org.sufficientlysecure.keychain.service;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.NetworkErrorException;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.widget.Toast;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.util.Log;

/**
 * This service actually does nothing, it's sole task is to show a Toast if the use tries to create an account.
 */
public class DummyAccountService extends Service {

    private class Toaster {
        private static final String TOAST_MESSAGE = "toast_message";
        private Context context;
        private Handler handler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                Toast.makeText(context, msg.getData().getString(TOAST_MESSAGE), Toast.LENGTH_LONG).show();
                return true;
            }
        });

        private Toaster(Context context) {
            this.context = context;
        }

        public void toast(int resourceId) {
            toast(context.getString(resourceId));
        }

        public void toast(String message) {
            Message msg = new Message();
            Bundle bundle = new Bundle();
            bundle.putString(TOAST_MESSAGE, message);
            msg.setData(bundle);
            handler.sendMessage(msg);
        }
    }

    private class Authenticator extends AbstractAccountAuthenticator {

        public Authenticator() {
            super(DummyAccountService.this);
        }

        @Override
        public Bundle editProperties(AccountAuthenticatorResponse response, String accountType) {
            Log.d(Constants.TAG, "DummyAccountService.editProperties");
            return null;
        }

        @Override
        public Bundle addAccount(AccountAuthenticatorResponse response, String accountType, String authTokenType,
                                 String[] requiredFeatures, Bundle options) throws NetworkErrorException {
            response.onResult(new Bundle());
            toaster.toast(R.string.info_no_manual_account_creation);
            Log.d(Constants.TAG, "DummyAccountService.addAccount");
            return null;
        }

        @Override
        public Bundle confirmCredentials(AccountAuthenticatorResponse response, Account account, Bundle options)
                throws NetworkErrorException {
            Log.d(Constants.TAG, "DummyAccountService.confirmCredentials");
            return null;
        }

        @Override
        public Bundle getAuthToken(AccountAuthenticatorResponse response, Account account, String authTokenType,
                                   Bundle options) throws NetworkErrorException {
            Log.d(Constants.TAG, "DummyAccountService.getAuthToken");
            return null;
        }

        @Override
        public String getAuthTokenLabel(String authTokenType) {
            Log.d(Constants.TAG, "DummyAccountService.getAuthTokenLabel");
            return null;
        }

        @Override
        public Bundle updateCredentials(AccountAuthenticatorResponse response, Account account, String authTokenType,
                                        Bundle options) throws NetworkErrorException {
            Log.d(Constants.TAG, "DummyAccountService.updateCredentials");
            return null;
        }

        @Override
        public Bundle hasFeatures(AccountAuthenticatorResponse response, Account account, String[] features)
                throws NetworkErrorException {
            Log.d(Constants.TAG, "DummyAccountService.hasFeatures");
            return null;
        }
    }

    private Toaster toaster;

    @Override
    public IBinder onBind(Intent intent) {
        toaster = new Toaster(this);
        return new Authenticator().getIBinder();
    }
}
