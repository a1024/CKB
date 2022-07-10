//
// Created by MSI on 2022-07-02.
//

//this information is only for creation of state file for first time or at reset all layouts
//layout order is identical to enum LayoutIdx
//capitalization (shift/capslock) is done procedurally on all unicode characters

//at startup attempt to load state file
//if state file has errors, back it up and create a fresh state file
//if it didn't exist then create a fresh state file

//when user writes to state file (creates new layouts, ...etc) write to temp file first
//when user saves changes, if temp file passes all checks overwrite state file
extern "C" const char default_config[]=
R"config(/*
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
	'q', 113, 0x71 all mean the same character
	'\U0001F602', 128514, 0x1F602 all mean the famous 'tears of joy' emoji

list special keys:
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
*/

layout lang en
portrait 0.36{//0.384
27 1, '\t' 1, caps 1, ctrl 1, alt 1, home 1, end 1, pgup 1, pgdn 1, 127 1;
'1' 1, '2' 1, '3' 1, '4' 1, '5' 1, '6' 1, '7' 1, '8' 1, '9' 1, '0' 1;
'q' 1, 'w' 1, 'e' 1, 'r' 1, 't' 1, 'y' 1, 'u' 1, 'i' 1, 'o' 1, 'p' 1;
nab 0.5, 'a' 1, 's' 1, 'd' 1, 'f' 1, 'g' 1, 'h' 1, 'j' 1, 'k' 1, 'l' 1, nab 0.5;
shift 1.5, 'z' 1, 'x' 1, 'c' 1, 'v' 1, 'b' 1, 'n' 1, 'm' 1, '\b' 1.5;
layout 1.25, settings 1.25, ' ' 5, '.' 1, '\n' 1.5;
}
landscape 0.32{
settings 1, 'q' 1, 'w' 1, 'e' 1, 'r' 1, 't' 1, nab 1, 27 1, home 1, end 1, '7' 1, '8' 1, '9' 1, '0' 1, 'y' 1, 'u' 1, 'i' 1, 'o' 1, 'p' 1;
shift 1, nab 1/3, 'a' 1, 's' 1, 'd' 1, 'f' 1, 'g' 1, nab 2/3, '\t' 1, ctrl 1, alt 1, '4' 1, '5' 1, '6' 1, ',' 1, nab 1/3, 'h' 1, 'j' 1, 'k' 1, 'l' 1, nab 2/3;
layout 1, nab 0.75, 'z' 1, 'x' 1, 'c' 1, 'v' 1, 'b' 1, nab 0.25, ' ' 3, '1' 1, '2' 1, '3' 1, '.' 1, nab 0.75, 'n' 1, 'm' 1, nab 0.25, '\b' 1, '\n' 1;
}

layout lang ar
portrait 0.36{//0.384
27 1, '\t' 1, caps 1, ctrl 1, alt 1, home 1, end 1, pgup 1, pgdn 1, 127 1;
0x0661 1, 0x0662 1, 0x0663 1, 0x0664 1, 0x0665 1, 0x0666 1, 0x0667 1, 0x0668 1, 0x0669 1, 0x0660 1;
0x0636 1, 0x0635 1, 0x062B 1, 0x0642 1, 0x0641 1, 0x063A 1, 0x0639 1, 0x0647 1, 0x062E 1, 0x062D 1, 0x062C 1;
0x0634 1, 0x0633 1, 0x064A 1, 0x0628 1, 0x0644 1, 0x0627 1, 0x062A 1, 0x0646 1, 0x0645 1, 0x0643 1, 0x0637 1;
0x0630 1, 0x0621 1, 0x0624 1, 0x0631 1, 0x0649 1, 0x0629 1, 0x0648 1, 0x0632 1, 0x0638 1, 0x062F 1, '\b' 1;
layout 1.25, settings 1.25, ' ' 4, 0x060C 1, '.' 1, '\n' 1.5;
}
landscape 0.32{
nab 2/3,			0x0636 1, 0x0635 1, 0x062B 1, 0x0642 1, 0x0641 1, 0x063A 1, nab 1/3,	27 1, home 1, end 1,	0x0667 1, 0x0668 1, 0x0669 1, 0x0660 1, nab 1,		0x0639 1, 0x0647 1, 0x062E 1, 0x062D 1, 0x062C 1;
settings 1,			0x0634 1, 0x0633 1, 0x064A 1, 0x0628 1, 0x0644 1, 0x0627 1,				'\t' 1, ctrl 1, alt 1,	0x0664 1, 0x0665 1, 0x0666 1, 0x060C 1, nab 1/3,	0x062A 1, 0x0646 1, 0x0645 1, 0x0643 1, 0x0637 1, nab 2/3;
layout 1, nab 0.25, 0x0630 1, 0x0621 1, 0x0624 1, 0x0631 1, 0x0649 1, nab 0.75,				' ' 3,					0x0661 1, 0x0662 1, 0x0663 1, '.' 1,				0x0629 1, 0x0648 1, 0x0632 1, 0x0638 1, '\b' 1, '\n' 1;
}

