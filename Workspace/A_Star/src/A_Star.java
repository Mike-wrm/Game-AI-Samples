import java.awt.Frame;
import java.awt.event.*;

import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.*;
import com.jogamp.opengl.util.awt.TextRenderer;

import java.awt.Color;
import java.awt.Font;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import java.util.Scanner;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

public class A_Star implements GLEventListener {
	// Drawing constants:
	public static final String WINDOW_TITLE = "A_Star"; 
	public static final int INITIAL_WIDTH = 1000;// [px]
	public static final int INITIAL_HEIGHT = 1000;// [px]
	static final long FPS = 60;// How often should the game be re-drawn? [Hz] 
	public static final float[][] SQUARE = { {-1, 1}, {-1, -1}, {1, -1}, {1, 1} };
	static final Colour CLEAR_COLOUR = new Colour(0, 0, 0);// Colour to reset to after drawing
	static final Colour OUTLINE = new Colour(1, 1, 1);// Outline colour for cells
	static final Colour TEXT_COLOUR = new Colour(1, 1, 1);
	public static final Colour CURR_CELL = new Colour(1, 0, 1);
	public static final Colour NEIGHBOR = new Colour(0.7f, 0, 0.7f);
	static final Colour BLOCKED = 	new Colour(0, 0, 0);
	static final Colour OPEN = new Colour(0.5f, 0.5f, 0.5f);
	static final Colour GRASS = new Colour(0, 0.4f, 0);
	static final Colour SWAMP = new Colour (0, 0, 0.4f);
	static final String FONT = "SansSerif";
	
	// Map constants:
	static final int OPEN_COST = 1;// "o"
	static final int GRASS_COST = 3;// "g"
	static final int SWAMP_COST = 4;// "s"
	// Blocked cells are "b"

	// Drawing variables:
	TextRenderer textRenderer = null;// For printing messages to user
	int displayNum = 0;
	public static ArrayList<ArrayList<Obj>> scenes = null;// Each element is a "scene": an array containing all stuff to be drawn
	Scanner input = null;
	String inputStr = "";
	static int textSize;
	public static float[] cellSize;
	public static Shape squareShape = null;// Stores cell Geometry 
	
	// Map variables:
	static String graphFileName = null;
	public static int numRows, numColumns;// Dimensions of grid
	static int[] startPosition = new int[2];// Position of the starting cell
	static int[] endPosition = new int[2];// Position of the ending cell
	static Node[][] graph = null;
	static ArrayList<Node> path = null;
	static int sceneNum = 0;// Current scene to display
	
	public static void main(String[] args) {
		if (args.length > 0)
			graphFileName = args[0];
		else
			System.out.println("ERROR: No map filename was provided");
		
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
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		canvas.setSize(INITIAL_WIDTH, INITIAL_HEIGHT);
		canvas.setAutoSwapBufferMode(true);

		frame.add(canvas);
		frame.pack();
		frame.setVisible(true);
		
//		System.out.println("\nEnd of processing.");
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
		
		gl.glClearColor(CLEAR_COLOUR.r, CLEAR_COLOUR.g, CLEAR_COLOUR.b, 0.0f);// i.e. the background colour
		
		input = new Scanner(System.in);// For reading user input
		readGraph();// Read graph data from file
		
		cellSize = new float[2];
		cellSize[0] = (float)(width)/numColumns;
		cellSize[1] = (float)(height)/numRows;
		
		textSize = 16;
		
		textRenderer = new TextRenderer(new Font(FONT, Font.PLAIN, textSize));
		
		// Setup graphics stuff:
		ArrayList<Point> verts = new ArrayList<Point>();
		for (float[] vert : SQUARE)
			verts.add(new Point(vert));
		squareShape = new Shape(verts);// The geometry to use for rendering cells
		
		// Print pathfinding results:
		scenes = new ArrayList();
		path = Pathfinding.aStar(graph, graph[startPosition[0]][startPosition[1]], graph[endPosition[0]][endPosition[1]]);
		System.out.println("The path found, starting at (" + startPosition[0] + ", " + startPosition[1] +
				") and ending at (" + endPosition[0] + ", " + endPosition[1] + ") is:");
		for (int i = 0; i < path.size(); i++)
		{
			System.out.print(path.get(i));
			if (i < path.size()-1)
				System.out.print(" -> ");
			else
				System.out.println();
		}
		System.out.println();
	}

