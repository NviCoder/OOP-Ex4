package algorithm;

import java.util.PriorityQueue;
import java.util.Set;

import GeoObjects.AllObjects;
import GeoObjects.Box;
import GeoObjects.Fruit;
import GeoObjects.GenericGeoObject;
import GeoObjects.Ghost;
import GeoObjects.Packman;
import GeoObjects.Point3D;
import gui.PanelBoard;
import guiObjects.Line;
import guiObjects.Map;
import guiObjects.Path;
import guiObjects.PathComperator;
import guiObjects.Pixel;
import guiObjects.Segment;
/**
 * This class is the algorithm and allows the automatic game.
 * We have modeled the problem into an unintended graph, this algorithm based on BFS algorithm, using priority queue.
 * So that the corners of the boxes are the nodes of the graph. Thus we knew to tell the player where he could go, 
 * so that for every available lane there was an arch between those nodes.
 * 
 * The progress of the game: 
 * 
 * Game start:
 * We have determined that the beginning of the game will be where the highest concentration of fruits.
 * 
 * Continue game:
 * The player will look for the closest node to him to be able to eat. And once he got to the fruit,
 * he would see if there was any more fruit next to that fruit.
 * This is how the player will continue until the fruit is eaten. Whether he ate them or the pacmans around him!
 * Additionally, the player will always look to see if there is a ghost threatening him and try to escape to the side or up, if necessary.
 * 
 * @author Yoav and Elad.
 *
 */
public class Shortest {

	AllObjects game;
	PanelBoard board;
	public boolean[][] matrixCorners; // represent the graph 
	public Pixel[] corners;

	private Point3D centeralPoint; //for init location
	int maxCloseObjects = 0; //how many objects nearby the most centeral object

	
////////////////////////***Constructor****///////////////////////////////////////////


	public Shortest(AllObjects game, PanelBoard board) {
		refresh(game, board);
		corners = new Pixel[game.boxes.size()*4+1];
		matrixCorners = new boolean[game.boxes.size()*4+1][game.boxes.size()*4+1];
		buildGraph();
	}


///////////////////////////****************//////////////////////////////////////////
///////////////////////////*** Methods ***//////////////////////////////////////////
//////////////////////////****************/////////////////////////////////////////

	
	/**
	 * This function initializes the representative matrix of the graph
	 */
	private void buildGraph() {

		//Init the corners array

		int k=1;
		for (Box box: game.boxes) {
			Pixel[] boxCorners = box.getPixelsCorners(board);
			for (int j=0; j<boxCorners.length; j++) {
				corners[k] = boxCorners[j];
				k++;
			}
		}

		//Init graph of the corners as matrix.
		//matrix[i][j] true iff exist free path between (corners[i],corners[j])

		for (int i=1; i<matrixCorners.length; i++)
			for (int j=1; j<matrixCorners.length; j++)
				matrixCorners[i][j] = freeCornersPath(i, j);
	}

