//
// Created by MSI on 2021-12-03.
//


#include <jni.h>
#include <android/log.h>

#include"array.h"
#include<stdio.h>
#include<stdlib.h>
#include<stdarg.h>
#include<string.h>
#include<sys/stat.h>
#include<errno.h>
#include<ctype.h>
#include<math.h>

const char statefn[]="/data/data/com.example.customkb/state.txt";//UPDATE AT RELEASE
const char tempfn[]="/data/data/com.example.customkb/temp.txt";
static const char file[]=__FILE__;
const char log_tag[]="CKBnativelib";
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO, log_tag, ##__VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR, log_tag, ##__VA_ARGS__)
#ifdef __cplusplus
#define EXTERN_C	extern "C"
#else
#define EXTERN_C
#endif

#define 	G_BUF_SIZE	1024
char 		g_buf[G_BUF_SIZE]={0};
extern const char default_config[];

typedef enum KeyTypeEnum
{
#define		CKBKEY(FLAG, DESCRIPTION)		FLAG,
#include	"ckb_keys.h"
#undef		CKBKEY
} KeyType;
const char *buttonlabels[]=
{
#define		CKBKEY(FLAG, DESCRIPTION)		DESCRIPTION,
#include	"ckb_keys.h"
#undef		CKBKEY
};
typedef enum ModKeyTypeEnum
{
	KEY_MODSTART=0xFFFFFF,
#define		CKBKEY(FLAG, DESCRIPTION, KEYWORD)		FLAG,
#include	"ckb_mkeys.h"
#undef		CKBKEY
} ModKeyType;
const char *modbuttonlabels[]=
{
#define		CKBKEY(FLAG, DESCRIPTION, KEYWORD)		DESCRIPTION,
#include	"ckb_mkeys.h"
#undef		CKBKEY
};

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
Globals *glob=0;
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
int			nerrors=0;
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

	//if(glob_alloc())
	//{
	//	if(!glob->error)
	//		STR_ALLOC(glob->error, printed+1);
	//	array_assign(&glob->error, g_buf, printed+1);
	//}
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

