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
package net.metanotion.util.skiplist;

import java.io.Flushable;
import java.io.IOException;
import java.util.Random;

//import net.metanotion.io.block.BlockFile;

public abstract class SkipList<K extends Comparable<? super K>, V> implements Flushable, Iterable<V> {
	/** the probability of each next higher level */
	protected static final int P = 2;
	private static final int MIN_SLOTS = 4;
	// these two are really final
	protected SkipSpan<K, V> first;
	protected SkipLevels<K, V> stack;
	public static final Random rng = new Random(System.currentTimeMillis());

	protected int size;

	public abstract void flush() throws IOException;


	public int size() { return size; }

	public void addItem() {
		size++;
	}

	public void delItem() {
		if (size > 0)
			size--;
	}

	/**
	 *  @return 4 since we don't track span count here any more - see override
	 *  Fix if for some reason you want a huge in-memory skiplist.
	 */
	public int maxLevels() {
		return MIN_SLOTS;
	}

	/**
	 *  @return 0..maxLevels(), each successive one with probability 1 / P
	 */
	public int generateColHeight() {
		int bits = rng.nextInt();
		int max = maxLevels();
		for(int res = 0; res < max; res++) {
			if (bits % P == 0)
				return res;
			bits /= P;
		}
		return max;
	}

	@SuppressWarnings("unchecked")
	public void put(K key, V val) throws IOException {
		if(key == null) { throw new NullPointerException(); }
		if(val == null) { throw new NullPointerException(); }
		SkipLevels<K, V> slvls = stack.put(stack.levels.length - 1, key, val, this);
		if(slvls != null) {
			// grow our stack
			//BlockFile.log.info("Top level old hgt " + stack.levels.length +  " new hgt " + slvls.levels.length);
			SkipLevels<K, V>[] levels = (SkipLevels<K, V>[]) new SkipLevels[slvls.levels.length];
			for(int i=0;i < slvls.levels.length; i++) {
				if(i < stack.levels.length) {
					levels[i] = stack.levels[i];
				} else {
					levels[i] = slvls;
				}
			}
			stack.levels = levels;
			stack.flush();
			flush();
		}
	}

	@SuppressWarnings("unchecked")
	public V remove(K key) throws IOException {
		if(key == null) { throw new NullPointerException(); }
		Object[] res = stack.remove(stack.levels.length - 1, key, this);
		if(res != null) {
			if(res[1] != null) {
				SkipLevels<K, V> slvls = (SkipLevels<K, V>) res[1];
				for(int i=0;i < slvls.levels.length; i++) {
					if(stack.levels[i] == slvls) {
						stack.levels[i] = slvls.levels[i];
					}
				}
				stack.flush();
			}
			flush();
			return (V) res[0];
		}
		return null;
	}

	public V get(K key) throws IOException {
		if(key == null) { throw new NullPointerException(); }
		return stack.get(stack.levels.length - 1, key);
	}

	public SkipIterator<K, V> iterator() { return new SkipIterator<K, V>(first, 0); }

	/** @return an iterator where nextKey() is the first one greater than or equal to 'key' */
	public SkipIterator<K, V> find(K key) {
		int[] search = new int[1];
		SkipSpan<K, V> ss = stack.getSpan(stack.levels.length - 1, key, search);
		if(search[0] < 0) { search[0] = -1 * (search[0] + 1); }
		return new SkipIterator<K, V>(ss, search[0]);
	}

	// Levels adjusted to guarantee O(log n) search
	// This is expensive proportional to the number of spans.
	public void balance() {
		// TODO Skip List Balancing Algorithm
	}
}
