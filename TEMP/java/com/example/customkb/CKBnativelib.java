package com.example.customkb;

public class CKBnativelib
{
	static
	{
		System.loadLibrary("ckb");
	}
	public static native int init(int width, int height);
}
