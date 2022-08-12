/*
Copyright (c) 2006, Matthew Estes
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

	* Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.
	* Redistributions in binary form must reproduce the above copyright
notice, this list of conditions and the following disclaimer in the
documentation and/or other materials provided with the distribution.
	* Neither the name of Metanotion Software nor the names of its
contributors may be used to endorse or promote products derived from this
software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package net.metanotionz.io;

import com.muwire.core.util.DataUtil;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

public class RAIFile implements RandomAccessInterface {
	
	static final long MAX_SIZE = 0x1 << 26;
	
	private final File dir;
	private final String prefix;

	private final List<FileChunk> chunkList = new ArrayList<>();
	
	private FileChunk current;
	private int maxPosition;

	public RAIFile(File dir, String prefix) throws IOException {
		this.dir = dir;
		this.prefix = prefix;
		chunkList.add(new FileChunk(0, dir, prefix));
		current = chunkList.get(0);
	}
	
	private static class FileChunk {
		private final File file;
		private final int index;
		private final ByteBuffer byteBuffer;
		private final FileChannel fileChannel;
		
		FileChunk(int index, File dir, String prefix) throws IOException {
			this.index = index;
			file = new File(dir, prefix + "." + index);
			file.createNewFile();
			file.deleteOnExit();
			fileChannel = (FileChannel) Files.newByteChannel(file.toPath(), StandardOpenOption.READ, StandardOpenOption.WRITE);
			byteBuffer = fileChannel.map(FileChannel.MapMode.READ_WRITE, 0, MAX_SIZE);
		}
		
		long position() {
			return index * MAX_SIZE + byteBuffer.position();
		}
		
		void close() throws IOException {
			fileChannel.close();
			DataUtil.tryUnmap(byteBuffer);
			file.delete();
		}
	}
	
	private void switchChunk(long position) throws IOException {
		int idx = (int) (position / MAX_SIZE);
		while(chunkList.size() <= idx)
			chunkList.add(new FileChunk(chunkList.size(), dir, prefix));
		current = chunkList.get(idx);
		current.byteBuffer.position((int)(position % MAX_SIZE));
	}
	
	private void ensureCapacity(int size) throws IOException {
		int remaining = current.byteBuffer.remaining();
		if (remaining < size) 
			switchChunk(current.position() + remaining);
	}
	
	private void updateMaxPosition() {
		maxPosition = (int) Math.max(maxPosition, current.position());
	}

	public long getFilePointer()		throws IOException { return current.position(); }
	public long length()				throws IOException { return maxPosition; }
	public void setLength(long length) {
		maxPosition = (int)length;
	}
	
	public int read() throws IOException {
		if (current.byteBuffer.remaining() < 1)
			switchChunk(current.position() + 1);
		return current.byteBuffer.get(); 
	}
	
	public int read(byte[] b) throws IOException { 
		return read(b, 0, b.length); 
	}
	
	public int read(byte[] b, int off, int len) throws IOException {
		int remaining = current.byteBuffer.remaining();
		int rv = 0;
		while (remaining < len) {
			current.byteBuffer.get(b, off, remaining);
			off += remaining;
			len -= remaining;
			rv += remaining;
			switchChunk(current.position());
			remaining = current.byteBuffer.remaining();
		}
		current.byteBuffer.get(b, off, len);
		return rv + len;
	}
	public void seek(long pos) throws IOException { 
		switchChunk(pos); 
		updateMaxPosition();
	}

	// Closeable Methods
	public void close()	throws IOException { 
		for (FileChunk chunk : chunkList)
			chunk.close();
	}

	// DataInput Methods
	public boolean readBoolean() throws IOException {
		ensureCapacity(1);
		return current.byteBuffer.get() == (byte)1; 
	}
	
	public byte readByte() throws IOException {
		ensureCapacity(1);
		return current.byteBuffer.get(); 
	}
	
	public char readChar() throws IOException {
		ensureCapacity(2);
		return current.byteBuffer.getChar(); 
	}
	
	public double readDouble() throws IOException {
		ensureCapacity(8);
		return current.byteBuffer.getDouble(); 
	}
	
	public float readFloat() throws IOException {
		ensureCapacity(4);
		return current.byteBuffer.getFloat(); 
	}
	
	public void readFully(byte[] b)		throws IOException { read(b); }
	public void readFully(byte[] b, int off, int len) throws IOException { read(b,off,len); }
	
	
	public int readInt() throws IOException {
		ensureCapacity(4);
		return current.byteBuffer.getInt(); 
	}
	
	public long readLong()	throws IOException {
		ensureCapacity(8);
		return current.byteBuffer.getLong(); 
	}
	
	public short readShort() throws IOException {
		ensureCapacity(2);
		return current.byteBuffer.getShort(); 
	}
	public int readUnsignedByte()		throws IOException { return read() & 0xFF; }
	public int readUnsignedShort()		throws IOException { return readShort() & 0xFFFF; }

	/** Read a UTF encoded string
	 	I would delegate here. But Java's read/writeUTF combo suck.
	 	A signed 2 byte length is not enough.
	 	This reads a 4 byte length.
	 	The upper byte MUST be zero, if its not, then its not this method and has used an
	 	extensible length encoding.
	 	This is followed by the bytes of the UTF encoded string, as
	 	returned by String.getBytes("UTF-8");
	*/
	public String readUTF()	throws IOException {
		ensureCapacity(4);
		int len = current.byteBuffer.getInt();
		if((len < 0) || (len >= MAX_SIZE)) { throw new IOException("Bad Length Encoding"); }
		byte[] bytes = new byte[len];
		
		int remaining = current.byteBuffer.remaining();
		int offset = 0;
		if (remaining < len) {
			current.byteBuffer.get(bytes, 0, remaining);
			len -= remaining;
			offset += remaining;
			switchChunk(current.position());
		}
		current.byteBuffer.get(bytes, offset, len );
		String s = new String(bytes, StandardCharsets.UTF_8);
		return s;
	}

	public int skipBytes(int n) throws IOException { 
		long desiredPosition = current.position() + n;
		switchChunk(desiredPosition);
		updateMaxPosition();
		return n;
	}

	// DataOutput Methods
	public void write(int b) throws IOException {
		ensureCapacity(1);
		current.byteBuffer.put((byte)b); 
		updateMaxPosition(); 
	}
	
	public void write(byte[] b)	throws IOException { write(b, 0, b.length);}
	
	public void write(byte[] b, int off, int len) throws IOException {
		int remaining = current.byteBuffer.remaining();
		while (remaining < len) {
			current.byteBuffer.put(b, off, remaining);
			off += remaining;
			len -= remaining;
			switchChunk(current.position());
			remaining = current.byteBuffer.remaining();
		}
		current.byteBuffer.put(b, off, len);
		updateMaxPosition();
	}
	
	public void writeBoolean(boolean v)	throws IOException {
		ensureCapacity(1);
		current.byteBuffer.put(v ? (byte)1 : (byte)0); 
		updateMaxPosition();
	}
	
	public void writeByte(int v) throws IOException {
		ensureCapacity(1);
		current.byteBuffer.put((byte)v); 
		updateMaxPosition();
	}
	
	public void writeShort(int v) throws IOException {
		ensureCapacity(2);
		current.byteBuffer.putShort((short)v); 
		updateMaxPosition();
	}
	
	public void writeChar(int v) throws IOException {
		ensureCapacity(2);
		current.byteBuffer.putChar((char)v); 
		updateMaxPosition();
	}
	
	public void writeInt(int v) throws IOException {
		ensureCapacity(4);
		current.byteBuffer.putInt(v); 
		updateMaxPosition();
	}
	
	public void writeLong(long v) throws IOException {
		ensureCapacity(8);
		current.byteBuffer.putLong(v); 
		updateMaxPosition();
	}
	
	public void writeFloat(float v)		throws IOException {
		ensureCapacity(4);
		current.byteBuffer.putFloat(v); 
		updateMaxPosition();
	}
	
	public void writeDouble(double v) throws IOException {
		ensureCapacity(8);
		current.byteBuffer.putDouble(v); 
		updateMaxPosition();
	}
	
	public void writeBytes(String s) throws IOException { write(s.getBytes());}

	/** Write a UTF encoded string
	 	I would delegate here. But Java's read/writeUTF combo suck.
	 	A signed 2 byte length is not enough.
	 	This writes a 4 byte length.
	 	The upper byte MUST be zero, if its not, then its not this method and has used an
	 	extensible length encoding.
	 	This is followed by the bytes of the UTF encoded string, as
	 	returned by String.getBytes("UTF-8");
	*/
	public void writeUTF(String str)	throws IOException {
		byte[] string = str.getBytes(StandardCharsets.UTF_8);
		if(string.length >= MAX_SIZE) { throw new IOException("String to long for encoding type"); }
		ensureCapacity(4);
		current.byteBuffer.putInt(string.length);
		write(string);
	}
}
