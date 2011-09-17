/*
 * Copyright (C) 2010 Thialfihar <thi@thialfihar.org>
 *
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

package org.thialfihar.android.apg;

import java.util.Vector;
import java.util.regex.Matcher;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;

public class MailListActivity extends ListActivity {
    LayoutInflater mInflater = null;

    public static final String EXTRA_ACCOUNT = "account";

    private static class Conversation {
        public long id;
        public String subject;
        public Vector<Message> messages;

        public Conversation(long id, String subject) {
            this.id = id;
            this.subject = subject;
        }
    }

    private static class Message {
        public Conversation parent;
        public long id;
        public String subject;
        public String fromAddress;
        public String data;
        public String replyTo;
        public boolean signedOnly;

        public Message(Conversation parent, long id, String subject,
                       String fromAddress, String replyTo,
                       String data, boolean signedOnly) {
            this.parent = parent;
            this.id = id;
            this.subject = subject;
            this.fromAddress = fromAddress;
            this.replyTo = replyTo;
            this.data = data;
            if (this.replyTo == null || this.replyTo.equals("")) {
                this.replyTo = this.fromAddress;
            }
            this.signedOnly = signedOnly;
        }
    }

    private Vector<Conversation> mConversations;
    private Vector<Message> mMessages;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Preferences prefs = Preferences.getPreferences(this);
        BaseActivity.setLanguage(this, prefs.getLanguage());

        super.onCreate(savedInstanceState);

        mInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        mConversations = new Vector<Conversation>();
        mMessages = new Vector<Message>();

        String account = getIntent().getExtras().getString(EXTRA_ACCOUNT);
        // TODO: what if account is null?
        Uri uri = Uri.parse("content://gmail-ls/conversations/" + account);
        Cursor cursor =
                managedQuery(uri, new String[] { "conversation_id", "subject" }, null, null, null);
        for (int i = 0; i < cursor.getCount(); ++i) {
            cursor.moveToPosition(i);

            int idIndex = cursor.getColumnIndex("conversation_id");
            int subjectIndex = cursor.getColumnIndex("subject");
            long conversationId = cursor.getLong(idIndex);
            Conversation conversation =
                    new Conversation(conversationId, cursor.getString(subjectIndex));
            Uri messageUri = Uri.withAppendedPath(uri, "" + conversationId + "/messages");
            Cursor messageCursor =
                    managedQuery(messageUri, new String[] {
                            "messageId",
                            "subject",
                            "fromAddress",
                            "replyToAddresses",
                            "body" }, null, null, null);
            Vector<Message> messages = new Vector<Message>();
            for (int j = 0; j < messageCursor.getCount(); ++j) {
                messageCursor.moveToPosition(j);
                idIndex = messageCursor.getColumnIndex("messageId");
                subjectIndex = messageCursor.getColumnIndex("subject");
                int fromAddressIndex = messageCursor.getColumnIndex("fromAddress");
                int replyToIndex = messageCursor.getColumnIndex("replyToAddresses");
                int bodyIndex = messageCursor.getColumnIndex("body");
                String data = messageCursor.getString(bodyIndex);
                data = Html.fromHtml(data).toString();
                boolean signedOnly = false;
                Matcher matcher = Apg.PGP_MESSAGE.matcher(data);
                if (matcher.matches()) {
                    data = matcher.group(1);
                } else {
                    matcher = Apg.PGP_SIGNED_MESSAGE.matcher(data);
                    if (matcher.matches()) {
                        data = matcher.group(1);
                        signedOnly = true;
                    } else {
                        data = null;
                    }
                }
                Message message =
                        new Message(conversation,
                                    messageCursor.getLong(idIndex),
                                    messageCursor.getString(subjectIndex),
                                    messageCursor.getString(fromAddressIndex),
                                    messageCursor.getString(replyToIndex),
                                    data, signedOnly);

                messages.add(message);
                mMessages.add(message);
            }
            conversation.messages = messages;
            mConversations.add(conversation);
        }

        setListAdapter(new MailboxAdapter());
        getListView().setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> arg0, View v, int position, long id) {
                Intent intent = new Intent(MailListActivity.this, DecryptActivity.class);
                intent.setAction(Apg.Intent.DECRYPT);
                Message message = (Message) ((MailboxAdapter) getListAdapter()).getItem(position);
                intent.putExtra(Apg.EXTRA_TEXT, message.data);
                intent.putExtra(Apg.EXTRA_SUBJECT, message.subject);
                intent.putExtra(Apg.EXTRA_REPLY_TO, message.replyTo);
                startActivity(intent);
            }
        });
    }

    private class MailboxAdapter extends BaseAdapter implements ListAdapter {

        @Override
        public boolean isEnabled(int position) {
            Message message = (Message) getItem(position);
            return message.data != null;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        public int getCount() {
            return mMessages.size();
        }

        public Object getItem(int position) {
            return mMessages.get(position);
        }

        public long getItemId(int position) {
            return mMessages.get(position).id;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            View view = mInflater.inflate(R.layout.mailbox_message_item, null);

            Message message = (Message) getItem(position);

            TextView subject = (TextView) view.findViewById(R.id.subject);
            TextView email = (TextView) view.findViewById(R.id.emailAddress);
            ImageView status = (ImageView) view.findViewById(R.id.ic_status);

            subject.setText(message.subject);
            email.setText(message.fromAddress);
            if (message.data != null) {
                if (message.signedOnly) {
                    status.setImageResource(R.drawable.signed);
                } else {
                    status.setImageResource(R.drawable.encrypted);
                }
                status.setVisibility(View.VISIBLE);
            } else {
                status.setVisibility(View.INVISIBLE);
            }

            return view;
        }
    }
}
