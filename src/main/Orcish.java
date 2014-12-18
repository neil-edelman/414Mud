/** Copyright 2014 Sid Gandhi and Neil Edelman, distributed under the terms of
 the GNU General Public License, see copying.txt
 
 Tolkienised random words are useful:
 @link{http://en.wikipedia.org/wiki/Languages_constructed_by_J._R._R._Tolkien}.
 
 @author Neil
 @version 1.1
 @since 2014 */

package main;

import java.util.Random;

public class Orcish {

	private static final String sylables[] = {
		"ub", "ul", "uk", "um", "uu", "oo", "ee", "uuk", "uru",
		"ick", "gn", "ch", "ar", "eth", "ith", "ath", "uth", "yth",
		"ur", "uk", "ug", "sna", "or", "ko", "uks", "ug", "lur", "sha", "grat",
		"mau", "eom", "lug", "uru", "mur", "ash", "goth", "sha", "cir", "un",
		"mor", "ann", "sna", "gor", "dru", "az", "azan", "nul", "biz", "balc",
		"balc", "tuo", "gon", "dol", "bol", "dor", "luth", "bolg", "beo",
		"vak", "bat", "buy", "kham", "kzam", "lg", "bo", "thi",
		"ia", "es", "en", "ion",
		"mok", "muk", "tuk", "gol", "fim", "ette", "moor", "goth", "gri",
		"shn", "nak", "ash", "bag", "ronk", "ask", "mal", "ome", "hi",
		"sek", "aah", "ove", "arg", "ohk", "to", "lag", "muzg", "ash", "mit",
		"rad", "sha", "saru", "ufth", "warg", "sin", "dar", "ann", "mor", "dab",
		"val", "dur", "dug", "bar",
		"ash", "krul", "gakh", "kraa", "rut", "udu", "ski", "kri", "gal",
		"nash", "naz", "hai", "mau", "sha", "akh", "dum", "olog", "lab", "lat"
	};

	private static final String suffixes[] = {
		"at", "ob", "agh", "uk", "uuk", "um", "uurz", "hai", "ishi", "ub",
		"ull", "ug", "an", "hai", "gae", "-hai", "luk", "tz", "hur", "dush",
		"ks", "mog", "grat", "gash", "th", "on", "gul", "gae", "gun",
		"dan", "og", "ar", "meg", "or", "lin", "dog", "ath", "ien", "rn", "bul",
		"bag", "ungol", "mog", "nakh", "gorg", "-dug", "duf", "ril", "bug",
		"snaga", "naz", "gul", "ak", "kil", "ku", "on", "ritz", "bad", "nya",
		"durbat", "durb", "kish", "olog", "-atul", "burz", "puga", "shar",
		"snar", "hai", "ishi", "uruk", "durb", "krimp", "krimpat", "zum",
		"gimb", "-gimb", "glob", "-glob", "sharku", "sha", "-izub", "-izish",
		"izg", "-izg", "ishi", "ghash", "thrakat", "thrak", "golug", "mokum",
		"ufum", "bubhosh", "gimbat", "shai", "khalok", "kurta", "ness", "funda"
	};

	private static Random random = new Random();

	/** @return Get a random word in psudo-Orcish. */
	public static String get() {
		StringBuilder sb = new StringBuilder();
		sb.append(sylables[random.nextInt(sylables.length)]);
		sb.append(sylables[random.nextInt(sylables.length)]);
		sb.append(suffixes[random.nextInt(suffixes.length)]);
		sb.setCharAt(0, Character.toUpperCase(sb.charAt(0)));
		return sb.toString();
	}

}