layout url en
portrait 0.36{//0.384
27 1, '\t' 1, caps 1, ctrl 1, alt 1, home 1, end 1, pgup 1, pgdn 1, 127 1;
'1' 1, '2' 1, '3' 1, '4' 1, '5' 1, '6' 1, '7' 1, '8' 1, '9' 1, '0' 1;
'q' 1, 'w' 1, 'e' 1, 'r' 1, 't' 1, 'y' 1, 'u' 1, 'i' 1, 'o' 1, 'p' 1;
nab 0.5, 'a' 1, 's' 1, 'd' 1, 'f' 1, 'g' 1, 'h' 1, 'j' 1, 'k' 1, 'l' 1, nab 0.5;
shift 1.5, 'z' 1, 'x' 1, 'c' 1, 'v' 1, 'b' 1, 'n' 1, 'm' 1, '\b' 1.5;
layout 1, settings 1, '/' 1, ' ' 4, '@' 1, '.' 1, '\n' 1;
}
landscape 0.32{
settings 1, 'q' 1, 'w' 1, 'e' 1, 'r' 1, 't' 1, nab 1, '/' 1, home 1, end 1, '7' 1, '8' 1, '9' 1, '0' 1, 'y' 1, 'u' 1, 'i' 1, 'o' 1, 'p' 1;
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
layout 1.25, settings 1.25, ',' 1, ' ' 4, '.' 1, '\n' 1.5;
}
landscape 0.48{
'~' 1, ';' 1, ':' 1, '\'' 1, '\"' 1, '\\' 1, '|' 1, '<' 1, '>' 1, '/' 1, '?' 1, '&' 1, '*' 1, '(' 1, ')' 1;//15
'`' 1, '-' 1, '_' 1, '=' 1, '+' 1, '[' 1, ']' 1, '{' 1, '}' 1, '!' 1, '@' 1, '#' 1, '$' 1, '%' 1, '^' 1;//15
'\t' 1,			'q' 1, 'w' 1, 'e' 1, 'r' 1, 't' 1, settings 1, '.' 1, ',' 1,	'7' 1, '8' 1, '9' 1, '0' 1,				'y' 1, 'u' 1, 'i' 1, 'o' 1, 'p' 1;
shift 1, nab 1/3,	'a' 1, 's' 1, 'd' 1, 'f' 1, 'g' 1, nab 2/3, home 1, end 1,	'4' 1, '5' 1, '6' 1, nab 1, nab 1/3,	'h' 1, 'j' 1, 'k' 1, 'l' 1, nab 2/3;
layout 1, nab 0.75,	'z' 1, 'x' 1, 'c' 1, 'v' 1, 'b' 1, nab 0.25, ' ' 2,			'1' 1, '2' 1, '3' 1, nab 1, nab 0.75,	'n' 1, 'm' 1, nab 0.25, '\b' 1, '\n' 1;
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
	button_idle		0xC0707070
	button_fast		0xC0237CE9
	button_pressed	0xC0FF8040
	labels		0xFFFFFFFF
	shadow_modifier		0xC000FF00
	shadow_letter		0xC0FF0000
	shadow_non_letter	0xC00000FF
	preview_popup	0xC0000000
}

)config";