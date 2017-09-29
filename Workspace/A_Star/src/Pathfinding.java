/* Pathfinding.java
 *
 * @author			Michael McMahon
 *
 * PURPOSE: This file contains all code concerning pathfinding; although not ideal,
 * graphical code has been included in this file which allows the user to view the progress
 * of the algorithm as it works*/

import java.util.ArrayList;

public abstract class Pathfinding {
	public static ArrayList<Node> aStar(Node[][] graph, Node start, Node goal)
	{
		assert (graph != null && start != null && goal != null);// The start and goal cannot be blocked cells
		if (graph != null && start != null && goal != null)
		{
			Node currNode = start;
			ArrayList<Node> openNodes = new ArrayList<Node>();// Visited but unprocessed nodes
			ArrayList<Node> closedNodes = new ArrayList<Node>();// Processed nodes
			
			currNode.setGValue(0);
			currNode.setHValue(Heuristic.euclidean(currNode, goal));
			openNodes.add(currNode);

			while(openNodes.size() > 0)
			{
				currNode = findBestNode(openNodes, goal);// Pick the most promising node
				
				ArrayList<Obj> currScene = createScene(currNode, // Create new scene
						A_Star.squareShape, A_Star.CURR_CELL, new ArrayList<Obj>());
				A_Star.scenes.add(currScene);// Enqueue scene
		
				if (currNode == goal)// We're there
					break;
				else
				{
					ArrayList<Node> neighbors = currNode.getNeighbors(graph);
					
					for (Node neighbor : neighbors)// For each neighbor:
					{
						if (neighbor != null)// Skip blocked neighbors
						{
							ArrayList<Obj> neighborScene = createScene(neighbor,// Base new scene off of currScene
									A_Star.squareShape, A_Star.NEIGHBOR, currScene);
							A_Star.scenes.add(neighborScene);// Enqueue neighborScene
							
							double newGValue = currNode.getGValue() + neighbor.getCost();// New gValue for neighbor
							
							if (closedNodes.contains(neighbor))// Has this neighbor already been processed?
							{
								if (neighbor.getGValue() <= newGValue)// Previous path to neighbor is better: skip this neighbor
									continue;
								else// New path to neighbor is better
									closedNodes.remove(neighbor);
							}
							else if (openNodes.contains(neighbor))// Has this neighbor already been discovered?
							{
								if (neighbor.getGValue() <= newGValue)// Previous path to neighbor is better: skip this neighbor
									continue;
							}
							else// Neighbor has been discovered!
								neighbor.setHValue(Heuristic.euclidean(neighbor, goal));
							
							neighbor.setFromNode(currNode);
							neighbor.setGValue(newGValue);
							
							if (!openNodes.contains(neighbor))
								openNodes.add(neighbor);
						}
					}
					// Done processing currNode: remove it from open, and add to closed
					openNodes.remove(currNode);
					closedNodes.add(currNode);
				}//else
			}// while
			
			if (currNode != goal)// Failure: no path found!
				return null;
			else// Return the path
			{
				ArrayList<Node> path = new ArrayList<Node>();
				
				while (currNode != start)
				{
					path.add(0, currNode);
					currNode = currNode.getFromNode();
				}
				return path;
			}
		}
		else// Error
		{
			System.out.println("ERROR: graph, start, and end cannot be null/blocked cells!");
			return null;
		}
	}// aStar()

	// Linearly searches for the Node with the lowest fValue
	private static Node findBestNode(ArrayList<Node> nodeList, Node goal)
	{
		assert(nodeList != null && goal != null && nodeList.size() > 0);
		
		Node bestNode = nodeList.get(0);
		
		for (Node currNode : nodeList)
		{
			currNode.setHValue(Heuristic.euclidean(currNode, goal));
			
			if (currNode.getFValue() >= 0 && currNode.getFValue() < bestNode.getFValue())
				bestNode = currNode;
		}
		return bestNode;
	}
	
	private static ArrayList<Obj> cloneScene(ArrayList<Obj> scene)
	{
		ArrayList<Obj> newScene = new ArrayList<Obj>();
		
		for (Obj cell : scene)
			newScene.add(cell.clone());
		return newScene;
	}
	
