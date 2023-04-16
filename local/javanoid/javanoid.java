// ######################################################################
// #                                                                    #
// #         ##   ####   ##   ##   ####   ##  ##   ####   ##  #####     #
// #         ##  ##  ##  ##   ##  ##  ##  ### ##  ##  ##  ##  ##  ##    #
// #         ##  ##  ##   ## ##   ##  ##  ######  ##  ##  ##  ##  ##    #
// #     ##  ##  ######   ## ##   ######  ## ###  ##  ##  ##  ##  ##    #
// #      ####   ##  ##    ###    ##  ##  ##  ##   ####   ##  #####     #
// #                                                                    #
// #    [][][][] - A BLOCK BREAKER APPLET BY REMI FAITOUT - [][][][]    #
// #    [][]  []                                            [][]  []    #
// #        o        VERSION 1.54 (JDK1.13) 17/03/1998          o       #
// #      o==o                                                o==o      #
// #                                                                    #
// ######################################################################

// ## IMPORTS ###########################################################
import java.lang.*;
import java.applet.*;
import java.io.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import java.net.*;

// ## JAVANOID : THE MAIN CLASS #########################################
// The user interface that contains the game itself...

public class javanoid extends Applet implements ActionListener, ItemListener {
	
	// References
	jngame game;
	
	// User interface
	Panel control;
	Checkbox soundCheck;
	Button startButton, pauseButton;
	
  // javanoid initialization
	public void init() {

		// Game init
		showStatus("Creating game objects...");
		game = new jngame(this);
		
		// User interface construction
		showStatus("Building user interface...");
		setLayout(new BorderLayout());
		setBackground(Color.lightGray);
		control = new Panel();
		control.setLayout(new FlowLayout(FlowLayout.CENTER));
		control.add(startButton = new Button("NEW GAME"));
		startButton.addActionListener(this);
		control.add(pauseButton = new Button("PAUSE"));
		pauseButton.addActionListener(this);
		control.add(soundCheck = new Checkbox("SOUND"));
		soundCheck.addItemListener(this);
		control.doLayout();
		add("Center", game);
		add("South", control);
		doLayout();
		
		// Start in demo mode
		showStatus("");
		game.startGame(true); 
		}

	// Control of widgets from the game
	public void setPauseButton(boolean b) { 
		pauseButton.setLabel( b ? "PAUSE" : "RESUME");
		startButton.setEnabled(b);
		}
	public void setSoundCheck() {
		soundCheck.setState(!soundCheck.getState());
		}
		
	// Event handler (panel)
	public void actionPerformed(ActionEvent evt) {
		Object src = evt.getSource();
		if (game!=null) {
			if (src==startButton) { 
				setPauseButton(true); 
				game.stopGame(); 
				game.startGame(false);
				game.requestFocus();
				} 
			else if (src==pauseButton) {
				boolean b = (pauseButton.getLabel()=="PAUSE");
				setPauseButton(!b); 
				game.setPause(b);
				if (!b) game.requestFocus();
				}
			}
		}
	public void itemStateChanged(ItemEvent evt) {
		Object src = evt.getSource();
		if ((game!=null)&&(src==soundCheck)) {
			game.toggleSound();
			game.requestFocus();
			}
		}

	// Applet information methods
	public String getAppletInfo() { return "Applet Javanoid v1.54 by Rémi FAITOUT"; }	
	}


// ## JAVANOID GAME CLASS ###############################################
// Main class for javanoid : It manages all the game objects, their lives, 
// collisions, both the user and game events (moves, bullets, pills and
// balls throws, next level, ...).  

final class jngame extends jnbuffer implements Runnable, MouseListener, MouseMotionListener, KeyListener {

	// Game default size
	final static int defaultWidth = 312;
	final static int defaultHeight = 300;
	
	// File names
	final static String imageFile = "javanoid.gif";
	final static String soundFiles[] = {"start.au", "wall.au", "shoot.au"};
	final static String levelFile = "javanoid.dat";
	
	// Number of images per object
	final static int nbBallImg = 2;	
	final static int nbPaddleImg = 4;
	final static int nbBlockImg = 16;					
	final static int nbPillImg = 6;
	final static int nbPillImgAnim = 4;
	final static int nbBulletImg = 1;

	// Number of sound files
	final static int nbSounds = 3;

	// Size & coordinates of the images
	final static int blockCoords[][] = { 
		{0 , 0, 24, 16}, {0 , 16, 24, 16}, {0 , 32, 24, 16}, {0 , 48, 24, 16}, 
		{24, 0, 24, 16}, {24, 16, 24, 16}, {24, 32, 24, 16}, {24, 48, 24, 16},
		{48, 0, 24, 16}, {48, 16, 24, 16}, {48, 32, 24, 16}, {48, 48, 24, 16},
		{72, 0, 24, 16}, {72, 16, 24, 16}, {72, 32, 24, 16}, {72, 48, 24, 16}};
	final static int ballCoords[][] = { 
		{0, 64, 12, 12}, {12, 64, 12, 12}};
	final static int paddleCoords[][] = { 
		{24, 64, 40, 12}, {64, 64, 32, 12}, {0, 76, 48, 12}, {48, 76, 40, 12} };
	final static int bulletCoords[][] = {
		{88, 82, 4, 6}};
	final static int pillCoords[][][] = {
		{{0,  88, 16, 8}, {0,  96, 16, 8}, {0, 104, 16, 8}, {0, 112, 16, 8}},
		{{16,  88, 16, 8}, {16,  96, 16, 8}, {16, 104, 16, 8}, {16, 112, 16, 8}}, 
		{{32,  88, 16, 8}, {32,  96, 16, 8}, {32, 104, 16, 8}, {32, 112, 16, 8}}, 
		{{48,  88, 16, 8}, {48,  96, 16, 8}, {48, 104, 16, 8}, {48, 112, 16, 8}}, 
		{{64,  88, 16, 8}, {64,  96, 16, 8}, {64, 104, 16, 8}, {64, 112, 16, 8}}, 
		{{80,  88, 16, 8}, {80,  96, 16, 8}, {80, 104, 16, 8}, {80, 112, 16, 8}}};

	// Arrays for images
	protected Image ballImg[] = new Image[nbBallImg];
	protected Image paddleImg[] = new Image[nbPaddleImg];
	protected Image blockImg[] = new Image[nbBlockImg];
	protected Image pillImg[][] = new Image[nbPillImg][nbPillImgAnim];	
	protected Image bulletImg[] = new Image[nbBulletImg];

