/** Copyright 2014 Neil Edelman, distributed under the terms of the GNU General
 Public License, see copying.txt */

package main;

import java.util.Queue;
import java.util.ArrayDeque;
import java.util.Set;
import java.util.HashSet;

import main.Connection;
import entities.Room;
import entities.Stuff;
import entities.Mob;
import entities.Player;

/** BFS Search in rooms from a given room.
 
 @author	Neil
 @version	1.1, 2014-12
 @since		1.1, 2014-12 */
public class Mapper {

	/* the braching factor could be used to upper-bound bfs; nah, rooms are
	 sparse, we'll just go with the default, 16, and allow it to grow */
	/* roomQueue and dirQueue are the same, but Java provides no static
	 allocation, so we can not group them w/o allocating memory */
	private Queue<Room>          roomQueue = new ArrayDeque<Room>();
	private Queue<Room.Direction> dirQueue = new ArrayDeque<Room.Direction>();
	private Queue<int[]>          posQueue = new ArrayDeque<int[]>();
	private int[][]              posBuffer;
	private Room[][]                   map;
	private Set<Room>              visited = new HashSet<Room>();

	public interface EachNode { boolean node(final Room node, final int distance, final Room.Direction direction); }

	/** Constructor; the handler that called it. */
	public Mapper() {
		final int mapSize = (LoadCommands.mapDepth() << 1) + 1;
		map               = new Room[mapSize][mapSize];
		posBuffer         = new int[mapSize * mapSize][2];
	}

	public String mapRooms(final Room root) {
		final int searchDepth = LoadCommands.mapDepth();
		Room node, near;
		Room.Direction dir, first;
		int[] pos;
		int dist = 0, thisIncrease = 1, nextIncrease = 0;
		int posStack = 0;

		roomQueue.clear();
		dirQueue.clear();
		visited.clear();

		posBuffer[posStack][0] = searchDepth;
		posBuffer[posStack][1] = searchDepth;
		roomQueue.add(root);
		dirQueue.add(Room.Direction.HERE);
		posQueue.add(posBuffer[posStack++]);

		while(!roomQueue.isEmpty()) {
			
			node = roomQueue.remove();
			dir  =  dirQueue.remove();
			pos  =  posQueue.remove();

			visited.add(node);
			map[pos[0]][pos[1]] = node;

			if(dist < searchDepth) {
				for(Room.Direction d : Room.Direction.values()) {

					if((near = node.getRoom(d)) == null
					   || visited.contains(near)
					   || d == Room.Direction.U
					   || d == Room.Direction.D) continue;
					
					posBuffer[posStack][0] = pos[0] - d.getY();
					posBuffer[posStack][1] = pos[1] + d.getX();
					roomQueue.add(near);
					dirQueue.add(d);
					posQueue.add(posBuffer[posStack++]);
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

		/* build up the string -- fixme: this is so un-optimised, player, mob,
		 and object lists, list.isEmpty() -- fixme: no colour (at least have the
		 spot you're on now be highlighted) -- fixme: flat (a mob on an object
		 will not be picked up) etc */
		boolean isUpD, isPlr, isMob, isObj;
		char      upd,   plr,   mob,   obj;
		StringBuilder sb = new StringBuilder(map.length * map[0].length);
		for(Room col[] : map) {
			for(Room row : col) {
				if(row == null) sb.append("      ");
				else            sb.append(String.format("[%4.4s", row.getName()));
			}
			sb.append("\n");
			for(Room row : col) {
				if(row == null) {
					sb.append("      ");
				} else {
					isUpD = row.getRoom(Room.Direction.U)   != null
					     || row.getRoom(Room.Direction.D) != null;
					isPlr = isMob = isObj = false;
					for(Stuff s : row) {
						if(s instanceof entities.Object) {
							isObj = true;
							System.err.format("BFS %s: %s obj\n", row, s);
						} else if(s instanceof Mob) {
							isMob = true;
							System.err.format("BFS %s: %s mob\n", row, s);
						} else if(s instanceof Player) {
							isPlr = true;
							System.err.format("BFS %s: %s plr\n", row, s);
						}
					}
					upd = isUpD ? 's' : ' ';
					plr = isPlr ? 'p' : ' ';
					mob = isMob ? 'm' : ' ';
					obj = isObj ? 'o' : ' ';
					sb.append(String.format("%c%c%c%c]", upd, plr, mob, obj));
				}
			}
			sb.append("\n");
		}

		/* clear it for the next time; the reason I have this here instead of at
		 the begging is to avoid temporary references */
		for(Room col[] : map) {
			for(int row = 0; row < col.length; row++) {
				col[row] = null;
			}
		}

		return sb.toString();
	}

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
