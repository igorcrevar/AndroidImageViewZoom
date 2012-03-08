package com.rogicrew.imagezoom.ontouch;

import java.util.List;

import android.view.MotionEvent;

public interface OnTouchInterface {
	/**
	 * Must call create method after creation of instance
	 * @param timeForClick
	 * @param timeForDoubleClick
	 */
	void create(long timeForClick, long timeForDoubleClick);
	void init();
	void processEvent (MotionEvent event);
	boolean isDoubleClick();
	boolean isSingleClick();
	boolean isScroll();
	boolean isMove();
	boolean isMultitouch();
	Pointer getClickPoint();
	List<Pointer> getAllPoints(); //for multitouch
}
