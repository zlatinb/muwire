package com.muwire.core.files

import static org.junit.jupiter.api.Assertions.assertAll

import org.junit.Before
import org.junit.Test

import com.muwire.core.EventBus
import com.muwire.core.InfoHash
import com.muwire.core.MuWireSettings
import com.muwire.core.SharedFile
import com.muwire.core.search.ResultsEvent
import com.muwire.core.search.SearchEvent
import com.muwire.core.util.DataUtil

import net.i2p.data.Base64

class FileManagerTest {

    EventBus eventBus

    FileManager manager
    volatile ResultsEvent results

    def listener = new Object() {
        void onResultsEvent(ResultsEvent e) {
            results = e
        }
    }

    @Before
    void before() {
        eventBus = new EventBus()
        eventBus.register(ResultsEvent.class, listener)
        manager = new FileManager(new File("testHome"), eventBus, new MuWireSettings())
        results = null
    }

    @Test
    void testHash1Result() {
        File f = new File("a b.c")
        byte [] root = new byte[32]
        SharedFile sf = new SharedFile(f,root, 0)
        FileHashedEvent fhe = new FileHashedEvent(sharedFile: sf)
        manager.onFileHashedEvent(fhe)

        UUID uuid = UUID.randomUUID()
        SearchEvent se = new SearchEvent(searchHash: root, uuid: uuid)

        manager.onSearchEvent(se)
        Thread.sleep(20)

        assert results != null
        assert results.results.size() == 1
        assert results.results.contains(sf)
        assert results.uuid == uuid
    }

    @Test
    void testHash2Results() {
        byte [] root = new byte[32]
        SharedFile sf1 = new SharedFile(new File("a b.c"), root, 0)
        SharedFile sf2 = new SharedFile(new File("d e.f"), root, 0)
        manager.onFileLoadedEvent new FileLoadedEvent(loadedFile : sf1)
        manager.onFileLoadedEvent new FileLoadedEvent(loadedFile : sf2)

        UUID uuid = UUID.randomUUID()
        SearchEvent se = new SearchEvent(searchHash: root, uuid: uuid)

        manager.onSearchEvent(se)
        Thread.sleep(20)

        assert results != null
        assert results.results.size() == 2
        assert results.results.contains(sf1)
        assert results.results.contains(sf2)
        assert results.uuid == uuid
    }

    @Test
    void testHash0Results() {
        File f = new File("a b.c")
        InfoHash ih = InfoHash.fromHashList(new byte[32])
        SharedFile sf = new SharedFile(f,ih.getRoot(), 0)
        FileHashedEvent fhe = new FileHashedEvent(sharedFile: sf)
        manager.onFileHashedEvent(fhe)

        manager.onSearchEvent new SearchEvent(searchHash: new byte[32], uuid: UUID.randomUUID())
        Thread.sleep(20)

        assert results == null
    }

    @Test
    void testKeyword1Result() {
        File f = new File("a b.c")
        InfoHash ih = InfoHash.fromHashList(new byte[32])
        SharedFile sf = new SharedFile(f,ih.getRoot(),0)
        FileHashedEvent fhe = new FileHashedEvent(sharedFile: sf)
        manager.onFileHashedEvent(fhe)

        UUID uuid = UUID.randomUUID()
        manager.onSearchEvent new SearchEvent(searchTerms: ["a"], uuid:uuid)
        Thread.sleep(20)

        assert results != null
        assert results.results.size() == 1
        assert results.results.contains(sf)
        assert results.uuid == uuid
    }

    @Test
    void testKeyword2Results() {
        File f1 = new File("a b.c")
        InfoHash ih1 = InfoHash.fromHashList(new byte[32])
        SharedFile sf1 = new SharedFile(f1, ih1.getRoot(), 0)
        manager.onFileLoadedEvent new FileLoadedEvent(loadedFile: sf1)

        File f2 = new File("c d.e")
        InfoHash ih2 = InfoHash.fromHashList(new byte[64])
        SharedFile sf2 = new SharedFile(f2, ih2.getRoot(), 0)
        manager.onFileLoadedEvent new FileLoadedEvent(loadedFile: sf2)

        UUID uuid = UUID.randomUUID()
        manager.onSearchEvent new SearchEvent(searchTerms: ["c"], uuid:uuid)
        Thread.sleep(20)

        assert results != null
        assert results.results.size() == 2
        assert results.results.contains(sf1)
        assert results.results.contains(sf2)
        assert results.uuid == uuid
    }

