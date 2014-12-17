package entities;

public class Mob extends Character {

	public boolean isFriendly;
	public boolean isXeno;

	public Mob() {
		super();
		title = "Someone is looking confused.";
	}

	public Mob(final String name, final String title, final boolean isF, final boolean isX) {
		super();
		this.name = name;
		this.title = title;
		isFriendly = isF;
		isXeno = isX;
	}

}
