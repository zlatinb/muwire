package com.muwire.core.search;

import com.muwire.core.util.DataUtil;
import net.metanotionz.io.Serializer;
import net.metanotionz.io.block.BlockFile;
import net.metanotionz.util.skiplist.SkipList;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class SearchIndexImpl {
    
    private final SkipList<String, int[]> keywords;
    private final SkipList<Integer, String[]> hashes;
    
    SearchIndexImpl(String name) throws IOException {
        BlockFile blockFile = new BlockFile(name, true);
        keywords = blockFile.makeIndex("keywords", new StringSerializer(), new HashArraySerializer());
        hashes = blockFile.makeIndex("hashes", new HashSerializer(), new StringArraySerializer2());
    }

    
    void add(String string, String [] split) throws IOException {
        final int hash = string.hashCode();
        for (String keyword : split) {
            int [] existingHashes = keywords.get(keyword);
            if (existingHashes == null) {
                existingHashes = new int[1];
                existingHashes[0] = hash;
                keywords.put(keyword, existingHashes);
                hashes.put(hash, new String[] {string});
            } else {
                int [] newHashes = DataUtil.insertIntoSortedArray(existingHashes, hash);
                if (newHashes != existingHashes) {
                    keywords.put(keyword, newHashes);
                }

                String[] fileNames = hashes.get(hash);
                if (fileNames == null) {
                    fileNames = new String[] {string};
                    hashes.put(hash, fileNames);
                } else {
                    Set<String> unique = new HashSet<>();
                    for (String fileName : fileNames)
                        unique.add(fileName);
                    if (unique.add(string))
                        hashes.put(hash, unique.toArray(new String[0]));
                }
            }
        }
    }
    
    void remove(String string, String[] split) throws IOException {
        final int hash = string.hashCode();

        // check if such string exists at all
        String [] strings = hashes.get(hash);
        if (strings == null)
            return;

        int idx = -1;
        for (int i = 0; i < strings.length; i++) {
            if (strings[i].equals(string)) {
                idx = i;
                break;
            }
        }

        if (idx == -1)
            return;

        String [] newStrings = new String[strings.length - 1];
        System.arraycopy(strings, 0, newStrings, 0, idx);
        System.arraycopy(strings, idx + 1, newStrings, idx, newStrings.length - idx);

        hashes.put(hash, newStrings);

        for (String keyword : split) {
            int [] existingHashes = keywords.get(keyword);
            if (existingHashes == null)
                throw new IllegalStateException();
            idx = -1;
            for (int i = 0; i < existingHashes.length; i ++) {
                if (existingHashes[i] == hash) {
                    idx = i;
                    break;
                }
            }
            if (idx == -1)
                throw new IllegalStateException();

            if (existingHashes.length == 1) {
                keywords.remove(keyword);
            } else {
                int [] newHashes = new int[existingHashes.length - 1];
                System.arraycopy(existingHashes, 0, newHashes, 0, idx);
                System.arraycopy(existingHashes, idx + 1, newHashes, idx, newHashes.length - idx);
                keywords.put(keyword, newHashes);
            }
        }
    }
    
    String [] search (List<String> terms) throws IOException {
        Set<String> rv = null;

        Set<String> powerSet = new HashSet<>();
        for(String it : terms) {
            String [] split = it.toLowerCase().split(" ");
            for (String splitIt : split)
                powerSet.add(splitIt);
        }

        for (String it : powerSet) {
            Set<String> forWord = new HashSet<>();
            int [] wordHashes = keywords.get(it);
            if (wordHashes == null)
                continue;
            for (int h : wordHashes) {
                String [] forHashArray = hashes.get(h);
                if (forHashArray == null)
                    continue;
                for (String s : forHashArray)
                    forWord.add(s);
            }
            if (rv == null) {
                rv = new HashSet<>(forWord);
            } else {
                rv.retainAll(forWord);
            }
        }

        if (rv == null) {
            return new String[0];
        }
        
        // now, filter by terms
        for (Iterator<String> iter = rv.iterator(); iter.hasNext();) {
            String candidate = iter.next();
            candidate = candidate.toLowerCase();
            boolean keep = true;
            for (String it : terms) {
                keep &= candidate.contains(it);
            }
            if (!keep)
                iter.remove();
        }
        return rv.toArray(new String[0]);
    }
    
    private static class HashSerializer implements Serializer<Integer> {

        @Override
        public byte[] getBytes(Integer hashCode) {
            byte [] hash = new byte[4];
            hash [0] = (byte)((hashCode >>> 24) & 0xFF);
            hash [1] = (byte)((hashCode >>> 16) & 0xFF);
            hash [2] = (byte)((hashCode >>> 8) & 0xFF);
            hash [3] = (byte) (hashCode & 0xFF);
            return hash;
        }

        @Override
        public Integer construct(byte[] b) {
            int rv = (b[0] & 0xFF) << 24 |
                    (b[1] & 0xFF) << 16 |
                    (b[2] & 0xFF) << 8 |
                    (b[3] & 0xFF);
            return rv;
        }
    }

    private static class HashArraySerializer implements Serializer<int[]> {

        @Override
        public byte[] getBytes(int[] hashes) {
            byte [] rv = new byte[hashes.length * 4];
            for (int i = 0; i < hashes.length; i ++) {
                int hash = hashes[i];
                rv[i * 4] = (byte) ((hash >>> 24) & 0xFF);
                rv[i * 4 + 1] = (byte) ((hash >>> 16) & 0xFF);
                rv[i * 4 + 2] = (byte) ((hash >>> 8) & 0xFF);
                rv[i * 4 + 3] = (byte) (hash & 0xFF);
            }
            return rv;
        }

        @Override
        public int[] construct(byte[] b) {
            if (b.length % 4 != 0)
                throw new IllegalArgumentException("invalid length ${b.length}");
            int [] rv = new int[b.length / 4];
            for (int i = 0; i < rv.length; i ++) {
                rv[i] = (b[i * 4] & 0xFF) << 24 |
                        (b[i * 4 + 1] & 0xFF) << 16 |
                        (b[i * 4 + 2] & 0xFF) << 8 |
                        (b[i * 4 + 3] & 0xFF);
            }
            return rv;
        }
    }

    private static class StringSerializer implements Serializer<String> {

        @Override
        public byte[] getBytes(String s) {
            return s.getBytes(StandardCharsets.UTF_8);
        }

        @Override
        public String construct(byte[] b) {
            return new String(b, StandardCharsets.UTF_8);
        }
    }

    private static class StringArraySerializer implements Serializer<String[]> {

        @Override
        public byte[] getBytes(String[] strings) {
            try {
                int size = 0;
                for (String it : strings)
                    size += it.length();
                ByteArrayOutputStream baos = new ByteArrayOutputStream(size * 2);
                DataOutputStream daos = new DataOutputStream(baos);
                daos.writeInt(strings.length);
                for (String it : strings) {
                    byte[] utf8 = it.getBytes(StandardCharsets.UTF_8);
                    daos.writeInt(utf8.length);
                    daos.write(utf8);
                }
                return baos.toByteArray();
            } catch (IOException impossible) {
                throw new RuntimeException(impossible);
            }
        }

        @Override
        public String[] construct(byte[] b) {
            try {
                DataInputStream dis = new DataInputStream(new ByteArrayInputStream(b));
                int count = dis.readInt();
                if (count < 0)
                    throw new IllegalStateException("negative count $count");
                String[] rv = new String[count];
                for (int i = 0; i < count; i++) {
                    int length = dis.readInt();
                    byte[] tmp = new byte[length];
                    dis.readFully(tmp);
                    rv[i] = new String(tmp, StandardCharsets.UTF_8);
                }
                return rv;
            } catch (IOException impossible) {
                throw new RuntimeException(impossible);
            }
        }
    }
    
    private static class StringArraySerializer2 implements Serializer<String[]> {

        @Override
        public byte[] getBytes(String[] o) {
            int size = 0;
            for (String s : o)
                size += s.length() * 2;
            size += o.length * 4;
            size += 4;
            if ((size & 0x1) == 1)
                size++;
            
            byte[] rv = new byte[size];
            ByteBuffer byteBuffer = ByteBuffer.wrap(rv);
            CharBuffer charBuffer = byteBuffer.asCharBuffer();
            
            byteBuffer.putInt(o.length);
            charBuffer.position(2);
            for (String s : o) {
                byteBuffer.putInt(s.length());
                charBuffer.position(charBuffer.position() + 2);
                charBuffer.put(s);
                byteBuffer.position(byteBuffer.position() + s.length() * 2);
            }
            return rv;
        }

        @Override
        public String[] construct(byte[] b) {
            ByteBuffer byteBuffer = ByteBuffer.wrap(b);
            int count = byteBuffer.getInt();
            String[] rv = new String[count];
            for (int i = 0; i < count; i ++) {
                int length = byteBuffer.getInt();
                byteBuffer.limit(byteBuffer.position() + length * 2);
                CharBuffer charBuffer = byteBuffer.slice().asCharBuffer();
                rv[i] = charBuffer.toString();
                byteBuffer.limit(byteBuffer.capacity());
                byteBuffer.position(byteBuffer.position() + length * 2);
            }
            return rv;
        }
    }
}
