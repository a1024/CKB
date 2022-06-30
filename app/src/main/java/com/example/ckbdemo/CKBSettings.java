/*  CKBSettings.java - The CKB settings ViewGroup
    Copyright (C) 2021 Ayman Wagih, unless source link provided

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
    */
package com.example.ckbdemo;

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

public class CKBSettings extends ViewGroup
{
	public static final String TAG="CKBview2";

	enum ViewId//child views
	{
		ZERO_PLACEHOLDER,
		ROOT,
		MULTILINE, URL, NUMERIC, DECIMAL, PASSWORD,
		COLOR_PICKER
	}
	public enum MenuIdx//index of menus in 'menus' array
	{
		L0_ROOT,

		L1_UNICODE, L1_THEME, L1_KB_TEST, L1_P_NUMPAD, L1_L_NUMPAD, L1_P_PASSWORD, L1_L_PASSWORD,

		L2_COLOR_BK, L2_COLOR_BUTTON, L2_COLOR_BUTTON_FAST, L2_ST2_COLOR_BUTTON_PRESSED, L2_COLOR_LABELS, L2_COLOR_SHADOW, L2_COLOR_SHADOW_LETTERS, L2_COLOR_SHADOW_SPECIAL, L2_COLOR_PREVIEW_TEXT,
		L2_PT_HEIGHT, L2_PT_NEW_LAYOUT, L2_FPT_STD, L2_FPT_STD_SHIFT, L2_FPT_URL_STD, L2_FPT_URL_STD_SHIFT, L2_FPT_SYM, L2_FPT_SPK, L2_FPT_SPK_SHIFT, L2_FPT_FUN,
		L2_LT_HEIGHT, L2_LT_NEW_LAYOUT, L2_FLT_STD, L2_FLT_STD_SHIFT, L2_FLT_SYM, L2_FLT_FUN,
		L2_PN_HEIGHT, L2_PN_NEW_LAYOUT, L2_FPN_NUM,
		L2_LN_HEIGHT, L2_LN_NEW_LAYOUT, L2_FLN_NUM,
		L2_PP_HEIGHT, L2_PP_NEW_LAYOUT, L2_FPP_PASSWORD, L2_FPP_PASSWORD_SHIFT,
		L2_LP_HEIGHT, L2_LP_NEW_LAYOUT, L2_FLP_PASSWORD, L2_FLP_PASSWORD_SHIFT,
	}
	public static abstract class MenuInterface
	{
		//abstract public void init();
		abstract public void draw(Canvas canvas);
		abstract public void touch(TouchInfo ti);
	}
	ArrayList<MenuInterface> menus=new ArrayList<>();
	public static class MenuGeneric extends MenuInterface
	{
		//@Override public void init(){}//menus don't include child views
		@Override public void draw(Canvas canvas)
		{
		}
		@Override public void touch(TouchInfo ti)
		{
		}
	}
	public static class EditText2 extends androidx.appcompat.widget.AppCompatEditText
	{
		CKBSettings parent;
		int viewId;
		public EditText2(Context context){super(context);}
		public EditText2(Context context, AttributeSet attrs){super(context, attrs);}
		public EditText2(Context context, AttributeSet attrs, int defStyle){super(context, attrs, defStyle);}

