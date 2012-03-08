package com.rogicrew.imagezoom.ontouch;

import java.util.ArrayList;
import java.util.List;

import android.view.MotionEvent;

public class OnTouchMulti implements OnTouchInterface{
//	private static final int ACTION_MASK = 0x000000ff;
//	private static final int ACTION_POINTER_DOWN = 0x00000005;
//	private static final int ACTION_POINTER_UP = 0x00000006;
	private long mTimeForClick;
	private long mTimeForDoubleClick;
	
	private boolean mIsClick;
	private boolean mIsDoubleClick;
	private boolean mIsScroll;
	private boolean mIsMove;
	
	
	private List<Pointer> mPoints;
	private long mTimeOfFirstPointerDown;
	private long mTimeOfLastClick;
	private boolean isOneFingerOnly;
	//holds pointers positions for last two clicks
	private Pointer[] mLastTwoClickPointers;
	private int mLastTwoClickPointersOffset; //0 or 1
	
	public OnTouchMulti()
	{
		mPoints = new ArrayList<Pointer>();
		mLastTwoClickPointers = new Pointer[] { new Pointer(0.0f, 0.0f, 0), new Pointer(0.0f, 0.0f, 0) };
		mLastTwoClickPointersOffset = 1;
	}
	
	/**
	 * Remove all pointers from points which ids are in event 
	 * @param event MotionEvent
	 */
	private void remove(MotionEvent event){
		for (int i = mPoints.size() - 1; i >= 0; --i){
			Pointer currPointer = mPoints.get(i);
			for (int pointerIndex = 0; pointerIndex < event.getPointerCount(); ++pointerIndex){
				int id = event.getPointerId(pointerIndex);
				if (currPointer.id == id){
					mPoints.remove(i);
					break;
				}
			}
		}			
	}
	
	
	/** Updates points list with current moved(or pressed down) pointers(including first finger)
	 * @param event MotionEvent
	 */
	private void update(MotionEvent event){
		for (int pointerIndex = 0; pointerIndex < event.getPointerCount(); ++pointerIndex){
			int id = event.getPointerId(pointerIndex);
			float x = event.getX(pointerIndex);
			float y = event.getY(pointerIndex);

			boolean isNewOne = true;
			for (Pointer currPointer: mPoints){
				if (currPointer.id == id){
					currPointer.update(x, y);
					isNewOne = false;
					break;
				}
			}
			
			if (isNewOne){
				mPoints.add(new Pointer(x, y, id));
			}
		}
	}
	
	@Override
	public void create(long timeForClick, long timeForDoubleClick){
		mTimeForClick = timeForClick;
		mTimeForDoubleClick = timeForDoubleClick;
		init();
	}
	
	@Override
	public void init(){
		mTimeOfFirstPointerDown = mTimeOfLastClick = 0;
		isOneFingerOnly = mIsMove = mIsClick = mIsDoubleClick = mIsScroll = false;
	}
	
	@Override
	public void processEvent(MotionEvent event) {
		mIsMove = mIsClick = mIsDoubleClick = mIsScroll = false;
		
		final int action = event.getAction();		
		switch (action & MotionEvent.ACTION_MASK) {
		    case MotionEvent.ACTION_DOWN:
		        isOneFingerOnly = true;
		        mPoints.clear();
		        update(event);
		        //remember when action occurred
		        mTimeOfFirstPointerDown = System.currentTimeMillis();
		        mLastTwoClickPointersOffset = 1 - mLastTwoClickPointersOffset;
		        mLastTwoClickPointers[mLastTwoClickPointersOffset].set(mPoints.get(0));
		        break;
		    
		    case MotionEvent.ACTION_POINTER_DOWN:
		    	update(event);
		    	mTimeOfFirstPointerDown = mTimeOfLastClick = 0; //click and double click are impossible now
		    	isOneFingerOnly = false; //we have more than one finger
		    	break;
		        
		    case MotionEvent.ACTION_MOVE:
		    	 update(event);
		    	 //if there is only one finger pressed
		    	 if (isOneFingerOnly){
		    		 mIsScroll = true;
		    	 }
		    	 //in future maybe it will be possible to move and not press :)
		    	 else if (mPoints.size() > 0){
		    		 mIsMove = true;
		    	 }
		         break;
		    
		    case MotionEvent.ACTION_UP:
		        //it was the only finger
		       	if (isOneFingerOnly  &&  
		       		System.currentTimeMillis() - mTimeOfFirstPointerDown <= mTimeForClick){		       		
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
		       	
		    	remove(event);
		       	mTimeOfFirstPointerDown = 0;
		       	isOneFingerOnly = false; //first(one) is released
		        break;
		        
		    case MotionEvent.ACTION_CANCEL:
		        mPoints.clear();
		        mTimeOfFirstPointerDown = mTimeOfLastClick = 0;
		        break;
		    
		    case MotionEvent.ACTION_POINTER_UP: 
		    	remove(event);
		    	break;
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
		return mIsMove;
	}

	@Override
	public boolean isMultitouch() {		
		return mPoints.size() > 1;
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
		return mPoints;
	}

}
