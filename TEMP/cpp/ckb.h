//
// Created by MSI on 7/3/2022.
//

#ifndef CUSTOMKB_CKB_H
#define CUSTOMKB_CKB_H
#include<jni.h>
#include<android/log.h>
#include"array.h"
extern const char log_tag[];
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO, log_tag, ##__VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR, log_tag, ##__VA_ARGS__)

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
#define 	MODMASK		0x80000000

typedef enum LayoutTypeEnum
{
	//these layouts must appear only once:
	LAYOUT_ASCII,
	LAYOUT_NUMPAD,
	LAYOUT_DECNUMPAD,

	//these layouts must have language name:
	LAYOUT_LANG,
	LAYOUT_URL,
} LayoutType;
typedef struct ButtonInfoStruct
{
	int code;
	float width;
} ButtonInfo;
typedef struct LayoutStruct
{
	LayoutType type;
	ArrayHandle name;//string, optional for type lang & url
	ArrayHandle portrait, landscape;//array of rows, each row is an array of buttons
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

extern int	nerrors;

void		free_row(void *data);
void		free_layout(void *data);
void		free_context(Context *ctx);

int 		parse_state(ArrayConstHandle text, Context *ctx);


#endif //CUSTOMKB_CKB_H
