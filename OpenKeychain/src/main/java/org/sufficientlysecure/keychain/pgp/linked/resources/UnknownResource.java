package org.sufficientlysecure.keychain.pgp.linked.resources;

import org.sufficientlysecure.keychain.operations.results.OperationResult.OperationLog;
import org.sufficientlysecure.keychain.pgp.linked.LinkedResource;

import java.net.URI;
import java.util.HashMap;
import java.util.Set;

public class UnknownResource extends LinkedResource {

    public UnknownResource(Set<String> flags, HashMap<String,String> params, URI uri) {
        super(flags, params, uri);
    }

    @Override
    protected String fetchResource(OperationLog log, int indent) {
        return null;
    }

}
