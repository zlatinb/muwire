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
package net.metanotionz.util.skiplist;

import java.io.Flushable;
import java.io.IOException;

//import net.metanotion.io.block.BlockFile;
@SuppressWarnings("unchecked")
public abstract class SkipSpan<K extends Comparable<? super K>, V> implements Flushable {
	/** This is actually limited by BlockFile.spanSize which is much smaller */
	public static final int MAX_SIZE = 256;

	public int nKeys = 0;
	public K[] keys;
	public V[] vals;
	public SkipSpan<K, V> next, prev;

	public abstract SkipSpan<K, V> newInstance(SkipList<K, V> sl);
	public abstract void killInstance() throws IOException;
	public abstract void flush();
	
	protected abstract void reload() throws IOException;
	
	private void loadVals() throws IOException {
		if (vals == null)
			reload();
	}

	private int binarySearch(K key) {
		int high = nKeys - 1;
		int low = 0;
		int cur;
		int cmp;
		while(low <= high) {
			cur = (low + high) >>> 1;
			cmp = keys[cur].compareTo(key);
			if(cmp > 0) {
				high = cur - 1;
			} else if(cmp < 0) {
				low = cur + 1;
			} else {
				return cur;
			}
		}
		return (-1 * (low + 1));
	}

	public SkipSpan<K, V> getEnd() {
		if(next == null) { return this; }
		return next.getEnd();
	}

	public SkipSpan<K, V> getSpan(K key, int[] search) {
		if(nKeys == 0) {
			search[0] = -1;
			return this;
		}

		if(keys[nKeys - 1].compareTo(key) < 0) {
			if(next == null) {
				search[0] = (-1 * (nKeys - 1)) - 1;
				return this;
			}
			return next.getSpan(key, search);
		}
		search[0] = binarySearch(key);
		return this;
	}

	public V get(K key) throws IOException {
		if(nKeys == 0) { return null; }
		if(keys[nKeys - 1].compareTo(key) < 0) {
			if(next == null) { return null; }
			return next.get(key);
		}
		int loc = binarySearch(key);
		if(loc < 0) { return null; }
		loadVals();
		V rv = vals[loc];
		vals = null;
		return rv;
	}

	private void pushTogether(int hole) throws IOException {
		loadVals();
		for(int i=hole;i<(nKeys - 1);i++) {
			keys[i] = keys[i+1];
			vals[i] = vals[i+1];
		}
		nKeys--;
	}

	private void pushApart(int start) throws IOException {
		loadVals();
		for(int i=(nKeys-1);i>=start;i--) {
			keys[i+1] = keys[i];
			vals[i+1] = vals[i];
		}
		nKeys++;
	}

	private void split(int loc, K key, V val, SkipList<K, V> sl) throws IOException {
		SkipSpan<K, V> right = newInstance(sl);

		if(this.next != null) { this.next.prev = right; }
		right.next = this.next;
		right.prev = this;
		this.next = right;

		loadVals();
		right.loadVals();
		int start = ((keys.length+1)/2);
		for(int i=start;i < keys.length; i++) {
			try {
				right.keys[i-start] = keys[i];
				right.vals[i-start] = vals[i];
				right.nKeys++;
				this.nKeys--;
			} catch (ArrayIndexOutOfBoundsException e) {
				System.out.println("i " + i + " start " + start);
				System.out.println("key: " + keys[i].toString());
				throw e;
			}
		}
		if(loc >= start) {
			right.pushApart(loc - start);
			right.keys[loc - start] = key;
			right.vals[loc - start] = val;
		} else {
			pushApart(loc);
			keys[loc] = key;
			vals[loc] = val;
		}
		this.flush();
		this.next.flush();
	}

	/**
	 *  @return the new span if it caused a split, else null if it went in this span
	 */
	private SkipSpan<K, V> insert(int loc, K key, V val, SkipList<K, V> sl) throws IOException {
		sl.addItem();
		if(nKeys == keys.length) {
			// split.
			split(loc, key, val, sl);
			return next;
		} else {
			pushApart(loc);
			keys[loc] = key;
			vals[loc] = val;
			this.flush();
			return null;
		}
	}

