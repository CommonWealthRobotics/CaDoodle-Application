package com.commonwealthrobotics.controls;

public enum Quadrant {
	first, second, third, fourth;
	
	public static Quadrant getQuad(double angle) {
		if (angle > 45 && angle <= 135)
			return Quadrant.first;
		if (angle > 135 || angle <= (-135))
			return Quadrant.second;
		if (angle > -135 && angle <= -45)
			return Quadrant.third;
		if (angle > -45 && angle <= 45)
			return Quadrant.fourth;
		throw new RuntimeException("Impossible nummber! " + angle);
	}

	public static double QuadrantToAngle(Quadrant q) {
		switch (q) {
		case first:
			return 90;
		case second:
			return 180;
		case third:
			return -90;
		case fourth:
		default:
			return 0;
		}
	}
}
