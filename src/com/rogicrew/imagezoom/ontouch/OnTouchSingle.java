package com.rogicrew.imagezoom.ontouch;

import java.util.ArrayList;
import java.util.List;

import android.view.MotionEvent;

public class OnTouchSingle implements OnTouchInterface{
	private long mTimeForClick;
	private long mTimeForDoubleClick;
	
	private boolean mIsClick;
	private boolean mIsDoubleClick;
	private boolean mIsScroll;
	
	private long mTimeOfFirstPointerDown;
	private long mTimeOfLastClick;
	//holds pointers positions for last two clicks
	private Pointer[] mLastTwoClickPointers;
	private int mLastTwoClickPointersOffset; //0 or 1
	
	public OnTouchSingle()
	{
		mLastTwoClickPointers = new Pointer[] { new Pointer(0.0f, 0.0f, 0), new Pointer(0.0f, 0.0f, 0) };
		mLastTwoClickPointersOffset = 1;
	}
	
	@Override
	public void create(long timeForClick, long timeForDoubleClick){
		mTimeForClick = timeForClick;
		mTimeForDoubleClick = timeForDoubleClick;
	}
	
	@Override
	public void init(){
		mTimeOfFirstPointerDown = mTimeOfLastClick = 0;
		mIsClick = mIsDoubleClick = mIsScroll = false;
	}
	
	@Override
	public void processEvent(MotionEvent event) {
		mIsClick = mIsDoubleClick = mIsScroll = false;
		
		final int action = event.getAction();		
		if ((action & MotionEvent.ACTION_DOWN) > 0){
			 mTimeOfFirstPointerDown = System.currentTimeMillis();
		     mLastTwoClickPointersOffset = 1 - mLastTwoClickPointersOffset;
		     mLastTwoClickPointers[mLastTwoClickPointersOffset].update(event.getX(), event.getY(), 0);
		}
		else if ((action & MotionEvent.ACTION_UP) > 0){
		 	if (System.currentTimeMillis() - mTimeOfFirstPointerDown <= mTimeForClick){		       		
	       		if (mTimeOfLastClick > 0  &&  
	       			System.currentTimeMillis() - mTimeOfLastClick <= mTimeForDoubleClick){
	       			mIsDoubleClick = true;
	       			mTimeOfLastClick = 0;
	       		}
	       		else{
	       			mTimeOfLastClick = System.currentTimeMillis();
	       			mIsClick = true;
	       		}
		    }
		}
		else if ((action & MotionEvent.ACTION_MOVE) > 0){
			mLastTwoClickPointers[mLastTwoClickPointersOffset].update(event.getX(), event.getY(), 0);
	    	 //if there is only one finger pressed
	    	mIsScroll = true;
		}    
	}

	@Override
	public boolean isDoubleClick() {
		return mIsDoubleClick;
	}

	@Override
	public boolean isSingleClick() {
		return mIsClick;
	}

	@Override
	public boolean isScroll() {
		return mIsScroll;
	}
	
	@Override
	public boolean isMove() {		
		return mIsScroll;
	}

	@Override
	public boolean isMultitouch() {		
		return false;
	}

	@Override
	public Pointer getClickPoint() {
		if (mIsClick || mIsScroll){
			return mLastTwoClickPointers[mLastTwoClickPointersOffset];
		}
		else if (mIsDoubleClick){
			return mLastTwoClickPointers[1 - mLastTwoClickPointersOffset];
		}
		
		return null;
	}

	@Override
	public List<Pointer> getAllPoints() {
		List<Pointer> rv = new ArrayList<Pointer>();
		rv.add(mLastTwoClickPointers[mLastTwoClickPointersOffset]);
		return rv;
	}
}
