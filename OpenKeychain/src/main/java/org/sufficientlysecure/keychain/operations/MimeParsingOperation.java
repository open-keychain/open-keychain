/*
 * Copyright (C) 2015 Dominik Schürmann <dominik@dominikschuermann.de>
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

package org.sufficientlysecure.keychain.operations;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;

import org.apache.james.mime4j.dom.BinaryBody;
import org.apache.james.mime4j.dom.Body;
import org.apache.james.mime4j.dom.Entity;
import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.dom.MessageBuilder;
import org.apache.james.mime4j.dom.Multipart;
import org.apache.james.mime4j.dom.TextBody;
import org.apache.james.mime4j.dom.address.Mailbox;
import org.apache.james.mime4j.dom.address.MailboxList;
import org.apache.james.mime4j.dom.field.AddressListField;
import org.apache.james.mime4j.dom.field.ContentTypeField;
import org.apache.james.mime4j.dom.field.DateTimeField;
import org.apache.james.mime4j.dom.field.UnstructuredField;
import org.apache.james.mime4j.field.address.AddressFormatter;
import org.apache.james.mime4j.message.BodyPart;
import org.apache.james.mime4j.message.DefaultMessageBuilder;
import org.apache.james.mime4j.message.MessageImpl;
import org.apache.james.mime4j.stream.Field;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.operations.results.MimeParsingResult;
import org.sufficientlysecure.keychain.operations.results.OperationResult;
import org.sufficientlysecure.keychain.pgp.Progressable;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.provider.TemporaryStorageProvider;
import org.sufficientlysecure.keychain.service.MimeParsingParcel;
import org.sufficientlysecure.keychain.service.input.CryptoInputParcel;
import org.sufficientlysecure.keychain.util.Log;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;

public class MimeParsingOperation extends BaseOperation<MimeParsingParcel> {

    public ArrayList<Uri> mTempUris;

    public MimeParsingOperation(Context context, ProviderHelper providerHelper, Progressable progressable) {
        super(context, providerHelper, progressable);
    }

    @NonNull
    @Override
    public MimeParsingResult execute(MimeParsingParcel parcel,
                                     CryptoInputParcel cryptoInputParcel) {
        OperationResult.OperationLog log = new OperationResult.OperationLog();

        log.add(OperationResult.LogType.MSG_MIME_PARSING, 0);

        mTempUris = new ArrayList<>();

        try {
            InputStream in = mContext.getContentResolver().openInputStream(parcel.getInputUri());

            final MessageBuilder builder = new DefaultMessageBuilder();
            final Message message = builder.parseMessage(in);

            SimpleTreeNode root = createNode(message);

            traverseTree(root);

            log.add(OperationResult.LogType.MSG_MIME_PARSING_SUCCESS, 1);

        } catch (Exception e) {
            Log.e(Constants.TAG, "Mime parsing error", e);
            log.add(OperationResult.LogType.MSG_MIME_PARSING_ERROR, 1);
        }

        return new MimeParsingResult(MimeParsingResult.RESULT_OK, log,
                mTempUris);
    }

    private void traverseTree(SimpleTreeNode node) {
        if (node.isLeaf()) {
            parseAndSaveAsUris(node);
            return;
        }

        for (SimpleTreeNode child : node.children) {
            traverseTree(child);
        }
    }


    /**
     * Wraps an Object and associates it with a text. All message parts
     * (headers, bodies, multiparts, body parts) will be wrapped in
     * ObjectWrapper instances before they are added to the JTree instance.
     */
    public static class ObjectWrapper {
        private String text = "";
        private Object object = null;

        public ObjectWrapper(String text, Object object) {
            this.text = text;
            this.object = object;
        }

        @Override
        public String toString() {
            return text;
        }

        public Object getObject() {
            return object;
        }
    }

