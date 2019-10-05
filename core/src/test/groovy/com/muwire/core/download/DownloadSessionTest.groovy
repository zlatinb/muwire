package com.muwire.core.download

import static org.junit.Assert.fail

import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Test

import com.muwire.core.EventBus
import com.muwire.core.InfoHash
import com.muwire.core.Persona
import com.muwire.core.Personas
import com.muwire.core.connection.Endpoint
import com.muwire.core.files.FileHasher
import static com.muwire.core.util.DataUtil.readTillRN
import static com.muwire.core.util.DataUtil.encodeXHave

import net.i2p.data.Base64
import net.i2p.util.ConcurrentHashSet

class DownloadSessionTest {

    private EventBus eventBus
    private File source, target
    private InfoHash infoHash
    private Endpoint endpoint
    private Pieces pieces
    private String rootBase64

    private DownloadSession session
    private Thread downloadThread

    private InputStream fromDownloader, fromUploader
    private OutputStream toDownloader, toUploader

    private volatile boolean performed
    private Set<Integer> available = new ConcurrentHashSet<>()
    private volatile IOException thrown


    @Before
    public void setUp() {
        eventBus = new EventBus()
    }

    private void initSession(int size, def claimedPieces = []) {
        Random r = new Random()
        byte [] content = new byte[size]
        r.nextBytes(content)

        source = File.createTempFile("source", "tmp")
        source.deleteOnExit()
        def fos = new FileOutputStream(source)
        fos.write(content)
        fos.close()

        def hasher = new FileHasher()
        infoHash = hasher.hashFile(source)
        rootBase64 = Base64.encode(infoHash.getRoot())

        target = File.createTempFile("target", "tmp")
        int pieceSize = 1 << FileHasher.getPieceSize(size)

        int nPieces
        if (size % pieceSize == 0)
            nPieces = size / pieceSize
        else
            nPieces = size / pieceSize + 1
        pieces = new Pieces(nPieces)
        claimedPieces.each {pieces.claimed.set(it)}

        fromDownloader = new PipedInputStream()
        fromUploader = new PipedInputStream()
        toDownloader = new PipedOutputStream(fromUploader)
        toUploader = new PipedOutputStream(fromDownloader)
        endpoint = new Endpoint(null, fromUploader, toUploader, null)

        session = new DownloadSession(eventBus, "",pieces, infoHash, endpoint, target, pieceSize, size, available)
        downloadThread = new Thread( { perform() } as Runnable)
        downloadThread.setDaemon(true)
        downloadThread.start()
    }

    private void perform() {
        try {
            performed = session.request()
        } catch (IOException e) {
            thrown = e
        }
    }

    @After
    public void teardown() {
        source?.delete()
        target?.delete()
        downloadThread?.interrupt()
        Thread.sleep(50)
    }

    @Test
    public void testSmallFile() {
        initSession(20)
        assert "GET $rootBase64" == readTillRN(fromDownloader)
        assert "Range: 0-19" == readTillRN(fromDownloader)
        readTillRN(fromDownloader)
        readTillRN(fromDownloader)
        assert "" == readTillRN(fromDownloader)

        toDownloader.write("200 OK\r\n".bytes)
        toDownloader.write("Content-Range: 0-19\r\n\r\n".bytes)
        toDownloader.write(source.bytes)
        toDownloader.flush()

        Thread.sleep(150)

        assert pieces.isComplete()
        assert target.bytes == source.bytes
        assert performed
        assert available.isEmpty()
        assert thrown == null
    }

    @Test
    public void testPieceSizeFile() {
        int size = FileHasher.getPieceSize(1)
        size = 1 << size
        initSession(size)

        assert "GET $rootBase64" == readTillRN(fromDownloader)
        readTillRN(fromDownloader)
        readTillRN(fromDownloader)
        readTillRN(fromDownloader)
        assert "" == readTillRN(fromDownloader)

        toDownloader.write("200 OK\r\n".bytes)
        toDownloader.write(("Content-Range: 0-"+(size - 1)+"\r\n\r\n").bytes)
        toDownloader.write(source.bytes)
        toDownloader.flush()

        Thread.sleep(150)
        assert pieces.isComplete()
        assert target.bytes == source.bytes
        assert performed
        assert available.isEmpty()
        assert thrown == null
    }

    @Test
    public void testPieceSizePlusOne() {
        int pieceSize = FileHasher.getPieceSize(1)
        int size = (1 << pieceSize) + 1
        initSession(size)

        assert "GET $rootBase64" == readTillRN(fromDownloader)
        String range = readTillRN(fromDownloader)
        def matcher = (range =~ /^Range: (\d+)-(\d+)$/)
        int start = Integer.parseInt(matcher[0][1])
        int end = Integer.parseInt(matcher[0][2])

        assert (start == 0 && end == ((1 << pieceSize) - 1)) ||
            (start == (1 << pieceSize) && end == (1 << pieceSize))

        readTillRN(fromDownloader)
        readTillRN(fromDownloader)
        assert "" == readTillRN(fromDownloader)

        toDownloader.write("200 OK\r\n".bytes)
        toDownloader.write(("Content-Range: $start-$end\r\n\r\n").bytes)
        byte [] piece = new byte[end - start + 1]
        System.arraycopy(source.bytes, start, piece, 0, piece.length)
        toDownloader.write(piece)
        toDownloader.flush()

        Thread.sleep(150)
        assert !pieces.isComplete()
        assert 1 == pieces.donePieces()
        assert performed
        assert available.isEmpty()
        assert thrown == null
    }