	/**
	 * This method calculates all the constraints and brings the ideal location for the player to go.
	 * @param source the location of the player.
	 * @return The calculation of the algorithm.
	 */
	public Pixel findPath(Pixel source) {
		initSource(source);

		//run away from nearby ghosts
		Pixel runAwayFromGhost = runAway(source);
		if (runAwayFromGhost!=null)
			return runAwayFromGhost;

		//find next pixel target
		Pixel algoFound = findPathAlgorithm(source);
		if (algoFound == null)
			return null;
		
		//run away from far ghost in the way		Segment segment = new Segment(new Line(source, algoFound), source, algoFound);
		Segment segment = new Segment(source, algoFound);

		for (Ghost ghost: game.ghosts) {
			Pixel ghostPixel = board.map.gps2pixel(ghost.getLocation(),  board.getWidth(), board.getHeight());
			if (source.distance(ghostPixel) < 200 && segment.onSegment(ghostPixel)) {
				int deltaY = (ghostPixel.y() - source.y());
				int deltaX = ghostPixel.x() - source.x();
				return gotoTheSide(source, deltaX, deltaY);
			}
		}
		return algoFound;
	}

	
	/**
	 * This method make the search fo the ideal path to the player.
	 * This algorithm based on BFS algorithm, using priority queue.
	 * @param source The source pixel 
	 * @return The Ideal pixel to go according to the algorithm
	 */
	public Pixel findPathAlgorithm(Pixel source) {

		PriorityQueue<Path> queue = new PriorityQueue<>(new PathComperator(corners)); //priority queue, poll the shortest path
		queue.add(new Path(0)); //add the source to queue

		while (!queue.isEmpty()) {
			Path shortPath = queue.poll(); //poll the shortest path
			Pixel closestDirectFruit = closestFruitAndPackman(corners[shortPath.getTail()]); //if exist direct path to fruits - go to the closest
			if (closestDirectFruit != null) { //found fruit from the end of the path
				if (shortPath.size() >= 2) {
					return corners[shortPath.get(1)]; //go to the next corner
				}
				return closestDirectFruit; //go to the closest fruit 
			}
			else
				for (int i=1; i<matrixCorners.length; i++)
					if (matrixCorners[shortPath.getTail()][i] && !shortPath.contains(i)) //exist direct path to other corner && this corner not close a circle on the path
						queue.add(new Path(shortPath, i));
		}
		return null; //not found any fruit or other corner
	}
	
/**
 * This method Initializes the pixel source and  by that, the matrix of the graph
 * @param source The pixel of the place on the map that we wanted to check.
 */
	public void initSource(Pixel source) {
		corners[0] = source;
		for (int i=1; i<corners.length; i++) {
			boolean free = freePath(source, corners[i]); // True - free path, False - no free path.
			matrixCorners[0][i] = free;
			matrixCorners[i][0] = free;
		}
	}


	/**
	 * This method checks if those to corners are on the same box, and knows to tell if they Nearby corners or Opposite corners.
	 * @param c1 The first corner 
	 * @param c2 The second corner
	 * @return True for a free path else False.
	 */
	private boolean freeCornersPath(int c1, int c2) {
		//check if the corners belong to the same box
		if (boxNumber(c1) == boxNumber(c2)) {
			return Math.abs((c1-c2))%2 == 1; //Nearby corners - return true, Opposite corners - return false
		}
		else return freePath(corners[c1], corners[c2]);					
	}

	//return the number of the box in the matrix, by thw corner number
	private int boxNumber(int corner) {
		return (corner-1)/4;
	}
	/**
	 * This method checks if there is a free path between two pixels on the borad
	 * @param source The source pixel
	 * @param target The target pixel
	 * @return True if there is a free path. False if there is not!
	 */
	private boolean freePath(Pixel source, Pixel target) {
		if (source.equals(target))
			return true;
		Segment directSegmant = new Segment(new Line(source,target), source, target);
		for (Box box: game.boxes) {
			Segment[] frame = box.getFrame(board);
			for (int i=0; i<frame.length; i++) { //the segments on the box's frame
				Pixel cutting = frame[i].cuttingPoint(directSegmant); //calculate the cutting point between direct line and the frame of the box
				if (cutting != null && cutting.x() != Integer.MAX_VALUE && cutting.y() != Integer.MAX_VALUE) //the lines are cutting
					return false;
			}
		}
		return true;
	}
	
	/**
	 * This method will tell us for some source pixel what is the closet Fruit And Packman to him.
	 * @param source The source pixel
	 * @return The Pixel that is the closet to the source.
	 */
	private Pixel closestFruitAndPackman(Pixel source) {
		Pixel closestPixel = null; 
		double minDistance = Double.MAX_VALUE;

		//find closest packman
		for (Packman packman: game.packmans) {
			Pixel packmanPixel = board.map.gps2pixel(packman.getLocation(),  board.getWidth(), board.getHeight());
			if (freePath(source, packmanPixel)) {
				if (source.distance(packmanPixel)/0.5 < minDistance) { //get priority of 2 to the packmans
					minDistance = source.distance(packmanPixel);
					closestPixel = packmanPixel;
				}
			}
		}

		//find closest fruit
		for (Fruit fruit: game.fruits) {
			Pixel fruitPixel = board.map.gps2pixel(fruit.getLocation(),  board.getWidth(), board.getHeight());
			if (freePath(source, fruitPixel)) {
				if (source.distance(fruitPixel) < minDistance) {
					minDistance = source.distance(fruitPixel);
					closestPixel = fruitPixel;
				}
			}
		}
		return closestPixel;
	}
	/**
	 * This method refresh the board!
	 * @param game new updated game 
	 * @param board new updated board
	 */
	public void refresh(AllObjects game, PanelBoard board) {
		this.game = game;
		this.board = board;
	}

