/*  TouchInfo.java - A class for tracking multitouch gestures
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

import android.util.Log;
import android.view.MotionEvent;

import java.util.ArrayList;
import java.util.Collections;

public class TouchInfo
{
	public static final int S_DOWN=0, S_MOVED=1, S_UP=2, S_CANCEL=3;
	public static class Pointer
	{
		int state,//down/up
			id;//order of appearance
		float x, y, prevx, prevy, startx, starty;
		Pointer(int _id, float _x, float _y){state=S_DOWN; id=_id; x=_x; y=_y; startx=_x; starty=_y;}
		void update(float _x, float _y)
		{
			prevx=x; prevy=y;
			x=_x; y=_y;
			state=S_MOVED;
		}
		boolean isValid(){return state!=S_UP&&state!=S_CANCEL;}
		float xTravel(){return x-startx;}
		float yTravel(){return y-starty;}
	}
	public ArrayList<Pointer> pointers=new ArrayList<>();
	MotionEvent e;
	int frame_counter=0;

	public int size(){return pointers.size();}
	public Pointer get(int idx){return pointers.get(idx);}
	public int findPointerIndex(int id)//returns data index given the unique identifier, if not found: returns size
	{
		int idx=0, size=pointers.size();
		for(;idx<size;++idx)
			if(pointers.get(idx).id==id)
				break;
		return idx;
	}
	public Pointer findPointer(int id)//the unique identifier
	{
		int idx=0, size=pointers.size();
		for(;idx<size;++idx)
		{
			Pointer p=pointers.get(idx);
			if(p.id==id)
				return p;
		}
		return null;
	}
	public void update(MotionEvent _e)
	{
		e=_e;
		int action=e.getActionMasked(), actionIndex=e.getActionIndex();
		for(int k=0;k<pointers.size();)//update pointers & remove invalid pointers
		{
			Pointer ti=pointers.get(k);
			if(ti.state==TouchInfo.S_UP||ti.state==TouchInfo.S_CANCEL)//was marked as UP before: remove
				pointers.remove(k);
			else//maintain old pointers
			{
				int index=e.findPointerIndex(ti.id);
				if(index==-1)
					ti.state=TouchInfo.S_UP;
				else
				{
					ti.update(e.getX(index), e.getY(index));
					if(index==actionIndex)
					{
						switch(action)
						{
						case MotionEvent.ACTION_DOWN://0
						case MotionEvent.ACTION_POINTER_DOWN://5
							ti.state=TouchInfo.S_DOWN;//unreachable
							break;
						case MotionEvent.ACTION_MOVE://2
						case MotionEvent.ACTION_OUTSIDE://4
							break;
						case MotionEvent.ACTION_UP://1
						case MotionEvent.ACTION_POINTER_UP://6
							ti.state=TouchInfo.S_UP;
							break;
						case MotionEvent.ACTION_CANCEL://3
							ti.state=TouchInfo.S_CANCEL;
							break;
						}
					}
				}
				++k;
			}
		}
		int np=e.getPointerCount();
		for(int k=0;k<np;++k)//add new (fresh) pointers
		{
			int id=e.getPointerId(k);
			int idx=findPointerIndex(id), size=pointers.size();
			if(idx==size)//fresh
			{
				pointers.add(new Pointer(id, e.getX(k), e.getY(k)));
				if(k==actionIndex)
				{
					Pointer ti=pointers.get(idx);
					switch(action)
					{
					case MotionEvent.ACTION_DOWN://0
					case MotionEvent.ACTION_POINTER_DOWN://5
						ti.state=TouchInfo.S_DOWN;
						break;
					case MotionEvent.ACTION_MOVE://2
					case MotionEvent.ACTION_OUTSIDE://4
						ti.state=TouchInfo.S_MOVED;
						break;
					case MotionEvent.ACTION_UP://1
					case MotionEvent.ACTION_POINTER_UP://6
						ti.state=TouchInfo.S_UP;
						break;
					case MotionEvent.ACTION_CANCEL://3
						ti.state=TouchInfo.S_CANCEL;
						break;
					}
				}
			}
		}//end of update pointers
		Collections.sort(pointers, (o1, o2)->Integer.compare(o1.id, o2.id));//sort pointers by order of appearance
		++frame_counter;
	}
	public void log(String TAG)
	{
		int np=e.getPointerCount(), size=pointers.size();
		Log.e(TAG, String.format("%d FRAME: ti.size=%d, np=%d%s", frame_counter, size, np, size!=np?", !!!BROKEN!!!":""));
		for(int k=0;k<size;++k)
		{
			TouchInfo.Pointer ti=pointers.get(k);
			switch(ti.state)
			{
			case TouchInfo.S_DOWN:
				Log.e(TAG, String.format("%d:  [%d] DOWN (%f, %f)", frame_counter, ti.id, ti.x, ti.y));
				break;
			case TouchInfo.S_MOVED:
				Log.e(TAG, String.format("%d:  [%d] MOVE (%f, %f)->(%f, %f)", frame_counter, ti.id, ti.prevx, ti.prevy, ti.x, ti.y));
				break;
			case TouchInfo.S_UP:
				Log.e(TAG, String.format("%d:  [%d] UP (%f, %f)", frame_counter, ti.id, ti.x, ti.y));
				break;
			case TouchInfo.S_CANCEL:
				Log.e(TAG, String.format("%d:  [%d] CANCEL (%f, %f)", frame_counter, ti.id, ti.x, ti.y));
				break;
			default:
				Log.e(TAG, String.format("%d:  [%d] UNRECOGNIZED STATE = %d", frame_counter, ti.id, ti.state));
				break;
			}
		}
	}
}
