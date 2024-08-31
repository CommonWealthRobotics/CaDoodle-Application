package com.commonwealthrobotics.controls;

public enum Quadrent {
	first, second, third, fourth;
	
	public static Quadrent getQuad(double angle) {
		if (angle > 45 && angle <= 135)
			return Quadrent.first;
		if (angle > 135 || angle <= (-135))
			return Quadrent.second;
		if (angle > -135 && angle <= -45)
			return Quadrent.third;
		if (angle > -45 && angle <= 45)
			return Quadrent.fourth;
		throw new RuntimeException("Impossible nummber! " + angle);
	}

	public static double QuadrentToAngle(Quadrent q) {
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
