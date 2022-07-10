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

//	#define ALWAYS_RESET
//	#define STORE_ERRORS

const char statefn[]="/data/data/com.example.customkb/state.txt";//UPDATE AT RELEASE
const char tempfn[]="/data/data/com.example.customkb/temp.txt";
const char log_tag[]="customkb";

#define 	G_BUF_SIZE	1024
char 		g_buf[G_BUF_SIZE]={0};

extern const char default_config[];

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
const char *mcodes[]=
{
#define		CKBKEY(FLAG, DESCRIPTION, KEYWORD)	KEYWORD,
#include	"ckb_mkeys.h"
#undef		CKBKEY
};

Globals		*glob=0;
ArrayHandle	errors=0;
int			nerrors=0;
JavaVM		*jvm=0;

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

void		free_row(void *data)
{
	ArrayHandle *row=(ArrayHandle*)data;
	array_free(row, 0);
}
void		free_layout(void *data)
{
	Layout *layout=(Layout*)data;
	array_free(&layout->lang, 0);
	array_free(&layout->portrait, free_row);
	array_free(&layout->landscape, free_row);
}
void		free_context(Context *ctx)
{
	array_free(&ctx->layouts, free_layout);
	array_free(&ctx->defaultlang, 0);
}

int			log_error(const char *f, int line, const char *format, ...)
{
	va_list args;
	int size=(int)strlen(f), start=size-1, printed=0;
	for(;start>=0&&f[start]!='/'&&f[start]!='\\';--start);
	start+=start==-1||f[start]=='/'||f[start]=='\\';

	printed+=snprintf(g_buf+printed, G_BUF_SIZE-printed, "[%d] %s(%d): ", nerrors, f+start, line);
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
	LOGE("errors == %p", errors);//

	if(!errors)
		ARRAY_ALLOC(ArrayHandle, errors, 0, 0);
	ArrayHandle *e=(ArrayHandle*)ARRAY_APPEND(errors, 0, 1, 1, 0);

	LOGE("e == %p", e);//
	if(e)//
		LOGE("*e == %p", *e);//

	array_assign(e, g_buf, printed+1);
#endif

	++nerrors;
	return nerrors-1;
}

ArrayHandle	load_text(const char *filename)
{
	struct stat info={0};
	FILE *f;
	ArrayHandle ret;
	size_t bytesread;

	int error=stat(filename, &info);
	if(error)
	{
		LOG_ERROR("Cannot read %s, %d:%s", filename, error, strerror(error));
		return 0;
	}
	f=fopen(filename, "r");
	if(!f)
	{
		LOG_ERROR("Cannot read %s", filename);
		return 0;
	}
	STR_ALLOC(ret, info.st_size);
	bytesread=fread(ret->data, 1, info.st_size, f);
	fclose(f);
	ret->count=bytesread;
	//array_fit(&ret, 1);
	((char*)ret->data)[bytesread]='\0';
	return ret;
}
int 		save_text(const char *filename, const char *text, size_t len)
{
	FILE *f;
	int error;

	f=fopen(filename, "w");
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
	int nlayouts=(int)glob->ctx.layouts->count;
	Layout *layout;

	if(type==LAYOUT_LANG||type==LAYOUT_URL)
	{
		for(int kl=0;kl<nlayouts;++kl)
		{
			layout=(Layout*)array_at(&glob->ctx.layouts, kl);
			if(layout->type==type&&!strcmp((char*)layout->lang->data, (char*)glob->ctx.defaultlang->data))
				return kl;
		}
	}
	else
	{
		for(int kl=0;kl<nlayouts;++kl)
		{
			layout=(Layout*)array_at(&glob->ctx.layouts, kl);
			if(layout->type==type)
				return kl;
		}
	}
	return -1;
}
ArrayHandle	get_rows(int layoutidx)
{
	Layout const *layout=(Layout const*)array_at(&glob->ctx.layouts, layoutidx);
	ArrayHandle rows;
	if(glob->w<glob->h)//portrait
		rows=layout->portrait;
	else
		rows=layout->landscape;
	if(!rows)
		LOG_ERROR("Rows pointer == nullptr");
	return rows;
}

