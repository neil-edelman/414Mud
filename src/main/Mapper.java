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

	/* braching factor, but rooms are mostly connected back */
	//private static final int approxSize  = 63;//(int)Math.ceil(Math.pow((1.0-1.0/Math.sqrt(2)) + (2 * searchDepth + 1), 3));
	/* nah, most rooms are sparse, we'll just go with the default, 16 */

	private Queue<DirRoom>     queue = new ArrayDeque<DirRoom>();
	private Set<Room>        visited = new HashSet<Room>();		/* 16 */
	private Buffer<DirRoom>    drbuf = new Buffer<DirRoom>();	/* 10 */

	/* queue is this */
	private class DirRoom {
		Room.Direction direction;
		Room.Direction firstDir;
		Room           room;
		DirRoom(final Room.Direction d, final Room.Direction f, final Room r) { dr(d, f, r); }
		void dr(final Room.Direction d, final Room.Direction f, final Room r) {
			direction = d;
			firstDir  = f;
			room      = r;
		}
	}

	public interface EachNode { void node(final Room node, final int distance, final Room.Direction direction); }

	/** Constructor. */
	public Mapper() { }

	/** Does bfs to map out the rooms n hops away, where n is a constant.
	 @pamam root		The place where you are starting from, ie distace = 0.
	 @param searchDepth	n.
	 @param each		What to do at each node. */
	public void map(final Room root, final int searchDepth, final EachNode each) {
		Room node, near;
		Room.Direction dir, first;
		DirRoom dr;
		int dist = 0, thisIncrease = 1, nextIncrease = 0;

		if(searchDepth < 0) return;
		queue.clear();
		visited.clear();

		/*dr = drbuf.getNew();
		dr.dr(Room.Direction.N, root);
		queue.add(dr);*/
		queue.add(new DirRoom(null, null, root));

		while(!queue.isEmpty()) {
			dr   = queue.remove();
			dir  = dr.direction;
			first= dr.firstDir;
			node = dr.room;

			visited.add(node);

			/*System.err.format("where+=(%d, %s)\tqueue{ ", dist, node);
			for(Room r : queue) System.err.format("%s ", r);
			System.err.print("}\n");*/
			each.node(node, dist, /*first<-from the yeller pov*/dir);

			if(dist < searchDepth) {
				for(Room.Direction d : Room.Direction.values()) {
					if((near = node.getRoom(d)) == null || visited.contains(near)) continue;

					/*dr = drbuf.getNew();
					dr.dr(d, near);
					queue.add(dr);*/
					queue.add(new DirRoom(d, first != null ? first : d, near));

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
	}

	/** @return A synecdochical {@link String}. */
	public String toString() {
		return "Mapper";
	}

}
