/* GraphicsUtilities.java
 *
 * @author			Michael McMahon, 7767398
 * @version			Jul 31, 2017 	
 *
 * PURPOSE: Contains core graphics utilities and classes
 * NOTES: 
 * - Numbers are stored as floats (OpenGL only likes working with floats and ints)
 * - Coordinate groups are referred to as points: (x, y), (x, y, z)
 * - LineSeg (GraphicsUtil) and Vector2D (MathUtil) are NOT THE SAME
 * - All code is written by me (Michael McMahon), unless indicated otherwise */

import java.lang.Math;
import java.util.ArrayList;

import com.jogamp.opengl.GL2;

public abstract class GraphicsUtil {// Contains general utility methods

	public static void glTransMatrix (GL2 gl, Trans trans, Point cent)
	// Creates a GL transformation matrix (object coords --> eye coords)
	{
		gl.glTranslatef(trans.x, trans.y, 0);// 4th: move object to desired location
		gl.glRotatef(trans.theta, 0, 0, 1);// 3rd: rotate object about z-axis 
		gl.glScalef(trans.sx, trans.sy, 0);// 2nd: scale object
		gl.glTranslatef(-cent.x, -cent.y, 0);// 1st: move object to origin
	}
	
	public static void glRevTransMatrix (GL2 gl, Trans trans, Point cent)
	// Creates a GL transformation matrix (eye coords --> object coords)
	{
		gl.glTranslatef(cent.x, cent.y, 0);
		gl.glScalef(1/trans.sx, 1/trans.sy, 0);
		gl.glRotatef(-trans.theta, 0, 0, 1);
		gl.glTranslatef(-trans.x, -trans.y, 0);
	}
	
	public static Point transformPt (Point point, Trans trans, Point cent)// 2D only
	// Transforms a point (object coords --> eye coords); returns a NEW Point object
	{
		float[][] transformation;
		float angleRad = (float)(trans.theta*Math.PI)/180.0f;// Convert angle to radians
		float[] ptMatrix = {point.x, point.y, 1};
		float[] product = new float[3];
		
		// Create transformation matrix (manual):
		transformation = MathUtil.matrixMult(scaleMatrix(trans.sx, trans.sy), translateMatrix(-cent.x, -cent.y));
		transformation = MathUtil.matrixMult(rotateMatrix(angleRad), transformation);
		transformation = MathUtil.matrixMult(translateMatrix(trans.x, trans.y), transformation);
		
		// transformation * ptMatrix:
		for (int i = 0; i < product.length; i++)
		{
			product[i] = transformation[i][0]*ptMatrix[0] + transformation[i][1]*ptMatrix[1] 
					+ transformation[i][2]*ptMatrix[2];
		}
		return new Point(product[0], product[1]);
	}
	
	public static Point revTransformPt (Point point, Trans trans, Point cent)
	// Transforms a point (eye coords --> object coords)
	{
		float[][] transformation;
		float angleRad = (float)(trans.theta*Math.PI)/180.0f;// Convert angle to radians
		float[] ptMatrix = {point.x, point.y, 1};
		float[] product = new float[3];
		
		// Create transformation matrix (manual):
		transformation = MathUtil.matrixMult(rotateMatrix(-angleRad), translateMatrix(-trans.x, -trans.y));
		transformation = MathUtil.matrixMult(scaleMatrix(1/trans.sx, 1/trans.sy), transformation);
		transformation = MathUtil.matrixMult(translateMatrix(cent.x, cent.y), transformation);
		
		// transformation * ptMatrix:
		for (int i = 0; i < product.length; i++)
		{
			product[i] = transformation[i][0]*ptMatrix[0] + transformation[i][1]*ptMatrix[1] 
					+ transformation[i][2]*ptMatrix[2];
		}
		return new Point(product[0], product[1]);
	}
	
	private static float[][] translateMatrix (float tx, float ty)// Translation Matrix
	{
		float[][] result = { {1, 0, tx}, {0, 1, ty}, {0, 0, 1} };
		return result;
	}

	private static float[][] rotateMatrix (double angle)// Rotation Matrix; angle is in radians
	{
		float[][] result = { {(float)Math.cos(angle), (float)-Math.sin(angle), 0},
								   {(float)Math.sin(angle), (float)Math.cos(angle), 0}, {0, 0, 1}};
		return result;
	}
	
