/*	parser.c - The implementation of the config parser
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
// Created by MSI on 2022-07-03.
//

#include"ckb.h"
#include<stdlib.h>
#include<string.h>
#include<ctype.h>
#include<math.h>
static const char file[]=__FILE__;


//	#define DEBUG_STRCMP
//	#define DEBUG_COLORS
//	#define DEBUG_PARSER


#define 	G_BUF_SIZE	1024
extern char g_buf[G_BUF_SIZE];//used by l o g _ e r r o r()

static int	get_lineNo(const char *text, int k, int *ret_lineStart)
{
	int lineNo=0, lineStart=0;
	for(int k2=0;k2<k;lineNo+=text[k2]=='\n', ++k2)
	{
		if(text[k2]=='\n')
			lineStart=k2;
	}
	if(ret_lineStart)
		*ret_lineStart=lineStart;
	return lineNo;
}
static int	parse_error(const char *text, int k, const char *msg)
{
	int lineNo, lineStart;

	lineNo=get_lineNo(text, k, &lineStart);
#ifdef _MSC_VER
	LOGE("Config(%d:%d) %s\n", lineNo+1, k-lineStart, msg);
#else
	log_error("config", lineNo+1, "col %d: %s", k-lineStart, msg);
#endif
	++nErrors;
	return 0;
}
static int	skip_ws(const char *text, size_t text_len, int *k)
{
#ifdef DEBUG_PARSER
	int k0=*k;
#endif
	for(;*k<(int)text_len;++*k)
	{
		switch(text[*k])
		{
		case '/':
			if(text[*k+1]=='/')//line comment
			{
				*k+=2;
				for(;*k<(int)text_len&&text[*k]!='\n';++*k);
				//*k+=*k<(int)text_len;//there is ++*k in loop header
			}
			else if(text[*k+1]=='*')//block comment
			{
#ifdef DEBUG_PARSER
				LOG_ERROR("Block comment at %d", *k);//
#endif
				*k+=2;
				for(;*k+1<(int)text_len&&!(text[*k]=='*'&&text[*k+1]=='/');++*k);
				*k+=*k<(int)text_len&&text[*k]=='*';
				//*k+=*k<(int)text_len&&text[*k]=='/';//there is ++*k in loop header
			}
			else
				break;
			continue;
		default:
			if(isspace(text[*k]))
				continue;
			break;
		}
		break;
	}
#ifdef DEBUG_PARSER
	int print=50;
	if(print+*k>=(int)text_len)
		print=(int)text_len-*k;
	LOG_ERROR("skip_ws: k %d -> %d text=%.*s", k0, *k, print, text+*k);//
#endif
	return *k>=(int)text_len;
}
static int	acme_strCmp_ascii_ci(const char *text, size_t text_len, int *k, const char *kw)//returns zero on match, otherwise which string comes first alphabetically
{
	int match=0;
	for(int k2=0;kw[k2];++*k, ++k2)
	{
		if(*k>=text_len)
			return -1;
		int diff=tolower(text[*k])-tolower(kw[k2]);
		if(diff)
		{
			match=(diff>0)-(diff<0);
			break;
		}
	}
	return match;
}
static int	get_id(const char *text, size_t text_len, int k)//returns length of identifier
{
	int end=k;

	if(end<(int)text_len&&(isalpha(text[end])||text[end]=='_'))
	{
		++end;
		for(;end<(int)text_len&&(isalnum(text[end])||text[end]=='_');++end);
	}
	return end-k;
}
static int	memCmp_ascii_ci(const char *text, const char *kw)//returns length of keyword on match, otherwise zero
{
	int k=0;
	for(;*text&&*kw&&tolower(*text)==tolower(*kw);++text, ++kw, ++k);
	if(*text&&*kw)
		return 0;
	return k;
}
static int	parse_langName(const char *text, size_t text_len, int *k, ArrayHandle *lang)
{
	if(skip_ws(text, text_len, k))
		return parse_error(text, *k, "Expected language type");
	int len=get_id(text, text_len, *k);
	STR_ALLOC(*lang, len);
	memcpy(lang[0]->data, text+*k, len);
	*k+=len;
	return 1;
}
static long long read_int(const char *text, size_t text_len, int *k, int base, int *ret_nDigits)//ret_nDigits: if a variable is provided, a non-zero digit limit can be provided
{
	long long val;
	int nDigits, limit, end;
	unsigned char c;

	val=0;
	nDigits=0;
	limit=base<10?base:10;
	end=(int)text_len;
	if(ret_nDigits&&*ret_nDigits&&*k+*ret_nDigits<end)
		end=*k+*ret_nDigits;
	for(;*k<end;++*k, ++nDigits)
	{
		c=text[*k]-'0';
		if(c<limit)
		{
			val*=base;
			val+=c;
		}
		else if(base==16)
		{
			c=(unsigned char)((text[*k]&0xDF)-'A');
			if(c>=6)
				break;
			val<<=4;
			val|=c+10;
		}
		else
			break;
	}
	if(ret_nDigits)
		*ret_nDigits=nDigits;
	return val;
}
static float read_float(const char *text, size_t text_len, int *k)
{
	float val;
	long long temp;
	int nDigits;

	val=(float)read_int(text, text_len, k, 10, 0);
	if(text[*k]=='.')
	{
		++*k;
		nDigits=0;
		temp=read_int(text, text_len, k, 10, &nDigits);
		val+=(float)temp*powf(10, -(float)nDigits);
	}
	return val;
}
static int	read_fraction(const char *text, size_t text_len, int *k, float *ret_val)//also skips trailing whitespace
{
	float temp;

	*ret_val=read_float(text, text_len, k);
	if(skip_ws(text, text_len, k))
		return 2;
	if(text[*k]=='/')
	{
		++*k;
		if(skip_ws(text, text_len, k))
			return parse_error(text, *k, "Expected a denominator");
		temp=read_float(text, text_len, k);
		if(!temp)
			return parse_error(text, *k, "Division by zero");
		*ret_val/=temp;
		if(skip_ws(text, text_len, k))
			return 2;
	}
	return 1;
}
static int	utf8ToCodepoint(const char *in, size_t text_len, int *k)
{
	char c, c2, c3, c4;

	c=in[*k];
	if(!(c&0x80))//binary 0*******		Note: c>=0 doesn't work here
	{
		++*k;
		return c;
	}
	if((c&0xE0)==0xC0)//binary 110*****, 10******
	{
		if(*k+1>=text_len)
		{
			LOG_ERROR("Invalid UTF-8 sequence: 0x%02X\n", (int)c);
			return '?';
		}
		c2=in[*k+1];
		*k+=2;
		if((c2&0xC0)==0x80)
			return (c&0x1F)<<6|(c2&0x3F);
		LOG_ERROR("Invalid UTF-8 sequence: 0x%02X-%02X\n", (int)c, (int)c2);
		return '?';
	}
	if((c&0xF0)==0xE0)//binary 1110****, 10******, 10******
	{
		if(*k+2>=text_len)
		{
			LOG_ERROR("Invalid UTF-8 sequence: 0x%02X\n", (int)c);
			return '?';
		}
		c2=in[*k+1], c3=in[*k+2];
		*k+=3;
		if((c2&0xC0)==0x80&&(c3&0xC0)==0x80)
			return (c&0x0F)<<12|(c2&0x3F)<<6|(c3&0x3F);
		LOG_ERROR("Invalid UTF-8 sequence: 0x%02X-%02X-%02X\n", (int)c, (int)c2, (int)c3);
		return '?';
	}
	if((c&0xF8)==0xF0)//binary 11110***, 10******, 10******, 10******
	{
		if(*k+3>=text_len)
		{
			LOG_ERROR("Invalid UTF-8 sequence: 0x%02X\n", (int)c);
			return '?';
		}
		c2=in[*k+1], c3=in[*k+2], c4=in[*k+3];
		*k+=4;
		if((c2&0xC0)==0x80&&(c3&0xC0)==0x80&&(c4&0xC0)==0x80)
			return (c&0x07)<<18|(c2&0x3F)<<12|(c3&0x3F)<<6|(c4&0x3F);
		LOG_ERROR("Invalid UTF-8 sequence: 0x%02X-%02X-%02X-%02X\n", (int)c, (int)c2, (int)c3, (int)c4);
		return '?';
	}
	LOG_ERROR("Invalid UTF-8 sequence: 0x%02X\n", (int)c);
	return '?';
}
static int	esc2codepoint(const char *text, size_t text_len, int *k)//one escape sequence -> code point
{
	int code, nDigits;

	if(text[*k]!='\\')
		return utf8ToCodepoint(text, text_len, k);

	++*k;//skip '\\'
	switch(text[*k])
	{
	case 'a':	++*k;return '\a';
	case 'b':	++*k;return '\b';
	case 'f':	++*k;return '\f';
	case 'n':	++*k;return '\n';
	case 'r':	++*k;return '\r';
	case 't':	++*k;return '\t';
	case 'v':	++*k;return '\v';
	case '\'':	++*k;return '\'';
	case '\"':	++*k;return '\"';
	case '\\':	++*k;return '\\';
	case '\?':	++*k;return '?';

	case '0':case '1':case '2':case '3':case '4':case '5':case '6':case '7'://3 octal(s)
		nDigits=3;
		code=(int)read_int(text, text_len, k, 8, &nDigits);
		break;
	case 'x':
		++*k;
		nDigits=0;//unlimited
		code=(int)read_int(text, text_len, k, 16, &nDigits);
		break;
	case 'u'://4 hex
		++*k;
		nDigits=4;
		code=(int)read_int(text, text_len, k, 16, &nDigits);
		break;
	case 'U'://8 hex
		++*k;
		nDigits=8;
		code=(int)read_int(text, text_len, k, 16, &nDigits);
		break;
	default:
		log_error("Unknown", 0, "Invalid escape sequence: %.*s", 3, text+*k-1);
		//LOGE("Invalid escape sequence: %.*s", 3, text->data+*k-1);
		code='?';
		break;
	}
	return code;
}
typedef struct KeywordStruct
{
	const char *kw;
	int code;
} Keyword;
static ArrayHandle kwMap=0;
static int	kwMap_cmp(const void *left, const void *right)
{
	const char
		*s1=((Keyword const*)left)->kw,
		*s2=((Keyword const*)right)->kw;

	for(;*s1&&*s2&&*s1==*s2;++s1, ++s2);
	if(!*s1&&!*s2)
		return 0;

	char ac=*s1, bc=*s2;
	if(!ac)
		ac=127;
	if(!bc)
		bc=127;
	return (ac>bc)-(ac<bc);
}
#ifdef _MSC_VER
static void	debug_print_keywords()
{
	printf("Keywords:\n");
	for(int k=0;k<(int)kwMap->count;++k)
	{
		Keyword const *kw=(Keyword const*)array_at_const(&kwMap, k);
		printf("%3d\t%s\n", k, kw->kw);
	}
	printf("\n");
}
#endif
static int	find_codepoint(const char *text, size_t text_len, int *k)
{
	int L, R, mid;

	L=0, R=(int)kwMap->count-1;
	while(L<=R)
	{
		mid=(L+R)>>1;
		int k2=*k;
		Keyword const *kw=(Keyword const*)array_at(&kwMap, mid);
		int ret=acme_strCmp_ascii_ci(text, text_len, &k2, kw->kw);
		if(ret>0)
			L=mid+1;
		else if(ret<0)
			R=mid-1;
		else
		{
			*k=k2;
			return kw->code;
		}
	}
	parse_error(text, *k, "Expected a codepoint or a button keyword");
	return -1;
}
static int	read_codepoint(const char *text, size_t text_len, int *k)
{
	int k0, code;

	if(!kwMap)//initialize kwMap
	{
		ARRAY_ALLOC(Keyword, kwMap, MKEY_COUNT, 0);
		for(int kk=0;kk<MKEY_COUNT;++kk)
		{
			Keyword *kw=array_at(&kwMap, kk);
			kw->code=MODMASK|kk;
			kw->kw=mcodes[kk];
		}
#ifdef _MSC_VER
		debug_print_keywords();//
#endif
#if 0
		qsort(kwMap->data, MKEY_COUNT, sizeof(Keyword), kwMap_cmp);//BROKEN
#else
		isort(kwMap->data, kwMap->count, kwMap->esize, kwMap_cmp);
#endif
#ifdef _MSC_VER
		debug_print_keywords();//
#endif
	}

	k0=*k;
	switch(text[*k])
	{
	case '\'':
		++*k;//skip '\''
		code=esc2codepoint(text, text_len, k);
		if(text[*k]!='\'')
		{
			int lineStart, lineNo=get_lineNo(text, *k, &lineStart);
			log_error("Config", lineNo+1, "col %d: Expected a closing quote, got code=%08X, followed by 0x%02X-%02X-%02X", *k-lineStart, code, text[*k], text[*k+1], text[*k+2]);
			log_error("Config", lineNo+1, "col %d: ...can't read character literal %.*s", *k-lineStart, k0+50<text_len?50:text_len-k0, text+k0);
			//LOGE("Config(%d:%d): Expected a closing quote", lineNo+1, *k-lineStart);
			return 0;
		}
		++*k;
		break;
	case '0':
		++*k;
		if((text[*k]&0xDF)=='X')
		{
			++*k;
			code=(int)read_int(text, text_len, k, 16, 0);
		}
		else
			code=(int)read_int(text, text_len, k, 8, 0);
		break;
	default:
		if(text[*k]<'1'||text[*k]>'9')
			code=find_codepoint(text, text_len, k);
		else
			code=(int)read_int(text, text_len, k, 10, 0);
		break;
	}
	return code;
}
static const char
	kw_layout[]="layout",

	kw_lang[]="lang",
	kw_symbols[]="symbols",
	kw_numPad[]="numpad", kw_decNumPad[]="decnumpad",

	kw_portrait[]="portrait", kw_landscape[]="landscape",

	kw_theme[]="theme",
	*kw_colors[]=
{
	"background",

	"button_idle",
	"button_fast",
	"button_pressed",

	"labels",

	"shadow_modifier",
	"shadow_letter",
	"shadow_non_letter",

	"preview_popup",
};
int 		parse_state(const char *cText, size_t text_len, Context *ctx0, ArrayHandle *aText, int color_value, int color_idx)
{
	int store_theme;
	const char *text;
	Context dummy_ctx={0}, *ctx;
	int k, len;
	Layout *layout;
	ArrayHandle *rows;
	Row *row;
	float *layout_height;
	Button *button;
	int numPadAppeared, decNumPadAppeared, symbolsAppeared;

	if(cText)
		store_theme=0, text=cText, ctx=ctx0;
	else if(aText)
		store_theme=1, text=(char*)aText[0]->data, text_len=aText[0]->count, ctx=&dummy_ctx;
	else
	{
		LOG_ERROR("parse_state(): both pointers are nullptr");
		return 0;
	}
	if(ctx->layouts)
	{
		LOG_ERROR("Possible memory leak: ctx->layouts == %p", ctx->layouts);
		free_context(ctx);
	}
	ARRAY_ALLOC(Layout, ctx->layouts, 0, 0);
	for(k=0, numPadAppeared=0, decNumPadAppeared=0, symbolsAppeared=0;;)
	{
		if(skip_ws(text, text_len, &k))
			break;
		if((len=memCmp_ascii_ci(text+k, kw_lang)))		//default language declaration
		{
			k+=len;
			if(skip_ws(text, text_len, &k))
				return parse_error(text, k, "Expected default language");
			if(!parse_langName(text, text_len, &k, &ctx->defaultLang))
				return 0;
			continue;
		}
		if((len=memCmp_ascii_ci(text+k, kw_theme)))		//theme declaration
		{
			int lineNo, lineStart;
#ifdef DEBUG_COLORS
			lineNo=get_lineNo(text, k, &lineStart);
			log_error("config", lineNo, "col %d: theme block: %.*s", k-lineStart, 50, text+k);
#endif
			char set_flags[THEME_COLOR_COUNT]={0};
			k+=len;
			if(skip_ws(text, text_len, &k)||text[k]!='{')
				return parse_error(text, k, "Expected opening brace \'{\' of theme declaration");
			++k;//skip '{'
			for(int k2=0;k2<THEME_COLOR_COUNT;++k2)
			{
				if(skip_ws(text, text_len, &k))
					return parse_error(text, k, "Expected a color keyword");
				int kw_idx=-1;
				for(int k3=0;k3<THEME_COLOR_COUNT;++k3)
				{
					if((len=memCmp_ascii_ci(text+k, kw_colors[k3])))
					{
						kw_idx=k3;
						break;
					}
				}
				if(kw_idx==-1)
					return parse_error(text, k, "Expected a color keyword");
				if(set_flags[kw_idx])
					return parse_error(text, k, "Duplicate color");
				k+=len;
				set_flags[kw_idx]=1;
				if(skip_ws(text, text_len, &k)||text[k]!='0'||(k+1<text_len&&(text[k+1]&0xDF)!='X'))
					return parse_error(text, k, "Expected a hex color value 0xAABBGGRR");
				k+=2;
				int nDigits=8;
				ctx->theme[kw_idx]=(int)read_int(text, text_len, &k, 16, &nDigits);//TODO parse decimal color quartet (eg: 255, 255, 255, 255)
				if(nDigits!=8)
					return parse_error(text, k, "Expected a hex color value 0xAABBGGRR");
				if(store_theme&&kw_idx==color_idx)
				{
					snprintf(g_buf, G_BUF_SIZE, "%08X", color_value);
					memcpy(aText[0]->data+k-8, g_buf, 8);
				}

#ifdef DEBUG_COLORS
				lineNo=get_lineNo(text, k, &lineStart);
				log_error("config", lineNo, "col %d: set %s = 0x%08X", k-lineStart, kw_colors[kw_idx], ctx->theme[kw_idx]);
#endif
			}
			for(int k2=0;k2<THEME_COLOR_COUNT;++k2)
			{
				if(!set_flags[k2])
				{
					lineNo=get_lineNo(text, k, &lineStart);
					if(store_theme&&k2==color_idx)
					{
						int printed=snprintf(g_buf, G_BUF_SIZE, "\n\t%s 0x%08X", kw_colors[k2], color_value);
						array_insert(aText, k, g_buf, printed, 1, 0);
						k+=printed;
						log_error("config", lineNo, "col %d: Missing color \'%s\', it was appended", k-lineStart, kw_colors[k2]);
					}
					else
					{
						ctx->theme[k2]=rand();//assumes RAND_MAX is 0x7FFFFFFF
						log_error("config", lineNo, "col %d: Missing color \'%s\', it was set to random color", k-lineStart, kw_colors[k2]);//TODO report syntax errors as toast or an overlay (better with multiple errors)
					}
				}
			}
			if(skip_ws(text, text_len, &k)||text[k]!='}')
				return parse_error(text, k, "Expected closing brace \'}\' of theme declaration");
			++k;//skip '}'
			continue;
		}
		len=memCmp_ascii_ci(text+k, kw_layout);			//new layout
#ifdef DEBUG_PARSER
		LOG_ERROR("k = %d", k);
#endif
		if(!len)
		{
			parse_error(text, k, "Expected a layout definition");
			return 0;
		}
		k+=len;
		if(skip_ws(text, text_len, &k))
			return parse_error(text, k, "Expected layout type");
		if((len=memCmp_ascii_ci(text+k, kw_lang)))//layout type is 'lang'
		{
			k+=len;
			layout=(Layout*)ARRAY_APPEND(ctx->layouts, 0, 1, 1, 0);
			if(!parse_langName(text, text_len, &k, &layout->lang))
				break;
			layout->type=LAYOUT_LANG;
		}
		else if((len=memCmp_ascii_ci(text+k, kw_symbols)))
		{
			if(symbolsAppeared)
				return parse_error(text, k, "Duplicate \'layout symbols\'");
			symbolsAppeared=1;
			k+=len;
			layout=&ctx->symbolsExtension;
			layout->type=LAYOUT_SYMBOLS_EXTENSION;
		}
	/*	else if((len=memCmp_ascii_ci(text+k, kw_url)))//layout type is 'url'
		{
			k+=len;
			layout->type=LAYOUT_URL;
			if(!parse_langName(text, text_len, &k, &layout->lang))
				break;
		}
		else if((len=memCmp_ascii_ci(text+k, kw_ascii)))//layout type is 'ascii'
		{
			k+=len;
			layout->type=LAYOUT_ASCII;
		}//*/
		else if((len=memCmp_ascii_ci(text+k, kw_numPad)))//layout type is 'numpad'
		{
			if(numPadAppeared)
				return parse_error(text, k, "Duplicate \'layout numpad\'");
			numPadAppeared=1;
			k+=len;
			layout=&ctx->numPad;
			//layout=(Layout*)ARRAY_APPEND(ctx->layouts, 0, 1, 1, 0);
			layout->type=LAYOUT_NUMPAD;
		}
		else if((len=memCmp_ascii_ci(text+k, kw_decNumPad)))//layout type is 'decnumpad'
		{
			if(decNumPadAppeared)
				return parse_error(text, k, "Duplicate \'layout decnumpad\'");
			decNumPadAppeared=1;
			k+=len;
			layout=&ctx->decNumPad;
			//layout=(Layout*)ARRAY_APPEND(ctx->layouts, 0, 1, 1, 0);
			layout->type=LAYOUT_DECNUMPAD;
		}
		else//layout must be one of the previous types
			return parse_error(text, k, "Expected layout type");

		//check for duplicate layouts
		for(int k2=0, nLayouts=(int)ctx->layouts->count-1;k2<nLayouts;++k2)//for each previously parsed layout
		{
			Layout const *l2=(Layout const*)array_at(&ctx->layouts, k2);
			if(layout->type==l2->type)
			{
				if(layout->type==LAYOUT_LANG)
				{
#ifdef DEBUG_STRCMP
					LOG_ERROR("comparing arrays %p and %p", l2->lang, layout->lang);
#endif
					if(!strcmp((char*)l2->lang->data, (char*)layout->lang->data))
					{
						int lineStart, lineNo=get_lineNo(text, k, &lineStart);
						log_error("Config", lineNo+1, "col %d: Duplicate \'layout lang %s\'", k-lineStart, (char*)layout->lang->data);
						//LOGE("Config(%d): Duplicate \'layout %s %s\'", lineNo, layout->type==LAYOUT_LANG?kw_lang:kw_url, (char*)layout->lang->data);
						//return 0;
					}
				}
				else
				{
					int lineStart, lineNo=get_lineNo(text, k, &lineStart);
					const char *a;
					switch(layout->type)
					{
					case LAYOUT_SYMBOLS_EXTENSION:	a="symbols";	break;
					case LAYOUT_NUMPAD:				a="numpad";		break;
					case LAYOUT_DECNUMPAD:			a="decnumpad";	break;
					default:						a="<unknown>";	break;
					}
					log_error("Config", lineNo+1, "col %d: Duplicate \'layout %s\'", k-lineStart, a);
					//LOGE("Config(%d): Duplicate \'layout %s\'", lineNo, a);
				}
			}
		}

		if(skip_ws(text, text_len, &k))
			return parse_error(text, k, "Expected portrait or landscape");
		for(int k2=0;k2<2;++k2)//for portrait & landscape declarations
		{
			if((len=memCmp_ascii_ci(text+k, kw_portrait)))
			{
				k+=len;
				rows=&layout->portrait;
				layout_height=&layout->p_percent;
			}
			else if((len=memCmp_ascii_ci(text+k, kw_landscape)))
			{
				k+=len;
				rows=&layout->landscape;
				layout_height=&layout->l_percent;
			}
			else
				return parse_error(text, k, "Expected portrait or landscape declaration");
			if(*rows)
			{
				LOG_ERROR("Error: Expected nullptr, got %p", *rows);
				if(rows==&layout->portrait)
					return parse_error(text, k, "Portrait declaration appeared before");
				return parse_error(text, k, "Landscape declaration appeared before");
			}

			if(layout==&ctx->symbolsExtension)
				*layout_height=0.5f;
			else
			{
				if(skip_ws(text, text_len, &k))
					return parse_error(text, k, "Expected layout height");
				if(read_fraction(text, text_len, &k, layout_height)!=1)
					return parse_error(text, k, "Expected layout definition");
				if(*layout_height<=0||*layout_height>1)
				{
					LOG_ERROR("layout height = %f", *layout_height);
					return parse_error(text, k, "Layout height should be between 0 and 1");
				}
			}

			if(skip_ws(text, text_len, &k)||text[k]!='{')
				return parse_error(text, k, "Expected layout definition");
			++k;//skip '{'
			if(skip_ws(text, text_len, &k))
				return parse_error(text, k, "Expected layout definition");
			ARRAY_ALLOC(Row, *rows, 0, 0);
			for(;k<(int)text_len;)//for each row
			{
				if(text[k]=='}')
				{
					if(!rows[0]->count)
						return parse_error(text, k, "Layout is empty");
					break;
				}
				row=(Row*)ARRAY_APPEND(*rows, 0, 1, 1, 0);
				ARRAY_ALLOC(Button, row->buttons, 0, 0);
				for(;k<(int)text_len;)//for each button
				{
					if(skip_ws(text, text_len, &k))
						break;
					button=ARRAY_APPEND(row->buttons, 0, 1, 1, 0);
					button->code=read_codepoint(text, text_len, &k);
					if(button->code==-1)
						return 0;
					if(skip_ws(text, text_len, &k))
						return parse_error(text, k, "Expected button width");
					if(read_fraction(text, text_len, &k, &button->relativeWidth)!=1)
						return parse_error(text, k, "Expected semicolon and closing brace");
					if(!button->relativeWidth)
						return parse_error(text, k, "Button width is zero");
					if(text[k]==',')
					{
						++k;
						continue;
					}
					if(text[k]==';')
					{
						++k;
						break;
					}
				}
				if(skip_ws(text, text_len, &k))
					return parse_error(text, k, "Expected a closing brace");
				if(text[k]=='}')
				{
					++k;
					break;
				}
			}
			if(skip_ws(text, text_len, &k))
				break;
		}
	}

	if(!symbolsAppeared)
		return parse_error(text, (int)text_len-1, "Missing extension \'layout symbols\'");
	if(!decNumPadAppeared)
		return parse_error(text, (int)text_len-1, "Missing \'layout decnumpad\'");
	if(!numPadAppeared)
		return parse_error(text, (int)text_len-1, "Missing \'layout numpad\'");
	//if(ctx->symbolsExtension.type!=LAYOUT_SYMBOLS_EXTENSION||!ctx->symbolsExtension.portrait||!ctx->symbolsExtension.landscape)
	//	return parse_error(text, (int)text_len-1, "Missing \'layout symbols{...}\'");
	//if(ctx->numPad.type!=LAYOUT_SYMBOLS_EXTENSION||!ctx->numPad.portrait||!ctx->numPad.landscape)
	//	return parse_error(text, (int)text_len-1, "Missing \'layout numpad{...}\'");
	//if(ctx->decNumPad.type!=LAYOUT_SYMBOLS_EXTENSION||!ctx->decNumPad.portrait||!ctx->decNumPad.landscape)
	//	return parse_error(text, (int)text_len-1, "Missing \'layout decnumpad{...}\'");

	if(!ctx->defaultLang)
		return parse_error(text, (int)text_len-1, "Missing default language declaration, for example: \'lang en\'");
	int nLayouts=(int)ctx->layouts->count, found_lang=0;
	for(int kl=0;kl<nLayouts;++kl)
	{
		layout=(Layout*)array_at(&ctx->layouts, kl);
#ifdef DEBUG_STRCMP
		LOG_ERROR("comparing arrays %p and %p", layout->lang, ctx->defaultLang);
#endif
		if(layout->type==LAYOUT_LANG&&!strcmp((char*)layout->lang->data, (char*)ctx->defaultLang->data))
		{
			found_lang=1;
			break;
		}
	}
	int lStart=0, lno=0;
	if(!found_lang)
	{
		lno=get_lineNo(text, (int)text_len-1, &lStart);
		log_error("Config", lno+1, "col %d: Missing \'layout lang %s\'", lStart, (char*)ctx->defaultLang->data);
	}
	//if(!found_url)
	//	log_error("Config", lno+1, "col %d: Missing \'layout url %s\'", lStart, (char*)ctx->defaultLang->data);
	if(store_theme)
		free_context(&dummy_ctx);
	return found_lang;
}

