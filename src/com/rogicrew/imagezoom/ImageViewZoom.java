package com.rogicrew.imagezoom;

import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;

import com.rogicrew.imagezoom.ontouch.OnTouchInterface;
import com.rogicrew.imagezoom.ontouch.Pointer;

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
	public static float minMaxZoom = 2.0f;
	public static float pinchToZoomMinDistance = 8; //must be greaterequal to 5
	public static boolean doubleTapZooms = true;
	public static int maxZoomSteps = 3;
	public static int angleTolerant = 50;
	public static long timeForClick = 300;
	public static long timeForDoubleClick = 300;
	public static long backgroundQualityUpdateMilis = 2000;
	public static int distanceZoomMultiplier = 3;
	
	private Rect mSrcRect = new Rect();
	private Rect mDstRect = new Rect();
	private boolean mIsInChanging = false;
	private int mCurrentZoomStep = 1;
	private int mCurrentZoomInc = 1;

	private Paint mPaint;
	private Bitmap mBitmap = null;

	private int mMinZoomWidth = 0;
	private int mMaxZoomWidth = 0;
	private int mScrollRectX = 0; // current left location of scroll rect
	private int mScrollRectY = 0; // current top location of scroll rect

	protected ZoomInfo mZoomInfo = new ZoomInfo();

	private OnTouchInterface onTouchHandler;
	
	private Handler backgroundQualityUpdateHandler = new Handler();
	private Runnable backgroundQualityUpdateRunnable = new Runnable() {		
		@Override
		public void run() {
			setPaintQuality(true);
			stopBackgroundQualityUpdate();
		}
	}; 
	
	public ImageViewZoom(Context context, AttributeSet attrs) {
		super(context, attrs);

		InitOnTouchHandler();
		setOrientation(VERTICAL);
		//without those settings zoomed bitmap will be full of ugly squares 
		mPaint = new Paint();
		setPaintQuality(true);

		//because we are overriding onDraw method
		this.setWillNotDraw(false);
	}
	
	
	/**
	 * Create onTouchHandler by reflection depending on mulitouch capability of device
	 */
	@SuppressWarnings("unchecked")
	private void InitOnTouchHandler(){
		ClassLoader classLoader = ImageViewZoom.class.getClassLoader();
		Class<OnTouchInterface> dynamicClass;
		//check if there is multitouch
		try {
			Class<?> eventClass = MotionEvent.class;
			eventClass.getMethod("getPointerCount");
			dynamicClass = (Class<OnTouchInterface>)classLoader.loadClass("com.rogicrew.imagezoom.ontouch.OnTouchMulti");
			onTouchHandler = dynamicClass.newInstance();
			//set times by reflection
			onTouchHandler.create(timeForClick, timeForDoubleClick);
		}
		catch(NoSuchMethodException nsme){
			try {
				dynamicClass = (Class<OnTouchInterface>)classLoader.loadClass("com.rogicrew.imagezoom.ontouch.OnTouchSingle");
				onTouchHandler = dynamicClass.newInstance();
				//set times by reflection
				onTouchHandler.create(timeForClick, timeForDoubleClick);
			}
			catch (Exception e){				
			}
		}
		catch (Exception e){			
		}
	}

	protected void setPaintQuality(boolean isHigh) {
		mPaint.setAntiAlias(isHigh);
		mPaint.setFilterBitmap(isHigh);
		mPaint.setDither(isHigh);
	}

	protected void startBackgroundQualityUpdate(){
		stopBackgroundQualityUpdate();
		backgroundQualityUpdateHandler.
			postDelayed(backgroundQualityUpdateRunnable, backgroundQualityUpdateMilis);
	}
	
	protected void stopBackgroundQualityUpdate(){
		backgroundQualityUpdateHandler.
			removeCallbacks(backgroundQualityUpdateRunnable, backgroundQualityUpdateMilis);
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
		float newWidth = (float)(mMaxZoomWidth - mMinZoomWidth) * (float)step / (float)maxZoomSteps + mMinZoomWidth;
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
		onTouchHandler.processEvent(event);
		
		if (!isImageValid()){ //if image is not ready to manipulate just exit
			return true;
		}
		
		if (onTouchHandler.isDoubleClick()){
			if (doubleTapZooms){
				Pointer p = onTouchHandler.getClickPoint();
				doubleTapZoom(p.x, p.y);
			}
		}
		else if (onTouchHandler.isSingleClick()){
			Pointer p = onTouchHandler.getClickPoint();
			float posX = (p.x - mZoomInfo.screenStartX) * mZoomInfo.scaleWidth + mScrollRectX;
			float posY = (p.y - mZoomInfo.screenStartY) * mZoomInfo.scaleWidth + mScrollRectY;
			onImageClick(posX, posY);
		}
		else if (onTouchHandler.isScroll()){
			Pointer p = onTouchHandler.getAllPoints().get(0);
			float distanceX = p.lastX - p.x;
			float distanceY = p.lastY - p.y;
			scroll(distanceX, distanceY);
		}
		else if (onTouchHandler.isMultitouch() && onTouchHandler.isMove()){
			List<Pointer> points = onTouchHandler.getAllPoints();
			if (points.size() == 2){
				Pointer f = points.get(0);
				Pointer s = points.get(1);
				PointF firstOld = new PointF(f.lastX, f.lastY);
				PointF firstNew = new PointF(f.x, f.y);
				PointF secondOld = new PointF(s.lastX, s.lastY);
				PointF secondNew = new PointF(s.x, s.y);
				zoomIfPinch(firstOld, firstNew, secondOld, secondNew);
			}
		}
		
		return true; //handled
	}

	protected boolean zoomIfPinch(PointF firstOld, PointF firstNew, PointF secondOld, PointF secondNew) {
		PointF firstFingerDiff = getDifVector(firstOld, firstNew);
		PointF secondFingerDiff = getDifVector(secondOld, secondNew);
		float firstFingerDistance = getVectorNorm(firstFingerDiff);
		float secondFingerDistance = getVectorNorm(secondFingerDiff);
		int distance = (int)(firstFingerDistance + secondFingerDistance);
		
		if (distance < pinchToZoomMinDistance){
			return false;
		}
		
		//if both fingers has been moved then check if there direction is good for pinch zoom
		if (firstFingerDistance > 1.0f && secondFingerDistance > 1.0f){
			float angleDiff = Math.abs(getVectorAngle(firstFingerDiff) - getVectorAngle(secondFingerDiff));
			
			if (angleDiff < 180 - angleTolerant || angleDiff > 180 + angleTolerant){
				return false;
			}
		}
		
		// point beetween two fingers at start - it will be center of new scroll position
		PointF center = getCenterVector(firstOld, secondOld);
		// difference from start and end points
		float startDiff = getDistance(firstOld, secondOld);
		float endDiff = getDistance(firstNew, secondNew);
		
		if (startDiff < endDiff){
			//zooom in
			zoomIt(true, distance * distanceZoomMultiplier, center.x, center.y);
		}
		else{
			//zoom out
			zoomIt(true, -distance * distanceZoomMultiplier, center.x, center.y);
		}
		
		setPaintQuality(false);
		startBackgroundQualityUpdate();

		return true;
	}

	protected float getDistance(PointF a, PointF b) {
		double difx = a.x - b.x;
		double dify = a.y - b.y;
		double value = Math.sqrt(difx * difx + dify * dify);
		return (float) value;
	}

	protected float getVectorAngle(PointF v) {
		if (v.y == 0) {
			return v.x >= 0 ? 0 : 180;
		}

		float norm = getVectorNorm(v);
		double angle = Math.asin(Math.abs(v.y) / norm) * 180 / Math.PI;

		if (v.y < 0) {
			if (v.x > 0) {
				angle = 270 + angle;
				if (angle >= 360){
					angle = angle - 360;
				}
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
}
