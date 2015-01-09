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

	/** Constructor. */
	public Chance() {
		super(); /* set a seed? when debugging, it's unhelpful */
	}

	/** @return Uniform distribution with n bits. */
	public int uniform(final int n) {
		return next(n);
	}

	/** @return Bernoulli, { 0, 1 }, with equal chance. */
	public int bernoulli() {
		return next(1);
	}

	/** @return Poisson */
	/*public int poisson(final double lambda) {
		int r = next(8);
	}*/

	public void compare(final int x) {
		System.err.format("%d\t%f\t%d\n", x, Math.log(x), ln(x));
		//System.err.format("%f\t%d\n", Math.sqrt(x), sqrt(x));
	}

	/** fixme: think of something clever */
	private static int sqrt(final int x) {
		return (int)Math.sqrt(x);
	}

	/** Borchardt's Algorithm; fixed point is problematic since, apperently, it's
	 unstable */
	private static int ln(int x) {
		return 6 * (x - 1) / ((x + 1) + (4 * sqrt(x)));
	}

	/** The is super-useful because it is time between events in a Poisson.
	 @param beta	Mean and the std dev; survival parameter,
	 \beta = \frac{1}{\lambda}.
	 @return		Exponential. */
	public int exponential(final int beta) {

		/* pdf:
		 f(x) = \lambda exp[-\lambda x], x >= 0
		   cdf:
		 F(x) = 1 - exp[-\lambda x]
		   inverse:
		 F^{-1}(y) = \frac{ -ln(1 - y) }{ \lambda }, y \in [0,1) */

		int y = next(16); /* <- this number determines \/ that (#byte + 1) */

		/* y+1 because we don't actually want \infty, [0,1) ~> (0,1);
		 fixme: so unoptimised */
		return (int)(-Math.log((double)(y + 1) / (0xFFFF + 1)) * beta);
	}

	/** @return A synecdochical {@link String}. */
	public String toString() {
		return "Chance";
	}

}
