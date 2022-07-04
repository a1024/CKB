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

const char statefn[]="/data/data/com.example.customkb/state.txt";//UPDATE AT RELEASE
const char tempfn[]="/data/data/com.example.customkb/temp.txt";
const char log_tag[]="CKBnativelib";
#ifdef __cplusplus
#define EXTERN_C	extern "C"
#else
#define EXTERN_C
#endif

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
int			nerrors=0;

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
}

int			log_error(const char *f, int line, const char *format, ...)
{
	va_list args;
	int size=(int)strlen(f), start=size-1, printed=0;
	for(;start>=0&&f[start]!='/'&&f[start]!='\\';--start);
	start+=start==-1||f[start]=='/'||f[start]=='\\';
	printed+=snprintf(g_buf+printed, G_BUF_SIZE-printed, "%s(%d): ", f+start, line);
	if(format)
	{
		va_start(args, format);
		printed+=vsnprintf(g_buf+printed, G_BUF_SIZE-printed, format, args);
		va_end(args);
	}
	else
		printed+=snprintf(g_buf+printed, G_BUF_SIZE-printed, "Error");

	LOGE("%s", g_buf);//TODO: display error on keyboard

#if 0
	if(glob_alloc())
	{
		if(!glob->error)
			STR_ALLOC(glob->error, printed+1);
		array_assign(&glob->error, g_buf, printed+1);
	}
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


EXTERN_C JNIEXPORT int JNICALL Java_com_example_customkb_CKBnativelib_init(JNIEnv *env, jclass clazz, jint width, jint height)
{
	int ret;
	ArrayHandle text;

	ret=glob_alloc();
	if(ret)
		glob->w=width, glob->h=height;
	if(ret==1)
	{
		text=load_text(statefn);
		if(!text)
		{
			size_t len=strlen(default_config);
			ret=save_text(statefn, default_config, len);
			if(ret)
				text=load_text(statefn);
		}
		if(text)
		{
			parse_state(text, &glob->ctx);
			calc_raster_sizes(&glob->ctx, glob->w, glob->h, glob->w>glob->h);
		}
	}
	return 0;
}
