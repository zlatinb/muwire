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
package net.metanotion.io.block.index;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import net.metanotion.io.block.BlockFile;
import net.metanotion.util.skiplist.SkipList;
import net.metanotion.util.skiplist.SkipLevels;
import net.metanotion.util.skiplist.SkipSpan;

import net.i2p.util.Log;

/**
 * On-disk format:
 *<pre>
 *    Magic number (long)
 *    max height (unsigned short)
 *    non-null height (unsigned short)
 *    span page (unsigned int)
 *    height number of level pages (unsigned ints)
 *</pre>
 *
 * Always fits on one page.
 */
public class BSkipLevels<K extends Comparable<? super K>, V> extends SkipLevels<K, V> {
	private static final long MAGIC = 0x42534c6576656c73l;  // "BSLevels"
	static final int HEADER_LEN = 16;
	public final int levelPage;
	public final int spanPage;
	public final BlockFile bf;
	private final BSkipList<K, V> bsl;
	private boolean isKilled;
	// the level pages, passed from the constructor to initializeLevels(),
	// NOT kept up to date
	private final int[] lps;

	/**
	 *  Non-recursive initializer initializeLevels()
	 *  MUST be called on the first BSkipLevel in the skiplist
	 *  after the constructor, unless it's a new empty
	 *  level and init() was previously called.
	 */
	@SuppressWarnings("unchecked")
	public BSkipLevels(BlockFile bf, int levelPage, BSkipList<K, V> bsl) throws IOException {
		this.levelPage = levelPage;
		this.bf = bf;
		this.bsl = bsl;

		BlockFile.pageSeek(bf.file, levelPage);
		long magic = bf.file.readLong();
		if (magic != MAGIC)
			throw new IOException("Bad SkipLevels magic number 0x" + Long.toHexString(magic) + " on page " + levelPage);

		bsl.levelHash.put(this.levelPage, this);

		int maxLen = bf.file.readUnsignedShort();
		int nonNull = bf.file.readUnsignedShort();
		if(maxLen < 1 || maxLen > MAX_SIZE || nonNull > maxLen)
			throw new IOException("Invalid Level Skip size " + nonNull + " / " + maxLen);
		spanPage = bf.file.readInt();
		bottom = bsl.spanHash.get(Integer.valueOf(spanPage));
		if (bottom == null) {
			// FIXME recover better?
			throw new IOException("No span found in cache???");
		}

		this.levels = (BSkipLevels<K, V>[]) new BSkipLevels[maxLen];
		// We have to read now because new BSkipLevels() will move the file pointer
		lps = new int[nonNull];
		for(int i = 0; i < nonNull; i++) {
			lps[i] = bf.file.readInt();
		}
	}

	/**
	 *  Non-recursive initializer.
	 *  MUST be called on the first BSkipLevel in the skiplist
	 *  after the constructor, unless it's a new empty
	 *  level and init() was previously called.
	 *  Only call on the first skiplevel in the list!
	 *
	 *  @since 0.9.20
	 */
	public void initializeLevels() {
		List<BSkipLevels<K, V>> toInit = new ArrayList<BSkipLevels<K, V>>(32);
		List<BSkipLevels<K, V>> nextInit = new ArrayList<BSkipLevels<K, V>>(32);
		initializeLevels(toInit);
		while (!toInit.isEmpty()) {
			for (BSkipLevels<K, V> bsl : toInit) {
				bsl.initializeLevels(nextInit);
			}
			List<BSkipLevels<K, V>> tmp = toInit;
			toInit = nextInit;
			nextInit = tmp;
			nextInit.clear();
		}
	}

