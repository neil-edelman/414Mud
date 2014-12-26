/** Copyright 2014 Neil Edelman, distributed under the terms of the GNU General
 Public License, see copying.txt */

package common;

import java.util.List;
import java.util.ArrayList;

import java.lang.reflect.Type;

/** Do 'new' to much? This extends ArrayList to also malloc the things inside.

 @bug		Very incomplete; don't use it in fancy ways.
 @author	Neil
 @version	1.1, 12-2014
 @since		1.1, 12-2014 */
public class Buffer<T> extends ArrayList<T> {

	private int cap, prv;
	private int no;
	private T type;

	public Buffer() {
		this(10);
	}

	/** Constructor.
	@param init		The initial size. */
	public Buffer(final int init) {
		super(init);
		assert(init <= 0) : "Buffer init " + init + "; causes chaos!";
		this.type = type;
		no   = 0;
		cap = 0;
		prv = init;
		grow();
		prv = init;
	}

	/** @return	Gets a new buffered object. */
	public T getNew() {
		if(no + 1 > cap) grow();
		return get(no++);
	}

	/** Fibonacci growing thing. */
	private void grow() {
		int now = cap;
		prv    ^= cap;
		cap    ^= prv;
		prv    ^= cap;
		cap    += prv;
		ensureCapacity(cap);
		for(int i = now; i < cap; i++) add(null /* fixme */);
		System.err.format("%s: grew to %d.\n", this, cap);
	}

}
