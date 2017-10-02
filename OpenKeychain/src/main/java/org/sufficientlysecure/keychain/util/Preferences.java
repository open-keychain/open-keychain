/*
 * Copyright (C) 2012-2014 Dominik Schürmann <dominik@dominikschuermann.de>
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

package org.sufficientlysecure.keychain.util;


import java.net.Proxy;
import java.util.ArrayList;
import java.util.ListIterator;

import android.accounts.Account;
import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.PreferenceManager;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.Constants.Pref;
import org.sufficientlysecure.keychain.KeychainApplication;
import org.sufficientlysecure.keychain.keyimport.HkpKeyserverAddress;
import org.sufficientlysecure.keychain.service.KeyserverSyncAdapterService;

/**
 * Singleton Implementation of a Preference Helper
 */
@SuppressLint("CommitPrefEdits")
public class Preferences {
    private static Preferences sPreferences;
    private SharedPreferences mSharedPreferences;

    private static String PREF_FILE_NAME = "APG.main";
    private static int PREF_FILE_MODE = Context.MODE_MULTI_PROCESS;

    public static synchronized Preferences getPreferences(Context context) {
        return getPreferences(context, false);
    }

    public static synchronized Preferences getPreferences(Context context, boolean forceNew) {
        if (sPreferences == null || forceNew) {
            sPreferences = new Preferences(context);
        } else {
            // to make it safe for multiple processes, call getSharedPreferences everytime
            sPreferences.updateSharedPreferences(context);
        }
        return sPreferences;
    }

    private Preferences(Context context) {
        updateSharedPreferences(context);
    }

    /**
     * Makes android's preference framework write to our file instead of default.
     * This allows us to use the xml "persistent" attribute to simplify code, which automatically
     * writes and reads preference values.
     */
    public static void setPreferenceManagerFileAndMode(PreferenceManager manager) {
        manager.setSharedPreferencesName(PREF_FILE_NAME);
        manager.setSharedPreferencesMode(PREF_FILE_MODE);
    }

    public void updateSharedPreferences(Context context) {
        // multi-process safe preferences
        mSharedPreferences = context.getSharedPreferences(PREF_FILE_NAME, PREF_FILE_MODE);
    }

    public String getLanguage() {
        return mSharedPreferences.getString(Constants.Pref.LANGUAGE, "");
    }

    public void setLanguage(String value) {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putString(Constants.Pref.LANGUAGE, value);
        editor.commit();
    }

    public boolean getPassphraseCacheSubs() {
        return mSharedPreferences.getBoolean(Pref.PASSPHRASE_CACHE_SUBS, false);
    }

    public int getCacheTtlSeconds() {
        return mSharedPreferences.getInt(Pref.PASSPHRASE_CACHE_LAST_TTL, Integer.MAX_VALUE);
    }

    public void setCacheTtlSeconds(int ttlSeconds) {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putInt(Pref.PASSPHRASE_CACHE_LAST_TTL, ttlSeconds);
        editor.commit();
    }

    public boolean getCachedConsolidate() {
        return mSharedPreferences.getBoolean(Pref.CACHED_CONSOLIDATE, false);
    }

    public void setCachedConsolidate(boolean value) {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putBoolean(Pref.CACHED_CONSOLIDATE, value);
        editor.commit();
    }

    public boolean isFirstTime() {
        return mSharedPreferences.getBoolean(Constants.Pref.FIRST_TIME, true);
    }

    public boolean useNumKeypadForSecurityTokenPin() {
        return mSharedPreferences.getBoolean(Pref.USE_NUMKEYPAD_FOR_SECURITY_TOKEN_PIN, true);
    }

    public void setFirstTime(boolean value) {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putBoolean(Constants.Pref.FIRST_TIME, value);
        editor.commit();
    }

