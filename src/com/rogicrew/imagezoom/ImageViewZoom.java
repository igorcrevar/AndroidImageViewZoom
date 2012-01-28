package com.rogicrew.imagezoom;

import java.lang.reflect.Method;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;

public class ImageViewZoom extends LinearLayout {

	protected class ZoomInfo {
		public int currentWidth;
		public int currentHeight;
		public float scaleWidth;
		public float scaleHeight;
		public int zoomDisplayWidth; //how much pixels of bitmap are drawn on screen
		public int zoomDisplayHeight;
		public int displayWidth; //for example if current zoom is 100px and layout width is 480px than screenWidth is 100px and not 480px
		public int displayHeight;
		public int screenStartX = 0;
		public int screenStartY = 0;
	}

	//TODO: put this in some options class
	private static int distanceZoomMultiplier = 4;
	public static float minMaxZoom = 2.0f;
	public static float pinchToZoomMinDistance = 4; //must be greaterequal to 1
	public static boolean doubleTapZooms = true;
	public static int maxZoomSteps = 3;
	public static int angleTolerant = 35;

	private Rect mSrcRect = new Rect();
	private Rect mDstRect = new Rect();
	private boolean mIsInChanging = false;
	private int mCurrentZoomStep = 1;
	private int mCurrentZoomInc = 1;

	private Paint mPaint;
	private GestureDetector mGestureDetector;
	private Bitmap mBitmap = null;

	private int mMinZoomWidth = 0;
	private int mMaxZoomWidth = 0;
	private int mScrollRectX = 0; // current left location of scroll rect
	private int mScrollRectY = 0; // current top location of scroll rect

	protected ZoomInfo mZoomInfo = new ZoomInfo();

	private PointF mFirstFingerStartPoint;
	private PointF mSecondFingerStartPoint;
	private boolean mIsTwoFinger = false;
	
	private Object[] mEmptyObjectArray = new Object[] {};
	private Object[] mInt1ObjectArray = new Object[] { 1 };
	private boolean mIsReflectionError = true;
	private Method mMethodPointerCount;
	private Method mMethodGetX;
	private Method mMethodGetY;

	public ImageViewZoom(Context context, AttributeSet attrs) {
		super(context, attrs);

		//get 2.1+ methods by reflection
		try {
			Class<?> eventClass = MotionEvent.class;
			mMethodPointerCount = eventClass.getMethod("getPointerCount");
			mMethodGetX = eventClass.getMethod("getX", new Class[] { int.class });
			mMethodGetY = eventClass.getMethod("getY", new Class[] { int.class });
			mIsReflectionError = false;
		}
		catch(Exception e){
			mIsReflectionError = true;
		}
		
		setOrientation(VERTICAL);
		//without those settings zoomed bitmap will be full of ugly squares 
		mPaint = new Paint();
		mPaint.setAntiAlias(true);
		mPaint.setFilterBitmap(true);
		mPaint.setDither(true);

		//because we are overriding onDraw method
		this.setWillNotDraw(false);
		mGestureDetector = new GestureDetector(context, new GestureListener());
	}

	public void setVisibility(boolean isVisible) {
		this.setVisibility(isVisible ? View.VISIBLE : View.GONE);
	}

	//must call after layout is loaded and measured
	public void setImage(Bitmap bitmap, int widthOfParent) {
		mIsInChanging = true;		
		mBitmap = bitmap;
		mMinZoomWidth = widthOfParent <= bitmap.getWidth() ? widthOfParent : bitmap.getWidth(); //try to fit to width if greater than width
		//if image to small make max zoom minMaxZoom(1.5) times larger than bitmap width
		mMaxZoomWidth = minMaxZoom * mMinZoomWidth > bitmap.getWidth() ? (int)(minMaxZoom * mMinZoomWidth) : bitmap.getWidth(); 
		mIsTwoFinger = false;
		mCurrentZoomStep = 1;
		mCurrentZoomInc = 1;
		mScrollRectY = mScrollRectX = 0;
		calcStepZoom(mCurrentZoomStep, mZoomInfo);
		setVisibility(true);
		ImageViewZoom.this.invalidate();
		mIsInChanging = false;
	}
	
