import java.awt.Frame;
import java.awt.event.*;

import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.*;
import com.jogamp.opengl.util.awt.TextRenderer;
import java.awt.Font;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class Steering_Behaviors implements GLEventListener, KeyListener, MouseListener, MouseMotionListener{
	// Graphical constants:
	public static final String WINDOW_TITLE = "COMP452 A1"; 
	public static final int INITIAL_WIDTH = 1000;// [px]
	public static final int INITIAL_HEIGHT = 1000;// [px]
	public static final String FONT = "SansSerif";
	public static final int FONT_SIZE = 40;
	public static final long FPS = 60;// How often should the game be re-drawn? [Hz] 
	static final float ACTOR_SCALE  = 35f;// [px]
	final Colour FOLLOWER_COLOUR = new Colour(0.5f, 0.5f, 0.5f);
	final Colour LEADER_COLOUR = new Colour(0, 0, 0);// Leader is invisible
	static final Colour CLEAR_COLOUR = new Colour(0, 0, 0);// Colour to reset to after drawing 
	static final Colour PLAYER_COLOUR = new Colour(0, 1, 0);
//	static final String DIRT_PATH = "/dirt.png";
//	static final String GRASS_PATH = "/grass.png";
//	static final String STONE_PATH = "stone.jpg";
	static final float[][] ACTOR_SHAPE = { {0.7f, 0.0f},// Right 
			{0.6657395614066074f, 0.21631189606246318f},
			{0.5663118960624631f, 0.4114496766047312f},
			{0.4114496766047312f, 0.5663118960624631f},
			{0.2163118960624632f, 0.6657395614066074f},
			{0.0f, 1.0f},// Top
			{-0.21631189606246312f, 0.6657395614066075f},
			{-0.4114496766047311f, 0.5663118960624631f},
			{-0.5663118960624631f, 0.41144967660473125f},
			{-0.6657395614066074f, 0.21631189606246323f},
			{-0.7f, 0.0f},// Left
			{-0.6657395614066075f, -0.2163118960624631f},
			{-0.5663118960624631f, -0.4114496766047311f},
			{-0.41144967660473125f, -0.5663118960624631f},
			{-0.2163118960624633f, -0.6657395614066074f},
			{0.0f, -0.7f},// Bottom
			{0.21631189606246304f, -0.6657395614066075f},
			{0.41144967660473103f, -0.5663118960624632f},
			{0.5663118960624631f, -0.4114496766047313f},
			{0.6657395614066074f, -0.21631189606246332f} };
	static final float[][] SQUARE_SHAPE = { {-1, 1}, {-1, -1}, {1, -1}, {1, 1} };
	
	// Actor constants:
	static final double PLAYER_MAX_HP = 1;
	static final double NPC_MAX_HP = 1;
	static final double PLAYER_MAX_SPEED = 200;// [px/s]
	static final double NPC_MAX_SPEED = 150;// [px/s] 
	
	// Steering constants:
	static final double NPC_MAX_ACCEL = NPC_MAX_SPEED/3;// [px/s^2]
	static final double SLOW_RADIUS = ACTOR_SCALE * 1.5;
	public static final double TARGET_RADIUS = ACTOR_SCALE;
	static final double THRESHOLD = ACTOR_SCALE * 3;// Separation threshold [px]
	static final double DECAY_COEFF = NPC_MAX_ACCEL * 5000;// Separation decay coefficient 
	
	// TODO: Put variables here:
	TextRenderer textRenderer = null;// For printing messages to user
	ArrayList<NPC> npcs = new ArrayList<NPC>();
	Player player = null;
	ArrayList<Terrain> terrains = new ArrayList<Terrain>();
	long currTime = 0;// Time at current frame
	long prevTime = 0;// Time at previous frame
	Vector2D mouseVector = null;// Vector formed from center of player to mouse
	double deltaT;// Time between frames [s]
	boolean[] activeWASD = {false, false, false, false};// Which movement keys are pressed?
	
	public static void main(String[] args) {
		final Frame frame = new Frame(WINDOW_TITLE);

		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				System.exit(0);
			}
		});
		
		final GLProfile profile = GLProfile.get(GLProfile.GL2);
		final GLCapabilities capabilities = new GLCapabilities(profile);
		final GLCanvas canvas = new GLCanvas(capabilities);
		try {
			Object self = self().getConstructor().newInstance();
			self.getClass().getMethod("setup", new Class[] { GLCanvas.class }).invoke(self, canvas);
			canvas.addGLEventListener((GLEventListener)self);
			canvas.addKeyListener((KeyListener)self);
			canvas.addMouseListener((MouseListener)self);
			canvas.addMouseMotionListener((MouseMotionListener)self);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		canvas.setSize(INITIAL_WIDTH, INITIAL_HEIGHT);
		canvas.setAutoSwapBufferMode(true);

		frame.add(canvas);
		frame.pack();
		frame.setVisible(true);

		System.out.println("\nEnd of processing.");
	}

	private static Class<?> self() {
		return new Object() { }.getClass().getEnclosingClass();
	}
	private int width = 1000;
	private int height = 1000;
	
	public void setup(final GLCanvas canvas) {
		new Timer().scheduleAtFixedRate(new TimerTask() {
			public void run() {
				canvas.repaint();
			}
		}, 1000, 1000/FPS);
	}

	@Override
	public void init(GLAutoDrawable drawable) {
		final GL2 gl = drawable.getGL().getGL2();
		textRenderer = new TextRenderer(new Font(FONT, Font.BOLD, FONT_SIZE));
		gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);// i.e. the background colour
		mouseVector = new Vector2D();
		
		initScene(gl);
	}

	@Override
	public void display(GLAutoDrawable drawable) {
		final GL2 gl = drawable.getGL().getGL2();
		gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
		gl.glLoadIdentity();

		// TODO: Put code here
		prevTime = currTime;
		currTime = System.nanoTime();
		if (prevTime != 0)
			deltaT = (currTime - prevTime)* 1e-9;
		
		if (player.getCurrHP() <= 0)// Is the player dead?
			gameOver(gl);
		else
		{			
			textRenderer.beginRendering(width, height);
			textRenderer.draw("Run away!", width/2-120, height-40);
			textRenderer.endRendering();
			
			updatePlayer(gl);
			updateNPCs();
		//		for (Terrain terrain : terrains)
		//			drawTerrain(gl, terrain);
			drawActor(gl, player);
			for (Actor npc : npcs)
				drawActor(gl, npc);
			}
	}

	@Override
	public void dispose(GLAutoDrawable drawable) {
	}

	@Override
	public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
		final GL2 gl = drawable.getGL().getGL2();

		gl.glViewport(x, y, width, height);

		this.width = width;
		this.height = height;
		
		gl.glMatrixMode(GL2.GL_PROJECTION);
		gl.glLoadIdentity();
		gl.glOrthof(0, width, 0, height, 0.0f, 1.0f);
		gl.glMatrixMode(GL2.GL_MODELVIEW);
		gl.glLoadIdentity();
	}
	
	private void initScene(GL2 gl)
	/* Purpose: Creates and places GameObjects in the world
	 * Remarks: 
	 * - The player and NPCs use the same basic geometry, but different colours
	 * - Every NPC shares the same graphic (i.e. same geometry and colour)
	 * - Flocking:
	 * 			> NPC1 is the pack leader: it steers towards the player; is the first element in npcs
	 * 			> All other NPCs are followers: they arrive towards the pack leader
	 * 			> All NPCs separate from each-other 
	 */
	{
		// ------------------------------------------------------------------------------- PLAYER SETUP
		// Make the actor graphic:
		Shape actorShape = new Shape();
		for (float[] vert : ACTOR_SHAPE)
			actorShape.verts.add(new Point(vert));
		
		// Set up player graphic:
		player = new Player(new Obj(), null, true, PLAYER_MAX_SPEED, PLAYER_MAX_HP);// Create Actor
		Shape playerShape = actorShape.clone();
		playerShape.colour = PLAYER_COLOUR;//  Set actor colour
		player.getObj().shapes.add(playerShape);
		player.getObj().trans = new Trans(new float[] {width/2, height/2, 0, ACTOR_SCALE, ACTOR_SCALE});// Transform actor
		
		// ------------------------------------------------------------------------------- PACK LEADER SETUP
		// Set up NPC1 (pack leader):
		NPC npc1 = new NPC(new Obj(), null, true, NPC_MAX_SPEED, 1, NPC_MAX_HP);// Create Actor
		npcs.add(npc1);// Add to array
		Shape npcShape = actorShape.clone();
		npcShape.colour = LEADER_COLOUR;// Set actor colour
		npc1.getObj().shapes.add(npcShape);
		npc1.getObj().trans = new Trans(new float[] {width/8, height * 7/8, 0, ACTOR_SCALE, ACTOR_SCALE});// Transform actor
		npc1.getSteeringList().add(new Seek(npc1, player, NPC_MAX_ACCEL));// Seek player
		npc1.getSteeringList().add(new FaceForwards(npc1, player, 1.75, 0.25, 0.5, 0.1, 0.1));// Look where you're going
		npc1.getSteeringList().get(0).weight = 1;// Seek weight
		npc1.getSteeringList().get(1).weight = 1;// FaceForwards weight

		// ------------------------------------------------------------------------------- FOLLOWERS SETUP
		// Set up NPC2 (follower):
		NPC npc2 = (NPC)npc1.clone();// Copy npc1
		npc2.getObj().shapes.get(0).colour = FOLLOWER_COLOUR;// Change colour
		npcs.add(npc2);// Add to array
		npc2.getObj().trans = new Trans(new float[] { width*2/8, height * 7/8, 0, ACTOR_SCALE, ACTOR_SCALE});// Transform actor
		npc2.getSteeringList().add(new Arrive(npc2, (GameObject)npc1, // Follow npc1 (pack leader)
				NPC_MAX_SPEED, NPC_MAX_ACCEL, ACTOR_SCALE*1.5, ACTOR_SCALE, 0.1));
		npc2.getSteeringList().add(new Align(npc2, (Actor)npc1, 1.75, 0.25, 0.5, 0.1, 0.1));// Align with npc1 (pack leader)
		npc2.getSteeringList().get(0).weight = 0.1;// Arrive weight
		npc2.getSteeringList().get(0).weight = 1;// Align Weight
		
		// Set up NPC3 (follower):
		NPC npc3 = (NPC)npc2.clone();// Copy npc2
		npcs.add(npc3);// Add to array
		npc3.getObj().trans = new Trans(new float[] { width/8, height * 3/4, 0, ACTOR_SCALE, ACTOR_SCALE});// Transform actor
		npc3.getSteeringList().add(new Arrive(npc3, (GameObject)npc1, // Follow npc1 (pack leader)
				NPC_MAX_SPEED, NPC_MAX_ACCEL, ACTOR_SCALE*1.5, ACTOR_SCALE, 0.1));
		npc3.getSteeringList().add(new Align(npc3, (Actor)npc1, 1.75, 0.25, 0.5, 0.1, 0.1));// Align with npc1 (pack leader)
		npc3.getSteeringList().get(0).weight = 0.1;// Arrive weight
		npc3.getSteeringList().get(0).weight = 1;// Align Weight
		
		// Set up NPC4 (follower):
		NPC npc4 = (NPC)npc2.clone();// Copy npc2
		npcs.add(npc4);// Add to array
		npc4.getObj().trans = new Trans(new float[] { width*1/2, height*7/8, 0, ACTOR_SCALE, ACTOR_SCALE});// Transform actor
		npc4.getSteeringList().add(new Arrive(npc4, (GameObject)npc1, // Follow npc1 (pack leader)
				NPC_MAX_SPEED, NPC_MAX_ACCEL, ACTOR_SCALE*1.5, ACTOR_SCALE, 0.1));
		npc4.getSteeringList().add(new Align(npc4, (Actor)npc1, 1.75, 0.25, 0.5, 0.1, 0.1));// Align with npc1 (pack leader)
		npc4.getSteeringList().get(0).weight = 0.1;// Arrive weight
		npc4.getSteeringList().get(0).weight = 1;// Align Weight

		// Add separation to all follower NPCs:
		for (int i = 1; i < npcs.size(); i++)// For each follower
		{
			// Build a list of targets
			ArrayList<GameObject> targets = new ArrayList<GameObject>();
			for (NPC npc : npcs)
			{
				if (npc != npcs.get(i))// Don't include yourself as a target!
					targets.add((GameObject)npc);
			}
			npcs.get(i).getSteeringList().add(new Separation(npcs.get(i), targets, NPC_MAX_ACCEL, THRESHOLD, DECAY_COEFF));
		}
			
		// Add weights to separation behaviors:
		npc2.getSteeringList().get(2).weight = 0.9;
		npc3.getSteeringList().get(2).weight = 0.9;
		npc4.getSteeringList().get(2).weight = 0.9;
		/*
		// -------------------------------------------------------------------- WORLD STUFF
		try
		{
			// Create textures:
			BufferedImage dirt = ImageIO.read(this.getClass().getResourceAsStream(DIRT_PATH));
			BufferedImage grass = ImageIO.read(this.getClass().getResourceAsStream(GRASS_PATH));
			BufferedImage stone = ImageIO.read(this.getClass().getResourceAsStream(STONE_PATH));
			ImageUtil.flipImageVertically(dirt);
			ImageUtil.flipImageVertically(grass);
			ImageUtil.flipImageVertically(stone);
			Texture dirtTexture = TextureIO.newTexture(AWTTextureIO.newTextureData(gl.getGLProfile(), dirt, false));
			Texture grassTexture = TextureIO.newTexture(AWTTextureIO.newTextureData(gl.getGLProfile(), dirt, false));
			Texture stoneTexture = TextureIO.newTexture(AWTTextureIO.newTextureData(gl.getGLProfile(), dirt, false));
			dirtTexture.setTexParameteri(gl, GL2.GL_TEXTURE_WRAP_S, GL2.GL_REPEAT); // GL_REPEAT or GL_CLAMP
			dirtTexture.setTexParameteri(gl, GL2.GL_TEXTURE_WRAP_T, GL2.GL_REPEAT);
			grassTexture.setTexParameteri(gl, GL2.GL_TEXTURE_WRAP_S, GL2.GL_REPEAT); // GL_REPEAT or GL_CLAMP
			grassTexture.setTexParameteri(gl, GL2.GL_TEXTURE_WRAP_T, GL2.GL_REPEAT);
			stoneTexture.setTexParameteri(gl, GL2.GL_TEXTURE_WRAP_S, GL2.GL_REPEAT); // GL_REPEAT or GL_CLAMP
			stoneTexture.setTexParameteri(gl, GL2.GL_TEXTURE_WRAP_T, GL2.GL_REPEAT);
			
			// Create base Obj for all terrain:
			ArrayList<Point> points = new ArrayList<Point>();
			for (float[] vert : SQUARE_SHAPE)
				points.add(new Point(vert));
			ArrayList<Shape> shapes = new ArrayList<Shape>();
			shapes.add(new Shape(points));
			Obj squareObj = new Obj(shapes);
			
			// Create walls:
			Obj northWallObj = squareObj.clone();
//			Obj westWallObj = squareObj.clone();
//			Obj southWall = squareObj.clone();
//			Obj eastWall = squareObj.clone();
			
			// Place walls:
			northWallObj.trans.x = width/2;
			northWallObj.trans.y = height - TILE_SIZE/2;
			northWallObj.trans.sx = width/2;
			northWallObj.trans.sy = TILE_SIZE/2;
			
			// Create Terrain Objects:
			Terrain northWall = new Terrain(northWallObj, stoneTexture, false);
			
		}
		catch (Exception e)
		{
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
		*/
	}
	
	private void updatePlayer(GL2 gl)
	{
		// Check for collisions with follower NPCs:
			for (int i = 1; i < npcs.size(); i++)
			{
				if (MathUtil.roughCollision(player.getObj(), npcs.get(i).getObj()))
					player.changeHP(-1);
			}
			Vector2D newVelocity = new Vector2D(new double[] {0, 0});
			
			// Add unit vectors according to the directions pressed:
			if (activeWASD[0])// Up pressed
				newVelocity.add(new double[] {0, 1});
			if (activeWASD[1])// Left pressed
				newVelocity.add(new double[] {-1, 0});
			if (activeWASD[2])// Down pressed
				newVelocity.add(new double[] {0, -1});
			if (activeWASD[3])// Right pressed
				newVelocity.add(new double[] {1, 0});
			
			// Cap the speed:
			newVelocity.normalize();
			newVelocity.multiply(PLAYER_MAX_SPEED);
			player.setLinVelocity(newVelocity);

			double[] deltaD = new double[] {newVelocity.getComponents()[0] * deltaT,// Player's displacement this frame
					newVelocity.getComponents()[1] * deltaT};
			
			if (player.getPosition()[0] + deltaD[0] + ACTOR_SCALE >= width)// Player at right border
				player.getObj().trans.x = width - ACTOR_SCALE;
			else if (player.getPosition()[0] + deltaD[0] - ACTOR_SCALE <= 0)// Player at left border
				player.getObj().trans.x = 0 + ACTOR_SCALE;
			else
				player.getObj().trans.x += deltaD[0];
			
			if (player.getPosition()[1] + deltaD[1] + ACTOR_SCALE >= height)// Player at top border
				player.getObj().trans.y = height - ACTOR_SCALE;
			else if (player.getPosition()[1] + deltaD[1] - ACTOR_SCALE <= 0)// Player at bottom border
				player.getObj().trans.y = 0 + ACTOR_SCALE;
			else
				player.getObj().trans.y += deltaD[1];
			
			// Change player orientation:
			Vector2D playerVector = new Vector2D(new double[] {0, 1} );// Default facing direction of player is forwards
			double angle = playerVector.angleBetween(mouseVector);// How much should the player be rotated?
			angle = MathUtil.radianToDegree(angle);
			player.getObj().trans.theta = (float)angle;
	}
	
	private void updateNPCs()
	{
		// Delete dead npcs:
		ArrayList<NPC> aliveActors = new ArrayList<NPC>();
		for (NPC currNPC : npcs)
		{
			if (currNPC.getCurrHP() > 0)
				aliveActors.add(currNPC);
		}
		npcs = aliveActors;
		
		// Update NPCs:
		for (NPC currNPC : npcs)
				currNPC.update(deltaT);
		
	}
	
	private void drawActor(GL2 gl, Actor actor)
	{
		gl.glPushMatrix();
		gl.glColor3f(CLEAR_COLOUR.r, CLEAR_COLOUR.g, CLEAR_COLOUR.b);
		Trans trans = actor.getObj().trans;
			
		// Create gl transfomation matrix:
		gl.glLoadIdentity();
		gl.glTranslatef(trans.x, trans.y, 0);
		gl.glRotatef(trans.theta, 0, 0, 1);
		gl.glScalef(trans.sx, trans.sy, 1);
		
		// Draw the actor:
		for (Shape shape : actor.getObj().shapes)
		{
			Colour currColour = shape.colour;
			
			gl.glColor3f(currColour.r, currColour.g, currColour.b);// Set colour
			gl.glBegin(GL2.GL_POLYGON);
			for (Point vert : shape.verts)
				gl.glVertex2f(vert.x, vert.y);
			gl.glEnd();
		}
			
		gl.glColor3f(CLEAR_COLOUR.r, CLEAR_COLOUR.g, CLEAR_COLOUR.b);
		gl.glPopMatrix();
	}
	
	/*
	private void drawTerrain(GL2 gl, Terrain terrain)
	{
		gl.glPushMatrix();
		gl.glColor3f(CLEAR_COLOUR.r, CLEAR_COLOUR.g, CLEAR_COLOUR.b);
		Trans trans = terrain.getObj().trans;
		Shape shape = terrain.getObj().shapes.get(0);// The first shape 	
		
		// Create gl transfomation matrix:
		gl.glLoadIdentity();
		gl.glTranslatef(trans.x, trans.y, 0);
		gl.glRotatef(trans.theta, 0, 0, 1);
		gl.glScalef(trans.sx, trans.sy, 1);
		
		terrain.getTexture().enable(gl);
		terrain.getTexture().bind(gl);
		
			gl.glBegin(GL2.GL_QUADS);
			gl.glTexCoord2f(0, 1);// Top left
			gl.glVertex2f(shape.verts.get(0).x, shape.verts.get(0).y);
			gl.glTexCoord2f(0, 0);// Bottom left
			gl.glVertex2f(shape.verts.get(1).x, shape.verts.get(1).y);
			gl.glTexCoord2f(1, 0);// Bottom right
			gl.glVertex2f(shape.verts.get(2).x, shape.verts.get(2).y);
			gl.glTexCoord2f(1, 1);// Top right
			gl.glVertex2f(shape.verts.get(3).x, shape.verts.get(3).y);
			terrain.getTexture().disable(gl);
			gl.glFlush();
			gl.glEnd();

		gl.glColor3f(CLEAR_COLOUR.r, CLEAR_COLOUR.g, CLEAR_COLOUR.b);
		gl.glPopMatrix();
	}*/
	
	private void gameOver(GL2 gl)
	{
		
		textRenderer.beginRendering(width, height);
		textRenderer.draw("GAME OVER! You died", width/2-width/5, height/2);
		textRenderer.endRendering();
	}
	
	// ------------------------- INPUT LISTENERS BEGIN -------------------------- 
	@Override
	public void keyTyped(KeyEvent e) {
	}

	@Override
	public void keyPressed(KeyEvent e) {
		if (e.getKeyChar() == 'w')// up
			activeWASD[0] = true;
		if (e.getKeyChar() == 'a')// left
			activeWASD[1] = true;
		if (e.getKeyChar() == 's')// down
			activeWASD[2] = true;
		if (e.getKeyChar() == 'd')// right
			activeWASD[3] = true;
	}
	
	@Override
	public void keyReleased(KeyEvent e) {
		if (e.getKeyChar() == 'w')// up
			activeWASD[0] = false;
		if (e.getKeyChar() == 'a')// left
			activeWASD[1] = false;
		if (e.getKeyChar() == 's')// down
			activeWASD[2] = false;
		if (e.getKeyChar() == 'd')// right
			activeWASD[3] = false;
	}
	
	@Override
	public void mouseDragged(MouseEvent e) {

	}

	@Override
	public void mouseMoved(MouseEvent e) {
		if (player != null)
		{
			mouseVector.setComponents(new double[] {e.getX() - player.getPosition()[0], 
					(height - e.getY()) - player.getPosition()[1]} );
		}
	}

	@Override
	public void mouseClicked(MouseEvent arg0) {
	}

	@Override
	public void mouseEntered(MouseEvent arg0) {
	}

	@Override
	public void mouseExited(MouseEvent arg0) {
	}

	@Override
	public void mousePressed(MouseEvent e) {

	}

	@Override
	public void mouseReleased(MouseEvent arg0) {

	}
	// --------------------------- INPUT LISTENERS END --------------------------- 
}
