/* Copyright 2014 Neil Edelman, distributed under the terms of the GNU General
 Public License, see copying.txt */

package common;

/** The {@link ParseException} requires more information then is available at
 this level; we will not worry about it and let the next level deal.
 
 @author	Neil
 @version	1.1, 12-2014
 @since		1.1, 12-2014 */
public class UnrecognisedTokenException extends Exception {
	/** @param why	Guess why it happened. */
	public UnrecognisedTokenException(final String why) {
		super(why);
	}
}
