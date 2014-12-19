/** Copyright 2014 Sid Gandhi and Neil Edelman, distributed under the terms of
 the GNU General Public License, see copying.txt
 
 For reading from sockets.
 
 @author Neil
 @version 1.1
 @since 2014 */

package main;

import java.io.BufferedReader;
import java.io.IOException;

public class BoundedReader extends BufferedReader {

	private final int  bufferSize;
	private       char buffer[];

	BoundedReader(final BufferedReader in, final int bufferSize) {
		super(in);
		this.bufferSize = bufferSize;
		this.buffer     = new char[bufferSize];
	}

	@Override
	public String readLine() throws IOException {
		int no;

		/* read up to bufferSize */
		if((no = this.read(buffer, 0, bufferSize)) == -1) return null;
		String input = new String(buffer, 0, no).trim();

		/* skip the rest */
		while(no >= bufferSize && ready()) {
			if((no = read(buffer, 0, bufferSize)) == -1) break;
		}

		return input;
	}

}