//    /**
//     * Create a node given a Multipart body.
//     * Add the Preamble, all Body parts and the Epilogue to the node.
//     *
//     * @return the root node of the tree.
//     */
//    private DefaultMutableTreeNode createNode(Header header) {
//        DefaultMutableTreeNode node = new DefaultMutableTreeNode(
//                new ObjectWrapper("Header", header));
//
//        for (Field field : header.getFields()) {
//            String name = field.getName();
//
//            node.add(new DefaultMutableTreeNode(new ObjectWrapper(name, field)));
//        }
//
//        return node;
//    }

    /**
     * Create a node given a Multipart body.
     * Add the Preamble, all Body parts and the Epilogue to the node.
     *
     * @param multipart the Multipart.
     * @return the root node of the tree.
     */
    private SimpleTreeNode createNode(Multipart multipart) {
        SimpleTreeNode node = new SimpleTreeNode(
                new ObjectWrapper("Multipart", multipart));

//        node.add(new DefaultMutableTreeNode(
//                new ObjectWrapper("Preamble", multipart.getPreamble())));
        for (Entity part : multipart.getBodyParts()) {
            node.add(createNode(part));
        }
//        node.add(new DefaultMutableTreeNode(
//                new ObjectWrapper("Epilogue", multipart.getEpilogue())));

        return node;
    }

    /**
     * Creates the tree nodes given a MIME entity (either a Message or
     * a BodyPart).
     *
     * @param entity the entity.
     * @return the root node of the tree displaying the specified entity and
     * its children.
     */
    private SimpleTreeNode createNode(Entity entity) {

        /*
         * Create the root node for the entity. It's either a
         * Message or a Body part.
         */
        String type = "Message";
        if (entity instanceof BodyPart) {
            type = "Body part";
        }
        SimpleTreeNode node = new SimpleTreeNode(
                new ObjectWrapper(type, entity));

        /*
         * Add the node encapsulating the entity Header.
         */
//        node.add(createNode(entity.getHeader()));

        Body body = entity.getBody();

        if (body instanceof Multipart) {
            /*
             * The body of the entity is a Multipart.
             */

            node.add(createNode((Multipart) body));
        } else if (body instanceof MessageImpl) {
            /*
             * The body is another Message.
             */

            node.add(createNode((MessageImpl) body));

        } else {
            /*
             * Discrete Body (either of type TextBody or BinaryBody).
             */
            type = "Text body";
            if (body instanceof BinaryBody) {
                type = "Binary body";
            }

            type += " (" + entity.getMimeType() + ")";
            node.add(new SimpleTreeNode(new ObjectWrapper(type, body)));

        }

        return node;
    }

    public void parseAndSaveAsUris(SimpleTreeNode node) {
        Object o = ((ObjectWrapper) node.getUserObject()).getObject();

        if (o instanceof TextBody) {
            /*
             * A text body. Display its contents.
             */
            TextBody body = (TextBody) o;
            StringBuilder sb = new StringBuilder();
            try {
                Reader r = body.getReader();
                int c;
                while ((c = r.read()) != -1) {
                    sb.append((char) c);
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            Log.d(Constants.TAG, "text: " + sb.toString());
//                textView.setText(sb.toString());

            Uri tempUri = null;
            try {
                tempUri = TemporaryStorageProvider.createFile(mContext, "text", "text/plain");
                OutputStream outStream = mContext.getContentResolver().openOutputStream(tempUri);
                body.writeTo(outStream);
                outStream.close();
            } catch (IOException e) {
                Log.e(Constants.TAG, "error mime parsing", e);
            }

            mTempUris.add(tempUri);

        } else if (o instanceof BinaryBody) {
            /*
             * A binary body. Display its MIME type and length in bytes.
             */
            BinaryBody body = (BinaryBody) o;
            int size = 0;
            try {
                InputStream is = body.getInputStream();
                while ((is.read()) != -1) {
                    size++;
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            Log.d(Constants.TAG, "Binary body\n"
                    + "MIME type: "
                    + body.getParent().getMimeType() + "\n"
                    + "Size of decoded data: " + size + " bytes");

        } else if (o instanceof ContentTypeField) {
            /*
             * Content-Type field.
             */
            ContentTypeField field = (ContentTypeField) o;
            StringBuilder sb = new StringBuilder();
            sb.append("MIME type: ").append(field.getMimeType()).append("\n");
            for (Map.Entry<String, String> entry : field.getParameters().entrySet()) {
                sb.append(entry.getKey()).append(" = ").append(entry.getValue()).append("\n");
            }
            Log.d(Constants.TAG, sb.toString());

        } else if (o instanceof AddressListField) {
            /*
             * An address field (From, To, Cc, etc)
             */
            AddressListField field = (AddressListField) o;
            MailboxList list = field.getAddressList().flatten();
            StringBuilder sb = new StringBuilder();
            for (Mailbox mailbox : list) {
                sb.append(AddressFormatter.DEFAULT.format(mailbox, false)).append("\n");
            }
            Log.d(Constants.TAG, sb.toString());

        } else if (o instanceof DateTimeField) {
            Date date = ((DateTimeField) o).getDate();
            Log.d(Constants.TAG, date.toString());
        } else if (o instanceof UnstructuredField) {
            Log.d(Constants.TAG, ((UnstructuredField) o).getValue());
        } else if (o instanceof Field) {
            Log.d(Constants.TAG, ((Field) o).getBody());
        } else {
            /*
             * The Object should be a Header or a String containing a
             * Preamble or Epilogue.
             */
            Log.d(Constants.TAG, o.toString());
        }
    }

    public class SimpleTreeNode {
        private SimpleTreeNode parent;
        private Object userObject;
        private ArrayList<SimpleTreeNode> children;

        protected SimpleTreeNode(Object userObject) {
            this.parent = null;
            this.userObject = userObject;
            this.children = new ArrayList<>();
        }

        protected Object getUserObject() {
            return userObject;
        }

        protected void setUserObject(Object userObject) {
            this.userObject = userObject;
        }

        public void add(SimpleTreeNode newChild) {
            newChild.parent = this;
            children.add(newChild);
        }

        public SimpleTreeNode getParent() {
            return parent;
        }

        public boolean isLeaf() {
            return children.isEmpty();
        }

    }
}
