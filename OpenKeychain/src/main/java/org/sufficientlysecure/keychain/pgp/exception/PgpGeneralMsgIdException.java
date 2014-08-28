/*
 * Copyright (C) 2012-2014 Dominik Schürmann <dominik@dominikschuermann.de>
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

package org.sufficientlysecure.keychain.pgp.exception;

import android.content.Context;

public class PgpGeneralMsgIdException extends Exception {
    static final long serialVersionUID = 0xf812773343L;

    private final int mMessageId;

    public PgpGeneralMsgIdException(int messageId) {
        super("msg[" + messageId + "]");
        mMessageId = messageId;
    }

    public PgpGeneralMsgIdException(int messageId, Throwable cause) {
        super("msg[" + messageId + "]", cause);
        mMessageId = messageId;
    }

    public PgpGeneralException getContextualized(Context context) {
        return new PgpGeneralException(context.getString(mMessageId), this);
    }
}
