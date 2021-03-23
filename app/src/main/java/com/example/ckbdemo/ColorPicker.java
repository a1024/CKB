/*  ColorPicker.java - A color picker used by the settings activity
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
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.text.InputType;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.EditText;

public class ColorPicker extends ViewGroup
{
	public static final String TAG="CKBview2";
	public static class HSL
	{
		float
			h,//hue: 0~360 degrees
			s,//saturation: 0~1
			l,//lightness: 0~1
			a;//alpha: 0~1
		void set(float _h, float _s, float _l, float _a){h=_h; s=_s; l=_l; a=_a;}
	}
	public static HSL rgb2hsl(int color)
	{
		HSL ret=new HSL();
		final float inv255=1.f/255;
		final int r_idx=0, g_idx=1, b_idx=2;
		float[] rgb={(color>>16&0xFF)*inv255, (color>>8&0xFF)*inv255, (color&0xFF)*inv255, (color>>24&0xFF)*inv255};
		int max_idx=0, min_idx=0;
		for(int k=1;k<3;++k)
		{
			if(rgb[max_idx]<rgb[k])
				max_idx=k;
			if(rgb[min_idx]>rgb[k])
				min_idx=k;
		}
		ret.a=rgb[3];
		ret.l=(rgb[max_idx]+rgb[min_idx])*0.5f;
		if(rgb[max_idx]==rgb[min_idx])
			ret.h=ret.s=0;//achromatic
		else
		{
			float d=rgb[max_idx]-rgb[min_idx], sum=rgb[max_idx]+rgb[min_idx];
			ret.s=ret.l>0.5f?d/(2-sum):d/sum;
			switch(max_idx)
			{
			case 0:ret.h=(rgb[g_idx]-rgb[b_idx])/d+(rgb[g_idx]<rgb[b_idx]?6:0);break;
			case 1:ret.h=(rgb[b_idx]-rgb[r_idx])/d+2;break;
			case 2:ret.h=(rgb[r_idx]-rgb[g_idx])/d+4;break;
			}
			ret.h*=360.f/6;
			//ret.h/=6;
		}
		return ret;
	}
	private static float hue2rgb(float p, float q, float t)
	{
		if(t<0)
			t+=1;
		if(t>1)
			t-=1;
		if(t<1.f/6)
			return p+(q-p)*6*t;
		if(t<0.5f)
			return q;
		if(t<2.f/3)
			return p+(q-p)*(2.f/3-t)*6;
		return p;
	}
	public static int hsl2rgb(float h, float s, float l, float a)//h: 0~360, s/l/a: 0~1
	{//https://stackoverflow.com/questions/2353211/hsl-to-rgb-color-conversion
		int r, g, b;
		if(s==0)
			r=g=b=(int)(255*l);//achromatic
		else
		{
			h/=360;
			float
				q=l<0.5f?l*(1+s):l+s-l*s,
				p=l+l-q;
			r=(int)(255*hue2rgb(p, q, h+1.f/3));
			g=(int)(255*hue2rgb(p, q, h));
			b=(int)(255*hue2rgb(p, q, h-1.f/3));
		}
		return (int)(255*a)<<24|r<<16|g<<8|b;
	}
	public static int hsl2rgb(HSL hsl){return hsl2rgb(hsl.h, hsl.s, hsl.l, hsl.a);}
	static void setHueBmp(Bitmap bmp)
	{
		int w=bmp.getWidth(), h=bmp.getHeight();
		float h_mul=360.f/w;
		for(int kx=0;kx<w;++kx)
		{
			int color=hsl2rgb(kx*h_mul, 1, 0.5f, 1);
			for(int ky=0;ky<h;++ky)
				bmp.setPixel(kx, ky, color);
		}
	}
	static void setSLBmp(Bitmap bmp, float hue)
	{
		int w=bmp.getWidth(), h=bmp.getHeight();
		float s_mul=1.f/w, l_mul=1.f/h;
		for(int ky=0;ky<h;++ky)
		{
			float lum=ky*l_mul;
			for(int kx=0;kx<w;++kx)
			{
				int color=hsl2rgb(hue, kx*s_mul, lum, 1);
				bmp.setPixel(kx, ky, color);
			}
		}
	}
	static Bitmap generateGradient(int start, int end)
	{
		int w=256, w1=w-1, h=10;
		int[]
			c1={start&0xFF, start>>8&0xFF, start>>16&0xFF, start>>24&0xFF},
			c2={end&0xFF, end>>8&0xFF, end>>16&0xFF, end>>24&0xFF},
			temp=new int[4];
		Bitmap bm=Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
		for(int kx=0;kx<w;++kx)
		{
			temp[0]=c1[0]+(c2[0]-c1[0])*kx/w1;
			temp[1]=c1[1]+(c2[1]-c1[1])*kx/w1;
			temp[2]=c1[2]+(c2[2]-c1[2])*kx/w1;
			temp[3]=c1[3]+(c2[3]-c1[3])*kx/w1;
			//for(int kc=0;kc<4;++kc)
			//	temp[kc]=c1[kc]+(c2[kc]-c1[kc])*kx/w1;
				//temp[kc]=(c1[kc]*(w1-kx)+c2[kc]*kx)/w1;
			int color=temp[3]<<24|temp[2]<<16|temp[1]<<8|temp[0];
			for(int ky=0;ky<h;++ky)
				bm.setPixel(kx, ky, color);
		}
		return bm;
	}

	//color picker
	Context context;
	int startId;
	static final int
		BOX_HUE=0, BOX_SAT=1, BOX_LUM=2, BOX_RED=3, BOX_GREEN=4, BOX_BLUE=5, BOX_ALPHA=6,
		BOX_COUNT=7,
		BM1=7, BM2=8, PREVIEW=9,
		SLIDER_R=10, SLIDER_G=11, SLIDER_B=12, SLIDER_A=13,
		OBJ_COUNT=14;
	String[] names=
	{
		"Hue:", "Sat:", "Lum:", "Red:", "Green:", "Blue:", "Alpha:",//for GUI
		"BM1", "BM2", "Preview", "R slider", "G slider", "B slider", "A slider"//for DEBUG
	};

	HSL ch_hsl=new HSL();//current choice
	int ch_rgb;
	Bitmap bm1, bm2;
	int bm1_w, bm1_h, bm2_w, bm2_h;
	Matrix identity, m1, m2;

	float textSize;
	int px, py, dx, dy;
	Paint paint_black, paint_white, paint_hue, paint_ch;

	Bitmap[] bm_slider=new Bitmap[4];//sliders
	Paint paint_marker;
	Matrix m_slider=new Matrix();

	EditText[] boxes=new EditText[BOX_COUNT];
	Rect[] bounds=new Rect[OBJ_COUNT];
	Rect rect;
	Path path;//slider triangle marker, depends on text size
	public ColorPicker(Context _context										){super(_context);					context=_context; init();}
	public ColorPicker(Context _context, AttributeSet attrs					){super(_context, attrs);			context=_context; init();}
	public ColorPicker(Context _context, AttributeSet attrs, int defStyle	){super(_context, attrs, defStyle);	context=_context; init();}
	public ColorPicker(Context _context, int viewId							){super(_context); startId=viewId;	context=_context; init();}
	EditText prepareBox(int viewId)
	{
		EditText et=new EditText(context);
		et.setSingleLine();
		et.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_VARIATION_PASSWORD);
		et.setTransformationMethod(null);
		et.setId(viewId);
		et.setPadding(0, 0, 0, 0);
		et.setGravity(Gravity.BOTTOM);//BOTTOM does nothing
		//et.setGravity(Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL);//CENTER_HORIZONTAL: disappeared
		addView(et);
		return et;
	}
	//void setLumBar(float h, float s)
	//{
	//	float l_mul=1.f/bm2_w;
	//	for(int kx=0;kx<bm2_w;++kx)
	//	{
	//		int color=hsl2rgb(h, s, kx*l_mul, 1);
	//		for(int ky=0;ky<bm2_h;++ky)
	//			bm2.setPixel(kx, ky, color);
	//	}
	//}
	void logColors(HSL hsl, int rgb, String op)
	{
		int r=rgb&0xFF, g=rgb>>8&0xFF, b=rgb>>16&0xFF, a=rgb>>24&0xFF;
		Log.e(TAG, String.format("hsl,a(%f, %f, %f, %f) %s rgba(%d, %d, %d, %d)", hsl.h, hsl.s, hsl.l, hsl.a, op, r, g, b, a));
	}
	void init()
	{
		//ch_hsl.set(160, 0, 0, 1);
		//ch_rgb=hsl2rgb(ch_hsl);

		ch_hsl.set(72.3f, 0.4208f, 0.3625f, 1);
		ch_rgb=hsl2rgb(ch_hsl);
		//logColors(ch_hsl, ch_rgb, "->");//
		//ch_rgb=0xFF000000|53<<16|132<<8|116;
		//ch_hsl=rgb2hsl(ch_rgb);
		//logColors(ch_hsl, ch_rgb, "<-");//

		bm1_w=360; bm1_h=30;
		//bm1_w=360; bm1_h=100;
		bm1=Bitmap.createBitmap(bm1_w, bm1_h, Bitmap.Config.ARGB_8888);//Bmp: 0xAABBGGRR	X swapped
		bm2_w=100; bm2_h=100;
		bm2=Bitmap.createBitmap(bm2_w, bm2_h, Bitmap.Config.ARGB_8888);
		setHueBmp(bm1);

		identity=new Matrix();
		m1=new Matrix();
		m2=new Matrix();

		paint_black	=new Paint();	paint_black.setColor(0xFF000000);//API: 0xAARRGGBB
		paint_white	=new Paint();	paint_white.setColor(0xFFFFFFFF);
		paint_hue	=new Paint();
		paint_ch	=new Paint();

		int[] colors={Color.RED, Color.GREEN, Color.BLUE, Color.WHITE};
		for(int k=0;k<4;++k)
			bm_slider[k]=generateGradient(0xFF000000, colors[k]);
		paint_marker=new Paint();
		paint_marker.setDither(true);
		paint_marker.setColor(Color.BLACK);
		paint_marker.setStyle(Paint.Style.FILL);
		paint_marker.setStrokeJoin(Paint.Join.ROUND);
		paint_marker.setStrokeCap(Paint.Cap.ROUND);
		paint_marker.setStrokeWidth(3);

		for(int k=0;k<BOX_COUNT;++k)
			boxes[k]=prepareBox(startId+1+k);

		for(int k=0;k<OBJ_COUNT;++k)
			bounds[k]=new Rect();

		setWillNotDraw(false);

		setChoice(true);
		paint_debug.setColor(0x80808080);
	}
	void setTextSize(float _TextSize)
	{
		textSize=_TextSize;
		paint_black.setTextSize(textSize);
		for(int k=0;k<BOX_COUNT;++k)
			boxes[k].setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize*0.75f);
	}
	@Override protected void onLayout(boolean changed, int l, int t, int r, int b)
	{
		//int padding=10;
		px=l; py=t; dx=r-l; dy=b-t;
		int h_total=(int)(dy*(1-0.32f));
		int yPos=0, vh;
		vh=h_total/8;	bounds[BM1].set(0, yPos, dx, yPos+vh);		yPos+=vh;

		vh=h_total*3/8;
		int x1=dx/3, x2=x1+dx*4/9;
				bounds[BM2].set(x1, yPos, x2, yPos+vh);	bounds[PREVIEW].set(x2, yPos, dx, yPos+vh);

		vh=h_total/8;
		bounds[BOX_HUE	].set(0, yPos, x1, yPos+vh);	yPos+=vh;
		bounds[BOX_SAT	].set(0, yPos, x1, yPos+vh);	yPos+=vh;
		bounds[BOX_LUM	].set(0, yPos, x1, yPos+vh);	yPos+=vh;

		//vh=h_total/16;
		bounds[BOX_RED	].set(0, yPos, x1, yPos+vh);	bounds[SLIDER_R].set(x1, yPos, dx, yPos+vh);	yPos+=vh;
		bounds[BOX_GREEN].set(0, yPos, x1, yPos+vh);	bounds[SLIDER_G].set(x1, yPos, dx, yPos+vh);	yPos+=vh;
		bounds[BOX_BLUE	].set(0, yPos, x1, yPos+vh);	bounds[SLIDER_B].set(x1, yPos, dx, yPos+vh);	yPos+=vh;
		bounds[BOX_ALPHA].set(0, yPos, x1, yPos+vh);	bounds[SLIDER_A].set(x1, yPos, dx, yPos+vh);

		if(path==null)
		{
			int reach=vh/4;
			path=new Path();
			path.lineTo(-reach, reach);
			path.lineTo( reach, reach);
			path.close();
		}

		//Log.e(TAG, bounds[BOX_HUE].toShortString());
		for(int k=0;k<BOX_COUNT;++k)
		{
			rect=bounds[k];
			int w=rect.right-rect.left, h=rect.bottom-rect.top;
			EditText box=boxes[BOX_HUE+k];
			ViewGroup.LayoutParams lp=box.getLayoutParams();
			if(lp==null)
				lp=new ViewGroup.LayoutParams(w, h);
			else
			{
				lp.width=w;
				lp.height=h;
			}
			box.setLayoutParams(lp);
			//Log.e(TAG, String.format("layout %d: %d, %d, %d, %d,  h=%d", k, rect.left, rect.top, rect.right, rect.bottom, h));//
			//Log.e(TAG, String.format("layout %d: %d, %d, %d, %d", k, rect.left, rect.top+h/2, rect.right, rect.top+h));//
			box.layout(rect.left, rect.top+h/2, rect.right, rect.top+h);
		}

		rect=bounds[BM1];
		m1.set(identity);
		m1.postScale((float)(rect.right-rect.left)/bm1_w, (float)(rect.bottom-rect.top)/bm1_h);
		m1.postTranslate(rect.left, rect.top);

		rect=bounds[BM2];
		m2.set(identity);
		m2.postScale((float)(rect.right-rect.left)/bm2_w, (float)(rect.bottom-rect.top)/bm2_h);
		m2.postTranslate(rect.left, rect.top);
	}

	void setChoice(boolean srcIsHSL)
	{
		if(srcIsHSL)
			ch_rgb=hsl2rgb(ch_hsl);
		else
			ch_hsl=rgb2hsl(ch_rgb);

		paint_hue.setColor(hsl2rgb(ch_hsl.h, 1, 0.5f, 1));
		paint_ch.setColor(hsl2rgb(ch_hsl));
		setSLBmp(bm2, ch_hsl.h);

		boxes[BOX_HUE].setText(String.format("%f", ch_hsl.h));
		boxes[BOX_SAT].setText(String.format("%f", ch_hsl.s));
		boxes[BOX_LUM].setText(String.format("%f", ch_hsl.l));
		boxes[BOX_RED	].setText(String.format("%d", ch_rgb>>16&0xFF));
		boxes[BOX_GREEN	].setText(String.format("%d", ch_rgb>>8&0xFF));
		boxes[BOX_BLUE	].setText(String.format("%d", ch_rgb&0xFF));
		boxes[BOX_ALPHA	].setText(String.format("%d", ch_rgb>>24&0xFF));
	}

	void drawStrongHLine(float x1, float x2, float y, Canvas canvas)
	{
		canvas.drawLine(x1, y-1, x2, y-1, paint_white);
		canvas.drawLine(x1, y  , x2, y  , paint_black);
		canvas.drawLine(x1, y+1, x2, y+1, paint_white);
	}
	void drawStrongVLine(float x, float y1, float y2, Canvas canvas)
	{
		canvas.drawLine(x-1, y1, x-1, y2, paint_white);
		canvas.drawLine(x  , y1, x  , y2, paint_black);
		canvas.drawLine(x+1, y1, x+1, y2, paint_white);
	}
	void drawCrossHair(float x, float y, Canvas canvas)
	{
		float r1=10, r2=50;
		drawStrongHLine(x-r2, x-r1, y, canvas);
		drawStrongHLine(x+r1, x+r2, y, canvas);
		drawStrongVLine(x, y-r1, y-r2, canvas);
		drawStrongVLine(x, y+r1, y+r2, canvas);
	}
	void drawBox(int idx, Canvas canvas)
	{
		Rect r=bounds[idx];
		float w=r.right-r.left, h=r.bottom-r.top, h2=h*0.5f;
		canvas.drawText(names[idx], r.left, r.top+textSize, paint_black);

		//float x=r.left, y=r.top+h2;
		//canvas.translate(x, y);
		//boxes[idx].draw(canvas);
		//canvas.translate(-x, -y);
	}
	void drawSlider(Rect r, Bitmap bm, int val255, Canvas canvas)
	{
		//Log.e(TAG, String.format("drawSlider() val=%d", val255));//

		float w=r.right-r.left, h=r.bottom-r.top;
		float x=val255*w/255, h2=h*0.5f;
		canvas.translate(r.left, r.top);
		m_slider.set(identity);
		m_slider.postScale(w/bm.getWidth(), h2/bm.getHeight());
		canvas.drawBitmap(bm, m_slider, null);

		if(path!=null)
		{
			//Log.e(TAG, String.format("drawSlider(): val=%d, h=%f, translating by (%f, %f)", val255, h, x, h2));//
			canvas.translate(x, h2);
			canvas.drawPath(path, paint_marker);
			//canvas.drawRect(0, 0, h2, h2, paint_debug);//
			canvas.translate(-x, -h2);
		}

		canvas.translate(-r.left, -r.top);
	}
	Paint paint_debug=new Paint();//
	@Override public void onDraw(Canvas canvas)
	{
		if(dx==0||dy==0)
		{
			Log.e(TAG, String.format("ColorPicker onDraw() dx=%d, dy=%d!!", dx, dy));
			return;
		}
		//Log.e(TAG, String.format("colorPicker.onDraw(); (%d, %d), %dx%d", px, py, dx, dy));
		//float r=20;
		//rectf.set(r, r, dx-r, dy-r);
		//canvas.drawRoundRect(rectf, r, r, paint_bmp);

		canvas.drawBitmap(bm1, m1, null);//draw bitmaps
		rect=bounds[BM1];
		int h1=(rect.bottom-rect.top)/2;
		canvas.drawRect(rect.left, rect.top+h1, rect.right, rect.bottom, paint_hue);
		canvas.drawBitmap(bm2, m2, null);
		rect=bounds[PREVIEW];

		float w2=(rect.right-rect.left)*0.5f;
		canvas.drawRect(rect.left,		rect.top, rect.left+w2, rect.bottom, paint_white);
		canvas.drawRect(rect.left+w2,	rect.top, rect.right,	rect.bottom, paint_black);
		canvas.drawRect(rect.left, rect.top, rect.right, rect.bottom, paint_ch);//draw preview
		//int h1=(rect.bottom-rect.top)/3;
		//canvas.drawRect(rect.left, rect.top,	rect.right, rect.top+h1, paint_hue);
		//canvas.drawRect(rect.left, rect.top+h1, rect.right, rect.bottom, paint_ch);

		int[] comp={ch_rgb>>16&0xFF, ch_rgb>>8&0xFF, ch_rgb&0xFF, ch_rgb>>24&0xFF};
		for(int k=0;k<4;++k)//draw sliders
			drawSlider(bounds[SLIDER_R+k], bm_slider[k], comp[k], canvas);

		//float y=0;
		for(int k=0;k<BOX_COUNT;++k)//draw boxes
			drawBox(k, canvas);

		//draw markers
		rect=bounds[BM1];
		drawStrongVLine(rect.left+ch_hsl.h/360.f*(rect.right-rect.left), rect.top, rect.bottom, canvas);
		//Log.e(TAG, String.format("V line: x=%f, y1=%d, y2=%d", rect.left+ch_hsl.h/360.f*(rect.right-rect.left), rect.top, rect.bottom));//
		rect=bounds[BM2];
		drawCrossHair(rect.left+ch_hsl.s*(rect.right-rect.left), rect.top+ch_hsl.l*(rect.bottom-rect.top), canvas);
		//Log.e(TAG, String.format("X hair: x=%f, y=%f", rect.left+ch_hsl.s*(rect.right-rect.left), rect.top+ch_hsl.l*(rect.bottom-rect.top)));//

		//float radius=20;
		////paint_black.setTextSize(30);
		//for(int k=0;k<OBJ_COUNT;++k)
		//{
		//	rect=bounds[k];
		//	rectF.set(rect.left+10, rect.top+10, rect.right-10, rect.bottom-10);
		//	canvas.drawRoundRect(rectF, radius, radius, paint_debug);
		//	canvas.drawText(names[k], (rect.left+rect.right)*0.5f, (rect.top+rect.bottom)*0.5f, paint_black);
		//}
	}
	TouchInfo ti=new TouchInfo();
	int hit_idx=OBJ_COUNT;
	boolean hitTest(Rect r, TouchInfo.Pointer p)
	{
		return p.x>=r.left&&p.x<r.right&&p.y>=r.top&&p.y<r.bottom;
	}
	float clamp0(float x, float hi)
	{
		if(x<0)
			return 0;
		return Math.min(x, hi);
	}
	int clamp0(int x, int hi)
	{
		if(x<0)
			return 0;
		return Math.min(x, hi);
	}
	@Override public boolean onTouchEvent(MotionEvent e)//returns true if gesture hit, false if miss
	{
		ti.update(e);
		//ti.log("CKBColorPicker");
		TouchInfo.Pointer p=ti.findPointer(0);
		if(p!=null)
		{
			for(hit_idx=0;hit_idx<OBJ_COUNT;++hit_idx)
				if(hitTest(bounds[hit_idx], p))
					break;
			int c;
			int[] comp_idx={16, 8, 0, 24};
			int[] comp_mask={0xFF00FFFF, 0xFFFF00FF, 0xFFFFFF00, 0x00FFFFFF};
			switch(hit_idx)
			{
			case BOX_HUE://TODO: detect scroll & return false (except BM2)
			case BOX_SAT:
			case BOX_LUM:
			case BOX_RED:
			case BOX_GREEN:
			case BOX_BLUE:
			case BOX_ALPHA:
			//	boxes[hit_idx].onTouchEvent(e);//doesn't work
				break;
			case BM1:
				if(p.state==TouchInfo.S_MOVED)
				{
					rect=bounds[BM1];
					ch_hsl.h+=(p.x-p.prevx)*360.f/(rect.right-rect.left);	ch_hsl.h=clamp0(ch_hsl.h, 360);
					//ch_hsl.h=(p.x-rect.left)*360.f/(rect.right-rect.left);
					setChoice(true);
				}
				break;
			case BM2:
				if(p.state==TouchInfo.S_MOVED)
				{
					rect=bounds[BM2];
					ch_hsl.s+=(p.x-p.prevx)/(rect.right-rect.left);	ch_hsl.s=clamp0(ch_hsl.s, 1);
					ch_hsl.l+=(p.y-p.prevy)/(rect.bottom-rect.top);	ch_hsl.l=clamp0(ch_hsl.l, 1);
				//	ch_hsl.s=(p.x-rect.left)/(rect.right-rect.left);
				//	ch_hsl.l=(p.y-rect.top)/(rect.bottom-rect.top);
					setChoice(true);
				}
				break;
			case PREVIEW:
				break;
			case SLIDER_R:
			case SLIDER_G:
			case SLIDER_B:
			case SLIDER_A:
				if(p.state==TouchInfo.S_MOVED)
				{
					rect=bounds[hit_idx];
					c=ch_rgb>>comp_idx[hit_idx-SLIDER_R]&0xFF;
					c+=(int)(p.x-p.prevx)*255/(rect.right-rect.left);	c=clamp0(c, 255);
				//	c=(int)(p.x-rect.left)*255/(rect.right-rect.left);
					ch_rgb=ch_rgb&comp_mask[hit_idx-SLIDER_R]|c<<comp_idx[hit_idx-SLIDER_R];
					setChoice(false);
				}
				break;
			}
		}
		invalidate();
		return true;
	}
}
