/*  CKBview2.java - The CKB ViewGroup: keyboard implementation
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

public class CKBview2 extends ViewGroup//need to clear setWillNotDraw() to draw
//public class CKBview2 extends View//can't add TextView
{
	//debug tools
	public static final boolean
		DEBUG		=true,
		DEBUG_TOUCH	=false,
		DEBUG_CC	=false,
		DEBUG_STATE	=false,
		DEBUG_MODE	=false;
	public static final String TAG="CKBview2";
	public static ArrayList<String> urgentMsg=new ArrayList<>();//when logcat isn't enough
	public static void addError(Exception e)
	{
		if(DEBUG)
		{
		//	urgentMsg.clear();
			urgentMsg.add(e.getMessage());
		}
		Log.e(TAG, "exception", e);
	}
	public static void addError(String str)
	{
		if(DEBUG)
		{
			urgentMsg.add(str);
		}
		Log.e(TAG, str);
	}

	//dimensions
	public static int w, h, kb_y1, kb_h;

	//mode
	public static final int
		MODE_TEXT=0, MODE_PASSWORD=1, MODE_URL=2, MODE_EMAIL=3,//text modes
		MODE_NUMBER=4, MODE_PHONE_NUMBER=5, MODE_NUMERIC_PASSWORD=6;//numeric modes
	public int mode;
	public static final int
		CAP_NONE		=0,
		CAP_WORDS		=1,
		CAP_SENTENCES	=2,//unused
		CAP_ALL			=3;
	public int capital=CAP_NONE;//text modes
	public boolean multiline=false;//text modes
	public boolean
		numeric_signed=false,//extra buttons: + -
		numeric_decimal=false;//extra buttons: . /

	public static final int
		SK_NAB=-39,//starting from -SK_START_OFFSET

		//CKB-specific
		SK_STD=-38, SK_SYM=-37, SK_SPK=-36, SK_FUN=-35, SK_TRANSPARENT=-34,
		SK_SETTINGS=-33, SK_UNICODE=-32,

		//PC keyLabels
		SK_DELETE	=-31,	SK_INSERT	=-30,
		SK_CAPS_LOCK=-29,	SK_SHIFT	=-28,
		SK_CTRL		=-27,	SK_ALT		=-26,
		SK_HOME		=-25,	SK_END		=-24,
		SK_PGUP		=-23,	SK_PGDN		=-22,
		SK_F1=-21, SK_F2=-20, SK_F3=-19, SK_F4=-18,
		SK_F5=-17, SK_F6=-16, SK_F7=-15, SK_F8=-14,
		SK_F9=-13, SK_F10=-12, SK_F11=-11, SK_F12=-10,
		SK_PRINTSCREEN=-9, SK_PAUSE=-8, SK_NUM_LOCK=-7, SK_SCROLL_LOCK=-6,
		SK_LEFT=-5, SK_RIGHT=-4, SK_UP=-3, SK_DOWN=-2,
		SK_MENU=-1,

		//special ASCII
		SK_BACKSPACE='\b', SK_TAB='\t', SK_ENTER='\n', SK_ESCAPE=27, SK_SPACE=' ',

		SK_START_OFFSET=-SK_NAB;

	public static class KeyLabel
	{
		public String label;
		public int code;//ASCII + special keys
		public KeyLabel(String _label, int _code)
		{
			label=_label;
			code=_code;
		}
	}
	public static KeyLabel[] keyLabels=
	{//disable inlay hints in Android Studio
		new KeyLabel(""			, SK_NAB		),
		new KeyLabel("std"		, SK_STD		),
		new KeyLabel("sym"		, SK_SYM		),
		new KeyLabel("spk"		, SK_SPK		),
		new KeyLabel("fun"		, SK_FUN		),
		new KeyLabel("Transp"	, SK_TRANSPARENT),
		new KeyLabel("Settings"	, SK_SETTINGS	),//icon: gear
		new KeyLabel(":)"		, SK_UNICODE	),//icon: smiley

		new KeyLabel("Del"		, SK_DELETE			),//icon: backspace flipped
		new KeyLabel("Ins"		, SK_INSERT			),
		new KeyLabel("Caps\nLock", SK_CAPS_LOCK		),
		new KeyLabel("Shift"	, SK_SHIFT			),
		new KeyLabel("Ctrl"		, SK_CTRL			),
		new KeyLabel("Alt"		, SK_ALT			),
		new KeyLabel("Home"		, SK_HOME			),
		new KeyLabel("End"		, SK_END			),
		new KeyLabel("PgUp"		, SK_PGUP			),
		new KeyLabel("PgDn"		, SK_PGDN			),
		new KeyLabel("F1"		, SK_F1				),
		new KeyLabel("F2"		, SK_F2				),
		new KeyLabel("F3"		, SK_F3				),
		new KeyLabel("F4"		, SK_F4				),
		new KeyLabel("F5"		, SK_F5				),
		new KeyLabel("F6"		, SK_F6				),
		new KeyLabel("F7"		, SK_F7				),
		new KeyLabel("F8"		, SK_F8				),
		new KeyLabel("F9"		, SK_F9				),
		new KeyLabel("F10"		, SK_F10			),
		new KeyLabel("F11"		, SK_F11			),
		new KeyLabel("F12"		, SK_F12			),
		new KeyLabel("PrScr"	, SK_PRINTSCREEN	),
		new KeyLabel("Pause"	, SK_PAUSE			),
		new KeyLabel("Num\nLock", SK_NUM_LOCK		),
		new KeyLabel("Scroll\nLock", SK_SCROLL_LOCK	),
		new KeyLabel("Left"		, SK_LEFT			),//icon: left arrow
		new KeyLabel("Right"	, SK_RIGHT			),//icon: right arrow
		new KeyLabel("Up"		, SK_UP				),//icon: up arrow
		new KeyLabel("Down"		, SK_DOWN			),//icon: down arrow
		new KeyLabel("Menu"		, SK_MENU			),//icon: menu

		new KeyLabel("NUL"		, '\0'	),
		new KeyLabel("SOH"		, 1		),
		new KeyLabel("STX"		, 2		),
		new KeyLabel("ETX"		, 3		),
		new KeyLabel("EOT"		, 4		),
		new KeyLabel("ENQ"		, 5		),
		new KeyLabel("ACK"		, 6		),
		new KeyLabel("BEL"		, 7		),
		new KeyLabel("Backspace", '\b'	),//8	//icon: backspace
		new KeyLabel("Tab"		, '\t'	),//9	//icon: tab arrows
		new KeyLabel("Enter"	, '\n'	),//10	//icon: enter arrow
		new KeyLabel("VTab"		, 11	),
		new KeyLabel("Form\nFeed", 12	),
		new KeyLabel("CR"		, '\r'	),//13
		new KeyLabel("SO"		, 14	),
		new KeyLabel("SI"		, 15	),
		new KeyLabel("DLE"		, 16	),
		new KeyLabel("DC1"		, 17	),
		new KeyLabel("DC2"		, 18	),
		new KeyLabel("DC3"		, 19	),
		new KeyLabel("DC4"		, 20	),
		new KeyLabel("NAK"		, 21	),
		new KeyLabel("SYN"		, 22	),
		new KeyLabel("ETB"		, 23	),
		new KeyLabel("CAN"		, 24	),
		new KeyLabel("EM"		, 25	),
		new KeyLabel("SUB"		, 26	),
		new KeyLabel("Esc"		, 27	),//escape
		new KeyLabel("FS"		, 28	),
		new KeyLabel("GS"		, 29	),
		new KeyLabel("RS"		, 30	),
		new KeyLabel("US"		, 31	),
	};
	public String getLabel(int code)
	{
		int idx=SK_START_OFFSET+code;
		if(idx>=0&&idx<keyLabels.length)
		{
			//if(!multiline&&code==SK_ENTER)
			//	return "Done";
			return keyLabels[idx].label;
		}
		try
		{
			//if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.KITKAT)
			//	return Character.getName(code);
			if(code>=0&&code<0x110000)
				return new String(Character.toChars(code));
		}
		catch(Exception e)
		{
			addError(e);
		}
		Log.e(TAG, String.format("Unrecognized codePoint %d", code));
		return null;
	}

	//CKB v3
	//TEXT LAYOUTS
	//portrait layouts
	public static int[][] layout_p_std=		//standard layout
	{
		{'1', '2', '3', '4', '5', '6', '7', '8', '9', '0'},
		{'q', 'w', 'e', 'r', 't', 'y', 'u', 'i', 'o', 'p'},
		{SK_NAB, 'a', 's', 'd', 'f', 'g', 'h', 'j', 'k', 'l', SK_NAB},
		{SK_SHIFT, 'z', 'x', 'c', 'v', 'b', 'n', 'm', SK_BACKSPACE},
		{SK_SYM, SK_SPK, SK_SPACE, '.', SK_ENTER}
	};
	public static int[][] layout_p_std_shift=
	{
		{'1', '2', '3', '4', '5', '6', '7', '8', '9', '0'},
		{'Q', 'W', 'E', 'R', 'T', 'Y', 'U', 'I', 'O', 'P'},
		{SK_NAB, 'A', 'S', 'D', 'F', 'G', 'H', 'J', 'K', 'L', SK_NAB},
		{SK_SHIFT, 'Z', 'X', 'C', 'V', 'B', 'N', 'M', SK_BACKSPACE},
		{SK_SYM, SK_SPK, SK_SPACE, ',', SK_ENTER}
	};
	public static float[][] layout_p_std_widths=
	{
		{1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
		{1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
		{0.5f, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0.5f},
		{1.5f, 1, 1, 1, 1, 1, 1, 1, 1.5f},
		{1.25f, 1.25f, 5, 1, 1.5f},
	};
	public static int[][] layout_p_url_std=		//standard layout for URLs/search/email
	{
		{'1', '2', '3', '4', '5', '6', '7', '8', '9', '0'},
		{'q', 'w', 'e', 'r', 't', 'y', 'u', 'i', 'o', 'p'},
		{SK_NAB, 'a', 's', 'd', 'f', 'g', 'h', 'j', 'k', 'l', SK_NAB},
		{SK_SHIFT, 'z', 'x', 'c', 'v', 'b', 'n', 'm', SK_BACKSPACE},
		{SK_SYM, SK_SPK, '/', SK_SPACE, '@', '.', SK_ENTER}
	};
	public static int[][] layout_p_url_std_shift=
	{
		{'1', '2', '3', '4', '5', '6', '7', '8', '9', '0'},
		{'Q', 'W', 'E', 'R', 'T', 'Y', 'U', 'I', 'O', 'P'},
		{SK_NAB, 'A', 'S', 'D', 'F', 'G', 'H', 'J', 'K', 'L', SK_NAB},
		{SK_SHIFT, 'Z', 'X', 'C', 'V', 'B', 'N', 'M', SK_BACKSPACE},
		{SK_SYM, SK_SPK, '/', SK_SPACE, '@', ',', SK_ENTER}
	};
	public static float[][] layout_p_url_std_widths=
	{
		{1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
		{1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
		{0.5f, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0.5f},
		{1.5f, 1, 1, 1, 1, 1, 1, 1, 1.5f},
		{1, 1, 1, 4, 1, 1, 1},
	};
	public static int[][] layout_p_sym=		//symbols
	{
		{'!', '@', '#', '$', '%', '^', '&', '*', '(', ')'},//TODO: ORGANIZE
		{'`', '~', '-', '_', '=', '+', '[', ']', '{', '}'},
		{';', ':', '\'', '\"', ',', '.', '<', '>', '/', '?'},
		{'\\', '|', SK_TAB, SK_CTRL, SK_ALT, SK_HOME, SK_END, SK_UNICODE, SK_BACKSPACE},
		{SK_STD, SK_SPK, SK_SPACE, SK_FUN, SK_ENTER}
	};
	public static float[][] layout_p_sym_widths=
	{
		{1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
		{1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
		{1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
		{1, 1, 1, 1, 1, 1, 1, 1, 2},
		{1.25f, 1.25f, 5, 1, 1.5f},
	};
	public static int[][] layout_p_spk=		//special keys
	{
		{SK_ESCAPE, SK_TAB, SK_CAPS_LOCK, SK_CTRL, SK_ALT, SK_HOME, SK_END, SK_PGUP, SK_PGDN, SK_DELETE},//TODO: ORGANIZE ROW
		{'q', 'w', 'e', 'r', 't', 'y', 'u', 'i', 'o', 'p'},
		{SK_NAB, 'a', 's', 'd', 'f', 'g', 'h', 'j', 'k', 'l', SK_NAB},
		{SK_SHIFT, 'z', 'x', 'c', 'v', 'b', 'n', 'm', SK_BACKSPACE},
		{SK_SYM, SK_STD, SK_SPACE, SK_SETTINGS, SK_ENTER}
	//	{SK_SYM, SK_STD, SK_SPACE, SK_FUN, SK_ENTER}
	};
	public static int[][] layout_p_spk_shift=
	{
		{SK_ESCAPE, SK_TAB, SK_CAPS_LOCK, SK_CTRL, SK_ALT, SK_HOME, SK_END, SK_PGUP, SK_PGDN, SK_DELETE},//TODO: ORGANIZE ROW
		{'Q', 'W', 'E', 'R', 'T', 'Y', 'U', 'I', 'O', 'P'},
		{SK_NAB, 'A', 'S', 'D', 'F', 'G', 'H', 'J', 'K', 'L', SK_NAB},
		{SK_SHIFT, 'Z', 'X', 'C', 'V', 'B', 'N', 'M', SK_BACKSPACE},
		{SK_SYM, SK_STD, SK_SPACE, SK_SETTINGS, SK_ENTER}
	};
	public static float[][] layout_p_spk_widths=
	{
		{1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
		{1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
		{0.5f, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0.5f},
		{1.5f, 1, 1, 1, 1, 1, 1, 1, 1.5f},
		{1.25f, 1.25f, 5, 1, 1.5f},
	};
	public static int[][] layout_p_fun=		//function keys
	{
		{SK_ESCAPE,		SK_F1, SK_F2,	SK_F3,	SK_F4,	SK_HOME, SK_UP, SK_PGUP},
		{SK_TAB,		SK_F5, SK_F6,	SK_F7,	SK_F8,	SK_LEFT, SK_NAB, SK_RIGHT},
		{SK_CAPS_LOCK,	SK_F9, SK_F10,	SK_F11,	SK_F12, SK_END, SK_DOWN, SK_PGDN},
		{SK_SHIFT,		SK_CTRL, SK_ALT, SK_INSERT, SK_DELETE, SK_NUM_LOCK, SK_SCROLL_LOCK, SK_BACKSPACE},
		{SK_SYM, SK_SPK, SK_SPACE, SK_STD, SK_ENTER}
	};
	public static float[][] layout_p_fun_widths=
	{
		{1, 1, 1, 1, 1, 1, 1, 1},
		{1, 1, 1, 1, 1, 1, 1, 1},
		{1, 1, 1, 1, 1, 1, 1, 1},
		{1, 1, 1, 1, 1, 1, 1, 1},
		{1.25f, 1.25f, 5, 1.25f, 1.25f},
	};

	//landscape layouts
	public static int[][] layout_l_std=		//standard layout
	{
		{SK_FUN,			'q', 'w', 'e', 'r', 't', SK_NAB,	SK_ESCAPE, SK_HOME, SK_END,	'7', '8', '9', '0',					'y', 'u', 'i', 'o', 'p'},
		{SK_SYM,	SK_NAB,	'a', 's', 'd', 'f', 'g', SK_NAB,	SK_TAB,	SK_CTRL, SK_ALT,	'4', '5', '6', SK_SETTINGS,	SK_NAB,	'h', 'j', 'k', 'l', SK_NAB},
		{SK_SHIFT,	SK_NAB,	'z', 'x', 'c', 'v', 'b', SK_NAB,	SK_SPACE,					'1', '2', '3', '.',			SK_NAB,	'n', 'm', SK_NAB, SK_BACKSPACE, SK_ENTER},
	};
	public static int[][] layout_l_std_shift=
	{
		{SK_FUN,			'Q', 'W', 'E', 'R', 'T', SK_NAB,	SK_ESCAPE, SK_HOME, SK_END,	'7', '8', '9', '0',					'Y', 'U', 'I', 'O', 'P'},
		{SK_SYM,	SK_NAB,	'A', 'S', 'D', 'F', 'G', SK_NAB,	SK_TAB,	SK_CTRL, SK_ALT,	'4', '5', '6', SK_UNICODE,	SK_NAB,	'H', 'J', 'K', 'L', SK_NAB},
		{SK_SHIFT,	SK_NAB,	'Z', 'X', 'C', 'V', 'B', SK_NAB,	SK_SPACE,					'1', '2', '3', ',',			SK_NAB,	'N', 'M', SK_NAB, SK_BACKSPACE, SK_ENTER},
	};
	public static int[][] layout_l_url_std=		//standard URL layout
	{
		{SK_FUN,			'q', 'w', 'e', 'r', 't', SK_NAB,	'/', SK_HOME, SK_END,	'7', '8', '9', '0',					'y', 'u', 'i', 'o', 'p'},
		{SK_SYM,	SK_NAB,	'a', 's', 'd', 'f', 'g', SK_NAB,	'@', SK_CTRL, SK_ALT,	'4', '5', '6', SK_SETTINGS,	SK_NAB,	'h', 'j', 'k', 'l', SK_NAB},
		{SK_SHIFT,	SK_NAB,	'z', 'x', 'c', 'v', 'b', SK_NAB,	SK_SPACE,				'1', '2', '3', '.',			SK_NAB,	'n', 'm', SK_NAB, SK_BACKSPACE, SK_ENTER},
	};
	public static int[][] layout_l_url_std_shift=
	{
		{SK_FUN,			'Q', 'W', 'E', 'R', 'T', SK_NAB,	'/', SK_HOME, SK_END,	'7', '8', '9', '0',					'Y', 'U', 'I', 'O', 'P'},
		{SK_SYM,	SK_NAB,	'A', 'S', 'D', 'F', 'G', SK_NAB,	'@', SK_CTRL, SK_ALT,	'4', '5', '6', SK_UNICODE,	SK_NAB,	'H', 'J', 'K', 'L', SK_NAB},
		{SK_SHIFT,	SK_NAB,	'Z', 'X', 'C', 'V', 'B', SK_NAB,	SK_SPACE,				'1', '2', '3', ',',			SK_NAB,	'N', 'M', SK_NAB, SK_BACKSPACE, SK_ENTER},
	};
	public static float[][] layout_l_std_widths=
	{
		{1,        1, 1, 1, 1, 1,	1,		1, 1, 1,	1, 1, 1, 1,			1, 1, 1, 1, 1},
		{1, 1.f/3,  1, 1, 1, 1, 1,	2.f/3,	1, 1, 1,	1, 1, 1, 1,	1.f/3,	1, 1, 1, 1, 2.f/3},
		{1, 0.75f,   1, 1, 1, 1, 1, 0.25f,	3,			1, 1, 1, 1, 0.75f,	1, 1, 0.25f, 1, 1},
	};
	public static int[][] layout_l_sym=		//symbols
	{
		{SK_FUN,	'!', '@', '#', '&',  '%', '^', '&', '*', '(', ')', SK_NAB,	SK_TAB, SK_CAPS_LOCK,	SK_NAB,	SK_HOME, SK_UP, SK_PGUP, SK_DELETE},
		{SK_STD,	'`', '~', '-', '_',  '=', '+', '[', ']', '{', '}', '|',		SK_CTRL, SK_ALT,		SK_NAB,	SK_LEFT, SK_NAB, SK_RIGHT, SK_BACKSPACE},
		{SK_SHIFT,	';', ':', '\'', '\"', ',', '.', '<', '>', '/', '?', '\\',	SK_SPACE,						SK_END, SK_DOWN, SK_PGDN, SK_ENTER},
	};
	public static float[][] layout_l_sym_widths=
	{
		{1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,	1, 1, 1, 1},
		{1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,	1, 1, 1, 1},
		{1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 3,			1, 1, 1, 1},
	};
	public static int[][] layout_l_fun=		//function keys
	{
		{SK_STD,	SK_F1, SK_F2, SK_F3, SK_F4, SK_NAB, SK_F5, SK_F6, SK_F7, SK_F8, SK_NAB, SK_F9, SK_F10, SK_F11, SK_F12,						SK_HOME, SK_UP, SK_PGUP, SK_DELETE},
		{SK_SYM,	SK_TAB, SK_ESCAPE, SK_NAB, SK_NAB, SK_NAB, SK_NAB, SK_NAB, SK_NAB, SK_NAB, SK_NAB, SK_NAB, SK_NAB, SK_NAB, SK_PRINTSCREEN,	SK_LEFT, SK_NAB, SK_RIGHT, SK_BACKSPACE},
		{SK_SHIFT,	SK_CAPS_LOCK, SK_CTRL, SK_ALT, SK_SPACE,									SK_NAB, SK_NAB, SK_NAB, SK_NAB, SK_NAB, SK_MENU,SK_END, SK_DOWN, SK_PGDN, SK_ENTER},
	};
	public static float[][] layout_l_fun_widths=
	{
		{1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
		{1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
		{1, 1, 1, 1, 5,				1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
	};

	//NUM-PAD LAYOUTS
	public static int[][] layout_p_num=			//portrait num-pad
	{
		{'1', '2', '3', SK_HOME},
		{'4', '5', '6', SK_END},
		{'7', '8', '9', SK_BACKSPACE},
		{'*', '0', '#', SK_ENTER},
	};
	public static float[][] layout_p_num_widths=
	{
		{1, 1, 1, 1},
		{1, 1, 1, 1},
		{1, 1, 1, 1},
		{1, 1, 1, 1},
	};
	public static int[][] layout_p_num2=			//portrait num-pad
	{
		{'1', '2', '3', '+', SK_HOME},
		{'4', '5', '6', '-', SK_END},
		{'7', '8', '9', '/', SK_BACKSPACE},
		{'*', '0', '#', '.', SK_ENTER},
	};
	public static float[][] layout_p_num2_widths=
	{
		{1, 1, 1, 1, 1},
		{1, 1, 1, 1, 1},
		{1, 1, 1, 1, 1},
		{1, 1, 1, 1, 1},
	};
	public static int[][] layout_l_num=
	{
		{'1', '2', '3', '4', '5', '6', '7', '8', '9', '0'},
		{'*', '#', '+', '-', SK_BACKSPACE, SK_ENTER},
	};
	public static float[][] layout_l_num_widths=
	{
		{1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
		{1, 1, 1, 1, 1, 1},
	};
	public static int[][] layout_l_num2=
	{
		{'1', '2', '3', '4', '5', '6', '7', '8', '9', '0'},
		{'+', '-', '*', '#', '/', '.', SK_BACKSPACE, SK_ENTER},
	};
	public static float[][] layout_l_num2_widths=
	{
		{1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
		{1, 1, 1, 1, 1, 1, 2, 2},
	};

	//PASSWORD LAYOUTS
	public static int[][] layout_p_password=		//portrait password layout
	{
		{'!', '@', '#', '$', '%', '^', '&', '*', '(', ')'},
		{'`', '~', '-', '_', '=', '+', '[', ']', '{', '}'},
		{';', ':', '\'', '\"', '\\', '|', '<', '>', '/', '?'},
		{'1', '2', '3', '4', '5', '6', '7', '8', '9', '0'},
		{'q', 'w', 'e', 'r', 't', 'y', 'u', 'i', 'o', 'p'},
		{SK_NAB, 'a', 's', 'd', 'f', 'g', 'h', 'j', 'k', 'l', SK_NAB},
		{SK_SHIFT, 'z', 'x', 'c', 'v', 'b', 'n', 'm', SK_BACKSPACE},
		{SK_SETTINGS, ',', SK_SPACE, '.', SK_ENTER},
	};
	public static int[][] layout_p_password_shift=
	{
		{'!', '@', '#', '$', '%', '^', '&', '*', '(', ')'},
		{'`', '~', '-', '_', '=', '+', '[', ']', '{', '}'},
		{';', ':', '\'', '\"', '\\', '|', '<', '>', '/', '?'},
		{'1', '2', '3', '4', '5', '6', '7', '8', '9', '0'},
		{'Q', 'W', 'E', 'R', 'T', 'Y', 'U', 'I', 'O', 'P'},
		{SK_NAB, 'A', 'S', 'D', 'F', 'G', 'H', 'J', 'K', 'L', SK_NAB},
		{SK_SHIFT, 'Z', 'X', 'C', 'V', 'B', 'N', 'M', SK_BACKSPACE},
		{SK_SETTINGS, ',', SK_SPACE, '.', SK_ENTER},
	};
	public static float[][] layout_p_password_widths=
	{
		{1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
		{1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
		{1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
		{1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
		{1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
		{0.5f, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0.5f},
		{1.5f, 1, 1, 1, 1, 1, 1, 1, 1.5f},
		{1.25f, 1.25f, 5, 1.25f, 1.25f},
	};
	public static int[][] layout_l_password=		//landscape password layout
	{
		{'~', ';', ':', '\'', '\"', '\\', '|', '<', '>', '/', '?', '&', '*', '(', ')'},//15
		{'`', '-', '_', '=', '+', '[', ']', '{', '}', '!', '@', '#', '$', '%', '^'},//15
		{SK_NAB,			'q', 'w', 'e', 'r', 't', SK_NAB,	'.', ',',			'7', '8', '9', '0',					'y', 'u', 'i', 'o', 'p'},
		{SK_NAB,	SK_NAB,	'a', 's', 'd', 'f', 'g', SK_NAB,	SK_HOME, SK_END,	'4', '5', '6', SK_SETTINGS,	SK_NAB,	'h', 'j', 'k', 'l', SK_NAB},
		{SK_SHIFT,	SK_NAB,	'z', 'x', 'c', 'v', 'b', SK_NAB,	SK_SPACE,			'1', '2', '3', SK_NAB,		SK_NAB,	'n', 'm', SK_NAB, SK_BACKSPACE, SK_ENTER},
	};
	public static int[][] layout_l_password_shift=
	{
		{'~', ';', ':', '\'', '\"', '\\', '|', '<', '>', '/', '?', '&', '*', '(', ')'},//15
		{'`', '-', '_', '=', '+', '[', ']', '{', '}', '!', '@', '#', '$', '%', '^'},//15
		{SK_NAB,			'Q', 'W', 'E', 'R', 'T', SK_NAB,	'.', ',',			'7', '8', '9', '0',					'Y', 'U', 'I', 'O', 'P'},
		{SK_NAB,	SK_NAB,	'A', 'S', 'D', 'F', 'G', SK_NAB,	SK_HOME, SK_END,	'4', '5', '6', SK_SETTINGS,	SK_NAB,	'H', 'J', 'K', 'L', SK_NAB},
		{SK_SHIFT,	SK_NAB,	'Z', 'X', 'C', 'V', 'B', SK_NAB,	SK_SPACE,			'1', '2', '3', SK_NAB,		SK_NAB,	'N', 'M', SK_NAB, SK_BACKSPACE, SK_ENTER},
	};
	public static float[][] layout_l_password_widths=
	{
		{1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1},//15
		{1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1},//15
		{1,        1, 1, 1, 1, 1,	1,		1, 1,	1, 1, 1, 1,			1, 1, 1, 1, 1},
		{1, 1.f/3,  1, 1, 1, 1, 1,	2.f/3,	1, 1,	1, 1, 1, 1,	1.f/3,	1, 1, 1, 1, 2.f/3},
		{1, 0.75f,   1, 1, 1, 1, 1, 0.25f,	2,		1, 1, 1, 1, 0.75f,	1, 1, 0.25f, 1, 1},
	};
	public void integrityTest(int[][] codes, float[][] widths, String name)
	{
		if(codes.length!=widths.length)
		{
			addError(String.format(loc, "%s: codes %d rows != widths %d rows", name, codes.length, widths.length));
			return;
		}
		for(int ky=0;ky<codes.length;++ky)
			if(codes[ky].length!=widths[ky].length)
				addError(String.format(loc, "%s row %d: codes %d buttons != widths %d buttons", name, ky, codes[ky].length, widths[ky].length));
	}
	public static class ButtonIdx
	{
		int ky, kx, code,
			x1, x2, y1, y2;//button bounds
		ButtonIdx(){kx=-1; ky=-1; code=SK_NAB;}
		boolean invalid(){return kx==-1||ky==-1;}
	}
	public static class Button2
	{
		int x1, x2, code;
		float relativeWidth;
		Button2(int _x1, int _x2, int _code, float rw){x1=_x1; x2=_x2; code=_code; relativeWidth=rw;}
	}
	public static class Row2
	{
		int y1, y2;
		ArrayList<Button2> buttons;
		Row2(int _y1, int _y2, int buttonCount){y1=_y1; y2=_y2; buttons=new ArrayList<>(buttonCount);}
	}
	public class Layout
	{
		String name;
		ArrayList<Row2> rows;
		public void init(int[][] codes, float[][] widths)
		{
			rows.ensureCapacity(codes.length);
			for(int ky=0;ky<codes.length;++ky)//for each row
			{
				int[] rowCodes=codes[ky];
				float[] rowWidths=widths[ky];
				Row2 row=new Row2(kb_h*ky/codes.length, kb_h*(ky+1)/codes.length, rowCodes.length);//equal height

				float sum=0;
				for(int kx=0;kx<rowCodes.length;++kx)
					sum+=rowWidths[kx];

				float x1=0;
				for(int kx=0;kx<rowCodes.length;++kx)
				{
					float x2=x1+rowWidths[kx];
					row.buttons.add(new Button2((int)(x1/sum*w), (int)(x2/sum*w), rowCodes[kx], rowWidths[kx]));
					x1=x2;
				}
				rows.add(row);
			}
		}
		Layout(String _name, int[][] codes, float[][] widths)
		{
			integrityTest(codes, widths, _name);

			name=_name;

			rows=new ArrayList<>();
			init(codes, widths);
		}
	}
	public static abstract class Keyboard
	{
		ArrayList<Layout> layouts;
		int idx;
		//constructor should set: layouts, idx, kb_h
		ArrayList<Row2> getRows(){return layouts.get(idx).rows;}
		ButtonIdx getButtonIdx(float x, float y)
		{
			ButtonIdx bIdx=new ButtonIdx();
			Layout layout=layouts.get(idx);
			for(int ky=0, nRows=layout.rows.size();ky<nRows;++ky)
			{
				Row2 row=layout.rows.get(ky);
				if(y>=row.y1&&y<row.y2)
				{
					bIdx.ky=ky; bIdx.y1=row.y1; bIdx.y2=row.y2;//
					for(int kx=0, nButtons=row.buttons.size();kx<nButtons;++kx)
					{
						Button2 b=row.buttons.get(kx);
						if(x>=b.x1&&x<b.x2)
						{
							bIdx.kx=kx; bIdx.x1=b.x1; bIdx.x2=b.x2;
							bIdx.code=b.code;
							break;
						}
					}
					break;
				}
			}
			return bIdx;
		}
		ButtonIdx getButtonIdx_layout(int kx, int ky)
		{
			ButtonIdx bIdx=new ButtonIdx();
			Layout layout=layouts.get(idx);
			if(ky>=0&&ky<layout.rows.size())
			{
				bIdx.ky=ky;//
				Row2 row=layout.rows.get(ky);
				if(kx>=0&&kx<row.buttons.size())
				{
					Button2 b=row.buttons.get(kx);
					bIdx.kx=kx;
					bIdx.code=b.code;
				}
			}
			return bIdx;
		}
		abstract void toggleShift();
		abstract void pressShift();
		abstract void releaseShift();
		abstract void selectLayout(int code);
		abstract void exitQuickMode();
	}
	public Keyboard kb;
	public static final int//keyboard: portrait-text
		KPT_STD=0, KPT_STD_SHIFT=1,
		KPT_SYM=2,
		KPT_SPK=3, KPT_SPK_SHIFT=4,
		KPT_FUN=5;
	public class PortraitTextKeyboard extends Keyboard
	{
		PortraitTextKeyboard()
		{
			kb_h=(int)(h*0.32f);
			idx=0;
			layouts=new ArrayList<>(6);
			if(mode==MODE_URL||mode==MODE_EMAIL)
			{
				layouts.add(new Layout("p_url_std",			layout_p_url_std,		layout_p_url_std_widths));
				layouts.add(new Layout("p_url_std_shift",	layout_p_url_std_shift, layout_p_url_std_widths));
			}
			else
			{
				layouts.add(new Layout("p_std",			layout_p_std,		layout_p_std_widths));
				layouts.add(new Layout("p_std_shift",	layout_p_std_shift, layout_p_std_widths));
			}

			layouts.add(new Layout("p_sym",			layout_p_sym,		layout_p_sym_widths));

			layouts.add(new Layout("p_spk",			layout_p_spk,		layout_p_spk_widths));
			layouts.add(new Layout("p_spk_shift",	layout_p_spk_shift,	layout_p_spk_widths));

			layouts.add(new Layout("p_fun",			layout_p_fun,		layout_p_fun_widths));
		}

		@Override void toggleShift()
		{
			switch(idx)
			{
			case KPT_STD:		idx=KPT_STD_SHIFT;	break;
			case KPT_STD_SHIFT:	idx=KPT_STD;		break;

			case KPT_SPK:		idx=KPT_SPK_SHIFT;	break;
			case KPT_SPK_SHIFT:	idx=KPT_SPK;		break;

		//	case KPT_SYM:
		//	case KPT_FUN:							break;
			}
		}
		@Override void pressShift()
		{
			switch(idx)
			{
			case KPT_STD:
			case KPT_STD_SHIFT:	idx=KPT_STD_SHIFT;	break;

			case KPT_SPK:
			case KPT_SPK_SHIFT:	idx=KPT_SPK_SHIFT;	break;

		//	case KPT_SYM:
		//	case KPT_FUN:							break;
			}
		}
		@Override void releaseShift()
		{
			switch(idx)
			{
			case KPT_STD:
			case KPT_STD_SHIFT:	idx=KPT_STD;	break;

			case KPT_SPK:
			case KPT_SPK_SHIFT:	idx=KPT_SPK;	break;

		//	case KPT_SYM:
		//	case KPT_FUN:						break;
			}
		}
		@Override void selectLayout(int code)
		{
			switch(code)
			{
			case SK_STD:	idx=isActive_shift?KPT_STD_SHIFT:KPT_STD;	break;
			case SK_SYM:	idx=KPT_SYM;								break;
			case SK_SPK:	idx=isActive_shift?KPT_SPK_SHIFT:KPT_SPK;	break;
			case SK_FUN:	idx=KPT_FUN;								break;
			}
		}
		@Override void exitQuickMode()
		{
			switch(idx)
			{
			case KPT_SYM:
			case KPT_FUN:
			case KPT_STD:
			case KPT_STD_SHIFT:	idx=isActive_shift?KPT_STD_SHIFT:KPT_STD;	break;

			case KPT_SPK:
			case KPT_SPK_SHIFT:	idx=isActive_shift?KPT_SPK_SHIFT:KPT_SPK;	break;
			}
		}
	}
	public static final int//keyboard: portrait-text
		KLT_STD=0, KLT_STD_SHIFT=1,
		KLT_SYM=2,
		KLT_FUN=3;
	public class LandscapeTextKeyboard extends Keyboard
	{
		LandscapeTextKeyboard()
		{
			kb_h=(int)(h*0.32f);
			idx=0;
			layouts=new ArrayList<>(4);
			if(mode==MODE_URL||mode==MODE_EMAIL)
			{
				layouts.add(new Layout("l_url_std",			layout_l_url_std,		layout_l_std_widths));
				layouts.add(new Layout("l_url_std_shift",	layout_l_url_std_shift,	layout_l_std_widths));
			}
			else
			{
				layouts.add(new Layout("l_std",			layout_l_std,		layout_l_std_widths));
				layouts.add(new Layout("l_std_shift",	layout_l_std_shift,	layout_l_std_widths));
			}

			layouts.add(new Layout("l_sym",			layout_l_sym,		layout_l_sym_widths));

			layouts.add(new Layout("l_fun",			layout_l_fun,		layout_l_fun_widths));
		}
		@Override void toggleShift()
		{
			switch(idx)
			{
			case KLT_STD:		idx=KLT_STD_SHIFT;	break;
			case KLT_STD_SHIFT:	idx=KLT_STD;		break;
			}
		}
		@Override void pressShift()
		{
			switch(idx)
			{
			case KLT_STD:
			case KLT_STD_SHIFT:	idx=KLT_STD_SHIFT;	break;
			}
		}
		@Override void releaseShift()
		{
			switch(idx)
			{
			case KLT_STD:
			case KLT_STD_SHIFT:	idx=KLT_STD;	break;
			}
		}
		@Override void selectLayout(int code)
		{
			switch(code)
			{
			case SK_STD:	idx=isActive_shift?KLT_STD_SHIFT:KLT_STD;	break;
			case SK_SYM:	idx=KLT_SYM;								break;
			case SK_FUN:	idx=KLT_FUN;								break;
			}
		}
		@Override void exitQuickMode(){idx=isActive_shift?KLT_STD_SHIFT:KLT_STD;}
	}
	public class PortraitNumPadKeyboard extends Keyboard
	{
		PortraitNumPadKeyboard()
		{
			kb_h=(int)(h*0.32f);
			idx=0;
			layouts=new ArrayList<>(1);
			if(numeric_signed||numeric_decimal)
				layouts.add(new Layout("p_num2", layout_p_num2, layout_p_num2_widths));
			else
				layouts.add(new Layout("p_num", layout_p_num, layout_p_num_widths));
		}
		@Override void toggleShift(){}
		@Override void pressShift(){}
		@Override void releaseShift(){}
		@Override void selectLayout(int code){}
		@Override void exitQuickMode(){}
	}
	public class LandscapeNumPadKeyboard extends Keyboard
	{
		LandscapeNumPadKeyboard()
		{
			kb_h=(int)(h*0.21);//TODO: correct height
			idx=0;
			layouts=new ArrayList<>(1);
			if(numeric_signed||numeric_decimal)
				layouts.add(new Layout("l_num2", layout_l_num2, layout_l_num2_widths));
			else
				layouts.add(new Layout("l_num", layout_l_num, layout_l_num_widths));
		}
		@Override void toggleShift(){}
		@Override void pressShift(){}
		@Override void releaseShift(){}
		@Override void selectLayout(int code){}
		@Override void exitQuickMode(){}
	}
	public class PortraitPasswordKeyboard extends Keyboard
	{
		PortraitPasswordKeyboard()
		{
			kb_h=(int)(h*0.45f);
			idx=0;
			layouts=new ArrayList<>(2);
			layouts.add(new Layout("p_password",		layout_p_password,			layout_p_password_widths));
			layouts.add(new Layout("p_password_shift",	layout_p_password_shift,	layout_p_password_widths));
		}
		@Override void toggleShift(){idx=1-idx;}
		@Override void pressShift(){idx=1;}
		@Override void releaseShift(){idx=0;}
		@Override void selectLayout(int code){}
		@Override void exitQuickMode(){idx=0;}
	}
	public class LandscapePasswordKeyboard extends Keyboard
	{
		LandscapePasswordKeyboard()
		{
			kb_h=(int)(h*0.48f);
			idx=0;
			layouts=new ArrayList<>(2);
			layouts.add(new Layout("l_password",		layout_l_password,			layout_l_password_widths));
			layouts.add(new Layout("l_password_shift",	layout_l_password_shift,	layout_l_password_widths));
		}
		@Override void toggleShift(){idx=1-idx;}
		@Override void pressShift(){idx=1;}
		@Override void releaseShift(){idx=0;}
		@Override void selectLayout(int code){}
		@Override void exitQuickMode(){idx=0;}
	}

	float textSize=32, textSize_long=20, shadowOffset=4;
	Paint textPaint, letterPaint, specialButtonPaint, textPaintDebug, penPaint, buttonPaint;
	TextPaint wrapPaint;
	//Bitmap mBuffer;
	PopupWindow previewPopup;
	TextView preview;

	CKBservice service;
	public boolean isPortrait(){return w<h;}
	public CKBview2(Context context){super(context);}
	public CKBview2(Context context, AttributeSet attrs){super(context, attrs);}
	public CKBview2(Context context, AttributeSet attrs, int defStyle){super(context, attrs, defStyle);}

	int ceilMultiple(int x, int n)
	{
		if(x<1)
			x=1;
		int q=n/x, r=n%x;
		q+=r!=0?1:0;
		return n/q;
	}
	public void initCKB(CKBservice _service)//called on switch to landscape & back
	{
		try
		{
			if(_service==null)
				throw new Exception("CKBservice == null");

			DisplayMetrics metrics=Resources.getSystem().getDisplayMetrics();
			w=metrics.widthPixels; h=metrics.heightPixels;

			int minDim=Math.min(w, h);
			textSize=minDim/20.f;//33.75.f
			textSize_long=minDim/40.f;

			textPaint=new Paint();
			textPaint.setAntiAlias(true);
			textPaint.setTextAlign(Paint.Align.CENTER);
			textPaint.setColor(color_labels);
			letterPaint=new Paint(textPaint);
			specialButtonPaint=new Paint(textPaint);

			textPaintDebug=new Paint();
			textPaintDebug.setTextSize(32);

			penPaint=new Paint();
			penPaint.setStyle(Paint.Style.STROKE);
			penPaint.setStrokeWidth(3);

			buttonPaint=new Paint();
			buttonPaint.setColor(color_button);

			shadowOffset=minDim/270.f;
			textPaint			.setShadowLayer(shadowOffset*0.5f, shadowOffset, shadowOffset, color_shadow);//0xAARRGGBB
			letterPaint			.setShadowLayer(shadowOffset*0.5f, shadowOffset, shadowOffset, color_shadow_letters);
			specialButtonPaint	.setShadowLayer(shadowOffset*0.5f, shadowOffset, shadowOffset, color_shadow_special);

			wrapPaint=new TextPaint();
			wrapPaint.setTextSize(16*getResources().getDisplayMetrics().density);
			wrapPaint.setColor(0xFF000000);

			setWillNotDraw(false);//for ViewGroup to call o n D r a w
			setClipChildren(false);

			service=_service;

			LayoutInflater inflater=service.getLayoutInflater();
			preview=(TextView)inflater.inflate(R.layout.popup_view, null);
			//preview=(TextView)inflater.inflate(R.layout.popup_view, null);//CRASH cast to TextView
			//preview=new TextView(service.getApplication());//

			preview.setGravity(Gravity.CENTER);
			preview.setTextSize(textSize/2);
			//preview.setTextSize(textSize);
			preview.setTextColor(color_preview_text);

			Drawable bk=DrawableCompat.wrap(preview.getBackground()).mutate();
			DrawableCompat.setTint(bk, color_button_pressed);//change preview bkColor
			//preview.setBackgroundColor(0x80FFFFFF);//don't set bkColor for bk to work

			//service.main_layout.removeView(preview);
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
	@Override protected void onLayout(boolean changed, int l, int t, int r, int b)
	{
		//preview.layout(0, 0, w, kb_h);//top of keyboard, not well visible when pressing top buttons
	}
	void makeTextKeyboard()
	{
		if(isPortrait())
			kb=new PortraitTextKeyboard();
		else
			kb=new LandscapeTextKeyboard();
	}
	void makeNumPad()
	{
		if(isPortrait())
			kb=new PortraitNumPadKeyboard();
		else
			kb=new LandscapeNumPadKeyboard();
	}
	void makePasswordKeyboard()
	{
		if(isPortrait())
			kb=new PortraitPasswordKeyboard();
		else
			kb=new LandscapePasswordKeyboard();
	}
	boolean mask(int x, int m){return (x&m)==m;}
	public void startCKB(EditorInfo info)
	{
		if(DEBUG_MODE)
			Log.e(TAG, String.format("inputType = 0x%08X", info.inputType));
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
		/*	switch(info.inputType&InputType.TYPE_MASK_FLAGS)//X independent flags
			{
			case InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS:
			case InputType.TYPE_TEXT_FLAG_CAP_WORDS:
			case InputType.TYPE_TEXT_FLAG_CAP_SENTENCES:
			case InputType.TYPE_TEXT_FLAG_AUTO_CORRECT:
			case InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE:
			case InputType.TYPE_TEXT_FLAG_MULTI_LINE:
			case InputType.TYPE_TEXT_FLAG_IME_MULTI_LINE:
			case InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS:
				break;
			}//*/
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
			//switch(info.inputType&InputType.TYPE_MASK_FLAGS)//X independent flags
			//{
			//case InputType.TYPE_NUMBER_FLAG_SIGNED://+ -
			//case InputType.TYPE_NUMBER_FLAG_DECIMAL://. /
			//	break;
			//}
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
		switch(mode)
		{
		case MODE_NUMBER:
		case MODE_NUMERIC_PASSWORD:
		case MODE_PHONE_NUMBER:
			makeNumPad();
			break;
		case MODE_PASSWORD:
			makePasswordKeyboard();
			break;
		case MODE_TEXT:
		case MODE_URL:
		case MODE_EMAIL:
		default:
			makeTextKeyboard();
			break;
		}
		//kb_h=(int)(h*0.32f);
		//if(w<h)//portrait
		//	kb_h=(int)(h*0.32f);//5 rows
		//else//landscape
		//	kb_h=(int)(h*0.32f);//3 rows
		kb_y1=h-kb_h;
		ccGridX=ceilMultiple((int)(textSize*1.5), w);
		ccGridY=kb_h/kb.getRows().size();

		ViewGroup.LayoutParams lp=getLayoutParams();
		if(lp==null)
		{
			Log.e(TAG, "CKBview2.LayoutParams == null");
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

	Locale loc=new Locale("US");
	public String debugInfo;

	int tap_x=-1, tap_y=-1;//layout coordinates of currently held button	multitouch?
	int quickMode=0;
	RectF rectf=new RectF();
	int color_clear=0x00202020,
		color_button=0xC0707070, color_button_fast=0xC0237CE9, color_button_pressed=0xC0FF8040,
		color_labels=0xFFFFFFFF,//0xAARRGGBB
		color_shadow=0xC00000FF, color_shadow_letters=0xC0FF0000, color_shadow_special=0xC000FF00,
		color_preview_text=0xC0000000;//0x80FFFFFF 0x80000000
	int ccGridX, ccGridY;
	Rect mBounds=new Rect();
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
		canvas.drawColor(color_clear);//same everywhere
		if(cursor_control!=CC_DISABLED)
		{
			float radius=10;
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
			int startIdx, endIdx;
			TouchInfo ti_start, ti_end;
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
			ArrayList<Row2> rows=kb.getRows();
			for(int ky=0, nRows=rows.size();ky<nRows;++ky)
			{
				Row2 row;
				try
				{
					row=rows.get(ky);
				}
				catch(Exception e)
				{
					continue;
				}
				int dy=row.y2-row.y1;
				for(int kx=0, nButtons=row.buttons.size();kx<nButtons;++kx)
				{
					Button2 button=row.buttons.get(kx);
					if(button.code!=SK_NAB)
					{
						int dx=button.x2-button.x1;
						canvas.translate(button.x1, row.y1);

						float radius=10;
						rectf.set(radius, radius, dx-radius, dy-radius);
						int color=buttonPaint.getColor(), color2=color;
						boolean colorWasSet=false;
						switch(button.code)//check long-pressable keys
						{
						case SK_CTRL:	if(isActive_ctrl){	colorWasSet=true; color2=color_button_pressed;}	break;
						case SK_ALT:	if(isActive_alt){	colorWasSet=true; color2=color_button_pressed;}	break;

						case SK_CAPS_LOCK:
						case SK_SHIFT:	if(isActive_shift){	colorWasSet=true; color2=color_button_pressed;}	break;
						case SK_SYM:	if(isActive_sym){	colorWasSet=true; color2=color_button_pressed;}	break;
					//	case SK_SPK:	if(isActive_spk){	colorWasSet=true; color2=color_button_pressed;}	break;
						case SK_FUN:	if(isActive_fun){	colorWasSet=true; color2=color_button_pressed;}	break;
						}
						if(!colorWasSet&&quickMode==1)
						{
							colorWasSet=true;
							color2=color_button_fast;
						}
						if(kx==tap_x&&ky==tap_y)
						{
							colorWasSet=true;
							color2=color_button_pressed;
						}
						if(colorWasSet)
							buttonPaint.setColor(color2);
						if(DEBUG_STATE)
						{
							if(button.code==SK_SHIFT)
							{
								ButtonIdx bIdx2=kb.getButtonIdx_layout(tap_x, tap_y);
								if(bIdx2.code==SK_NAB)
									Log.e(TAG, String.format("%d  c1=0x%08X, c2=0x%08X, tap(%d, %d)", frame_counter, color, color2, tap_x, tap_y));
								else
								{
									String label=getLabel(bIdx2.code);
									if(label==null)
										label="OOB";
									Log.e(TAG, String.format("%d  c1=0x%08X, c2=0x%08X, tap(%d, %d): %s", frame_counter, color, color2, tap_x, tap_y, label));
								}
							}
						}
						canvas.drawRoundRect(rectf, radius, radius, buttonPaint);
						if(colorWasSet)
							buttonPaint.setColor(color);

						float currentSize;
						Paint p=null;
						if(button.code>=SK_STD&&button.code<=SK_TRANSPARENT)//
							p=specialButtonPaint;
						String label=getLabel(button.code);
						if(label==null)
							label="INVALID";
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
								if(Character.isLetter(label.charAt(0)))
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
							float lineHeight=(int) ((float) mBounds.height()*1.2),
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

						canvas.translate(-button.x1, -row.y1);
					}
				}//end row loop
			}//end keyboard loop
		}

	/*	if(DEBUG_TOUCH)//multitouch test
		{
			int color=penPaint.getColor();
			final int red=0xFFFF0000, grey=0xFF808080, pink=0xFFFF00FF;
			for(int k=0, size=touchInfo.size();k<size;++k)
			{
				TouchInfo ti=touchInfo.get(k);
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
	public int cursor_control=CC_DISABLED;//TODO: pure horizontal cursor control
	//int touchId_current=-1;
	public boolean isHeld_shift=false;//finger holds shift, needed to initiate selection control
	int touchId_shift=-1, touchId_selStart=-1, touchId_selEnd=-1;

	//int startRowIdx=-1, startButtonIdx=-1;
	float currentX=-1, currentY=-1;
	//int ccX1=-1, ccY1=-1;

	int turboStart_ms=600, turbo_ms=35,
		longPress_ms=400;
	class TurboTask extends TimerTask
	{
		public int code;
		TurboTask(int _code){code=_code;}
		@Override public void run()
		{
			service.onKeyCallback(code, 3);
		}
	}
	boolean isActive_ctrl, isActive_alt, isActive_shift,
		isActive_sym, isActive_spk, isActive_fun;//<- no need to track these
	class LongPressTask extends TimerTask
	{
		public int code;
		public boolean done=false;
		LongPressTask(int _code){code=_code;}
		@Override public void run()
		{
			service.onKeyCallback(code, 1);
			switch(code)
			{
			//sends pure down
		//	case SK_CTRL:	isActive_ctrl=true;		service.onKeyCallback(SK_CTRL, 1);	break;
			case SK_ALT:	isActive_alt=true;		service.onKeyCallback(SK_ALT, 1);	break;

			//stays on (green mode), otherwise layout is reset to std
			case SK_SHIFT:	isActive_shift=true;	service.onKeyCallback(SK_SHIFT, 1); quickMode=0;	break;
			case SK_SYM:	isActive_sym=true;		quickMode=0;	break;
		//	case SK_SPK:	isActive_spk=true;		quickMode=0;	break;
			case SK_FUN:	isActive_fun=true;		quickMode=0;	break;
			}
			done=true;
		}
	}
	Timer timer=new Timer();
	int timerOn=0;//0: off, 1: turboTask, 2: longPressTask
	int touchId_timer;
	TurboTask turboTask=new TurboTask(SK_NAB);//TODO: check if initialization needed, probably not
	LongPressTask longPressTask=new LongPressTask(SK_NAB);
	boolean isTurboKey(int code)
	{
		switch(code)
		{
		case SK_ESCAPE:
		case SK_INSERT:case SK_CAPS_LOCK:case SK_SHIFT:case SK_CTRL:case SK_ALT:case SK_HOME:case SK_END:
		case SK_NUM_LOCK:case SK_SCROLL_LOCK:
		case SK_MENU:case SK_STD:case SK_SYM:case SK_SPK:case SK_FUN:case SK_TRANSPARENT:case SK_SETTINGS:case SK_UNICODE:case SK_NAB:
			return false;
		}
		return true;
	}
	boolean isLongPressableKey(int code)
	{
		switch(code)
		{
		case SK_SHIFT://sends pure down, stays on
	//	case SK_CTRL:case SK_ALT://sends pure down
		case SK_SYM:
	//	case SK_SPK:
		case SK_FUN://stays on (green mode), otherwise layout is reset to std
			return true;
		}
		return false;
	}
	int clamp(int x, int hi)
	{
		if(x<0)
			x=0;
		if(x>hi)
			x=hi;
		return x;
	}
	void showKeyPreview(ButtonIdx bIdx, String label)//see KeyboardView.java showKey() line 924
	{
		if(bIdx.invalid()||bIdx.code==SK_NAB||label==null)
			return;
		try
		{
			preview.setCompoundDrawables(null, null, null, null);
			if(bIdx.code==SK_SPACE)
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
			x1=clamp((x1+x2-width)/2, w-width);
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

			//android.inputmethodservice.KeyboardView;
			//if(android.os.Build.VERSION.SDK_INT>=android.os.Build.VERSION_CODES.M)//
			//{
			//	ViewGroup.LayoutParams lp2=preview.getLayoutParams();
			//	Log.e(TAG, "TextView has "+lp2.getClass().getName());
			//}
		}
		catch(Exception e)
		{
			Log.e(TAG, "exception", e);
		}
	}
	void hideKeyPreview()
	{
		previewPopup.dismiss();
		preview.setVisibility(INVISIBLE);
		//preview.setText("");
	}
	@Override public boolean performClick()
	{
		super.performClick();
		return true;
	}

	TouchInfo touchInfo=new TouchInfo();

	int frame_counter=0;
	void exitCursorControl()
	{
		cursor_control=CC_DISABLED;
		isActive_shift=false;	service.onKeyCallback(SK_SHIFT, 2);
	//	isActive_ctrl=false;	service.onKeyCallback(SK_CTRL, 2);
	//	isActive_alt=false;		service.onKeyCallback(SK_ALT, 2);
		isActive_fun=false;
		isActive_spk=false;
		isActive_sym=false;
		kb.releaseShift();
	}
	void applyCursorControl(TouchInfo.Pointer ti, int cc_flags)
	{
		int ccX1=(int)(ti.prevx	/ccGridX), ccY1=(int)(ti.prevy	/ccGridY),
			ccX2=(int)(ti.x		/ccGridX), ccY2=(int)(ti.y		/ccGridY);
		if(DEBUG_CC)
			Log.e(TAG, String.format("applyCC: (%d, %d)->(%d, %d)", ccX1, ccY1, ccX2, ccY2));//

		if(ccX2<ccX1)
			service.onNavigateCallback(SK_LEFT, Math.abs(ccX2-ccX1), cc_flags);
		else if(ccX2>ccX1)
			service.onNavigateCallback(SK_RIGHT, Math.abs(ccX2-ccX1), cc_flags);

		if(multiline)
		{
			if(ccY2<ccY1)
				service.onNavigateCallback(SK_UP, Math.abs(ccY2-ccY1), cc_flags);
			else if(ccY2>ccY1)
				service.onNavigateCallback(SK_DOWN, Math.abs(ccY2-ccY1), cc_flags);
		}
	}
	void logCursorControl(String tag)
	{
		if(!DEBUG_CC)
			return;
		switch(cursor_control)
		{
		case CC_DISABLED:
			ButtonIdx bIdx=kb.getButtonIdx_layout(tap_x, tap_y);
			String label=getLabel(bIdx.code);
			if(isHeld_shift)
			{
				if(label==null)
					Log.e(TAG, String.format(loc, "%s: CC=DISABLED: shift_id=%d, label==null (row %d, button %d)", tag, touchId_shift, tap_y, tap_x));
				else
					Log.e(TAG, String.format(loc, "%s: CC=DISABLED: shift_id=%d, %s", tag, touchId_shift, label));
			}
			else
			{
				if(label==null)
					Log.e(TAG, String.format(loc, "%s: CC=DISABLED: label==null (row %d, button %d)", tag, tap_y, tap_x));
				else
					Log.e(TAG, String.format(loc, "%s: CC=DISABLED: %s", tag, label));
			}
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
				ButtonIdx bIdx=kb.getButtonIdx(ti.x, ti.y);
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
						ButtonIdx bIdx0=kb.getButtonIdx(ti.prevx, ti.prevy);
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
						bIdx0=kb.getButtonIdx(ti.prevx, ti.prevy),
						bIdx=kb.getButtonIdx(ti.x, ti.y);
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
								Log.e(TAG, String.format("BEFORE:  DOWN - %d%s%s", bIdx.code, bIdx.code==SK_SHIFT?" SHIFT":"", isActive_shift?" ACTIVE":""));
							switch(bIdx.code)
							{
							case SK_BACKSPACE:
								service.onKeyCallback(bIdx.code, 3);
								break;
							case SK_SHIFT:
								isHeld_shift=true; touchId_shift=ti.id;
								if(isActive_shift)
								{
									isActive_shift=false;	service.onKeyCallback(SK_SHIFT, 2);
									quickMode=0;
									kb.selectLayout(SK_STD);
								}
								else
								{
									quickMode=1-quickMode;
									service.onKeyCallback(bIdx.code, 3);
								}
								break;
							}
							if(DEBUG_STATE)
								Log.e(TAG, String.format("AFTER:  DOWN - %d%s%s", bIdx.code, bIdx.code==SK_SHIFT?" SHIFT":"", isActive_shift?" ACTIVE":""));
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
							isHeld_shift=false;
						if(np==1&&cursor_control!=CC_DISABLED)//
							exitCursorControl();//
						hideKeyPreview();
						if(bIdx.code!=-1&&bIdx0.ky!=-1&&bIdx0.kx!=-1&&bIdx.ky==bIdx0.ky&&bIdx.kx==bIdx0.kx)
						{
							boolean handled=false;
							switch(timerOn)
							{
							case 0:
								if(bIdx.code==SK_STD)
								{
									isActive_sym=false;
									isActive_spk=false;
									isActive_fun=false;
								}
								break;
							case 1://turbo timer
								if(turboTask.code==bIdx.code)
								{
									timerOn=0;
									timer.cancel();
								}
								break;
							case 2://long press timer
								if(isLongPressableKey(longPressTask.code)&&(isLongPressableKey(bIdx.code)||bIdx.code==SK_STD))
								{
									handled=longPressTask.done;
									if(!longPressTask.done)
									{
										switch(bIdx.code)
										{
										case SK_SYM:
										case SK_FUN:
											quickMode=1-quickMode;//quick press, long press
											break;
										}
										switch(bIdx.code)
										{
										//sends pure down, stays on
									/*	case SK_CTRL:
											if(isActive_ctrl)
											{
												handled=true;
												if(ti.state==TouchInfo.S_UP)
													service.onKeyCallback(bIdx.code, 2);
												isActive_ctrl=false;	service.onKeyCallback(SK_CTRL, 2);
											}
											break;
										case SK_ALT:
											if(isActive_alt)
											{
												handled=true;
												if(ti.state==TouchInfo.S_UP)
													service.onKeyCallback(bIdx.code, 2);
												isActive_alt=false;		service.onKeyCallback(SK_ALT, 2);
											}
											break;//*/

										case SK_SYM:case SK_FUN:case SK_STD:
											isActive_sym=false;
											isActive_spk=false;
											isActive_fun=false;
											break;
										}
									}
									timerOn=0;
									timer.cancel();
								}
								break;
							}

							//reset layout if quick mode
							if(quickMode!=0&&!isLongPressableKey(bIdx.code))
							{
								quickMode=0;
								kb.exitQuickMode();
							}

							if(bIdx.code!=SK_BACKSPACE&&bIdx.code!=SK_SHIFT&&!handled&&ti.state!=TouchInfo.S_CANCEL)
								service.onKeyCallback(bIdx.code, 3);
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
