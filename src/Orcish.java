import java.util.Random;

/** Random words are useful. Apologies to linguists; this is very crude.
 The strings are loosely based on Orcish from Smaug1.8.
 @author Neil */

class Orcish {

	private static final String sylables[] = {
		"uk", "all", "uk", "ul", "um", "orc", "uruk", "ee", "ou", "eth", "om",
		"ith", "uuk", "ath", "ohk", "uth", "um", "th", "gn", "oo", "uu", "ar",
		"arg", "en", "yth", "ion", "um", "es", "ac", "ch", "k", "ul", "um", "ick",
		"uk", "of", "tuk", "ove", "aah", "ome", "ask", "my", "mal", "me", "mok",
		"to", "sek", "hi", "come", "vak", "bat", "buy", "muk", "kham", "kzam"
	};

	private static final String suffixes[] = {
		"agh", "ash", "bag", "ronk", "bubhosh", "burz", "dug", "durbat", "durb",
		"ghash", "gimbat", "gimb", "-glob", "glob", "gul", "hai", "ishi", "krimpat",
		"krimp", "lug", "nazg", "nazgul", "olog", "shai", "sha", "sharku", "snaga",
		"thrakat", "thrak", "gorg", "khalok", "snar", "kurta", "ness", "-dug",
		"-gimb"
	};

	private static Random random;

	static {
		random = new Random();
	}

	/** get a random word in "Orcish" */
	public static String get() {
		StringBuilder sb = new StringBuilder();
		sb.append(sylables[random.nextInt(sylables.length)]);
		sb.append(sylables[random.nextInt(sylables.length)]);
		sb.append(suffixes[random.nextInt(suffixes.length)]);
		sb.setCharAt(0, Character.toUpperCase(sb.charAt(0)));
		return sb.toString();
	}

}
