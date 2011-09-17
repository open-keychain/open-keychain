package org.thialfihar.android.apg;

public class CachedPassPhrase {
    public final long timestamp;
    public final String passPhrase;

    public CachedPassPhrase(long timestamp, String passPhrase) {
        super();
        this.timestamp = timestamp;
        this.passPhrase = passPhrase;
    }

    @Override
	public int hashCode() {
        int hc1 = (int)(this.timestamp & 0xffffffff);
        int hc2 = (this.passPhrase == null ? 0 : this.passPhrase.hashCode());
        return (hc1 + hc2) * hc2 + hc1;
    }

    @Override
	public boolean equals(Object other) {
        if (!(other instanceof CachedPassPhrase)) {
            return false;
        }

        CachedPassPhrase o = (CachedPassPhrase) other;
        if (timestamp != o.timestamp) {
            return false;
        }

        if (passPhrase != o.passPhrase) {
            if (passPhrase == null || o.passPhrase == null) {
                return false;
            }

            if (!passPhrase.equals(o.passPhrase)) {
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
