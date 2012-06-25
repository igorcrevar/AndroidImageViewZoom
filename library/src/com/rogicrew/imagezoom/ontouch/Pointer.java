package com.rogicrew.imagezoom.ontouch;

public class Pointer {
	public float x;
	public float y;
	public float lastX;
	public float lastY;
	public int id; 
	
	public Pointer(float x, float y, int id){
		update(x, y, id);
	}
	
	public void update(float x, float y){
		lastX = this.x;
		lastY = this.y;
		this.x = x;
		this.y = y;
	}
	
	public void update(float x, float y, int id){
		this.id = id;
		update(x, y);
	}
	
	public void set(Pointer pointer){
		update(pointer.x, pointer.y, pointer.id);
		lastX = lastY = 0;
	}
}