	// Array for sounds
	protected AudioClip sounds[] = new AudioClip[nbSounds];

	// Level definitions
	final static int nbLevels = 12;
	final static int nbLevelRows = 12;
	final static int nbLevelCols = 13;
	final static int colWidth = 24;
	final static int rowHeight = 16;	
	protected String levelNames[] = new String[nbLevels];
  protected String levelDefinitions[][] = new String[nbLevels][nbLevelRows];

	// Current level definition
	protected int currentLevel = 0;
	protected int nbBlocksLeft = 0;	
	protected jnblock levelMap[][] = new jnblock[nbLevelCols][nbLevelRows];

	// Game default attributes
	final static int nbPills = 4;
	final static int nbBullets = 8;
	final static int nbBalls = 3;
	final static int displayAlt = 90;
	final static int paddleAlt = 40;
	final static int scoreAlt = 20;
	final static int nbPillValues = 6;
	
	// Idle time during the run loop
	protected long gameRate = 25;

	// Current sound
	protected int currentSound = 0;
	
	// Flags for game state
	protected boolean goToNext = false;
	protected boolean demoMode = false;
	protected boolean fireMode = false;
	protected boolean soundOn = false;
	protected boolean gameover = false;
	protected boolean paused = false;
	protected boolean pause = false;
	
	// Time between game over & demo (10s)
	protected long demoWait = 10000;
	
	// Keyboard support
	protected boolean keyboard = false;
	protected int lastKey = KeyEvent.VK_UNDEFINED;
	
	// Javanoid objects
	protected jnstatus status;
	protected jnpill pills[] = new jnpill[nbPills];
	protected jnbullet bullets[] = new jnbullet[nbBullets];
	protected jnball balls[] = new jnball[nbBalls];
	protected jnpaddle paddle;
	
	// References
	Thread myThread;
	javanoid parent;
	
	// Constructor
	public jngame (javanoid p) {
		
		// Canvas init
		super(p, defaultWidth, defaultHeight);
		parent = p;

		// Loading images
		parent.showStatus("Loading images...");
		MediaTracker tracker = new MediaTracker(parent);
		URL url = parent.getCodeBase();		
		Image imageTmp = parent.getImage(url, imageFile);
		tracker.addImage(imageTmp, 0);
		ImageFilter filter;
		
		// Extracting images
		for (int i=0; i<nbBlockImg; i++) {
			filter = new CropImageFilter(blockCoords[i][0], blockCoords[i][1], blockCoords[i][2], blockCoords[i][3]);
			blockImg[i] = parent.createImage(new FilteredImageSource(imageTmp.getSource(), filter));
			tracker.addImage(blockImg[i], 0);
			}
		for (int i=0; i<nbBallImg; i++) {
			filter = new CropImageFilter(ballCoords[i][0], ballCoords[i][1], ballCoords[i][2], ballCoords[i][3]);
			ballImg[i] = parent.createImage(new FilteredImageSource(imageTmp.getSource(), filter));
			tracker.addImage(ballImg[i], 0);
			}
		for (int i=0; i<nbPaddleImg; i++) {
			filter = new CropImageFilter(paddleCoords[i][0], paddleCoords[i][1], paddleCoords[i][2], paddleCoords[i][3]);
			paddleImg[i] = parent.createImage(new FilteredImageSource(imageTmp.getSource(), filter));
			tracker.addImage(paddleImg[i], 0);
			}
		for (int i=0; i<nbPillImg; i++) {
			for (int j=0; j<nbPillImgAnim; j++) {
				filter = new CropImageFilter(pillCoords[i][j][0], pillCoords[i][j][1], pillCoords[i][j][2], pillCoords[i][j][3]);
				pillImg[i][j] = parent.createImage(new FilteredImageSource(imageTmp.getSource(), filter));
				tracker.addImage(pillImg[i][j], 0);
				}
			}
		for (int i=0; i<nbBulletImg; i++) {
			filter = new CropImageFilter(bulletCoords[i][0], bulletCoords[i][1], bulletCoords[i][2], bulletCoords[i][3]);
			bulletImg[i] = parent.createImage(new FilteredImageSource(imageTmp.getSource(), filter));
			tracker.addImage(bulletImg[i], 0);
			}

		// Waiting for completion
		try { tracker.waitForAll(); }
		catch (InterruptedException e) {}

		// Loading sounds
		parent.showStatus("Loading sounds...");
		for (int i=0;i<nbSounds;i++) {
			sounds[i] = parent.getAudioClip(url, soundFiles[i]);
			try { Thread.sleep(100); } 
			catch (InterruptedException e) { }
			}

		// Loading levels
		parent.showStatus("Loading levels...");
		try {
			String line;
			URL url2 = new URL(parent.getCodeBase(), levelFile);
			URLConnection cnx = url2.openConnection();
			BufferedReader br = new BufferedReader(new InputStreamReader(cnx.getInputStream())); 
			for (int l=0;l<nbLevels;l++) {
				levelNames[l]=br.readLine();
				for (int r=0;r<nbLevelRows;r++) { levelDefinitions[l][r] = br.readLine(); }
				}
			br.close();
      }
		catch (FileNotFoundException e) { System.out.println("File " + levelFile + "not found."); } 
		catch (IOException e) { System.out.println("File " + levelFile + " is corrupt : " + e); }
		
		// Status & level init
		parent.showStatus("Creating game objects...");
		status = new jnstatus(this, ballImg[0], scoreAlt, displayAlt);

		// Adding event handlers
		addMouseListener(this);
		addMouseMotionListener(this);
		addKeyListener(this);
		}
		
	// Start in demo or game mode
	void startGame(boolean d) {

		// Game, status & level reset
		demoMode = d; 
		goToNext = false; 
		fireMode = false;
		keyboard = false;
		pause = false;
		demoWait = 10000;
		currentLevel = 0;		
	  clearBackground();
		computeLevel();
		status.reset();
		
		// Objects creation
		paddle = new jnpaddle(this, paddleImg, paddleAlt, true);
		balls[0] = new jnball(this, paddle, ballImg);
		
		// Automatic start or level name display
		if (demoMode) { 
			status.showHiScore(); 
			balls[0].launch(); 
			}
		else status.showName(levelNames[currentLevel]);
		
		// Restart the thread if necessary
		gameover = false;
		paused = false;
		start();
		}

