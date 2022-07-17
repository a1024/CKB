/*  CKBview3.java - The 3rd version of the customizable keyboard main view
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
package com.example.customkb;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.InputType;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.core.graphics.drawable.DrawableCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class CKBview3 extends ViewGroup
{
	//ModKey - keys not present in unicode
	public static final int
		MODMASK=0x80000000,

		//these constants correspond to ckb_mKeys.h
		//make sure that the labels are consequent when modifying this list
		KEY_NAB=0,//not a button

		KEY_SYMBOLS=1,
		KEY_SETTINGS=2,

		KEY_HOME=3,
		KEY_END=4,
		KEY_PGUP=5,
		KEY_PGDN=6,

		KEY_SHIFT=7,
		KEY_CTRL=8,
		KEY_ALT=9,

		KEY_LEFT=10,
		KEY_RIGHT=11,
		KEY_UP=12,
		KEY_DOWN=13,

		KEY_INSERT=14,

		KEY_CAPSLOCK=15,
		KEY_NUMLOCK=16,
		KEY_SCROLLLOCK=17,

		KEY_PRINTSCREEN=18,
		KEY_PAUSE=19,

		KEY_F1=20,
		KEY_F2=21,
		KEY_F3=22,
		KEY_F4=23,
		KEY_F5=24,
		KEY_F6=25,
		KEY_F7=26,
		KEY_F8=27,
		KEY_F9=28,
		KEY_F10=29,
		KEY_F11=30,
		KEY_F12=31,

		KEY_CUT=32,
		KEY_COPY=33,
		KEY_PASTE=34,
		KEY_CLIPBOARD=35,
		KEY_SELECTALL=36,

		KEY_MENU=37,
		KEY_UNICODE=38;
	public static class ModKey
	{
		final int code;//code here is UNUSED (idx of ModKey)
		final String label;
		ModKey(int _code, String _label)
		{
			code=MODMASK|_code;
			label=_label;
		}
	}
	public static final ModKey[] mKeys=new ModKey[]
	{
		new ModKey(KEY_NAB, ""),

		new ModKey(KEY_SYMBOLS, "Symbols"),
		new ModKey(KEY_SETTINGS, "Settings"),

		new ModKey(KEY_HOME, "Home"),
		new ModKey(KEY_END, "End"),
		new ModKey(KEY_PGUP, "PgUp"),
		new ModKey(KEY_PGDN, "PgDn"),

		new ModKey(KEY_SHIFT, "Shift"),
		new ModKey(KEY_CTRL, "Ctrl"),
		new ModKey(KEY_ALT, "Alt"),

		new ModKey(KEY_LEFT, "Left"),
		new ModKey(KEY_RIGHT, "Right"),
		new ModKey(KEY_UP, "Up"),
		new ModKey(KEY_DOWN, "Down"),

		new ModKey(KEY_INSERT, "Ins"),

		new ModKey(KEY_CAPSLOCK, "Caps\nLock"),
		new ModKey(KEY_NUMLOCK, "Num\nLock"),
		new ModKey(KEY_SCROLLLOCK, "Scroll\nLock"),

		new ModKey(KEY_PRINTSCREEN, "Print\nScreen"),
		new ModKey(KEY_PAUSE, "Pause"),

		new ModKey(KEY_F1, "F1"),
		new ModKey(KEY_F2, "F2"),
		new ModKey(KEY_F3, "F3"),
		new ModKey(KEY_F4, "F4"),
		new ModKey(KEY_F5, "F5"),
		new ModKey(KEY_F6, "F6"),
		new ModKey(KEY_F7, "F7"),
		new ModKey(KEY_F8, "F8"),
		new ModKey(KEY_F9, "F9"),
		new ModKey(KEY_F10, "F10"),
		new ModKey(KEY_F11, "F11"),
		new ModKey(KEY_F12, "F12"),

		new ModKey(KEY_CUT, "Cut"),
		new ModKey(KEY_COPY, "Copy"),
		new ModKey(KEY_PASTE, "Paste"),
		new ModKey(KEY_CLIPBOARD, "Clipboard"),
		new ModKey(KEY_SELECTALL, "Select\nAll"),

		new ModKey(KEY_MENU, "Menu"),
		new ModKey(KEY_UNICODE, "Unicode\nSearch"),
	};

	float backup_height=0.3f;
	int[][] backup_layout_port=
	{
		{'1', 1, '2', 1, '3', 1, '4', 1, '5', 1, '6', 1, '7', 1, '8', 1, '9', 1, '0', 1},
		{'q', 1, 'w', 1, 'e', 1, 'r', 1, 't', 1, 'y', 1, 'u', 1, 'i', 1, 'o', 1, 'p', 1},
		{MODMASK|KEY_NAB, 1, 'a', 2, 's', 2, 'd', 2, 'f', 2, 'g', 2, 'h', 2, 'j', 2, 'k', 2, 'l', 2, MODMASK|KEY_NAB, 1},
		{MODMASK|KEY_SHIFT, 3, 'z', 2, 'x', 2, 'c', 2, 'v', 2, 'b', 2, 'n', 2, 'm', 2, '\b', 3},
	//	{MODMASK|KEY_SETTINGS, 6, MODMASK|KEY_NAB, 4, ' ', 20, MODMASK|KEY_NAB, 4, '\n', 6},
		{MODMASK|KEY_NAB, 6, MODMASK|KEY_NAB, 4, ' ', 20, MODMASK|KEY_NAB, 4, '\n', 6},
	//	{MODMASK|KEY_SETTINGS, 6, MODMASK|KEY_COPY, 4, MODMASK|KEY_PASTE, 4, ' ', 12, ',', 4, '.', 4, '\n', 6},
	};
	int[][] backup_layout_land_left=
	{
		{'1', 12, '2', 12, '3', 12, '4', 12, '5', 12},
		{'q', 12, 'w', 12, 'e', 12, 'r', 12, 't', 12},
		{MODMASK|KEY_NAB, 4, 'a', 12, 's', 12, 'd', 12, 'f', 12, 'g', 12, MODMASK|KEY_NAB, 8},
		{MODMASK|KEY_SETTINGS, 9, 'z', 12, 'x', 12, 'c', 12, 'v', 12, 'b', 12, MODMASK|KEY_NAB, 3},
	};
	int[][] backup_layout_land_right=
	{
		{'6', 12, '7', 12, '8', 12, '9', 12, '0', 12},
		{'y', 12, 'u', 12, 'i', 12, 'o', 12, 'p', 12},
		{MODMASK|KEY_NAB, 4, 'h', 12, 'j', 12, 'k', 12, 'l', 12, MODMASK|KEY_NAB, 8},
		{MODMASK|KEY_NAB, 9, 'n', 12, 'm', 12, MODMASK|KEY_NAB, 3, '\b', 12, '\n', 12},
	};

	//dimensions
	public static int
		w, h,//screen dimensions
		kb_h;//keyboard height

	//customkb
	public static final int//consistent with enum ModeType in ckb.h
		MODE_TEXT=0, MODE_PASSWORD=1, MODE_URL=2, MODE_EMAIL=3,//text modes
		MODE_NUMBER=4, MODE_PHONE_NUMBER=5, MODE_NUMERIC_PASSWORD=6;//numeric modes
	public int mode;
	public static final int
		CAP_NONE		=0,
		CAP_WORDS		=1,
		CAP_SENTENCES	=2,//unused
		CAP_ALL			=3;
	public int capital=CAP_NONE;//text modes	UNUSED
	public boolean multiline=false;//text modes
	public boolean
		numeric_signed=false,//extra buttons: + -
		numeric_decimal=false;//extra buttons: . /
	public String layoutName;

	//unicode
	public static final boolean
		ENABLE_UNICODE_SEARCH=true;
	public boolean mode_unicode=false;

	//unicode search
	boolean bypass=false;//fixes bug: when switching back from unicode search, pointer.up should be ignored
	static class SearchResult
	{
		int code;
		String name, displayString;
	}
	StringBuilder uniSearch_query=new StringBuilder();
	float uniSearch_wpy=0;//window (scroll) position y, set by unicodeSearch & onTouch.scroll
	ArrayList<SearchResult> uniSearch_results=new ArrayList<>();
	public int//hit-test params set by o n D r a w()
		//(portrait) vertical: search_results |y1| search_query |y2| QWERTY
		UNI_SEARCH_PORT_Y1=0,
		UNI_SEARCH_PORT_Y2=0,

		//(landscape) horizontal: QWERTY_LEFT |x1| middle_vertical |x2| QWERTY_RIGHT
		//middle_vertical:		search_query |y1| search_results
		UNI_SEARCH_LAND_X1=0,
		UNI_SEARCH_LAND_X2=0,
		UNI_SEARCH_LAND_Y1=0,

		//unicode search keyboard total height in rows
		UNI_SEARCH_N_ROWS_TOTAL=0;
	float UNI_SEARCH_ROW_HEIGHT=0;
	public static final int
		UNI_SEARCH_DRAG_NONE=0,
		UNI_SEARCH_DRAG_SCROLL=1;
		//UNI_SEARCH_DRAG_CC=2;
	int uniSearch_drag=UNI_SEARCH_DRAG_NONE;

	//cursor control:
	//{(MSB) switch_language, start, end, shift (LSB)}
	//0000	CC_DISABLED
	//0001	CC_SEL_READY
	//0011	CC_SEL_END
	//0101	CC_SEL_START
	//0111	CC_SEL_BOTH
	//0110	CC_CURSOR
	//1000	CC_LANG
	public static final int
		CC_DISABLED=0,
		CC_SEL_READY=1,		//touchId_shift
		CC_SEL_END=3,		//[touchId_shift,] touchId_end
		CC_SEL_END_MASK=2,
		CC_SEL_START=5,		//[touchId_shift,] touchId_start
		CC_SEL_START_MASK=4,
		CC_SEL_BOTH=7,		//[touchId_shift,] touchId_start & touchId_end
		CC_CURSOR=6,		//touchId_start
		CC_LANG=8;
	public int cursor_control=CC_DISABLED;
	public boolean isHeld_shift=false;//finger holds shift, needed to initiate selection control
	int touchId_shift=-1,
		touchId_selStart=-1, touchId_selEnd=-1,
		touchId_switchLang=-1;

	//graphics
	public static final int
		COLOR_BACKGROUND=0,
		COLOR_BUTTON_IDLE=1,
		COLOR_BUTTON_FAST=2,
		COLOR_BUTTON_PRESSED=3,
		COLOR_LABELS=4,
		COLOR_SHADOW_MODIFIER=5,
		COLOR_SHADOW_LETTER=6,
		COLOR_SHADOW_NON_LETTER=7,
		COLOR_PREVIEW_POPUP=8,
		THEME_COLOR_COUNT=9;
	public int[] theme_colors=null;//0xAARRGGBB
	int[] backup_theme=
	{
		0x00202020,//background
		0xDC707070,//button_idle
		0xDC237CE9,//button_fast
		0xDCFF8040,//button_pressed
		0xFFFFFFFF,//labels
		0xC000FF00,//shadow_modifier
		0xC08600FF,//shadow_letter
		0xC00000FF,//shadow_non_letter
		0xC0000000,//preview_popup
	};

	float textSize=32, textSize_long=20, shadowOffset=4;
	Paint textPaint, letterPaint, specialButtonPaint, textPaintDebug, penPaint, buttonPaint;
	TextPaint wrapPaint;
	TextView preview;
	PopupWindow previewPopup;
	final RectF rectf=new RectF();
	final Rect rect=new Rect();
	final TouchInfo touchInfo=new TouchInfo();

	//keyboard
	public static class ButtonIdx
	{
		int ky, kx, code,
			x1, x2, y1, y2;//button bounds
		ButtonIdx(){kx=-1; ky=-1; code=MODMASK|KEY_NAB;}
		boolean invalid(){return kx==-1||ky==-1;}
	}
	public static final int
		INFO_IDX_SELECTED_IDX=0,
		INFO_IDX_LAYOUT_COUNT=1;
	int[] layoutInfo=null;
	public static final int
		LAYOUT_IDX_ROW_COUNT=0,
		LAYOUT_IDX_HEIGHT_PX=1;
	int[] layout=null;//see CKBnativelib.java
	ArrayList<String> layoutNames=new ArrayList<>();
	boolean layoutHasExtension=false;//symbols extension		should be on at the start in case mode == MODE_PASSWORD, MODE_URL, or MODE_EMAIL
	int tap_x=-1, tap_y=-1;//layout coordinates of currently held button
	int ccGridX, ccGridY;
	int quickMode=0;
	boolean isActive_ctrl, isActive_alt, isActive_shift;
	//boolean isActive_layout;
	CKBservice service;

	//timing
	final int
		turboStart_ms=800,//600
		turbo_ms=35,
		longPress_ms=400;
	Timer timer=new Timer();
	int timerOn=0;//0: off, 1: turboTask, 2: longPressTask
	int touchId_timer;
	//public int pend_switch=0;//X  only shift has long press action		1: prev. l a y o u t, 2: next l a y o u t			//3: toggle unicode mode
	class TurboTask extends TimerTask
	{
		public int code;
		TurboTask(int _code){code=_code;}
		@Override public void run(){service.onKeyCallback(code, 3);}//this is run every time turbo is triggered
	}
	class LongPressTask extends TimerTask
	{
		public int code;
		public boolean done=false;
		LongPressTask(int _code){code=_code;}
		@Override public void run()//this is run when the period is expired
		{
			if(code==(MODMASK|KEY_SHIFT))
			{
				service.onKeyCallback(code, 1);
				isActive_shift=true;
				service.onKeyCallback(MODMASK|KEY_SHIFT, 1);
				quickMode=0;
			}
			done=true;
		}
	}
	TurboTask turboTask=new TurboTask(MODMASK|KEY_NAB);//TODO: check if initialization needed (probably not)
	LongPressTask longPressTask=new LongPressTask(MODMASK|KEY_NAB);

	//debug tools
	public static final String TAG="customkb";
	public static final boolean
		DEBUG_ACCUMULATE=true,//was true
		DEBUG_TOUCH		=false,//works
		DEBUG_CC		=false,//CC_DISABLED
		DEBUG_STATE		=false,//just shift
		DEBUG_MODE		=false,
		DEBUG_COLORS	=false,
		DEBUG_UNICODE	=false;
	public static final ArrayList<String> urgentMsg=new ArrayList<>();//when logcat isn't enough
	public static final Locale loc=new Locale("US");
	int frame_counter=0;


	//standard
	public CKBview3(Context context){super(context);}
	public CKBview3(Context context, AttributeSet attrs){super(context, attrs);}
	public CKBview3(Context context, AttributeSet attrs, int defStyle){super(context, attrs, defStyle);}
	@Override protected void onLayout(boolean changed, int l, int t, int r, int b){}

	//util
	static boolean mask(int x, int m){return (x&m)==m;}
	static int clamp(int lo, int x, int hi)
	{
		if(x<lo)
			x=lo;
		if(x>hi)
			x=hi;
		return x;
	}
	static int ceilMultiple(int x, int n)
	{
		if(x<1)
			x=1;
		int q=n/x, r=n%x;
		q+=r!=0?1:0;
		return n/q;
	}
	public static void addError(Exception e)
	{
		if(DEBUG_ACCUMULATE)
		{
		//	urgentMsg.clear();
			urgentMsg.add(e.getMessage());
		}
		Log.e(TAG, "exception", e);
	}
	public static void addError(String str)
	{
		if(DEBUG_ACCUMULATE)
			urgentMsg.add(str);
		Log.e(TAG, str);
	}
	public String getLabel(int code)
	{
		if((code&MODMASK)!=0)
		{
			code&=~MODMASK;
			if(code>=mKeys.length)
				return "INVALID";
			ModKey key=mKeys[code];
			return key.label;
		}
		switch(code)//https://www.ascii-code.com/
		{
		case '\0':	return "\\0";
		case 1:		return "Start of\nHeading";
		case 2:		return "Start of\nText";
		case 3:		return "End of\nText";
		case 4:		return "End of\nTx";
		case 5:		return "Enquiry";
		case 6:		return "Ack";
		case 7:		return "Bell";
		case '\b':	return "Backspace";
		case '\t':	return "Tab";
		case '\n':	return "Enter";
		case 11:	return "Vertical\nTab";
		case 12:	return "Form\nFeed";
		case '\r':	return "Carriage\nReturn";
		case 14:	return "Shift\nOut";
		case 15:	return "Shift\nIn";
		case 16:	return "Data Line\nEscape";
		case 17:	return "Device\nControl 1";
		case 18:	return "Device\nControl 2";
		case 19:	return "Device\nControl 3";
		case 20:	return "Device\nControl 4";
		case 21:	return "Neg.\nAck.";
		case 22:	return "Sync.\nIdle";
		case 23:	return "End Tx\nBlock";
		case 24:	return "Cancel";
		case 25:	return "End of\nMedium";
		case 26:	return "Substitute";
		case 27:	return "Esc";
		case 28:	return "File\nSeparator";
		case 29:	return "Group\nSeparator";
		case 30:	return "Record\nSeparator";
		case 31:	return "Unit\nSeparator";
		case ' ':	return layoutName;
		case 127:	return "Del";
		}
		StringBuilder b=new StringBuilder();//don't use String.format() with unicode
		b.appendCodePoint(code);
		return b.toString();
	}
	public ButtonIdx getButton_layout(int tap_x, int tap_y)
	{
		ButtonIdx button=new ButtonIdx();
		if(layout!=null&&tap_y>=0&&tap_y<layout[LAYOUT_IDX_ROW_COUNT])
		{
			int rowStartIdx=layout[2+tap_y], nextRowStartIdx=layout[tap_y+3];
			int nButtons=(nextRowStartIdx-rowStartIdx-1)/2;
			if(tap_x>=0&&tap_y<nButtons)
			{
				int idx=rowStartIdx+tap_x*2;
				button.kx=tap_x;
				button.ky=tap_y;
				button.y1=tap_y*layout[LAYOUT_IDX_HEIGHT_PX]/layout[LAYOUT_IDX_ROW_COUNT];
				button.y2=(tap_y+1)*layout[LAYOUT_IDX_HEIGHT_PX]/layout[LAYOUT_IDX_ROW_COUNT];
				button.x1=layout[idx];
				button.code=layout[idx+1];
				button.x2=layout[idx+2];
			}
		}
		return button;
	}
	public ButtonIdx getButton_px(float x, float y)//linear search,  assumes layout is fullscreen {0~w, 0~kb_h}
	{
		ButtonIdx button=new ButtonIdx();
		if(y>=0&&y<kb_h)
		{
			int ky=(int)(y*layout[LAYOUT_IDX_ROW_COUNT]/layout[LAYOUT_IDX_HEIGHT_PX]);
			//addError(String.format(loc, "getButton_px 1: nRows=%d, (%f, %f), ky=%f", layout[LAYOUT_IDX_ROW_COUNT], x, y, y*layout[LAYOUT_IDX_ROW_COUNT]/layout[LAYOUT_IDX_HEIGHT_PX]));//DEBUG
			if(ky>=0&&ky<layout[LAYOUT_IDX_ROW_COUNT])
			{
				int rowStartIdx=layout[2+ky], nextRowStartIdx=layout[ky+3];
				int nButtons=(nextRowStartIdx-rowStartIdx-1)/2;
				//addError(String.format(loc, "getButton_px 2: ky=%d, nButtons=%d", ky, nButtons));//DEBUG
				for(int kx=0;kx<nButtons;++kx)
				{
					int idx=rowStartIdx+kx*2;
					//addError(String.format(loc, "getButton_px 3: testing [%d]= %08X %d~%d", idx, layout[idx+1], layout[idx], layout[idx+2]));//DEBUG
					if(x>=layout[idx]&&x<layout[idx+2])
					{
						button.ky=ky;
						button.kx=kx;
						button.x1=layout[idx];
						button.code=layout[idx+1];
						button.x2=layout[idx+2];
						button.y1=(int)(ky*layout[LAYOUT_IDX_HEIGHT_PX]/layout[LAYOUT_IDX_ROW_COUNT]);
						button.y2=(int)((ky+1)*layout[LAYOUT_IDX_HEIGHT_PX]/layout[LAYOUT_IDX_ROW_COUNT]);
						break;
					}
				}
			}
		}
		return button;
	}
	public ButtonIdx getButton_px_static(int[][] layout, int x1, int x2, int y1, int y2, float x, float y)
	{
		ButtonIdx button=new ButtonIdx();
		if(y>=y1&&y<y2&&x>=x1&&x<x2)
		{
			int dx=x2-x1, dy=y2-y1;
			int ky=(int)((y-y1)*layout.length/dy);
			int[] row=layout[ky];
			int relativeWidth=0;
			for(int kx=0;kx<row.length;kx+=2)
				relativeWidth+=row[kx+1];
			int xa1=0, xa2=x1;
			for(int kx=0;kx<row.length;kx+=2)
			{
				int xb1=xa1+row[kx+1];
				int xb2=x1+xb1*dx/relativeWidth;
				if(x>=xa2&&x<xb2)
				{
					button.ky=ky;
					button.kx=kx/2;
					button.code=row[kx];
					button.x1=xa2;
					button.x2=xb2;
					button.y1=y1+ky*dy/layout.length;
					button.y2=y1+(ky+1)*dy/layout.length;
					break;
				}
				xa1=xb1;
				xa2=xb2;
			}
		}
		return button;
	}

	//implementation
	private void set_kb_height(int height)
	{
		kb_h=height;
		LayoutParams lp=getLayoutParams();
		if(lp==null)
		{
			Log.e(TAG, "CKBview3.LayoutParams == null");
			lp=new LayoutParams(w, height);
		}
		else
		{
			lp.width=w;
			lp.height=height;
		}
		setLayoutParams(lp);
	}
	private void switch_layout(int layoutIdx, boolean hasExtension)
	{
		int[] nextLayout=CKBnativelib.getLayout(layoutIdx, hasExtension);
		if(nextLayout==null)
		{
			addError(String.format(loc, "Failed to switch layout to idx=%d%s", layoutIdx, hasExtension?" with extension":""));
			return;
		}
		layoutInfo[INFO_IDX_SELECTED_IDX]=layoutIdx;
		layout=nextLayout;
		kb_h=layout[LAYOUT_IDX_HEIGHT_PX];

		//for(int k=0;k<layout.length;++k)//FIXME DEBUG
		//	addError(String.format(loc, "[%d]=%d", k, layout[k]));

		set_kb_height(kb_h);
		switch(layoutIdx)
		{
		case -3:
			layoutName="Symbols";
			break;
		case -2:
			layoutName="DecNumPad";
			break;
		case -1:
			layoutName="NumPad";
			break;
		default:
			if(layoutIdx>=0&&layoutIdx<layoutInfo[INFO_IDX_LAYOUT_COUNT])
				layoutName=layoutNames.get(layoutIdx);
			else
			{
				layoutName=null;
				addError(String.format(loc, "Layout index is out of bounds: idx=%d, nLayouts=%d", layoutIdx, layoutInfo[INFO_IDX_LAYOUT_COUNT]));
			}
			break;
		}
	}
	public void toggleSymbolsExtension()
	{
		layoutHasExtension=!layoutHasExtension;
		switch_layout(layoutInfo[INFO_IDX_SELECTED_IDX], layoutHasExtension);
	}
	private void get_layout_backup()
	{
		int nRows=backup_layout_port.length;
		int nButtonsTotal=0;
		for(int[] ints: backup_layout_port)
			nButtonsTotal+=ints.length/2;

		layoutInfo=new int[2];
		//layoutInfo[INFO_IDX_SELECTED_IDX]=0;
		layoutInfo[INFO_IDX_LAYOUT_COUNT]=1;

		int arrCount=3+nRows*2+nButtonsTotal*2;
		layout=new int[arrCount];
		layout[LAYOUT_IDX_ROW_COUNT]=nRows;
		layout[LAYOUT_IDX_HEIGHT_PX]=(int)(backup_height*h);
		layout[2+nRows]=arrCount;
		int rowStartIdx=0;
		for(int kr=0;kr<nRows;++kr)
		{
			layout[2+kr]=rowStartIdx;
			int[] row=backup_layout_port[kr];
			int nButtons=row.length/2;
			int relativeWidth=0;
			for(int kb=0;kb<nButtons;++kb)
				relativeWidth+=row[kb*2+1];
			int x=0;
			layout[rowStartIdx]=0;//first x1
			for(int kb=0;kb<nButtons;++kb)
			{
			//	layout[rowStartIdx+kb*2]=x*w/relativeWidth;		//x1
				layout[rowStartIdx+kb*2+1]=row[kb*2];			//code
				x+=row[kb*2+1];
				layout[rowStartIdx+kb*2+2]=x*w/relativeWidth;	//x2
			}
		}
	}
	public void toggleUnicode()
	{
		if(ENABLE_UNICODE_SEARCH)
			mode_unicode=!mode_unicode;
		else
			return;
		if(mode_unicode)//turn on unicode
		{
			uniSearch_setRowHeight();
			uniSearch_updateResults();
		}
		else if(layout!=null)//turn off unicode
			set_kb_height(layout[LAYOUT_IDX_HEIGHT_PX]);
	}
	public boolean uniSearch_isScrollable()
	{
		if(w<h)//portrait
			return uniSearch_results.size()>backup_layout_port.length-1;
		//landscape
		return uniSearch_results.size()>backup_layout_land_left.length-2;
	}
	public void initCKB(CKBservice _service)//called in CKBservice.onCreateInputView
	{
		try
		{
			if(_service==null)
				throw new Exception("CKBservice == null");

			service=_service;

			DisplayMetrics metrics=Resources.getSystem().getDisplayMetrics();
			w=metrics.widthPixels; h=metrics.heightPixels;

			int minDim=Math.min(w, h);
			textSize=minDim/18.f;//33.75f	20.f
			textSize_long=minDim/40.f;

			textPaint=new Paint();
			textPaint.setAntiAlias(true);
			textPaint.setTextAlign(Paint.Align.CENTER);
			letterPaint=new Paint(textPaint);
			specialButtonPaint=new Paint(textPaint);

			textPaintDebug=new Paint();
			textPaintDebug.setTextSize(32);

			penPaint=new Paint();
			penPaint.setStyle(Paint.Style.STROKE);
			penPaint.setStrokeWidth(3);

			buttonPaint=new Paint();

			shadowOffset=minDim/270.f;

			wrapPaint=new TextPaint();
			wrapPaint.setTextSize(16*getResources().getDisplayMetrics().density);
			wrapPaint.setColor(0xFF000000);

			setWillNotDraw(false);//for ViewGroup to call o n D r a w
			setClipChildren(false);

			LayoutInflater inflater=service.getLayoutInflater();
			preview=(TextView)inflater.inflate(R.layout.popup_view, null);

			preview.setGravity(Gravity.CENTER);
			preview.setTextSize(textSize/2);

			previewPopup=new PopupWindow(service.getApplication());
			previewPopup.setContentView(preview);
			previewPopup.setTouchable(false);
			previewPopup.setClippingEnabled(false);
			previewPopup.setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
		}
		catch(Exception e)
		{
			Log.e(TAG, "exception", e);
		}
	}
	public void startCKB(EditorInfo info, boolean restarting)//called in CKBservice.onStartInputView	TODO react to 'restarting'
	{
		if(DEBUG_MODE)
			Log.e(TAG, String.format(loc, "inputType = 0x%08X", info.inputType));
		switch(info.inputType&InputType.TYPE_MASK_CLASS)
		{
		case InputType.TYPE_NULL:
			mode=MODE_TEXT;
			break;
		case InputType.TYPE_CLASS_TEXT:
			switch(info.inputType&InputType.TYPE_MASK_VARIATION)
			{
			case InputType.TYPE_TEXT_VARIATION_URI:
				mode=MODE_URL;
				layoutHasExtension=true;
				break;
			case InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS:
			case InputType.TYPE_TEXT_VARIATION_EMAIL_SUBJECT:
				mode=MODE_EMAIL;
				layoutHasExtension=true;
				break;
			case InputType.TYPE_TEXT_VARIATION_PASSWORD:
			case InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD:
			case InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD:
				mode=MODE_PASSWORD;
				layoutHasExtension=true;
				break;
			case InputType.TYPE_TEXT_VARIATION_NORMAL:
			case InputType.TYPE_TEXT_VARIATION_SHORT_MESSAGE:
			case InputType.TYPE_TEXT_VARIATION_LONG_MESSAGE:
			case InputType.TYPE_TEXT_VARIATION_PERSON_NAME:
			case InputType.TYPE_TEXT_VARIATION_POSTAL_ADDRESS:
			case InputType.TYPE_TEXT_VARIATION_WEB_EDIT_TEXT:
			case InputType.TYPE_TEXT_VARIATION_FILTER:
			case InputType.TYPE_TEXT_VARIATION_PHONETIC:
			case InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS:
			default:
				mode=MODE_TEXT;
				break;
			}
			if(mask(info.inputType, InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS))
				capital=CAP_ALL;
			else if(mask(info.inputType, InputType.TYPE_TEXT_FLAG_CAP_WORDS))
				capital=CAP_WORDS;
			else if(mask(info.inputType, InputType.TYPE_TEXT_FLAG_CAP_SENTENCES))
				capital=CAP_SENTENCES;
			multiline=mask(info.inputType, InputType.TYPE_TEXT_FLAG_MULTI_LINE)||mask(info.inputType, InputType.TYPE_TEXT_FLAG_IME_MULTI_LINE);
			break;
		case InputType.TYPE_CLASS_PHONE:
			mode=MODE_PHONE_NUMBER;
			break;
		case InputType.TYPE_CLASS_NUMBER:
			switch(info.inputType&InputType.TYPE_MASK_VARIATION)
			{
			case InputType.TYPE_NUMBER_VARIATION_PASSWORD:
				mode=MODE_NUMERIC_PASSWORD;
				break;
			case InputType.TYPE_NUMBER_VARIATION_NORMAL:
			default:
				mode=MODE_NUMBER;
				break;
			}
			numeric_signed=mask(info.inputType, InputType.TYPE_NUMBER_FLAG_SIGNED);
			numeric_decimal=mask(info.inputType, InputType.TYPE_NUMBER_FLAG_DECIMAL);
			break;
		case InputType.TYPE_CLASS_DATETIME:
			mode=MODE_TEXT;
			switch(info.inputType&InputType.TYPE_MASK_VARIATION)
			{
			case InputType.TYPE_DATETIME_VARIATION_NORMAL:
			case InputType.TYPE_DATETIME_VARIATION_DATE:
			case InputType.TYPE_DATETIME_VARIATION_TIME:
				break;
			}
			break;
		}

		if(!restarting||layoutInfo==null)
		{
			boolean decimals=numeric_signed||numeric_decimal;
			int hint=layoutInfo!=null?layoutInfo[INFO_IDX_SELECTED_IDX]:-10;//-10 is an illegal layout index, see ckb.c

			//addError(String.format(loc, "CKBnative.init() is called, mode=%d, hint=%d, decimals=%d", mode, decimals?1:0, hint));//DEBUG

			layoutInfo=CKBnativelib.init(mode, decimals, hint, w, h);
		}
		int nErrors=CKBnativelib.getNErrors();
		//addError("init: "+nErrors+" errors");
		if(layoutInfo==null||nErrors>0)
		{
			service.displayToast(CKBnativelib.getError(0));//toast just the first error for now
			urgentMsg.clear();
			for(int ke=0;ke<nErrors;++ke)
			{
				String err=CKBnativelib.getError(ke);
				addError(err);
				urgentMsg.add(err);
			}
			CKBnativelib.clearErrors();
		}
		theme_colors=CKBnativelib.getTheme();
		boolean theme_invalid=true;
		if(theme_colors!=null)
		{
			for(int k=1;k<theme_colors.length;++k)//a simple check if all colors are the same
				theme_invalid=theme_invalid&&theme_colors[k]==theme_colors[0];
		}
		if(theme_invalid)
		{
			Log.e(TAG, "Theme is invalid, fallback to backup theme");
			theme_colors=backup_theme;
		}
		if(DEBUG_COLORS)
		{
			Log.e(TAG, "The theme is:");
			Log.e(TAG, String.format(loc, "background        0x%08X", theme_colors[COLOR_BACKGROUND]));
			Log.e(TAG, String.format(loc, "idle button       0x%08X", theme_colors[COLOR_BUTTON_IDLE]));
			Log.e(TAG, String.format(loc, "quick mode        0x%08X", theme_colors[COLOR_BUTTON_FAST]));
			Log.e(TAG, String.format(loc, "pressed button    0x%08X", theme_colors[COLOR_BUTTON_PRESSED]));
			Log.e(TAG, String.format(loc, "label             0x%08X", theme_colors[COLOR_LABELS]));
			Log.e(TAG, String.format(loc, "shadow modifier   0x%08X", theme_colors[COLOR_SHADOW_MODIFIER]));
			Log.e(TAG, String.format(loc, "shadow letter     0x%08X", theme_colors[COLOR_SHADOW_LETTER]));
			Log.e(TAG, String.format(loc, "shadow non-letter 0x%08X", theme_colors[COLOR_SHADOW_NON_LETTER]));
			Log.e(TAG, String.format(loc, "preview popup     0x%08X", theme_colors[COLOR_PREVIEW_POPUP]));
		}
		textPaint			.setColor(theme_colors[COLOR_LABELS]);
		letterPaint			.setColor(theme_colors[COLOR_LABELS]);
		specialButtonPaint	.setColor(theme_colors[COLOR_LABELS]);
		buttonPaint.setColor(theme_colors[COLOR_BUTTON_IDLE]);
		textPaint			.setShadowLayer(shadowOffset*0.5f, shadowOffset, shadowOffset, theme_colors[COLOR_SHADOW_NON_LETTER]);//0xAARRGGBB
		letterPaint			.setShadowLayer(shadowOffset*0.5f, shadowOffset, shadowOffset, theme_colors[COLOR_SHADOW_LETTER]);
		specialButtonPaint	.setShadowLayer(shadowOffset*0.5f, shadowOffset, shadowOffset, theme_colors[COLOR_SHADOW_MODIFIER]);
		preview.setTextColor(theme_colors[COLOR_PREVIEW_POPUP]);
		Drawable bk=DrawableCompat.wrap(preview.getBackground()).mutate();
		DrawableCompat.setTint(bk, theme_colors[COLOR_BUTTON_PRESSED]);//change preview bkColor

		if(layoutInfo==null)
		{
			service.displayToast("LayoutInfo error. Opening backup layout.");
			get_layout_backup();
		}
		else
		{
			int nLayouts=layoutInfo[INFO_IDX_LAYOUT_COUNT];
			for(int kl=0;kl<nLayouts;++kl)//only 'lang' layouts
			{
				String name=CKBnativelib.getLayoutName(kl);
				if(name==null)
				{
					addError(String.format(loc, "Error retrieving name of layout %d, nLayouts=%d", kl, nLayouts));
					layoutNames.add("Error");
				}
				else
					layoutNames.add(name);
			}
			switch_layout(layoutInfo[INFO_IDX_SELECTED_IDX], layoutHasExtension);

			//addError(String.format(loc, "Selecting layout %d/%d, extension=%d, layout size=%d",
			//	layoutInfo[INFO_IDX_SELECTED_IDX], layoutInfo[INFO_IDX_LAYOUT_COUNT], layoutHasExtension?1:0, layout.length));//DEBUG
		}

		if(layout==null)
		{
			service.displayToast("Layout error. Opening backup layout.");
			layoutNames.clear();
			get_layout_backup();
		}

		mode_unicode=false;

		ccGridX=ceilMultiple((int)(textSize*1.5), w);
		if(layout[LAYOUT_IDX_ROW_COUNT]==0)
		{
			addError("Layout count == 0");
			ccGridY=kb_h/5;
		}
		else
			ccGridY=layout[LAYOUT_IDX_HEIGHT_PX]/layout[LAYOUT_IDX_ROW_COUNT];

		invalidate();
	}

	//GUI
	void showKeyPreview(ButtonIdx bIdx, String label)//see KeyboardView.java showKey() line 924
	{
		if(bIdx.invalid()||bIdx.code==(MODMASK|KEY_NAB)||label==null)
			return;
		try
		{
			preview.setCompoundDrawables(null, null, null, null);
			if(bIdx.code==' ')
				preview.setText("space");
			else
				preview.setText(label);
			if(label.length()>1)
				preview.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize_long*2);
			else
				preview.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize*2);
			preview.setTypeface(Typeface.DEFAULT_BOLD);

			//position the preview
			int x1=bIdx.x1, x2=bIdx.x2, y1=bIdx.y1, y2=bIdx.y2;
			preview.measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED), MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
			int width=preview.getMeasuredWidth()+preview.getPaddingLeft()+preview.getPaddingRight(),
				height=preview.getMeasuredHeight()+preview.getPaddingTop()+preview.getPaddingBottom();
			//int width=x2-x1, height=y2-y1;
			//if(y1<height)//show preview on the side	X
			//{
			//	if(x1<w/2)
			//		x1+=width;
			//	else
			//		x1-=width;
			//}
			//else
				y1-=height;
			if(w>h)//bugfix just for landscape
				y1+=(h-kb_h)-(bIdx.y2-bIdx.y1);//y1 starts from screen top, add kb_y1 minus button height

			//x1=(x1+x2)/2-width/2;
			x1=clamp(0, (x1+x2-width)/2, w-width);

			//addError(String.format(loc, "Preview is on for button 0x%08X (%d~%d, %d~%d) (%d, %d) %dx%d",
			//	bIdx.code, bIdx.x1, bIdx.x2, bIdx.y1, bIdx.y2, x1, y1, width, height));//DEBUG

			if (previewPopup.isShowing())
				previewPopup.update(x1, y1, width, height);
			else
			{
				previewPopup.setWidth(width);
				previewPopup.setHeight(height);
				previewPopup.showAtLocation(service.main_layout, Gravity.NO_GRAVITY, x1, y1);//works
				//previewPopup.showAtLocation(this, Gravity.NO_GRAVITY, x1, y1);//works
			}
			preview.setVisibility(VISIBLE);
		}
		catch(Exception e)
		{
			addError(e);
		}
	}
	void hideKeyPreview()
	{
		previewPopup.dismiss();
		preview.setVisibility(INVISIBLE);
	}

	//drawing functions
	void drawRect(float x1, float x2, float y1, float y2, Paint paint, Canvas canvas)
	{
		rectf.set(x1, y1, x2, y2);
		canvas.drawRect(rectf, paint);
	}
	void drawRoundRect(float x1, float x2, float y1, float y2, float radius, Paint paint, Canvas canvas)
	{
		rectf.set(x1, y1, x2, y2);
		canvas.drawRoundRect(rectf, radius, radius, paint);
	}
	void drawTouchMark(int touchId, int color, Canvas canvas)
	{
		int idx=touchInfo.findPointerIndex(touchId);
		if(idx<touchInfo.size())
		{
			TouchInfo.Pointer ti=touchInfo.get(idx);
			if(ti!=null)
			{
				penPaint.setColor(color);
				canvas.drawLine(0, ti.y, w, ti.y, penPaint);
				canvas.drawLine(ti.x, 0, ti.x, h, penPaint);
			}
		}
	}
	void draw_cc_grid(int x1, int x2, int y1, int y2, float radius, Canvas canvas)
	{
		if(multiline)
		{
			for(int ky=y1;ky<y2;ky+=ccGridY)//draw grid
			{
				for(int kx=x1;kx<x2;kx+=ccGridX)
					drawRoundRect(kx+radius, kx+ccGridX-radius, ky+radius, ky+ccGridY-radius, radius, buttonPaint, canvas);
			}
		}
		else
		{
			for(int kx=x1;kx<x2;kx+=ccGridX)
				drawRoundRect(kx+radius, kx+ccGridX-radius, radius, kb_h-radius, radius, buttonPaint, canvas);
		}
	}
	void draw_kb_button(int x1, int x2, int y1, int y2, int kx, int ky, int code, float radius, Canvas canvas)
	{
		if(code==(MODMASK|KEY_NAB))
			return;

		int dx=x2-x1, dy=y2-y1;
		canvas.translate(x1, y1);

		int color=buttonPaint.getColor(), color2=color;
		boolean colorWasSet=false;
		if((code&MODMASK)!=0)
		{
			switch(code&~MODMASK)//check long-pressable keys
			{
			case KEY_SYMBOLS:	if(layoutHasExtension){	colorWasSet=true; color2=theme_colors[COLOR_BUTTON_PRESSED];}	break;
			case KEY_CTRL:		if(isActive_ctrl){		colorWasSet=true; color2=theme_colors[COLOR_BUTTON_PRESSED];}	break;
			case KEY_ALT:		if(isActive_alt){		colorWasSet=true; color2=theme_colors[COLOR_BUTTON_PRESSED];}	break;

			case KEY_CAPSLOCK:
			case KEY_SHIFT:		if(isActive_shift){		colorWasSet=true; color2=theme_colors[COLOR_BUTTON_PRESSED];}	break;
			}
		}
		if(!colorWasSet&&quickMode==1)
		{
			colorWasSet=true;
			color2=theme_colors[COLOR_BUTTON_FAST];
		}
		if(kx==tap_x&&ky==tap_y)
		{
			colorWasSet=true;
			color2=theme_colors[COLOR_BUTTON_PRESSED];
		}
		if(colorWasSet)
			buttonPaint.setColor(color2);
		drawRoundRect(radius, dx-radius, radius, dy-radius, radius, buttonPaint, canvas);
		if(colorWasSet)
			buttonPaint.setColor(color);

		float currentSize;
		Paint p=null;
		int code_case=code;
		if((code&MODMASK)!=0)
			p=specialButtonPaint;
		else if(isActive_shift)
			code_case=Character.toUpperCase(code);
		else
			code_case=Character.toLowerCase(code);
		String label=getLabel(code_case);

		if(label.length()>1)
		{
			if(p==null)
				p=textPaint;
			currentSize=textSize_long;
			textPaint.setTypeface(Typeface.DEFAULT_BOLD);
		}
		else
		{
			if(p==null)
			{
				if(label.length()==1&&Character.isLetter(label.charAt(0)))
					p=letterPaint;
				else
					p=textPaint;
			}
			currentSize=textSize;
			textPaint.setTypeface(Typeface.DEFAULT);
		}
		p.setTextSize(currentSize);
		if(label.length()>1)
		{
			float x=dx*0.5f, y=dy*0.5f+currentSize*0.15f;//button center + correction offset

			p.getTextBounds("Ig", 0, 2, rect);//https://stackoverflow.com/questions/3153870/canvas-drawtext-does-not-print-linebreak
			float
				lineHeight=(int)((float)rect.height()*1.2f),
				yOffset=0;
			String[] lines=label.split("\n");
			y-=(lines.length-1)*lineHeight/2;
			for(String line:lines)//draw each line
			{
				canvas.drawText(line, x, y+yOffset, p);
				yOffset+=lineHeight;
			}
		}
		else
			canvas.drawText(label, dx*0.5f, dy*0.5f+currentSize*0.2f, p);

		canvas.translate(-x1, -y1);
	}
	void draw_kb_static_layout(int[][] layout, int x1, int x2, int y1, int y2, float radius, Canvas canvas)
	{
		int dx=x2-x1, dy=y2-y1;
		int ya=0;
		for(int ky=0;ky<layout.length;++ky)
		{
			int yb=(ky+1)*dy/layout.length;
			int[] row=layout[ky];
			int rel_width=0;
			for(int kx=0;kx<row.length;kx+=2)
				rel_width+=row[kx+1];
			int xa=0, xa2=0;
			for(int kx=0;kx<row.length;kx+=2)
			{
				int xb=xa+row[kx+1], xb2=xb*dx/rel_width;
				draw_kb_button(x1+xa2, x1+xb2, y1+ya, y1+yb, kx, ky, row[kx], radius, canvas);
				xa=xb;
				xa2=xb2;
			}
			ya=yb;
		}
	}
	int drawText_byTopLeft(String text, float x, float y, boolean centerX, boolean centerY, Paint paint, Canvas canvas)
	{
		if(!centerX||!centerY)
			paint.getTextBounds(text, 0, text.length(), rect);
		if(!centerX)
			x+=rect.exactCenterX();

		if(!centerY)
			y+=rect.height();
		canvas.drawText(text, x, y, paint);
		return rect.height();
	}
	int drawText_wrap(String text, int x1, int x2, int y, Canvas canvas)
	{
		if(android.os.Build.VERSION.SDK_INT>=android.os.Build.VERSION_CODES.M)//multiline text
		{
			StaticLayout.Builder builder=StaticLayout.Builder.obtain(text, 0, text.length(), wrapPaint, x2-x1);
			StaticLayout sl=builder.build();
			int textHeight=sl.getHeight(), py=y+textHeight;
			canvas.translate(x1, py);
			sl.draw(canvas);
			canvas.translate(-x1, -py);
			return textHeight;
		}
		return drawText_byTopLeft(text, x1, y, false, false, textPaint, canvas);
	}
	void draw_searchResults(int x1, int x2, int y1, int y2, Canvas canvas)
	{
		float radius=10,
			r2=radius/2;
		int color=buttonPaint.getColor();
		float ya=y1-uniSearch_wpy;
		if(uniSearch_results.size()>0)
		{
			for(int k=0;k<uniSearch_results.size()&&ya<y2;++k)		//print search results
			{
				float yb=ya+UNI_SEARCH_ROW_HEIGHT;
				SearchResult result=uniSearch_results.get(k);

				buttonPaint.setColor((k&1)!=0?0xC0A0A0A0:0xC0D0D0D0);//alternating shades
				drawRoundRect((float)x1+r2, (float)x2-r2, ya+r2, yb-r2, radius, buttonPaint, canvas);

				drawText_byTopLeft(result.displayString, x1+radius, (ya+yb)*0.5f, false, true, textPaint, canvas);

				ya=yb;
			}
		}
		else
		{
			buttonPaint.setColor(0xC0A0A0A0);
			drawRoundRect(x1+r2, x2-r2, y1+r2, y2-r2, radius, buttonPaint, canvas);

			drawText_byTopLeft("(no results)", (x1+x2)*0.5f, (y1+y2)*0.5f, true, true, textPaint, canvas);
		}
		buttonPaint.setColor(color);
	}
	@Override public void onDraw(Canvas canvas)
	{
		float radius=10;

		canvas.drawColor(theme_colors[COLOR_BACKGROUND]);//same everywhere
		if(mode_unicode)
		{
			int searchBarColor=0xFF6A1FF5;
			float textSizeFactor=1.3f;
			float textSize=textPaint.getTextSize();
			float textMarginX=10, textMarginY=10, queryMarginX=30;
			int nResults=uniSearch_results.size();
			uniSearch_setRowHeight();
			if(w<h)//unicode search - portrait
			{
				int rBoxHeight=nResults;		//calculate dimensions
				if(rBoxHeight<1)
					rBoxHeight=1;
				if(rBoxHeight>backup_layout_port.length-1)
					rBoxHeight=backup_layout_port.length-1;
				int nRowsTotal=rBoxHeight+1+backup_layout_port.length;
				int layoutPos=(int)((rBoxHeight+1)*UNI_SEARCH_ROW_HEIGHT);
				UNI_SEARCH_PORT_Y1=(int)(layoutPos-UNI_SEARCH_ROW_HEIGHT);
				UNI_SEARCH_PORT_Y2=layoutPos;
				UNI_SEARCH_N_ROWS_TOTAL=nRowsTotal;

				textPaint.setTextSize(textSize*textSizeFactor);

				draw_searchResults(0, w, 0, UNI_SEARCH_PORT_Y1, canvas);		//print search results

				if(!DEBUG_UNICODE)		//hide OOB text with opaque background
				{
					int color=buttonPaint.getColor();

					buttonPaint.setColor(0xFF000000|theme_colors[COLOR_BACKGROUND]);
					drawRect(0, w, UNI_SEARCH_PORT_Y2, kb_h, buttonPaint, canvas);

					buttonPaint.setColor(searchBarColor);//the searchbar is the back button
					drawRect(0, w, UNI_SEARCH_PORT_Y1, UNI_SEARCH_PORT_Y2, buttonPaint, canvas);

					buttonPaint.setColor(color);
				}

				//print search query
				drawText_byTopLeft(uniSearch_query.toString(), queryMarginX, (UNI_SEARCH_PORT_Y1+UNI_SEARCH_PORT_Y2)*0.5f, false, true, textPaint, canvas);

				textPaint.setTextSize(textSize);

				//draw layout
				draw_kb_static_layout(backup_layout_port, 0, w, UNI_SEARCH_PORT_Y2, kb_h, radius, canvas);
			}
			else//unicode search - landscape
			{
				UNI_SEARCH_LAND_X1=(int)(w*0.3);
				UNI_SEARCH_LAND_X2=(int)(w*0.7);
				UNI_SEARCH_LAND_Y1=(int)(kb_h-UNI_SEARCH_ROW_HEIGHT);
				UNI_SEARCH_N_ROWS_TOTAL=backup_layout_land_left.length;

				textPaint.setTextSize(textSize*textSizeFactor);

				draw_searchResults(UNI_SEARCH_LAND_X1, UNI_SEARCH_LAND_X2, 0, UNI_SEARCH_LAND_Y1, canvas);

				if(!DEBUG_UNICODE)		//hide OOB text with opaque background
				{
					int color=buttonPaint.getColor();

					buttonPaint.setColor(0xFF000000|theme_colors[COLOR_BACKGROUND]);
					drawRect(0, UNI_SEARCH_LAND_X1, 0, kb_h, buttonPaint, canvas);
					drawRect(UNI_SEARCH_LAND_X2, w, 0, kb_h, buttonPaint, canvas);

					buttonPaint.setColor(searchBarColor);//the searchbar is the back button
					drawRect(UNI_SEARCH_LAND_X1, UNI_SEARCH_LAND_X2, UNI_SEARCH_LAND_Y1, kb_h, buttonPaint, canvas);

					buttonPaint.setColor(color);
				}

				//print search query
				drawText_byTopLeft(uniSearch_query.toString(), UNI_SEARCH_LAND_X1+textMarginX, UNI_SEARCH_LAND_Y1+textMarginY, false, false, textPaint, canvas);

				textPaint.setTextSize(textSize);

				draw_kb_static_layout(backup_layout_land_left, 0, UNI_SEARCH_LAND_X1, 0, kb_h, radius, canvas);
				draw_kb_static_layout(backup_layout_land_right, UNI_SEARCH_LAND_X2, w, 0, kb_h, radius, canvas);
			}

			//addError(String.format(loc, "UNI_SEARCH_PORT_Y1=%d", UNI_SEARCH_PORT_Y1));//DEBUG-2
			//addError(String.format(loc, "UNI_SEARCH_PORT_Y2=%d", UNI_SEARCH_PORT_Y2));
			//addError(String.format(loc, "kb_h=%d", kb_h));
			//addError(String.format(loc, "UNI_SEARCH_LAND_X1=%d", UNI_SEARCH_LAND_X1));
			//addError(String.format(loc, "UNI_SEARCH_LAND_X2=%d", UNI_SEARCH_LAND_X2));
			//addError(String.format(loc, "UNI_SEARCH_LAND_Y1=%d", UNI_SEARCH_LAND_Y1));
			//addError(String.format(loc, "UNI_SEARCH_N_ROWS_TOTAL=%d", UNI_SEARCH_N_ROWS_TOTAL));
			//addError(String.format(loc, "UNI_SEARCH_ROW_HEIGHT=%f", UNI_SEARCH_ROW_HEIGHT));
			return;
		}
		if(cursor_control==CC_LANG)
		{
			if(layoutInfo!=null)
			{
				int nLayouts=layoutInfo[INFO_IDX_LAYOUT_COUNT];
				int choice=-1;
				TouchInfo.Pointer pointer=touchInfo.findPointer(touchId_switchLang);
				if(pointer!=null)
					choice=(int)(pointer.x*nLayouts/w);
				int color=buttonPaint.getColor();
				for(int kl=0;kl<nLayouts;++kl)
				{
					if(kl==choice)
						buttonPaint.setColor(theme_colors[COLOR_BUTTON_PRESSED]);
					drawRoundRect((float)kl*w/nLayouts+25, (float)(kl+1)*w/nLayouts-25, 25, kb_h-25, 50, buttonPaint, canvas);
					if(kl==choice)
						buttonPaint.setColor(color);
				}
				float size=textPaint.getTextSize();
				textPaint.setTextSize(size*4);
				for(int kl=0;kl<layoutNames.size();++kl)
					drawText_byTopLeft(layoutNames.get(kl), (float)(kl+0.5f)*w/nLayouts, (float)kb_h*0.5f, true, true, textPaint, canvas);
				textPaint.setTextSize(size);
			}
		}
		else if(cursor_control!=CC_DISABLED)
		{
			draw_cc_grid(0, w, 0, kb_h, radius, canvas);

			//draw cross hair
			int color=penPaint.getColor();
			final int color_cursor=0xFFFFFFFF, color_start=0xFF0000FF, color_end=0xFFFF0000;
			switch(cursor_control)
			{
			case CC_SEL_END:
				drawTouchMark(touchId_selEnd, color_end, canvas);
				break;
			case CC_SEL_START:
				drawTouchMark(touchId_selStart, color_start, canvas);
				break;
			case CC_SEL_BOTH:
				drawTouchMark(touchId_selStart, color_start, canvas);
				drawTouchMark(touchId_selEnd, color_end, canvas);
				break;
			case CC_CURSOR:
				drawTouchMark(touchId_selStart, color_cursor, canvas);
				break;
			}
			penPaint.setColor(color);
		}
		else if(layout!=null)//normal keyboard
		{
			int nRows=layout[LAYOUT_IDX_ROW_COUNT];
			for(int ky=0;ky<nRows;++ky)
			{
				int rowStartIdx=layout[2+ky], nextRowStartIdx=layout[ky+3];
				int nButtons=(nextRowStartIdx-rowStartIdx-1)/2;
				int y1=ky*kb_h/nRows;
				int y2=(ky+1)*kb_h/nRows;
				//addError(String.format(loc, "ky=%d rowStartIdx=%d, nextRowStartIdx=%d nButtons=%d y1=%d y2=%d", ky, rowStartIdx, nextRowStartIdx, nButtons, y1, y2));//DEBUG
				for(int kx=0;kx<nButtons;++kx)
				{
					int x1	=layout[rowStartIdx+kx*2];
					int code=layout[rowStartIdx+kx*2+1];
					int x2	=layout[rowStartIdx+kx*2+2];
					draw_kb_button(x1, x2, y1, y2, kx, ky, code, radius, canvas);
				}
			}
		}

	/*	if(DEBUG_TOUCH)//multitouch test
		{
			int color=penPaint.getColor();
			final int red=0xFFFF0000, grey=0xFF808080, pink=0xFFFF00FF;
			for(int k=0, size=touchInfo.size();k<size;++k)
			{
				TouchInfo.Pointer ti=touchInfo.get(k);
				switch(ti.state)
				{
				case TouchInfo.S_DOWN:
					penPaint.setColor(red);
					canvas.drawLine(0, ti.y, w, ti.y, penPaint);
					canvas.drawLine(ti.x, 0, ti.x, h, penPaint);
					break;
				case TouchInfo.S_MOVED:
					penPaint.setColor(grey);
					canvas.drawLine(0, ti.prevy, w, ti.prevy, penPaint);
					canvas.drawLine(ti.prevx, 0, ti.prevx, h, penPaint);
					penPaint.setColor(red);
					canvas.drawLine(0, ti.y, w, ti.y, penPaint);
					canvas.drawLine(ti.x, 0, ti.x, h, penPaint);
					break;
				case TouchInfo.S_UP:
					penPaint.setColor(grey);
					canvas.drawLine(0, ti.y, w, ti.y, penPaint);
					canvas.drawLine(ti.x, 0, ti.x, h, penPaint);
					break;
				case TouchInfo.S_CANCEL:
					penPaint.setColor(pink);
					canvas.drawLine(0, ti.y, w, ti.y, penPaint);
					canvas.drawLine(ti.x, 0, ti.x, h, penPaint);
					break;
				}
			}
			penPaint.setColor(color);
		}//*/

		int xPos=w/4, py=0;//when logcat isn't enough
		for(int ky=0;ky<urgentMsg.size();++ky)//print urgent D E B U G messages on keyboard
			py+=drawText_wrap(urgentMsg.get(urgentMsg.size()-1-ky), xPos, w, py, canvas);//newest-first
	}

	//timing
	boolean isTurboKey(int code)
	{
		switch(code)
		{
		case MODMASK|KEY_NAB:
		case 27://esc
		case MODMASK|KEY_HOME:case MODMASK|KEY_END:
		case MODMASK|KEY_SHIFT:case MODMASK|KEY_CTRL:case MODMASK|KEY_ALT:
		case MODMASK|KEY_INSERT:
		case MODMASK|KEY_CAPSLOCK:case MODMASK|KEY_NUMLOCK:case MODMASK|KEY_SCROLLLOCK:
		case MODMASK|KEY_SETTINGS:
		case MODMASK|KEY_UNICODE:
		case MODMASK|KEY_SYMBOLS:
			return false;
		}
		return true;
	}
	boolean isLongPressableKey(int code)
	{
		return code==(MODMASK|KEY_SHIFT);//only shift has long press action
	}

	//cursor control
	void exitCursorControl()
	{
		cursor_control=CC_DISABLED;

		isActive_shift=false;
		service.onKeyCallback(MODMASK|KEY_SHIFT, 2);
	}
	void applyCursorControl(TouchInfo.Pointer ti, int cc_flags)
	{
		int ccX1=(int)(ti.prevx	/ccGridX), ccY1=(int)(ti.prevy	/ccGridY),
			ccX2=(int)(ti.x		/ccGridX), ccY2=(int)(ti.y		/ccGridY);
		if(DEBUG_CC)
			Log.e(TAG, String.format(loc, "applyCC: (%d, %d)->(%d, %d)", ccX1, ccY1, ccX2, ccY2));//

		if(ccX2<ccX1)
			service.onNavigateCallback(MODMASK|KEY_LEFT, Math.abs(ccX2-ccX1), cc_flags);
		else if(ccX2>ccX1)
			service.onNavigateCallback(MODMASK|KEY_RIGHT, Math.abs(ccX2-ccX1), cc_flags);

		if(multiline)
		{
			if(ccY2<ccY1)
				service.onNavigateCallback(MODMASK|KEY_UP, Math.abs(ccY2-ccY1), cc_flags);
			else if(ccY2>ccY1)
				service.onNavigateCallback(MODMASK|KEY_DOWN, Math.abs(ccY2-ccY1), cc_flags);
		}
	}
	void logCursorControl(String tag)
	{
		if(!DEBUG_CC)
			return;
		switch(cursor_control)
		{
		case CC_DISABLED:
			ButtonIdx button=getButton_layout(tap_x, tap_y);
			String label=getLabel(button.code);
			if(isHeld_shift)
				Log.e(TAG, String.format(loc, "%s: CC=DISABLED: shift_id=%d, %s", tag, touchId_shift, label));
			else
				Log.e(TAG, String.format(loc, "%s: CC=DISABLED: %s", tag, label));
			break;
		case CC_SEL_READY:
			if(isHeld_shift)
				Log.e(TAG, String.format(loc, "%s: CC=%d READY: shift_id=%d", tag, cursor_control, touchId_shift));
			else
				Log.e(TAG, String.format(loc, "%s: CC=%d READY: BROKEN: SHIFT IS NOT HELD!", tag, cursor_control));
			break;
		case CC_SEL_END:
			if(isHeld_shift)
				Log.e(TAG, String.format(loc, "%s: CC=%d END: shift_id=%d, end_id=%d", tag, cursor_control, touchId_shift, touchId_selEnd));
			else
				Log.e(TAG, String.format(loc, "%s: CC=%d END: end_id=%d", tag, cursor_control, touchId_selEnd));
			break;
		case CC_SEL_START:
			if(isHeld_shift)
				Log.e(TAG, String.format(loc, "%s: CC=%d START: shift_id=%d, start_id=%d", tag, cursor_control, touchId_shift, touchId_selStart));
			else
				Log.e(TAG, String.format(loc, "%s: CC=%d START: start_id=%d", tag, cursor_control, touchId_selStart));
			break;
		case CC_SEL_BOTH:
			if(isHeld_shift)
				Log.e(TAG, String.format(loc, "%s: CC=%d BOTH: shift_id=%d, start_id=%d, end_id=%d", tag, cursor_control, touchId_shift, touchId_selStart, touchId_selEnd));
			else
				Log.e(TAG, String.format(loc, "%s: CC=%d BOTH: start_id=%d, end_id=%d", tag, cursor_control, touchId_selStart, touchId_selEnd));
			break;
		case CC_CURSOR:
			if(isHeld_shift)
				Log.e(TAG, String.format(loc, "%s: CC=%d CURSOR: shift_id=%d, cursor_id=%d", tag, cursor_control, touchId_shift, touchId_selStart));
			else
				Log.e(TAG, String.format(loc, "%s: CC=%d CURSOR: cursor_id=%d", tag, cursor_control, touchId_selStart));
			break;
		case CC_LANG:
			if(isHeld_shift)
				Log.e(TAG, String.format(loc, "%s: CC=%d LANG: shift_id=%d, cursor_id=%d", tag, cursor_control, touchId_shift, touchId_switchLang));
			else
				Log.e(TAG, String.format(loc, "%s: CC=%d LANG: cursor_id=%d", tag, cursor_control, touchId_switchLang));
			break;
		}
	}
	void logState(String tag)
	{
		if(!DEBUG_STATE)
			return;
		if(isHeld_shift)
			Log.e(TAG, String.format(loc, "%s:  isActive_shift: %b, touchId_shift=%d", tag, isActive_shift, touchId_shift));
		else
			Log.e(TAG, String.format(loc, "%s:  isActive_shift: %b", tag, isActive_shift));
	}
	int findValidPointer(int skipId, int skipId2)
	{
		for(int k=0, size=touchInfo.size();k<size;++k)
		{
			TouchInfo.Pointer temp=touchInfo.get(k);
			if(temp.id!=skipId&&temp.id!=skipId2&&temp.isValid())
				return temp.id;
		}
		return -1;
	}
	int selectionControl_reassign(boolean control_other, int id_other, int flag_other)
	{
		int id_current;
		if(isHeld_shift)
		{
			id_current=findValidPointer(touchId_shift, control_other?id_other:-1);
			if(id_current==-1)
			{
				if(control_other)
					cursor_control=flag_other;
				else
					cursor_control=CC_SEL_READY;
			}
		}
		else
		{
			id_current=findValidPointer(control_other?id_other:-1, -1);
			if(id_current==-1)
			{
				if(control_other)
					cursor_control=flag_other;
				else
				{
					exitCursorControl();
					hideKeyPreview();
				}
			}
		}
		return id_current;
	}
	int selectionControl(boolean control_current, boolean control_other, int id_current, int id_other, int flag_current, int flag_other)//returns new id_current
	{
		if(control_current)
		{
			int idx=touchInfo.findPointerIndex(touchId_selEnd);
			TouchInfo.Pointer ti=null;
			if(idx<touchInfo.size())
				ti=touchInfo.get(idx);
			if(ti==null)//unreachable
				return selectionControl_reassign(control_other, id_other, flag_other);
			switch(ti.state)
			{
			case TouchInfo.S_MOVED:
				applyCursorControl(ti, flag_current);
				break;
			case TouchInfo.S_UP:
			case TouchInfo.S_CANCEL://reassign, ready or exit
				id_current=selectionControl_reassign(control_other, id_other, flag_other);
				break;
			}
		}
		return id_current;
	}

	void set_possible_timers(TouchInfo.Pointer pointer, ButtonIdx button)
	{
		//String label=getLabel(button.code);//DEBUG
		//addError(String.format(loc, "set_possible_timers(%s)", label));//
		try
		{
			if(isTurboKey(button.code))
			{
				if(timerOn==0)
				{
					timerOn=1; touchId_timer=pointer.id;
					turboTask=new TurboTask(button.code);
					timer=new Timer();
					timer.schedule(turboTask, turboStart_ms, turbo_ms);
				}
			}
			else if(isLongPressableKey(button.code))
			{
				//addError(String.format(loc, "set_possible_timers(): if(!%d) setting long press timer", timerOn));//DEBUG
				if(timerOn==0)
				{
					//addError("set_possible_timers(): setting long press timer");//DEBUG
					timerOn=2; touchId_timer=pointer.id;
					longPressTask=new LongPressTask(button.code);
					timer=new Timer();
					timer.schedule(longPressTask, longPress_ms);
				}
			}
		}
		catch(Exception ex)
		{
			addError(ex);
		}
	}
	void uniSearch_setRowHeight()
	{
		if(w<h)//landscape
			UNI_SEARCH_ROW_HEIGHT=(float)h/20;
		else
			UNI_SEARCH_ROW_HEIGHT=(float)kb_h/backup_layout_land_left.length;
	}
	void uniSearch_updateResults()
	{
		String str=uniSearch_query.toString();
		//addError(String.format(loc, "Entering searchUnicode(%s)", str));//DEBUG
		//long start=System.nanoTime();
		int[] codes=CKBnativelib.searchUnicode(str);
		//long end=System.nanoTime();
		//addError(String.format(loc, "Elapsed: %d", end-start));//DEBUG

		uniSearch_results.clear();
		if(codes!=null)
		{
			StringBuilder sb=new StringBuilder();
			uniSearch_results.ensureCapacity(codes.length/2);
			if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.KITKAT)
			{
				for(int k=0;k<codes.length;k+=2)
				{
					SearchResult result=new SearchResult();
					result.code=codes[k];
					result.name=Character.getName(codes[k]);
					sb.appendCodePoint(codes[k]);
					sb.append(String.format(loc, " (U+%X): %s", codes[k], result.name));
					result.displayString=sb.toString();
					sb.delete(0, sb.length());

					uniSearch_results.add(result);
				}
			}
			else
			{
				for(int k=0;k<codes.length;k+=2)
				{
					SearchResult result=new SearchResult();
					result.code=codes[k];
					result.displayString=Arrays.toString(Character.toChars(codes[k]))+String.format(" (U+%X)", codes[k]);
					uniSearch_results.add(result);
				}
			}
		}

		uniSearch_wpy=0;
		if(w<h)//portrait
		{
			int nRows=uniSearch_results.size();
			if(nRows<1)
				nRows=1;
			if(nRows>backup_layout_port.length-1)
				nRows=backup_layout_port.length-1;

			nRows+=backup_layout_port.length+1;
			int height=(int)(nRows*UNI_SEARCH_ROW_HEIGHT);
			set_kb_height(height);

			//addError(String.format(loc, "nRows = %d, height = %d", nRows, height));//DEBUG-2
		}
	}
	void uniSearch_handle_layout(int[][] layout, int x1, int x2, int y1, int y2, TouchInfo.Pointer pointer)//keyboard writes to 'unicode search bar'
	{
		ButtonIdx button=getButton_px_static(layout, x1, x2, y1, y2, pointer.x, pointer.y);
		if(pointer.state==TouchInfo.S_DOWN)
		{
			String label=getLabel(button.code);
			showKeyPreview(button, label);
			//if(!button.invalid())				//X  no timers in unicode search mode for now
			//	set_possible_timers(pointer, button);
		}
		if(pointer.state==TouchInfo.S_UP||pointer.state==TouchInfo.S_CANCEL)
		{
			hideKeyPreview();
			//if(button.code==longPressTask.code||button.code==turboTask.code)
			//{
			//	if(button.code==longPressTask.code)
			//		longPressTask.code=MODMASK|KEY_NAB;
			//	if(button.code==turboTask.code)
			//		turboTask.code=MODMASK|KEY_NAB;
			//	timerOn=0;
			//	timer.cancel();
			//}
		}
		if(pointer.state!=TouchInfo.S_CANCEL)
		{
			if(pointer.state==TouchInfo.S_DOWN)
			{
				//FIXME
				//here backspace works on pointer.up instead of the usual pointer.down
				//because of an infuriating bug
				//	where you press backspace and it types some key above backspace immediately
				if(button.code==(MODMASK|KEY_SHIFT))
				{
					isActive_shift=!isActive_shift;
					invalidate();
				}
				//switch(button.code)
				//{
				//case MODMASK|KEY_SHIFT:
				//	isActive_shift=!isActive_shift;
				//	invalidate();
				//	break;
				//case '\b':
				//	if(uniSearch_query.length()>0)
				//	{
				//		uniSearch_query.deleteCharAt(uniSearch_query.length()-1);
				//		uniSearch_updateResults();
				//		invalidate();
				//	}
				//	break;
				//}
			}
			else if(pointer.state==TouchInfo.S_UP)
			{
				if((button.code&MODMASK)==MODMASK||button.code==127)
					return;
				//if(button.code=='\b')
				//	return;
				if(button.code=='\b')
				{
					if(uniSearch_query.length()>0)
						uniSearch_query.deleteCharAt(uniSearch_query.length()-1);
				}
				else
					uniSearch_query.appendCodePoint(button.code);
				uniSearch_updateResults();
				invalidate();
			}
		}
	}
	void uniSearch_handle_resultBox(TouchInfo.Pointer pointer)
	{
		if(uniSearch_drag==UNI_SEARCH_DRAG_NONE)
		{
			if(Math.abs(pointer.x-pointer.startx)>10||Math.abs(pointer.y-pointer.starty)>10)
			{
				if(uniSearch_isScrollable())//scroll
					uniSearch_drag=UNI_SEARCH_DRAG_SCROLL;
			}
			else if(pointer.state==TouchInfo.S_UP)//select result
			{
				int choice_idx=(int)((uniSearch_wpy+pointer.y)*UNI_SEARCH_N_ROWS_TOTAL/kb_h);
				if(choice_idx>=0&&choice_idx<uniSearch_results.size())
				{
					SearchResult choice=uniSearch_results.get(choice_idx);
					service.onKeyCallback(choice.code, 3);
				}
			}
		}
		else if(uniSearch_drag==UNI_SEARCH_DRAG_SCROLL)
		{
			if(pointer.state==TouchInfo.S_MOVED)
			{
				uniSearch_wpy+=pointer.prevy-pointer.y;

				float wpy_limit=uniSearch_results.size()*UNI_SEARCH_ROW_HEIGHT-UNI_SEARCH_PORT_Y1;
				if(uniSearch_wpy<0)
					uniSearch_wpy=0;
				if(uniSearch_wpy>wpy_limit)
					uniSearch_wpy=wpy_limit;
				invalidate();
			}
			else if(pointer.state==TouchInfo.S_UP||pointer.state==TouchInfo.S_CANCEL)
				uniSearch_drag=UNI_SEARCH_DRAG_NONE;
		}
	}
	@Override public boolean performClick()
	{
		super.performClick();
		return true;
	}
	@Override public boolean onTouchEvent(MotionEvent e)
	{
		touchInfo.update(e);
		int np=e.getPointerCount(), size=touchInfo.size();

		if(DEBUG_TOUCH)//pointer tracking seems perfect		FIXME remove this
		{
			Log.e(TAG, String.format(loc, "%d FRAME: ti.size=%d, np=%d%s", frame_counter, size, np, size!=np?", !!!BROKEN!!!":""));
			for(int k=0;k<size;++k)
			{
				TouchInfo.Pointer ti=touchInfo.get(k);
				int gx=(int)(ti.x/ccGridX), gy=(int)(ti.y/ccGridY);
				ButtonIdx bIdx=getButton_px(ti.x, ti.y);
				String label=getLabel(bIdx.code);
				if(label==null)
					label="OOB";
				switch(ti.state)
				{
				case TouchInfo.S_DOWN:
					Log.e(TAG, String.format(loc, "%d:  [%d] DOWN (%f, %f), %s", frame_counter, ti.id, ti.x, ti.y, label));
					break;
				case TouchInfo.S_MOVED:
					{
						int gx0=(int)(ti.prevx/ccGridX), gy0=(int)(ti.prevy/ccGridY);
						ButtonIdx bIdx0=getButton_px(ti.prevx, ti.prevy);
						String label0=getLabel(bIdx0.code);
						if(label0==null)
							label0="OOB";
						Log.e(TAG, String.format(loc, "%d:  [%d] MOVE (%f, %f)->(%f, %f), %s->%s, (%d, %d)->(%d, %d)",
							frame_counter, ti.id, ti.prevx, ti.prevy, ti.x, ti.y, label0, label, gx0, gy0, gx, gy));
					}
					break;
				case TouchInfo.S_UP:
					Log.e(TAG, String.format(loc, "%d:  [%d] UP (%f, %f), %s", frame_counter, ti.id, ti.x, ti.y, label));
					break;
				case TouchInfo.S_CANCEL:
					Log.e(TAG, String.format(loc, "%d:  [%d] CANCEL (%f, %f), %s", frame_counter, ti.id, ti.x, ti.y, label));
					break;
				default:
					Log.e(TAG, String.format(loc, "%d:  [%d] UNRECOGNIZED STATE = %d", frame_counter, ti.id, ti.state));
					break;
				}
			}
		}//*/

		logState(frame_counter+" BEFORE");
		logCursorControl(frame_counter+" BEFORE");

		TouchInfo.Pointer ti_shift=null, ti=null, ti2=null;
		if(isHeld_shift)
		{
			int shift_idx=touchInfo.findPointerIndex(touchId_shift);
			if(shift_idx<touchInfo.size())
				ti_shift=touchInfo.get(shift_idx);
		}
		for(int k=0;k<size;++k)
		{
			TouchInfo.Pointer temp=touchInfo.get(k);
			if(ti_shift!=null&&temp.id==ti_shift.id)
				continue;
			if(ti==null)
				ti=temp;
			else if(ti2==null)
				ti2=temp;
			else
				break;
		}
		if(ti_shift==null||!ti_shift.isValid())
			isHeld_shift=false;
		if(mode_unicode)
		{
			int idx=touchInfo.findPointerIndex(0);//find 1st pointer
			if(idx==touchInfo.size())//ignore other pointers
				return true;
			TouchInfo.Pointer pointer=touchInfo.get(idx);
			if(pointer==null)
				return true;
			if(w<h)//portrait
			{
				if(pointer.starty<UNI_SEARCH_PORT_Y1)//search results box
					uniSearch_handle_resultBox(pointer);
					//uniSearch_handle_resultBox(0, w, 0, UNI_SEARCH_PORT_Y1, pointer);
				else if(pointer.starty<UNI_SEARCH_PORT_Y2)//search bar
				{
					bypass=true;
					toggleUnicode();//FIXME provide a visual clue on how to exit unicode search
				}
				else if(pointer.starty<kb_h)//keyboard
					uniSearch_handle_layout(backup_layout_port, 0, w, UNI_SEARCH_PORT_Y2, kb_h, pointer);
			}
			else//landscape
			{
				if(pointer.startx<UNI_SEARCH_LAND_X1)//QWERTY left
					uniSearch_handle_layout(backup_layout_land_left, 0, UNI_SEARCH_LAND_X1, 0, kb_h, pointer);
				else if(pointer.startx<UNI_SEARCH_LAND_X2)
				{
					if(pointer.starty<UNI_SEARCH_LAND_Y1)//search bar
						uniSearch_handle_resultBox(pointer);
						//uniSearch_handle_resultBox(UNI_SEARCH_LAND_X1, UNI_SEARCH_LAND_X2, 0, UNI_SEARCH_LAND_Y1, pointer);
					else//results box
					{
						bypass=true;
						toggleUnicode();//FIXME provide a visual clue on how to exit unicode search
					}
				}
				else//QWERTY right
					uniSearch_handle_layout(backup_layout_land_right, UNI_SEARCH_LAND_X2, w, 0, kb_h, pointer);
			}
			return true;
		}
		if(cursor_control==CC_DISABLED)//normal keyboard
		{
			for(int k=0;k<size;++k)
			{
				ti=touchInfo.get(k);
				if(ti!=null)
				{
					ButtonIdx
						bIdx0=getButton_px(ti.startx, ti.starty),
					//	bIdx0=getButton_px(ti.prevx, ti.prevy),

						bIdx=getButton_px(ti.x, ti.y);

					switch(ti.state)
					{
					case TouchInfo.S_DOWN://normal keyboard
						tap_x=bIdx.kx; tap_y=bIdx.ky;
						if(isHeld_shift)//start selection (and cursor) control immediately
						{
							if(ti.id!=touchId_shift)//always true
							{
								cursor_control=CC_SEL_END; touchId_selEnd=ti.id;
								tap_x=-1; tap_y=-1;//
							}
						}
						else if(bIdx.kx!=-1&&bIdx.ky!=-1)
						{
							showKeyPreview(bIdx, getLabel(bIdx.code));
							set_possible_timers(ti, bIdx);
							if(DEBUG_STATE)
								Log.e(TAG, String.format(loc, "BEFORE:  DOWN - %d%s%s", bIdx.code, bIdx.code==(MODMASK|KEY_SHIFT)?" SHIFT":"", isActive_shift?" ACTIVE":""));
							switch(bIdx.code)
							{
							case '\b':
								service.onKeyCallback(bIdx.code, 3);
								break;
							case MODMASK|KEY_SHIFT:
								isHeld_shift=true; touchId_shift=ti.id;
								if(isActive_shift)
								{
									isActive_shift=false;
									service.onKeyCallback(bIdx.code, 2);
									quickMode=0;
								}
								else
								{
									isActive_shift=true;//2022-07-07
									quickMode=1-quickMode;
									service.onKeyCallback(bIdx.code, 3);
								}
								break;
							}
							if(DEBUG_STATE)
								Log.e(TAG, String.format(loc, "AFTER:  DOWN - %d%s%s", bIdx.code, bIdx.code==(MODMASK|KEY_SHIFT)?" SHIFT":"", isActive_shift?" ACTIVE":""));
						}
						break;
					case TouchInfo.S_MOVED://normal keyboard
						//tap_x=bIdx.kx; tap_y=bIdx.ky;
						if(bIdx0.code==' '&&(Math.abs(ti.x-ti.startx)>20||Math.abs(ti.y-ti.starty)>20))
						{
							cursor_control=CC_LANG;
							touchId_switchLang=ti.id;
							tap_x=-1; tap_y=-1;
							hideKeyPreview();
							if(timerOn!=0)
							{
								timerOn=0;
								timer.cancel();
							}
						}
						else if(bIdx.ky!=bIdx0.ky||bIdx.kx!=bIdx0.kx)//turn on cursor control
						{
							tap_x=-1; tap_y=-1;
							{
								if(isHeld_shift&&ti.id!=touchId_shift)
								{
									cursor_control=CC_SEL_END;
									touchId_selEnd=ti.id;
								}
								else
								{
									cursor_control=CC_CURSOR;
									touchId_selStart=ti.id;
								}
								//ccX1=(int)(ti.x/ccGridX);
								//ccY1=(int)(ti.y/ccGridY);
								showKeyPreview(bIdx, "Cursor\nControl");
							}
							if(timerOn!=0)
							{
								timerOn=0;
								timer.cancel();
							}
						}
						break;
					case TouchInfo.S_UP://normal keyboard
					case TouchInfo.S_CANCEL:
						tap_x=-1; tap_y=-1;//
						if(ti.id==touchId_shift)
						{
							isHeld_shift=false;
							//isActive_shift=!isActive_shift;//2022-07-07
						}
						if(np==1&&cursor_control!=CC_DISABLED)//
							exitCursorControl();//
						hideKeyPreview();
						if(bIdx.code!=-1&&bIdx0.ky!=-1&&bIdx0.kx!=-1&&bIdx.ky==bIdx0.ky&&bIdx.kx==bIdx0.kx)
						{
							boolean handled=false;
							switch(timerOn)
							{
							case 0:
								//if(bIdx.code==SK_STD)
								//{
								//	isActive_sym=false;
								//	isActive_spk=false;
								//	isActive_fun=false;
								//}
								break;
							case 1://cancel the turbo timer
								if(turboTask.code==bIdx.code)
								{
									timerOn=0;
									timer.cancel();
								}
								break;
							case 2://cancel the long press timer
								if(isLongPressableKey(longPressTask.code)&&(isLongPressableKey(bIdx.code)))
								{
									handled=longPressTask.done;
								/*	if(!longPressTask.done)
									{
										switch(bIdx.code)
										{
										case MODMASK|KEY_SYM:
										case MODMASK|KEY_FUN:
											quickMode=1-quickMode;//quick press, long press
											break;
										}
										switch(bIdx.code)
										{
										case MODMASK|KEY_SYM:case MODMASK|KEY_FUN:case MODMASK|KEY_STD:
											isActive_sym=false;
											isActive_spk=false;
											isActive_fun=false;
											break;
										}
									}//*/
									timerOn=0;
									timer.cancel();
								}
								break;
							}

							if(bIdx.code==(MODMASK|KEY_UNICODE))
								toggleUnicode();
							else if(bypass)
								bypass=false;
							else if(bIdx.code!='\b'&&bIdx.code!=(MODMASK|KEY_SHIFT)&&!handled&&ti.state!=TouchInfo.S_CANCEL)//2022-07-07 this if condition was moved from the bottom of the code block
								service.onKeyCallback(bIdx.code, 3);

							//reset layout if quick mode
							if(quickMode!=0&&!isLongPressableKey(bIdx.code))
							{
								quickMode=0;
								isActive_shift=false;//2022-07-07
							}
						}
						break;
					}
				}
			}
		}//end CC_DISABLED
		else if(cursor_control==CC_LANG)//switch language
		{
			int idx=touchInfo.findPointerIndex(touchId_switchLang);
			if(idx>=size)
				cursor_control=CC_DISABLED;
			else
			{
				ti=touchInfo.get(idx);
				if(ti.state==TouchInfo.S_UP)
				{
					int nLayouts=layoutInfo[INFO_IDX_LAYOUT_COUNT];
					int choice=(int)(ti.x*nLayouts/w);
					switch_layout(choice, layoutHasExtension);
					cursor_control=CC_DISABLED;
				}
			}
		}
		else if(size>0)//CURSOR CONTROL
		{
			if(cursor_control==CC_CURSOR)//listen to one pointer
			{
				int idx=touchInfo.findPointerIndex(touchId_selStart);
				if(idx<size)
					ti=touchInfo.get(idx);
				if(ti==null)//reassign
				{
					touchId_selStart=findValidPointer(-1, -1);
					if(touchId_selStart==-1)
					{
						exitCursorControl();
						hideKeyPreview();
					}
				}
				else
				{
					switch(ti.state)
					{
					case TouchInfo.S_MOVED:
						applyCursorControl(ti, CC_CURSOR);
						break;
					case TouchInfo.S_UP:
					case TouchInfo.S_CANCEL://reassign or exit
						touchId_selStart=findValidPointer(-1, -1);
						if(touchId_selStart==-1)
						{
							exitCursorControl();
							hideKeyPreview();
						}
						break;
					}
				}
			}//end CC_CURSOR
			else if(cursor_control==CC_SEL_READY)
			{
				if(isHeld_shift)
				{
					touchId_selEnd=findValidPointer(touchId_shift, -1);
					if(touchId_selEnd!=-1)
						cursor_control=CC_SEL_END;
				}
				else//unreachable
				{
					exitCursorControl();
					hideKeyPreview();
				}
			}
			else//selection control: end/start/both
			{
				boolean
					control_end=(cursor_control&CC_SEL_END_MASK)==CC_SEL_END_MASK,
					control_start=(cursor_control&CC_SEL_START_MASK)==CC_SEL_START_MASK;
				touchId_selEnd	=selectionControl(control_end, control_start, touchId_selEnd, touchId_selStart, CC_SEL_END, CC_SEL_START);
				touchId_selStart=selectionControl(control_start, control_end, touchId_selStart, touchId_selEnd, CC_SEL_START, CC_SEL_END);
			}
		}//end cursor control

		logState(frame_counter+"  AFTER");
		logCursorControl(frame_counter+"  AFTER");
		++frame_counter;

		invalidate();
		return true;
	}
}
