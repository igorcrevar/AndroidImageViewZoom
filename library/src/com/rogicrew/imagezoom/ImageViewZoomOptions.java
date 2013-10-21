package com.rogicrew.imagezoom;

public class ImageViewZoomOptions {
	public int minWidth; //if minWidth == 0 than minWidth = min(parent view width, bitmap width)
	public int maxWidth; //if > 0 than this width will be for max zoom
	public float maxWidthMultiplier; //if maxWidth == 0 && maxWidthMultiplier > 0 than maxWidth = minWidth * maxWidthMultiplier
	public float pinchToZoomMinDistance; //must be greaterequal to 5 -- minimal distance beetween two fingers to perform pinch zoom
	public boolean isDoubleTapZoomEnabled; //should double tap perform zoom in steps
	public int maxZoomSteps; //how much zoom steps
	public long timeForClick; //in miliseconds. if finger down/up interval <= timeForClick than click occured 
	public long timeForDoubleClick; //in miliseconds. if two click occured in interval <= timeForDoubleClick we have double click
	public float distanceZoomMultiplier; //for pinch zoom - zooming in pixels = move distance * distanceZoomMultiplier
	public long backgroundQualityUpdateMilis; //
	public int angleTolerant; //degree tolerance for pinch zoom - leave as it is in most cases
	public boolean afterPinchZoomSetLowerQualityAndUpdateQualityLater; // better to leave false as it is
	
	public ImageViewZoomOptions(){
		minWidth = 0;
		maxWidth = 0;
		maxWidthMultiplier = 0.0f;
		pinchToZoomMinDistance = 5;
		isDoubleTapZoomEnabled = true;
		maxZoomSteps = 3;
		angleTolerant = 30;
		timeForClick = 300;
		timeForDoubleClick = 300;
		backgroundQualityUpdateMilis = 2000;
		distanceZoomMultiplier = 4.0f;
		afterPinchZoomSetLowerQualityAndUpdateQualityLater = false;
	}
}
