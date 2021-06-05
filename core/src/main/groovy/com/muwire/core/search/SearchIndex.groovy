package com.muwire.core.search

import com.muwire.core.SplitPattern
import net.metanotionz.io.Serializer
import net.metanotionz.io.block.BlockFile
import net.metanotionz.util.skiplist.SkipList

import java.nio.charset.StandardCharsets

class SearchIndex {

    private final SkipList keywords, hashes
    
    SearchIndex(String name) {
        BlockFile blockFile = new BlockFile(name, true)
        keywords = blockFile.makeIndex("keywords", new StringSerializer(), new HashArraySerializer())
        hashes = blockFile.makeIndex("hashes", new HashSerializer(), new StringArraySerializer())
    }
    
    void add(String string) {
        final int hash = string.hashCode()
        String [] split = split(string)
        split.each {keyword ->
            int [] existingHashes = (int[])keywords.get(keyword)
            if (existingHashes == null) {
                existingHashes = new int[1]
                existingHashes[0] = hash
                keywords.put(keyword, existingHashes)
                hashes.put(hash, new String[] {string})
            } else {
                boolean found = false
                for (int existingHash : existingHashes) {
                    if (existingHash == hash) {
                        found = true
                        break
                    }
                }
                if (!found) {
                    int [] newHashes = new int[existingHashes.length + 1]
                    System.arraycopy(existingHashes, 0, newHashes, 0, existingHashes.length)
                    newHashes[newHashes.length - 1] = hash
                    keywords.put(keyword, newHashes)
                }
                
                String[] fileNames = (String []) hashes.get(hash)
                if (fileNames == null) {
                    fileNames = new String[] {string}
                    hashes.put(hash, fileNames)
                } else {
                    Set<String> unique = new HashSet<>()
                    for (String fileName : fileNames)
                        unique.add(fileName)
                    unique.add(string)
                    hashes.put(hash, unique.toArray(new String[0]))
                }
            }
        }
    }

    void remove(String string) {
        final int hash = string.hashCode()
        
        // check if such string exists at all
        String [] strings = (String[]) hashes.get(hash)
        if (strings == null)
            return
        
        int idx = -1
        for (int i = 0; i < strings.length; i++) {
            if (strings[i] == string) {
                idx = i
                break
            }
        }
        
        if (idx == -1)
            return
        
        String [] newStrings = new String[strings.length - 1]
        System.arraycopy(strings, 0, newStrings, 0, idx)
        System.arraycopy(strings, idx + 1, newStrings, idx, newStrings.length - idx)
        
        hashes.put(hash, newStrings)
        
        String [] split = split(string)
        for (String keyword : split) {
            int [] existingHashes = (int [])keywords.get(keyword)
            if (existingHashes == null)
                throw new IllegalStateException()
            idx = -1
            for (int i = 0; i < existingHashes.length; i ++) {
                if (existingHashes[i] == hash) {
                    idx = i
                    break
                }
            }
            if (idx == -1)
                throw new IllegalStateException()
            
            if (existingHashes.length == 1) {
                keywords.remove(keyword)
            } else {
                int [] newHashes = new int[existingHashes.length - 1]
                System.arraycopy(existingHashes, 0, newHashes, 0, idx)
                System.arraycopy(existingHashes, idx + 1, newHashes, idx, newHashes.length - idx)
                keywords.put(keyword, newHashes)
            }
        }
    }

    private static String[] split(final String source) {
        // first split by split pattern
        String sourceSplit = source.replaceAll(SplitPattern.SPLIT_PATTERN, " ").toLowerCase()
        String [] split = sourceSplit.split(" ")
        def rv = new HashSet()
        split.each { if (it.length() > 0) rv << it }
        
        // then just by ' '
        source.toLowerCase().split(' ').each { if (it.length() > 0) rv << it }
        
        // and add original string
        rv << source
        rv << source.toLowerCase()
        rv.toArray(new String[0])
    }