		public EditText2(Context context, CKBSettings _parent, int _viewId)
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
	public static class MenuTest extends MenuInterface
	{
		public static final int
			ET_MULTILINE=0, ET_URL=1, ET_NUMERIC=2, ET_DECIMAL=3, ET_PASSWORD=4, ET_COUNT=5;
		EditText2[] et=new EditText2[ET_COUNT];
		int color_theme;
		float textSize;
		public MenuTest(Context context, CKBSettings _parent, int rootViewId)
		{
			for(int k=0;k<ET_COUNT;++k)
				et[k]=new EditText2(context, _parent, rootViewId+1+k);
			et[ET_MULTILINE	].setInputType(InputType.TYPE_CLASS_TEXT);
			et[ET_MULTILINE	].setSingleLine(false);
			et[ET_MULTILINE	].setImeOptions(EditorInfo.IME_FLAG_NO_ENTER_ACTION);
			et[ET_URL		].setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_URI);
			et[ET_NUMERIC	].setInputType(InputType.TYPE_CLASS_NUMBER);
			et[ET_DECIMAL	].setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_SIGNED|InputType.TYPE_NUMBER_FLAG_DECIMAL);
			et[ET_PASSWORD	].setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);
			for(int k=0;k<ET_COUNT;++k)
				_parent.addView(et[k]);
		}
		public void init(int _color_theme, float _TextSize)
		{
			color_theme=_color_theme;
			textSize=_TextSize;
			for(int k=0;k<ET_COUNT;++k)
				setTheme(et[k],	color_theme, textSize);
		}
		@Override public void draw(Canvas canvas)
		{
		}
		@Override public void touch(TouchInfo ti)
		{
		}
	}



	public static final int//not counting the back button
		//root
		ST1_UNICODE=0, ST1_THEME=1, ST1_KB_TEST=2,
		ST1_P_TEXT=3, ST1_L_TEXT=4,
		ST1_P_NUMPAD=5, ST1_L_NUMPAD=6,
		ST1_P_PASSWORD=7, ST1_L_PASSWORD=8,
		ST1_COUNT_ROOT=9,

		//theme
		ST2_COLOR_BK=0,
		ST2_COLOR_BUTTON=1, ST2_COLOR_BUTTON_FAST=2, ST2_COLOR_BUTTON_PRESSED=3,
		ST2_COLOR_LABELS=4,
		ST2_COLOR_SHADOW=5, ST2_COLOR_SHADOW_LETTERS=6, ST2_COLOR_SHADOW_SPECIAL=7,
		ST2_COLOR_PREVIEW_TEXT=8,
		ST2_COUNT_THEME=9,

		//LAYOUTS
		//generic
		ST2_HEIGHT=0, ST2_NEW_LAYOUT=1,

		//portrait text
		ST2_LAYOUT_P_STD=2, ST2_LAYOUT_P_STD_SHIFT=3, ST2_LAYOUT_P_URL_STD=4, ST2_LAYOUT_P_URL_STD_SHIFT=5, ST2_LAYOUT_P_SYM=6, ST2_LAYOUT_P_SPK=7, ST2_LAYOUT_P_SPK_SHIFT=8, ST2_LAYOUT_P_FUN=9,
		ST2_COUNT_P_TEXT=10,

		//landscape text
		ST2_LAYOUT_L_STD=2, ST2_LAYOUT_L_STD_SHIFT=3, ST2_LAYOUT_L_SYM=4, ST2_LAYOUT_L_FUN=5,
		ST2_COUNT_L_TEXT=6,

		//portrait numpad
		ST2_LAYOUT_P_NUM=2,
		ST2_COUNT_P_NUMPAD=3,

		//landscape numpad
		ST2_LAYOUT_L_NUM=2,
		ST2_COUNT_L_NUMPAD=3,

		//portrait password
		ST2_LAYOUT_P_PASSWORD=2, ST2_LAYOUT_P_PASSWORD_SHIFT=3,
		ST2_COUNT_P_PASSWORD=4,

		//landscape password
		ST2_LAYOUT_L_PASSWORD=2, ST2_LAYOUT_L_PASSWORD_SHIFT=3,
		ST2_COUNT_L_PASSWORD=4;
	ArrayList<Integer> state=new ArrayList<>();
	//int state=STATE_ROOT;
	String string_back="<- Back";
	String[] strings_root=//TODO: breadth-first array
	{
		"Find Unicode characters",
		"Theme",
		"Keyboard test",
		"Portrait text",
		"Landscape text",
		"Portrait numpad",
		"Landscape numpad",
		"Portrait password",
		"Landscape password",
	};
	String[] strings_theme=
	{
		"color_bk",
		"color_button", "color_button_fast", "color_button_pressed",
		"color_labels",
		"color_shadow", "color_shadow_letters", "color_shadow_special",
		"color_preview_text",
	};
	String[] strings_test=
	{
		"Text",
		"URL",
		"Numeric",
		"Signed decimal",
		"Password",
	};
	String[] strings_sub=
	{
		"Height",
		"New layout",
	};
	String[] strings_p_text=
	{
		"layout_p_std",
		"layout_p_std_shift",
		"layout_p_url_std",
		"layout_p_url_std_shift",
		"layout_p_sym",
		"layout_p_spk",
		"layout_p_spk_shift",
		"layout_p_fun",
	};
	String[] strings_l_text=
	{
		"layout_l_std",
		"layout_l_std_shift",
		"layout_l_sym",
		"layout_l_fun",
	};
	String[] strings_p_numpad=
	{
		"layout_p_num",
	};
	String[] strings_l_numpad=
	{
		"layout_l_num",
	};
	String[] strings_p_password=
	{
		"layout_p_password",
		"layout_p_password_shift",
	};
	String[] strings_l_password=
	{
		"layout_l_password",
		"layout_l_password_shift",
	};

	int w, h,//screen dimensions
		px, py, dx, dy;//view position & dimensions
	int scroll_y,//view position = scroll_y + position in list,  scroll_y <= 0
		h_item, h_multiline, h_total;
	float textSize;
	public CKBactivity activity;
	EditText2 multiline, url, numeric, decimal, password;
	ColorPicker colorPicker;
	void init(Context context)
	{
		setId(ViewId.ROOT.ordinal());

		//test
		multiline	=new EditText2(context, this, ViewId.MULTILINE	.ordinal());
		url			=new EditText2(context, this, ViewId.URL		.ordinal());
		numeric		=new EditText2(context, this, ViewId.NUMERIC	.ordinal());
		decimal		=new EditText2(context, this, ViewId.DECIMAL	.ordinal());
		password	=new EditText2(context, this, ViewId.PASSWORD	.ordinal());

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
	public CKBSettings(Context context){super(context); init(context);}
	public CKBSettings(Context context, AttributeSet attrs){super(context, attrs); init(context);}
	public CKBSettings(Context context, AttributeSet attrs, int defStyle){super(context, attrs, defStyle); init(context);}

	static void setTheme(EditText et, int color_theme, float textSize)
	{
		et.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
		et.setTextColor(0xFF000000);

		ColorStateList csl=ColorStateList.valueOf(color_theme);//https://stackoverflow.com/questions/40838069/programmatically-changing-underline-color-of-edittext
		ViewCompat.setBackgroundTintList(et, csl);

		//setCursorColor(et, color_theme);
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
		//fastSpeed=Math.max(h/80, 1);
		//slowSpeed=Math.max(h/350, 1);
		Log.e(TAG, String.format("threshold=%d, fast=%d, slow=%d", fastThreshold, fastSpeed, slowSpeed));
		setTestVisibility(INVISIBLE);
		scroll_y=0;

		setBkColorRand();
	}

	boolean redraw=false;
	int fastThreshold, fastSpeed, slowSpeed;
	float friction=0.9f;//from 10px/f to 0 in 22 frames
	//float friction=0.97f;//from 10px/f to 0 in 76 frames
	//static int clamp(int lo, int x, int hi)
	//{
	//	if(x<lo)
	//		return lo;
	//	return Math.min(x, hi);
	//}
	boolean isStateTest(){return state.size()==1&&state.get(0)==ST1_KB_TEST;}
	boolean isStateTheme(){return state.size()==2&&state.get(0)==ST1_THEME;}
	void logState(String label)
	{
		StringBuilder str=new StringBuilder();
		int size=state.size();
		if(size==0)
			str.append("root");
		else
		{
			for(int k=0;k<size;++k)
			{
				if(k!=0)
					str.append('/');
				str.append(state.get(k));
			}
		}
		Log.e(TAG, String.format("%s, state: %s%s", label, str.toString(), isStateTheme()?" THEME":""));
	}
	void scroll()
	{
		if(multiline!=null)
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
			/*	if(scroll_y<0)
				{
					scroll_y+=(scroll_y<-100?4:2);
					if(scroll_y>0)
						scroll_y=0;
					redraw=true;
				}
				if(scroll_y>dx-h_total)
				{
					scroll_y-=(scroll_y+h_total>dx+100?4:2);
					if(scroll_y<dx-h_total)
						scroll_y=dx-h_total;
					redraw=true;
				}//*/
			}
			//logState("scroll()");//
			if(isStateTest())
			{
				int y=scroll_y+h_item*2;
				multiline	.layout(0, y, dx, y+h_multiline);	y+=h_multiline+h_item;
				url			.layout(0, y, dx, y+h_item);		y+=h_item*2;
				numeric		.layout(0, y, dx, y+h_item);		y+=h_item*2;
				decimal		.layout(0, y, dx, y+h_item);		y+=h_item*2;
				password	.layout(0, y, dx, y+h_item);		y+=h_item*2;
			}
			if(isStateTheme())
			{
			//	Log.e(TAG, String.format("(%d, %d)->(%d, %d)", 0, h_item, dx, dy));
				colorPicker.setVisibility(VISIBLE);
				colorPicker.layout(0, h_item, dx, dy);//
			}
			else
				colorPicker.setVisibility(INVISIBLE);
			invalidate();
		}
	}
	@Override protected void onLayout(boolean changed, int l, int t, int r, int b)//called on each text update
	{
		px=l; py=t; dx=r-l; dy=b-t;

		multiline.measure(dx, MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
		h_multiline=multiline.getMeasuredHeight();
		//h_multiline=h_item+(int)((multiline.getLineCount()-1)*textSize);//X text is shifted up
		//h_multiline=(int)(Math.max(multiline.getLineCount(), 1)*textSize);//X text is shifted up badly

		h_total=h_item+h_multiline+h_item*8;
		scroll();
	}
	Paint paint_text=new Paint(), paint_item=new Paint();
	RectF rectf=new RectF();
	void setTestVisibility(int visibility)
	{
		multiline	.setVisibility(visibility);
		url			.setVisibility(visibility);
		numeric		.setVisibility(visibility);
		decimal		.setVisibility(visibility);
		password	.setVisibility(visibility);
	}
	float drawLabel(float x1, float x2, float y1, float y2, float r, String label, Canvas canvas)
	{
		rectf.set(x1, y1, x2, y2);
		canvas.drawRect(rectf, paint_item);

		rectf.set(x1+r, y1+r, x2-r, y2-r);
		canvas.drawRoundRect(rectf, r, r, paint_item);

		canvas.drawText(label, x1+r+r, (y1+y2+textSize)*0.5f, paint_text);
		//canvas.drawText(label, x1+r, y2-r, paint_text);
		return y2;
	}
	float drawOptions(String[] labels, float yPos, float radius, Canvas canvas)
	{
		//drawLabel(0, dx, yPos, yPos+h_item, radius, string_back, canvas);
		//yPos+=h_item;
		for(String label:labels)
		{
			drawLabel(0, dx, yPos, yPos+h_item, radius, label, canvas);
			yPos+=h_item;
		}
		return yPos;
	}
	float drawTestLabels(String[] labels, float[] heights, float yPos, float separation, float radius, Canvas canvas)
	{
		for(int ky=0;ky<labels.length;++ky)
		{
			drawLabel(0, dx, yPos, yPos+h_item, radius, labels[ky], canvas);
			yPos+=heights[ky]+separation;
		}
		return yPos;
	}
/*	int drawFolders(String[] labels, int yPos, float radius, Canvas canvas)
	{
		for(int ky=0;ky<labels.length;++ky)
		{
			rectf.set(0, yPos, dx, yPos+h_item);
			canvas.drawRect(rectf, paint_item);
			rectf.set(radius, yPos+radius, dx-radius, yPos+h_item-radius);
			canvas.drawRoundRect(rectf, radius, radius, paint_item);

			canvas.drawText(labels[ky], 0, yPos+h_item, paint_text);

			yPos+=(ky==0?h_multiline:h_item)+h_item;
		}
		return yPos;
	}//*/
	@Override public void onDraw(Canvas canvas)
	{
		//super.onDraw(canvas);//does nothing
		float radius=20;

		int size=state.size();
		float yPos=drawLabel(0, dx, scroll_y, scroll_y+h_item, radius, string_back, canvas);
		if(size==0)
			drawOptions(strings_root, yPos, radius, canvas);
		else if(size==1)
		{
			switch(state.get(0))//level 1
			{
			case ST1_UNICODE:
				break;
			case ST1_THEME:
				drawOptions(strings_theme, yPos, radius, canvas);
				break;
			case ST1_KB_TEST:
				float[] heights={h_multiline, h_item, h_item, h_item, h_item};
				drawTestLabels(strings_test, heights, yPos, h_item, radius, canvas);
				break;
			case ST1_P_TEXT:
				yPos=drawOptions(strings_sub, yPos, radius, canvas);
				drawOptions(strings_p_text, yPos, radius, canvas);
				break;
			case ST1_L_TEXT:
				yPos=drawOptions(strings_sub, yPos, radius, canvas);
				drawOptions(strings_l_text, yPos, radius, canvas);
				break;
			case ST1_P_NUMPAD:
				yPos=drawOptions(strings_sub, yPos, radius, canvas);
				drawOptions(strings_p_numpad, yPos, radius, canvas);
				break;
			case ST1_L_NUMPAD:
				yPos=drawOptions(strings_sub, yPos, radius, canvas);
				drawOptions(strings_l_numpad, yPos, radius, canvas);
				break;
			case ST1_P_PASSWORD:
				yPos=drawOptions(strings_sub, yPos, radius, canvas);
				drawOptions(strings_p_password, yPos, radius, canvas);
				break;
			case ST1_L_PASSWORD:
				yPos=drawOptions(strings_sub, yPos, radius, canvas);
				drawOptions(strings_l_password, yPos, radius, canvas);
				break;
			}
		}
		else if(size==2)
		{
			switch(state.get(0))//level 2
			{
			case ST1_UNICODE://unreachable
				break;
			case ST1_THEME://draw color picker
			/*	switch(state.get(1))
				{
				case ST_COLOR_BK:
				case ST_COLOR_BUTTON:
				case ST_COLOR_BUTTON_FAST:
				case ST_COLOR_BUTTON_PRESSED:
				case ST_COLOR_LABELS:
				case ST_COLOR_SHADOW:
				case ST_COLOR_SHADOW_LETTERS:
				case ST_COLOR_SHADOW_SPECIAL:
				case ST_COLOR_PREVIEW_TEXT:
					break;
				}//*/
				break;
			case ST1_KB_TEST://unreachable
				break;
			case ST1_P_TEXT:
				switch(state.get(1))
				{
				case ST2_HEIGHT:
				case ST2_NEW_LAYOUT:
				case ST2_LAYOUT_P_STD:
				case ST2_LAYOUT_P_STD_SHIFT:
				case ST2_LAYOUT_P_URL_STD:
				case ST2_LAYOUT_P_URL_STD_SHIFT:
				case ST2_LAYOUT_P_SYM:
				case ST2_LAYOUT_P_SPK:
				case ST2_LAYOUT_P_SPK_SHIFT:
				case ST2_LAYOUT_P_FUN:
					break;
				}
				break;
			case ST1_L_TEXT:
				switch(state.get(1))
				{
				case ST2_HEIGHT:
				case ST2_NEW_LAYOUT:
				case ST2_LAYOUT_L_STD:
				case ST2_LAYOUT_L_STD_SHIFT:
				case ST2_LAYOUT_L_SYM:
				case ST2_LAYOUT_L_FUN:
					break;
				}
				break;
			case ST1_P_NUMPAD:
				switch(state.get(1))
				{
				case ST2_HEIGHT:
				case ST2_NEW_LAYOUT:
				case ST2_LAYOUT_P_NUM:
					break;
				}
				break;
			case ST1_L_NUMPAD:
				switch(state.get(1))
				{
				case ST2_HEIGHT:
				case ST2_NEW_LAYOUT:
				case ST2_LAYOUT_L_NUM:
					break;
				}
				break;
			case ST1_P_PASSWORD:
				switch(state.get(1))
				{
				case ST2_HEIGHT:
				case ST2_NEW_LAYOUT:
				case ST2_LAYOUT_P_PASSWORD:
				case ST2_LAYOUT_P_PASSWORD_SHIFT:
					break;
				}
				break;
			case ST1_L_PASSWORD:
				switch(state.get(1))
				{
				case ST2_HEIGHT:
				case ST2_NEW_LAYOUT:
				case ST2_LAYOUT_L_PASSWORD:
				case ST2_LAYOUT_L_PASSWORD_SHIFT:
					break;
				}
				break;
			}
		}
	/*	if(state==STATE_KB_TEST)
		{
			for(int ky=0, yPos=scroll_y;ky<strings_test.length;++ky)
			{
				rectf.set(0, yPos, dx, yPos+h_item);
				canvas.drawRect(rectf, paint_item);
				rectf.set(radius, yPos+radius, dx-radius, yPos+h_item-radius);
				canvas.drawRoundRect(rectf, radius, radius, paint_item);

				canvas.drawText(strings_test[ky], 0, yPos+h_item, paint_text);

				yPos+=(ky==0?h_multiline:h_item)+h_item;
			}
		}
		else
		{
			String[] labels={"Unreachable"}, labels2=null;
			switch(state)
			{
			case STATE_ROOT:labels=strings_root;break;
			case STATE_THEME:labels=strings_theme;break;

			case STATE_P_TEXT:labels=strings_sub; labels2=strings_p_text;break;
			case STATE_L_TEXT:labels=strings_sub; labels2=strings_l_text;break;
			case STATE_P_NUMPAD:labels=strings_sub; labels2=strings_p_numpad;break;
			case STATE_L_NUMPAD:labels=strings_sub; labels2=strings_l_numpad;break;
			case STATE_P_PASSWORD:labels=strings_sub; labels2=strings_p_password;break;
			case STATE_L_PASSWORD:labels=strings_sub; labels2=strings_l_password;break;
			}
			int yPos=scroll_y;
			yPos=drawFolders(labels, yPos, radius, canvas);
			if(labels2!=null)
				drawFolders(labels2, yPos, radius, canvas);
		}//*/
		if(redraw)
		{
			redraw=false;
			scroll();
		}
	}
	@Override public boolean performClick()
	{
		super.performClick();
		return true;
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
	TouchInfo touchInfo=new TouchInfo();
	float scrollThreshold=20;//px
	boolean scrolling=false;
	float scrollSpeed=0;
	void changeTitle(String[] strings, int idx)
	{
		String broken="What.";
		if(idx>=0&&idx<strings.length)
			activity.setTitle(strings[idx]);
		else
			activity.setTitle(broken);
	}
	void changeTitle(String[] s1, String[] s2, int idx)
	{
		String broken="What.";
		if(idx>=0&&idx<s1.length)
			activity.setTitle(s1[idx]);
		else
		{
			idx-=s1.length;
			if(idx>=0&&idx<s2.length)
				activity.setTitle(s2[idx]);
			else
				activity.setTitle(broken);
		}
	}
	void changeTitle()
	{
		if(state.size()==0)
			activity.setTitle("CKB Settings");
		else if(state.size()==1)
			changeTitle(strings_root, state.get(0));
		else
		{
			switch(state.get(0))
			{
			case ST1_UNICODE:
				break;
			case ST1_THEME:
				changeTitle(strings_theme, state.get(1));
				break;
			case ST1_KB_TEST:
				break;
			case ST1_P_TEXT:
				changeTitle(strings_sub, strings_p_text, state.get(1));
				break;
			case ST1_L_TEXT:
				changeTitle(strings_sub, strings_l_text, state.get(1));
				break;
			case ST1_P_NUMPAD:
				changeTitle(strings_sub, strings_p_numpad, state.get(1));
				break;
			case ST1_L_NUMPAD:
				changeTitle(strings_sub, strings_l_numpad, state.get(1));
				break;
			case ST1_P_PASSWORD:
				changeTitle(strings_sub, strings_p_password, state.get(1));
				break;
			case ST1_L_PASSWORD:
				changeTitle(strings_sub, strings_l_password, state.get(1));
				break;
			}
		}
	}
	void enterChoice(int choice, int nChoices)
	{
		if(choice>=0&&choice<nChoices)
		{
			state.add(choice);
			changeTitle();
		}
	}
	public boolean touch(MotionEvent e, int viewId)
	{
		if(viewId>ViewId.ROOT.ordinal())
			return scrolling;//if scrolling then handled in root view
		touchInfo.update(e);
		//touchInfo.log("CKBSettings");//
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
				int c0		=(int)((p.starty-scroll_y)/h_item)-1,
					choice	=(int)((p.y		-scroll_y)/h_item)-1;
				if(c0==choice)
				{
					int state_size=state.size();
					if(choice==-1)//back: always the top option
					{
						if(state_size>0)
						{
							if(state.size()==1&&state.get(0)==ST1_KB_TEST)
								setTestVisibility(INVISIBLE);
							state.remove(state_size-1);
							changeTitle();
						}
						else//quit
						{
							activity.finish();
							return true;
						}
					}
					else if(state_size==0)
					{
						enterChoice(choice, ST1_COUNT_ROOT);
						if(choice==ST1_KB_TEST)
							setTestVisibility(VISIBLE);
					}
					else if(state_size==1)
					{
						switch(state.get(0))
						{
						case ST1_UNICODE:		break;
						case ST1_THEME:			enterChoice(choice, ST2_COUNT_THEME);break;
						case ST1_KB_TEST:		break;
						case ST1_P_TEXT:		enterChoice(choice, ST2_COUNT_P_TEXT);break;
						case ST1_L_TEXT:		enterChoice(choice, ST2_COUNT_L_TEXT);break;
						case ST1_P_NUMPAD:		enterChoice(choice, ST2_COUNT_P_NUMPAD);break;
						case ST1_L_NUMPAD:		enterChoice(choice, ST2_COUNT_L_NUMPAD);break;
						case ST1_P_PASSWORD:	enterChoice(choice, ST2_COUNT_P_PASSWORD);break;
						case ST1_L_PASSWORD:	enterChoice(choice, ST2_COUNT_L_PASSWORD);break;
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