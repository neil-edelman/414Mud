/* Copyright 2014 Neil Edelman, distributed under the terms of the GNU General
 Public License, see copying.txt */

package common;

import java.io.BufferedReader;
import java.io.LineNumberReader;
import java.lang.StringBuilder;
import java.io.IOException;
import java.util.NoSuchElementException;
import java.text.ParseException;

/** {@link LineNumberReader} (extends {@link BufferedReader}) that has special
 features for reading text settings painlessly.
 
 @author	Neil
 @version	1.1, 12-2014
 @since		1.1, 12-2014 */
public class TextReader extends LineNumberReader {
	/** Creates a new MudReader wrapping around BufferedReader.
	 @param in	The BufferedReader. */
	public TextReader(BufferedReader in) {
		super(in);
		setLineNumber(1);
	}
	/** Like {@link BufferedReader#readLine}, but throws an exception when
	 the line isn't there.
	 @return A string minus the newline.
	 @throws ParseException	The file has ended.
	 @throws IOException	Underlying readLine. */
	public String nextLine() throws ParseException, IOException {
		String line = this.readLine(); /* IOException */
		if(line == null) throw new ParseException("unexpected eof", getLineNumber());
		return line;
	}
	/** Like {@link #nextLine}, but throws an exception when asrt is not
	 exactly like the file.
	 @throws ParseException	The file has ended.
	 @throws IOException	Underlying nextLine. */
	public void assertLine(final String asrt) throws ParseException, IOException {
		String line = nextLine();
		if(asrt.compareTo(line) != 0) throw new ParseException("expected " + asrt, getLineNumber());
	}
	/** Like {@link #nextLine}, but reads all paragraph.
	 @return				The whole paragraph, minus newlines, as a string.
	 @throws ParseException	The file has ended before the paragraph was complete.
	 @throws IOException	Underlying nextLine. */
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
