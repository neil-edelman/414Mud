/* Copyright 2014 Neil Edelman, distributed under the terms of the GNU General
 Public License, see copying.txt */

package common;

import java.io.BufferedReader;
import java.io.IOException;

/** For reading from sockets.
 
 @author	Neil
 @version	1.1, 12-2014
 @since		1.1, 12-2014 */
public class BoundedReader extends BufferedReader {

	private static final int IAC = 255;

	private final int  bufferSize;
	private       char buffer[];
	/*boolean            isLastIac;*/

	/**
	 @param in			The {@link BufferedReader} that this is wrapped around.
	 @param bufferSize	The maximum characters to read. */
	public BoundedReader(final BufferedReader in, final int bufferSize) {
		super(in);
		this.bufferSize = bufferSize;
		this.buffer     = new char[bufferSize];
	}

	/** Read a line up to {@link #bufferSize} and discard the rest.
	 @return {@link String#trim()} version of a String. */
	@Override
	public String readLine() throws IOException {
		int no;

		/* read up to bufferSize */
		if((no = this.read(buffer, 0, bufferSize)) == -1) return null;
		String input = new String(buffer, 0, no).trim();
		/*if(no > 0 && buffer[0] == IAC) {
			isLastIac = true;
		} else {
			isLastIac = false;
			input.trim();
		}*/

		/* skip the rest */
		while(no >= bufferSize && ready()) {
			if((no = read(buffer, 0, bufferSize)) == -1) break;
			// no = skip(bufferSize); ??
		}

		//isNewLine = true;
		return input;
	}

	/**
	 @return Whether the last input should be "Interpret as command" per
	 Telnet. */
	/*public boolean isIac() { return isLastIac; }*/

}
