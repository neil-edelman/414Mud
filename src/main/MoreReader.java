/** Copyright 2014 Sid Gandhi and Neil Edelman, distributed under the terms of
 the GNU General Public License, see copying.txt
 
 I was lazy, LineNumberReader always checking for error conditions; made my own.
 
 @author Neil
 @version 1.1
 @since 2014 */

package main;

import java.io.BufferedReader;
import java.io.LineNumberReader;
import java.lang.StringBuilder;
import java.io.IOException;
import java.util.NoSuchElementException;

public class MoreReader extends LineNumberReader {
	/** Creates a new MudReader wrapping around BufferedReader.
	 @param in
	 The BufferedReader. */
	public MoreReader(BufferedReader in) {
		super(in);
		setLineNumber(1);
	}
	/** Like {@see BufferedReader#readLine}, but throws an exception when
	 the line isn't there.
	 @throws ParseException
		The file has ended.
	 @throws IOException
		Underlying readLine.
	 @return A string minus the newline. */
	public String nextLine() throws ParseException, IOException {
		String line = this.readLine(); /* IOException */
		if(line == null) throw new ParseException(this, "unexpected eof");
		return line;
	}
	/** Like {@see #nextLine}, but throws an exception when asrt is not
	 exactly like the file.
	 @throws ParseException
		The file has ended.
	 @throws IOException
		Underlying nextLine. */
	public void assertLine(final String asrt) throws ParseException, IOException {
		String line = nextLine();
		if(asrt.compareTo(line) != 0) throw new ParseException(this, "expected " + asrt);
	}
	/** Like {@see #nextLine}, but reads all paragraph.
	 @throws ParseException
		The file has ended before the paragraph was complete.
	 @throws IOException
		Underlying nextLine.
	 @return The whole paragraph, minus newlines, as a string. */
	public String nextParagraph() throws ParseException, IOException {
		boolean       isFirst = true;
		StringBuilder sb = new StringBuilder(256);
		String        str;
		for( ; ; ) {
			if((str = nextLine()).length() <= 0) break;
			if(isFirst) {
				isFirst = false;
			} else {
				sb.append(" ");
			}
			sb.append(str);
		}
		return sb.toString();
	}

}

/** We are very stict about this. */
class ParseException extends Exception {
	/** @param in
		The MoreReader that caused the exception.
	 @param why
		Guess why it happened. */
	ParseException(final MoreReader in, final String why) {
		super(why + "; line " + in.getLineNumber());
	}
}
