//
// Created by MSI on 2022-07-02.
//

//EDIT THIS FILE OUTSIDE ANDROID STUDIO
//because Android studio handles Unicode and RTL languages in a confusing way

//this information is only for creation of state file for first time or at reset all layouts
//layout order is identical to enum LayoutIdx
//capitalization (shift/capslock) is done procedurally on all unicode characters

//at startup attempt to load state file
//if state file has errors, back it up and create a fresh state file
//if it didn't exist then create a fresh state file

//when user writes to state file (creates new layouts, ...etc) write to temp file first
//when user saves changes, if temp file passes all checks overwrite state file
extern "C" const char default_config[]=R"config(
/*
Welcome to CustomKB!
This file defines the keyboard layouts.

Syntax:
layout [lang] layout_name
portrait <relative_height>{
	<button_code> <button_width>, ...;
	//buttons are separated by commas
	//each row ends with a semicolon
}
landscape <relative_height>{
	//each layout should have both orientations
}

Keyboard theme is set with the 'theme' keyword.
Color keywords must be followed only by C-style 8-digit hexadecimals:

	0xAARRGGBB (which means: alpha, red, green, blue)

These exact keywords set keyboard colors:
theme{
	background		//keyboard 'clear color'
	button_idle			//non-pressed button color
	button_fast			//'quick mode' color
	button_pressed		//pressed button color
	labels			//text color
	shadow_modifier		//shadow color for modifier keys
	shadow_letter		//shadow color for letter buttons
	shadow_non_letter	//shadow color for non-letter buttons
	preview_popup	//pressed button popup text color
}

default layouts that must be present:
	lang <language_name> (eg: en)
	url <language_name> (eg: en)
	ascii
	numpad
	decnumpad

C-style line and block comments are supported

describe button_code with its unicode codepoint or the C-style character literal
examples:
	'q', 113, and 0x71 all mean the same character
	'\U0001F602', 128514, and 0x1F602 all mean the famous 'tears of joy' emoji

list of special keys:
	nab (stands for 'not a button', an empty space)
	layout (changes layout, long press to switch language)
	settings
	home
	end
	pgup (page up)
	pgdn (page down)
	shift
	ctrl
	alt
	left
	right
	up
	down
	'\n' (enter)
	'\t' (tab)
	'\b' (backspace)
	127 (delete)
	ins (insert)
	27 (escape)
	caps (caps lock)
	numlk (num lock)
	scrlk (scroll lock)
	prscr (print screen)
	pause
	f1 ... f12
	cut
	copy
	paste
	selectall
	menu
*/

layout lang en
portrait 0.36{//0.384
unicode 1, copy 1, paste 1, caps 1, ctrl 1, home 1, end 1, pgup 1, pgdn 1, 127 1;
'1' 1, '2' 1, '3' 1, '4' 1, '5' 1, '6' 1, '7' 1, '8' 1, '9' 1, '0' 1;
'q' 1, 'w' 1, 'e' 1, 'r' 1, 't' 1, 'y' 1, 'u' 1, 'i' 1, 'o' 1, 'p' 1;
nab 0.5, 'a' 1, 's' 1, 'd' 1, 'f' 1, 'g' 1, 'h' 1, 'j' 1, 'k' 1, 'l' 1, nab 0.5;
shift 1.5, 'z' 1, 'x' 1, 'c' 1, 'v' 1, 'b' 1, 'n' 1, 'm' 1, '\b' 1.5;
layout 1.25, settings 1.25, '\t' 1, ' ' 3, ',' 1, '.' 1, '\n' 1.5;
}
landscape 0.32{
unicode 1, 'q' 1, 'w' 1, 'e' 1, 'r' 1, 't' 1, settings 1, 27 1, home 1, end 1, '7' 1, '8' 1, '9' 1, '0' 1, 'y' 1, 'u' 1, 'i' 1, 'o' 1, 'p' 1;
shift 1, nab 1/3, 'a' 1, 's' 1, 'd' 1, 'f' 1, 'g' 1, nab 2/3, '\t' 1, ctrl 1, alt 1, '4' 1, '5' 1, '6' 1, ',' 1, nab 1/3, 'h' 1, 'j' 1, 'k' 1, 'l' 1, nab 2/3;
layout 1, nab 0.75, 'z' 1, 'x' 1, 'c' 1, 'v' 1, 'b' 1, nab 0.25, ' ' 3, '1' 1, '2' 1, '3' 1, '.' 1, nab 0.75, 'n' 1, 'm' 1, nab 0.25, '\b' 1, '\n' 1;
}