	private static float[][] scaleMatrix (float sx, float sy)// Scaling Matrix
	{
		float[][] result = { {sx, 0, 0}, {0, sy, 0}, {0, 0, 1} };
		return result;
	}
}

class Colour
{
	public float r, g, b;// Public for easy access
	
	public Colour (float r, float g, float b)
	{
		this.r = r;
		this.g = g;
		this.b = b;
	}
	
	public String toString() { return "R: " + r + ", G: " + g + ", B: " + b; }
	
	public Colour clone() { return new Colour(r, g, b); }
}

class Point
{
	public float x, y;// Public for easy access
	
	public Point(float x, float y)
	{
		this.x = x;
		this.y = y;
	}
	
	public Point (float[] point)
	{
		this.x = point[0];
		this.y = point[1];
	}
	
	public String toString() { return "(" + x + ", " + y + ")"; }
	
	public Point clone() { return new Point (x, y); }
	
	public boolean inShape (Shape shape)
	/* Purpose: Is this point in the given shape?
	 * 
	 * Input: polyVertices (ArrayList<Point>)
	 * Output: is this point in the given polygon? (boolean) */
	{
		// Preconditions:
		assert (shape != null);
		assert (shape.verts.size() > 0);
		
		ArrayList<Float> results = new ArrayList<Float>();
		LineSeg edgeA = null;
		LineSeg edgeB = null;
		
		for (int i = 0; i < shape.verts.size(); i++)
		{
			if (i == shape.verts.size()-1)// Last vertex in polygon
				edgeA = new LineSeg (shape.verts.get(i), shape.verts.get(0));
			else
				edgeA = new LineSeg (shape.verts.get(i), shape.verts.get(i+1));
			
			edgeB = new LineSeg (shape.verts.get(i), this);
			
			results.add(edgeA.cross(edgeB));
		}
		
		// Do all cross products have the same sign?
		float sign = results.get(0)/Math.abs(results.get(0));
		
		for (int i = 1; i < results.size(); i++)
		{
			if ((results.get(i)/Math.abs(results.get(i))) != sign)
				return false;
		}
		return true;
	}
	
	public boolean inBB (Shape BB)// Coarse hit test: is this point within the bounds of BB?
	{
		return (x > BB.verts.get(0).x && x < BB.verts.get(1).x && y > BB.verts.get(1).y && y < BB.verts.get(2).y);
	}
}

class LineSeg
{
	// Public for easy access:
	public Point p1 = null;
	public Point p2 = null;
	
	public LineSeg (Point p1, Point p2)
	{
		// Preconditions:
		assert (p1 != null && p2 != null);
		
		this.p1 = p1;
		this.p2 = p2;
	}
	
	public LineSeg (float x1, float y1, float x2, float y2)
	{
		this.p1 = new Point(x1, y1);
		this.p2 = new Point(x2, y2);
	}
	
	public String toString() 
	{ 
		// Preconditions:
		assert (p1 != null && p2 != null);
		
		return String.format("[(%f, %f), (%f, %f)]", p1.x, p1.y, p2.x, p2.y); 
	}
	
	public LineSeg clone() { return new LineSeg(p1.clone(), p2.clone()); }
	
	public float length() // Returns the length of this LineSeg
	{
		// Preconditions:
		assert (p1 != null && p2 != null);
		
		return (float)Math.sqrt(Math.pow(p2.x-p1.x, 2)+Math.pow(p2.y-p1.y, 2));
	}
	
