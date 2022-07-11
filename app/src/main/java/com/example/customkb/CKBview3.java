package com.example.customkb;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
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
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class CKBview3 extends ViewGroup
{
	//ModKey - keys not present in unicode
	public static final int//should match ckb_mkeys.h
		MODMASK=0x80000000,

		KEY_NAB=0,

		KEY_LAYOUT=1,
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
		KEY_SELECTALL=35,

		KEY_MENU=36;
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
	public static final ModKey[] mkeys=new ModKey[]
	{
		new ModKey(KEY_NAB, ""),

		new ModKey(KEY_LAYOUT, "Layout"),
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
		new ModKey(KEY_SELECTALL, "Select\nAll"),

		new ModKey(KEY_MENU, "Menu"),
	};

	//dimensions
	public static int
		w, h,//screen dimensions
		kb_y1, kb_h;//keyboard height: h = kb_y1 + kb_h		kb_y1 is UNUSED

	//customkb
	public static final int
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

	//cursor control:
	//{(MSB) start, end, shift (LSB)}
	//000	CC_DISABLED
	//001	CC_SEL_READY
	//011	CC_SEL_END
	//101	CC_SEL_START
	//111	CC_SEL_BOTH
	//110	CC_CURSOR
	public static final int
		CC_DISABLED=0,
		CC_SEL_READY=1,//touchId_shift
		CC_SEL_END=3,//[touchId_shift,] touchId_end
		CC_SEL_END_MASK=2,
		CC_SEL_START=5,//[touchId_shift,] touchId_start
		CC_SEL_START_MASK=4,
		CC_SEL_BOTH=7,//[touchId_shift,] touchId_start & touchId_end
		CC_CURSOR=6;//touchId_start
	public int cursor_control=CC_DISABLED;
	public boolean isHeld_shift=false;//finger holds shift, needed to initiate selection control
	int touchId_shift=-1, touchId_selStart=-1, touchId_selEnd=-1;

	//graphics	TODO load colors from config
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
	public int[] theme_colors;
	//int color_clear=0x00202020,
	//	color_button=0xC0707070, color_button_fast=0xC0237CE9, color_button_pressed=0xC0FF8040,
	//	color_labels=0xFFFFFFFF,//0xAARRGGBB
	//	color_shadow=0xC00000FF, color_shadow_letters=0xC0FF0000, color_shadow_special=0xC000FF00,
	//	color_preview_text=0xC0000000;//0x80FFFFFF 0x80000000

	float textSize=32, textSize_long=20, shadowOffset=4;
	Paint textPaint, letterPaint, specialButtonPaint, textPaintDebug, penPaint, buttonPaint;
	TextPaint wrapPaint;
	TextView preview;
	PopupWindow previewPopup;
	final RectF rectf=new RectF();
	final Rect mBounds=new Rect();
	final TouchInfo touchInfo=new TouchInfo();

	//keyboard
	public static class ButtonIdx
	{
		int ky, kx, code,
			x1, x2, y1, y2;//button bounds
		ButtonIdx(){kx=-1; ky=-1; code=MODMASK|KEY_NAB;}
		boolean invalid(){return kx==-1||ky==-1;}
	}
	final ArrayList<int[]> layout=new ArrayList<>();
	int tap_x=-1, tap_y=-1;//layout coordinates of currently held button
	int ccGridX, ccGridY;
	int quickMode=0;
	boolean isActive_ctrl, isActive_alt, isActive_shift, isActive_layout;
	CKBservice service;

	//timing
	final int
		turboStart_ms=600, turbo_ms=35,
		longPress_ms=400;
	Timer timer=new Timer();
	int timerOn=0;//0: off, 1: turboTask, 2: longPressTask
	int touchId_timer;
	public int pend_switch=0;//1: switch layout, 2: switch language
	class TurboTask extends TimerTask
	{
		public int code;
		TurboTask(int _code){code=_code;}
		@Override public void run()//this is run every time turbo is triggered
		{
			service.onKeyCallback(code, 3);
		}
	}
	class LongPressTask extends TimerTask
	{
		public int code;
		public boolean done=false;
		LongPressTask(int _code){code=_code;}
		@Override public void run()//this is run when the period is expired
		{
			service.onKeyCallback(code, 1);
			switch(code)
			{
			//sends pure down
		//	case MODMASK|KEY_CTRL:
		//		isActive_ctrl=true;
		//		service.onKeyCallback(MODMASK|KEY_CTRL, 1);
		//		break;

			case MODMASK|KEY_ALT:
				isActive_alt=true;
				service.onKeyCallback(MODMASK|KEY_ALT, 1);
				break;

			//stays on (green mode), otherwise layout is reset to std
			case MODMASK|KEY_SHIFT:
				isActive_shift=true;
				service.onKeyCallback(MODMASK|KEY_SHIFT, 1);
				quickMode=0;
				break;

			case MODMASK|KEY_LAYOUT:
				pend_switch=2;
				postInvalidate();
				//switchLanguage();//CRASH
				break;
			}
			done=true;
		}
	}
	TurboTask turboTask=new TurboTask(MODMASK|KEY_NAB);//TODO: check if initialization needed, probably not
	LongPressTask longPressTask=new LongPressTask(MODMASK|KEY_NAB);

	//debug tools
	public static final String TAG="customkb";
	public static final boolean
		DEBUG_ACCUMULATE=false,//was true
		DEBUG_TOUCH		=false,
		DEBUG_CC		=false,
		DEBUG_STATE		=false,
		DEBUG_MODE		=false,
		DEBUG_COLORS	=false;
	public static final ArrayList<String> urgentMsg=new ArrayList<>();//when logcat isn't enough
	public static final Locale loc=new Locale("US");
	int frame_counter=0;


	//boilerplate
	public CKBview3(Context context){super(context);}
	public CKBview3(Context context, AttributeSet attrs){super(context, attrs);}
	public CKBview3(Context context, AttributeSet attrs, int defStyle){super(context, attrs, defStyle);}
	@Override protected void onLayout(boolean changed, int l, int t, int r, int b){}

	//util
	//public static boolean isPortrait(){return w<h;}
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
	public static String getLabel(int code)
	{
		if((code&MODMASK)!=0)
		{
			code&=~MODMASK;
			if(code>=mkeys.length)
				return "INVALID";
			ModKey key=mkeys[code];
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
		case 127:	return "Del";
		}
		StringBuilder b=new StringBuilder();
		b.appendCodePoint(code);
		return b.toString();
		//return String.format(loc, "%c", code);
	}
	public ButtonIdx getButton_layout(int tap_x, int tap_y)
	{
		ButtonIdx button=new ButtonIdx();
		if(tap_y>=0&&tap_y<layout.size())
		{
			int[] row=layout.get(tap_y);
			button.kx=tap_x;
			button.ky=tap_y;
			button.y1=row[0];
			button.y2=row[1];
			int idx=3+tap_x*2;
			button.x1=row[idx-1];
			button.code=row[idx];
			button.x2=row[idx+1];
		}
		return button;
	}
	public ButtonIdx getButton_px(float x, float y)//linear search
	{
		ButtonIdx button=new ButtonIdx();
		for(int ky=0, nRows=layout.size();ky<nRows;++ky)
		{
			int[] row=layout.get(ky);
			int y1=row[0], y2=row[1];
			if(y>=y1&&y<y2)
			{
				for(int kx=3;kx+1<row.length;kx+=2)
				{
					int x1=row[kx-1], x2=row[kx+1];
					if(x>=x1&&x<x2)
					{
						button.ky=ky;
						button.kx=(kx-3)/2;
						button.code=row[kx];
						button.x1=x1;
						button.x2=x2;
						button.y1=y1;
						button.y2=y2;
					}
				}
			}
		}
		return button;
	}

	//implementation
	private void get_layout(int nRows)
	{
		layout.clear();
		layout.ensureCapacity(nRows);
		for(int k=0;k<nRows;++k)
		{
			int[] row=CKBnativelib.getRow(k);
			if(row==null)
				addError(String.format(loc, "Java: Row %d == null", k));
			else
				layout.add(row);
		}

		ViewGroup.LayoutParams lp=getLayoutParams();
		lp.height=kb_h=CKBnativelib.getKbHeight();
		setLayoutParams(lp);
	}
	public void switchLayout()
	{
		int nRows=CKBnativelib.nextLayout();
		if(nRows<1)
		{
			addError("Failed to switch layout");
			return;
		}
		get_layout(nRows);
	}
	public void switchLanguage()
	{
		int nRows=CKBnativelib.nextLanguage();
		if(nRows<1)
		{
			addError("Failed to switch language");
			return;
		}
		get_layout(nRows);
	}
	public void initCKB(CKBservice _service)
	{
		try
		{
			if(_service==null)
				throw new Exception("CKBservice == null");

			service=_service;

			DisplayMetrics metrics=Resources.getSystem().getDisplayMetrics();
			w=metrics.widthPixels; h=metrics.heightPixels;

			int minDim=Math.min(w, h);
			textSize=minDim/20.f;//33.75.f
			textSize_long=minDim/40.f;

			textPaint=new Paint();
			textPaint.setAntiAlias(true);
			textPaint.setTextAlign(Paint.Align.CENTER);
			//textPaint.setColor(color_labels);
			letterPaint=new Paint(textPaint);
			specialButtonPaint=new Paint(textPaint);

			textPaintDebug=new Paint();
			textPaintDebug.setTextSize(32);

			penPaint=new Paint();
			penPaint.setStyle(Paint.Style.STROKE);
			penPaint.setStrokeWidth(3);

			buttonPaint=new Paint();
			//buttonPaint.setColor(color_button);

			shadowOffset=minDim/270.f;
			//textPaint			.setShadowLayer(shadowOffset*0.5f, shadowOffset, shadowOffset, color_shadow);//0xAARRGGBB
			//letterPaint			.setShadowLayer(shadowOffset*0.5f, shadowOffset, shadowOffset, color_shadow_letters);
			//specialButtonPaint	.setShadowLayer(shadowOffset*0.5f, shadowOffset, shadowOffset, color_shadow_special);

			wrapPaint=new TextPaint();
			wrapPaint.setTextSize(16*getResources().getDisplayMetrics().density);
			wrapPaint.setColor(0xFF000000);

			setWillNotDraw(false);//for ViewGroup to call o n D r a w
			setClipChildren(false);

			LayoutInflater inflater=service.getLayoutInflater();
			preview=(TextView)inflater.inflate(R.layout.popup_view, null);

			preview.setGravity(Gravity.CENTER);
			preview.setTextSize(textSize/2);
			//preview.setTextColor(color_preview_text);

			//Drawable bk=DrawableCompat.wrap(preview.getBackground()).mutate();
			//DrawableCompat.setTint(bk, color_button_pressed);//change preview bkColor

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
	public void startCKB(EditorInfo info)
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
				break;
			case InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS:
			case InputType.TYPE_TEXT_VARIATION_EMAIL_SUBJECT:
				mode=MODE_EMAIL;
				break;
			case InputType.TYPE_TEXT_VARIATION_PASSWORD:
			case InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD:
			case InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD:
				mode=MODE_PASSWORD;
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

		int nRows=CKBnativelib.init(mode, numeric_signed||numeric_decimal?1:0, w, h);
		int nErrors=CKBnativelib.getNErrors();
		//addError("init: "+nErrors+" errors");
		if(nErrors>0)
		{
			service.displayToast(CKBnativelib.getError(0));//toast just the first error for now
			for(int ke=0;ke<nErrors;++ke)
				addError(CKBnativelib.getError(ke));
			//service.displayToast("Failed to initialize keyboard, opening settings");

			Intent intent=new Intent(service, CKBactivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			service.startActivity(intent);
			return;
		}

		theme_colors=CKBnativelib.getColors();
		if(theme_colors==null)
		{
			Log.e(TAG, "Failed to retrieve theme colors, fallback to random");
			theme_colors=new int[THEME_COLOR_COUNT];
			for(int k=0;k<THEME_COLOR_COUNT;++k)
				theme_colors[k]=(int)(255*Math.random())<<24|(int)(255*Math.random())<<16|(int)(255*Math.random())<<8|(int)(255*Math.random());
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

		get_layout(nRows);

		kb_y1=h-kb_h;
		ccGridX=ceilMultiple((int)(textSize*1.5), w);
		ccGridY=kb_h/layout.size();

		ViewGroup.LayoutParams lp=getLayoutParams();
		if(lp==null)
		{
			Log.e(TAG, "CKBview3.LayoutParams == null");
			lp=new ViewGroup.LayoutParams(w, kb_h);
		}
		else
		{
			lp.width=w;
			lp.height=kb_h;
		}
		setLayoutParams(lp);
		invalidate();
	}
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
			//if(y1<height)
			//{
			//	if(x1<w/2)
			//		x1+=width;
			//	else
			//		x1-=width;
			//}
			//else
				y1-=height;
			//x1=(x1+x2)/2-width/2;
			x1=clamp(0, (x1+x2-width)/2, w-width);
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
		//preview.setText("");
	}

	//drawing functions
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
	@Override public void onDraw(Canvas canvas)
	{
		if(pend_switch!=0)
		{
			if(pend_switch==1)
				switchLayout();
			else if(pend_switch==2)
				switchLanguage();
			pend_switch=0;
		}
		float radius=10;

		canvas.drawColor(theme_colors[COLOR_BACKGROUND]);//same everywhere
		//canvas.drawColor(color_clear);
		if(cursor_control!=CC_DISABLED)
		{
			if(multiline)
			{
				for(int ky=0;ky<kb_h;ky+=ccGridY)//draw grid
				{
					for(int kx=0;kx<w;kx+=ccGridX)
					{
						rectf.set(kx+radius, ky+radius, kx+ccGridX-radius, ky+ccGridY-radius);
						canvas.drawRoundRect(rectf, radius, radius, buttonPaint);
					}
				}
			}
			else
			{
				for(int kx=0;kx<w;kx+=ccGridX)
				{
					rectf.set(kx+radius, radius, kx+ccGridX-radius, kb_h-radius);
					canvas.drawRoundRect(rectf, radius, radius, buttonPaint);
				}
			}

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
		else//normal keyboard
		{
			for(int ky=0, nRows=layout.size();ky<nRows;++ky)
			{
				int[] row=layout.get(ky);
				int y1=row[0], y2=row[1], dy=y2-y1, nButtons=(row.length-3)/2;
				for(int kx=0;kx<nButtons;++kx)
				{
					int idx=3+kx*2,
						x1=row[idx-1], code=row[idx], x2=row[idx+1];
					if(code==(MODMASK|KEY_NAB))
						continue;

					int dx=x2-x1;
					canvas.translate(x1, y1);

					rectf.set(radius, radius, dx-radius, dy-radius);
					int color=buttonPaint.getColor(), color2=color;
					boolean colorWasSet=false;
					if((code&MODMASK)!=0)
					{
						switch(code&~MODMASK)//check long-pressable keys
						{
						case KEY_LAYOUT:if(isActive_layout){colorWasSet=true; color2=theme_colors[COLOR_BUTTON_PRESSED];}	break;
						case KEY_CTRL:	if(isActive_ctrl){	colorWasSet=true; color2=theme_colors[COLOR_BUTTON_PRESSED];}	break;
						case KEY_ALT:	if(isActive_alt){	colorWasSet=true; color2=theme_colors[COLOR_BUTTON_PRESSED];}	break;

						case KEY_CAPSLOCK:
						case KEY_SHIFT:	if(isActive_shift){	colorWasSet=true; color2=theme_colors[COLOR_BUTTON_PRESSED];}	break;
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
					if(DEBUG_STATE)//TODO REMOVE AT RELEASE
					{
						if(code==(MODMASK|KEY_SHIFT)&&tap_y!=-1&&tap_x!=-1)
						{
							int[] row2=layout.get(tap_y);
							int temp_idx=3+tap_x*2, x1_b=row2[temp_idx-1], code_b=row2[temp_idx], x2_b=row2[temp_idx+1];
							if(code_b==(MODMASK|KEY_NAB))
								Log.e(TAG, String.format(loc, "%d  c1=0x%08X, c2=0x%08X, tap(%d, %d)", frame_counter, color, color2, tap_x, tap_y));
							else
							{
								String label2=getLabel(code_b);
								Log.e(TAG, String.format(loc, "%d  c1=0x%08X, c2=0x%08X, tap(%d, %d): %s", frame_counter, color, color2, tap_x, tap_y, label2));
							}
						}
					}
					canvas.drawRoundRect(rectf, radius, radius, buttonPaint);
					if(colorWasSet)
						buttonPaint.setColor(color);

					float currentSize;
					Paint p=null;
					int code_case=code;
					if((code&MODMASK)!=0)//
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

						p.getTextBounds("Ig", 0, 2, mBounds);//https://stackoverflow.com/questions/3153870/canvas-drawtext-does-not-print-linebreak
						float
							lineHeight=(int)((float)mBounds.height()*1.2f),
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
				}//end row loop
			}//end keyboard loop
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

		int xPos=w/4;//when logcat isn't enough
		if(android.os.Build.VERSION.SDK_INT>=android.os.Build.VERSION_CODES.M)//multiline text
		{
			float py=0;
			for(int ky=0;ky<urgentMsg.size();++ky)
			{
				String str=urgentMsg.get(ky);
				StaticLayout.Builder builder=StaticLayout.Builder.obtain(str, 0, str.length(), wrapPaint, w-xPos);
				StaticLayout sl=builder.build();
				int textHeight=sl.getHeight();
				py+=textHeight;
				canvas.translate(xPos, py);
				sl.draw(canvas);
				canvas.translate(-xPos, -py);
			}
		}
		else//single line text
		{
			float py=textSize;//debug
			for(int ky=0;ky<urgentMsg.size();++ky)
			{
				canvas.drawText(urgentMsg.get(ky), w*0.25f, py, textPaintDebug);
				py+=textSize;
			}
		}
	}

	//timing
	boolean isTurboKey(int code)
	{
		switch(code)
		{
		case MODMASK|KEY_NAB:
		case 27:
		case MODMASK|KEY_HOME:case MODMASK|KEY_END:
		case MODMASK|KEY_SHIFT:case MODMASK|KEY_CTRL:case MODMASK|KEY_ALT:
		case MODMASK|KEY_INSERT:
		case MODMASK|KEY_CAPSLOCK:case MODMASK|KEY_NUMLOCK:case MODMASK|KEY_SCROLLLOCK:
	//	case MODMASK|KEY_MENU:
	//	case MODMASK|KEY_STD:case MODMASK|KEY_SYM:case MODMASK|KEY_SPK:case MODMASK|KEY_FUN:
	//	case MODMASK|KEY_TRANSPARENT:
		case MODMASK|KEY_SETTINGS:
	//	case MODMASK|KEY_UNICODE:
		case MODMASK|KEY_LAYOUT:
			return false;
		}
		return true;
	}
	boolean isLongPressableKey(int code)
	{
		switch(code)
		{
		case MODMASK|KEY_SHIFT://sends pure down, stays on

		case MODMASK|KEY_LAYOUT://switches language

	//	case MODMASK|KEY_CTRL:case SK_ALT://sends pure down
	//	case MODMASK|KEY_SYM:
	//	case MODMASK|KEY_SPK:
	//	case MODMASK|KEY_FUN://stays on (green mode), otherwise layout is reset to std
			return true;
		}
		return false;
	}

	//cursor control
	void exitCursorControl()
	{
		cursor_control=CC_DISABLED;
		isActive_shift=false;	service.onKeyCallback(MODMASK|KEY_SHIFT, 2);
	//	isActive_ctrl=false;	service.onKeyCallback(MODMASK|KEY_CTRL, 2);
	//	isActive_alt=false;		service.onKeyCallback(MODMASK|KEY_ALT, 2);
		isActive_layout=false;
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
	@Override public boolean performClick()
	{
		super.performClick();
		return true;
	}
	@Override public boolean onTouchEvent(MotionEvent e)
	{
		touchInfo.update(e);
		int np=e.getPointerCount(), size=touchInfo.size();

		if(DEBUG_TOUCH)//tracking seems perfect
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
		//Collections.sort(touchInfo, (o1, o2)->Integer.compare(o1.id, o2.id));//sort touchInfo by order of appearance
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
		if(cursor_control==CC_DISABLED)//normal keyboard
		{
			for(int k=0;k<size;++k)
			{
				ti=touchInfo.get(k);
				if(ti!=null)
				{
					ButtonIdx
						bIdx0=getButton_px(ti.prevx, ti.prevy),
						bIdx=getButton_px(ti.x, ti.y);
					switch(ti.state)
					{
					case TouchInfo.S_DOWN://normal keyboard
						//touchId_current=ti.id;//
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
							try
							{
								if(isTurboKey(bIdx.code))
								{
									if(timerOn==0)
									{
										timerOn=1; touchId_timer=ti.id;
										turboTask=new TurboTask(bIdx.code);
										timer=new Timer();
										timer.schedule(turboTask, turboStart_ms, turbo_ms);
									}
								}
								else if(isLongPressableKey(bIdx.code))
								{
									if(timerOn==0)
									{
										timerOn=2; touchId_timer=ti.id;
										longPressTask=new LongPressTask(bIdx.code);
										timer=new Timer();
										timer.schedule(longPressTask, longPress_ms);
									}
								}
							}
							catch(Exception ex)
							{
								addError(ex);
							}
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
									isActive_shift=false;	service.onKeyCallback(MODMASK|KEY_SHIFT, 2);
									quickMode=0;
									//kb.selectLayout(MODMASK|KEY_STD);
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
						if(bIdx.ky!=bIdx0.ky||bIdx.kx!=bIdx0.kx)//turn on cursor control
						{
							tap_x=-1; tap_y=-1;
							if(isHeld_shift&&ti.id!=touchId_shift)
							{
								cursor_control=CC_SEL_END; touchId_selEnd=ti.id;
							}
							else
							{
								cursor_control=CC_CURSOR; touchId_selStart=ti.id;
							}
							//ccX1=(int)(ti.x/ccGridX);
							//ccY1=(int)(ti.y/ccGridY);
							showKeyPreview(bIdx, "Cursor\nControl");
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

							if(bIdx.code!='\b'&&bIdx.code!=(MODMASK|KEY_SHIFT)&&!handled&&ti.state!=TouchInfo.S_CANCEL)//2022-07-07 this if condition was moved from the bottom of the code block
								service.onKeyCallback(bIdx.code, 3);

							//reset layout if quick mode
							if(quickMode!=0&&!isLongPressableKey(bIdx.code))
							{
								quickMode=0;
								//kb.exitQuickMode();
								isActive_shift=false;//2022-07-07
							}
						}
						break;
					}
				}
			}
		}//end CC_DISABLED
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