	/**
	 *  @return the new span if it caused a split, else null if it went in an existing span
	 */
	public SkipSpan<K, V> put(K key, V val, SkipList<K, V> sl)	throws IOException {
		if(nKeys == 0) {
			sl.addItem();
			vals = (V[]) (new Object[1]);
			keys[0] = key;
			vals[0] = val;
			nKeys++;
			this.flush();
			return null;
		}
		int loc = binarySearch(key);
		if(loc < 0) {
			loc = -1 * (loc + 1);
			if(next != null) {
				int cmp = next.firstKey().compareTo(key);
				if((loc >= nKeys) && (cmp > 0)) {
					// It fits in between this span and the next
					// Try to avoid a split...
					if(nKeys == keys.length) {
						if(next.nKeys == keys.length) {
							return insert(loc, key, val, sl);
						} else {
							return next.put(key, val, sl);
						}
					} else {
						return insert(loc, key, val, sl);
					}
				} else {
					// Its either clearly in the next span or this span.
					if(cmp > 0) {
						return insert(loc, key, val, sl);
					} else {
						return next.put(key, val, sl);
					}
				}
			} else {
				// There is no next span, So
				// either it goes here, or causes a split.
				return insert(loc, key, val, sl);
			}
		} else {
			// Key already exists. Overwrite value.
			loadVals();
			vals[loc] = val;
			this.flush();
			return null;
		}
	}

	/**
	 *  @return An array of two objects or null.
	 *          rv[0] is the removed object.
	 *          rv[1] is the deleted SkipSpan if the removed object was the last in the SkipSpan.
	 *          rv is null if no object was removed.
	 */
	public Object[] remove(K key, SkipList<K, V> sl) throws IOException {
		if(nKeys == 0) { return null; }
		if(keys[nKeys - 1].compareTo(key) < 0) {
			if(next == null) { return null; }
			return next.remove(key, sl);
		}
		int loc = binarySearch(key);
		if(loc < 0) { return null; }
		loadVals();
		Object o = vals[loc];
		Object[] res = new Object[2];
		res[0] = o;
		sl.delItem();
		if(nKeys == 1) {
			if((this.prev == null) && (this.next != null)) {
				this.next.loadVals();
				res[1] = this.next;
				// We're the first node in the list... copy the next node over and kill it. See also bottom of SkipLevels.java
				for(int i=0;i<next.nKeys;i++) {
					keys[i] = next.keys[i];
					vals[i] = next.vals[i];
				}

				nKeys = next.nKeys;
				//BlockFile.log.error("Killing next span " + next + ") and copying to this span " + this + " in remove of " + key);
				// Make us point to next.next and him point back to us
				SkipSpan<K, V> nn = next.next;
				next.killInstance();
				if (nn != null) {
					nn.prev = this;
					nn.flush();
				}
				this.next = nn;
				this.flush();
			} else {
				// Normal situation. We are now empty, kill ourselves
				//BlockFile.log.error("Killing this span " + this + ", prev " + this.prev + ", next " + this.next);
				if(this.prev != null) {
					this.prev.next = this.next;
					this.prev.loadVals();
					this.prev.flush();
				}
				if(this.next != null) {
					this.next.prev = this.prev;
					this.next.loadVals();
					this.next.flush();
					this.next = null;
				}
				if (this.prev != null) {
					// Kill ourselves
					this.prev = null;
					this.killInstance();
					res[1] = this;
				} else {
					// Never kill first span
					//BlockFile.log.error("Not killing First span, now empty!!!!!!!!!!!!!!!!!!");
					this.flush();
					res[1] = null;
				}
				nKeys = 0;
			}
		} else {
			pushTogether(loc);
			this.flush();
		}
		return res;
	}

	/** I2P */
	public K firstKey() {
		return keys[0];
	}
}