	private static ArrayList<Obj> createScene(Node node, Shape cellShape, Colour colour, ArrayList<Obj> baseScene)
	/* Remarks: Creates a scene to be drawn 
	 * 
	 * Input: @pararm cellShape		The dimensions of a cell in world coordinates
	 * 			  @param node				The node of intrest (can be the current node, or a neighbor)
	 * 			  @pararm colour			The colour to be applied to the cell 
	 * 			  @param scene				The scene to be modified and enqueued to A_Star.scenes
	 */
	{
		float[] cellSize = A_Star.cellSize;
		ArrayList<Shape> shapes = new ArrayList<Shape>();
		shapes.add(cellShape.clone());
		Obj cell = new Obj(shapes);
		
		cell.shapes.get(0).colour = colour;// Colour cell
		
		// Transform cell:
		cell.trans.sx = cellSize[0]/2;
		cell.trans.sy = cellSize[1]/2;
		cell.trans.x = (node.getPosition()[1] * cellSize[0]) + cellSize[0]/2;
		cell.trans.y = ((A_Star.numRows - 1 - node.getPosition()[0]) * cellSize[1]) + (cellSize[1]/2);
		
		// Create and return a new scene (modified from baseScene):
		ArrayList<Obj> newScene = cloneScene(baseScene);
		newScene.add(cell);
		return newScene;
	}
}

class Node 
{
	// Instance variables:
	private Node fromNode = null;
	private int[] position = null;// Coordinate pair on grid
	private double cost;// A negative
	private 	double gValue;// Cost so far
	private double hValue;// Heuristic estimate (cost to goal)
	
	public Node(int[] position, double cost)
	{
		this.position = position;
		this.cost = cost;
		gValue = -1;
		hValue = -1;
	}
	
	// Getters:
	public Node getFromNode() { return fromNode; }
	public int[] getPosition() { return position; }
	public double getCost() { return cost; }
	public double getGValue() { return gValue; }
	public double getHValue() { return hValue; }
	public double getFValue() { return gValue + hValue; }// Total cost estimate; f(n) = g(n) + h(n)
	
	// Setters:
	public void setFromNode(Node from) { fromNode = from; }
	public void setPosition(int[] position) { this.position = position; }
	public void setCost (double cost) { this.cost = cost; }
	public void setGValue (double value) { this.gValue = value; }
	public void setHValue (double value) { this.hValue = value; }
	
	// Other Instance methods:
	public ArrayList<Node> getNeighbors(Node[][] graph) 
	{
		assert (graph != null && graph.length > 0 && graph[0].length > 0);
		
		// Greatest row and column indexes, respectively (assumes columns are equally sized)
		int maxR = graph.length - 1;
		int maxC = graph[0].length - 1;
		
		ArrayList<Node> neighbors = new ArrayList<Node>();
		
		if (position[1] - 1 >= 0)// Top neighbor?
			neighbors.add(graph[position[0]] [position[1] - 1]);
		if (position[0] - 1 >= 0)// Left neighbor?
			neighbors.add(graph[position[0] - 1] [position[1]]);
		if (position[1] + 1 <= maxR)// Bottom neighbor?
			neighbors.add(graph[position[0]] [position[1] + 1]);
		if (position[0] + 1 <= maxC)// Right neighbor?
			neighbors.add(graph[position[0] + 1] [position[1]]);

		return neighbors;
	}
	
//	public String toString() { return "NODE (position: " + position[0] + ", " + position[1] + "; cost: " + cost + ")"; }
	public String toString() { return "(" +  position[0] + ", " + position[1] + ")"; }
}

class Connection
{
	private Node fromNode = null;
	private Node toNode = null;
	
	public Connection(){}
	
	public Node getFrom() { return fromNode; }
	public Node getTo() { return toNode; }
	
	public void setFrom(Node fromNode) { this.fromNode = fromNode; }
	public void setTo (Node toNode) { this.toNode = toNode; }
}

abstract class Heuristic
{
	public static double euclidean(Node curr, Node goal)// Straight-line heuristic
	{
		double[] vectorArray = new double[] {goal.getPosition()[0] - curr.getPosition()[0], goal.getPosition()[1] - curr.getPosition()[1]};
		Vector2D vector = new Vector2D(vectorArray);
		return vector.length();
	}
}