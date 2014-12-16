package entities;

public class Object extends Stuff {

	boolean isBreakable;
	boolean isTransportable;
	int     mass;

	public Object() {
		super();
		isBreakable     = false;
		isTransportable = false;
		mass            = 1;
		name  = "object";
		title = "Some sort of object is here.";
		/* name.clear() ? */
		//name.add("object");
	}

	public Object(final String name, final String title, final boolean isB, final boolean isT) {
		super();
		this.name = name;
		this.title = title;
		isBreakable = isB;
		isTransportable = isT;
	}

}
