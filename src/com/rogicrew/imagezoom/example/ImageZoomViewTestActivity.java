package com.rogicrew.imagezoom.example;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.rogicrew.imagezoom.ImageViewZoom;
import com.rogicrew.imagezoom.R;

public class ImageZoomViewTestActivity extends Activity {
	private ImageViewZoom mImageView;
	private Bitmap mBitmap;
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        mImageView = (ImageViewZoom)findViewById(R.id.imagezoomComponent);
        loadBitmap(R.drawable.picture1);
    }
    
    private void loadBitmap(int res){
    	if (mBitmap != null){
    		mBitmap.recycle();
    	}
    	mBitmap = UnscaledBitmapOperations.loadFromResource(getResources(), res, null);
    	mImageView.setImage(mBitmap);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) 
    {
        switch (item.getItemId()) {
        case R.id.picture1:
        	loadBitmap(R.drawable.picture1);
        	return true;
        case R.id.picture2:
        	loadBitmap(R.drawable.picture2);
        	return true;	
        case R.id.picture3:
        	loadBitmap(R.drawable.picture3);
        	return true;	
        case R.id.picture4:
        	loadBitmap(R.drawable.picture4);
        	return true; 	
        }
        return false;
    }
}