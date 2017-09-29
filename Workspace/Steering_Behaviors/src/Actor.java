/* Actor.java
 *
 * @author			Michael McMahon, 7767398
 *
 * PURPOSE: This file contains all code relevant to both playable and non-playable characters */

import java.util.ArrayList;

import com.jogamp.opengl.util.texture.Texture;

public abstract class Actor extends GameObject 
{
	// Instance Variables:
	protected double maxLinSpeed;// In pixels/sec
	protected double maxHP;
	protected double currHP;
	protected Vector2D linVelocity = null;
	protected double angVelocity = 0;

	// ------------------------- CONSTRUCTORS BEGIN --------------------------
	Actor (Obj obj, Texture texture, boolean collidable, double maxLinSpeed, double maxHP) 
	{ 
		super(obj, texture, true);
		linVelocity = new Vector2D(new double[] {0, 0});
		this.maxLinSpeed = maxLinSpeed; 
		this.maxHP = maxHP;
		this.currHP = maxHP;// Actors start with max health
	}
	// -------------------------- CONSTRUCTORS END ----------------------------
	
	public Vector2D getLinVelocity() { return linVelocity; }
	public double getAngVelocity() { return angVelocity; }
	public double getCurrHP() { return currHP; }
	public double getMaxLinSpeed() { return maxLinSpeed; }
	
	public void setLinVelocity(Vector2D linVelocity) { this.linVelocity = linVelocity; }
	
	public void changeHP(double value)// Adds value to HP
	{
		if (value > maxHP)
			currHP = maxHP;
		else if (currHP + value < 0)
			currHP = 0;
		else
			currHP += value;
	}
}

class Player extends Actor
{
	public Player(Obj obj, Texture texture, boolean collidable, double maxSpeed, double maxHP)
	{
		super(obj, texture, collidable, maxSpeed, maxHP);
	}
	@Override
	public Object clone() { return new Player(obj.clone(), texture, collidable, maxLinSpeed, maxHP); }

	@Override
	public String toString() { return "Player (Position: " + getPosition()[0] + ", " + getPosition()[1]; }
}

class NPC extends Actor
{
	// Instance variables:
	private double maxAngSpeed;
	private ArrayList<Steering> steeringList = new ArrayList<Steering>();
	
	// ------------------------- CONSTRUCTORS BEGIN --------------------------
	public NPC (Obj obj, Texture texture, boolean collidable, double maxLinSpeed, double maxAngSpeed, double maxHP) 
	{
		super(obj, texture, collidable, maxLinSpeed, maxHP);
		this.maxAngSpeed = maxAngSpeed;
	}
	// -------------------------- CONSTRUCTORS END ----------------------------
	
	// ----------------------- INSTANCE METHODS BEGIN -----------------------
	public ArrayList<Steering> getSteeringList() { return steeringList; }
	
	public void update(double deltaT) 
	/* Remarks: updates the actor's position and orientation based on steering data
	 * Input: @param deltaT (long)		The time between frames in nanoseconds 
	 */
	{
		assert (obj != null);
		if (obj != null)
		{	
			double[] deltaD = new double[] {linVelocity.getComponents()[0] * deltaT,// NPC's displacement this frame
					linVelocity.getComponents()[1] * deltaT};
			
			// Update horizontal position:
			if (getPosition()[0] + deltaD[0] + COMP452_A1.ACTOR_SCALE >= COMP452_A1.INITIAL_WIDTH)// NPC at right border
				getObj().trans.x = COMP452_A1.INITIAL_WIDTH - COMP452_A1.ACTOR_SCALE;
			else if (getPosition()[0] + deltaD[0] - COMP452_A1.ACTOR_SCALE <= 0)// NPC at left border
				getObj().trans.x = 0 + COMP452_A1.ACTOR_SCALE;
			else
				getObj().trans.x += deltaD[0];
			
			// Update vertical position:
			if (getPosition()[1] + deltaD[1] + COMP452_A1.ACTOR_SCALE >= COMP452_A1.INITIAL_HEIGHT)// NPC at top border
				getObj().trans.y = COMP452_A1.INITIAL_HEIGHT - COMP452_A1.ACTOR_SCALE;
			else if (getPosition()[1] + deltaD[1] - COMP452_A1.ACTOR_SCALE <= 0)// NPC at bottom border
				getObj().trans.y = 0 + COMP452_A1.ACTOR_SCALE;
			else
				getObj().trans.y += deltaD[1];
			
			obj.trans.theta += angVelocity;// Update orientation 
			
			// Update velocities:
			for (Steering steering : steeringList)
			{
				double[] steeringOutput = steering.getSteering();
				
				// Update velocities:
				linVelocity.add(new double[] {steering.weight * steeringOutput[0] * deltaT,
						steering.weight * steeringOutput[1] * deltaT});
				angVelocity += steeringOutput[2];
				
				// Clip linVelocity:
				if (linVelocity.length() > maxLinSpeed)
				{
					linVelocity.normalize();
					linVelocity.multiply(maxLinSpeed);
				}
				
				// Clip angVelocity:
				while (Math.abs(angVelocity) % 360 > 360)
					angVelocity %= 360;
			}
		}
		else if (obj == null)
			System.out.println("ERROR: Actor's Obj Object is null");
	}
	
	public double getMaxAngSpeed() { return maxAngSpeed; }
	
	@Override
	public String toString()
	{ 
		String result = "NPC:\n\tPosition: (" + getPosition()[0] + ", " + getPosition()[1] + ")\n\tSteering List: ";
		if (steeringList != null)
		{
			result += "[";
			for (int i = 0; i < steeringList.size(); i++)
			{
				result += steeringList.get(i);
				if (i < steeringList.size()-1)
					result += ", ";
			}
			result += "]";
		}
		return result;
	}
	
	@Override
	public Object clone() { return new NPC (obj.clone(), texture, collidable, maxLinSpeed, maxAngSpeed, maxHP); }
	// ------------------------ INSTANCE METHODS END -------------------------
}