	/**
	 *  Non-recursive initializer.
	 *  MUST be called after constructor.
	 *
	 *  @param nextInit out parameter, next levels to initialize
	 *  @since 0.9.20
	 */
	private void initializeLevels(List<BSkipLevels<K, V>> nextInit) {
		boolean fail = false;
		for(int i = 0; i < lps.length; i++) {
			int lp = lps[i];
			if(lp != 0) {
				levels[i] = bsl.levelHash.get(lp);
				if(levels[i] == null) {
					try {
						BSkipLevels<K, V> lev = new BSkipLevels<K, V>(bf, lp, bsl);
						levels[i] = lev;
						nextInit.add(lev);
					} catch (IOException ioe) {
						levels[i] = null;
						fail = true;
						continue;
					}
				}
				K ourKey = key();
				K nextKey = levels[i].key();
				if (ourKey != null && nextKey != null &&
						ourKey.compareTo(nextKey) >= 0) {
					// This will be fixed in blvlfix() via BlockFile.getIndex()
					//levels[i] = null;
					//fail = true;
				}
				// TODO also check that the level[] array is not out-of-order
			} else {
				levels[i] = null;
				fail = true;
			}
		}
		if (fail) {
			// corruption is actually fixed in blvlfix() via BlockFile.getIndex()
			// after instantiation is complete
			flush();
			// if the removed levels have no other links to them, they and their data
			// are lost forever, but the alternative is infinite loops / stack overflows
			// in SkipSpan.
		}
	}

	public static void init(BlockFile bf, int page, int spanPage, int maxHeight) throws IOException {
		BlockFile.pageSeek(bf.file, page);
		bf.file.writeLong(MAGIC);
		bf.file.writeShort((short) maxHeight);
		bf.file.writeShort(0);
		bf.file.writeInt(spanPage);
	}

	@Override
	public void flush() {
		if (isKilled) {
			return;
		}
		try {
			BlockFile.pageSeek(bf.file, levelPage);
			bf.file.writeLong(MAGIC);
			bf.file.writeShort((short) levels.length);
			int i = 0;
			for( ; i < levels.length; i++) {
				if(levels[i] == null)
					break;
			}
			bf.file.writeShort(i);
			bf.file.writeInt(((BSkipSpan<K, V>) bottom).page);
			for(int j = 0; j < i; j++) {
				bf.file.writeInt(((BSkipLevels<K, V>) levels[j]).levelPage);
			}
		} catch (IOException ioe) { throw new RuntimeException("Error writing to database", ioe); }
	}

	@Override
	public void killInstance() throws IOException {
		if (isKilled) {
			return;
		}
		isKilled = true;
		bsl.levelHash.remove(levelPage);
		bf.freePage(levelPage);
	}

	@Override
	public SkipLevels<K, V> newInstance(int levels, SkipSpan<K, V> ss, SkipList<K, V> sl) {
		try {
			BSkipSpan<K, V> bss = (BSkipSpan<K, V>) ss;
			BSkipList<K, V> bsl = (BSkipList<K, V>) sl;
			int page = bf.allocPage();
			BSkipLevels.init(bf, page, bss.page, levels);
			return new BSkipLevels<K, V>(bf, page, bsl);
			// do not need to call initLevels() here
		} catch (IOException ioe) { throw new RuntimeException("Error creating database page", ioe); }
	}


	/**
	 *  Breadth-first, sortof
	 *  We assume everything is findable from the root level
	 *  @param l non-null
	 *  @param lvlSet out parameter, the result
	 *  @since 0.8.8
	 */
	private void getAllLevels(SkipLevels<K, V> l, Set<SkipLevels<K, V>> lvlSet) {
		// Do level 0 without recursion, on the assumption everything is findable
		// from the root
		SkipLevels<K, V> cur = l;
		while (cur != null && lvlSet.add(cur)) {
			cur = cur.levels[0];
		}
		// If there were no nulls at level 0 in the middle,
		// i.e. there are no problems, this won't find anything
		for (int i = 1; i < l.levels.length; i++) {
			SkipLevels<K, V> lv = l.levels[i];
			if (lv != null && !lvlSet.contains(lv))
				getAllLevels(lv, lvlSet);
		}
	}

	/**
	 *  For sorting levels in blvlfix()
	 *  Sorts in REVERSE order.
	 *  @since 0.8.8
	 */
	private static class LevelComparator<K extends Comparable<? super K>, V> implements Comparator<SkipLevels<K, V>>, Serializable {
		public int compare(SkipLevels<K, V> l, SkipLevels<K, V> r) {
			K lk = l.key();
			K rk = r.key();
			if (lk == null && rk == null)
				return 0;
			if (lk == null)
				return 1;
			if (rk == null)
				return -1;
			// reverse!
			return rk.compareTo(lk);
		}
	}
}
