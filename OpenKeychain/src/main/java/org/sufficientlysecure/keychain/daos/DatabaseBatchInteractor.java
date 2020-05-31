package org.sufficientlysecure.keychain.daos;


import java.util.List;

import androidx.sqlite.db.SupportSQLiteDatabase;

import org.sufficientlysecure.keychain.CertsModel.InsertCert;
import org.sufficientlysecure.keychain.KeyRingsPublicModel.InsertKeyRingPublic;
import org.sufficientlysecure.keychain.KeySignaturesModel.InsertKeySignature;
import org.sufficientlysecure.keychain.KeysModel.InsertKey;
import org.sufficientlysecure.keychain.UserPacketsModel.InsertUserPacket;
import org.sufficientlysecure.keychain.model.Certification;
import org.sufficientlysecure.keychain.model.KeyRingPublic;
import org.sufficientlysecure.keychain.model.KeySignature;
import org.sufficientlysecure.keychain.model.SubKey;
import org.sufficientlysecure.keychain.model.UserPacket;


public class DatabaseBatchInteractor {
    private final SupportSQLiteDatabase db;

    private final InsertKeyRingPublic insertKeyRingPublicStatement;
    private final InsertKey insertSubKeyStatement;
    private final InsertUserPacket insertUserPacketStatement;
    private final InsertCert insertCertificationStatement;
    private final InsertKeySignature insertKeySignerStatement;

    DatabaseBatchInteractor(SupportSQLiteDatabase db) {
        this.db = db;

        insertKeyRingPublicStatement = KeyRingPublic.createInsertStatement(db);
        insertSubKeyStatement = SubKey.createInsertStatement(db);
        insertUserPacketStatement = UserPacket.createInsertStatement(db);
        insertCertificationStatement = Certification.createInsertStatement(db);
        insertKeySignerStatement = KeySignature.createInsertStatement(db);
    }

    public SupportSQLiteDatabase getDb() {
        return db;
    }

    public void applyBatch(List<BatchOp> operations) {
        for (BatchOp op : operations) {
            if (op.keyRingPublic != null) {
                op.keyRingPublic.bindTo(insertKeyRingPublicStatement);
                insertKeyRingPublicStatement.executeInsert();
            } else if (op.subKey != null) {
                op.subKey.bindTo(insertSubKeyStatement);
                insertSubKeyStatement.executeInsert();
            } else if (op.userPacket != null) {
                op.userPacket.bindTo(insertUserPacketStatement);
                insertUserPacketStatement.executeInsert();
            } else if (op.certification != null) {
                op.certification.bindTo(insertCertificationStatement);
                insertCertificationStatement.executeInsert();
            } else if (op.keySignature != null) {
                op.keySignature.bindTo(insertKeySignerStatement);
                insertKeySignerStatement.executeInsert();
            } else {
                throw new IllegalStateException();
            }
        }
    }

    public static BatchOp createInsertKeyRingPublic(KeyRingPublic keyRingPublic) {
        return new BatchOp(keyRingPublic, null, null, null, null);
    }

    static BatchOp createInsertSubKey(SubKey subKey) {
        return new BatchOp(null, subKey, null, null, null);
    }

    public static BatchOp createInsertUserPacket(UserPacket userPacket) {
        return new BatchOp(null, null, userPacket, null, null);
    }

    public static BatchOp createInsertCertification(Certification certification) {
        return new BatchOp(null, null, null, certification, null);
    }

    static BatchOp createInsertSignerKey(KeySignature keySignature) {
        return new BatchOp(null, null, null, null, keySignature);
    }

    static class BatchOp {
        final KeyRingPublic keyRingPublic;
        final SubKey subKey;
        final UserPacket userPacket;
        final Certification certification;
        final KeySignature keySignature;

        BatchOp(KeyRingPublic keyRingPublic, SubKey subKey, UserPacket userPacket,
                Certification certification, KeySignature keySignature) {
            this.subKey = subKey;
            this.keyRingPublic = keyRingPublic;
            this.userPacket = userPacket;
            this.certification = certification;
            this.keySignature = keySignature;
        }
    }
}
