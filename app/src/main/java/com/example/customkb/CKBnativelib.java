package com.example.customkb;

public class CKBnativelib
{
	static
	{
		System.loadLibrary("ckb");
	}
	//TODO unicode config to describe layouts with native characters

	//ckb.c
	public static native int[] init(int mode, boolean decNumPad, int layoutIdxHint, int width, int height);//returns {selected layoutIdx, nLayouts}, or nullptr
	public static native void finish();

	public static native int[] getTheme();//check for null

	//getLayout/getLayoutName():
	//idx:
	//	-3	Symbols (extension)
	//	-2	DecNumPad
	//	-1	NumPad
	//	>=0	lang...
	//	otherwise null
	//
	//getLayout() returns layout array of size (3 + nRows*2 + nButtonsTotal*2) integers:
	//
	//	nRows, pxLayoutHeight,
	//		rowStartIdx[0], ...rowStartIdx[nRows-1], totalArrayCount,		//to get row button count easily, even in bottom row
	//		x[0], code[0], x[1], ...code[n-1], x[n],
	//		...
	//		x[0], code[0], x[1], ...code[n-1], x[n],
	//
	//returns null: check getNErrors()
	public static native int[] getLayout(int layoutIdx, boolean hasSymbolsExtension);
	public static native String getLayoutName(int layoutIdx);

	public static native int getNErrors();
	public static native String getError(int errorIdx);
	public static native void clearErrors();


	//unicode search
	public static native int[] searchUnicode(String query);


	//settings activity
	public static native String loadConfig();
	public static native boolean saveConfig(String str);
	public static native boolean storeThemeColor(int color, int idx);
	public static native boolean resetConfig();
}
