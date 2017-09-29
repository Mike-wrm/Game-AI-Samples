/* MathUtil.java
 *
 * @author			Michael McMahon, 7767398
 * @version			Jul 31, 2017 			
 *
 * PURPOSE: Contains various physics and math utilities
 * NOTES:
 * - Numbers are stored as doubles (makes more sense from a physics standpoint)
 * - Coordinate groups are referred to as locations: (x, y), (x, y, z)
 * - LineSeg (GraphicsUtil) and Vector2D (MathUtil) are NOT THE SAME */

import java.lang.Math;

public abstract class MathUtil 
{
	public static boolean roughCollision (Obj obj1, Obj obj2)// Rough collision test using bounding boxes
	{
		assert (obj1 != null && obj2 != null);
		if (obj1 != null && obj2 != null)
		{
			// Centers of the two objects:
			Point center1 = obj1.center();
			Point center2 = obj2.center();
			
			// Create bounding boxes:
			Shape BB1 = obj1.BB();
			Shape BB2 = obj2.BB();
			Shape transformedBB1 = new Shape();
			Shape transformedBB2 = new Shape();
			
			// Transform bounding boxes to eye coords:
			for (Point currPt : BB1.verts)
				transformedBB1.verts.add(GraphicsUtil.transformPt(currPt, obj1.trans, center1));
			
			for (Point currPt : BB2.verts)
				transformedBB2.verts.add(GraphicsUtil.transformPt(currPt, obj2.trans, center2));
				
			for (Point currPt1 : transformedBB1.verts)
				if (currPt1.inShape(transformedBB2))
					return true;// Collision detected!
			return false;// No collision 
		}
			return false;// Error
	}
	
	// TODO: place into Vector3D when it is eventually created
	public static float[][] matrixMult (float[][] a, float[][] b) {// For 3x3 matrices only
		float[][] result = new float[3][3];

		for (int i = 0; i < 3; i++)
			for (int j = 0; j < 3; j++)
				for (int k = 0; k < 3; k++)
					result[i][j] += a[i][k] * b[k][j];

		return result;
	}
	
	public static double mapAngle (double angle)
	// Maps the given angle (in radians) to the range [-pi, +pi]
	{
		while (angle > 2 * Math.PI)
			angle %= (2 * Math.PI);
		
		if (angle <= Math.PI)
			return angle;
		else
			return (2 * Math.PI) - angle;
	}
	public static double radianToDegree(double angle)
	{
		return (angle * 180) / Math.PI;
	}
	
	public static double degreeToRadian (double angle)
	{
		return (angle * Math.PI) / 180;
	}
}

class Vector2D// Similar to LineSeg, but used for physics
{
	// Instance variables:
	private double[] components = null;
	
	// Constructors:
	public Vector2D()
	{
		components = new double[2];
		components[0] = 0;
		components[1] = 0;
	}
	
	public Vector2D(double[] components)
	{
		this.components = components;
	}
	
	public Vector2D(double[] location1, double[] location2)
	{
		assert (location1 != null && location2 != null);
		if (location1 != null && location2 != null)
		{
			components[0] = location2[0] - location1[0];
			components[1] = location2[1] - location1[1];
		}
		else
			System.out.println("ERROR: Constructor cannot accept null pointers");
	}
	
	//--------------------------------- INSTANCE METHODS BEGIN --------------------------------
	public double[] getComponents() { return components; }
	
	public void setComponents (double[] components) { this.components = components; }
	
	public double length()
	{
		assert (components != null);
		if (components != null)
		{
			return Math.sqrt(Math.pow(components[0], 2) + Math.pow(components[1], 2));
		}
		else
		{
			System.out.println("ERROR: components is null");
			return -1;// ERROR
		}
	}
	
	public void normalize()
	{
		assert (components != null);
		if (components != null && this.length() != 0)// Cannot divide by 0
		{
			double length = this.length();
			components[0] /= length;
			components[1] /= length;
		}
		else if (components == null)
			System.out.println("ERROR: components is null");
	}
	
	public void multiply(double scalar)// Multiplies the components by a scalar
	{
		components[0] *= scalar;
		components[1] *= scalar;
	}
	
	public void add (double[] components)// Add another vector as components
	{
		this.components[0] += components[0];
		this.components[1] += components[1];
	}
	
	public void add (Vector2D other)// Add another vector as a Vector2D
	{
		this.components[0] += other.components[0];
		this.components[1] += other.components[1];
	}
	
	public void subtract(double[] components)// Subtract another vector as components
	{
		this.components[0] -= components[0];
		this.components[1] -= components[1];
	}
	
	public void subtract (Vector2D other)// Subtract another vector as a Vector2D
	{
		this.components[0] -= other.components[0];
		this.components[1] -= other.components[1];
	}
	
	public double dot (Vector2D other)
	{
		return components[0] * other.components[0] + components[1] * other.components[1];
	}
	
	public double cross (Vector2D other)
	{
		// Preconditions:
		assert(other != null);
		
		return (components[0] * other.components[1] - components[1] * other.components[0]);
	}
	
	public double angleBetween (Vector2D other)// Returned angle is in radians
	{
		double sign = cross(other)/Math.abs(cross(other));

		if (this.length() != 0 && other.length() != 0)
//			return sign * Math.acos(this.dot(other)/(this.length() * other.length()));
		{
			double dotProd = dot(other);
			double myLen = length();
			double otherLen = other.length();
			double result = Math.acos(dotProd/(myLen * otherLen));
			result *= sign;
			return result;
		}
		else 
			return 0;
	}
	
	public String toString() { return "<" + components[0] + ", " + components[1] + ">"; }
	
	public Object clone() { return new Vector2D(components); }
	//---------------------------------- INSTANCE METHODS END ---------------------------------
}