int			get_lineno(ArrayConstHandle text, int k)
{
	int lineno=0;
	for(int k2=0;k2<k;lineno+=text->data[k2]=='\n', ++k2);
	return lineno;
}
void 		parse_error(ArrayConstHandle text, int k, const char *kw_expected)
{
	int lineno=get_lineno(text, k);
	LOGE("Config line %d: Expected \'%s\'", lineno, kw_expected);
	//LOG_ERROR("Config line %d: Expected \'%s\'", lineno, kw_expected);
	++nerrors;
}
void 		parse_error_eof(ArrayConstHandle text, int k, const char *expected)
{
	int lineno=get_lineno(text, k);
	LOGE("Config line %d: Unexpected end of file. %s", lineno, expected);
	++nerrors;
}
int 		skip_ws(ArrayConstHandle text, int *k)
{
	for(;*k<(int)text->count;++*k)
	{
		switch(text->data[*k])
		{
		case '/':
			if(text->data[*k+1]=='/')//line comment
			{
				*k+=2;
				for(;*k<text->count&&text->data[*k]!='\n';++*k);
				*k+=*k<text->count;
			}
			else if(text->data[*k+1]=='*')//block comment
			{
				*k+=2;
				for(;*k+1<text->count&&!(text->data[*k]=='*'&&text->data[*k+1]=='/');++*k);
				*k+=*k<text->count&&text->data[*k]=='*';
				*k+=*k<text->count&&text->data[*k]=='/';
			}
			continue;
		default:
			if(isspace(text->data[*k]))
				continue;
			break;
		}
		break;
	}
	return *k>=text->count;
}
int 		match_ascii_ci(ArrayConstHandle text, int *k, const char *kw)
{
	int match=1;
	for(int k2=0;;++*k, ++k2)
	{
		if(tolower(text->data[*k])!=tolower(kw[k2]))
		{
			match=0;
			break;
		}
	}
	return match;
}
int 		get_id(ArrayConstHandle text, int k)//returns length of identifier
{
	int end=k;

	if(end<text->count&&(isalpha(text->data[end])||text->data[end]=='_'))
	{
		++end;
		for(;end<text->count&&(isalnum(text->data[end])||text->data[end]=='_');++end);
	}
	return end-k;
}
int			memcmp_ascii_ci(const char *text, const char *kw)//returns length of keyword on match, otherwise zero
{
	int k=0;
	for(;*text&&*kw&&tolower(*text)==tolower(*kw);++text, ++kw, ++k);
	if(*text&&*kw)
		return 0;
	return k;
}
int			parse_langname(ArrayConstHandle text, int *k, Layout *layout)
{
	if(skip_ws(text, k))
	{
		parse_error_eof(text, *k, "Expected language type");
		return 0;
	}
	int len=get_id(text, *k);
	STR_ALLOC(layout->name, len);
	memcpy(layout->name->data, text->data+*k, len);
	return 1;
}
long long 	read_int(ArrayConstHandle text, int *k, int base, int *ret_ndigits)
{
	long long val;
	int ndigits, limit;
	unsigned char c;

	val=0;
	ndigits=0;
	limit=base<10?base:10;
	for(;*k<text->count;)
	{
		c=text->data[*k]-'0';
		if(c<limit)
		{
			val*=base;
			val+=c;
		}
		else if(base==16)
		{
			c=(unsigned char)((text->data[*k]&0xDF)-'A');
			if(c>=6)
				break;
			val<<=4;
			val|=c+10;
		}
		else
			break;
	}
	if(ret_ndigits)
		*ret_ndigits=ndigits;
	return val;
}
float		read_float(ArrayConstHandle text, int *k)
{
	float val;
	long long temp;
	int ndigits;

	val=(float)read_int(text, k, 10, 0);
	if(text->data[*k]=='.')
	{
		++*k;
		temp=read_int(text, k, 10, &ndigits);
		val+=(float)temp*powf(10, -(float)ndigits);
	}
	return val;
}
int 		parse_state(ArrayConstHandle text, Context *ctx)
{
	const char
		kw_layout[]="layout",
		kw_lang[]="lang", kw_url[]="url", kw_ascii[]="ascii",
		kw_numpad[]="numpad", kw_decnumpad[]="decnumpad",

		kw_portrait[]="portrait", kw_landscape[]="landscape";

	int k, len, ret;
	Layout *layout;
	ArrayHandle *rows;
	float *layout_height;

	ret=1;
	if(ctx->layouts)
		LOG_ERROR("Possible memory leak: cts->layouts == %p", ctx->layouts);
	ARRAY_ALLOC(Layout, ctx->layouts, 0, 0);
	for(k=0;;)
	{
		if(skip_ws(text, &k))
			break;
		if(!match_ascii_ci(text, &k, kw_layout))
			parse_error(text, k, kw_layout);
		if(skip_ws(text, &k))
		{
			int lineno=get_lineno(text, k);
			LOGE("Config error line %d: Expected layout type", lineno);
			ret=0, ++nerrors;
			break;
		}
		layout=(Layout*)ARRAY_APPEND(ctx->layouts, 0, 1, 1, 0);
		if(len=memcmp_ascii_ci((char *)text->data+k, kw_lang))
		{
			k+=len;
			layout->type=LAYOUT_LANG;
			if(!parse_langname(text, &k, layout))
				break;
		}
		else if(len=memcmp_ascii_ci((char*)text->data+k, kw_url))
		{
			k+=len;
			layout->type=LAYOUT_URL;
			if(!parse_langname(text, &k, layout))
				break;
		}
		else if(len=memcmp_ascii_ci((char*)text->data+k, kw_ascii))
		{
			k+=len;
			layout->type=LAYOUT_ASCII;
		}
		else if(len=memcmp_ascii_ci((char*)text->data+k, kw_numpad))
		{
			k+=len;
			layout->type=LAYOUT_NUMPAD;
		}
		else if(len=memcmp_ascii_ci((char*)text->data+k, kw_decnumpad))
		{
			k+=len;
			layout->type=LAYOUT_DECNUMPAD;
		}
		else
		{
			int lineno=get_lineno(text, k);
			LOGE("Config error line %d: Expected layout type", lineno);
			ret=0, ++nerrors;
			break;
		}
		if(skip_ws(text, &k))
		{
			int lineno=get_lineno(text, k);
			LOGE("Config error line %d: Expected portrait or landscape declarations", lineno);
			ret=0, ++nerrors;
			break;
		}
		for(int k2=0;k2<2;++k2)
		{
			if(len=memcmp_ascii_ci((char *)text->data+k, kw_portrait))
			{
				k+=len;
				rows=&layout->portrait;
				layout_height=&layout->p_height;
			}
			else if(len=memcmp_ascii_ci((char*)text->data+k, kw_landscape))
			{
				k+=len;
				rows=&layout->landscape;
				layout_height=&layout->l_height;
			}
			else
			{
				int lineno=get_lineno(text, k);
				LOGE("Config error line %d: Expected portrait or landscape declaration", lineno);
				ret=0, ++nerrors;
				break;
			}
			if(*rows)
			{
				int lineno=get_lineno(text, k);
				LOGE("Config error line %d: Expected portrait or landscape declaration appeared before", lineno);
				ret=0, ++nerrors;
				break;
			}
			if(skip_ws(text, &k))
			{
				int lineno=get_lineno(text, k);
				LOGE("Config error line %d: Expected layout height", lineno);
				ret=0, ++nerrors;
				break;
			}
			*layout_height=read_float(text, &k);
			if(skip_ws(text, &k)||text->data[k]!='{')
			{
				int lineno=get_lineno(text, k);
				LOGE("Config error line %d: Expected layout definition", lineno);
				ret=0, ++nerrors;
				break;
			}
			++k;//skip '{'
			for(;k<text->count;)//row loop
			{
				for(;k<text->count;)//button loop
				{
					if(skip_ws(text, &k))
						break;
					
				}
				if(skip_ws(text, &k))
					break;
			}
			if(skip_ws(text, &k))
				break;
		}
		if(!ret)
			break;
	}

	return ret;
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
			parse_state(text, &glob->ctx);
	}
	return 0;
}
