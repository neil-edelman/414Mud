package entities;

import java.util.List;
import java.util.LinkedList;

public class Container extends Object {

	public int massLimit;
	public List<Stuff> contents = new LinkedList<Stuff>();

	public Container() {
		super();
		massLimit = 10;
		name = "container";
		title= "Some sort of container is here.";
		/* name.clear() */
		/*name.add("container");*/
	}

}
