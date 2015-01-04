/* Copyright 2015 Neil Edelman, distributed under the terms of the GNU General
 Public License, see copying.txt */

package main;

import java.io.IOException;
import java.text.ParseException;

import common.TextReader;
import main.Mud;

/** One of the damage types.
 
 @author	Neil
 @version	1.1, 2015-01
 @since		1.1, 2015-01 */
public class Damage {
	String aggressor, bystander, victim;
	public Damage(final TextReader in) throws ParseException, IOException {
		aggressor = in.readLine();
		bystander = in.readLine();
		victim    = in.readLine();
		if(in.readLine() != null) throw new ParseException("expecting EOF", in.getLineNumber());
		if(Mud.isVerbose) System.err.printf("Damage: %s, %s, %s.\n", aggressor, bystander, victim);
	}
}
