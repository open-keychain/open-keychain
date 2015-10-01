/*
 * Copyright (C) 2015 Dominik Schürmann <dominik@dominikschuermann.de>
 * Copyright (C) 2014 Jake McGinty (Open Whisper Systems)
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

package org.sufficientlysecure.keychain.experimental;

import android.content.Context;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.util.Log;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * From https://github.com/mcginty/TextSecure/tree/mnemonic-poem
 */
public class SentenceConfirm {
    Context context;
    List<String> n, vi, vt, adj, adv, p, art;

    public SentenceConfirm(Context context) {
        this.context = context;
        try {
            n = readFile(R.raw.fp_sentence_nouns);
            vi = readFile(R.raw.fp_sentence_verbs_i);
            vt = readFile(R.raw.fp_sentence_verbs_t);
            adj = readFile(R.raw.fp_sentence_adjectives);
            adv = readFile(R.raw.fp_sentence_adverbs);
            p = readFile(R.raw.fp_sentence_prepositions);
            art = readFile(R.raw.fp_sentence_articles);
        } catch (IOException e) {
            Log.e(Constants.TAG, "Reading sentence files failed", e);
        }
    }

    List<String> readFile(int resId) throws IOException {
        if (context.getApplicationContext() == null) {
            throw new AssertionError("app context can't be null");
        }

        BufferedReader in = new BufferedReader(new InputStreamReader(
                context.getApplicationContext()
                        .getResources()
                        .openRawResource(resId)));
        List<String> words = new ArrayList<>();
        String word = in.readLine();
        while (word != null) {
            words.add(word);
            word = in.readLine();
        }
        in.close();
        return words;
    }

    public String fromBytes(final byte[] bytes, int desiredBytes) throws IOException {
        BitInputStream bin = new BitInputStream(new ByteArrayInputStream(bytes));
        EntropyString fingerprint = new EntropyString();

        while (fingerprint.getBits() < (desiredBytes * 8)) {
            if (!fingerprint.isEmpty()) {
                fingerprint.append("\n\n");
            }
            try {
                fingerprint.append(getSentence(bin));
            } catch (IOException e) {
                Log.e(Constants.TAG, "IOException when creating the sentence");
                throw e;
            }
        }
        return fingerprint.toString();
    }

    /**
     * Grab a word for a list of them using the necessary bits to choose from a BitInputStream
     *
     * @param words the list of words to select from
     * @param bin   the bit input stream to encode from
     * @return A Pair of the word and the number of bits consumed from the stream
     */
    private EntropyString getWord(List<String> words, BitInputStream bin) throws IOException {
        final int neededBits = log(words.size(), 2);
        Log.d(Constants.TAG, "need: " + neededBits + " bits of entropy");
        Log.d(Constants.TAG, "available: " + bin.available() + " bits");
        if (neededBits > bin.available()) {
            Log.d(Constants.TAG, "stuffed with " + (neededBits - bin.available()) + " ones!");
        }
        int bits = bin.readBits(neededBits, true);
        Log.d(Constants.TAG, "got word " + words.get(bits) + " with " + neededBits + " bits of entropy");
        return new EntropyString(words.get(bits), neededBits);
    }

    private EntropyString getNounPhrase(BitInputStream bits) throws IOException {
        final EntropyString phrase = new EntropyString();
        phrase.append(getWord(art, bits)).append(" ");
        if (bits.readBit(true) != 0) {
            phrase.append(getWord(adj, bits)).append(" ");
        }
        phrase.incBits();

        phrase.append(getWord(n, bits));
        Log.d(Constants.TAG, "got phrase " + phrase + " with " + phrase.getBits() + " bits of entropy");
        return phrase;
    }

    EntropyString getSentence(BitInputStream bits) throws IOException {
        final EntropyString sentence = new EntropyString();
        sentence.append(getNounPhrase(bits));   // Subject
        if (bits.readBit(true) != 0) {
            sentence.append(" ").append(getWord(vt, bits));   // Transitive verb
            sentence.append(" ").append(getNounPhrase(bits)); // Object of transitive verb
        } else {
            sentence.append(" ").append(getWord(vi, bits));   // Intransitive verb
        }
        sentence.incBits();

        if (bits.readBit(true) != 0) {
            sentence.append(" ").append(getWord(adv, bits)); // Adverb
        }

        sentence.incBits();
        if (bits.readBit(true) != 0) {
            sentence.append(" ").append(getWord(p, bits));    // Preposition
            sentence.append(" ").append(getNounPhrase(bits)); // Object of preposition
        }
        sentence.incBits();
        Log.d(Constants.TAG, "got sentence '" + sentence + "' with " + sentence.getBits() + " bits of entropy");

        // uppercase first character, end with dot (without increasing the bits)
        sentence.getBuilder().replace(0, 1,
                Character.toString(Character.toUpperCase(sentence.getBuilder().charAt(0))));
        sentence.getBuilder().append(".");

        return sentence;
    }

    public static class EntropyString {
        private StringBuilder builder;
        private int bits;

        public EntropyString(String phrase, int bits) {
            this.builder = new StringBuilder(phrase);
            this.bits = bits;
        }

        public EntropyString() {
            this("", 0);
        }

        public StringBuilder getBuilder() {
            return builder;
        }

        public boolean isEmpty() {
            return builder.length() == 0;
        }

        public EntropyString append(EntropyString phrase) {
            builder.append(phrase);
            bits += phrase.getBits();
            return this;
        }

        public EntropyString append(String string) {
            builder.append(string);
            return this;
        }

        public int getBits() {
            return bits;
        }

        public void setBits(int bits) {
            this.bits = bits;
        }

        public void incBits() {
            bits += 1;
        }

        @Override
        public String toString() {
            return builder.toString();
        }
    }

    private static int log(int x, int base) {
        return (int) (Math.log(x) / Math.log(base));
    }

}