	// End level & resets the game
	void stopGame() {

		// Reset game objects
		for (int i=0;i<nbBalls;i++) balls[i] = null;
		for (int i=0;i<nbPills;i++) pills[i] = null;
		for (int i=0;i<nbBullets;i++) bullets[i] = null;
		paddle = null;

		// Force garbage collection
		System.gc();
		}
	
	// Pause and resume game
	public void setPause(boolean b) {
		if (b!=paused) {
			// Ask for a pause at next loop
			if (b) { 
				status.showPause(demoMode); 
				pause = true;
				}
			// Or resume now
			else paused = false;
			}
		} 
		
	// Start & stop methods
	public void stop() {
		if (myThread!=null) myThread.stop();
		myThread = null;
		}	
	public void start() { 
		if (myThread==null) {
			myThread = new Thread(this); 
			myThread.setPriority(Thread.MIN_PRIORITY); 
			myThread.start(); 
			}
		}

	// Run method
	public synchronized void run() {
	
		long sleepTime = 0;
		long startTime = 0;
		
		while (Thread.currentThread()==myThread) {

			startTime = System.currentTimeMillis();
			
			if ((!paused)&&(!gameover)) {

				// Level life
				if ((nbBlocksLeft==0) || goToNext) jumpToNextLevel();

				// Paddle move
				if (paddle != null) {
					// Just follow the ball in demo mode
					if (demoMode) { paddle.followBall(balls[0].getX() + balls[0].getWidth() / 2); }	
					// When using keyboard, paddle moves according to the last key pressed
					else if (keyboard) {
						if (lastKey==KeyEvent.VK_LEFT) paddle.followKeys(-1);
						else if (lastKey==KeyEvent.VK_RIGHT) paddle.followKeys(1);
						}
					paddle.live();	
					}

				// Ball move
				for (int i=0;i<nbBalls;i++) { 	
					if (balls[i] != null) {
						if (balls[i].isAlive()) balls[i].live();
						else ballLost(i);
						}
					}

	 			// Pills move
				for (int i=0;i<nbPills;i++) {
					if (pills[i] != null) {
						if (pills[i].isAlive()) pills[i].live();
						else pills[i]=null;
						}
					}

				// Bullets move
				for (int i=0;i<nbBullets;i++) {
					if (bullets[i] != null) {
						if (bullets[i].isAlive()) bullets[i].live();
						else bullets[i]=null;
						}
					}

				// Repaint & notify the applet thread
				repaint(); 
			
				// Wait for notify
				try { wait(); } 
				catch (InterruptedException e) { }
				
				// Stops if gameover is set
				if (gameover) stopGame();

				// Pause if pause is set
				if (pause) { paused = true; pause = false; }			
				}
			
			// Counter before demo starts
			else if (!paused) {
				demoWait -= gameRate;
				if (demoWait<0) { startGame(true); }
				// Pause the game if pause is set
				if (pause) { paused = true; pause = false; }			
				}

			// Compute sleeping time for next loop
			sleepTime = startTime + gameRate - System.currentTimeMillis();
			// parent.showStatus ("Sleep : " + Math.max(sleepTime, 0) + "ms.");
			
			// Sleep if the game's running too fast
			if (sleepTime > 0) {
				try { Thread.sleep(sleepTime); }
				catch (InterruptedException e) { }
				}
			}
		}

	// Ball launch
	public void launchBall() { 
		if (!demoMode) {
			if (fireMode) throwBullet(paddle.getX() + paddle.getWidth() / 2, paddle.getY());
			else { 
				status.hideName(); 
				for (int i=0;i<nbBalls;i++) { 
					if ((balls[i]!=null) && (!balls[i].isLaunched())) balls[i].launch(); 
					}
				}
			}
		}
	
	// Pill events
	public void pillEvent (int v) {

		// Reseting objects	
		paddle.setNormal(); fireMode = false;
		for (int i=0;i<nbBalls;i++) { 
			if (balls[i]!=null) {
				balls[i].setKiller(false);
				balls[i].setGlue(false);
				if (!balls[i].isLaunched()) { balls[i].launch(); }
				}
			}

		switch(v) {
			// Set multi-ball mode		
			case 0: 
				throwMultiBall(); 
				break;
			// Set fire mode
			case 1: 
				paddle.setFire(); 
				fireMode = true; 
				break;
			// Set glue mode
			case 2: 
				for (int i=0;i<nbBalls;i++) { 
					if (balls[i]!=null) balls[i].setGlue(!demoMode); 
					} 
				break;
			// Set big paddle				
			case 3: 
				paddle.setBig(); 
				break;
			// Set small paddle
			case 4: 
				paddle.setSmall(); 
				break;
			// Add bonus life, bonus score or gives super ball !
			case 5: 
				switch((int)(6 * Math.random())) {
					case 0: 
						status.addLife(); 	
						break;
					case 1: 
						for (int i=0;i<nbBalls;i++) { 
							if (balls[i]!=null) balls[i].setKiller(true); 
							} 
						break;
					default: 
						status.addBonus(); 
						break;
					}
				break;											
			}
		}

	// Next level
	void jumpToNextLevel() {

		// Reset current level
		for (int i=0;i<nbPills;i++) pills[i] = null;
		for (int i=0;i<nbBullets;i++) bullets[i] = null;
		for (int i=0;i<nbBalls;i++) balls[i] = null;
		goToNext = false;
		
		// Force garbage collection
		System.gc();
		
		// Compute the same level in demo mode
		if (demoMode) {
			clearBackground();
			computeLevel();
			status.sameLevel();
			}
		// Compute next level
		else {
			currentLevel++;
			if (currentLevel>=nbLevels) currentLevel = 0;
		  clearBackground();
			computeLevel();
			status.addLevel(); 
			}
			
		// New ball & paddle reset
		balls[0] = new jnball(this, paddle, ballImg); 
		paddle.setNormal(); 
		fireMode = false; 

		// Automatic start or level name display whether demo or not
		if (demoMode) balls[0].launch();
		else status.showName(levelNames[currentLevel]);
		}