	/**
	 * This method make the player run away from ghosts
	 * @param source Where the player is.
	 * @return Where the should go
	 */
	public Pixel runAway(Pixel source) {
		if (game.ghosts.isEmpty()) //no ghosts in this game
			return null;

		Pixel closestGhost = closestGhost(source);

		//the ghost is far away
		if (source.distance(closestGhost) > 30)
			return null;

		int deltaY = (closestGhost.y() - source.y());
		int deltaX = closestGhost.x() - source.x();

		//Go to the opposite direction
		if (source.distance(closestGhost) > 8)
			if (freePath(source, new Pixel(source.x()-deltaX, source.y()-deltaY)))
				return new Pixel(source.x()-deltaX, source.y()-deltaY);

		//Go in the free direction, by vector vertical to the vector from current location to the ghost
		return gotoTheSide(source, deltaX, deltaY);
	}
	
	/**
	 * This method tells to the player where to run away from the ghost
	 * 
	 * @return The pixel to go!
	 */
	private Pixel gotoTheSide(Pixel source, int deltaX, int deltaY) {
		if (freePath(source, new Pixel(source.x()+deltaY, source.y()-deltaX)))
			return new Pixel(source.x()+deltaY, source.y()-deltaX);
		if (freePath(source, new Pixel(source.x()-deltaY, source.y()+deltaX)))
			return new Pixel(source.x()-deltaY, source.y()+deltaX);

		return null;
	}

	
	/**
	 * This method find the closest ghost to source point
	 * @param source Where the player is.
	 * @return  Where the closest ghost is.
	 */
	private Pixel closestGhost(Pixel source) {
		Pixel closestPixel = null; 
		double minDistance = Double.MAX_VALUE;
		//find mun distance, for all ghosts in the game
		for (Ghost ghost: game.ghosts) {
			Pixel ghostPixel = board.map.gps2pixel(ghost.getLocation(),  board.getWidth(), board.getHeight());
			if (source.distance(ghostPixel) < minDistance) {
				minDistance = source.distance(ghostPixel);
				closestPixel = ghostPixel;
			}
		}
		return closestPixel;
	}

	
	/**
	 * This method find first location for the player.
	 * @param radius The radius we want.
	 * @return The ideal point for the player.
	 */
	public Point3D mostCenteral(double radius) {
		maxCloseObjects = 0;
		centeralPoint = null;
		for (Fruit fruit: game.fruits)
			calculateCloseObject(fruit, radius);

		for (Packman packman: game.packmans)
			calculateCloseObject(packman, radius);

		return centeralPoint;
	}

	/**
	 * This method calculate the object that have the most close objects. 
	 * @param object The object we check.
	 * @param radius The radius we what to compute with.
	 */
	private void calculateCloseObject(GenericGeoObject object, double radius) {
		int counter = countCloseObjects(object.getLocation(), radius);
		if (counter > maxCloseObjects) {
			maxCloseObjects = counter;
			centeralPoint = object.getLocation(); 
		}
	}
	/**
	 * This method is a help method to the one above, and count the object that close to some object. 
	 * @param location Where to look for.
	 * @param radius The radius that we want to compute with.
	 * @return The sum of the object.
	 */
	private int countCloseObjects(Point3D location, double radius) {
		Pixel source = board.map.gps2pixel(location, board.getWidth(), board.getHeight());
		int counter = 0;
		for (Packman packman: game.packmans) {
			Pixel target = board.map.gps2pixel(packman.getLocation(), board.getWidth(), board.getHeight());
			if (source.distance(target) < radius && freePath(source, target))
				counter++;
		}

		for (Fruit fruit: game.fruits) {
			Pixel target = board.map.gps2pixel(fruit.getLocation(), board.getWidth(), board.getHeight());
			if (source.distance(target) < radius && freePath(source, target))
				counter++;
		}

		return counter;
	}

}
