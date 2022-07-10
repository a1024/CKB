package com.example.customkb;

public class CKBnativelib
{
	static
	{
		System.loadLibrary("ckb");
	}
	//ckb.c
	public static native int init(int mode, int decnumpad, int width, int height);//returns nRows in current layout
	public static native void finish();
	public static native int getKbHeight();
	public static native int[] getRow(int rowIdx);//y1, y2, x[0], code[0], x[1], code[1], ...x[n-1], code[n-1], x[n]

	//switches between lang/url and ASCII if mode allows
	//returns nRows in selected layout
	//returns 0 if layout/lang switch is not allowed (numpad)
	//returns -1 in case of error
	public static native int nextLayout();
	public static native int nextLanguage();//returns nRows in selected layout

	public static native int getNErrors();
	public static native String getError(int errorIdx);


	//settings activity
	public static native String loadConfig();
	public static native boolean saveConfig(String text);
	public static native boolean resetConfig();
}