    String[] search(List<String> terms) {
        Set<String> rv = null;

        Set<String> powerSet = new HashSet<>()
        terms.each {
            powerSet.addAll(it.toLowerCase().split(' '))
        }
        
        powerSet.each {
            Set<String> forWord = new HashSet<>()
            int [] wordHashes = (int[])keywords.get(it)
            for (int h : wordHashes) {
                String [] forHashArray = hashes.get(h)
                if (forHashArray == null)
                    continue
                for (String s : forHashArray)
                    forWord.add(s)
            }
            if (rv == null) {
                rv = new HashSet<>(forWord)
            } else {
                rv.retainAll(forWord)
            }
        }
        
        // now, filter by terms
        for (Iterator<String> iter = rv.iterator(); iter.hasNext();) {
            String candidate = iter.next()
            candidate = candidate.toLowerCase()
            boolean keep = true
            terms.each { 
                keep &= candidate.contains(it)
            }
            if (!keep)
                iter.remove()
        }

        if (rv != null)
            return rv.asList()
        []
    }
    
    private static class HashSerializer implements Serializer {

        @Override
        byte[] getBytes(Object o) {
            if (!(o instanceof Integer))
                throw new IllegalArgumentException()
            int hashCode = (Integer)o
            byte [] hash = new byte[4]
            hash [0] = (byte)((hashCode >>> 24) & 0xFF)
            hash [1] = (byte)((hashCode >>> 16) & 0xFF)
            hash [2] = (byte)((hashCode >>> 8) & 0xFF)
            hash [3] = (byte) (hashCode & 0xFF)
            return hash
        }

        @Override
        Object construct(byte[] b) {
            int rv = (b[0] & 0xFF) << 24 |
                    (b[1] & 0xFF) << 16 |
                    (b[2] & 0xFF) << 8 |
                    (b[3] & 0xFF)
            rv
        }
    }
    
    private static class HashArraySerializer implements Serializer {

        @Override
        byte[] getBytes(Object o) {
            if (!(o instanceof int[]))
                throw new IllegalArgumentException()
            int [] hashes = (int[]) o
            byte [] rv = new byte[hashes.length * 4]
            for (int i = 0; i < hashes.length; i ++) {
                int hash = hashes[i]
                rv[i * 4] = (byte) ((hash >>> 24) & 0xFF)
                rv[i * 4 + 1] = (byte) ((hash >>> 16) & 0xFF)
                rv[i * 4 + 2] = (byte) ((hash >>> 8) & 0xFF)
                rv[i * 4 + 3] = (byte) (hash & 0xFF)
            }
            return rv
        }

        @Override
        Object construct(byte[] b) {
            if (b.length % 4 != 0)
                throw new IllegalArgumentException("invalid length ${b.length}")
            int [] rv = new int[b.length / 4]
            for (int i = 0; i < rv.length; i ++) {
                rv[i] = (b[i * 4] & 0xFF) << 24 |
                        (b[i * 4 + 1] & 0xFF) << 16 |
                        (b[i * 4 + 2] & 0xFF) << 8 |
                        (b[i * 4 + 3] & 0xFF)
            }
            rv
        }
    }
    
    private static class StringSerializer implements Serializer {

        @Override
        byte[] getBytes(Object o) {
            if (!(o instanceof String))
                throw new IllegalArgumentException()
            String s = (String)o
            return s.getBytes(StandardCharsets.UTF_8)
        }

        @Override
        Object construct(byte[] b) {
            new String(b, StandardCharsets.UTF_8)
        }
    }
    
    private static class StringArraySerializer implements Serializer {

        @Override
        byte[] getBytes(Object o) {
            if (!(o instanceof String[]))
                throw new IllegalArgumentException()
            String[] strings = (String[]) o
            def baos = new ByteArrayOutputStream()
            def daos = new DataOutputStream(baos)
            daos.writeInt(strings.length)
            strings.each {
                byte [] utf8 = it.getBytes(StandardCharsets.UTF_8)
                daos.writeInt(utf8.length)
                daos.write(utf8)
            }
            daos.close()
            baos.toByteArray()
        }

        @Override
        Object construct(byte[] b) {
            def dis = new DataInputStream(new ByteArrayInputStream(b))
            int count = dis.readInt()
            if (count < 0)
                throw new IllegalStateException("negative count $count")
            String[] rv = new String[count]
            for (int i = 0; i < count; i ++) {
                int length = dis.readInt()
                byte [] tmp = new byte[length]
                dis.readFully(tmp)
                rv[i] = new String(tmp, StandardCharsets.UTF_8)
            }
            rv
        }
    }
}
