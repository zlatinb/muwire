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

import java.util.Random;

public class SkipList {
	protected SkipSpan first;
	protected SkipLevels stack;
	public Random rng;

	public int size=0;
	public int spans=0;

	public void flush() { }
	protected SkipList() { }

	public int size() { return size; }

	public int maxLevels() {
		int hob = 0, s = spans;
		while(spans > 0) {
			hob++;
			spans = spans / 2;
		}
		return (hob > 4) ? hob : 4;
	}

	public int generateColHeight() {
		int bits = rng.nextInt();
		boolean cont = true;
		int res=0;
		for(res=0; cont; res++) {
			cont = ((bits % 2) == 0) ? true : false;
			bits = bits / 2;
		}
		return Math.max(0, Math.min(res, maxLevels()));
	}

	public void put(Comparable key, Object val)	{
		if(key == null) { throw new NullPointerException(); }
		if(val == null) { throw new NullPointerException(); }
		SkipLevels slvls = stack.put(stack.levels.length - 1, key, val, this);
		if(slvls != null) {
			SkipLevels[] levels = new SkipLevels[slvls.levels.length];
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

	public Object remove(Comparable key) {
		if(key == null) { throw new NullPointerException(); }
		Object[] res = stack.remove(stack.levels.length - 1, key, this);
		if(res != null) {
			if(res[1] != null) {
				SkipLevels slvls = (SkipLevels) res[1];
				for(int i=0;i < slvls.levels.length; i++) {
					if(stack.levels[i] == slvls) {
						stack.levels[i] = slvls.levels[i];
					}
				}
				stack.flush();
			}
			flush();
			return res[0];
		}
		return null;
	}

	public void printSL() {
		System.out.println("List size " + size + " spans " + spans);
		stack.print();
	}

	public void print() {
		System.out.println("List size " + size + " spans " + spans);
		first.print();
	}

	public Object get(Comparable key) {
		if(key == null) { throw new NullPointerException(); }
		return stack.get(stack.levels.length - 1, key);
	}

	public SkipIterator iterator() { return new SkipIterator(first, 0); }

	public SkipIterator min() { return new SkipIterator(first, 0); }

	public SkipIterator max() {
		SkipSpan ss = stack.getEnd();
		return new SkipIterator(ss, ss.nKeys - 1);
	}

	public SkipIterator find(Comparable key) {
		int[] search = new int[1];
		SkipSpan ss = stack.getSpan(stack.levels.length - 1, key, search);
		if(search[0] < 0) { search[0] = -1 * (search[0] + 1); }
		return new SkipIterator(ss, search[0]);
	}


	// Levels adjusted to guarantee O(log n) search
	// This is expensive proportional to the number of spans.
	public void balance() {
		// TODO Skip List Balancing Algorithm
	}
}