    public ArrayList<HkpKeyserverAddress> getKeyServers() {
        String rawData = mSharedPreferences.getString(Constants.Pref.KEY_SERVERS,
                Constants.Defaults.KEY_SERVERS);
        if ("".equals(rawData)) {
            return new ArrayList<>();
        }
        ArrayList<HkpKeyserverAddress> servers = new ArrayList<>();
        String[] entries = rawData.split(",");
        for (String entry : entries) {
            String[] addresses = entry.trim().split(";");
            String url = addresses[0];
            String onion = addresses.length == 1 ? null : addresses[1];

            if (url.isEmpty()) {
                continue;
            }

            servers.add(HkpKeyserverAddress.createWithOnionProxy(url, onion));
        }
        return servers;
    }

    public HkpKeyserverAddress getPreferredKeyserver() {
        ArrayList<HkpKeyserverAddress> keyservers = getKeyServers();
        return keyservers.size() == 0 ? null : keyservers.get(0);
    }

    public void setKeyServers(ArrayList<HkpKeyserverAddress> keyservers) {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        String rawData = "";
        for (HkpKeyserverAddress server : keyservers) {
            if (server.getUrl().isEmpty()) {
                continue;
            }
            rawData += server.getUrl();

            if (server.getOnion() != null && !server.getOnion().isEmpty()) {
                rawData += ";" + server.getOnion();
            }

            if (!"".equals(rawData)) {
                rawData += ",";
            }
        }
        editor.putString(Constants.Pref.KEY_SERVERS, rawData);
        editor.commit();
    }

    public void setFilesUseCompression(boolean compress) {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putBoolean(Pref.FILE_USE_COMPRESSION, compress);
        editor.commit();
    }

    public boolean getFilesUseCompression() {
        return mSharedPreferences.getBoolean(Pref.FILE_USE_COMPRESSION, true);
    }

    public void setTextUseCompression(boolean compress) {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putBoolean(Pref.TEXT_USE_COMPRESSION, compress);
        editor.commit();
    }

    public boolean getTextUseCompression() {
        return mSharedPreferences.getBoolean(Pref.TEXT_USE_COMPRESSION, true);
    }

    public String getTheme() {
        return mSharedPreferences.getString(Pref.THEME, Pref.Theme.LIGHT);
    }

    public void setTheme(String value) {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putString(Constants.Pref.THEME, value);
        editor.commit();
    }

    public void setUseArmor(boolean useArmor) {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putBoolean(Pref.USE_ARMOR, useArmor);
        editor.commit();
    }

    public void setUseNumKeypadForSecurityTokenPin(boolean useNumKeypad) {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putBoolean(Pref.USE_NUMKEYPAD_FOR_SECURITY_TOKEN_PIN, useNumKeypad);
        editor.commit();
    }

    public boolean getUseArmor() {
        return mSharedPreferences.getBoolean(Pref.USE_ARMOR, false);
    }

    public void setEncryptFilenames(boolean encryptFilenames) {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putBoolean(Pref.ENCRYPT_FILENAMES, encryptFilenames);
        editor.commit();
    }

    public boolean getEncryptFilenames() {
        return mSharedPreferences.getBoolean(Pref.ENCRYPT_FILENAMES, true);
    }

    // proxy preference functions start here

    public boolean getUseNormalProxy() {
        return mSharedPreferences.getBoolean(Constants.Pref.USE_NORMAL_PROXY, false);
    }

    public boolean getUseTorProxy() {
        return mSharedPreferences.getBoolean(Constants.Pref.USE_TOR_PROXY, false);
    }

    public String getProxyHost() {
        return mSharedPreferences.getString(Constants.Pref.PROXY_HOST, null);
    }

    /**
     * we store port as String for easy interfacing with EditTextPreference, but return it as an integer
     *
     * @return port number of proxy
     */
    public int getProxyPort() {
        return Integer.parseInt(mSharedPreferences.getString(Pref.PROXY_PORT, "-1"));
    }

    Proxy.Type getProxyType() {
        final String typeHttp = Pref.ProxyType.TYPE_HTTP;
        final String typeSocks = Pref.ProxyType.TYPE_SOCKS;

        String type = mSharedPreferences.getString(Pref.PROXY_TYPE, typeHttp);

        switch (type) {
            case typeHttp:
                return Proxy.Type.HTTP;
            case typeSocks:
                return Proxy.Type.SOCKS;
            default:  // shouldn't happen
                Log.e(Constants.TAG, "Invalid Proxy Type in preferences");
                return null;
        }
    }

