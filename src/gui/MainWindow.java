package gui;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Menu;
import java.awt.MenuBar;
import java.awt.MenuItem;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.filechooser.FileNameExtensionFilter;

import Coords.MyCoords;
import GeoObjects.AllObjects;
import GeoObjects.Fruit;
import GeoObjects.Packman;
import GeoObjects.Point3D;
import Robot.Play;
import algorithm.Shortest;
import audio.EatingSound;
import audio.SimplePlayer;
import convertor.Csv2Game;
import convertor.Data2Game;
import gameData.FilterOption;
import gameData.Report;
import gameData.SQLPull;
import gameData.SystemPrinter;
import guiObjects.Pixel;
import guiObjects.Line;
import guiObjects.Map;
import guiObjects.Path;

/**
 * This class is the main window of the GUI.
 * This class contains all the GUI Objects, and arranges them for a user-friendly window
 * This GUI powered by the main class.
 * @author Yoav and Elad.
 * @version 1.0
 *
 */
public class MainWindow extends JFrame
{
	public PanelBoard myBoard;
	public PanelBottom bottom;
	public AllObjects game;
	public Press press = Press.NOTHING;
	public Play play;
	public File file;
	
	double azimuth = 0;
	Point3D lastLocation = null;
	int lastNumObjects = 0;

	private Csv2Game convertor = new Csv2Game();
	private Data2Game dataConvertor = new Data2Game();

////////////////////////////////*******************///////////////////////////////////
///////////////////////////////***Constructors****///////////////////////////////////
//////////////////////////////********************//////////////////////////////////
	
	public MainWindow(Map map) 
	{		
		this.setTitle("Mario Game");
	    ImageIcon img = new ImageIcon("ImagesforGui\\Icons\\super-mario.png");
	    this.setIconImage(img.getImage());
		initMenu();
		myBoard = new PanelBoard(this, map);
		initPanels();
	}

////////////////////////////////***Menu Bar****///////////////////////////////////

