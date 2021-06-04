package com.muwire.core.search

import com.muwire.core.SplitPattern
import net.metanotionz.io.Serializer
import net.metanotionz.io.block.BlockFile
import net.metanotionz.util.skiplist.SkipList

import java.nio.charset.StandardCharsets

class SearchIndex {

    private final SkipList keywords
    
    SearchIndex(String name) {
        BlockFile blockFile = new BlockFile(name, true)
        keywords = blockFile.makeIndex("keywords", new KeySerializer(), new ValueSerializer())
    }
    
    void add(String string) {
        String [] split = split(string)
        split.each {keyword ->
            String [] existing = keywords.get(keyword)
            if (existing == null) {
                existing = new String[1]
                existing[0] = string
            } else {
                Set<String> unique = new HashSet<>()
                existing.each {old -> unique.add(old)}
                unique.add(string)
                existing = unique.toArray(existing)
            }
            keywords.put(keyword, existing)
        }
    }

    void remove(String string) {
        String [] split = split(string)
        split.each {keyword ->
            String [] existing = keywords.get(keyword)
            if (existing != null) {
                Set<String> unique = new HashSet<>()
                existing.each {old -> unique.add(old)}
                unique.remove(string)
                if (unique.isEmpty()) {
                    keywords.remove(keyword)
                } else {
                    keywords.put(keyword, unique.toArray(new String[0]))
                }
            }
        }
    }

    private static String[] split(final String source) {
        // first split by split pattern
        String sourceSplit = source.replaceAll(SplitPattern.SPLIT_PATTERN, " ").toLowerCase()
        String [] split = sourceSplit.split(" ")
        def rv = []
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
            String [] forWordArray = keywords.get(it)
            if (forWordArray != null) {
                forWordArray.each {found -> forWord.add(found)}
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
    
    private static class KeySerializer implements Serializer {

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
    
    private static class ValueSerializer implements Serializer {

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
