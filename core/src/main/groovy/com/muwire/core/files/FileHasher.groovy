package com.muwire.core.files

import com.muwire.core.InfoHash
import com.muwire.core.util.DataUtil

import net.i2p.data.Base64

import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.nio.channels.FileChannel.MapMode
import java.nio.channels.FileLock
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

class FileHasher {

    public static final int MIN_PIECE_SIZE_POW2 = 17
    public static final int MAX_PIECE_SIZE_POW2 = 37
    /** max size of shared file is 128 GB */
    public static final long MAX_SIZE = 0x1L << MAX_PIECE_SIZE_POW2

    /**
     * @param size of the file to be shared
     * @return the size of each piece in power of 2
     * piece size is minimum 128 KBytees and maximum 16 MBytes in power of 2 steps (2^17 - 2^24)
     * there can be up to 8192 pieces maximum per file
     */
    static int getPieceSize(long size) {
        if (size <= 0x1 << 30)
            return MIN_PIECE_SIZE_POW2

        for (int i = 31; i <= MAX_PIECE_SIZE_POW2; i++) {
            if (size <= 0x1L << i) {
                return i-13
            }
        }

        throw new IllegalArgumentException("File too large $size")
    }

    final MessageDigest digest

    FileHasher() {
        try {
            digest = MessageDigest.getInstance("SHA-256")
        } catch (NoSuchAlgorithmException impossible) {
            digest = null
            System.exit(1)
        }
    }

    InfoHash hashFile(File file) {
        final long length = file.length()
        final long size = 0x1L << getPieceSize(length)
        int numPieces = (length / size).toInteger()
        if (numPieces * size < length)
            numPieces++

        def output = new ByteArrayOutputStream()
        RandomAccessFile raf = new RandomAccessFile(file, "r")
        MappedByteBuffer buf = null
        
        try(FileLock lock = raf.getChannel().lock(0, Long.MAX_VALUE, true)) {
            for (int i = 0; i < numPieces - 1; i++) {
                buf = raf.getChannel().map(MapMode.READ_ONLY, size * i, size.toInteger())
                digest.update buf
                DataUtil.tryUnmap(buf)
                output.write(digest.digest(), 0, 32)
            }
            long lastPieceLength = length - (numPieces - 1) * size
            buf = raf.getChannel().map(MapMode.READ_ONLY, length - lastPieceLength, lastPieceLength.toInteger())
            digest.update buf
            output.write(digest.digest(), 0, 32)
        } finally {
            raf.close()
            DataUtil.tryUnmap(buf)
        }

        byte [] hashList = output.toByteArray()
        InfoHash.fromHashList(hashList)
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            println "This utility computes an infohash of a file"
            println "Pass absolute path to a file as an argument"
            System.exit(1)
        }

        def file = new File(args[0])
        file = file.getAbsoluteFile()
        def hasher = new FileHasher()
        def infohash = hasher.hashFile(file)
        println Base64.encode(infohash.getRoot())
    }
}