static int	calc_raster_sizes_rows(ArrayHandle rows, int width, int height, float kb_percent)
{
	int nRows, nButtons, kb_height;
	Row *row;

	if(kb_percent<=0||kb_percent>1)
		return 0;
	kb_height=(int)((float)height*kb_percent);
	nRows=(int)rows->count;
	for(int ky=0;ky<nRows;++ky)
	{
		row=(Row*)array_at(&rows, ky);
		row->y1=ky*kb_height/nRows;
		row->y2=(ky+1)*kb_height/nRows;
		nButtons=(int)row->buttons->count;
		float gain=0;
		for(int kx=0;kx<nButtons;++kx)
		{
			Button *button=(Button*)array_at(&row->buttons, kx);
			gain+=button->relativeWidth;
		}
		gain=(float)width/gain;
		float x1=0;
		for(int kx=0;kx<nButtons;++kx)
		{
			Button *button=(Button*)array_at(&row->buttons, kx);
			float x2=x1+button->relativeWidth;
			button->x1=(int)(x1*gain);
			button->x2=(int)(x2*gain);
			x1=x2;
		}
	}
	return 1;
}
int 		calc_raster_sizes(Context *ctx, int width, int height, int is_landscape)
{
	int ret=1;

	if(is_landscape)
		memswap_slow(&width, &height, sizeof(int));

	//calculate button raster sizes from relative sizes
	for(int kl=0;kl<(int)ctx->layouts->count;++kl)//for each layout
	{
		Layout *layout=(Layout*)array_at(&ctx->layouts, kl);
		ret&=calc_raster_sizes_rows(layout->portrait, width, height, layout->p_percent);
		ret&=calc_raster_sizes_rows(layout->landscape, height, width, layout->l_percent);
		if(ret)
		{
			layout->p_height=(int)((float)height*layout->p_percent);
			layout->l_height=(int)((float)width*layout->l_percent);
		}
	}
	ret&=calc_raster_sizes_rows(ctx->symbolsExtension.portrait, width, height, 0.5f);//height percentage is irrelevant here because the extension rows have same height as layout rows
	ret&=calc_raster_sizes_rows(ctx->symbolsExtension.landscape, height, width, 0.5f);
	return ret;
}
