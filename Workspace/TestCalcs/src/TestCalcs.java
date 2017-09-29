import java.lang.Math;
import java.util.Random;
import java.io.*;
import java.util.Scanner;

public class TestCalcs {
	
	public static void main(String[] args) 
	{
		/*
		// Circle:
		double angle = 0;
		int precision = 49;
		double radius = 1;
		String result = "{ "; 
		
		for (int i = 0; i <= precision; i++)
		{
			angle = i * (2 * Math.PI/precision);
			result += String.format("{%.2ff, %.2ff}", radius * Math.cos(angle),  radius * Math.sin(angle));
			
			if (i < precision)
			{
				result += ", ";
				
				if ((i+1) % 5 == 0)
					result += "\n";
			}
			else
				result += " };";
		}
	
		System.out.print(result);
		*/
		/*
		Random rand = new Random();
		int NUM_ROWS = 16;
		int NUM_COLS = 16;
		int[] start = {0, 0};
		int[] end = {NUM_ROWS -1, NUM_COLS -1};
		final String[] TYPES = {"o", "g", "s", "b"};
		
		System.out.println(NUM_ROWS + " " + NUM_COLS + " " + start[0] + " " + start[1] + " " + end[0] + " " + end[1]);
		for (int row = 0; row < NUM_ROWS; row++)
		{
			for (int col = 0; col < NUM_COLS; col++)
			{
				if (row == start[0] && col == start[1] || row == end[0] && col == end[1])
					System.out.print(TYPES[rand.nextInt(TYPES.length-1)] + " ");
				else
					System.out.print(TYPES[rand.nextInt(TYPES.length)] + " ");
			}
			System.out.println();
		}
		*/
		
		
		// Connect 4
		// Variables:
		final int NUM_ROWS = 6;
		final int NUM_COLS = 7;
		
		assert(NUM_ROWS >= 4 && NUM_COLS >=4);
		int[][] boardScores = new int[NUM_ROWS][NUM_COLS];
		
		// Initialize boardScores:
		for (int r = 0; r < NUM_ROWS; r++)
			for (int c = 0; c < NUM_COLS; c++)
				boardScores[r][c] = 0;
		
		
		// Find boardScores:
		for (int r = 0; r < NUM_ROWS; r++)
		{
			for (int c = 0; c < NUM_COLS; c++)
			{
				// -------------------------------------------------------------------------------- Horizontal match checks
				if (c - 3 >= 0)
					boardScores[r][c]++;
				if (c - 2 >= 0 && c + 1 < NUM_COLS)
					boardScores[r][c]++;
				if (c - 1 >= 0 && c + 2 < NUM_COLS)
					boardScores[r][c]++;
				if (c + 3 < NUM_COLS)
					boardScores[r][c]++;
				// -------------------------------------------------------------------------------- Vertical match checks
				if (r - 3 >= 0)
					boardScores[r][c]++;
				if (r - 2 >= 0 && r + 1 < NUM_ROWS)
					boardScores[r][c]++;
				if (r - 1 >= 0 && r + 2 < NUM_ROWS)
					boardScores[r][c]++;
				if (r + 3 < NUM_ROWS)
					boardScores[r][c]++;
				// -------------------------------------------------------------------------------- Left-Right diagonal match checks
				if (c - 3 >= 0 && r - 3 >= 0)
					boardScores[r][c]++;
				if (c - 2 >= 0 && c + 1 < NUM_COLS && r - 2 >= 0 && r + 1 < NUM_ROWS)
					boardScores[r][c]++;
				if (c - 1 >= 0 && c + 2 < NUM_COLS && r - 1 >= 0 && r + 2 < NUM_ROWS)
					boardScores[r][c]++;
				if (c + 3 < NUM_COLS && r + 3 < NUM_ROWS)
					boardScores[r][c]++;
				// -------------------------------------------------------------------------------- Right-Left diagonal match checks
				if (c + 3 < NUM_COLS && r - 3 >= 0)
					boardScores[r][c]++;
				if (c - 1 >= 0 && c + 2 < NUM_COLS && r - 2 >= 0 && r + 1 < NUM_ROWS)
					boardScores[r][c]++;
				if (c - 2 >= 0 && c + 1 < NUM_COLS && r - 1 >= 0 && r + 2 < NUM_ROWS)
					boardScores[r][c]++;
				if (c - 3 >= 0 && r + 3 < NUM_ROWS)
					boardScores[r][c]++;
			}
		}
		
		// Print boardScores:
		for (int r = 0; r < NUM_ROWS; r++)
		{
			for (int c = 0; c < NUM_COLS; c++)
			{
				System.out.print(boardScores[r][c]);
				
				if (c < NUM_COLS-1)
					System.out.print(" ");
			}
			System.out.println();
		}
	
	}

}