    @Test
    void testKeyword0Results() {
        File f = new File("a b.c")
        InfoHash ih = InfoHash.fromHashList(new byte[32])
        SharedFile sf = new SharedFile(f,ih.getRoot(),0)
        FileHashedEvent fhe = new FileHashedEvent(sharedFile: sf)
        manager.onFileHashedEvent(fhe)

        manager.onSearchEvent new SearchEvent(searchTerms: ["d"], uuid: UUID.randomUUID())
        Thread.sleep(20)

        assert results == null
    }

    @Test
    void testRemoveFileExistingHash() {
        InfoHash ih = InfoHash.fromHashList(new byte[32])
        SharedFile sf1 = new SharedFile(new File("a b.c"), ih.getRoot(), 0)
        SharedFile sf2 = new SharedFile(new File("d e.f"), ih.getRoot(), 0)
        manager.onFileLoadedEvent new FileLoadedEvent(loadedFile : sf1)
        manager.onFileLoadedEvent new FileLoadedEvent(loadedFile : sf2)

        manager.onFileUnsharedEvent new FileUnsharedEvent(deleted : true, unsharedFiles: new SharedFile[]{sf2})

        manager.onSearchEvent new SearchEvent(searchHash : ih.getRoot())
        Thread.sleep(20)
        assert results != null
        assert results.results.size() == 1
        assert results.results.contains(sf1)
    }

    @Test
    void testRemoveFile() {
        File f1 = new File("a b.c")
        InfoHash ih1 = InfoHash.fromHashList(new byte[32])
        SharedFile sf1 = new SharedFile(f1, ih1.getRoot(), 0)
        manager.onFileLoadedEvent new FileLoadedEvent(loadedFile: sf1)

        File f2 = new File("c d.e")
        InfoHash ih2 = InfoHash.fromHashList(new byte[64])
        SharedFile sf2 = new SharedFile(f2, ih2.getRoot(), 0)
        manager.onFileLoadedEvent new FileLoadedEvent(loadedFile: sf2)

        manager.onFileUnsharedEvent new FileUnsharedEvent(deleted : true, unsharedFiles: new SharedFile[]{sf2})

        // 1 match left
        manager.onSearchEvent new SearchEvent(searchTerms: ["c"])
        Thread.sleep(20)
        assert results != null
        assert results.results.size() == 1
        assert results.results.contains(sf1)

        // no match
        results = null
        manager.onSearchEvent new SearchEvent(searchTerms: ["d"])
        assert results == null

    }
    
    @Test
    void testComplicatedScenario() {
        // this tries to reproduce an NPE when un-sharing then sharing again and searching
        String comment = "same comment"
        comment = Base64.encode(DataUtil.encodei18nString(comment))
        File f1 = new File("MuWire-0.5.10.AppImage")
        InfoHash ih1 = InfoHash.fromHashList(new byte[32])
        SharedFile sf1 = new SharedFile(f1, ih1.getRoot(), 0)
        sf1.setComment(comment)
        
        manager.onFileLoadedEvent(new FileLoadedEvent(loadedFile : sf1))
        manager.onFileUnsharedEvent(new FileUnsharedEvent(unsharedFiles : new SharedFile[]{sf1}, deleted : true))
        
        File f2 = new File("MuWire-0.6.0.AppImage")
        InfoHash ih2 = InfoHash.fromHashList(new byte[64])
        SharedFile sf2 = new SharedFile(f2, ih2.getRoot(), 0)
        sf2.setComment(comment)
        
        manager.onFileLoadedEvent(new FileLoadedEvent(loadedFile : sf2))
        
        manager.onSearchEvent(new SearchEvent(searchTerms : ["muwire"]))
        Thread.sleep(20)
        
        assert results != null
        assert results.results.size() == 1
        assert results.results.contains(sf2)
        
        results = null
        manager.onSearchEvent(new SearchEvent(searchTerms : ['comment'], searchComments : true, oobInfohash : true))
        Thread.sleep(20)
        assert results != null
        assert results.results.size() == 1
        assert results.results.contains(sf2)
    }
}
