/** Copyright 2014 Neil Edelman, distributed under the terms of the GNU General
 Public License, see copying.txt */

package common;

import java.util.Random;
import java.lang.Math;

/** Chance extends Random and provides extended stuff.
 
 @author	Neil
 @version	1.1, 12-2014
 @since		1.1, 12-2014 */
public class Chance extends Random {

	/** Javadocs {@link URL}.
	 <p>
	 More.
	 
	 @param p			Thing.
	 @return			Thing.
	 @throws Exception	Thing.
	 @see				package.Class#method(Type)
	 @see				#field
	 @since				1.0
	 @deprecated		Ohnoz. */

	/** Constructor. */
	public Chance() {
		super(); /* set a seed? when debugging, it's unhelpful */
	}

	/** @return Uniform distribution with n bits. */
	public int uniform(final int n) {
		return next(n);
	}

	/** @return Bernoulli, { 0, 1 } */
	public int bernoulli() {
		return next(1);
	}

	/** @return Poisson */
	public int poisson(final double lambda) {
		int r = next(8); /* 8 should be enought */
		return (int)(Math.exp(-lambda) * Math.pow(lambda, r) / r/*!*/);
	}

	/** @return Exponential */
	public int exponential(/*final double lambda*/) {
		/*int r = next(8);
		return (int)(lambda * Math.exp(-lambda * r));*/
		return next(4); /* lol */
	}

	/** @return A synecdochical {@link String}. */
	public String toString() {
		return "Chance";
	}

}
