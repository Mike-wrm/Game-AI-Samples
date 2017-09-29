import java.util.Random;
/* Prints out data for a randomly generated graph: simply copy-and-paste the output
 * into a .txt file
 */
public class GraphGenerator {

	public static void Main(String[] args)
	{
		Random rand = new Random();
		final String[] TYPES = {"o", "g", "s", "b"};
		
		// TODO: Customize your graph here:
		int numRows = 16;
		int numCols = 16;
		int[] start = {0, 0};
		int[] end = {numRows -1, numCols -1};
		
		System.out.println(numRows + " " + numCols + " " + start[0] + " " + start[1] + " " + end[0] + " " + end[1]);
		for (int row = 0; row < numRows; row++)
		{
			for (int col = 0; col < numCols; col++)
			{
				if (row == start[0] && col == start[1] || row == end[0] && col == end[1])
					System.out.print(TYPES[rand.nextInt(TYPES.length-1)] + " ");
				else
					System.out.print(TYPES[rand.nextInt(TYPES.length)] + " ");
			}
			System.out.println();
		}
	}
}
