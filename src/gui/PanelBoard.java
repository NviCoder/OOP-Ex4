package gui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JPanel;

import Coords.MyCoords;
import GeoObjects.Fruit;
import GeoObjects.GenericGeoObject;
import GeoObjects.Ghost;
import GeoObjects.AllObjects;
import GeoObjects.Box;
import GeoObjects.Packman;
import GeoObjects.Player;
import GeoObjects.Point3D;
import guiObjects.Line;
import guiObjects.Map;
import guiObjects.Pixel;
/**
 * This class is the Panel Board of main window for the GUI.
 * This panel contains all the map elements to show for the user.
 * @author Yoav and Elad.
 * @version 1.0
 *
 */
public class PanelBoard extends JPanel implements MouseListener {

	public MainWindow window;
	public Map map;
	public Box bounding;

	private BufferedImage[] fruitsImages;
	private BufferedImage packmanImage;
	private BufferedImage ghostImage;
	private BufferedImage playerImage;
	public MyCoords mc = new MyCoords();

	////////////////////////***Constructor****///////////////////////////////////////////


	public PanelBoard(MainWindow window, Map map) {
		this.window = window;
		this.map = map;

		fruitsImages = new BufferedImage[6];
		try {
			for (int i=0; i<6; i++)
				fruitsImages[i] = ImageIO.read( new File(Fruit.chooseImage(i)));
			packmanImage = ImageIO.read( new File(Packman.imagePath));
			ghostImage =  ImageIO.read( new File(Ghost.imagePath));		
			playerImage = ImageIO.read( new File(Player.imagePath));
		} catch (IOException exc) {
			System.out.println(exc.toString());
		}
		this.addMouseListener(this);
	}

	///////////////////////////*** Methods ***//////////////////////////////////////////


	/////////////////////////////****Painting the map***///////////////////////////////////////

	public void paint(Graphics g)
	{
		//		this.setSize(window.getWidth()-16, window.getHeight()-59); //check this numbers!!
		this.setSize(window.getWidth()-16, window.getHeight()-90); //check this numbers!!

		//draw background
		g.drawImage(map.myImage,0, 0, this.getWidth(), this.getHeight(), this);

		if (window.game == null)
			return;

		//draw boxes
		g.setColor(Color.BLACK);
		for (Box box: window.game.boxes) {
			Pixel nw = box.getPixelNw(this);
			int width = map.gps2pixel(box.getNe(), this.getWidth(), this.getHeight()).x() - nw.x();
			int hight = map.gps2pixel(box.getSw(), this.getWidth(), this.getHeight()).y() - nw.y();
			g.fillRect(nw.x(), nw.y(), width, hight);
		}

		//draw fruits
		for (Fruit fruit: window.game.fruits) {
			Pixel pixel = map.gps2pixel(fruit.getLocation(), this.getWidth(), this.getHeight());
			g.drawImage(fruitsImages[fruit.getRandImage()], pixel.x() - fruitsImages[fruit.getRandImage()].getWidth()/2, pixel.y() - fruitsImages[fruit.getRandImage()].getHeight()/2, this);
		}

		//draw packmans
		for (Packman packman: window.game.packmans) {
			Pixel pixel = map.gps2pixel(packman.getLocation(), this.getWidth(), this.getHeight());
			g.drawImage(packmanImage, pixel.x() - packmanImage.getWidth()/2, pixel.y() - packmanImage.getHeight()/2, this);
		}

		//draw ghosts
		for (Ghost ghost: window.game.ghosts) {
			Pixel pixel = map.gps2pixel(ghost.getLocation(), this.getWidth(), this.getHeight());
			g.drawImage(ghostImage, pixel.x() - ghostImage.getWidth()/2, pixel.y() - ghostImage.getHeight()/2, this);
		}

		//draw player
		if (window.game.player != null) {
			Pixel pixel = map.gps2pixel(window.game.player.getLocation(), this.getWidth(), this.getHeight());
			g.drawImage(playerImage, pixel.x() - playerImage.getWidth()/2, pixel.y() - playerImage.getHeight()/2, this);
		}		

	}

	@Override
	public void mouseClicked(MouseEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mousePressed(MouseEvent e) {

		switch (window.press) {
		case FIRST_LOCATION:
			Point3D pointToStart = map.pixel2gps(new Pixel(e.getX(),  e.getY()), this.getWidth(), this.getHeight());
			window.lastLocation = pointToStart;
			window.play.setInitLocation(pointToStart.x(), pointToStart.y());
			window.startGame(false);
			repaintMe();
			break;

		case GO:
			Point3D pointToGo = map.pixel2gps(new Pixel(e.getX(),  e.getY()), this.getWidth(), this.getHeight());
			double azimuth = mc.azimuth(window.game.player.getLocation(), pointToGo);
			window.azimuth = azimuth;
			window.play.rotate(azimuth);
			break;

		default: //NOTHING
			break;
		}
	}

	@Override
	public void mouseReleased(MouseEvent e) {

	}

	@Override
	public void mouseEntered(MouseEvent e) {

	}

	@Override
	public void mouseExited(MouseEvent e) {

	}

	public void repaintMe() {
		paintImmediately(0, 0, this.getWidth(), this.getHeight());
	}

	/**
	 * This method set the bounding for the panel according to the map data
	 * that he gets.
	 * @param map_data The data from the map.
	 */
	void setBounding(String map_data) {
		String[] csvRow = map_data.split(",");
		Point3D point1 = new Point3D(Double.parseDouble(csvRow[2]),
				Double.parseDouble(csvRow[3]),Double.parseDouble(csvRow[4]));
		Point3D point2 = new Point3D(Double.parseDouble(csvRow[5]),
				Double.parseDouble(csvRow[6]),0);

		bounding = new Box(point1, (int)Double.parseDouble(csvRow[1]), point2, 0.0, 0.0);
	}

}