layout lang ar
portrait 0.36{//0.384
unicode 1, copy 1, paste 1, caps 1, ctrl 1, home 1, end 1, pgup 1, pgdn 1, 127 1;
'١' 1, '٢' 1, '٣' 1, '٤' 1, '٥' 1, '٦' 1, '٧' 1, '٨' 1, '٩' 1, '٠' 1;
'ض' 1, 'ص' 1, 'ث' 1, 'ق' 1, 'ف' 1, 'غ' 1, 'ع' 1, 'ه' 1, 'خ' 1, 'ح' 1, 'ج' 1;
'ش' 1, 'س' 1, 'ي' 1, 'ب' 1, 'ل' 1, 'ا' 1, 'ت' 1, 'ن' 1, 'م' 1, 'ك' 1, 'ط' 1;
'ذ' 1, 'ء' 1, 'ؤ' 1, 'ر' 1, 'ى' 1, 'ة' 1, 'و' 1, 'ز' 1, 'ظ' 1, 'د' 1, '\b' 1;
layout 1, settings 1, '؟' 1, '\t' 1, ' ' 3, '،' 1, '.' 1, '\n' 1;
}
landscape 0.32{
unicode 1,			'ض' 1, 'ص' 1, 'ث' 1, 'ق' 1, 'ف' 1, 'غ' 1,		settings 1, home 1, end 1,	'٧' 1, '٨' 1, '٩' 1, '،' 1, '؟' 1,		'ع' 1, 'ه' 1, 'خ' 1, 'ح' 1, 'ج' 1;
nab 1,				'ش' 1, 'س' 1, 'ي' 1, 'ب' 1, 'ل' 1, 'ا' 1,		'\t' 1, ctrl 1, alt 1,	'٤' 1, '٥' 1, '٦' 1, '.' 1, nab 1/3,	'ت' 1, 'ن' 1, 'م' 1, 'ك' 1, 'ط' 1, nab 2/3;
layout 1, nab 0.25, 'ذ' 1, 'ء' 1, 'ؤ' 1, 'ر' 1, 'ى' 1, nab 0.75,	' ' 2,			'٠' 1,	'١' 1, '٢' 1, '٣' 1, 'ة' 1,				'و' 1, 'ز' 1, 'ظ' 1, 'د' 1, '\b' 1, '\n' 1;
}

