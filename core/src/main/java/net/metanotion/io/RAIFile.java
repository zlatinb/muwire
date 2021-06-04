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
package net.metanotion.io;

import com.muwire.core.util.DataUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.Set;

public class RAIFile implements RandomAccessInterface {
	
	private static final long MAX_SIZE = 0x1 << 29;
	
	private File f;
	private final ByteBuffer byteBuffer;
	private final FileChannel fileChannel;
			
	private boolean r=false, w=false;
	
	private int maxPosition = 2048; // PAGESIZE * 2

	public RAIFile(RandomAccessFile file) throws IOException {
		this.f = null;
		fileChannel = file.getChannel();
		byteBuffer = fileChannel.map(FileChannel.MapMode.READ_WRITE, 0, MAX_SIZE);
	}

	public RAIFile(File file, boolean read, boolean write) throws FileNotFoundException, IOException {
		this.f = file;
		this.r = read;
		this.w = write;
		Set<OpenOption> openOptionSet = new HashSet<>();
		if(this.r) { openOptionSet.add(StandardOpenOption.READ); }
		if(this.w) { openOptionSet.add(StandardOpenOption.WRITE); }
		fileChannel = (FileChannel) Files.newByteChannel(f.toPath(), openOptionSet);
		byteBuffer = fileChannel.map(FileChannel.MapMode.READ_WRITE,0, MAX_SIZE);
	}
	
	private void updateMaxPosition() {
		maxPosition = Math.max(maxPosition, byteBuffer.position());
	}

	public long getFilePointer()		throws IOException { return byteBuffer.position(); }
	public long length()				throws IOException { return maxPosition; }
	public void setLength(long length) {
		maxPosition = (int)length;
	}
	public int read()					throws IOException { return byteBuffer.get(); }
	public int read(byte[] b)			throws IOException { byteBuffer.get(b); return b.length; }
	public int read(byte[] b, int off, int len) throws IOException { byteBuffer.get(b,off,len); return len; }
	public void seek(long pos)			throws IOException { byteBuffer.position((int)pos); updateMaxPosition();}

	// Closeable Methods
	// TODO May need to change.
	public void close()	throws IOException { 
		fileChannel.close();
		DataUtil.tryUnmap(byteBuffer);
	}

	// DataInput Methods
	public boolean readBoolean()		throws IOException { return byteBuffer.get() == (byte)1; }
	public byte readByte()				throws IOException { return byteBuffer.get(); }
	public char readChar()				throws IOException { return byteBuffer.getChar(); }
	public double readDouble()			throws IOException { return byteBuffer.getDouble(); }
	public float readFloat()			throws IOException { return byteBuffer.getFloat(); }
	public void readFully(byte[] b)		throws IOException { byteBuffer.get(b); }
	public void readFully(byte[] b, int off, int len) throws IOException { byteBuffer.get(b,off,len); }
	public int readInt()				throws IOException { return byteBuffer.getInt(); }
	public long readLong()				throws IOException { return byteBuffer.getLong(); }
	public short readShort()			throws IOException { return byteBuffer.getShort(); }
	public int readUnsignedByte()		throws IOException { return byteBuffer.get() & 0xFF; }
	public int readUnsignedShort()		throws IOException { return byteBuffer.getShort() & 0xFFFF; }

	/** Read a UTF encoded string
	 	I would delegate here. But Java's read/writeUTF combo suck.
	 	A signed 2 byte length is not enough.
	 	This reads a 4 byte length.
	 	The upper byte MUST be zero, if its not, then its not this method and has used an
	 	extensible length encoding.
	 	This is followed by the bytes of the UTF encoded string, as
	 	returned by String.getBytes("UTF-8");
	*/
	public String readUTF()				throws IOException {
		int len = byteBuffer.getInt();
		if((len < 0) || (len >= 16777216)) { throw new IOException("Bad Length Encoding"); }
		byte[] bytes = new byte[len];
		byteBuffer.get(bytes);
		String s = new String(bytes, StandardCharsets.UTF_8);
		return s;
	}

	public int skipBytes(int n) throws IOException { 
		byteBuffer.position(byteBuffer.position() + n);
		updateMaxPosition();
		return n;
	}

	// DataOutput Methods
	public void write(int b)			throws IOException { byteBuffer.put((byte)b); updateMaxPosition(); }
	public void write(byte[] b)			throws IOException { byteBuffer.put(b); updateMaxPosition();}
	public void write(byte[] b, int off, int len) throws IOException { byteBuffer.put(b,off,len); updateMaxPosition();}
	public void writeBoolean(boolean v)	throws IOException { byteBuffer.put(v ? (byte)1 : (byte)0); updateMaxPosition();}
	public void writeByte(int v)		throws IOException { byteBuffer.put((byte)v); updateMaxPosition();}
	public void writeShort(int v)		throws IOException { byteBuffer.putShort((short)v); updateMaxPosition();}
	public void writeChar(int v)		throws IOException { byteBuffer.putChar((char)v); updateMaxPosition();}
	public void writeInt(int v)			throws IOException { byteBuffer.putInt(v); updateMaxPosition();}
	public void writeLong(long v)		throws IOException { byteBuffer.putLong(v); updateMaxPosition();}
	public void writeFloat(float v)		throws IOException { byteBuffer.putFloat(v); updateMaxPosition();}
	public void writeDouble(double v)	throws IOException { byteBuffer.putDouble(v); updateMaxPosition();}
	public void writeBytes(String s)	throws IOException { byteBuffer.put(s.getBytes()); updateMaxPosition();}

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
		if(string.length >= 16777216) { throw new IOException("String to long for encoding type"); }
		byteBuffer.putInt(string.length);
		byteBuffer.put(string);
		updateMaxPosition();
	}
}
