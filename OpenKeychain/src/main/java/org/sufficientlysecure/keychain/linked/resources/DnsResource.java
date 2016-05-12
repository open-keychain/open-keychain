/*
 * Copyright (C) 2015-2016 Vincent Breitmoser <v.breitmoser@mugenguild.com>
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

package org.sufficientlysecure.keychain.linked.resources;

import android.content.Context;
import android.support.annotation.DrawableRes;
import android.support.annotation.StringRes;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.operations.results.OperationResult.OperationLog;
import org.sufficientlysecure.keychain.linked.LinkedTokenResource;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;

import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.measite.minidns.Client;
import de.measite.minidns.DNSMessage;
import de.measite.minidns.Question;
import de.measite.minidns.Record;
import de.measite.minidns.Record.CLASS;
import de.measite.minidns.Record.TYPE;
import de.measite.minidns.record.TXT;

public class DnsResource extends LinkedTokenResource {

    final static Pattern magicPattern =
            Pattern.compile("openpgpid\\+token=([a-zA-Z0-9]+)(?:#|;)([a-zA-Z0-9]+)");

    String mFqdn;
    CLASS mClass;
    TYPE mType;

    DnsResource(Set<String> flags, HashMap<String, String> params, URI uri,
                String fqdn, CLASS clazz, TYPE type) {
        super(flags, params, uri);

        mFqdn = fqdn;
        mClass = clazz;
        mType = type;
    }

    public static String generateText(byte[] fingerprint) {

        return String.format("openpgp4fpr=%s",
                KeyFormattingUtils.convertFingerprintToHex(fingerprint));

    }

    public static DnsResource createNew (String domain) {
        HashSet<String> flags = new HashSet<>();
        HashMap<String,String> params = new HashMap<>();
        URI uri = URI.create("dns:" + domain + "?TYPE=TXT");
        return create(flags, params, uri);
    }

    public static DnsResource create(Set<String> flags, HashMap<String,String> params, URI uri) {
        if ( ! ("dns".equals(uri.getScheme())
                && (flags == null || flags.isEmpty())
                && (params == null || params.isEmpty()))) {
            return null;
        }

        //
        String spec = uri.getSchemeSpecificPart();
        // If there are // at the beginning, this includes an authority - we don't support those!
        if (spec.startsWith("//")) {
            return null;
        }

        String[] pieces = spec.split("\\?", 2);
        // In either case, part before a ? is the fqdn
        String fqdn = pieces[0];
        // There may be a query part
        /*
        if (pieces.length > 1) {
            // parse CLASS and TYPE query paramters
        }
        */

        CLASS clazz = CLASS.IN;
        TYPE type = TYPE.TXT;

        return new DnsResource(flags, params, uri, fqdn, clazz, type);
    }

    @SuppressWarnings("unused")
    public String getFqdn() {
        return mFqdn;
    }

    @Override
    protected String fetchResource (Context context, OperationLog log, int indent) {

        Client c = new Client();
        DNSMessage msg = c.query(new Question(mFqdn, mType, mClass));
        Record aw = msg.getAnswers()[0];
        TXT txt = (TXT) aw.getPayload();
        return txt.getText().toLowerCase();

    }

    @Override
    protected Matcher matchResource(OperationLog log, int indent, String res) {
        return magicPattern.matcher(res);
    }

    @Override
    public @StringRes
    int getVerifiedText(boolean isSecret) {
        return isSecret ? R.string.linked_verified_secret_dns : R.string.linked_verified_dns;
    }

    @Override
    public @DrawableRes int getDisplayIcon() {
        return R.drawable.linked_dns;
    }

    @Override
    public String getDisplayTitle(Context context) {
        return context.getString(R.string.linked_title_dns);
    }

    @Override
    public String getDisplayComment(Context context) {
        return mFqdn;
    }
}
