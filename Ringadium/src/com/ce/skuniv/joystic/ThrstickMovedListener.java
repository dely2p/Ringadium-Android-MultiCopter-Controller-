package com.ce.skuniv.joystic;

public interface ThrstickMovedListener {
	public void OnMoved(int pan, int tilt);
	public void OnReleased();
	public void OnReturnedToCenter();
}
