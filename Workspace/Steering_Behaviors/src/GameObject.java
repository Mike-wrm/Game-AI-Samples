/* GameObject.java
 *
 * @author			Michael McMahon
 *
 * PURPOSE: All graphical objects that can be placed on the game grid are GameObjects
 * (e.g. Actors, Terrain, etc.) */

import com.jogamp.opengl.util.texture.Texture;

public abstract class GameObject {
	// Instance Variables:
	protected Obj obj = null;// Stores graphical stuff (geometry, location, transformations)
	protected Texture texture = null;
	protected boolean collidable;

	// ------------------------- CONSTRUCTORS BEGIN --------------------------
	GameObject (Obj obj, Texture texture, boolean collidable)
	{
		this.obj = obj;
		this.collidable = collidable;
	}
	// -------------------------- CONSTRUCTORS END ----------------------------
	// ----------------------- INSTANCE METHODS BEGIN -----------------------
	public Obj getObj() { return obj; }
	public double getOrientation() { return (double)obj.trans.theta; }
	public Texture getTexture() { return texture; }
	public double[] getPosition() { return new double[] { (double)obj.trans.x, (double)obj.trans.y}; }

	// ------------------------ INSTANCE METHODS END -------------------------
}

class Terrain extends GameObject
{
	public Terrain(Obj obj, Texture texture, boolean collidable)
	{
		super(obj, texture, collidable);
	}
	
	public Object clone()
	{
		return new Terrain(obj.clone(), texture, collidable);
	}
	public String toString() { return "Terrain"; }
}
