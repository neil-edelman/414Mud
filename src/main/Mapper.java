/** Copyright 2014 Neil Edelman, distributed under the terms of the GNU General
 Public License, see copying.txt */

package main;

import java.util.Queue;
import java.util.ArrayDeque;
import java.util.Set;
import java.util.HashSet;

import common.Buffer;
import main.Connection;
import entities.Room;

/** BFS Search in rooms from a given room.
 
 @author	Neil
 @version	1.1, 12-2014
 @since		1.1, 12-2014 */
public class Mapper {

	/* the braching factor could be used to upper-bound bfs; nah, rooms are
	 sparse, we'll just go with the default, 16, and allow it to grow */
	/* roomQueue and dirQueue are the same, but Java provides no static
	 allocation, so we can not group them w/o allocating memory */
	private Queue<Room>          roomQueue = new ArrayDeque<Room>();
	private Queue<Room.Direction> dirQueue = new ArrayDeque<Room.Direction>();
	private Set<Room>              visited = new HashSet<Room>();

	public interface EachNode { boolean node(final Room node, final int distance, final Room.Direction direction); }

	/** Constructor. */
	public Mapper() { }

	/** Does bfs to map out the rooms n hops away, where n is a constant.
	 @pamam root		The place where you are starting from, ie distace = 0.
	 @param searchDepth	n.
	 @param each		What to do at each node. */
	public boolean map(final Room root, final int searchDepth, final EachNode each) {
		Room node, near;
		Room.Direction dir, first;
		int dist = 0, thisIncrease = 1, nextIncrease = 0;

		if(searchDepth < 0) return false;
		roomQueue.clear();
		 dirQueue.clear();
		visited.clear();

		roomQueue.add(root);
		dirQueue.add(Room.Direction.HERE);

		while(!roomQueue.isEmpty()) {

			node = roomQueue.remove();
			dir  =  dirQueue.remove();

			visited.add(node);

			if(!each.node(node, dist, dir)) return false;

			if(dist < searchDepth) {
				for(Room.Direction d : Room.Direction.values()) {

					if((near = node.getRoom(d)) == null || visited.contains(near)) continue;

					roomQueue.add(near);
					 dirQueue.add(d);

					visited.add(near);
					nextIncrease++;

				}
			}

			if(--thisIncrease <= 0) {
				thisIncrease = nextIncrease;
				nextIncrease = 0;
				dist++;
			}

		}

		return true;
	}

	/** @return A synecdochical {@link String}. */
	public String toString() {
		return "Mapper";
	}

}
