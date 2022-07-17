/*  CKBservice.java - The CKB InputMethodService
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

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.inputmethodservice.InputMethodService;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.ExtractedTextRequest;
import android.view.inputmethod.InputConnection;

import android.view.inputmethod.EditorInfo;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.nio.CharBuffer;

public class CKBservice extends InputMethodService//implements KeyboardView.OnKeyboardActionListener
{
	public static final boolean DEBUGGER=false;
	private static final String TAG="customkb";

	//public boolean transparent=true;
	public CKBservice(){}

	//for user-interface initialization, in particular to deal with configuration changes while the service is running
	//called prior to any of your UI objects being created
	//both after the service is first created and after a configuration change happens
	@Override public void onInitializeInterface(){}
	//called when a new client has bound to the input method
	//may be followed by a series of onStartInput() and onFinishInput() calls as the user navigates through its UI
	@Override public void onBindInput(){}
	//deals with an input session starting with the client
	//Called to inform the input method that text input has started in an editor.
	//You should use this callback to initialize the state of your input to match the state of the editor given to it.
	@Override public void onStartInput(EditorInfo attribute, boolean restarting){}

	RelativeLayout main_layout;
	CKBview3 myView;
	long thread_id=0;
	//Create and return the view hierarchy used for the input area (such as a soft keyboard).
	//This will be called once, when the input area is first displayed.
	//You can return null to have no input area; the default implementation returns null.
	@Override public View onCreateInputView()
	{
		try
		{
			if(DEBUGGER)
				android.os.Debug.waitForDebugger();
			//Log.e(TAG, System.getProperty("java.version"));//
			LayoutInflater inflater=getLayoutInflater();
			main_layout=(RelativeLayout)inflater.inflate(R.layout.parent_layout, null);
			myView=main_layout.findViewById(R.id.kb_view);
			myView.initCKB(this);
			return main_layout;
		}
		catch(Exception e)
		{
			CKBview3.addError(e);
		//	Log.e(TAG, "exception", e);
		}
		return null;
	}
	//Create and return the view hierarchy used to show candidates.
	//This will be called once, when the candidates are first displayed.
	//You can return null to have no candidates view; the default implementation returns null.
	@Override public View onCreateCandidatesView()
	{
		return null;
	}
	//Called by the framework to create the layout for showing extracted text.
	//Only called when in fullscreen mode. The returned view hierarchy must have an ExtractEditText whose ID is R.id.inputExtractEditText.
	@Override public View onCreateExtractTextView()
	{
		return null;
	}
	//deal with input starting within the input area of the IME.
	//Called when the input view is being shown and input has started on a new editor.
	@Override public void onStartInputView(EditorInfo info, boolean restarting)//restarting: same text field
	{
		if(myView!=null)
		{
			thread_id=Thread.currentThread().getId();
			myView.startCKB(info, restarting);
		}
		setExtractViewShown(false);
	}


	//nppBehavior:
	//when moving up and no line above, move to start of line
	//when moving down and no line below, move to end of line
	boolean nppBehavior=false;
	int findBackward(String text, int start, char c)//returns index of c if found or -1
	{
		for(;start>=0;--start)
			if(text.charAt(start)==c)
				break;
		return start;
	}
	int moveUp(String text, int idx)//expects monospace font
	{
		int nl1=findBackward(text, idx, '\n');
		if(nl1<0)
		{
			if(nppBehavior)
				return 0;
			return idx;
		}
		int col=idx-(nl1+1);
		--nl1;
		int nl0=findBackward(text, nl1, '\n');
		idx=Math.min(nl0+col, nl1);
		return idx;
	}
	int findForward(String text, int start, char c)//returns index of c if found of size
	{
		int size=text.length();
		for(;start<size;++start)
			if(text.charAt(start)==c)
				break;
		return start;
	}
	int moveDown(String text, int idx)
	{
		int nl0=findBackward(text, idx, '\n');
		if(nl0<0)
			nl0=0;
		int col=idx-nl0;
		int nl1=findForward(text, idx, '\n');
		int size=text.length();
		if(nl1==size)
		{
			if(nppBehavior)
				return size;
			return idx;
		}
		int nl2=findForward(text, nl1+1, '\n');
		idx=Math.min(nl1+col, nl2);
		return idx;
	}
	public void onNavigateCallback(int key, int count, int flags)
	{
		try
		{
			InputConnection inCon=getCurrentInputConnection();
			if(inCon==null)
				throw new Exception("onNavigateCallback(): inCon == null");
			boolean success=true;
			switch(flags)
			{
			case CKBview3.CC_CURSOR://TODO: try to send shift down, arrows, shift up
				switch(key)
				{
				case CKBview3.MODMASK|CKBview3.KEY_LEFT:
					for(int k=0;k<count;++k)
					{
						ExtractedText et=inCon.getExtractedText(new ExtractedTextRequest(), 0);
						if(et!=null&&et.selectionStart==0&&et.selectionEnd==0)
							break;
						if(et==null)
						{
							success&=inCon.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_LEFT));
							success&=inCon.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_LEFT));
						}
						else
						{
							int cursor=et.selectionStart;
							if(et.selectionStart!=et.selectionEnd)
							{
								if(cursor>et.selectionEnd)
									cursor=et.selectionEnd;
								inCon.setSelection(cursor, cursor);
							}
							else
							{
								char c=et.text.charAt(cursor-1);
								if((c&0xDC00)==0xDC00)//surrogate pair
								//if(c>=0xD800&&c<=0xDFFF)//X  condition doesn't differentiate between first and second surrogate
									cursor-=2;
								else
									--cursor;
								inCon.setSelection(cursor, cursor);
							}
						}
					}
					break;
				case CKBview3.MODMASK|CKBview3.KEY_RIGHT:
					for(int k=0;k<count;++k)
					{
						ExtractedText et=inCon.getExtractedText(new ExtractedTextRequest(), 0);
						if(et!=null&&et.selectionStart==et.text.length()&&et.selectionEnd==et.text.length())
							break;
						if(et==null)
						{
							success&=inCon.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT));
							success&=inCon.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_RIGHT));
						}
						else
						{
							int cursor=et.selectionStart;
							if(et.selectionStart!=et.selectionEnd)
							{
								if(cursor<et.selectionEnd)
									cursor=et.selectionEnd;
								inCon.setSelection(cursor, cursor);
							}
							else
							{
								char c=et.text.charAt(cursor);
								if((c&0xD800)==0xD800)//surrogate pair
								//if(c>=0xD800&&c<=0xDFFF)//X  condition doesn't differentiate between first and second surrogate
									cursor+=2;
								else
									++cursor;
								inCon.setSelection(cursor, cursor);
							}
						}
					}
					break;
				case CKBview3.MODMASK|CKBview3.KEY_UP:
					for(int k=0;k<count;++k)
					{
						ExtractedText et=inCon.getExtractedText(new ExtractedTextRequest(), 0);
						if(et!=null&&et.selectionStart==0&&et.selectionEnd==0)
							break;
						success&=inCon.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_UP));
						success&=inCon.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_UP));
					}
					break;
				case CKBview3.MODMASK|CKBview3.KEY_DOWN:
					for(int k=0;k<count;++k)
					{
						ExtractedText et=inCon.getExtractedText(new ExtractedTextRequest(), 0);
						if(et!=null&&et.selectionStart==et.text.length()&&et.selectionEnd==et.text.length())
							break;
						success&=inCon.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_DOWN));
						success&=inCon.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_DOWN));
					}
					break;
				}
				break;
			case CKBview3.CC_SEL_END:
				success=inCon.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_SHIFT_LEFT));
				switch(key)
				{
				case CKBview3.MODMASK|CKBview3.KEY_LEFT:
					for(int k=0;k<count;++k)
					{
						success&=inCon.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_LEFT));
						success&=inCon.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_LEFT));
					}
					break;
				case CKBview3.MODMASK|CKBview3.KEY_RIGHT:
					for(int k=0;k<count;++k)
					{
						success&=inCon.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT));
						success&=inCon.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_RIGHT));
					}
					break;
				case CKBview3.MODMASK|CKBview3.KEY_UP:
					for(int k=0;k<count;++k)
					{
						success&=inCon.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_UP));
						success&=inCon.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_UP));
					}
					break;
				case CKBview3.MODMASK|CKBview3.KEY_DOWN:
					for(int k=0;k<count;++k)
					{
						success&=inCon.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_DOWN));
						success&=inCon.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_DOWN));
					}
					break;
				}
				success&=inCon.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_SHIFT_LEFT));
			/*	switch(key)
				{
				case CKBview3.SK_LEFT:
					end-=count;
					if(end<0)
						end=0;
					break;
				case CKBview3.SK_RIGHT:
					end+=count;
					if(end>length)
						end=length;
					break;
				case CKBview3.SK_UP:
					for(int k=0;k<count;++k)
						end=moveUp(text, end);
					break;
				case CKBview3.SK_DOWN:
					for(int k=0;k<count;++k)
						end=moveDown(text, end);
					break;
				}//*/
				break;
			case CKBview3.CC_SEL_START://doesn't work	//TODO: arrows(count), setSelection()
				ExtractedText exText=inCon.getExtractedText(new ExtractedTextRequest(), 0);
				if(exText==null)
					throw new Exception("onNavigateCallback(): text == null");
				String text=exText.text.toString();
				int start=exText.selectionStart, end=exText.selectionEnd, length=text.length();
				//int start=text.startOffset+text.selectionStart, end=text.startOffset+text.selectionEnd, length=text.text.length();
				switch(key)
				{
				case CKBview3.MODMASK|CKBview3.KEY_LEFT:
					start-=count;
					if(start<0)
						start=0;
					break;
				case CKBview3.MODMASK|CKBview3.KEY_RIGHT:
					start+=count;
					if(start>length)
						start=length;
					break;
				case CKBview3.MODMASK|CKBview3.KEY_UP:
					for(int k=0;k<count;++k)
						start=moveUp(text, start);
					break;
				case CKBview3.MODMASK|CKBview3.KEY_DOWN:
					for(int k=0;k<count;++k)
						start=moveDown(text, start);
					break;
				}
				success=inCon.setSelection(start, end);
				break;
			}
			if(!success)
				throw new Exception("onNavigateCallback(): FAILED");
		}
		catch(Exception e)
		{
			Log.e(CKBview3.TAG, e.getMessage());
		}
	}
	public void onKeyCallback(int key, int flags)//flags: {bit1 "2": up, bit0 "1": down}
	{
		InputConnection inCon=getCurrentInputConnection();
		if(inCon!=null)
		{
			//CharSequence selection;
			switch(key)
			{
			case '\n':
				inCon.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER));
				inCon.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER));
				break;
			case '\b':
				inCon.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL));
				inCon.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL));
				break;
			case 127:
				inCon.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_FORWARD_DEL));
				inCon.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_FORWARD_DEL));
				break;
			case CKBview3.MODMASK|CKBview3.KEY_INSERT:
				inCon.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_INSERT));
				inCon.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_INSERT));
				break;
			case CKBview3.MODMASK|CKBview3.KEY_CAPSLOCK:
				myView.isActive_shift=!myView.isActive_shift;
				break;
			case CKBview3.MODMASK|CKBview3.KEY_SHIFT://TODO: separate ACTION_DOWN (long press) & ACTION_UP, short press is normal
				//if(flags==3)
				//	myView.kb.toggleShift();
				//else
				if(flags!=3)
				{
					if((flags&1)!=0)
						inCon.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_CAPS_LOCK));
					if((flags&2)!=0)
						inCon.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_CAPS_LOCK));
				}
				break;
			case CKBview3.MODMASK|CKBview3.KEY_CTRL:
				myView.isActive_ctrl=!myView.isActive_ctrl;
				if(myView.isActive_ctrl)//does nothing
					inCon.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_CTRL_LEFT));
				else
					inCon.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_CTRL_LEFT));
				break;
			case CKBview3.MODMASK|CKBview3.KEY_ALT:
				myView.isActive_alt=!myView.isActive_alt;
				if(myView.isActive_alt)//does nothing
					inCon.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ALT_LEFT));
				else
					inCon.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ALT_LEFT));
				break;
			case CKBview3.MODMASK|CKBview3.KEY_HOME://FIXME sending ctrl down, home, ctrl up doesn't work
				if(myView.isActive_ctrl)//ctrl home
				{
					inCon.setSelection(0, 0);
					myView.isActive_ctrl=false;
				}
				else
				{
					inCon.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MOVE_HOME));
					inCon.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MOVE_HOME));
				}
				break;
			case CKBview3.MODMASK|CKBview3.KEY_END:
				if(myView.isActive_ctrl)//ctrl end
				{
					ExtractedText exText=inCon.getExtractedText(new ExtractedTextRequest(), 0);
					if(exText==null)
						Log.e(TAG, "getExtractedText() FAILED");
					else
					{
						int length=exText.text.length();
						inCon.setSelection(length, length);
						myView.isActive_ctrl=false;
					}
				}
				else
				{
					inCon.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MOVE_END));
					inCon.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MOVE_END));
				}
				break;
			case CKBview3.MODMASK|CKBview3.KEY_PGUP:
				inCon.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_PAGE_UP));
				inCon.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_PAGE_UP));
				break;
			case CKBview3.MODMASK|CKBview3.KEY_PGDN:
				inCon.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_PAGE_DOWN));
				inCon.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_PAGE_DOWN));
				break;
			case CKBview3.MODMASK|CKBview3.KEY_F1:case CKBview3.MODMASK|CKBview3.KEY_F2:case CKBview3.MODMASK|CKBview3.KEY_F3:case CKBview3.MODMASK|CKBview3.KEY_F4:
			case CKBview3.MODMASK|CKBview3.KEY_F5:case CKBview3.MODMASK|CKBview3.KEY_F6:case CKBview3.MODMASK|CKBview3.KEY_F7:case CKBview3.MODMASK|CKBview3.KEY_F8:
			case CKBview3.MODMASK|CKBview3.KEY_F9:case CKBview3.MODMASK|CKBview3.KEY_F10:case CKBview3.MODMASK|CKBview3.KEY_F11:case CKBview3.MODMASK|CKBview3.KEY_F12:
				int functionKey=KeyEvent.KEYCODE_F1+(key&~CKBview3.MODMASK)-CKBview3.KEY_F1;
				inCon.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, functionKey));
				inCon.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, functionKey));
				break;
			case CKBview3.MODMASK|CKBview3.KEY_PRINTSCREEN://does nothing
				inCon.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_SYSRQ));
				inCon.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_SYSRQ));
				break;
			case CKBview3.MODMASK|CKBview3.KEY_PAUSE://useless: this is sent to an EditText
				inCon.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE));
				inCon.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE));
				break;
			case CKBview3.MODMASK|CKBview3.KEY_NUMLOCK:
				inCon.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_NUM_LOCK));
				inCon.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_NUM_LOCK));
				break;
			case CKBview3.MODMASK|CKBview3.KEY_SCROLLLOCK:
				inCon.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_SCROLL_LOCK));
				inCon.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_SCROLL_LOCK));
				break;
			case CKBview3.MODMASK|CKBview3.KEY_LEFT:
				{
					ExtractedText et=inCon.getExtractedText(new ExtractedTextRequest(), 0);
					if(et==null||et.selectionStart>0||et.selectionEnd>0)
					{
						inCon.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_LEFT));
						inCon.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_LEFT));
					}
				}
				break;
			case CKBview3.MODMASK|CKBview3.KEY_RIGHT:
				{
					ExtractedText et=inCon.getExtractedText(new ExtractedTextRequest(), 0);
					if(et==null||et.selectionStart<et.text.length()||et.selectionEnd<et.text.length())
					{
						inCon.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT));
						inCon.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_RIGHT));
					}
				}
				break;
			case CKBview3.MODMASK|CKBview3.KEY_UP:
				{
					ExtractedText et=inCon.getExtractedText(new ExtractedTextRequest(), 0);
					if(et==null||et.selectionStart>0||et.selectionEnd>0)
					{
						inCon.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_UP));
						inCon.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_UP));
					}
				}
				break;
			case CKBview3.MODMASK|CKBview3.KEY_DOWN:
				{
					ExtractedText et=inCon.getExtractedText(new ExtractedTextRequest(), 0);
					if(et==null||et.selectionStart<et.text.length()||et.selectionEnd<et.text.length())
					{
						inCon.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_DOWN));
						inCon.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_DOWN));
					}
				}
				break;

			case CKBview3.MODMASK|CKBview3.KEY_SYMBOLS:
				myView.toggleSymbolsExtension();
				break;
			case CKBview3.MODMASK|CKBview3.KEY_SETTINGS://TODO: keyboard settings activity (unicode search, layout customization, color theme)
				if((flags&1)!=0)//down
				{
					//Intent intent=new Intent(this, SettingsActivity.class);
					Intent intent=new Intent(this, CKBactivity.class);//https://developer.android.com/training/basics/firstapp/starting-activity
					//Intent intent=new Intent("com.example.ckbdemo.CKBsettings");//https://stackoverflow.com/questions/13636631/how-to-start-activity-for-result-from-ime
					//intent.putExtra("keyboard", true);
					intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					startActivity(intent);
				}
				break;
			case CKBview3.MODMASK|CKBview3.KEY_NAB:
				//this should be empty
				break;
			case 'A':case 'a':
				if(myView.isActive_ctrl)
				{
					ExtractedText exText=inCon.getExtractedText(new ExtractedTextRequest(), 0);
					if(exText==null)
						Log.e(TAG, "getExtractedText() FAILED");
					else
					{
						int length=exText.text.length();
						inCon.setSelection(0, length);
						myView.isActive_ctrl=false;
					}
				}
				else
					sendAsIs(key, inCon);
				break;
			case CKBview3.MODMASK|CKBview3.KEY_CUT:
				{
					boolean success;
					if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.N)//TODO: test this
					{
						success=inCon.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_COPY));
						success&=inCon.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_COPY));
						if(success)
							displayToast("Copied to clipboard");
					}
					else
						success=copySelectionToClipboardLoud(inCon);
					if(success)
					{
						inCon.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL));
						inCon.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL));
					}
					else
						displayToast("Failed to copy");
				}
				break;
			case CKBview3.MODMASK|CKBview3.KEY_COPY:
				{
					boolean success;
					if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.N)
					{
						success=inCon.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_COPY));
						success&=inCon.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_COPY));
						if(success)
							displayToast("Copied to clipboard");
					}
					else
						success=copySelectionToClipboardLoud(inCon);
					if(!success)
						displayToast("Failed to copy");
				}
				break;
			case CKBview3.MODMASK|CKBview3.KEY_PASTE:
				{
					boolean success=pasteFromClipboard(inCon);
					if(!success)
						displayToast("Failed to paste");
				}
				break;
			case 'C':case 'c':
				if(myView.isActive_ctrl)
				{
					boolean success;
					if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.N)
					{
						success=inCon.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_COPY));
						success&=inCon.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_COPY));
						if(success)
							displayToast("Copied to clipboard");
						else
							displayToast("Failed to copy");
					}
					else
						success=copySelectionToClipboardLoud(inCon);
					if(success)
						myView.isActive_ctrl=false;
				}
				else
					sendAsIs(key, inCon);
				break;
			case 'V':case 'v':
				if(myView.isActive_ctrl)
				{
					boolean success=pasteFromClipboard(inCon);
					if(!success)
						displayToast("Failed to paste");
					myView.isActive_ctrl=false;
				}
				else
					sendAsIs(key, inCon);
				break;
			case 'X':case 'x':
				if(myView.isActive_ctrl)
				{
					boolean success;
					if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.N)
					{
						success=inCon.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_COPY));
						success&=inCon.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_COPY));
					}
					else
						success=copySelectionToClipboardLoud(inCon);
					if(success)
					{
						inCon.commitText("", 1);
						myView.isActive_ctrl=false;
					}
				}
				else
					sendAsIs(key, inCon);
				break;
			default:
				sendAsIs(key, inCon);
				break;
			}//end switch
		}//end if inCon!=null
	}
	void displayToast(String str)
	{
		Handler mainHandler = new Handler(getMainLooper());
		mainHandler.post(()->Toast.makeText(getApplicationContext(), str, Toast.LENGTH_SHORT).show());
		//mainHandler.post(new Runnable()
		//{
		//	@Override public void run()
		//	{
		//		Toast.makeText(getApplicationContext(), str, Toast.LENGTH_SHORT).show();
		//	}
		//});
	}
	boolean copySelectionToClipboardLoud(InputConnection inCon)
	{
		CharSequence cs=inCon.getSelectedText(0);
		if(cs!=null&&cs.length()>0)
		{
			ClipboardManager clipboard=(ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
			if(clipboard==null)
			{
				Log.e(TAG, "clipboard == null");
				return false;
			}
			ClipData data=ClipData.newPlainText("simple text", cs);
			clipboard.setPrimaryClip(data);
			displayToast("Copied to clipboard");
			return true;
		}
		return false;
	}
	boolean pasteFromClipboard(InputConnection inCon)
	{
		ClipboardManager clipboard=(ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
		if(clipboard==null)
		{
			Log.e(TAG, "clipboard == null");
			return false;
		}
		ClipData clipData=clipboard.getPrimaryClip();
		if(clipData==null)
		{
			Log.e(TAG, "clipboard.getPrimaryClip() == null");
			return false;
		}
		ClipData.Item item=clipData.getItemAt(0);
		if(item==null)
		{
			Log.e(TAG, "clipData.getItemAt(0) == null");
			return false;
		}
		CharSequence cs=item.getText();
		String str;
		if(cs!=null)
			str=cs.toString();
		else
		{
			Uri url=item.getUri();
			if(url==null)
				return false;
			str=url.toString();
		}
		inCon.commitText(str, 1);//2020-07-11
	//	displayToast("Pasted from clipboard");
		return true;
	}
	void sendAsIs(int key, InputConnection inCon)
	{
		if(key>=0&&key<0x110000)
		{
			if(myView.isActive_shift)
				key=Character.toUpperCase(key);
			else
				key=Character.toLowerCase(key);
			CharSequence cs=CharBuffer.wrap(Character.toChars(key));
			inCon.commitText(cs, 1);
		}
	}
}