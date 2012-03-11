package com.rogicrew.imagezoom.example;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.util.AttributeSet;

import com.rogicrew.imagezoom.ImageViewZoom;
import com.rogicrew.imagezoom.ImageViewZoomOptions;

public class ImageViewZoomTest extends ImageViewZoom {

	public ImageViewZoomTest(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	@Override
	public void setImage(Bitmap bitmap) {
		//Simpsons
		if (bitmap.getWidth() == 260){
			if (mOptions == null){
				setOptions(new ImageViewZoomOptions());
			}
			mOptions.maxWidthMultiplier = 3.0f;
			mOptions.maxZoomSteps = 4;
		}
		super.setImage(bitmap);
	}

	@Override
	protected void onImageClick(float posX, float posY) {
		if ( posX >= 112 && posX <= 147 &&  posY >= 121 && posY <= 149){
			AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
			builder.setMessage("Homer's head!").setCancelable(false);
			builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
	           public void onClick(DialogInterface dialog, int id) {	
	        	   dialog.dismiss();
	           }
			});
			AlertDialog alert = builder.create();
			alert.show();
		}
	}
}
