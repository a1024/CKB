CKB: Customizable Keyboard

An open-source customizable keyboard.
This project is a work-in-progress.

How to compile:
1) Create a new Android Studio project with no activity
2) Add the following java files:
	app/src/main/java/com/ckbdemo/CKBactivity.java
	app/src/main/java/com/ckbdemo/CKBservice.java
	app/src/main/java/com/ckbdemo/CKBSettings.java
	app/src/main/java/com/ckbdemo/CKBview3.java
	app/src/main/java/com/ckbdemo/ColorPicker.java
	app/src/main/java/com/ckbdemo/TouchInfo.java
3) Add the following xml files:
	app/src/main/res/drawable/popup_bk.xml
	app/src/main/res/layout/parent_layout.xml
	app/src/main/res/layout/popup_view.xml
	app/src/main/res/xml/method.xml
4) Replace AndroidManifest with:
	app/src/main/AndroidManifest.xml
5) Add the following C/C++ sources:
	app/src/main/cpp/array.c
	app/src/main/cpp/parser.c
	app/src/main/cpp/default_config.cpp
	app/src/main/cpp/ckb.c
