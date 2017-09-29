/* Steering.java
 *
 * @author			Michael McMahon, 7767398
 *
 * PURPOSE: This file contains all code concerning steering behaviors */

import java.util.ArrayList;

public abstract class Steering {
	// Instance Variables:
	protected double[] steeringOutput;// {linAccelX, linAccelY, angAccel}
	protected GameObject target;
	protected NPC npc = null;
	public double weight = 0;// For blending; public for easy access 

	// Constructor:
	public Steering (NPC npc, GameObject target) 
	{
		assert (npc != null && target != null);
		if (npc != null && target != null)
		{
			steeringOutput = new double[3];
			steeringOutput[0] = 0;
			steeringOutput[1] = 0;
			steeringOutput[2] = 0;
			this.npc = npc;
			this.target = target;
		}
		else
			System.out.println("ERROR: npc and target cannot be null!");
	}

	// Constructor specifically for Separation:
	public Steering (NPC npc) 
	{
		assert (npc != null);
		if (npc != null)
		{
			steeringOutput = new double[3];
			steeringOutput[0] = 0;
			steeringOutput[1] = 0;
			steeringOutput[2] = 0;
			this.npc = npc;
			this.target = null;// Separation uses a target list instead of a single target
		}
		else
			System.out.println("ERROR: npc and target cannot be null!");
	}
	
	public NPC getNPC() { return npc; }
	public GameObject getTarget() { return target; }
	
	public abstract double[] getSteering();// {linAccelX, linAccelY, angAccel}
	public abstract String toString();
}

class Align extends Steering {
	// Instance Variables:
	protected double maxSpeed;// Angular [rad/s]
	protected double maxAccel;// Angular [rad/s^2]
	protected double slowRadius;// [px]
	protected double targetRadius;// [px]
	protected double timeToTarget;// [s]

	// ------------------------- CONSTRUCTORS BEGIN --------------------------
	public Align (NPC npc, Actor target, double maxSpeed, double maxAccel, 
			double slowRadius, double targetRadius, double timeToTarget)
	{
			super(npc, target);
			this.maxSpeed = maxSpeed;
			this.maxAccel = maxAccel;
			this.targetRadius = targetRadius;
			this.slowRadius = slowRadius;
			this.timeToTarget = timeToTarget;
	}
	// -------------------------- CONSTRUCTORS END ----------------------------

	// ----------------------- INSTANCE METHODS BEGIN -----------------------
	public double[] getSteering()
	{
		double rotation = target.getOrientation() - npc.getOrientation();
		rotation = MathUtil.degreeToRadian(rotation);
		rotation = MathUtil.mapAngle(rotation);// Map to [-pi, pi]
		double rotationMagnitude = Math.abs(rotation);
		
		if (rotationMagnitude < targetRadius)// We're already there
			return new double[] {0, 0, 0};

		double angVelocity;
		if (rotationMagnitude > slowRadius)// No need to slow down: rotate at max speed
			angVelocity = maxSpeed;
		else// Begin slowing down
			angVelocity = maxSpeed * rotationMagnitude/slowRadius;
		
		angVelocity *= rotation/rotationMagnitude;// Add direction
		
		// Calculate angAccel:
		steeringOutput[2] = angVelocity - npc.getAngVelocity();
		steeringOutput[2] /= timeToTarget;
		
		// Clip angAccel:
		if (Math.abs(steeringOutput[2]) > maxAccel)
		{
			steeringOutput[2] /= Math.abs(steeringOutput[2]);
			steeringOutput[2] *= maxAccel;
		}
		
		return steeringOutput;
	}
	
	public String toString() { return "Align"; }
	public Object clone() { return new Align (npc, (Actor)target, maxSpeed, maxAccel,
			targetRadius, slowRadius, timeToTarget); }
	// ------------------------ INSTANCE METHODS END -------------------------
}


class FaceForwards extends Align // i.e. look where you're going
{
	public FaceForwards(NPC npc, Actor target, double maxSpeed, double maxAccel, 
			double targetRadius, double slowRadius, double timeToTarget)
		{
			super(npc, target, maxSpeed, maxAccel, targetRadius, slowRadius, timeToTarget);
		}
	
	public double[] getSteering()
	{
		if (npc.getLinVelocity().length() == 0)
			return new double[] {0, 0, 0};
		else
		{
			Obj newObj = new Obj();
			newObj.trans.theta = (float)MathUtil.radianToDegree(
					Math.atan2(-npc.getLinVelocity().getComponents()[0], npc.getLinVelocity().getComponents()[1]));
			target = new Player(newObj, null, false, 0, 0);
			
			return super.getSteering();
		}
	}
	
	public String toString() { return "FaceForwards"; }
	public Object clone() { return null; }// TODO: fix this
}

class Seek extends Steering
{
	// Instance Variables:
	private double maxAccel = 0;
	private Vector2D linAccel = null;
	
	public Seek (NPC npc, GameObject target, double maxAccel)
	{
		super(npc, target);
		this.maxAccel = maxAccel;
		linAccel = new Vector2D();
	}
	
