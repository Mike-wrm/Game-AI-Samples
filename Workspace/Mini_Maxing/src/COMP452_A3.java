import java.awt.Frame;
import java.awt.event.*;

import com.jogamp.common.util.InterruptSource.Thread;
import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.*;
import com.jogamp.opengl.util.awt.TextRenderer;
import java.awt.Font;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class COMP452_A3 implements GLEventListener, MouseListener{
	// Game constants:
	public static final int NUM_COLS = 7;// 7
	public static final int NUM_ROWS = 6;// 6
	static final int MAX_DEPTH = 4;// 4
	static final char PLAYER = 'p';
	static final char COMP = 'c';
	static final char EMPTY = 'e';
	
	// Graphical constants:
	public static final String WINDOW_TITLE = "COMP452 A3"; 
	public static final int INITIAL_WIDTH = 700;// [px]
	public static final int INITIAL_HEIGHT = 600;// [px]
	public static final long FPS = 60;// How often should the game be re-drawn? [Hz] 
//	public static final String FONT = "SansSerif";
//	public static final int FONT_SIZE = 40;
	static final Colour PLAYER_COLOUR = new Colour(1, 1, 1);
	static final Colour COMP_COLOUR = new Colour(0, 0, 0);
	static final Colour BOARD_COLOUR = new Colour(1, 1, 1);
	static final Colour OUTLINE_COLOUR = new Colour(0, 0, 0);// Chip and board outline colour
	static final float CHIP_OUTLINE_WIDTH = 2.0f;
	static final float[] CELL_SIZE = {INITIAL_WIDTH/NUM_COLS, INITIAL_HEIGHT/NUM_ROWS};
	static final float[] CHIP_SCALE = {(float) ((0.7/2) * CELL_SIZE[0]), (float) ((0.7/2) * CELL_SIZE[1])};
	static final float[][] CIRCLE =
				{ {1.00f, 0.00f}, {0.99f, 0.13f}, {0.97f, 0.25f}, {0.93f, 0.38f}, {0.87f, 0.49f}, 
				{0.80f, 0.60f}, {0.72f, 0.70f}, {0.62f, 0.78f}, {0.52f, 0.86f}, {0.40f, 0.91f}, 
				{0.28f, 0.96f}, {0.16f, 0.99f}, {0.03f, 1.00f}, {-0.10f, 1.00f}, {-0.22f, 0.97f}, 
				{-0.35f, 0.94f}, {-0.46f, 0.89f}, {-0.57f, 0.82f}, {-0.67f, 0.74f}, {-0.76f, 0.65f}, 
				{-0.84f, 0.55f}, {-0.90f, 0.43f}, {-0.95f, 0.32f}, {-0.98f, 0.19f}, {-1.00f, 0.06f}, 
				{-1.00f, -0.06f}, {-0.98f, -0.19f}, {-0.95f, -0.32f}, {-0.90f, -0.43f}, {-0.84f, -0.55f}, 
				{-0.76f, -0.65f}, {-0.67f, -0.74f}, {-0.57f, -0.82f}, {-0.46f, -0.89f}, {-0.35f, -0.94f}, 
				{-0.22f, -0.97f}, {-0.10f, -1.00f}, {0.03f, -1.00f}, {0.16f, -0.99f}, {0.28f, -0.96f}, 
				{0.40f, -0.91f}, {0.52f, -0.86f}, {0.62f, -0.78f}, {0.72f, -0.70f}, {0.80f, -0.60f}, 
				{0.87f, -0.49f}, {0.93f, -0.38f}, {0.97f, -0.25f}, {0.99f, -0.13f}, {1.00f, -0.00f} };
	
	static final int INFINITY = Integer.MAX_VALUE;
	static int[] selectedCell = null;// Which board cell did the user select? {r, c}
	static char[][] board = new char[NUM_ROWS][NUM_COLS];// EMPTY = empty; PLAYER = player; COMP = computer 
	static int[][] boardHeuristic = new int[NUM_ROWS][NUM_COLS];/* Stores the "usefulness" of each cell: 
																		* i.e. the number of connect 4 matches each cell is involved in */
	
	// TODO: Put variables here:
	
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
			canvas.addMouseListener((MouseListener)self);
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
//		textRenderer = new TextRenderer(new Font(FONT, Font.BOLD, FONT_SIZE));
		gl.glClearColor(BOARD_COLOUR.r, BOARD_COLOUR.g, BOARD_COLOUR.b, 1);// i.e. the background colour
		
		// Initialize board:
		for (int r = 0; r < NUM_ROWS; r++)
			for (int c = 0; c < NUM_COLS; c++)
				board[r][c] = EMPTY;
		
		initBoardScores();
	}

	@Override
	public void display(GLAutoDrawable drawable) {
		final GL2 gl = drawable.getGL().getGL2();
		gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
		gl.glLoadIdentity();

		// TODO: Put code here
		if (selectedCell != null && board[selectedCell[0]][selectedCell[1]] == EMPTY)// The user made a move
		{
			deployPlayerChip(selectedCell);// Player's Move
			
			// Computer's Move:
			ArrayList result = abNegaMax(board, new int[2], MAX_DEPTH, 0, -INFINITY, INFINITY);
			int[] compMove = (int[])result.get(1);
			
			assert(board != null);
			assert(compMove != null && compMove[0] >= 0 && compMove[1] >= 0);
			assert(board[compMove[0]] != null);
			
			board[compMove[0]][compMove[1]] = COMP;
			
			selectedCell = null;
		}
		drawBoard(gl);
		drawChips(gl);
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
	
	// ------------------------- INPUT LISTENERS BEGIN -------------------------- 

	@Override
	public void mouseClicked(MouseEvent e) {
		selectedCell = findSelectedCell(e.getX(), height - e.getY());// Did the user select a cell?
	}

	@Override
	public void mouseEntered(MouseEvent e) {
	}

	@Override
	public void mouseExited(MouseEvent e) {
	}

	@Override
	public void mousePressed(MouseEvent e) {

	}

	@Override
	public void mouseReleased(MouseEvent e) {

	}
	// --------------------------- INPUT LISTENERS END --------------------------- 
	
	private void initBoardScores()
	{
		// Find boardHeuristic:
		for (int r = 0; r < NUM_ROWS; r++)
		{
			for (int c = 0; c < NUM_COLS; c++)
			{
				// -------------------------------------------------------------------------------- Horizontal match checks
				if (c - 3 >= 0)
					boardHeuristic[r][c]++;
				if (c - 2 >= 0 && c + 1 < NUM_COLS)
					boardHeuristic[r][c]++;
				if (c - 1 >= 0 && c + 2 < NUM_COLS)
					boardHeuristic[r][c]++;
				if (c + 3 < NUM_COLS)
					boardHeuristic[r][c]++;
				// -------------------------------------------------------------------------------- Vertical match checks
				if (r - 3 >= 0)
					boardHeuristic[r][c]++;
				if (r - 2 >= 0 && r + 1 < NUM_ROWS)
					boardHeuristic[r][c]++;
				if (r - 1 >= 0 && r + 2 < NUM_ROWS)
					boardHeuristic[r][c]++;
				if (r + 3 < NUM_ROWS)
					boardHeuristic[r][c]++;
				// -------------------------------------------------------------------------------- Left-Right diagonal match checks
				if (c - 3 >= 0 && r - 3 >= 0)
					boardHeuristic[r][c]++;
				if (c - 2 >= 0 && c + 1 < NUM_COLS && r - 2 >= 0 && r + 1 < NUM_ROWS)
					boardHeuristic[r][c]++;
				if (c - 1 >= 0 && c + 2 < NUM_COLS && r - 1 >= 0 && r + 2 < NUM_ROWS)
					boardHeuristic[r][c]++;
				if (c + 3 < NUM_COLS && r + 3 < NUM_ROWS)
					boardHeuristic[r][c]++;
				// -------------------------------------------------------------------------------- Right-Left diagonal match checks
				if (c + 3 < NUM_COLS && r - 3 >= 0)
					boardHeuristic[r][c]++;
				if (c - 1 >= 0 && c + 2 < NUM_COLS && r - 2 >= 0 && r + 1 < NUM_ROWS)
					boardHeuristic[r][c]++;
				if (c - 2 >= 0 && c + 1 < NUM_COLS && r - 1 >= 0 && r + 2 < NUM_ROWS)
					boardHeuristic[r][c]++;
				if (c - 3 >= 0 && r + 3 < NUM_ROWS)
					boardHeuristic[r][c]++;
			}
		}
	}
	
	private void drawBoard(GL2 gl)
	{
		gl.glColor3f(OUTLINE_COLOUR.r, OUTLINE_COLOUR.g, OUTLINE_COLOUR.b);

		for (int r = 0; r < NUM_ROWS; r++)
		{
			gl.glBegin(GL2.GL_LINES);
			gl.glVertex2f(0, r * CELL_SIZE[1]);
			gl.glVertex2f(INITIAL_WIDTH, r * CELL_SIZE[1]);
			gl.glEnd();
		}
		
		for (int c = 0; c < NUM_COLS; c++)
		{
			gl.glBegin(GL2.GL_LINES);
			gl.glVertex2f(c * CELL_SIZE[0], 0);
			gl.glVertex2f(c * CELL_SIZE[0], INITIAL_HEIGHT);
			gl.glEnd();
		}
	}
	
	private void drawChips(GL2 gl)
	{
		gl.glPushMatrix();
		
		for (int r = 0; r < NUM_ROWS; r++)
		{
			for (int c = 0; c < NUM_COLS; c++)
			{
				// Pick the colour of the chip to be used:
				switch (board[r][c])
				{
					case EMPTY:// Empty cell: skip
						continue;
					case PLAYER:// Player's cell
						gl.glColor3f(PLAYER_COLOUR.r, PLAYER_COLOUR.g, PLAYER_COLOUR.b); break;
					case COMP:// Computer's cell
						gl.glColor3f(COMP_COLOUR.r, COMP_COLOUR.g, COMP_COLOUR.b); break;
					default:// ERROR
						System.out.println("ERROR: Cell type '" + board[r][c] +"' @ cell [" + r + "] [" + c+ "] is invalid"); break;
				}
				
				// Transform the chip:
				gl.glLoadIdentity();
				gl.glTranslatef(c * CELL_SIZE[0] + CELL_SIZE[0]/2, (NUM_ROWS - 1 - r) * CELL_SIZE[1] + CELL_SIZE[1]/2, 0);
				gl.glScalef(CHIP_SCALE[0], CHIP_SCALE[1], 0);
				
				// Draw the chip:
				gl.glBegin(GL2.GL_POLYGON);
				for (float[] vertex : CIRCLE)
					gl.glVertex2f(vertex[0], vertex[1]);
				gl.glEnd();
				
				// Draw the chip's outline:
				gl.glLineWidth(CHIP_OUTLINE_WIDTH);
				gl.glColor3f(OUTLINE_COLOUR.r, OUTLINE_COLOUR.g, OUTLINE_COLOUR.b);
				gl.glBegin(GL2.GL_LINE_STRIP);
				for (float[] vertex : CIRCLE)
					gl.glVertex2f(vertex[0], vertex[1]);
				gl.glEnd();
			}
		}
		gl.glPopMatrix();
		gl.glLineWidth(1);// Default width
	}
	
	private int[] findSelectedCell(float mouseX, float mouseY)
	/* Purpose: Which cell, if any, did the user click/select?
	 * 
	 */
	{
		Point mouse = new Point(mouseX, mouseY);// Mouse click point
		
		// Check each cell (checks the top row FIRST)
		for (int r = 0; r < NUM_ROWS; r++)
		{
			for (int c = 0; c < NUM_COLS; c++)
			{
				// Make a Shape object to represent the current cell
				Shape cell = new Shape();
				cell.verts.add(new Point(c * CELL_SIZE[0], (NUM_ROWS-r-1) * CELL_SIZE[1] + CELL_SIZE[1]));// Top left
				cell.verts.add(new Point(c * CELL_SIZE[0], (NUM_ROWS-r-1) * CELL_SIZE[1]));// Bottom left
				cell.verts.add(new Point(c * CELL_SIZE[0] + CELL_SIZE[0], (NUM_ROWS-r-1) * CELL_SIZE[1]));// Bottom right
				cell.verts.add(new Point(c * CELL_SIZE[0] + CELL_SIZE[0], (NUM_ROWS-r-1) * CELL_SIZE[1] + CELL_SIZE[1]));// Top right
				
				if (mouse.inShape(cell))// Hit detected!
					return new int[] {r, c};
			}
		}
		return null;// No hit found :(
	}
	
	private void deployPlayerChip(int[] selectedCell)
	/* Purpose: Deploys a player chip to the bottom-most cell of the provided column (col)
	 */
	{
		if (board[selectedCell[0]][selectedCell[1]] == EMPTY)// Only empty cells can be selected
		{
			int nextRow = selectedCell[0]+1;
			
			// Find the bottom-most cell of the current column that is empty
			while (nextRow < NUM_ROWS && board[nextRow][selectedCell[1]] == EMPTY)
			{
				nextRow++;
			}
			board[nextRow-1][selectedCell[1]] = PLAYER;
		}
	}
	
	private boolean gameOver()// Checks to see if all board cells are filled
	{
		for (int r = 0; r < NUM_ROWS; r++)
			for (int c = 0; c < NUM_COLS; c++)
				if (board[r][c] == EMPTY)// Empty cell found!
					return false;
		
		return true;// All cells are filled
	}
	
	private ArrayList<int[]> getMoves(char[][] someBoard)// Returns a list of the next possible moves that can be made
	{
		ArrayList<int[]> moves = new ArrayList<int[]>();
		
		// Find the bottom-most empty cell for each column:
		for (int c = 0; c < NUM_COLS; c++)
		{
			int r = 0;// Work from the top-down
			
			while (r < NUM_ROWS && someBoard[r][c] == EMPTY)
				r++;
			
			if (r > 0)
				moves.add(new int[] {r-1, c});
		}
		
		return moves;
	}
	
	private int evalParticipant(char participant, char[][] currBoard)// Static evaluation function
	/* Purpose: evaluates the likeliness for a particular game participant to win at 
	 * the current game state using boardHeuristic 
	 * Input: @param participant 	Who should be evaluated? (player or computer)
	 * 			  @param currBoard		The  current state of the game
	 * Output: (int) How likely (or unlikely) the given participant is to win the game; 
	 * similar to a compare() function: 
	 * 		- greater than 0 = more likely to win
	 * 		- less than 0 = less likely to win
	 * 		- exactly 0 = both participants are equally likely to win
	 */
	{
		int mySum = 0;/* Sum of all heuristic values for cells which the given participant 
											has chips on */
		int theirSum = 0;// Vice-versa of the above
		
		for (int r = 0; r < NUM_ROWS; r++)
		{
			for (int c = 0; c < NUM_COLS; c++)
			{	
				if (currBoard[r][c] == participant)// We have a chip in this cell
					mySum += boardHeuristic[r][c];
				
				if (currBoard[r][c] != participant && currBoard[r][c] != EMPTY)// They have a chip in this cell
					theirSum += boardHeuristic[r][c];
			}
		}
		
		return mySum - theirSum;
	}
	
	private ArrayList abNegaMax (char[][] currBoard, int[] prevMove, int MAX_DEPTH, int currDepth, int alpha, int beta)
	/* Purpose: Recursive alpha-beta pruned nega-max algorithm
	 * 
	 * Input:	@param currBoard		Represent the state of the game at the current method iteration
	 * 														array storing the contents of each cell
	 * 				@param prevMove		The previous move 
	 * 				@param MAX_DEPTH	The depth cut-off for the recursion 
	 * 				@param currDepth 		The current iteration number of the method
	 * 				@param alpha				The alpha value (i.e. best current alternative score for min)
	 * 				@param beta					The beta value (i.e. bets current alternative score for max)
	 * 
	 * Output: (ArrayList) [bestScore, [bestMoveRow, bestMoveCol] ] A 2-element arraylist containing 
	 * the best recursive score of the current node, and the corresponding best move 
	 */
	{
		ArrayList result = new ArrayList();
		
		if (gameOver() || currDepth == MAX_DEPTH)// Board is filled or we've hit the depth cut-off
		{
			// How likely are we to win the game right now?
			if (currDepth % 2 == 0)
				result.add(evalParticipant(COMP, currBoard));
			else
				result.add(evalParticipant(PLAYER, currBoard));
				
			result.add(null);
			return result;
		}
		else// Keep recursing
		{
			int[] bestMove = null;
			int bestScore = -INFINITY;

			ArrayList stuff = getMoves(currBoard);
			for (int[] move : getMoves(currBoard))
			{
				// Copy current board into a new board:
				char[][] newBoard = new char[NUM_ROWS][NUM_COLS];
				for (int r = 0; r < NUM_ROWS; r++)
					System.arraycopy(currBoard[r], 0, newBoard[r], 0, currBoard[r].length);
				
				// Switch between comp and player moves:
				if (currDepth % 2 == 0)
					newBoard[move[0]][move[1]] = COMP;
				else
					newBoard[move[0]][move[1]] = PLAYER;
				
				// Recurse:
				ArrayList recursedResult = abNegaMax(newBoard, move, MAX_DEPTH, currDepth+1,
						-beta, -Math.max(alpha, bestScore));
				
				int currScore = -(int)recursedResult.get(0);
				
				if (currScore > bestScore)// Better alternative move found!
				{
					bestScore = currScore;
					bestMove = move;
					
					if (bestScore >= beta)// We're out-of-bounds: prune
					{
						result.add(bestScore);
						result.add(bestMove);
						return result;
					}
				}
			}
			result.add(bestScore);
			result.add(bestMove);
			return result;
		}
	}
}
