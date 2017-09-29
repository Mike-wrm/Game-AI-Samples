// Project: Steering_Behaviors
// Status: Complete (August 2017)

A simple game where zombies (grey) chase the player/user (green); move using WASD, and look using the mouse. The grey zombies
flock to the player using the following implementation:
	- The "invisible" (black) zombie is the "pack leader": it chases the player using the Seek and FaceForwards behaviors; this 
		zombie is also non-collidable 
	- grey zombies are "followers": they group around the pack leader using the Arrive and Align behaviors; they also use the 
		Separation behavior to avoid bumping into each-other. 