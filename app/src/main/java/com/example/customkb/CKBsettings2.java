package com.example.customkb;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.InputType;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;

import androidx.core.view.ViewCompat;

import java.util.ArrayList;

public class CKBsettings2 extends ViewGroup
{
	public static final String TAG="customkb";
	public static final int
		STATE_ROOT=0,
		STATE_CONFIG=1,
		STATE_TEST=2,
		STATE_COLORPICKER=3;
	int state=STATE_ROOT;
	String[] strings_root=
	{
		"<- Back",
		"Config",
		"Keyboard test",
		"Color Picker",
	};
	String[] strings_test=
	{
		"<- Back",
		"Text",
		"URL",
		"Numeric",
		"Signed decimal",
		"Password",
	};

	int w, h,//screen dimensions
		px, py, dx, dy;//view position & dimensions
	int scroll_y,//view position = scroll_y + position in list,  scroll_y <= 0
		h_item, h_multiline, h_total;
	RectF rectf=new RectF();
	Paint paint_text=new Paint(), paint_item=new Paint();
	float textSize;
	boolean scrolling=false;
	float scrollThreshold=20;//px
	float scrollSpeed=0;//px/frame
	boolean redraw=false;
	int fastThreshold, fastSpeed, slowSpeed;
	float friction=0.9f;//from 10px/f to 0 in 22 frames
	TouchInfo touchInfo=new TouchInfo();
	EditText2 multiline, url, numeric, decimal, password,
		box_config;
	ColorPicker colorPicker;
	public CKBactivity activity;

	int frame_count=0;


	enum ViewId//child views
	{
		ZERO_PLACEHOLDER,
		ROOT,//must be first
		MULTILINE, URL, NUMERIC, DECIMAL, PASSWORD,
		COLOR_PICKER,
		BOX_CONFIG,
	}
	public static class EditText2 extends androidx.appcompat.widget.AppCompatEditText
	{
		CKBsettings2 parent;
		int viewId;
		public EditText2(Context context){super(context);}
		public EditText2(Context context, AttributeSet attrs){super(context, attrs);}
		public EditText2(Context context, AttributeSet attrs, int defStyle){super(context, attrs, defStyle);}

		public EditText2(Context context, CKBsettings2 _parent, int _viewId)
		{
			super(context);
			parent=_parent; viewId=_viewId;
			setId(viewId);
		}
		@Override public boolean onTouchEvent(MotionEvent e)
		{
			if(parent.touch(e, viewId))
				return true;
			return super.onTouchEvent(e);
		}
	}
	void init(Context context)
	{
		setId(ViewId.ROOT.ordinal());

		//test
		multiline	=new EditText2(context, this, ViewId.MULTILINE	.ordinal());
		url			=new EditText2(context, this, ViewId.URL		.ordinal());
		numeric		=new EditText2(context, this, ViewId.NUMERIC	.ordinal());
		decimal		=new EditText2(context, this, ViewId.DECIMAL	.ordinal());
		password	=new EditText2(context, this, ViewId.PASSWORD	.ordinal());
		box_config	=new EditText2(context, this, ViewId.BOX_CONFIG	.ordinal());

		multiline.setInputType(InputType.TYPE_CLASS_TEXT);
		multiline.setSingleLine(false);
		multiline.setImeOptions(EditorInfo.IME_FLAG_NO_ENTER_ACTION);
		url		.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_URI);
		numeric	.setInputType(InputType.TYPE_CLASS_NUMBER);
		decimal	.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_SIGNED|InputType.TYPE_NUMBER_FLAG_DECIMAL);
		password.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);

		addView(multiline);
		addView(url);
		addView(numeric);
		addView(decimal);
		addView(password);
		//end test

		colorPicker=new ColorPicker(context, ViewId.COLOR_PICKER.ordinal());
		addView(colorPicker);
	}
	public CKBsettings2(Context context){super(context); init(context);}
	public CKBsettings2(Context context, AttributeSet attrs){super(context, attrs); init(context);}
	public CKBsettings2(Context context, AttributeSet attrs, int defStyleAttr){super(context, attrs, defStyleAttr); init(context);}
