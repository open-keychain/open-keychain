/*
 * Copyright (C) 2014 Tim Bray <tbray@textuality.com>
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

package org.sufficientlysecure.keychain.keyimport;

import com.textuality.keybase.lib.KeybaseException;
import com.textuality.keybase.lib.Match;
import com.textuality.keybase.lib.Search;
import com.textuality.keybase.lib.User;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.pgp.PgpKeyHelper;
import org.sufficientlysecure.keychain.util.Log;

import java.util.ArrayList;
import java.util.List;

public class KeybaseKeyserver extends Keyserver {
    public static final String ORIGIN = "keybase:keybase.io";
    private String mQuery;

    @Override
    public ArrayList<ImportKeysListEntry> search(String query) throws QueryFailedException,
            QueryNeedsRepairException {
        ArrayList<ImportKeysListEntry> results = new ArrayList<ImportKeysListEntry>();

        if (query.startsWith("0x")) {
            // cut off "0x" if a user is searching for a key id
            query = query.substring(2);
        }
        mQuery = query;

        try {
            Iterable<Match> matches = Search.search(query);
            for (Match match : matches) {
                results.add(makeEntry(match));
            }
        } catch (KeybaseException e) {
            Log.e(Constants.TAG, "keybase result parsing error", e);
            throw new QueryFailedException("Unexpected structure in keybase search result: " + e.getMessage());
        }

        return results;
    }

    private ImportKeysListEntry makeEntry(Match match) throws KeybaseException {
        final ImportKeysListEntry entry = new ImportKeysListEntry();
        entry.setQuery(mQuery);
        entry.setOrigin(ORIGIN);

        String username = null;
        username = match.getUsername();
        String fullName = match.getFullName();
        String fingerprint = match.getFingerprint();
        entry.setFingerprintHex(fingerprint);

        entry.setKeyIdHex("0x" + match.getKeyID());
        // store extra info, so we can query for the keybase id directly
        entry.setExtraData(username);

        final int algorithmId = match.getAlgorithmId();
        entry.setAlgorithm(PgpKeyHelper.getAlgorithmInfo(algorithmId));
        final int bitStrength = match.getBitStrength();
        entry.setBitStrength(bitStrength);

        ArrayList<String> userIds = new ArrayList<String>();
        String name = fullName + " <keybase.io/" + username + ">";
        userIds.add(name);

        List<String> proofLabels = match.getProofLabels();
        for (String proofLabel : proofLabels) {
            userIds.add(proofLabel);
        }
        entry.setUserIds(userIds);
        entry.setPrimaryUserId(name);
        return entry;
    }

    @Override
    public String get(String id) throws QueryFailedException {
        try {
            /*
            JSONObject user = getUser(id);
            return JWalk.getString(user, "them", "public_keys", "primary", "bundle");
            */
            return User.keyForUsername(id);
        } catch (KeybaseException e) {
            throw new QueryFailedException(e.getMessage());
        }
    }

    @Override
    public void add(String armoredKey) throws AddKeyException {
        throw new AddKeyException();
    }
}