    @Test
    @Ignore // this needs to be rewritten with stealing in mind
    public void testSmallFileClaimed() {
        initSession(20, [0])
        long now = System.currentTimeMillis()
        downloadThread.join(150)
        assert 100 >= (System.currentTimeMillis() - now)
        assert !performed
        assert available.isEmpty()
        assert thrown == null
    }

    @Test
    public void testClaimedPiecesAvoided() {
        int pieceSize = FileHasher.getPieceSize(1)
        int size = (1 << pieceSize) * 10
        initSession(size, [1,2,3,4,5,6,7,8,9])
        assert !pieces.claimed.get(0)

        assert "GET $rootBase64" == readTillRN(fromDownloader)
        String range = readTillRN(fromDownloader)
        def matcher = (range =~ /^Range: (\d+)-(\d+)$/)
        int start = Integer.parseInt(matcher[0][1])
        int end = Integer.parseInt(matcher[0][2])

        assert pieces.claimed.get(0)
        assert start == 0 && end == (1 << pieceSize) - 1
    }

    @Test
    public void test416NoHave() {
        initSession(20)
        readAllHeaders(fromDownloader)

        toDownloader.write("416 don't have it\r\n\r\n".bytes)
        toDownloader.flush()
        Thread.sleep(150)
        assert !performed
        assert available.isEmpty()
        assert thrown != null
    }

    @Test
    public void test416Have() {
        initSession(20)
        readAllHeaders(fromDownloader)

        toDownloader.write("416 don't have it\r\n".bytes)
        toDownloader.write("X-Have: ${encodeXHave([0], 1)}\r\n\r\n".bytes)
        toDownloader.flush()

        Thread.sleep(150)
        assert performed
        assert available.contains(0)
        assert thrown == null
    }

    @Test
    public void test416Have2Pieces() {
        int pieceSize = FileHasher.getPieceSize(1)
        int size = (1 << pieceSize) + 1
        initSession(size)
        readAllHeaders(fromDownloader)

        toDownloader.write("416 don't have it\r\n".bytes)
        toDownloader.write("X-Have: ${encodeXHave([1], 2)}\r\n\r\n".bytes)
        toDownloader.flush()

        Thread.sleep(150)
        assert performed
        assert available.contains(1)
        assert thrown == null
    }

    @Test
    public void test200TwoPieces1Available() {
        int pieceSize = FileHasher.getPieceSize(1)
        int size = (1 << pieceSize) * 9 + 1
        initSession(size)

        Set<String> headers = readAllHeaders(fromDownloader)
        def matcher = null
        headers.each {
            if (it.startsWith("Range"))
                matcher = (it =~ /^Range: (\d+)-(\d+)$/)
        }
        assert matcher.groupCount() > 0
        int start = Integer.parseInt(matcher[0][1])
        int end = Integer.parseInt(matcher[0][2])

        if (start == 0)
            fail("inconlcusive")

        toDownloader.write("416 don't have it \r\n".bytes)
        toDownloader.write("X-Have: ${encodeXHave([0],2)}\r\n\r\n".bytes)
        toDownloader.flush()
        downloadThread.join()

        assert performed
        performed = false
        assert available.contains(0)
        assert thrown == null

        // request same session
        downloadThread = new Thread( { perform() } as Runnable)
        downloadThread.setDaemon(true)
        downloadThread.start()

        Thread.sleep(150)

        headers = readAllHeaders(fromDownloader)
        matcher = null
        headers.each {
            if (it.startsWith("Range"))
            matcher = (it =~ /^Range: (\d+)-(\d+)$/)
        }
        assert matcher.groupCount() > 0
        start = Integer.parseInt(matcher[0][1])
        end = Integer.parseInt(matcher[0][2])
        assert start == 0
    }

    @Test
    public void testXAlt() throws Exception {
        Personas personas = new Personas()
        def sources = []
        def listener = new Object() {
            public void onSourceDiscoveredEvent(SourceDiscoveredEvent e) {
                sources << e.source
            }
        }
        eventBus.register(SourceDiscoveredEvent.class, listener)

        initSession(20)
        readAllHeaders(fromDownloader)
        toDownloader.write("416 don't have it\r\n".bytes)
        toDownloader.write("X-Alt: ${personas.persona1.toBase64()},${personas.persona2.toBase64()}\r\n\r\n".bytes)
        toDownloader.flush()

        Thread.sleep(150)
        assert sources.contains(personas.persona1)
        assert sources.contains(personas.persona2)
        assert 2 == sources.size()
    }

    private static Set<String> readAllHeaders(InputStream is) {
        Set<String> rv = new HashSet<>()
        String header
        while((header = readTillRN(is)) != "")
            rv.add(header)
        rv
    }
}
