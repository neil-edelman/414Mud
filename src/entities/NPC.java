package entities;

public class NPC extends Character {

	public boolean isFriendly;
	public boolean isXeno;

	public NPC() {
		super();
		title = "Someone is looking confused.";
	}

	public NPC(final String name, final String title, final boolean isF, final boolean isX) {
		super();
		this.name = name;
		this.title = title;
		isFriendly = isF;
		isXeno = isX;
	}

}
