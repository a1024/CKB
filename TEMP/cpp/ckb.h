//
// Created by MSI on 2022-07-03.
//

#ifndef CUSTOMKB_CKB_H
#define CUSTOMKB_CKB_H
#ifdef _MSC_VER
#define _CRT_SECURE_NO_WARNINGS
#include<stdio.h>
#define snprintf sprintf_s
#define vsnprintf vsprintf_s
#define LOGI printf
#define LOGE printf
#else
#include<jni.h>
#include<android/log.h>
extern const char log_tag[];
#define		LOGI(...)  __android_log_print(ANDROID_LOG_INFO, log_tag, ##__VA_ARGS__)
#define		LOGE(...)  __android_log_print(ANDROID_LOG_ERROR, log_tag, ##__VA_ARGS__)
#endif
#include"array.h"


typedef enum KeyTypeEnum
{
#define		CKBKEY(FLAG, DESCRIPTION)		FLAG,
#include	"ckb_keys.h"
#undef		CKBKEY
	KEY_COUNT,
} KeyType;
typedef enum ModKeyTypeEnum
{
#define		CKBKEY(FLAG, DESCRIPTION, KEYWORD)		FLAG,
#include	"ckb_mkeys.h"
#undef		CKBKEY
	MKEY_COUNT,
} ModKeyType;
extern const char *buttonlabels[], *modbuttonlabels[], *mcodes[];
#define 	MODMASK		0x80000000//keys which are not in unicode have MSB set

typedef enum LayoutTypeEnum
{
	//these layouts must appear only once:
	LAYOUT_ASCII,
	LAYOUT_NUMPAD,
	LAYOUT_DECNUMPAD,

	//these layouts must have language 'lang':
	LAYOUT_LANG,
	LAYOUT_URL,
} LayoutType;
typedef struct ButtonInfoStruct
{
	int x1, x2, code;
	float relativeWidth;
} Button;
typedef struct RowStruct
{
	int y1, y2;
	ArrayHandle buttons;
} Row;
typedef struct LayoutStruct
{
	LayoutType type;
	ArrayHandle lang;//string, optional for type lang & url
	ArrayHandle portrait, landscape;//array of rows
	float p_height, l_height;//percentage of screen height (width and height get swapped on landscape)
} Layout;
typedef struct ContextStruct
{
	ArrayHandle layouts;//'Layout' array
	int defaultlangidx;
} Context;
typedef struct GlobalsStruct
{
	int w, h;

	Context ctx;
	int layoutidx;

	ArrayHandle error;
} Globals;

extern Globals *glob;
extern int	nerrors;

void		free_row(void *data);
void		free_layout(void *data);
void		free_context(Context *ctx);

int 		parse_state(ArrayConstHandle text, Context *ctx);
int 		calc_raster_sizes(Context *ctx, int width, int height, int is_landscape);


#endif //CUSTOMKB_CKB_H