	void computeLevel() {
		char blockRow[];
		nbBlocksLeft = 0;
		for (int y=0;y<nbLevelRows;y++) { 
			for (int x=0;x<nbLevelCols;x++) { 
				levelMap[x][y] = null; 
				} 
			}
		for (int r=0;r<nbLevelRows;r++) {
			blockRow = levelDefinitions[currentLevel][r].toCharArray();
			for (int c=0;c<nbLevelCols;c++) {
				switch(blockRow[c]) {
				  case 'F':	
						levelMap[c][r] = new jnblock(this, blockImg[15], c, r, -1); 
						break; 
					case 'E': 
						levelMap[c][r] = new jnblock(this, blockImg[14], c, r, 2); 
						nbBlocksLeft++; 
						break;
				  case 'D':	
						levelMap[c][r] = new jnblock(this, blockImg[13], c, r, 0); 
						nbBlocksLeft++; 
						break; 
					case 'C': 
						levelMap[c][r] = new jnblock(this, blockImg[12], c, r, 0); 
						nbBlocksLeft++; 
						break;
				  case 'B':	
						levelMap[c][r] = new jnblock(this, blockImg[11], c, r, 0); 
						nbBlocksLeft++;
						break; 
					case 'A': 
						levelMap[c][r] = new jnblock(this, blockImg[10], c, r, 0); 
						nbBlocksLeft++; 
						break;
					case '9' : case '8' : case '7' : case '6' : case '5' : 
					case '4' : case '3' : case '2' : case '1' : case '0' : 
						levelMap[c][r] = new jnblock(this, blockImg[blockRow[c] - '0'], c, r, 0); 
						nbBlocksLeft++; break;
					default : levelMap[c][r] = null; 
						break;
					}
				}		
			}
		}

	// Ball lost
	void ballLost(int j) {
		balls[j]=null;
		if (j==0) {
			boolean found = false;
			for (int i=1;i<nbBalls;i++) {
				if ((balls[i]!=null) && (!found)) { 
					balls[0]=balls[i]; 
					balls[i]=null; 
					found = true; 
					}
				}
			if (!found) {
				for (int i=0;i<nbPills;i++) pills[i] = null;
				for (int i=0;i<nbBullets;i++) bullets[i] = null;
				status.removeLife();
				
				// Game Over ...
				if (status.getLives() < 0) {
					status.showGameOver();
					gameover = true;
					}

				// Next ball
				else {
					balls[0] = new jnball(this, paddle, ballImg);
					paddle.setNormal(); 
					fireMode = false;
					}
				}
			}
		}

	// Pill throwing
	void throwPill(int x, int y) {
		int i = 0; int v = (int)(nbPillValues * Math.random());
		while ((i<nbPills) && (pills[i]!=null)) i++; 
		if (i<nbPills) pills[i] = new jnpill(this, paddle, pillImg[v], v, x, y);
		}
	// Bullet throwing
	void throwBullet(int x, int y) {
		int i = 0; 
		while ((i<nbBullets) && (bullets[i]!=null)) i++; 
		if (i<nbBullets) bullets[i] = new jnbullet(this, bulletImg[0], x, y);
		}
	// MultiBallThrowing
	void throwMultiBall() {
		for (int i=1;i<nbBalls;i++) {
			if (balls[i]==null) {
				balls[i] = new jnball(this, paddle, ballImg, balls[0], i);
				}
			}
		}

	// Collision detection & effects for blocks
	public jnblock block(int x, int y) {
		int c = x / colWidth; int r = y / rowHeight; 
		if ((c<nbLevelCols) && (r<nbLevelRows)) return levelMap[c][r];
		else return null;
		}
	public boolean hitBlock(int x, int y, boolean kill) { 
		int c = x / colWidth; int r = y / rowHeight;
		boolean found = ((c<nbLevelCols) && (r<nbLevelRows) && (levelMap[c][r]!=null));
		if (found) {
			if (kill) {
				if (levelMap[c][r].kill()) {
					nbBlocksLeft--; 
					status.addScore(); 
					if (Math.random()<0.2) throwPill(x, y);
					}
				levelMap[c][r] = null;
				}
			else if (levelMap[c][r].hit()) { 
				nbBlocksLeft--; 
				levelMap[c][r] = null;
				status.addScore(); 
				if (Math.random()<0.2) throwPill(x, y);
				}
			}
		return ((found) && (!kill));
		}		

	// Forced jump to next level
	public void forceNextLevel () { goToNext = true; }

	// Unlock balls when no solutions
	public void unlockBalls() {
		for (int i=0;i<nbBalls;i++) { if (balls[i]!=null) balls[i].setRandomDX(); }
		}

	// Game speed settings
	public void setGameRate (int i) { gameRate = (long)(5+5*i); }
	
	// Sound play
	public void playStart() { playSound(0, false); }
	public void playTouch() { playSound(1, false); }
	public void playHit() { playSound(2, false); }
	public void playSound (int i, boolean l) {
		if ((soundOn) && (i<nbSounds)) {
			sounds[currentSound].stop();
			currentSound = i;
			if (l) sounds[currentSound].loop();
			else sounds[currentSound].play();
			}
		}
	public void toggleSound() { 
		if (soundOn) sounds[currentSound].stop();
		soundOn = (!soundOn);
		}

	// Event handlers
	public void mouseClicked(MouseEvent evt) { }
	public void mouseEntered(MouseEvent evt) { }
	public void mouseExited(MouseEvent evt) { }
	public void mousePressed(MouseEvent evt) {
		keyboard = false;
		if ((!demoMode) && (paddle!=null)) launchBall();
		else requestFocus();
		}
	public void mouseReleased(MouseEvent evt) { }
	public void mouseDragged(MouseEvent evt) { 
		if ((!demoMode) && (paddle!=null)) paddle.followMouse(evt.getX());
		}
	public void mouseMoved(MouseEvent evt) { 
		if ((!demoMode) && (paddle!=null)) paddle.followMouse(evt.getX());
		}
	public void keyTyped(KeyEvent evt) { }
	public void keyReleased(KeyEvent evt) {
		int key = evt.getKeyCode();
		if (!(paused||gameover||demoMode)&&(key==lastKey)) lastKey = KeyEvent.VK_UNDEFINED;
		}
	public void keyPressed(KeyEvent evt) {
		int key = evt.getKeyCode(); 
		// In game actions
		if ((key==KeyEvent.VK_CONTROL)||(key==KeyEvent.VK_SPACE)) {
			if (!(paused||gameover||demoMode)) launchBall();
			}
		else if (key==KeyEvent.VK_LEFT) {
			if (!(paused||gameover||demoMode)) { keyboard = true; lastKey = key; }
			}
		else if (key==KeyEvent.VK_RIGHT) { 
			if (!(paused||gameover||demoMode)) { keyboard = true; lastKey = key; }
			}
		else if (key==KeyEvent.VK_U) {
			if (!(paused||gameover||demoMode)) unlockBalls();
			}
		// Pause / resume
		else if (key==KeyEvent.VK_P) {
			if (paused) { paused = false; parent.setPauseButton(true); }
			else if (!gameover) { status.showPause(demoMode); pause = true; parent.setPauseButton(false); }
			}
		// Start a new game
		else if (key==KeyEvent.VK_N) {
			if (!paused&&(gameover||demoMode)) {
				if (!gameover) stopGame();
				startGame(false);
				}
			}
		// Settings
		else if (key==KeyEvent.VK_S) { toggleSound(); parent.setSoundCheck(); } 
		else if (key==KeyEvent.VK_9) setGameRate(0);
		else if (key==KeyEvent.VK_8) setGameRate(1);
		else if (key==KeyEvent.VK_7) setGameRate(2);
		else if (key==KeyEvent.VK_6) setGameRate(3);
		else if (key==KeyEvent.VK_5) setGameRate(4);
		else if (key==KeyEvent.VK_4) setGameRate(5);
		else if (key==KeyEvent.VK_3) setGameRate(6);
		else if (key==KeyEvent.VK_2) setGameRate(7);
		else if (key==KeyEvent.VK_1) setGameRate(8);
		}
	}