    public ParcelableProxy getParcelableProxy() {
        boolean useTor = getUseTorProxy();
        boolean useNormalProxy = getUseNormalProxy();

        if (useTor) {
            //TODO: Replace Constants.Orbot.PROXY_PORT with OrbotStatusReceiver.getProxyPortHttp()
            //TODO: in order to always have the actual port Orbot is offering?

            return new ParcelableProxy(Constants.Orbot.PROXY_HOST, Constants.Orbot.PROXY_PORT,
                    Constants.Orbot.PROXY_TYPE, ParcelableProxy.PROXY_MODE_TOR);
        } else if (useNormalProxy) {
            return new ParcelableProxy(getProxyHost(), getProxyPort(), getProxyType(),
                    ParcelableProxy.PROXY_MODE_NORMAL);
        } else {
            return new ParcelableProxy(null, -1, null, ParcelableProxy.PROXY_MODE_NORMAL);
        }
    }

    /**
     * @return true if a periodic sync exists and is set to run automatically, false otherwise
     */
    public static boolean getKeyserverSyncEnabled(Context context) {
        Account account = KeychainApplication.createAccountIfNecessary(context);

        if (account == null) {
            // if the account could not be created for some reason, we can't have a sync
            return false;
        }

        String authority = Constants.PROVIDER_AUTHORITY;

        return ContentResolver.getSyncAutomatically(account, authority) &&
                !ContentResolver.getPeriodicSyncs(account, authority).isEmpty();
    }

    // cloud prefs

    public CloudSearchPrefs getCloudSearchPrefs() {
        return new CloudSearchPrefs(mSharedPreferences.getBoolean(Pref.SEARCH_KEYSERVER, true),
                mSharedPreferences.getBoolean(Pref.SEARCH_KEYBASE, true),
                false,
                getPreferredKeyserver());
    }

    public boolean isKeySignaturesTableInitialized() {
        return mSharedPreferences.getBoolean(Pref.KEY_SIGNATURES_TABLE_INITIALIZED, false);
    }

    public void setKeySignaturesTableInitialized() {
        Editor editor = mSharedPreferences.edit();
        editor.putBoolean(Pref.KEY_SIGNATURES_TABLE_INITIALIZED, true);
        editor.commit();
    }

    public static class CloudSearchPrefs implements Parcelable {
        public final boolean searchKeyserver;
        public final boolean searchKeybase;
        public final boolean searchFacebook;
        public final HkpKeyserverAddress keyserver;

        /**
         * @param searchKeyserver should passed keyserver be searched
         * @param searchKeybase   should keybase.io be searched
         * @param keyserver       the keyserver url authority to search on
         */
        public CloudSearchPrefs(boolean searchKeyserver, boolean searchKeybase,
                                boolean searchFacebook, HkpKeyserverAddress keyserver) {
            this.searchKeyserver = searchKeyserver;
            this.searchKeybase = searchKeybase;
            this.searchFacebook = searchFacebook;
            this.keyserver = keyserver;
        }

        protected CloudSearchPrefs(Parcel in) {
            searchKeyserver = in.readByte() != 0x00;
            searchKeybase = in.readByte() != 0x00;
            searchFacebook = in.readByte() != 0x00;
            keyserver = in.readParcelable(HkpKeyserverAddress.class.getClassLoader());
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeByte((byte) (searchKeyserver ? 0x01 : 0x00));
            dest.writeByte((byte) (searchKeybase ? 0x01 : 0x00));
            dest.writeByte((byte) (searchFacebook ? 0x01 : 0x00));
            dest.writeParcelable(keyserver, flags);
        }

