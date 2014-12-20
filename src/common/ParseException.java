/* Copyright 2014 Neil Edelman, distributed under the terms of the GNU General
 Public License, see copying.txt */

package common;

/** We are very stict about this.
 
 @author	Neil
 @version	1.1, 12-2014
 @since		1.1, 12-2014 */
public class ParseException extends Exception {
	/** @param in
		The TextReader that caused the exception.
	 @param why
		Guess why it happened. */
	public ParseException(final TextReader in, final String why) {
		super(why + "; line " + in.getLineNumber());
	}
}