// ## JAVANOID STATUS CLASS #############################################
// This class maintains and displays the state of the game (score, lives 
// & level). 

final class jnstatus {

	// strings for display
	final static String appletString1 = "JAVANOID V1.54";
	final static String appletString2 = "by Rémi Faitout";
	final static String scoreString = "SCORE : ";
	final static String levelString = "LEVEL : ";
	final static String livesString = "LIVES : ";
	final static String hiScoreString = "HIGH SCORE : ";
	final static String gameOverString = "GAME OVER";
	final static String pauseString = "PAUSED";
	
	// default game settings
	final static int nbLives = 2;	
	final static int maxLives = 5;
	final static int lifeSize = 12;
	final static int hitScore = 10;
	final static int bonusScore = 100;
	final static int levelScore = 500;

	// status values
	protected int hiScore = 1000; 
	protected int score = 0;
	protected int lives = 0;
	protected int level = 0;

	// String positions	
	protected int scoreX, scoreXX, livesX, livesXX, levelX, namesX;
	protected int statusY, textY, namesY, livesY;
	protected int width, height;	
	protected int livesWidth, scoreWidth, levelWidth;

	// display the level name or not	
	protected boolean nameDisplay;
	
	jnbuffer buffer;
	Image lifeImage;
	
	// Constructor
	public jnstatus(jnbuffer b, Image i, int alt1, int alt2 ) {
		buffer = b;
		lifeImage = i;
		width = buffer.getWidth();
		height = buffer.getStringHeight();	
		livesX = 1 * width / 18; 
		levelX = 8 * width / 18; 
		scoreX = 12 * width / 18; 
		namesX = width / 2;
		livesXX = livesX + buffer.getStringWidth(livesString);
		scoreXX = scoreX + buffer.getStringWidth(scoreString);
		statusY = buffer.getHeight() - alt1;
		namesY = buffer.getHeight() - alt2;
		livesY = statusY - lifeSize + buffer.getStringHeight();
		nameDisplay = false;
		}
			
	// Actions on the status : add or remove
	public void addLife() { 
		if (lives<maxLives) buffer.drawPermanentImage(lifeImage, livesXX + lives * lifeSize, livesY); 
		lives++;  
		}
	public void removeLife() { lives--; set();}
	public void addLevel() { level++; score+=levelScore; set();}
	public void sameLevel() { score=0; set(); }
	public void addScore() { score+=hitScore; setScore();}
	public void addBonus() { score+=bonusScore; set(); }

	// hiding or showing the level name
	public void showName(String s) {
		nameDisplay = true;
		buffer.drawPermanentString(s, namesX - buffer.getStringWidth(s) / 2, namesY + height);
		}
	public void hideName() {
		if (nameDisplay) buffer.clearPermanentImage(0, namesY, width, height);
		}

	// showing high scores
	public void showHiScore() { 
		int h = (int)(height/2);
		String scoreString = hiScoreString + hiScore;
		buffer.drawPermanentString(appletString1, namesX - buffer.getStringWidth(appletString1) / 2, namesY - 2*h);
		buffer.drawPermanentString(appletString2, namesX - buffer.getStringWidth(appletString2) / 2, namesY + h);
		buffer.drawPermanentString(scoreString, namesX - buffer.getStringWidth(scoreString) / 2, namesY + 5*h);		
		}
		
	// Show game over and pause messages
	public void showGameOver() {
		buffer.drawString(gameOverString, namesX - buffer.getStringWidth(gameOverString) / 2, namesY + height);
		}
	public void showPause(boolean b) {
		if (!b) buffer.drawString(pauseString, namesX - buffer.getStringWidth(pauseString) / 2, namesY + height);
		}
	
	// tell whether it's a high score
	public boolean isHighScore() { return (score > hiScore); } 
		
	// reset & set (partial or not) : Draw the status on the buffer
	public void reset() { 
		if (score > hiScore) hiScore = score;
		lives = nbLives; 
		level = 1; 
		score = 0; 
		set();
		}
	void set() {
		buffer.clearPermanentImage(0, statusY, width, height);
		buffer.drawPermanentString(livesString, livesX, statusY + height);
		for (int i=0;i<lives;i++) { 
			if (i<maxLives) buffer.drawPermanentImage(lifeImage, livesXX + i * lifeSize, livesY); 
			}
		buffer.drawPermanentString(levelString + level, levelX, statusY + height);
		buffer.drawPermanentString(scoreString + score, scoreX, statusY + height);
		}
	void setScore() {
		buffer.clearPermanentImage(scoreXX, statusY, width - scoreXX, height);
		buffer.drawPermanentString(String.valueOf(score), scoreXX, statusY + height);
		}

	public int getLives() { return lives; }
	public int getScore() { return score; }
	}
		
// ## JAVANOID BALL CLASS ###############################################
// This class represents the balls in the game : they are moving object 
// that have a really hard life (paddle & block collisions)

final class jnball extends jnmovingobject {

	// default attributes
	final static int defaultSpeed = 6;
	final static int defaultWidth = 12;
	final static int defaultHeight = 12;
	final static int defaultImage = 0;
	
	// ball speed (speed^2 = dx^2 + dy^2)
	protected int speed;
	
	// ball mode
	protected boolean launched = false;
	protected boolean glueMode = false;
	protected boolean killMode = false;

	// Default direction & position
	protected int defaultDX = 3;
	protected int posX = 0, posY = 0;
	