layout lang ru
portrait 0.36{//0.384
unicode 1, copy 1, paste 1, caps 1, ctrl 1, home 1, end 1, pgup 1, pgdn 1, 127 1;
'1' 1, '2' 1, '3' 1, '4' 1, '5' 1, '6' 1, '7' 1, '8' 1, '9' 1, '0' 1;
'й' 1, 'ц' 1, 'у' 1, 'к' 1, 'е' 1, 'н' 1, 'г' 1, 'ш' 1, 'щ' 1, 'з' 1, 'х' 1;
'ф' 1, 'ы' 1, 'в' 1, 'а' 1, 'п' 1, 'р' 1, 'о' 1, 'л' 1, 'д' 1, 'ж' 1, 'э' 1;
shift 1, 'я' 1, 'ч' 1, 'с' 1, 'м' 1, 'и' 1, 'т' 1, 'ь' 1, 'б' 1, 'ю' 1, '\b' 1;
layout 1, settings 1, 'ё' 1, '\t' 1, ' ' 3, 'ъ' 1, '.' 1, '\n' 1;
}
landscape 0.32{
unicode 1,			'й' 1, 'ц' 1, 'у' 1, 'к' 1, 'е' 1, 'ё' 1, 'н' 1,	settings 1, home 1, end 1,	'7' 1, '8' 1, '9' 1, '0' 1,				'г' 1, 'ш' 1, 'щ' 1, 'з' 1, 'х' 1, 'ъ' 1;
shift 1, nab 1/3,	'ф' 1, 'ы' 1, 'в' 1, 'а' 1, 'п' 1, 'р' 1, nab 2/3,	'\t' 1, ctrl 1, alt 1,	'4' 1, '5' 1, '6' 1, ',' 1, nab 1/3,	'о' 1, 'л' 1, 'д' 1, 'ж' 1, 'э' 1, nab 2/3;
layout 1, nab 0.75,	'я' 1, 'ч' 1, 'с' 1, 'м' 1, 'и' 1, nab 1, nab 0.25,	' ' 3,					'1' 1, '2' 1, '3' 1, '.' 1,				'т' 1, 'ь' 1, 'б' 1, 'ю' 1, '\b' 1, '\n' 1;
}

layout url en
portrait 0.36{//0.384
unicode 1, copy 1, paste 1, caps 1, ctrl 1, home 1, end 1, pgup 1, pgdn 1, 127 1;
'1' 1, '2' 1, '3' 1, '4' 1, '5' 1, '6' 1, '7' 1, '8' 1, '9' 1, '0' 1;
'q' 1, 'w' 1, 'e' 1, 'r' 1, 't' 1, 'y' 1, 'u' 1, 'i' 1, 'o' 1, 'p' 1;
nab 0.5, 'a' 1, 's' 1, 'd' 1, 'f' 1, 'g' 1, 'h' 1, 'j' 1, 'k' 1, 'l' 1, nab 0.5;
shift 1.5, 'z' 1, 'x' 1, 'c' 1, 'v' 1, 'b' 1, 'n' 1, 'm' 1, '\b' 1.5;
layout 1, settings 1, '/' 1, ' ' 4, '@' 1, '.' 1, '\n' 1;
}
landscape 0.32{
unicode 1, 'q' 1, 'w' 1, 'e' 1, 'r' 1, 't' 1, settings 1, '/' 1, home 1, end 1, '7' 1, '8' 1, '9' 1, '0' 1, 'y' 1, 'u' 1, 'i' 1, 'o' 1, 'p' 1;
shift 1, nab 1/3, 'a' 1, 's' 1, 'd' 1, 'f' 1, 'g' 1, nab 2/3, '@' 1, ctrl 1, alt 1, '4' 1, '5' 1, '6' 1, ',' 1, nab 1/3, 'h' 1, 'j' 1, 'k' 1, 'l' 1, nab 2/3;
layout 1, nab 0.75, 'z' 1, 'x' 1, 'c' 1, 'v' 1, 'b' 1, nab 0.25, ' ' 3, '1' 1, '2' 1, '3' 1, '.' 1, nab 0.75, 'n' 1, 'm' 1, nab 0.25, '\b' 1, '\n' 1;
}