	@Override
	public void display(GLAutoDrawable drawable) {
		final GL2 gl = drawable.getGL().getGL2();
		gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
		gl.glLoadIdentity();

		if (displayNum > 2)// Wait for user input
			inputStr = input.next();
		
		if (displayNum == 2)
			System.out.println("Legend: 'A' = Advance to next step; 'F' = Draw Full Path; 'Q' = Quit\n");
			
		if (displayNum == 2 || (displayNum > 2 && inputStr.equalsIgnoreCase("A")))// Keep drawing
		{
			drawGraph(gl);// Draw base graph colours
			drawScene(gl, sceneNum);// Draw next scene
			drawGraphOutline(gl);// Draw graph base (outlines and cell types)
			
			System.out.print("Step #" + (sceneNum + 1) + "; Continue? ");
			
			if (sceneNum == scenes.size() - 1)// Wrap to first scene
				sceneNum = 0;
			else
				sceneNum++;
		}
		else if (displayNum > 2 && inputStr.equalsIgnoreCase("F"))// Draw full path
		{
			drawGraph(gl);// Draw base graph colours
			drawFullPath(gl);
			drawGraphOutline(gl);// Draw graph base (outlines and cell types)
			
			System.out.print("Full Path; Continue? ");
			
			sceneNum = 0;
		}
		else if (displayNum > 2 && inputStr.equalsIgnoreCase("Q"))// Quit
		{
			System.out.println("User terminated program");
			System.exit(0);
		}
		displayNum++;
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

	public void readGraph()
	// Remarks: reads all map data (grid dimensions, start cell, end cell, and the cost/type of each cell)
	{
		try 
		{
			Scanner graphFile = new Scanner(new File(graphFileName));
			
			// Get the dimensions of the grid
			numRows = graphFile.nextInt();
			numColumns = graphFile.nextInt();
			graph = new Node[numRows][numColumns];
			
			// Get the start and end cells:
			startPosition[0] = graphFile.nextInt();
			startPosition[1] = graphFile.nextInt();
			endPosition[0] = graphFile.nextInt();
			endPosition[1] = graphFile.nextInt();
			
			// Read and store cell data:
			for (int row = 0; row < numRows; row++)// For each row
			{
				graphFile.nextLine();// Advance to next line
				for (int col = 0; col < numColumns; col++)// For each column
				{
					String cellType = graphFile.next();
					
					if (cellType.equals("o"))// Open cell
						graph[row][col] = new Node(new int[]{row, col}, OPEN_COST);
					else if (cellType.equals("g"))// Grassland
						graph[row][col] = new Node(new int[]{row, col}, GRASS_COST);
					else if (cellType.equals("s"))// Swampland
						graph[row][col] = new Node(new int[]{row, col}, SWAMP_COST);
					else if (cellType.equals("b"))// Blocked cell
						graph[row][col] = null;
					else
						System.out.println("ERROR: \"" + cellType + "\" is not a valid cell type!");
				}
			}
			graphFile.close();
			
			// Ensure the start and end cells aren't blocked:
			if (graph[startPosition[0]][startPosition[1]] == null)
				System.out.println("ERROR: The starting cell cannot be blocked!");
			if (graph[endPosition[0]][endPosition[1]] == null)
				System.out.println("ERROR: The ending cell cannot be blocked!");
		}
		catch (Exception e)
		{
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
	}
	
	public void drawGraphOutline(GL2 gl)// Draws cell outlines and text
	{
		gl.glPushMatrix();
		gl.glLoadIdentity();
		// Draw cell outlines:
		for (int i = 1; i < numRows; i++)
		{
			gl.glColor3f(OUTLINE.r, OUTLINE.g, OUTLINE.b);
			gl.glBegin(GL2.GL_LINES);
			gl.glVertex2f(0, i * cellSize[1]);
			gl.glVertex2f(width, i * cellSize[1]);
			gl.glEnd();
		}
		for (int i = 1; i < numColumns; i++)
		{
			gl.glColor3f(OUTLINE.r, OUTLINE.g, OUTLINE.b);
			gl.glBegin(GL2.GL_LINES);
			gl.glVertex2f(i * cellSize[0], height);
			gl.glVertex2f(i * cellSize[0], 0);
			gl.glEnd();
		}
		
		for (int row = 0; row < numRows; row++)
		{
			for (int col = 0; col < numColumns; col++)
			{
				try
				{
					textRenderer.beginRendering(width, height);
					textRenderer.setColor(TEXT_COLOUR.r, TEXT_COLOUR.g, TEXT_COLOUR.b, 1);
					int x = (int)(col * cellSize[0]);
					int y = (int)((numRows - 1 - row) * cellSize[1]);
					
					if (graph[row][col] == null)// Blocked cell
						textRenderer.draw("Blocked", x + textSize * 1/9, y + textSize * 3/2);
					else if (graph[row][col].getCost() == OPEN_COST)// Open cell
						textRenderer.draw("Open", x + textSize * 2/3, y + textSize * 3/2);
					else if (graph[row][col].getCost() == GRASS_COST)// Grassland
						textRenderer.draw("Grass", x + textSize * 1/2, y + textSize * 3/2);
					else if (graph[row][col].getCost() == SWAMP_COST)// Swampland
						textRenderer.draw("Swamp", x + textSize * 1/4, y + textSize * 3/2);
					else
						textRenderer.draw("Invalid Cell Type (" + row + ", " + col + ")", x, y);
					
					textRenderer.endRendering();
				}
				catch (GLException e)
				{
					System.out.println(e.getMessage());
					e.printStackTrace();
				}
			}
		}
		gl.glPopMatrix();
	}
	
	public void drawGraph(GL2 gl)// Fills in graph cells with colours
	{
		Shape cell = squareShape;
		
		gl.glPushMatrix();
		for (int row = 0; row < numRows; row++)
		{
			for (int col = 0; col < numColumns; col++)
			{
				gl.glLoadIdentity();
				gl.glTranslatef((col * cellSize[0]) + cellSize[0]/2, 
						((numRows - 1 - row) * cellSize[1]) + (cellSize[1]/2), 0f);
				gl.glScalef(cellSize[0]/2, cellSize[1]/2, 1);
				
				Colour colour = null;
				// Pick cell colour:
				if (graph[row][col] == null)// Blocked cell
					colour = BLOCKED;
				else if (graph[row][col].getCost() == OPEN_COST)// Open cell
					colour = OPEN;
				else if (graph[row][col].getCost() == GRASS_COST)// Grassland
					colour = GRASS;
				else if (graph[row][col].getCost() == SWAMP_COST)// Swampland
					colour = SWAMP;
				gl.glColor3f(colour.r, colour.g, colour.b);
				
				gl.glBegin(GL2.GL_QUADS);
				for (Point vert : squareShape.verts)
					gl.glVertex2f(vert.x, vert.y);
				gl.glEnd();
			}
		}
		gl.glPopMatrix();
	}
	
	public void drawScene (GL2 gl, int sceneNum)// Draws the current step in the pathfinding process
	{
		ArrayList<Obj> scene = scenes.get(sceneNum);
		
		gl.glPushMatrix();
		for (Obj cell : scene)
		{
			// Create transformation matrix:
			gl.glLoadIdentity();
			gl.glTranslatef(cell.trans.x, cell.trans.y, 0);
			gl.glRotatef(cell.trans.theta, 0, 0, 1);
			gl.glScalef(cell.trans.sx, cell.trans.sy, 1);
			
			// Draw cell:
			
			for (Shape shape : cell.shapes)
			{
				gl.glColor3f(shape.colour.r, shape.colour.g, shape.colour.b);
				gl.glBegin(GL2.GL_QUADS);
				
				for (Point vert : shape.verts)
					gl.glVertex2f(vert.x, vert.y);
				
				gl.glEnd();
			}
		}
		gl.glPopMatrix();
	}
	
	public void drawFullPath(GL2 gl)
	{

		Shape cell = squareShape;
		
		for (Node node : path)
		{
			gl.glLoadIdentity();
			gl.glTranslatef((node.getPosition()[1] * cellSize[0]) + cellSize[0]/2, 
					((numRows - 1 - node.getPosition()[0]) * cellSize[1]) + (cellSize[1]/2), 0f);
			gl.glScalef(cellSize[0]/2, cellSize[1]/2, 1);
			
			gl.glColor3f(CURR_CELL.r, CURR_CELL.g, CURR_CELL.b);
			gl.glBegin(GL2.GL_QUADS);
			for (Point vert : squareShape.verts)
				gl.glVertex2f(vert.x, vert.y);
			gl.glEnd();
		}
	}
}