	Image images[];
	jngame game;
	jnpaddle paddle;
	
	// Constructor for a normal single ball
	public jnball(jngame g, jnpaddle p, Image i[]) {
		super(g, i[0], defaultWidth, defaultHeight, p.getX() + p.getWidth() / 2, p.getY() - defaultHeight, 0, 0);
		game = g;
		paddle = p;
		images = i;
		speed = defaultSpeed;
		posX = (paddle.getWidth() - width) / 2; 
		posY = - height;
		}
	// Constructor for the second or third ball of the game
	public jnball(jngame g, jnpaddle p, Image i[], jnball b, int n) {
		super(g, i[0], defaultWidth, defaultHeight, b.getX(), b.getY(), 0, 0);
		game = g;
		paddle = p;
		images = i;
		speed = defaultSpeed;
		posX = (paddle.getWidth() - width) / 2; 
		posY = - height;
		if (n==1) setBallDX(b.getDX() + 1, (b.getDY()<0));
		else setBallDX(b.getDX() - 1, (b.getDY()<0)); 
		launched = true;
		}

	// ball launch & live methods
	public void launch() { 
		if (!glueMode) { 
			game.playStart(); 
			setBallDX(defaultDX, true); 
			}
		launched = true; 
		}
		
	// ball life (paddle & block detection)
	public void live() {
		int oldX = X; 
		int oldY = Y;

		// Follow paddle when not launched
		if (!launched) { X = paddle.getX() + posX; Y = paddle.getY() + posY; }
		
		// Ball movement
		super.live();

		if (launched) {
			if (dY > 0) {
				// Paddle hit 
				if (paddle.isHit(X + width / 2, Y + height)) {
					if (glueMode) { 
						launched = false; 
						posX = X - paddle.getX(); 
						posY = Y - paddle.getY(); 
						}
					setBallDX(paddle.hit(X + width / 2, dX, speed), true); 
					}
				else if (game.hitBlock(X + width / 2, Y + height, killMode)) invertDY();
				}
			else if (game.hitBlock(X + width / 2, Y, killMode)) invertDY(); 
			if (dX < 0) { 
				if (game.hitBlock(X, Y + height / 2, killMode)) invertDX();	 
				}
			else if (game.hitBlock(X + width, Y + height / 2, killMode)) invertDX(); 
			}
		}
	
	// Ball settings
	public void setBallDX (int d, boolean b) { 
		dX = d; 
		dY = (int)(Math.max(Math.sqrt((double)(speed * speed - dX * dX)), 1));
		if (b) dY = - dY; 
		}
	public void setSpeed (int s) { speed = s; }
	public void setGlue (boolean g) { glueMode = g; }
	public void setKiller (boolean g) { 
		killMode = g; 
		if (killMode) change(images[1], defaultWidth, defaultHeight);
		else change(images[0], defaultWidth, defaultHeight);
		}
	public void setRandomDX() { 
		setBallDX((int)((speed - 1) * (2 * Math.random() - 1)), false);
		}

	// Ball requests
	public int getSpeed() { return speed; }
	public boolean isLaunched() { return launched; }
	}

// ## JAVANOID PILL CLASS ###############################################
// This class represents the pills in the game : They are moving objects 
// with a value that gives their effects on the game when they're catched 
// by the paddle

final class jnpill extends jnmovingobject {

	// Default attributes
	final static int defaultSpeed = 3;
	final static int defaultWidth = 16;
	final static int defaultHeight = 8;
	
	// Animation frame
	protected int pillNbFrames;
	protected double pillStep = 0.25;
	protected double pillFrame;
	
	// Values gives the effect
	protected int pillValue;

	Image images[];
	jngame game;
	jnpaddle paddle;

	// Constructor
	public jnpill(jngame g, jnpaddle p, Image i[], int v, int bx, int by) {
		super(g, i[0], defaultWidth, defaultHeight, bx, by, 0, defaultSpeed);
		game = g;
		paddle = p; 
		images = i;
		pillNbFrames = images.length;
		pillValue = v;
		pillFrame = 0;
		}

	// Pill life (paddle detection + dies when at the bottom of the screen)
	public void live() {
	
		// Pill animation
		pillFrame+=pillStep;
		if (pillFrame>=pillNbFrames) pillFrame = 0;
		change(images[(int)pillFrame], defaultWidth, defaultHeight);

		// Pill move
		super.live();

		// Pill death
		if (paddle.isHit(X + width / 2, Y + height)) {
			game.playHit(); 
			game.pillEvent(pillValue); 
			die(); 
			}
		}
	}
	
// ## JAVANOID BULLET CLASS #############################################
// This class represents the paddle bullets : They are very similar to 
// pills except they move from bottom to top and hit the blocks.

final class jnbullet extends jnmovingobject {

	final static int defaultSpeed = 6;
	final static int defaultWidth = 4;
	final static int defaultHeight = 6;

	jngame game;	
	
	// Constructor
	public jnbullet(jngame g, Image i, int px, int py) {
		super(g, i, defaultWidth, defaultHeight, px, py, 0, - defaultSpeed);
		game = g;
		}

	public void live() {
		super.live();
		if ((dY > 0) || (game.hitBlock(X + width / 2, Y, false))) { die(); }
		}
	}

// ## JAVANOID PADDLE CLASS #############################################
// This class represents the paddle in the game : It a 'static' object 
// that can be moved to a position (ex : mouse position). It also gives 
// the direction of the ball.

final class jnpaddle extends jnobject {

	// Default attributes
	final static int defaultWidth = 40;
	final static int defaultWidthBig = 48;
	final static int defaultWidthSmall = 32;
	final static int defaultHeight = 12;
	final static int defaultImage = 0;
	
	// Paddle position a movement
	protected int stepLength = 8;
	protected int minX, maxX;	
	protected int newX;
	protected boolean demo;
	
	jngame game;
	Image images[];
	
	// Constructor
	public jnpaddle (jngame g, Image[] i, int alt, boolean d) { 
		super(g, i[0], defaultWidth, defaultHeight, (g.getWidth() - defaultWidth) / 2, g.getHeight() - alt);
		game = g;
		images = i;
		demo = d;
		minX = 0; 
		maxX = game.getWidth() - defaultWidth;
		newX = X;
		}

	// Paddle life (move to the saved position)
	public void live() { 
		setX(newX); 
		super.live();	
		}
	
	// Paddle settings
	public int hit(int bx, int bdx, int s) {
		int ndx = 2 * s * (bx - X - width / 2) / width; 
		game.playTouch(); 
		return (ndx==0) ? bdx : ndx;
		}

