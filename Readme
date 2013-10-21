## ImageViewZoom 
Library provides you with options for manipulating images: zoom (double tap and/or pinch zoom), scroll, determine position of touch, etc...
Example project included!

### public methods:
 + ``` java setImage(Bitmap) ``` - Bitmap which you want to show in view(layout)
 + ``` java setImage(Bitmap bitmap, int widthOfParent) ``` - you can set maximum width(in pixels) of parent  if you want. But in most cases above will be enough.
 
### You can extend class and override method
``` java protected void onImageClick(float posX, float posY) ```
to add custom ontouchhandler (for example try touch Homer's head on second picture in example project)

### ImageViewZoomOptions:
+ minWidth  default: 0 if minWidth == 0 than minWidth = min(parent view width, bitmap width)
+ maxWidth default: 0 if > 0 than this width will be for max zoom
+ maxWidthMultiplier default: 0 if maxWidth == 0 && maxWidthMultiplier > 0 than maxWidth = minWidth * maxWidthMultiplier
+ pinchToZoomMinDistance default: 5
+ isDoubleTapZoomEnabled default: true  should double tap perform zoom in steps
+ maxZoomSteps default: 3 how much zoom steps
+ timeForClick default: 300 in miliseconds. if finger down/up interval <= timeForClick than click occured 
+ timeForDoubleClick default: 300 in miliseconds. if two click occured in interval <= timeForDoubleClick we have double click
+ distanceZoomMultiplier default: 4.0f for pinch zoom - zooming in pixels = move distance * distanceZoomMultiplier
+ backgroundQualityUpdateMilis default: 2000 - this is paired with setting bellow to allow "smarter" / "optimize" pinch zooming but its not fully tested
+ afterPinchZoomSetLowerQualityAndUpdateQualityLater - default: false - better leave it as it is. If true may render faster or render image slower, but
this is not fully tested
+ angleTolerant default: 30 degree tolerance for pinch zoom - leave as it is in most cases - currently not used, because made a lot of problems in past