	public double[] getSteering()// Returns the linAccel and angAccel needed
	{
		// Get the direction actor needs to travel in:
		double[] direction = {target.getPosition()[0] - npc.getPosition()[0], target.getPosition()[1] - npc.getPosition()[1]};
		
		// Find linAccel:
		linAccel.setComponents(direction);
		linAccel.normalize();
		linAccel.multiply(maxAccel);
		
		steeringOutput[0] = linAccel.getComponents()[0];
		steeringOutput[1] = linAccel.getComponents()[1];
		return steeringOutput;
	}
	
	public Object clone() { return new Seek(npc, target, maxAccel); }
	public String toString() { return "Seek"; }
}

class Arrive extends Steering
{
	// Instance variables:
	private double maxSpeed;// Linear [px/s]
	private double maxAccel;// Linear [px/s^2]
	private double slowRadius;// Determines when actor begins slowing down when approaching target [px]
	private double targetRadius;// Margin of error [px]
	private double timeToTarget;// [s]
	
	// Constructor:
	public Arrive(NPC npc, GameObject target, double maxSpeed, double maxAccel, 
			double slowRadius, double targetRadius, double timeToTarget)
	{
		super(npc, target);
		this.maxSpeed = maxSpeed;
		this.maxAccel = maxAccel;
		this.slowRadius = slowRadius;
		this.targetRadius = targetRadius;
		this.timeToTarget = timeToTarget;
	}
	
	public double[] getSteering()
	{
		double newSpeed = 0;
		
		Vector2D direction = new Vector2D(new double[] {target.getPosition()[0] - // Get the direction actor needs to travel in
				npc.getPosition()[0], target.getPosition()[1] - npc.getPosition()[1]});
		double distance = direction.length();
		
		if (distance < targetRadius)// We're already there
		{
			return new double[] {0, 0, 0};
		}
		else// We're not there yet
		{
			if (distance > slowRadius)// We're outside target radius: full speed ahead!
				newSpeed = maxSpeed;
			else// Begin to slow down
				newSpeed = maxSpeed * distance / slowRadius;// Calculate a scaled speed
				
				// Calculate new lin velocity for actor:
				Vector2D newVelocity = direction;
				newVelocity.normalize();
				newVelocity.multiply(newSpeed);
				
				// Find acceleration:
				Vector2D newAccel = new Vector2D(new double[] {
						newVelocity.getComponents()[0] - npc.getLinVelocity().getComponents()[0],
						newVelocity.getComponents()[1] - npc.getLinVelocity().getComponents()[1]});
				newAccel.multiply(1.0 / timeToTarget);
				
				// Clip acceleration:
				if (newAccel.length() > maxAccel)
				{
					newAccel.normalize();
					newAccel.multiply(maxAccel);
				}
				
				steeringOutput[0] = newAccel.getComponents()[0];
				steeringOutput[1] = newAccel.getComponents()[1];
				return steeringOutput;
		}
	}
	
	public Object clone() { return new Arrive (npc, target, maxSpeed, maxAccel, slowRadius, targetRadius, timeToTarget); }
	public String toString() { return "Arrive"; }
}

class Separation extends Steering// Uses inverse square law
{
	private ArrayList<GameObject> targets = null;// Targets to be separated from
	private double decayCoeff;
	private double maxAccel;
	private double threshold;// [px]
	
	public Separation (NPC npc, ArrayList<GameObject> targets, double maxAccel, double threshold, double decayCoeff)
	{
		super(npc);
		
		assert(targets != null);
		assert (maxAccel >= 0 && threshold >= 0);
		if (targets != null && maxAccel >= 0 && threshold >= 0)
		{
			this.targets = targets;
			this.maxAccel = maxAccel;
			this.threshold = threshold;
			this.decayCoeff = decayCoeff;
		}
	}
	
	public double[] getSteering()
	{
		// Reset steeringOutput:
		steeringOutput[0] = 0;
		steeringOutput[1] = 0;
		
		for (GameObject currTarget : targets)
		{
			Vector2D direction = new Vector2D(new double[] {currTarget.getPosition()[0] -// Vector from npc to target
					npc.getPosition()[0], currTarget.getPosition()[1] - npc.getPosition()[1]});
			double distance = direction.length();
			
			if (distance < threshold)// We're too close to target
			{
				double strength = Math.min(decayCoeff/Math.pow(distance, 2), maxAccel);// Repulsion strength
				
				// Find linAccel:
				direction.normalize();
				direction.multiply(strength);
				
				// Move away from target:
				steeringOutput[0] -= direction.getComponents()[0];
				steeringOutput[1] -= direction.getComponents()[1];
			}
		}
		return steeringOutput;
	}
	
	public ArrayList<GameObject> getTargets() { return targets; }
	
	public Object clone() { return new Separation(npc, targets, maxAccel, threshold, decayCoeff); }
	public String toString() { return "Separation"; }
}