	// Methods of controls
	public void followBall (int bx) { newX = bx - width / 2; }
	public void followMouse (int mx) { newX = mx - width / 2; }
	public void followKeys (int dir) { newX = X + dir*stepLength; }
	
	// Paddle settings
	public void setNormal() { change(images[0], defaultWidth, defaultHeight); }
	public void setSmall () { change(images[1], defaultWidthSmall, defaultHeight); }
	public void setBig () { change(images[2], defaultWidthBig, defaultHeight); }
	public void setFire () { change(images[3], defaultWidth, defaultHeight); }
	}
	
// ## JAVANOID BLOCK CLASS ##############################################
// Blocks are basic 'static object', except they die after a certain 
// number of hits. As they don't move at all, they are drawn or erased on 
// the permanent buffer.

final class jnblock extends jnobject {

	final static int defaultWidth = 24;
	final static int defaultHeight = 16;
	final static int defaultImage = 0;

	// i.e. nb hits before it dies
	protected int resistance;	

	// position in the level	
	protected int column, row;
		
	// Constructor
	public jnblock (jngame g, Image i, int col, int row, int r) {
		super(g, i, defaultWidth, defaultHeight, col * defaultWidth, row * defaultHeight);
		resistance = r;
		}

	// Life : no drawing there !!!
	public void live() {}	
	
	// Blocks are "permanent" images : Draw on the background & erase
	public void draw() { game.drawPermanentImage(image, X, Y); }
	public void clear() { game.clearPermanentImage(X, Y, width, height); }

	// Death depends on resistance
	public void die() { 
		if (resistance==0) { 
			clear(); 
			super.die(); 
			} 
		else resistance--; 
		}
	
	// Hit method (tells if it dies or not)
	public boolean hit() { 
		die(); 
		if (alive) game.playTouch(); 
		else game.playHit(); 
		return alive ? false : true;
		}
		
	// Kill method (dies & tells if it's a gold one)
	public boolean kill() { 
		clear(); 
		super.die(); 
		return (resistance >= 0); 
		}
	}
	
// ## JAVANOID BUFFER CLASS #############################################
// This class implements a sort of triple buffer : one for the background 
// image & the objects that you draw for a long time, one where the moving 
// objects are drawn, and one for display (see update method).

abstract class jnbuffer extends Panel {

	// Width & height of the game canvas
	protected int width;
	protected int height;
	protected int numBackground = 0;
	protected int nbBackgrounds = 6;
	protected int backgroundColors[][] = {
		{255, 191, 0  , 0  , 191, 0  , 0  , 0  , 255, 0  , 191, 255}, // yellow, green, blue & cyan
		{0  , 191, 255, 0  , 0  , 255, 255, 0  , 0  , 255, 0  , 255}, // cyan, blue, red & pink
		{255, 0  , 255, 255, 0  , 0  , 0  , 191, 0  , 255, 191, 0  }, // pink, red, green & yellow
		{255, 191, 0  , 255, 0  , 0  , 0  , 0  , 255, 255, 0  , 255}, // yellow, red, blue & pink
		{255, 0  , 255, 0  , 0  , 255, 0  , 191, 0  , 0  , 191, 255}, // pink, blue, green & cyan
		{0  , 191, 255, 0  , 191, 0  , 255, 0  , 0  , 255, 191, 0  }};// cyan, green, red & yellow
		 
	// Frame rate
	protected int frameRate = 1;
	protected int currentFrame = 0;
	
	// References
	Applet applet;

	// On & off graphics for triple (!) buffering
	Graphics offGraphics, offBackGraphics;

	// Fontmetrics object
	FontMetrics fontMetrics;

	// Buffer & background images
	Image offImage, offBackImage, backImage;
	Dimension minSize;

	// Font style for text
	Font textFont = new Font("SansSerif", Font.BOLD, 12);

	// Color for texts & background
	Color textColor = new Color (255, 223, 0);
	Color backColor = new Color (0, 0, 0);
	
	// Constructor
	public jnbuffer (Applet a, int w, int h) { 
		applet = a; 
		width = w; 
		height = h;

		// Off backround graphics : "Permanent" background graphics
		offBackImage = applet.createImage(width, height);
		offBackGraphics = offBackImage.getGraphics();
		offBackGraphics.setColor(textColor);
		offBackGraphics.setFont(textFont);

		// Off graphics : Double buffer graphics
		offImage = applet.createImage(width, height);
		offGraphics = offImage.getGraphics();
		offGraphics.setColor(textColor);
		offGraphics.setFont(textFont);

		// Dimension
		fontMetrics = offGraphics.getFontMetrics();
		minSize = new Dimension(width, height);
		setSize(width, height);
		clearBackground();
		}

	// Compute background image
	void computeBackground() {
		int[] pix = new int[width * height];
		int index = 0;
		double alphai, alphaj, betai, betaj;
		int r1, r2, r, g1, g2, g, b1, b2, b;
		if (numBackground >= nbBackgrounds) numBackground = 0;

		// Make a fade between the four corner colors
		for (int j=0;j<height;j++) {
			alphaj = (double)j / (height - 1);
			betaj = 1.0 - alphaj;
			r1 = (int)(backgroundColors[numBackground][3]*alphaj + backgroundColors[numBackground][0]*betaj);
			r2 = (int)(backgroundColors[numBackground][9]*alphaj + backgroundColors[numBackground][6]*betaj);
			g1 = (int)(backgroundColors[numBackground][4]*alphaj + backgroundColors[numBackground][1]*betaj);
			g2 = (int)(backgroundColors[numBackground][10]*alphaj + backgroundColors[numBackground][7]*betaj);
			b1 = (int)(backgroundColors[numBackground][5]*alphaj + backgroundColors[numBackground][2]*betaj);
			b2 = (int)(backgroundColors[numBackground][11]*alphaj + backgroundColors[numBackground][8]*betaj);
			for (int i=0;i<width;i++) {
				alphai = (double)i / (width - 1);
				betai = 1.0 - alphai;
				r = (int)(r2*alphai + r1*betai);
				g = (int)(g2*alphai + g1*betai);
				b = (int)(b2*alphai + b1*betai);
				pix[index++] = (255 << 24) | (r << 16) | (g << 8) | b;
				}
			}
		backImage = applet.createImage(new MemoryImageSource(width, height, pix, 0, width));

		// Next background
		numBackground++;
		}