	public void setImage(Bitmap bitmap) {
		setImage(bitmap, this.getWidth());
	}
	
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		if (mBitmap != null){
			setImage(mBitmap, w);
		}
	}

	@Override
	protected void onDraw(Canvas canvas) {
		// bitmap not exist or component didnt recieved width yet
		if (mBitmap != null && this.getWidth() > 0) {
			mDstRect.set(mZoomInfo.screenStartX, mZoomInfo.screenStartY, 
						 mZoomInfo.displayWidth + mZoomInfo.screenStartX, 
						 mZoomInfo.displayHeight + mZoomInfo.screenStartY);
			mSrcRect.set(mScrollRectX, mScrollRectY, 
						 mScrollRectX + mZoomInfo.zoomDisplayWidth, 
						 mScrollRectY + mZoomInfo.zoomDisplayHeight);
			canvas.drawBitmap(mBitmap, mSrcRect, mDstRect, mPaint);
		}
	}

	protected void updateZoomInfo(float newWidth, ZoomInfo zoomInfo) {
		this.mIsInChanging = true;
		//fix maximal width
		if (newWidth < mMinZoomWidth){
			newWidth = mMinZoomWidth;
		}
		else if (newWidth > mMaxZoomWidth){
			newWidth = mMaxZoomWidth;
		}
		
		//calculate new height
		float newHeight = (float)mBitmap.getHeight() / mBitmap.getWidth() * newWidth;
		
		//width/height of current zoomed image
		zoomInfo.currentWidth = (int) newWidth;
		zoomInfo.currentHeight = (int) newHeight;
		//width/height of current "display"
		zoomInfo.displayWidth = this.getWidth() <= zoomInfo.currentWidth ? this.getWidth() : zoomInfo.currentWidth;
		zoomInfo.displayHeight = this.getHeight() <= zoomInfo.currentHeight ? this.getHeight() : zoomInfo.currentHeight;
		//ration of real image size and current image size
		zoomInfo.scaleWidth = (float) mBitmap.getWidth() / zoomInfo.currentWidth;
		zoomInfo.scaleHeight = (float) mBitmap.getHeight() / zoomInfo.currentHeight; 
		//how much virtual bitmap pixels are shown on display
		zoomInfo.zoomDisplayWidth = (int) (zoomInfo.scaleWidth * zoomInfo.displayWidth);
		zoomInfo.zoomDisplayHeight = (int) (zoomInfo.scaleHeight * zoomInfo.displayHeight);
		
		//calculate start positions of drawing - if zoom to small for current 
		zoomInfo.screenStartX = this.getWidth() <= zoomInfo.displayWidth ? 0 : (int)(0.5f * (this.getWidth() - zoomInfo.displayWidth));
		zoomInfo.screenStartY = this.getHeight() <= zoomInfo.displayHeight ? 0 : (int)(0.5f * (this.getHeight() - zoomInfo.displayHeight));
		this.mIsInChanging = false;
	}

	protected void calcStepZoom(int step, ZoomInfo zoomInfo) {
		if (step == 1){
			updateZoomInfo(mMinZoomWidth, zoomInfo);
			return;
		}
		else if (step == maxZoomSteps){
			updateZoomInfo(mMaxZoomWidth, zoomInfo);
			return;
		}
		float newWidth = (float)(mMaxZoomWidth - mMinZoomWidth) * step / maxZoomSteps + mMinZoomWidth;
		updateZoomInfo(newWidth, zoomInfo);		
	}

	protected void calcSmoothZoom(int offset, ZoomInfo zoomInfo) {
		int newWidth = zoomInfo.currentWidth + offset;
		updateZoomInfo(newWidth, zoomInfo);
		
		//for now - reset double tap zoom variables to middle zoom step
		mCurrentZoomStep = (int)Math.ceil((double)maxZoomSteps / 2);
		mCurrentZoomInc = offset > 0 ? 1 : -1;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (mIsReflectionError){			
			return mGestureDetector.onTouchEvent(event);
		}
		
		boolean rv = true;		
		//if (event.getPointerCount() == 2) // two fingers are active
		try {
			if ((Integer)mMethodPointerCount.invoke(event, mEmptyObjectArray) == 2)
			{
				PointF newFirstFingerPosition = new PointF(event.getX(), event.getY());
				//PointF newSecondFingerPosition = new PointF(event.getX(1), event.getY(1));
				PointF newSecondFingerPosition = new PointF((Float)mMethodGetX.invoke(event, mInt1ObjectArray), 
															(Float)mMethodGetY.invoke(event, mInt1ObjectArray));
				
				if (mIsTwoFinger) {
					zoomIfPinch(newFirstFingerPosition, newSecondFingerPosition);
				} else {
					mFirstFingerStartPoint = newFirstFingerPosition;
					mSecondFingerStartPoint = newSecondFingerPosition;
					mIsTwoFinger = true;
				}
			} else {
				mIsTwoFinger = false;
				rv = mGestureDetector.onTouchEvent(event);
			}
		} 
		catch(Exception e){
			rv = mGestureDetector.onTouchEvent(event);
		}

		return rv;
	}

	protected boolean zoomIfPinch(PointF newFirst, PointF newSecond) {

		PointF firstFingerDiff = getDifVector(mFirstFingerStartPoint, newFirst);
		PointF secondFingerDiff = getDifVector(mSecondFingerStartPoint, newSecond);
		float firstFingerDistance = getVectorNorm(firstFingerDiff);
		float secondFingerDistance = getVectorNorm(secondFingerDiff);
		int distance = (int)(firstFingerDistance + secondFingerDistance);
		
		if (distance < pinchToZoomMinDistance){
			return false;
		}
		
		float angleDiff = Math.abs(getVectorAngle(firstFingerDiff) - getVectorAngle(secondFingerDiff));

		//if one finger didnt move at all we have pinch zoom also
		if ( (angleDiff < 180 - angleTolerant || angleDiff > 180 + angleTolerant) && firstFingerDistance > 0.0f && secondFingerDistance > 0.0f){
			return false;
		}

		// point beetween two fingers at start - it will be center of new scroll position
		PointF center = getCenterVector(mFirstFingerStartPoint, mSecondFingerStartPoint);
		// difference from start and end points
		float startDiff = getDistance(mFirstFingerStartPoint, mSecondFingerStartPoint);
		float endDiff = getDistance(newFirst, newSecond);
		
		if (startDiff < endDiff){
			//zooom in
			zoomIt(true, distance * distanceZoomMultiplier, center.x, center.y);
		}
		else{
			//zoom out
			zoomIt(true, -distance * distanceZoomMultiplier, center.x, center.y);
		}
		
		mFirstFingerStartPoint = newFirst;
		mSecondFingerStartPoint = newSecond;
		return true;
	}

	protected float getDistance(PointF a, PointF b) {
		double difx = a.x - b.x;
		double dify = a.y - b.y;
		double value = Math.sqrt(difx * difx + dify * dify);
		return (float) value;
	}

	protected float getVectorAngle(PointF v) {
		float norm = getVectorNorm(v);
		if (v.y == 0) {
			return v.x >= 0 ? 0 : -180;
		}

		double angle = Math.asin(Math.abs(v.y) / norm) * 180 / Math.PI;

		if (v.y < 0) {
			if (v.x > 0) {
				angle = 270 + angle;
			} else {
				angle = 180 + angle;
			}
		} else if (v.x < 0) {
			angle = 90 + angle;
		}

		return (float) angle;
	}

	protected PointF getDifVector(PointF v1, PointF v2) {
		return new PointF(v2.x - v1.x, v2.y - v1.y);
	}

	protected float getVectorNorm(PointF v) {
		double value = Math.sqrt(v.x * v.x + v.y * v.y);
		return (float) value;
	}

	protected PointF getCenterVector(PointF v1, PointF v2) {
		PointF v = new PointF(v1.x + (v2.x - v1.x) / 2, v1.y + (v2.y - v1.y) / 2);
		return v;
	}
	
	//how i fish java has delegates... i dont want to make class of everything
	//offsetOrZoomStep is offset if smooth or currentZoomStep otherwise
	protected void zoomIt(boolean isSmooth, int offsetOrZoomStep, float x, float y) {
		x -= mZoomInfo.screenStartX;
		y -= mZoomInfo.screenStartY;
		
		float posX = mScrollRectX + (x * mZoomInfo.scaleWidth);
		float posY = mScrollRectY + (y * mZoomInfo.scaleHeight);

		if (isSmooth){
			calcSmoothZoom(offsetOrZoomStep, mZoomInfo); // new zoom data
		}
		else{
			calcStepZoom(offsetOrZoomStep, mZoomInfo); // new zoom data
		}

		mScrollRectX = (int) (posX - x * mZoomInfo.scaleWidth);
		mScrollRectY = (int) (posY - y * mZoomInfo.scaleHeight);
		fixScrollXY();

		ImageViewZoom.this.invalidate();
	}

	// fix scroll x and y so user cant scroll outside the image
	protected void fixScrollXY() {
		if (mScrollRectX < 0) {
			mScrollRectX = 0;
		} else if (mScrollRectX + mZoomInfo.zoomDisplayWidth >= mBitmap.getWidth()) {
			mScrollRectX = mBitmap.getWidth() - mZoomInfo.zoomDisplayWidth;
		}

		if (mScrollRectY < 0) {
			mScrollRectY = 0;
		} else if (mScrollRectY + mZoomInfo.zoomDisplayHeight >= mBitmap.getHeight()) {
			mScrollRectY = mBitmap.getHeight() - mZoomInfo.zoomDisplayHeight;
		}
	}

	protected boolean isImageValid() {
		return mBitmap != null && !mIsInChanging && getWidth() > 0;
	}

	protected void scroll(float distanceX, float distanceY) {
		mScrollRectX = mScrollRectX + (int) (distanceX * mZoomInfo.scaleWidth);
		mScrollRectY = mScrollRectY + (int) (distanceY * mZoomInfo.scaleHeight);
		fixScrollXY();
		ImageViewZoom.this.invalidate(); // force a redraw
	}

	protected void doubleTapZoom(float x, float y) {
		mCurrentZoomStep += mCurrentZoomInc;
		if (mCurrentZoomStep == maxZoomSteps || mCurrentZoomStep == 1) {
			mCurrentZoomInc = -mCurrentZoomInc;
		}

		zoomIt(false, mCurrentZoomStep, x, y);
	}

	// ment to be overriden - add some hotspots or simular
	protected void onImageClick(float posX, float posY) {
	}

	private class GestureListener extends GestureDetector.SimpleOnGestureListener {
		// its important that small bitmal is loaded
		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
			if (isImageValid()) {
				scroll(distanceX, distanceY);
			}
			return true;
		}

		@Override
		public boolean onDown(MotionEvent e) {
			if (isImageValid()) {
				float posX = (e.getX() - mZoomInfo.screenStartX) * mZoomInfo.scaleWidth + mScrollRectX;
				float posY = (e.getY() - mZoomInfo.screenStartY) * mZoomInfo.scaleWidth + mScrollRectY;
				onImageClick(posX, posY);
			}
			return true;
		}

		// event when double tap occurs
		@Override
		public boolean onDoubleTap(MotionEvent e) {
			if (isImageValid() && doubleTapZooms) {
				doubleTapZoom(e.getX(), e.getY());
			}
			return true;
		}
	}

}
