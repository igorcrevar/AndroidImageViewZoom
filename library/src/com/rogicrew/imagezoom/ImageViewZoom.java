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

	protected ZoomInfo mZoomInfo = new ZoomInfo();
	protected ImageViewZoomOptions mOptions = null;
	protected boolean mIsInChanging = false;
	protected int mCurrentZoomStep = 1;
	protected int mCurrentZoomInc = 1;
	protected int mMinZoomWidth = 0;
	protected int mMaxZoomWidth = 0;
	protected int mScrollRectX = 0; // current left location of scroll rect
	protected int mScrollRectY = 0; // current top location of scroll rect
	protected Paint mPaint;
	protected Bitmap mBitmap = null; //actual bitmap we are drawing
	
	private Rect mSrcRect = new Rect();
	private Rect mDstRect = new Rect();	
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
			onTouchHandler.init();
		}
		catch(NoSuchMethodException nsme){		//not exist
		}
		catch (Exception e){			
		}
		
		if (onTouchHandler == null){ //if multitouch handler not created create simple one
			try {
				dynamicClass = (Class<OnTouchInterface>)classLoader.loadClass("com.rogicrew.imagezoom.ontouch.OnTouchSingle");
				onTouchHandler = dynamicClass.newInstance();
				onTouchHandler.init();				
			}
			catch (Exception e){				
			}
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
			postDelayed(backgroundQualityUpdateRunnable, mOptions.backgroundQualityUpdateMilis);
	}
	
	protected void stopBackgroundQualityUpdate(){
		backgroundQualityUpdateHandler.
			removeCallbacks(backgroundQualityUpdateRunnable, mOptions.backgroundQualityUpdateMilis);
	}
	
	public void setVisibility(boolean isVisible) {
		this.setVisibility(isVisible ? View.VISIBLE : View.GONE);
	}

	/**
	 * Sets imageviewzoom options
	 * @param options
	 */
	public void setOptions(ImageViewZoomOptions options){
		mOptions = options;
		//update times for on touch handler
		onTouchHandler.setTimes(mOptions.timeForClick, mOptions.timeForDoubleClick);
	}
	
	/**
	 * Sets bitmap which we want to show and sets parent container width. 
	 * @param bitmap
	 * @param widthOfParent width of parent container in pixels
	 */
	public void setImage(Bitmap bitmap, int widthOfParent) {
		mIsInChanging = true;
		if (mOptions == null){
			setOptions(new ImageViewZoomOptions()); //init default if not exist
		}
		mBitmap = bitmap;
		//calculate min/max zoom
		if (mOptions.minWidth > 0){
			mMinZoomWidth = mOptions.minWidth;
		}
		else if (widthOfParent <= bitmap.getWidth()){ //minimal zoom cannot be greater than parent width
			mMinZoomWidth = widthOfParent;
		}
		else{
			mMinZoomWidth = bitmap.getWidth();
		}
		
		if (mOptions.maxWidth > 0){
			mMaxZoomWidth = mOptions.maxWidth;
		}
		else if (mOptions.maxWidthMultiplier > 0.0f){
			mMaxZoomWidth = (int)(mOptions.maxWidthMultiplier * mMinZoomWidth);
		}
		else{
			mMaxZoomWidth = mBitmap.getHeight();
		}		
		
		mCurrentZoomStep = 1;
		mCurrentZoomInc = 1;
		mScrollRectY = mScrollRectX = 0;
		calcStepZoom(mCurrentZoomStep, mZoomInfo);
		setVisibility(true);
		ImageViewZoom.this.invalidate();
		mIsInChanging = false;
	}
	
	/**
	 * Sets bitmap we want to show. Proxy for setImage(Bitmap bitmap, int widthOfParent). widthOfParent will be current parent container width
	 * @param bitmap
	 */
	public void setImage(Bitmap bitmap) {
		setImage(bitmap, this.getWidth());
	}
	
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		if (mBitmap != null){
			//reset image, to correct calculate minWidth and other parameters 
			setImage(mBitmap, w);
		}
	}

	@Override
	protected void onDraw(Canvas canvas) {
		// bitmap not exist or component didnt recieved width yet
		if (isImageValid()) {
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
		else if (step == mOptions.maxZoomSteps){
			updateZoomInfo(mMaxZoomWidth, zoomInfo);
			return;
		}
		
		//decrement step and maxzoomsteps
		float multiplier = (step - 1.0f) / (mOptions.maxZoomSteps - 1.0f);
		float newWidth = (float)(mMaxZoomWidth - mMinZoomWidth) * multiplier + mMinZoomWidth;
		updateZoomInfo(newWidth, zoomInfo);		
	}

	protected void calcSmoothZoom(int offset, ZoomInfo zoomInfo) {
		int newWidth = zoomInfo.currentWidth + offset;
		updateZoomInfo(newWidth, zoomInfo);
		
		//for now - reset double tap zoom variables to middle zoom step
		mCurrentZoomStep = (int)Math.ceil((double)mOptions.maxZoomSteps / 2);
		mCurrentZoomInc = offset > 0 ? 1 : -1;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		onTouchHandler.processEvent(event);
		
		if (!isImageValid()){ //if image is not ready to manipulate just exit
			return true;
		}
		
		if (onTouchHandler.isDoubleClick()){
			if (mOptions.isDoubleTapZoomEnabled){
				Pointer p = onTouchHandler.getClickPoint();
				doubleTapZoom(p.x, p.y);
			}
		}
		else if (onTouchHandler.isSingleClick()){
			Pointer p = onTouchHandler.getClickPoint();
			float posX = (p.x - mZoomInfo.screenStartX) * mZoomInfo.scaleWidth + mScrollRectX;
			float posY = (p.y - mZoomInfo.screenStartY) * mZoomInfo.scaleWidth + mScrollRectY;
			//call click handler only if click in image boundary
			if (posX >= 0 && posX < mBitmap.getWidth() && posY >= 0 && posY < mBitmap.getHeight()){ 
				onImageClick(posX, posY);
			}
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
		PointF firstFingerDiff = getDiffVector(firstOld, firstNew);
		PointF secondFingerDiff = getDiffVector(secondOld, secondNew);
		float firstFingerDistance = getVectorNorm(firstFingerDiff);
		float secondFingerDistance = getVectorNorm(secondFingerDiff);
		int distance = (int)(firstFingerDistance + secondFingerDistance);
		
		if (distance < mOptions.pinchToZoomMinDistance){
			return false;
		}
		
		/*//if both fingers has been moved then check if there direction is good for pinch zoom
		float abAbs = firstFingerDistance * secondFingerDistance;
		if (abAbs > 0) {
			float ab = firstFingerDiff.x * secondFingerDiff.x + firstFingerDiff.y * secondFingerDiff.y;
			double angle = Math.acos(ab / abAbs) * 180 / Math.PI;
			if (angle < 180 - mOptions.angleTolerant || angle > mOptions.angleTolerant){
				return false;
			}
		}*/
		
		// point beetween two fingers at start - it will be center of new scroll position
		PointF center = getCenterVector(firstOld, secondOld);
		// difference from start and end points
		float startDiff = getDistance(firstOld, secondOld);
		float endDiff = getDistance(firstNew, secondNew);
		
		if (startDiff < endDiff){
			//zooom in
			zoomIt(true, (int)(distance * mOptions.distanceZoomMultiplier), center.x, center.y);
		}
		else{
			//zoom out
			zoomIt(true, (int)(-distance * mOptions.distanceZoomMultiplier), center.x, center.y);
		}
		
		if (mOptions.afterPinchZoomSetLowerQualityAndUpdateQualityLater) {
			setPaintQuality(false);
			startBackgroundQualityUpdate();
		}		

		return true;
	}

	protected float getDistance(PointF a, PointF b) {
		double difx = a.x - b.x;
		double dify = a.y - b.y;
		double value = Math.sqrt(difx * difx + dify * dify);
		return (float) value;
	}
	
	protected PointF getDiffVector(PointF v1, PointF v2) {
		return new PointF(v2.x - v1.x, v2.y - v1.y);
	}

	protected float getVectorNorm(PointF v) {
		float value = android.util.FloatMath.sqrt(v.x * v.x + v.y * v.y);
		return value;
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
	
	//image is valid if is set, not in changing state(calculation) and parent container has width
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
		if (mCurrentZoomStep == mOptions.maxZoomSteps || mCurrentZoomStep == 1) {
			mCurrentZoomInc = -mCurrentZoomInc;
		}

		zoomIt(false, mCurrentZoomStep, x, y);
	}

	/**
	 * Handler for onclick on image event. Override this method in child
	 * @param posX float, x position of finger
	 * @param posY float, y position of finger
	 */
	protected void onImageClick(float posX, float posY) {
	}
}
