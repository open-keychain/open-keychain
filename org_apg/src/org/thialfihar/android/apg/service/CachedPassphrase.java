/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.thialfihar.android.apg.service;

public class CachedPassphrase {
    private final long timestamp;
    private final String passphrase;

    public CachedPassphrase(long timestamp, String passPhrase) {
        super();
        this.timestamp = timestamp;
        this.passphrase = passPhrase;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getPassphrase() {
        return passphrase;
    }

    @Override
    public int hashCode() {
        int hc1 = (int) (this.timestamp & 0xffffffff);
        int hc2 = (this.passphrase == null ? 0 : this.passphrase.hashCode());
        return (hc1 + hc2) * hc2 + hc1;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof CachedPassphrase)) {
            return false;
        }

        CachedPassphrase o = (CachedPassphrase) other;
        if (timestamp != o.timestamp) {
            return false;
        }

        if (passphrase != o.passphrase) {
            if (passphrase == null || o.passphrase == null) {
                return false;
            }

            if (!passphrase.equals(o.passphrase)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public String toString() {
        return "(" + timestamp + ", *******)";
    }
}
