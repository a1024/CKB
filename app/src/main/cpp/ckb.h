//
// Created by MSI on 2022-07-03.
//

#ifndef CUSTOMKB_CKB_H
#define CUSTOMKB_CKB_H
#include"array.h"


typedef enum KeyTypeEnum
{
#define		CKBKEY(FLAG, DESCRIPTION)		FLAG,
#include	"ckb_keys.h"
#undef		CKBKEY
	//KEY_COUNT,
} KeyType;
typedef enum ModKeyTypeEnum
{
#define		CKBKEY(FLAG, DESCRIPTION, KEYWORD)		FLAG,
#include	"ckb_mkeys.h"
#undef		CKBKEY
	MKEY_COUNT,
} ModKeyType;
extern const char
//	*buttonlabels[],
//	*modbuttonlabels[],
	*mcodes[];
#define 	MODMASK		0x80000000//keys which are not in unicode have MSB set

typedef enum ModeTypeEnum//consistent with labels in CKBview3.java
{
	//text modes			allow layout & language switch
	MODE_TEXT,
	MODE_PASSWORD,			//default is ASCII
	MODE_URL, MODE_EMAIL,	//both are the same

	//numeric modes			cannot switch layout nor language
	MODE_NUMBER,
	MODE_PHONE_NUMBER,
	MODE_NUMERIC_PASSWORD,
} ModeType;
typedef enum LayoutTypeEnum
{
	LAYOUT_UNINITIALIZED,//illegal value

	//these layout(s) must have language name 'lang':
	LAYOUT_LANG,

	//these layout(s) must appear only once:
	LAYOUT_NUMPAD,				//layoutIdx == -1
	LAYOUT_DECNUMPAD,			//layoutIdx == -2
	LAYOUT_SYMBOLS_EXTENSION,	//layoutIdx == -3
} LayoutType;
typedef struct ButtonInfoStruct
{
	int x1, x2, code;
	float relativeWidth;
} Button;
typedef struct RowStruct
{
	int y1, y2;
	ArrayHandle buttons;//'Button' array
} Row;
typedef struct LayoutStruct
{
	LayoutType type;
	ArrayHandle lang;//string, for layout types 'lang' & 'url'
	ArrayHandle portrait, landscape;//'Row' arrays
	float p_percent, l_percent;//percentage of screen height (width and height get swapped on landscape)
	int p_height, l_height;//keyboard height in pixels
} Layout;
typedef enum ColorIdxEnum
{
	COLOR_BACKGROUND,
	COLOR_BUTTON_IDLE,
	COLOR_BUTTON_FAST,
	COLOR_BUTTON_PRESSED,
	COLOR_LABELS,
	COLOR_SHADOW_MODIFIER,
	COLOR_SHADOW_LETTER,
	COLOR_SHADOW_NON_LETTER,
	COLOR_PREVIEW_POPUP,
	THEME_COLOR_COUNT,
} ColorIdx;
typedef struct ContextStruct
{
	ArrayHandle layouts;//'Layout' array
	Layout
		symbolsExtension,
		numPad,		//layoutIdx == -1
		decNumPad;	//layoutIdx == -2
	ArrayHandle defaultLang;//string
	int theme[9];
} Context;
typedef struct GlobalsStruct
{
	int w, h;

	Context ctx;
	//ModeType mode;
	//int layoutIdx;
	//int prevLayoutIdx;
} Globals;

extern Globals *glob;
extern ArrayHandle errors;
extern int	nErrors;

void		free_row(void *data);
void		free_layout(void *data);
void		free_context(Context *ctx);

//parse_state:
//call like this when parsing the state:
//	success=parse_state(/*(const char*)*/ text, text_len, &ctx, 0, 0, 0);
//call like this when setting config colors:
//	success=parse_state(0, 0, 0, /*(ArrayHandle*)*/ &text, color, idx);
int 		parse_state(const char *cText, size_t text_len, Context *ctx0, ArrayHandle *aText, int color_value, int color_idx);
int 		calc_raster_sizes(Context *ctx, int width, int height, int is_landscape);


//unicode
typedef struct CodeRankStruct
{
	int code, rank;
} CodeRank;
ArrayHandle unicode_search(const char *query);//returns CodeRank array


#endif //CUSTOMKB_CKB_H
