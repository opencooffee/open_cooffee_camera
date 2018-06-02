package com.opencooffeecamera.library;

import android.content.Context;
import android.graphics.Typeface;
import android.support.v7.widget.AppCompatTextView;
import android.util.AttributeSet;

import com.opencooffeecamera.Common;
import com.opencooffeecamera.application.OpenCooffeeCameraApplication;

public class CustomTextView extends AppCompatTextView {

	//private static final String LOG_TAG = CustomTextView.class.getSimpleName();

	public CustomTextView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	
		setCustomFont(attrs);
	}

	public CustomTextView(Context context, AttributeSet attrs) {
	    super(context, attrs);
	    
	    setCustomFont(attrs);
	}
	 	
	private void setCustomFont(AttributeSet attrs) {
	
		int textStyle = attrs.getAttributeIntValue(Common.ANDROID_SCHEMA, "textStyle", Typeface.NORMAL);

	    Typeface customFont = selectTypeface(textStyle);
	
	    setTypeface(customFont);
	}
 	
	private Typeface selectTypeface(int textStyle) {

		if (textStyle == Typeface.NORMAL) {
			return OpenCooffeeCameraApplication.getRegularFont();
		} else {	    
			return OpenCooffeeCameraApplication.getBoldFont();
	    }
	}
		
}