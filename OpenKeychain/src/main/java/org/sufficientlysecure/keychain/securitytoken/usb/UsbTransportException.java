/*
 * Copyright (C) 2016 Nikita Mikhailov <nikita.s.mikhailov@gmail.com>
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

package org.sufficientlysecure.keychain.securitytoken.usb;

import java.io.IOException;

public class UsbTransportException extends IOException {
    public UsbTransportException() {
    }

    public UsbTransportException(final String detailMessage) {
        super(detailMessage);
    }

    public UsbTransportException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public UsbTransportException(final Throwable cause) {
        super(cause);
    }
}