	public boolean intersects (LineSeg lineSegB)
	/* Purpose: Does line segment A ("this") intersect line segment B (lineSegB)?
	 * 
	 * Input: lineSegB (LineSeg)
	 * Output: Does line segment A ("this") intersect line segment B (lineSegB)? (boolean) */
	{
		// Preconditions:
		assert(lineSegB != null);
		assert(p1 != null && p2 != null & lineSegB.p1 != null && lineSegB.p2 != null);
		
		float paramA = ((lineSegB.p2.x-lineSegB.p1.x)*(p1.y-lineSegB.p1.y)-(lineSegB.p2.y-lineSegB.p1.y)*
				(p1.x-lineSegB.p1.x))/((lineSegB.p2.y-lineSegB.p1.y)*(p2.x-p1.x)-(lineSegB.p2.x-lineSegB.p1.x)*(p2.y-p1.y));
		
		float paramB = ((p2.x-p1.x)*(p1.y-lineSegB.p1.y)-(p2.y-p1.y)*(p1.x-lineSegB.p1.x))/ 
				((lineSegB.p2.y-lineSegB.p1.y)*(p2.x-p1.x)-(lineSegB.p2.x-lineSegB.p1.x)*(p2.y-p1.y));
		
		return (paramA >= 0 && paramA <= 1 && paramB >=0 && paramB <=1);
	}
	
	public Point intersection (LineSeg lineSegB)
	/* Purpose: returns the intersection point of two line segments. 
	 * 
	 * Input: lineSegB (LineSeg)
	 * Output: intersection (Point) */
	{
		// Preconditions:
		assert(lineSegB != null);
		assert(p1 != null && p2 != null & lineSegB.p1 != null && lineSegB.p2 != null);
		
		Point intersection = null;
		
		float paramA = ((lineSegB.p2.x-lineSegB.p1.x)*(p1.y-lineSegB.p1.y)-(lineSegB.p2.y-lineSegB.p1.y)*
				(p1.x-lineSegB.p1.x))/((lineSegB.p2.y-lineSegB.p1.y)*(p2.x-p1.x)-(lineSegB.p2.x-lineSegB.p1.x)*(p2.y-p1.y));
		
		// Find the intersection coordinates:
		intersection = new Point((p1.x + paramA * (p2.x-p1.x)), (p1.y + paramA * (p2.y-p1.y)));
		
		// Postconditions:
		assert (intersection != null);
		
		return intersection;
	}
	
	public float cross (LineSeg vectorB)
	/* Purpose: returns vectorA X vectorB: 2D vectors that are directed line segments from p1 to p2
	 * 
	 * Input: vectorB (LineSeg)
	 * Output: crossProd (float) */
	{
		// Preconditions:
		assert(vectorB != null);
		assert(p1 != null && p2 != null & vectorB.p1 != null && vectorB.p2 != null);
		
		return ((p2.x-p1.x)*(vectorB.p2.y-vectorB.p1.y)-(p2.y-p1.y)*(vectorB.p2.x-vectorB.p1.x));
	}
	
	public boolean contains (Point p)// Does this line segment contain the point p?
	{
		assert (p != null);
		
		//Calculate t once using p.x and once using p.y; if these t values are the same, then p is on this line segment:
		return (p.x-p1.x)/(p2.x-p1.x) == (p.y-p1.y)/(p2.y-p1.y);
	}
}

class Shape// Primitive shapes (tri, quad, poly)
{
	// Public for easy access:
	public ArrayList<Point> verts = null;
	public Colour colour = null;
	
	public Shape(){ verts = new ArrayList<Point>(); }
	
	public Shape (ArrayList<Point> verts)
	{
		// Preconditions:
		assert (verts != null);
		
		this.verts = verts;
	}
	
	public Shape (ArrayList<Point> verts, Colour colour)
	{
		// Preconditions:
		assert (verts != null);
		
		this.verts = verts;
		this.colour = colour;
	}
	
	public Point center()// Returns the centroid of this shape as a Point
	{
		float sumX = 0;
		float sumY = 0;
		
		for (Point vert : verts)
		{
			sumX += vert.x;
			sumY += vert.y;
		}
		
		return new Point(sumX/verts.size(), sumY/verts.size());
	}
	
	public int winding()
	/* Purpose: returns the winding of a polygon: +1 if CCW; -1 if CW
	 * 
	 * Input: vertexList (ArrayList<Point>)
	 * Output: winding (int) */
	{
		// Preconditions:
		assert (verts != null);
		assert (verts.size() > 2);
		
		float signedArea = 0;
		
		for (int i = 0; i < verts.size()-2; i++)
		{
			LineSeg edgeA = new LineSeg(verts.get(0), verts.get(i+1));
			LineSeg edgeB = new LineSeg(verts.get(0), verts.get(i+2));
	
			signedArea += edgeA.cross(edgeB);
		}
		return (int)(signedArea/Math.abs(signedArea));
	}