        public static final Parcelable.Creator<CloudSearchPrefs> CREATOR
                = new Parcelable.Creator<CloudSearchPrefs>() {
            @Override
            public CloudSearchPrefs createFromParcel(Parcel in) {
                return new CloudSearchPrefs(in);
            }

            @Override
            public CloudSearchPrefs[] newArray(int size) {
                return new CloudSearchPrefs[size];
            }
        };
    }

    // sync preferences

    public boolean getWifiOnlySync() {
        return mSharedPreferences.getBoolean(Pref.ENABLE_WIFI_SYNC_ONLY, true);
    }

    // experimental prefs

    public boolean getExperimentalEnableWordConfirm() {
        return mSharedPreferences.getBoolean(Pref.EXPERIMENTAL_ENABLE_WORD_CONFIRM, false);
    }

    public boolean getExperimentalEnableLinkedIdentities() {
        return mSharedPreferences.getBoolean(Pref.EXPERIMENTAL_ENABLE_LINKED_IDENTITIES, false);
    }

    public boolean getExperimentalEnableKeybase() {
        return mSharedPreferences.getBoolean(Pref.EXPERIMENTAL_ENABLE_KEYBASE, false);
    }

    public boolean getExperimentalSmartPGPAuthoritiesEnable() {
        return mSharedPreferences.getBoolean(Pref.EXPERIMENTAL_SMARTPGP_VERIFY_AUTHORITY, false);
    }

    public void upgradePreferences(Context context) {
        Log.d(Constants.TAG, "Upgrading preferences…");
        int oldVersion = mSharedPreferences.getInt(Constants.Pref.PREF_VERSION, 0);
        boolean requiresUpgrade = oldVersion < Constants.Defaults.PREF_CURRENT_VERSION;

        if (requiresUpgrade) {
            switch (oldVersion) {
                case 1:
                    // fall through
                case 2:
                    // fall through
                case 3: {
                    // migrate keyserver to hkps
                    ArrayList<HkpKeyserverAddress> servers = getKeyServers();
                    ListIterator<HkpKeyserverAddress> it = servers.listIterator();
                    while (it.hasNext()) {
                        HkpKeyserverAddress server = it.next();
                        if (server == null) {
                            continue;
                        }
                        switch (server.getUrl()) {
                            case "pool.sks-keyservers.net": {
                                // use HKPS!
                                it.set(HkpKeyserverAddress.createFromUri("hkps://hkps.pool.sks-keyservers.net"));
                                break;
                            }
                            case "pgp.mit.edu": {
                                // use HKPS!
                                it.set(HkpKeyserverAddress.createFromUri("hkps://pgp.mit.edu"));
                                break;
                            }
                            case "subkeys.pgp.net": {
                                // remove, because often down and no HKPS!
                                it.remove();
                                break;
                            }
                        }

                    }
                    setKeyServers(servers);
                }
                // fall through
                case 4: {
                    setTheme(Constants.Pref.Theme.DEFAULT);
                }
                // fall through
                case 5: {
                    KeyserverSyncAdapterService.enableKeyserverSync(context);
                }
                // fall through
                case 6: {
                }
                // fall through
                case 7: {
                    // add onion address to sks-keyservers.net
                    ArrayList<HkpKeyserverAddress> servers = getKeyServers();
                    ListIterator<HkpKeyserverAddress> it = servers.listIterator();
                    while (it.hasNext()) {
                        HkpKeyserverAddress server = it.next();
                        if (server == null) {
                            continue;
                        }
                        if ("hkps://hkps.pool.sks-keyservers.net".equals(server.getUrl())) {
                            it.set(HkpKeyserverAddress.createWithOnionProxy(
                                    "hkps://hkps.pool.sks-keyservers.net",
                                    "hkp://jirk5u4osbsr34t5.onion"));
                        }

                    }
                    setKeyServers(servers);
                }
            }

            // write new preference version
            mSharedPreferences.edit()
                    .putInt(Constants.Pref.PREF_VERSION, Constants.Defaults.PREF_CURRENT_VERSION)
                    .commit();
        }
    }

    public void clear() {
        mSharedPreferences.edit().clear().commit();
    }

}
