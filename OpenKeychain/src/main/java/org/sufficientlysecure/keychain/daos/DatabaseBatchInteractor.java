package org.sufficientlysecure.keychain.daos;


import java.util.List;

import com.squareup.sqldelight.TransactionWithoutReturn;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import org.sufficientlysecure.keychain.Certs;
import org.sufficientlysecure.keychain.Database;
import org.sufficientlysecure.keychain.Key_signatures;
import org.sufficientlysecure.keychain.Keyrings_public;
import org.sufficientlysecure.keychain.Keys;
import org.sufficientlysecure.keychain.User_packets;


public class DatabaseBatchInteractor {
    private final Database db;

    DatabaseBatchInteractor(Database db) {
        this.db = db;

    }

    public void applyBatch(List<BatchOp> operations) {
        db.transaction(true,
                (Function1<TransactionWithoutReturn, Unit>) transactionWithoutReturn -> {
                    for (BatchOp op : operations) {
                        if (op.keyRingPublic != null) {
                            db.getKeyRingsPublicQueries().insertKeyRingPublic(op.keyRingPublic);
                        } else if (op.subKey != null) {
                            db.getKeysQueries().insertKey(op.subKey);
                        } else if (op.userPacket != null) {
                            db.getUserPacketsQueries().insertUserPacket(op.userPacket);
                        } else if (op.certification != null) {
                            db.getCertsQueries().insertCert(op.certification);
                        } else if (op.keySignature != null) {
                            db.getKeySignaturesQueries().insertKeySignature(op.keySignature);
                        } else {
                            throw new IllegalStateException();
                        }
                    }
                    return Unit.INSTANCE;
                });
    }

    public static BatchOp createInsertKeyRingPublic(Keyrings_public keyRingPublic) {
        return new BatchOp(keyRingPublic, null, null, null, null);
    }

    static BatchOp createInsertSubKey(Keys subKey) {
        return new BatchOp(null, subKey, null, null, null);
    }

    public static BatchOp createInsertUserPacket(User_packets userPacket) {
        return new BatchOp(null, null, userPacket, null, null);
    }

    public static BatchOp createInsertCertification(Certs certification) {
        return new BatchOp(null, null, null, certification, null);
    }

    static BatchOp createInsertSignerKey(Key_signatures keySignature) {
        return new BatchOp(null, null, null, null, keySignature);
    }

    static class BatchOp {
        final Keyrings_public keyRingPublic;
        final Keys subKey;
        final User_packets userPacket;
        final Certs certification;
        final Key_signatures keySignature;

        BatchOp(Keyrings_public keyRingPublic, Keys subKey, User_packets userPacket,
                Certs certification, Key_signatures keySignature) {
            this.subKey = subKey;
            this.keyRingPublic = keyRingPublic;
            this.userPacket = userPacket;
            this.certification = certification;
            this.keySignature = keySignature;
        }
    }
}