//	public CKBsettings2(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes){super(context, attrs, defStyleAttr, defStyleRes); init(context);}

	@Override protected void onLayout(boolean changed, int l, int t, int r, int b)
	{
		px=l; py=t; dx=r-l; dy=b-t;

		multiline.measure(dx, MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
		h_multiline=multiline.getMeasuredHeight();

		h_total=h_item+h_multiline+h_item*8;
		scroll();
	}
	static void setTheme(EditText et, int color_theme, float textSize)
	{
		et.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
		et.setTextColor(0xFF000000);

		ColorStateList csl=ColorStateList.valueOf(color_theme);//https://stackoverflow.com/questions/40838069/programmatically-changing-underline-color-of-edittext
		ViewCompat.setBackgroundTintList(et, csl);
	}
	int randLightShade()
	{
		return 128+(int)(128*Math.random());
	}
	void setBkColorRand()
	{
		int color=0xFF000000|randLightShade()<<16|randLightShade()<<8|randLightShade();
		setBackgroundColor(color);
	}
	public void setDimensions()
	{
		DisplayMetrics metrics=Resources.getSystem().getDisplayMetrics();
		w=metrics.widthPixels; h=metrics.heightPixels;
		int length=Math.max(w, h);
		h_item=length/15;
		textSize=length*0.02f;
		paint_text.setTextSize(textSize);
		paint_item.setColor(0x80808080);
		colorPicker.setTextSize(textSize);

		//test
		int color_theme=0xFF03DAC5;//colors.xml/teal_200
		//int color_theme=0xFF0000FF;
		setTheme(multiline,	color_theme, textSize);
		setTheme(url,		color_theme, textSize);
		setTheme(numeric,	color_theme, textSize);
		setTheme(decimal,	color_theme, textSize);
		setTheme(password,	color_theme, textSize);
		//end test

		//scroll parameters
		fastThreshold=Math.max(h/20, 1);//109
		fastSpeed=Math.max(h/60, 1);//36
		slowSpeed=Math.max(h/300, 1);//7
		Log.e(TAG, String.format("threshold=%d, fast=%d, slow=%d", fastThreshold, fastSpeed, slowSpeed));
		setTestVisibility(INVISIBLE);
		scroll_y=0;

		setBkColorRand();
	}

	//drawing functions
	void drawLabel(float x1, float x2, float y1, float y2, float r, String label, Canvas canvas)
	{
		rectf.set(x1, y1, x2, y2);
		canvas.drawRect(rectf, paint_item);

		rectf.set(x1+r, y1+r, x2-r, y2-r);
		canvas.drawRoundRect(rectf, r, r, paint_item);

		canvas.drawText(label, x1+r+r, (y1+y2+textSize)*0.5f, paint_text);
		//canvas.drawText(label, x1+r, y2-r, paint_text);
	}
	void drawOptions(String[] labels, float yPos, float radius, Canvas canvas)
	{
		for(String label:labels)
		{
			drawLabel(0, dx, yPos, yPos+h_item, radius, label, canvas);
			yPos+=h_item;
		}
	}
	@Override public void onDraw(Canvas canvas)
	{
		float radius=20, yPos=scroll_y;
		switch(state)
		{
		case STATE_ROOT:
			drawOptions(strings_root, yPos, radius, canvas);
			break;
		case STATE_CONFIG:
			drawLabel(0, dx, 0, h_item, radius, "Apply", canvas);
			break;
		case STATE_TEST:
			drawLabel(0, dx, yPos, yPos+h_item, radius, strings_test[0], canvas);
			yPos+=h_item;

			drawLabel(0, dx, yPos, yPos+h_item, radius, strings_test[1], canvas);
			yPos+=h_item+h_multiline;

			drawLabel(0, dx, yPos, yPos+h_item, radius, strings_test[2], canvas);
			yPos+=h_item+h_item;

			drawLabel(0, dx, yPos, yPos+h_item, radius, strings_test[3], canvas);
			yPos+=h_item+h_item;

			drawLabel(0, dx, yPos, yPos+h_item, radius, strings_test[4], canvas);
			yPos+=h_item+h_item;

			drawLabel(0, dx, yPos, yPos+h_item, radius, strings_test[5], canvas);

			//for(int ky=0;ky<strings_test.length;++ky)
			//{
			//	drawLabel(0, dx, yPos, yPos+h_item, radius, strings_test[ky], canvas);
			//	yPos+=heights[ky]+h_item;
			//}
			break;
		case STATE_COLORPICKER:
			drawLabel(0, dx, 0, h_item, radius, "Apply", canvas);
			break;
		}
		//canvas.drawText(String.format("frame %d scroll_y %d", frame_count, scroll_y), w>>1, h>>1, paint_text);//
		//++frame_count;//
		if(redraw)
		{
			redraw=false;
			scroll();
		}
	}

	void setTestVisibility(int visibility)
	{
		multiline	.setVisibility(visibility);
		url			.setVisibility(visibility);
		numeric		.setVisibility(visibility);
		decimal		.setVisibility(visibility);
		password	.setVisibility(visibility);
	}
	boolean isStateTest(){return state==STATE_TEST;}
	boolean isStateTheme(){return state==STATE_COLORPICKER;}
	void scroll()
	{
		if(multiline!=null)//if every edittext was initialized
		{
			if(scrolling)
				scroll_y+=scrollSpeed;
			else//bring views back on screen
			{
				int y1=dy-h_total,//scroll up limit
					y2=0;//scroll down limit
				if(y1>0)
					y1=0;
				if(scroll_y<y1||scroll_y>y2)
				{
					if(scroll_y>y2)//action for extra scroll down
					{
						scroll_y-=(scroll_y>y2+fastThreshold?fastSpeed:slowSpeed);
						if(scroll_y<y2)
							scroll_y=y2;
						redraw=true;
					}
					if(scroll_y<y1)//action for extra scroll up
					{
						scroll_y+=(scroll_y<y1-fastThreshold?fastSpeed:slowSpeed);
						if(scroll_y>y1)
							scroll_y=y1;
						redraw=true;
					}
				}
				else if(scrollSpeed!=0)
				{
					scroll_y+=scrollSpeed;
					scroll_y=Math.min(Math.max(y1, scroll_y), y2);
					//scroll_y=clamp(y1, scroll_y, y2);
					scrollSpeed*=friction;
					if(Math.abs(scrollSpeed)<1)
						scrollSpeed=0;
					redraw=true;
				}
			}
			if(state==STATE_TEST)//set position of keyboard test boxes
			{
				int y=scroll_y+h_item*2;
				multiline	.layout(0, y, dx, y+h_multiline);	y+=h_multiline+h_item;
				url			.layout(0, y, dx, y+h_item);		y+=h_item*2;
				numeric		.layout(0, y, dx, y+h_item);		y+=h_item*2;
				decimal		.layout(0, y, dx, y+h_item);		y+=h_item*2;
				password	.layout(0, y, dx, y+h_item);	//	y+=h_item*2;
			}
			else if(state==STATE_COLORPICKER)//set position of color picker
				colorPicker.layout(0, h_item, dx, dy);
			invalidate();
		}
	}
	public boolean touch(MotionEvent e, int viewId)
	{
		if(viewId>ViewId.ROOT.ordinal())
			return scrolling;//if scrolling then handled in root view
		touchInfo.update(e);
		TouchInfo.Pointer p=touchInfo.findPointer(0);
		if(p==null)//no multitouch
			return true;
		switch(p.state)
		{
		case TouchInfo.S_DOWN:
			return true;
		case TouchInfo.S_MOVED:
			if(Math.abs(p.yTravel())>scrollThreshold)
				scrolling=true;
			if(scrolling)
			{
				scrollSpeed=p.y-p.prevy;
				if(scrollSpeed!=0)
					scroll();
				return true;
			}
			break;
		case TouchInfo.S_UP:
		case TouchInfo.S_CANCEL:
			if(!scrolling)
			{
				int c0		=(int)((p.starty-scroll_y)/h_item),
					choice	=(int)((p.y		-scroll_y)/h_item);
				if(c0==choice)
				{
					int s0=state;
					switch(state)
					{
					case STATE_ROOT:
						switch(choice)
						{
						case 0://quit
							activity.finish();
							break;
						case 1:
							state=STATE_CONFIG;
							break;
						case 2:
							setTestVisibility(VISIBLE);
							state=STATE_TEST;
							break;
						case 3:
							colorPicker.setVisibility(VISIBLE);
							state=STATE_COLORPICKER;
							break;
						}
						break;
					case STATE_CONFIG:
						if(choice==0)
							state=STATE_ROOT;
						break;
					case STATE_TEST:
						if(choice==0)
						{
							setTestVisibility(INVISIBLE);
							state=STATE_ROOT;
						}
						break;
					case STATE_COLORPICKER:
						if(choice==0)
						{
							colorPicker.setVisibility(INVISIBLE);
							state=STATE_ROOT;
						}
						break;
					default:
						break;
					}
					if(state!=s0)
					{
						switch(state)
						{
						case STATE_ROOT:
							activity.setTitle("CKB Settings");
							break;
						case STATE_CONFIG:
							activity.setTitle("Config");
							break;
						case STATE_TEST:
							activity.setTitle("Keyboard Test");
							break;
						case STATE_COLORPICKER:
							activity.setTitle("Color Picker");
							break;
						default:
							activity.setTitle("What.");
							break;
						}
					}
				}
			}
			scrolling=false;
			scroll();
			return true;
		}
		return false;
	}
	@Override public boolean onTouchEvent(MotionEvent e)
	{
		if(touch(e, ViewId.ROOT.ordinal()))
			return true;
		return super.onTouchEvent(e);
	}
}