EXTERN_C JNIEXPORT jint JNICALL Java_com_example_customkb_CKBnativelib_init(JNIEnv *env, jclass clazz, jint mode, jint decnumpad, jint width, jint height)
{
	int ret;
	ArrayHandle text;

	ret=env[0]->GetJavaVM(env, &jvm);
	if(ret)
		LOG_ERROR("GetJavaVM() returned %d", ret);

	ret=glob_alloc();
	if(ret)
	{
		glob->mode=(ModeType)mode;
		glob->w=width, glob->h=height;
	}
	if(ret==1)
	{
#ifdef ALWAYS_RESET
		text=0;
#else
		text=load_text(statefn);
#endif
		if(!text)
		{
			size_t len=strlen(default_config);
			ret=save_text(statefn, default_config, len);
			if(ret)
				text=load_text(statefn);
		}
		if(text)
		{
			ret=parse_state((char*)text->data, text->count, &glob->ctx);
			ret&=calc_raster_sizes(&glob->ctx, glob->w, glob->h, glob->w>glob->h);
			array_free(&text, 0);
		}
		else
			ret=0;
	}
	else
		ret=1;

	if(ret)
	{
		switch(glob->mode)
		{
		//text modes
		case MODE_TEXT:
			glob->layoutidx=find_layout_idx(LAYOUT_LANG, glob->ctx.defaultlang);
			break;
		case MODE_PASSWORD:
			glob->layoutidx=find_layout_idx(LAYOUT_ASCII, 0);
			break;
		case MODE_URL:
		case MODE_EMAIL:
			glob->layoutidx=find_layout_idx(LAYOUT_URL, glob->ctx.defaultlang);
			break;

		//numeric modes
		case MODE_NUMBER:
		case MODE_PHONE_NUMBER:
		case MODE_NUMERIC_PASSWORD:
			if(decnumpad)
				glob->layoutidx=find_layout_idx(LAYOUT_DECNUMPAD, 0);
			else
				glob->layoutidx=find_layout_idx(LAYOUT_NUMPAD, 0);
			break;
		}
		Layout *l2=array_at(&glob->ctx.layouts, glob->layoutidx);
		if(width<height)//portrait
			return (int)l2->portrait->count;
		return (int)l2->landscape->count;
	}
	return 0;
}
EXTERN_C JNIEXPORT void JNICALL Java_com_example_customkb_CKBnativelib_finish(JNIEnv *env, jclass clazz)
{
	if(glob)
		free_context(&glob->ctx);
}
EXTERN_C JNIEXPORT jint JNICALL Java_com_example_customkb_CKBnativelib_getKbHeight(JNIEnv *env, jclass clazz)
{
	Layout *layout=(Layout*)array_at(&glob->ctx.layouts, glob->layoutidx);
	if(glob->w<glob->h)
		return layout->p_height;
	return layout->l_height;
}
EXTERN_C JNIEXPORT jintArray JNICALL Java_com_example_customkb_CKBnativelib_getColors(JNIEnv *env, jclass clazz)
{
	jintArray jArr;

	if(!glob)
	{
		LOG_ERROR("getColors(): Globals were not allocated");
		return 0;
	}

	jArr=env[0]->NewIntArray(env, THEME_COLOR_COUNT);
	if(!jArr)
		return 0;
	env[0]->SetIntArrayRegion(env, jArr, 0, THEME_COLOR_COUNT, glob->ctx.theme);
	return jArr;
}
EXTERN_C JNIEXPORT jintArray JNICALL Java_com_example_customkb_CKBnativelib_getRow(JNIEnv *env, jclass clazz, jint rowIdx)
{
	ArrayHandle retRow;
	Layout *layout;
	Row *row;
	int arrSize;
	jintArray jArr;

	if(!glob)
	{
		LOG_ERROR("getRow(): Globals were not allocated");
		return 0;
	}

	layout=(Layout*)array_at(&glob->ctx.layouts, glob->layoutidx);
	if(glob->w<glob->h)//portrait
		row=(Row*)array_at(&layout->portrait, rowIdx);
	else
		row=(Row*)array_at(&layout->landscape, rowIdx);

	arrSize=3+2*(int)row->buttons->count;
	ARRAY_ALLOC(int, retRow, arrSize, 0);

	int *ptr=(int*)array_at(&retRow, 0);
	*ptr=row->y1, ++ptr;
	*ptr=row->y2, ++ptr;
	*ptr=0, ++ptr;
	for(int kb=0;kb<(int)row->buttons->count;++kb)
	{
		Button *button=(Button*)array_at(&row->buttons, kb);
		*ptr=button->code, ++ptr;
		*ptr=button->x2, ++ptr;
	}

	ptr=(int*)array_at(&retRow, 0);
	jArr=env[0]->NewIntArray(env, arrSize);
	env[0]->SetIntArrayRegion(env, jArr, 0, arrSize, ptr);

	array_free(&retRow, 0);
	return jArr;
}