	private void initMenu() 
	{
		JMenuBar menuBar = new JMenuBar();

		//new game menu
		JMenu newGame = new JMenu("New Game");

		JMenuItem  importGame = new JMenuItem ("Open CSV File",new ImageIcon("ImagesforGui\\Icons\\csv.png"));
		importGame.addActionListener(new ActionListener() {		
			@Override
			public void actionPerformed(ActionEvent e) {
				importCsv();
			}
		});

		JMenuItem  tryAgain = new JMenuItem ("Try Again",new ImageIcon("ImagesforGui\\Icons\\tryAgain.png"));
		tryAgain.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (play != null && play.isRuning())
					endGame();
				newGame();
			}
		});

		newGame.add(importGame);
		newGame.add(tryAgain);
		menuBar.add(newGame);

		//start menu
		JMenu start = new JMenu("Start!");

		//playing by the mouse
		JMenuItem  manual = new JMenuItem ("Manual",new ImageIcon("ImagesforGui\\Icons\\Manual.png"));
		manual.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				chooseManualLocation();
			}
		});

		//playing automatic
		JMenuItem  auto = new JMenuItem("Auto",new ImageIcon("ImagesforGui\\Icons\\Auto.png"));
		auto.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				Point3D startingPoint = chooseAutoLocation();
				play.setInitLocation(startingPoint.x(), startingPoint.y());
				startGame(true);
			}
		});

		start.add(manual);
		start.add(auto);
		menuBar.add(start);

		
		//statics menu
		JMenu statics = new JMenu("Statics");
		
		JMenuItem lastGame = new JMenuItem("Last game",new ImageIcon("ImagesforGui\\Icons\\lastGame.png"));
		JMenuItem currentMyBest = new JMenuItem("My best in this scenario",new ImageIcon("ImagesforGui\\Icons\\best.png"));
		JMenuItem currentPlayersBest = new JMenuItem("Best of all players in this scenario",new ImageIcon("ImagesforGui\\Icons\\best2.png"));
		JMenuItem allMyBest = new JMenuItem("My best in all scenarios",new ImageIcon("ImagesforGui\\Icons\\best3.png"));
		JMenuItem allBest = new JMenuItem("Best of all players in all scenarios",new ImageIcon("ImagesforGui\\Icons\\best4.png"));
		
		lastGame.addActionListener(new ListenerStatics(FilterOption.getMyLastGame));
		currentMyBest.addActionListener(new ListenerStatics(FilterOption.getMyBest));
		currentPlayersBest.addActionListener(new ListenerStatics(FilterOption.getMaxForScenario));
		allMyBest.addActionListener(new ListenerStatics(FilterOption.getAllMyBest));
		allBest.addActionListener(new ListenerStatics(FilterOption.getAllBest));
		
		statics.add(lastGame);
		statics.add(currentMyBest);
		statics.add(currentPlayersBest);
		statics.add(allMyBest);
		statics.add(allBest);
		
		menuBar.add(statics);	


		this.setJMenuBar(menuBar);
		
		try { //////Window windows
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

////////////////////////////////*******************///////////////////////////////////
///////////////////////////////*****Methods*******///////////////////////////////////
//////////////////////////////********************//////////////////////////////////
	/**
	 * This method initialization the panels of the window.
	 */
	private void initPanels() {
		this.add("Center", myBoard);
		myBoard.setVisible(true);

		bottom = new PanelBottom(this);
		add("South", bottom);
		bottom.setSize(this.getWidth(), 100);
		bottom.setVisible(true);
	}
	/**
	 * This method responsible for the import from csv file.
	 * @return True or false.
	 */
	public boolean importCsv() {
		if (play != null && play.isRuning())
			endGame();
		JFileChooser fc = new JFileChooser();
		FileNameExtensionFilter filter = new FileNameExtensionFilter("CSV files", "csv");
		fc.setFileFilter(filter);
		int returnVal = fc.showOpenDialog(this);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			file = fc.getSelectedFile();
			newGame();
			return true;
		}
		return false;
	}
	
	/**
	 * This method responsible for making a new game.
	 * 
	 */
	private void newGame() {
		if (file == null)
			return;
		play = new Play(file.getAbsolutePath());
		play.setIDs(204533632, 206284267);
		game = convertor.convert(file);
		myBoard.setBounding(play.getBoundingBox());
		myBoard.repaintMe();
		lastNumObjects = game.getNumOfFriutsAndPackmans();
	}
	
	/**
	 * This method responsible for starting the game.
	 * 
	 */
	public void startGame(boolean automatic) {
		if (!automatic)
			press = Press.GO;
		Thread startAutoGame = new Thread(new Runnable() {

			@Override
			public void run() {
				//play music
				Thread backgroundMusic = new Thread(new SimplePlayer());
				backgroundMusic.start();

				play.start();

				Shortest algo = new Shortest(game, myBoard);
				while (play.isRuning()) {
					algo.refresh(game, myBoard);

					//refresh the bottom menu!
					Report report = Report.Parse(play.getStatistics());
					updateAll(report);

					ArrayList<String> board_data = play.getBoard();
					game = dataConvertor.convert(board_data);
					play.rotate(azimuth);
					myBoard.repaintMe();

					if (lastNumObjects > game.getNumOfFriutsAndPackmans()) {
						Thread eatingSoung = new Thread(new EatingSound());
						eatingSoung.start();
					}
					lastNumObjects = game.getNumOfFriutsAndPackmans();

					//find new azimuth
					if (automatic)
						autoRotate(algo);

					try {
						Thread.sleep(30);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}					
				}

				//end of the game
				Report report = Report.Parse(play.getStatistics());
				updateAll(report);

				if (backgroundMusic.isAlive())
					backgroundMusic.stop();
				if (!automatic)
					press = Press.NOTHING;
				endGame();
			}

			//This function updates data in the bottom panel
			private void updateAll(Report report) {
				Runnable updater = new LabelUpdater(bottom.killByGhosts, "Kill By Ghosts: "+report.getKillByGhosts());
				EventQueue.invokeLater(updater);

				updater = new LabelUpdater(bottom.score, "    Score: "+report.getScore());
				EventQueue.invokeLater(updater);

				updater = new LabelUpdater(bottom.outOfBox," Out Of Box: "+report.getOutOfBox());
				EventQueue.invokeLater(updater);

				updater = new LabelUpdater(bottom.timeLeft, "Time Left: "+report.getTimeLeft());
				EventQueue.invokeLater(updater);
			}
		});
		startAutoGame.start();
	}
	
	/**
	 * This method responsible for choose Manual Location,
	 * for the first location. 
	 */
	private void chooseManualLocation() {
		press = Press.FIRST_LOCATION;
	}

	/**
	 * This method responsible for choose auto Location for the player
	 * @return The starting point according to the algorithm.
	 */
	private Point3D chooseAutoLocation() {
		Shortest algo = new Shortest(game, myBoard);
		Point3D startingPoint = algo.mostCenteral(50);

		//if the algorithms don't find point:
		if (startingPoint == null)
			startingPoint = new Point3D(32.1044700993651, 35.2079930001858, 0); //point in the center of the screen

		lastLocation = startingPoint;
		return startingPoint;	
	}
	
	/**
	 * This method responsible for choose a new direction for the player,
	 * using the algorithm. 
	 */
	private void autoRotate(Shortest algo) {
		if (game.player != null && !game.fruits.isEmpty()) {
			Pixel playerPixelLocation = myBoard.map.gps2pixel(game.player.getLocation(), myBoard.getWidth(), myBoard.getHeight());
			Pixel nextPixel = algo.findPath(playerPixelLocation); //calculate what is the next target (in pixels)
			if (nextPixel != null) {
				azimuth = myBoard.mc.azimuth(game.player.getLocation(), //refresh the azimuth
						myBoard.map.pixel2gps(nextPixel, myBoard.getWidth(), myBoard.getHeight()));
			}
		}
		if (game.player != null && lastLocation.equals(game.player.getLocation())) {
			azimuth += 90*(int)(Math.random()*3+1); //if the player stack go to another location
		}
		lastLocation = game.player.getLocation();
	}
	
	/**
	 * This method responsible for clearing the board probably for a 
	 * new game.
	 */
	public void clear() {
		game.clear();
	}
	
	/**
	 * This method responsible for ending the game.
	 */
	public void endGame() {
		if (play==null)
			return;
		if (play.isRuning())
			play.stop();

		// print the data & save to the course DB
		System.out.println("**** Done Game ****");	
		String info = play.getStatistics();
		System.out.println(info);
	}
	
}