	public void drawImage(Image i, int x, int y) { 
		offGraphics.drawImage(i, x, y, this); 
		}
	public void drawString(String s, int x, int y) { 
		offGraphics.drawString(s, x, y); 
		}
	public void drawPermanentString(String s, int x, int y) { 
		offBackGraphics.drawString(s, x, y); 
		}
	public void drawPermanentImage (Image i, int x, int y) { 
		offBackGraphics.drawImage(i, x, y, this); 
		}
	public void clearPermanentImage (int x, int y, int w, int h) {
		offBackGraphics.drawImage(backImage, x, y, x + w, y + h, x, y, x + w, y + h, this);
		}
	public void clearBackground() {
		computeBackground(); 
		if (offBackGraphics==null) {
			offBackGraphics = offBackImage.getGraphics();
			offBackGraphics.setColor(textColor);
			offBackGraphics.setFont(textFont);
			}
		offBackGraphics.drawImage(backImage, 0, 0, this);
		}		

	// Update & paint methods
	public void paint(Graphics g) { 
		if (g==null) g = this.getGraphics();
		g.drawImage(offImage, 0, 0, this);
		}
	public synchronized void update(Graphics g) {

		// Repaint the new image
		currentFrame++;
		if (currentFrame >= frameRate) {
			currentFrame = 0;
			if (g==null) g = this.getGraphics();
			g.drawImage(offImage, 0, 0, this);
			}
			
		// Repaint the back image
		if (offGraphics==null) {
			offGraphics = offImage.getGraphics(); 
			offGraphics.setColor(textColor);
			offGraphics.setFont(textFont);
			}
		offGraphics.drawImage(offBackImage, 0, 0, this);

		// Notify the game thread
		notifyAll(); 
		}

	// Frame rate setting
	public void setFrameRate (int r) { frameRate = r; }

	// Dimension functions
	public int getStringWidth(String s) { return fontMetrics.stringWidth(s); }
	public int getStringHeight() { return fontMetrics.getHeight(); }
	public int getStringAscent() { return fontMetrics.getAscent(); }
	public int getWidth() { return width; }
	public int getHeight() { return height; }

	// Canvas resizing methods
	public Dimension getPreferredSize() { return getMinimumSize(); }
	public synchronized Dimension getMinimumSize() { return minSize; }
	}
	
// ## JAVANOID MOVING OBJECT ############################################
// A javanoid moving object is an object that moves by itself. It has dX 
// & dY values that give its movement in relative coordinates

abstract class jnmovingobject extends jnobject {

	// Initial & current movement of the object
	protected int dX0, dY0;
	protected int dX, dY;

	// Constructor
	public jnmovingobject(jngame g, Image i, int w, int h, int x, int y, int dx, int dy) {
		super(g, i, w, h, x, y);
		dX0 = dx; 
		dY0 = dy;
		dX = dx; 
		dY = dy;
		}
	public void die() { 
		dX = 0; 
		dY = 0; 
		super.die(); 
		}

	// Object movement settings
	public void setX(int x) {
		if (x > maxX) { 
			X = maxX; 
			invertDX();
			// game.playTouch();
			} 
		else if (x < minX) {
			X = minX; 
			invertDX(); 
			// game.playTouch();
			}
		else X = x;
		}
	public void setY(int y) {
		if (y > maxY) die();  
		else if (y < minY) { 
			Y = minY; 
			invertDY(); 
			// game.playTouch();
			}
		else Y = y;
		}
	public void setDXDY(int dx, int dy) { dX = dx; dY = dy; }
	public void setDX(int dx) { dX = dx; }
	public void setDY(int dy) { dY = dy; }
	public void invertDXDY() { dX = - dX; dY = - dY; } 
	public void invertDX() { dX = - dX; }
	public void invertDY() { dY = - dY; }
	public void reset() { 
		dX = dX0; 
		dY = dY0; 
		super.reset(); 
		}

	// Object life : move + drawing (dies when at the bottom of the screen) 
	public void live() { 
		if (alive) {
			setX(X + dX); 
			setY(Y + dY); 
			draw();
			}
		}

	// Object requests
	public int getDX() { return dX; }
	public int getDY() { return dY; }
	}

// ## JAVANOID OBJECT ###################################################
// It's the basic object of javanoid. Though it doesn't move by itself, 
// it can be moved to absolute coordinates (ex: the paddle, which moves 
// to the position of the mouse pointer). A javanoid object lives (i.e. 
// is on the screen) & dies when it touches the bottom

abstract class jnobject extends Object {

	// Width & height of the object
	protected int width, height;

	// Initial & current position of the object
	protected int X0, Y0;	
	protected int X, Y;

	// X & Y limits for the canvas
	protected int minX, maxX;
	protected int minY, maxY;

	// Tells if the object is still alive
	protected boolean alive;

	// The buffer where the object is drawn
	jngame game;

	// The bitmap image of the object	
	Image image;

	// Constructor
	public jnobject(jngame g, Image i, int w, int h, int x, int y) {
		game = g;
		image = i; 
		width = w; 
		height = h;
		X0 = x; 
		Y0 = y;
		X = x; 
		Y = y;
		minX = 0; 
		maxX = game.getWidth() - width;
		minY = 0; 
		maxY = game.getHeight() - height;
		alive = true;
		draw();
		}

	// Object position settings
	public void setXY(int x, int y) { setX(x); setY(y); }
	public void setX(int x) {
		if (x > maxX) X = maxX;  
		else if (x < minX) X = minX;
		else X = x;
		}
	public void setY(int y) {
		if (y > maxY) Y = maxY;  
		else if (y < minY) Y = minY;
		else Y = y;
		}
	public void reset() { X = X0; Y = Y0; }

	// Object drawing & erasing
	public void draw() { game.drawImage(image, X, Y); }

	// Objet life (what he does until he's dead)
	public void live() { if (alive) draw(); }

	// Object death
	public void die() { alive = false; }

	// Collision detection
	public boolean isHit(int x, int y) { 
		return (alive) && (x > X) && (x < X + width) && (y > Y) && (y < Y + height); 
		}
	
	// Object graphic changes
	public void change(Image i, int w, int h) {
		image = i; width = w; height = h;
		minX = 0; maxX = game.getWidth() - width;
		minY = 0; maxY = game.getHeight() - height;
		}
	
	// Object requests
	public boolean isAlive() { return alive; }
	public int getWidth() { return width; }
	public int getHeight() { return height; }
	public int getX() { return X; }
	public int getY() { return Y; }
	}	


