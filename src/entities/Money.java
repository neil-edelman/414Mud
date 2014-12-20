/* Copyright 2014 Sid Gandhi and Neil Edelman, distributed under the terms of
 the GNU General Public License, see copying.txt */

package entities;

/** Money.

 @author	Sid, Neil
 @version	1.0, 11-2014
 @since		1.0, 11-2014 */
public class Money extends Object {

	public int amount;

	public Money() {
		super();
		amount = 1;
		name = "coin";
		title= "One coin is sitting on the ground.";
		/*name.add("money");
		name.add("dollar");*/
	}

	public Money(int amount) {
		this.amount = amount;
	}
	
	public void AddMoney(int amountToAdd) {
		this.amount += amountToAdd;
	}
	
	public void SubtractMoney(int amountToRemove) {
		this.amount -= amountToRemove;
	}
	
	public int GetAmount() {
		return this.amount;
	}

}