layout ascii
portrait 0.45{
'!' 1, '@' 1, '#' 1, '$' 1, '%' 1, '^' 1, '&' 1, '*' 1, '(' 1, ')' 1;
'`' 1, '~' 1, '-' 1, '_' 1, '=' 1, '+' 1, '[' 1, ']' 1, '{' 1, '}' 1;
';' 1, ':' 1, '\'' 1, '\"' 1, '\\' 1, '|' 1, '<' 1, '>' 1, '/' 1, '?' 1;
'1' 1, '2' 1, '3' 1, '4' 1, '5' 1, '6' 1, '7' 1, '8' 1, '9' 1, '0' 1;
'q' 1, 'w' 1, 'e' 1, 'r' 1, 't' 1, 'y' 1, 'u' 1, 'i' 1, 'o' 1, 'p' 1;
nab 0.5, 'a' 1, 's' 1, 'd' 1, 'f' 1, 'g' 1, 'h' 1, 'j' 1, 'k' 1, 'l' 1, nab 0.5;
shift 1.5, 'z' 1, 'x' 1, 'c' 1, 'v' 1, 'b' 1, 'n' 1, 'm' 1, '\b' 1.5;
layout 1, settings 1, unicode 1, '\t' 1, ' ' 3, ',' 1, '.' 1, '\n' 1;
}
landscape 0.48{
'~' 1, ';' 1, ':' 1, '\'' 1, '\"' 1, '\\' 1, '|' 1, '<' 1, '>' 1, '/' 1, '?' 1, '&' 1, '*' 1, '(' 1, ')' 1;//15
'`' 1, '-' 1, '_' 1, '=' 1, '+' 1, '[' 1, ']' 1, '{' 1, '}' 1, '!' 1, '@' 1, '#' 1, '$' 1, '%' 1, '^' 1;//15
unicode 1,			'q' 1, 'w' 1, 'e' 1, 'r' 1, 't' 1, settings 1, 27 1, home 1, end 1,	'7' 1, '8' 1, '9' 1, '0' 1, 'y' 1,		'u' 1, 'i' 1, 'o' 1, 'p' 1;
shift 1, nab 1/3,	'a' 1, 's' 1, 'd' 1, 'f' 1, 'g' 1, nab 2/3, '\t' 1, ctrl 1, alt 1,	'4' 1, '5' 1, '6' 1, ',' 1, nab 1/3,	'h' 1, 'j' 1, 'k' 1, 'l' 1, nab 2/3;
layout 1, nab 0.75, 'z' 1, 'x' 1, 'c' 1, 'v' 1, 'b' 1, nab 0.25, ' ' 3,					'1' 1, '2' 1, '3' 1, '.' 1, nab 0.75,	'n' 1, 'm' 1, nab 0.25, '\b' 1, '\n' 1;
}

layout numpad
portrait 0.256{
'1' 1, '2' 1, '3' 1, home 1;
'4' 1, '5' 1, '6' 1, end 1;
'7' 1, '8' 1, '9' 1, '\b' 1;
'*' 1, '0' 1, '#' 1, '\n' 1;
}
landscape 0.21{
'1' 1, '2' 1, '3' 1, '4' 1, '5' 1, '6' 1, '7' 1, '8' 1, '9' 1, '0' 1;
'*' 1, '#' 1, '+' 1, '-' 1, '\b' 2, '\n' 2;
}

layout decnumpad
portrait 0.256{
'1' 1, '2' 1, '3' 1, '+' 1, home 1;
'4' 1, '5' 1, '6' 1, '-' 1, end 1;
'7' 1, '8' 1, '9' 1, '/' 1, '\b' 1;
'*' 1, '0' 1, '#' 1, '.' 1, '\n' 1;
}
landscape 0.21{
'1' 1, '2' 1, '3' 1, '4' 1, '5' 1, '6' 1, '7' 1, '8' 1, '9' 1, '0' 1;
'*' 1, '#' 1, '+' 1, '-' 1, '/' 1, '.' 1, '\b' 1, '\n' 1;
}

lang en		//default language

theme{
	background	0x00202020
	button_idle		0xDC707070
	button_fast		0xDC237CE9
	button_pressed	0xDCFF8040
	labels		0xFFFFFFFF
	shadow_modifier		0xC000FF00
	shadow_letter		0xC08600FF
	shadow_non_letter	0xC00000FF
	preview_popup	0xC0000000
}

)config";