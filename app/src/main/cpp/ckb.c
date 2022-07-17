/*	ckb.c - The implementation of the customizable keyboard JNI
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

//
// Created by MSI on 2021-12-03.
//

#include"ckb.h"
#include<stdio.h>
#include<stdlib.h>
#include<stdarg.h>
#include<string.h>
#include<sys/stat.h>
#include<errno.h>
static const char file[]=__FILE__;


//FIXME
//	no debug macros should be defined
//	STORE_ERRORS should be defined
//	ALWAYS_RESET shouldn't be defined

//	#define DEBUG_ARRAY
//	#define DEBUG_HEAP

	#define STORE_ERRORS//disable this macro when using L O G _ E R R O R for debug

	#define USE_CCS_UTF8//non-standard & apparently does nothing
//	#define ALWAYS_RESET//don't use this macro, just press reset in settings


const char stateFilename[]="/data/data/com.example.customkb/state.txt";//TODO UPDATE AT RELEASE
const char log_tag[]="customkb";

#define 	G_BUF_SIZE	1024
char 		g_buf[G_BUF_SIZE]={0};

extern const char default_config[];

#if 0
const char *buttonlabels[]=
{
#define		CKBKEY(FLAG, DESCRIPTION)		DESCRIPTION,
#include	"ckb_keys.h"
#undef		CKBKEY
};
const char *modbuttonlabels[]=
{
#define		CKBKEY(FLAG, DESCRIPTION, KEYWORD)		DESCRIPTION,
#include	"ckb_mkeys.h"
#undef		CKBKEY
};
#endif
const char *mcodes[]=
{
#define		CKBKEY(FLAG, DESCRIPTION, KEYWORD)	KEYWORD,
#include	"ckb_mkeys.h"
#undef		CKBKEY
};

Globals		*glob=0;
ArrayHandle	errors=0;
int			nErrors=0;

void		free_row(void *data)
{
	ArrayHandle *row=(ArrayHandle*)data;
#ifdef DEBUG_HEAP
	LOG_ERROR("Freeing row=%p...", row);
#endif
	array_free(row, 0);
}
void		free_layout(void *data)
{
	Layout *layout=(Layout*)data;
#ifdef DEBUG_HEAP
	LOG_ERROR("Freeing layout=%p...", layout);
	LOG_ERROR("Freeing layout->lang=%p...", layout->lang);
	LOG_ERROR("Freeing layout->portrait=%p...", layout->portrait);
	LOG_ERROR("Freeing layout->landscape=%p...", layout->landscape);
#endif
	array_free(&layout->lang, 0);
	array_free(&layout->portrait, free_row);
	array_free(&layout->landscape, free_row);
}
void		free_context(Context *ctx)
{
#ifdef DEBUG_HEAP
	LOG_ERROR("Freeing ctx->layouts=%p...", ctx->layouts);
#endif
	array_free(&ctx->layouts, free_layout);
	free_layout(&ctx->symbolsExtension);
	free_layout(&ctx->decNumPad);
	free_layout(&ctx->numPad);
	array_free(&ctx->defaultLang, 0);
}
void 		free_error(void *data)
{
	ArrayHandle *str=(ArrayHandle*)data;
#ifdef DEBUG_HEAP
	LOG_ERROR("Freeing error string=%p...", *str);
#endif
	array_free(str, 0);
}

int 		glob_alloc()
{
	if(!glob)
	{
		glob=(Globals*)malloc(sizeof(Globals));
		if(!glob)
			return 0;//alloc error
		memset(glob, 0, sizeof(Globals));
		return 1;//new allocation
	}
	return 2;//already allocated
}

int			log_error(const char *f, int line, const char *format, ...)
{
	va_list args;

	int size=(int)strlen(f), start=size-1, printed=0;
	for(;start>=0&&f[start]!='/'&&f[start]!='\\';--start);
	start+=start==-1||f[start]=='/'||f[start]=='\\';

	printed+=snprintf(g_buf+printed, G_BUF_SIZE-printed, "[%d] %s(%d): ", nErrors, f+start, line);
	if(format)
	{
		va_start(args, format);
		printed+=vsnprintf(g_buf+printed, G_BUF_SIZE-printed, format, args);
		va_end(args);
	}
	else
		printed+=snprintf(g_buf+printed, G_BUF_SIZE-printed, "Error");

	LOGE("%s", g_buf);

#ifdef STORE_ERRORS
	if(!errors)
		ARRAY_ALLOC(ArrayHandle, errors, 0, 0);
	ArrayHandle *e=(ArrayHandle*)ARRAY_APPEND(errors, 0, 1, 1, 0);
	STR_ALLOC(*e, 0);
	array_assign(e, g_buf, printed+1);
#endif

	++nErrors;
	return nErrors-1;
}

ArrayHandle	load_text(const char *filename)
{
	struct stat info={0};
	FILE *f;
	ArrayHandle ret;
	size_t bytesRead;

	int error=stat(filename, &info);
	if(error)
	{
		//do not emit error here
		//the file doesn't exist just after installing the app
		//LOG_ERROR("Cannot read %s, %d: %s", filename, error, strerror(error));
		return 0;
	}
#ifdef USE_CCS_UTF8
	f=fopen(filename, "r,ccs=UTF-8");//'ccs=UTF-8' is non-standard
#else
	f=fopen(filename, "r");
#endif
	if(!f)
	{
		LOG_ERROR("Cannot read %s, %d: %s", filename, error, strerror(error));
		return 0;
	}
	STR_ALLOC(ret, info.st_size);
	bytesRead=fread(ret->data, 1, info.st_size, f);
	fclose(f);
	ret->count=bytesRead;
	//array_fit(&ret, 1);
	((char*)ret->data)[bytesRead]='\0';
	return ret;
}
int 		save_text(const char *filename, const char *text, size_t len)
{
	FILE *f;
	int error;

#ifdef USE_CCS_UTF8
	f=fopen(filename, "w,ccs=UTF-8");//'ccs=UTF-8' is non-standard
#else
	f=fopen(filename, "w");//'ccs=UTF-8' is non-standard
#endif
	if(!f)
	{
		error=errno;
		LOG_ERROR("Cannot write to %s, %d:%s", filename, error, strerror(error));
		return 0;
	}
	fwrite(text, 1, len, f);
	fclose(f);
	return 1;
}

int			find_layout_idx(LayoutType type, ArrayHandle lang)
{
	int nLayouts=(int)glob->ctx.layouts->count;
	Layout *layout;

	if(type==LAYOUT_LANG)
	{
		for(int kl=0;kl<nLayouts;++kl)
		{
			layout=(Layout*)array_at(&glob->ctx.layouts, kl);
			if(layout->type==type&&!strcmp((char*)layout->lang->data, (char*)lang->data))
				return kl;
		}
	}
	else
	{
		for(int kl=0;kl<nLayouts;++kl)
		{
			layout=(Layout*)array_at(&glob->ctx.layouts, kl);
			if(layout->type==type)
				return kl;
		}
	}
	return -1;
}
const char*	layoutType2str(Layout const *layout)//for debug
{
	switch(layout->type)
	{
	case LAYOUT_UNINITIALIZED:	return "UNINITIALIZED";//illegal value

	//these layouts must have language 'lang':
	case LAYOUT_LANG:			return "lang";

	//these layouts must appear only once:
	case LAYOUT_SYMBOLS_EXTENSION:	return "symbols";
	case LAYOUT_NUMPAD:				return "numPad";
	case LAYOUT_DECNUMPAD:			return "decNumPad";
	default:						break;
	}
	return "UNIDENTIFIED";
}
Layout*		get_layout(Context *ctx, int idx)
{
	if(idx==-3)
		return &ctx->symbolsExtension;
	if(idx==-2)
		return &ctx->decNumPad;
	if(idx==-1)
		return &ctx->numPad;
	if(idx<0||idx>=(int)ctx->layouts->count)
	{
		LOG_ERROR("Layout index out of bounds: idx=%d, count=%d", idx, ctx->layouts->count);
		return 0;
	}
	return (Layout*)array_at(&ctx->layouts, idx);
}
int 		getTotalButtonCount(ArrayHandle rows)
{
	int nButtonsTotal;
	Row *row;

	nButtonsTotal=0;
	for(int kr=0;kr<(int)rows->count;++kr)
	{
		row=(Row*)array_at(&rows, kr);
		nButtonsTotal+=(int)row->buttons->count;
	}
	return nButtonsTotal;
}
void		checkedAssign(ArrayHandle arr, int idx, int val)
{
	int *v0=(int*)array_at(&arr, idx);
	if(*v0)
		LOG_ERROR("Error: array[%] = %d overwritten with %d", idx, *v0, val);
	else
		*v0=val;
}
void		fillLayoutArray(ArrayHandle arr, ArrayHandle rows, int *rowIdx, int *start_buttons)
{
	Row *row;

	for(int kr=0;kr<(int)rows->count;++kr, ++*rowIdx)//for each row in layout
	{
		row=(Row*)array_at(&rows, kr);
#ifdef DEBUG_ARRAY
		LOG_ERROR("[%d]rowStartIdx=%d, [%d]x[0]=0", 2+*rowIdx, *start_buttons, *start_buttons);
#endif
		checkedAssign(arr, 2+*rowIdx, *start_buttons);
		checkedAssign(arr, *start_buttons, 0);
		//*(int*)array_at(&arr, 2+*rowIdx)=*start_buttons;
		//*(int*)array_at(&arr, *start_buttons)=0;//x[0] is always zero (for easy looping)
		for(int kb=0;kb<(int)row->buttons->count;++kb)//for each button in row
		{
			Button *button=(Button*)array_at(&row->buttons, kb);
#ifdef DEBUG_ARRAY
			LOG_ERROR("[%d, %d] [%d]code=0x%08X, [%d]x2=%d", *rowIdx, kb, *start_buttons+kb*2+1, button->code, *start_buttons+kb*2+2, button->x2);
#endif
			checkedAssign(arr, *start_buttons+kb*2+1, button->code);
			checkedAssign(arr, *start_buttons+kb*2+2, button->x2);
			//*(int*)array_at(&arr, *start_buttons+kb*2+1)=button->code;
			//*(int*)array_at(&arr, *start_buttons+kb*2+2)=button->x2;
		}
		*start_buttons+=(int)row->buttons->count*2+1;
	}
}

EXTERN_C JNIEXPORT jintArray JNICALL Java_com_example_customkb_CKBnativelib_init(JNIEnv *env, jclass clazz, jint mode, jboolean decNumPad, jint layoutIdxHint, jint width, jint height)
{
	int ret;
	ArrayHandle text;
	jintArray jResult;

	ret=glob_alloc();
	if(ret)
		glob->w=width, glob->h=height;
	if(ret==1)
	{
#ifdef ALWAYS_RESET
		text=0;
#else
		text=load_text(stateFilename);
#endif
		if(!text)
		{
			size_t len=strlen(default_config);
			ret=save_text(stateFilename, default_config, len);
			if(ret)
				text=load_text(stateFilename);
		}
		if(text)
		{
			ret=parse_state((char*)text->data, text->count, &glob->ctx, 0, 0, 0);
			ret&=calc_raster_sizes(&glob->ctx, glob->w, glob->h, glob->w>glob->h);
			array_free(&text, 0);
		}
		else
			ret=0;
	}
	else
		ret=1;

	if(!ret)
		return 0;

	int layoutIdx=layoutIdxHint;
	switch(mode)//should take hint only if it's not -10 and it has the same mode (text vs numeric)
	{
	//text modes
	case MODE_TEXT:
	case MODE_PASSWORD:
	case MODE_URL:
	case MODE_EMAIL:
		if(layoutIdx<0)//replace numPad layouts that have indices {-2, -1}
			layoutIdx=find_layout_idx(LAYOUT_LANG, glob->ctx.defaultLang);
		break;

	//numeric modes
	case MODE_NUMBER:
	case MODE_PHONE_NUMBER:
	case MODE_NUMERIC_PASSWORD:
		if(decNumPad)
			layoutIdx=-2;
		else
			layoutIdx=-1;
		break;
	default:
		LOG_ERROR("Unrecognized mode %d", mode);
		break;
	}
	Layout *l2=get_layout(&glob->ctx, layoutIdx);
	if(!l2)
		return 0;

	jResult=env[0]->NewIntArray(env, 2);
	if(!jResult)
	{
		LOG_ERROR("init(): NewIntArray returned nullptr");
		return 0;
	}
	jint smallArr[]={(jint)layoutIdx, (jint)glob->ctx.layouts->count};
	env[0]->SetIntArrayRegion(env, jResult, 0, 2, smallArr);
	return jResult;
}
EXTERN_C JNIEXPORT void JNICALL Java_com_example_customkb_CKBnativelib_finish(JNIEnv *env, jclass clazz)
{
	if(glob)
		free_context(&glob->ctx);
	free(glob), glob=0;
}
EXTERN_C JNIEXPORT jintArray JNICALL Java_com_example_customkb_CKBnativelib_getTheme(JNIEnv *env, jclass clazz)
{
	jintArray jArr;

	if(!glob)
	{
		LOG_ERROR("getColors(): Globals were not allocated");
		return 0;
	}

	jArr=env[0]->NewIntArray(env, THEME_COLOR_COUNT);
	if(!jArr)
	{
		LOG_ERROR("getColors(): NewIntArray() returned nullptr");
		return 0;
	}
	env[0]->SetIntArrayRegion(env, jArr, 0, THEME_COLOR_COUNT, glob->ctx.theme);
	return jArr;
}
EXTERN_C JNIEXPORT jintArray JNICALL Java_com_example_customkb_CKBnativelib_getLayout(JNIEnv *env, jclass clazz, jint layoutIdx, jboolean hasSymbolsExtension)
{
	ArrayHandle arr, rows, extRows;
	Layout *layout;
	int arrCount;
	jintArray jResult;
	if(!glob)
	{
		LOG_ERROR("getRow(): Globals were not allocated");
		return 0;
	}
	layout=get_layout(&glob->ctx, layoutIdx);
	if(!layout)//OOB layoutIdx
		return 0;

	if(glob->w<glob->h)//portrait
		rows=layout->portrait;
	else//landscape
		rows=layout->landscape;

	//count the total number of buttons in layout
	int nRowsTotal=0, nButtonsTotal=0;
	if(hasSymbolsExtension)
	{
		if(glob->w<glob->h)//portrait
			extRows=glob->ctx.symbolsExtension.portrait;
		else//landscape
			extRows=glob->ctx.symbolsExtension.landscape;
		nRowsTotal+=(int)extRows->count;
		nButtonsTotal+=getTotalButtonCount(extRows);
	}
	else
		extRows=0;
	nRowsTotal+=(int)rows->count;
	nButtonsTotal+=getTotalButtonCount(rows);

	arrCount=3+nRowsTotal*2+nButtonsTotal*2;
	ARRAY_ALLOC(int, arr, arrCount, 0);
	if(!arr)
	{
		LOG_ERROR("malloc returned nullptr");
		return 0;
	}

	int layoutHeight=glob->w<glob->h?layout->p_height:layout->l_height;
	if(hasSymbolsExtension)//calculate layout height		not row height, because of accumulated truncation error
	{
		int originalRowCount, extensionRowCount;
		if(glob->w<glob->h)
			originalRowCount=(int)layout->portrait->count, extensionRowCount=(int)glob->ctx.symbolsExtension.portrait->count;
		else
			originalRowCount=(int)layout->landscape->count, extensionRowCount=(int)glob->ctx.symbolsExtension.landscape->count;
		if(originalRowCount)
			//layoutHeight=layoutHeight*((originalRowCount+extensionRowCount)<<3)/((originalRowCount<<3)+extensionRowCount);
			layoutHeight=layoutHeight*(originalRowCount+extensionRowCount)/originalRowCount;//X  keyboard is too tall
		else
			LOG_ERROR("Row count = 0");
		//LOG_ERROR("h0=%dpx, he=%dpx, h=%dpx", layoutHeight);//FIXME DEBUG
	}
#ifdef DEBUG_ARRAY
	LOG_ERROR("[0]nRows=%d, [1]height=%d, [%d]arrCount=%d", nRowsTotal, layoutHeight, 2+nRowsTotal, arrCount);
#endif
	*(int*)array_at(&arr, 0)=nRowsTotal;
	*(int*)array_at(&arr, 1)=layoutHeight;

	*(int*)array_at(&arr, 2+nRowsTotal)=arrCount;

	int rowIdx=0, start_buttons=3+nRowsTotal;
	if(extRows)
		fillLayoutArray(arr, extRows, &rowIdx, &start_buttons);
	fillLayoutArray(arr, rows, &rowIdx, &start_buttons);

	int *ptr=(int*)array_at(&arr, 0);
	jResult=env[0]->NewIntArray(env, arrCount);
	if(!jResult)
	{
		LOG_ERROR("getLayout(): NewIntArray returned nullptr");
		return 0;
	}
#ifdef DEBUG_ARRAY
	LOG_ERROR("Sending:");//DEBUG
	for(int k=0;k<arrCount;++k)//DEBUG
		LOG_ERROR("[%d]=%d", k, ptr[k]);
#endif
	env[0]->SetIntArrayRegion(env, jResult, 0, arrCount, ptr);
	array_free(&arr, 0);
	return jResult;
}

EXTERN_C JNIEXPORT jstring JNICALL Java_com_example_customkb_CKBnativelib_getLayoutName(JNIEnv *env, jclass clazz, jint layoutIdx)
{
	Layout const *layout;
	const char *a;
	jstring result;
	if(!glob)
	{
		LOG_ERROR("getLayoutName(): Globals were not allocated");
		return 0;
	}

	layout=get_layout(&glob->ctx, layoutIdx);
	if(!layout)
		return 0;
	switch(layout->type)
	{
	case LAYOUT_UNINITIALIZED:		a="Layout\nError";	break;//illegal value

	case LAYOUT_LANG:				a=(char*)layout->lang->data;break;//must have a language 'lang'

	case LAYOUT_SYMBOLS_EXTENSION:	a="Symbols";		break;
	case LAYOUT_NUMPAD:				a="NumPad";			break;
	case LAYOUT_DECNUMPAD:			a="DecNumPad";		break;
	default:						a="Layout\nError";	break;
	}
	result=env[0]->NewStringUTF(env, a);
	if(!result)
	{
		LOG_ERROR("getLayoutName(): NewStringUTF returned nullptr");
		return 0;
	}
	return result;
}

EXTERN_C JNIEXPORT jint JNICALL Java_com_example_customkb_CKBnativelib_getNErrors(JNIEnv *env, jclass clazz)
{
	if(!errors)
		return 0;
	return (int)errors->count;
}
EXTERN_C JNIEXPORT jstring JNICALL Java_com_example_customkb_CKBnativelib_getError(JNIEnv *env, jclass clazz, jint errorIdx)
{
	if(!errors||errorIdx<0||errorIdx>=(int)errors->count)
		return 0;
	ArrayHandle *error=(ArrayHandle*)array_at(&errors, errorIdx);
	return env[0]->NewStringUTF(env, (char*)error[0]->data);
}
EXTERN_C JNIEXPORT void JNICALL Java_com_example_customkb_CKBnativelib_clearErrors(JNIEnv *env, jclass clazz)
{
	array_free(&errors, free_error);
}


//unicode search
EXTERN_C JNIEXPORT jintArray JNICALL Java_com_example_customkb_CKBnativelib_searchUnicode(JNIEnv *env, jclass clazz, jstring query)
{
	const char *input;
	ArrayHandle result;
	jsize count;
	jintArray jResult;

	input=env[0]->GetStringUTFChars(env, query, 0);
	if(!input)
	{
		LOG_ERROR("searchUnicode(): query is nullptr");
		return 0;
	}
	result=unicode_search(input);
	if(result->count)
	{
		count=(jsize)result->count*2;//count*2 because each result is {int code, rank;}
		jResult=env[0]->NewIntArray(env, count);
		if(!jResult)
			LOG_ERROR("searchUnicode(): NewIntArray returned nullptr");
		else
			env[0]->SetIntArrayRegion(env, jResult, 0, count, (int*)result->data);
	}
	else
		jResult=0;
	array_free(&result, 0);
	return jResult;
}


//settings activity
EXTERN_C JNIEXPORT jstring JNICALL Java_com_example_customkb_CKBnativelib_loadConfig(JNIEnv *env, jclass clazz)
{
	ArrayHandle text;
	jstring ret;

	text=load_text(stateFilename);
	if(!text)
		return 0;
	ret=env[0]->NewStringUTF(env, (char*)text->data);
	array_free(&text, 0);
	return ret;
}
EXTERN_C JNIEXPORT jboolean JNICALL Java_com_example_customkb_CKBnativelib_saveConfig(JNIEnv *env, jclass clazz, jstring str)
{
	const char *text;
	size_t text_len;
	Context dummy_ctx={0};
	int success;

	if(!str)
	{
		LOG_ERROR("Error: config text is nullptr");
		return 0;
	}
	text=env[0]->GetStringUTFChars(env, str, 0);
	if(!text)
	{
		LOG_ERROR("Error: config text is nullptr");
		return 0;
	}
	text_len=strlen(text);
	success=parse_state(text, text_len, &dummy_ctx, 0, 0, 0);
	free_context(&dummy_ctx);
	if(success)
		success=save_text(stateFilename, text, text_len);
	env[0]->ReleaseStringUTFChars(env, str, text);
	return success;
}
EXTERN_C JNIEXPORT jboolean JNICALL Java_com_example_customkb_CKBnativelib_storeThemeColor(JNIEnv *env, jclass clazz, jint color, jint idx)
{
	ArrayHandle text;
	int success;

	text=load_text(stateFilename);
	if(!text)
	{
		LOG_ERROR("storeThemeColors(): failed to open %s", stateFilename);
		return 0;
	}
	success=parse_state(0, 0, 0, &text, color, idx);
	if(!success)
		return 0;
	success=save_text(stateFilename, (char*)text->data, text->count);
	if(!success)
	{
		LOG_ERROR("storeThemeColors(): failed to write to %s", stateFilename);
		return 0;
	}
	return 1;
}
EXTERN_C JNIEXPORT jboolean JNICALL Java_com_example_customkb_CKBnativelib_resetConfig(JNIEnv *env, jclass clazz)
{
	size_t len;
	Context dummy_ctx={0};
	int success;

	len=strlen(default_config);

	success=parse_state(default_config, len, &dummy_ctx, 0, 0, 0);
	if(!success)
	{
		LOG_ERROR("Internal error in default config file");
		return 0;
	}
	free_context(&dummy_ctx);

	success=save_text(stateFilename, default_config, len);
	if(!success)
	{
		LOG_ERROR("Failed to write to %s", stateFilename);
		return 0;
	}
	return success;
}