EXTERN_C JNIEXPORT int JNICALL Java_com_example_customkb_CKBnativelib_nextLayout(JNIEnv *env, jclass clazz)
{
	if(!glob)
	{
		LOG_ERROR("nextLayout(): Globals were not allocated");
		return -1;
	}
	if(glob->layoutidx<0)
	{
		LOG_ERROR("nextLayout(): Invalid layout idx");
		return -1;
	}
	switch(glob->mode)
	{
	case MODE_NUMBER:
	case MODE_PHONE_NUMBER:
	case MODE_NUMERIC_PASSWORD:
		return 0;
	default://'-Wall' was a mistake
		break;
	}
	Layout *layout=(Layout*)array_at(&glob->ctx.layouts, glob->layoutidx);
	int idx=-1;
	switch(layout->type)
	{
	case LAYOUT_LANG:
	case LAYOUT_URL:		//just switch to ASCII
		idx=find_layout_idx(LAYOUT_ASCII, 0);
		break;
	case LAYOUT_ASCII:
	case LAYOUT_NUMPAD:
	case LAYOUT_DECNUMPAD:	//reset to lang/url <defaultlang>
		switch(glob->mode)
		{
		case MODE_TEXT:
		case MODE_PASSWORD:
		case MODE_URL:case MODE_EMAIL:
			idx=find_layout_idx(LAYOUT_LANG, glob->ctx.defaultlang);
			break;
		default://'-Wall' was a mistake
			break;
		}
		break;
	default://'-Wall' was a mistake
		break;
	}
	if(idx<0)
		return 0;
	glob->layoutidx=idx;
	ArrayHandle rows=get_rows(glob->layoutidx);
	if(!rows)
		return 0;
	return (int)rows->count;
}
EXTERN_C JNIEXPORT jint JNICALL Java_com_example_customkb_CKBnativelib_nextLanguage(JNIEnv *env, jclass clazz)
{
	if(!glob)
	{
		LOG_ERROR("nextLayout(): Globals were not allocated");
		return -1;
	}
	if(glob->layoutidx<0)
	{
		LOG_ERROR("nextLayout(): Invalid layout idx");
		return -1;
	}
	switch(glob->mode)
	{
	case MODE_NUMBER:
	case MODE_PHONE_NUMBER:
	case MODE_NUMERIC_PASSWORD:
		return 0;
	default://'-Wall' was a mistake
		break;
	}
	Layout *layout=(Layout*)array_at(&glob->ctx.layouts, glob->layoutidx);
	int nlayouts=(int)glob->ctx.layouts->count, idx=-1;
	for(int kl=(glob->layoutidx+1)%nlayouts;kl!=glob->layoutidx;kl=(kl+1)%nlayouts)
	{
		Layout *l2=(Layout*)array_at(&glob->ctx.layouts, kl);
		if(l2->type==LAYOUT_LANG||l2->type==LAYOUT_URL)
		{
			if((layout->type==LAYOUT_LANG||layout->type==LAYOUT_URL)&&l2->type!=layout->type)
				continue;
			idx=kl;
			break;
		}
	}
	if(idx<0)
		return 0;
	glob->layoutidx=idx;
	ArrayHandle rows=get_rows(glob->layoutidx);
	if(!rows)
		return 0;
	return (int)rows->count;
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

//settings activity
EXTERN_C JNIEXPORT jstring JNICALL Java_com_example_customkb_CKBnativelib_loadConfig(JNIEnv *env, jclass clazz)
{
	ArrayHandle text;
	jstring ret;

	text=load_text(statefn);
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
	Context ctx={0};
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
	success=parse_state(text, text_len, &ctx);
	free_context(&ctx);
	if(success)
		success=save_text(statefn, text, text_len);
	return success;
}
EXTERN_C JNIEXPORT jboolean JNICALL Java_com_example_customkb_CKBnativelib_resetConfig(JNIEnv *env, jclass clazz)
{
	size_t len=strlen(default_config);
	return save_text(statefn, default_config, len);
}