	public String toString()
	{
		String result = "[";
		
		// Preconditions:
		assert (verts != null);
		assert (verts.size() > 0);
		
		for (int i = 0; i < verts.size(); i++)
		{
			if (i == verts.size()-1)// Last vertex
				result += verts.get(0) + "]";
			else
				result += verts.get(i) + ", ";
		}
		return result;
	}
	
	public Shape clone() 
	{ 
		ArrayList<Point> clonedVerts = new ArrayList<Point>();
		
		for (Point vert : verts)
		{
			clonedVerts.add(vert.clone());
		}
		if (colour == null)
			return new Shape(clonedVerts);	
		else
			return new Shape(clonedVerts, colour.clone());
	}
}

class Obj// i.e. geometrical object: a collection of Shape Objects
{
	// Public for easy access:
	public ArrayList<Shape> shapes = null;// This object's geometry in object coordinates 
	public Trans trans = null;
	
	public Obj() 
	{ 
		shapes = new ArrayList<Shape>();
		trans = new Trans();
	}
	
	public Obj (ArrayList<Shape> shapes)
	{
		assert(shapes != null);
		
		this.shapes = shapes;
		trans = new Trans();
	}
	
	public Point center()// Returns the centroid of this Obj as a Point
	{
		float sumX = 0;
		float sumY = 0;
		
		for (Shape shape : shapes)
		{
			sumX += shape.center().x;
			sumY += shape.center().y;
		}
		
		return new Point(sumX/shapes.size(), sumY/shapes.size());
	}
	
	public Shape BB()// Returns the bounding box (CCW) in object coordinates
	{
		float minX, maxX, minY, maxY;
		ArrayList<Point> corners = new ArrayList<Point>();
		
		assert (shapes != null && shapes.get(0).verts != null);

		// Initial values:
		minX = shapes.get(0).verts.get(0).x;
		maxX = shapes.get(0).verts.get(0).x;
		minY = shapes.get(0).verts.get(0).y;
		maxY = shapes.get(0).verts.get(0).y;
		
		for (Shape objPrim : shapes)
		{
			for (Point vert : objPrim.verts)
			{
				if (vert.x < minX)
					minX = vert.x;
				if (vert.x > maxX)
					maxX = vert.x;
				if (vert.y < minY)
					minY = vert.y;
				if (vert.y > maxY)
					maxY = vert.y;
			}
		}
		
		// Find BB corner vertices:
		corners.add(new Point(minX, minY));
		corners.add(new Point(maxX, minY));
		corners.add(new Point(maxX, maxY));
		corners.add(new Point(minX, maxY));
		
		return new Shape(corners);
	}
	
	public String toString()
	{
		assert (trans != null && shapes != null);
		if (trans != null && shapes != null)
		{
			String result = "Position: (" + trans.x + ", " + trans.y + "); Rotation: " + trans.theta + "�; Scale: [" + trans.sx + " " + trans.sy + "]\n";
			
			result += "Object Geometry (object coordinates):\n";
			for (int i = 0; i < shapes.size(); i++)
				result += "Shape " + (i + 1) + ": " + shapes.get(i) + "\n";
			
			
			return result;
		}
		System.out.println("ERROR: obj's trans and/or shapes = null");
		return null;
	}
	
	public Obj clone() 
	{
		ArrayList<Shape> clonedShapes = new ArrayList<Shape>();
		
		for (Shape shape : shapes)
		{
			clonedShapes.add(shape.clone());
		}
		
		Obj clone = new Obj(clonedShapes);
		clone.trans = trans.clone();
		return clone;
	}
}

class Trans
// Stores the transformation data for an Obj object
{
	public float x, y, theta, sx, sy;// Public for easy access; theta is in degrees 
	
	public Trans(){}
	
	public Trans (float[] transArray)
	{
		this.x = (float)transArray[0];
		this.y = (float)transArray[1];
		this.theta = (float)transArray[2];
		this.sx = (float)transArray[3];
		this.sy = (float)transArray[4];
	}
	
	public Trans clone() { return new Trans(new float[] {x, y, theta, sx, sy}); }
}