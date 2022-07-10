//
// Created by MSI on 2022-07-03.
//

#include"ckb.h"
#include<stdlib.h>
#include<string.h>
#include<ctype.h>
#include<math.h>
static const char file[]=__FILE__;

static int	get_lineno(const char *text, int k)
{
	int lineno=0;
	for(int k2=0;k2<k;lineno+=text[k2]=='\n', ++k2);
	return lineno;
}
static int	parse_error(const char *text, int k, const char *msg)
{
	int lineno=get_lineno(text, k);
#ifdef _MSC_VER
	LOGE("Config error line %d: %s\n", lineno+1, msg);
#else
	log_error("config", lineno+1, "%s", msg);
#endif
	++nerrors;
	return 0;
}
static int	skip_ws(const char *text, size_t text_len, int *k)
{
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
	return *k>=(int)text_len;
}
static int	acme_strcmp_ascii_ci(const char *text, size_t text_len, int *k, const char *kw)//returns zero on match, otherwise which string comes first alphabetically
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
static int	memcmp_ascii_ci(const char *text, const char *kw)//returns length of keyword on match, otherwise zero
{
	int k=0;
	for(;*text&&*kw&&tolower(*text)==tolower(*kw);++text, ++kw, ++k);
	if(*text&&*kw)
		return 0;
	return k;
}
static int	parse_langname(const char *text, size_t text_len, int *k, ArrayHandle *lang)
{
	if(skip_ws(text, text_len, k))
		return parse_error(text, *k, "Expected language type");
	int len=get_id(text, text_len, *k);
	STR_ALLOC(*lang, len);
	memcpy(lang[0]->data, text+*k, len);
	*k+=len;
	return 1;
}
static long long read_int(const char *text, size_t text_len, int *k, int base, int *ret_ndigits)//ret_ndigits: if a variable is provided, a non-zero digit limit can be provided
{
	long long val;
	int ndigits, limit, end;
	unsigned char c;

	val=0;
	ndigits=0;
	limit=base<10?base:10;
	end=(int)text_len;
	if(ret_ndigits&&*ret_ndigits&&*k+*ret_ndigits<end)
		end=*k+*ret_ndigits;
	for(;*k<end;++*k, ++ndigits)
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
	if(ret_ndigits)
		*ret_ndigits=ndigits;
	return val;
}
static float read_float(const char *text, size_t text_len, int *k)
{
	float val;
	long long temp;
	int ndigits;

	val=(float)read_int(text, text_len, k, 10, 0);
	if(text[*k]=='.')
	{
		++*k;
		ndigits=0;
		temp=read_int(text, text_len, k, 10, &ndigits);
		val+=(float)temp*powf(10, -(float)ndigits);
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
static int	esc2codepoint(const char *text, size_t text_len, int *k)//one escape sequence -> code point
{
	int code, ndigits;

	if(text[*k]!='\\')
	{
		code=(unsigned char)text[*k];
		++*k;
		return code;
	}
	++*k;
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

	case '0':case '1':case '2':case '3':case '4':case '5':case '6':case '7'://3 octals
		ndigits=3;
		code=(int)read_int(text, text_len, k, 8, &ndigits);
		break;
	case 'x':
		++*k;
		ndigits=0;//unlimited
		code=(int)read_int(text, text_len, k, 16, &ndigits);
		break;
	case 'u'://4 hex
		++*k;
		ndigits=4;
		code=(int)read_int(text, text_len, k, 16, &ndigits);
		break;
	case 'U'://8 hex
		++*k;
		ndigits=8;
		code=(int)read_int(text, text_len, k, 16, &ndigits);
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
static ArrayHandle kwmap=0;
static int	kwmap_cmp(const void *left, const void *right)
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
	for(int k=0;k<(int)kwmap->count;++k)
	{
		Keyword const *kw=(Keyword const*)array_at_const(&kwmap, k);
		printf("%3d\t%s\n", k, kw->kw);
	}
	printf("\n");
}
#endif
static void	kwmap_init(ArrayHandle *kwmap)
{
	ARRAY_ALLOC(Keyword, *kwmap, MKEY_COUNT, 0);
	for(int k=0;k<MKEY_COUNT;++k)
	{
		Keyword *kw=array_at(kwmap, k);
		kw->code=MODMASK|k;
		kw->kw=mcodes[k];
	}
#ifdef _MSC_VER
	debug_print_keywords();//
#endif
#if 0
	qsort(kwmap[0]->data, MKEY_COUNT, sizeof(Keyword), kwmap_cmp);//BROKEN
#else
	isort(kwmap[0]->data, kwmap[0]->count, kwmap[0]->esize, kwmap_cmp);
#endif
#ifdef _MSC_VER
	debug_print_keywords();//
#endif
}
static int	find_codepoint(const char *text, size_t text_len, int *k, ArrayConstHandle kwmap)
{
	int L, R, mid;

	L=0, R=(int)kwmap->count-1;
	while(L<=R)
	{
		mid=(L+R)>>1;
		int k2=*k;
		Keyword const *kw=(Keyword const*)array_at_const(&kwmap, mid);
		int ret=acme_strcmp_ascii_ci(text, text_len, &k2, kw->kw);
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
	int code;

	if(!kwmap)
		kwmap_init(&kwmap);

	switch(text[*k])
	{
	case '\'':
		++*k;
		code=esc2codepoint(text, text_len, k);
		if(text[*k]!='\'')
		{
			int lineno=get_lineno(text, *k);
			log_error("Config", lineno+1, "Expected a closing quote");
			//LOGE("Config error line %d: Expected a closing quote", lineno);
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
			code=find_codepoint(text, text_len, k, kwmap);
		else
			code=(int)read_int(text, text_len, k, 10, 0);
		break;
	}
	return code;
}
int 		parse_state(const char *text, size_t text_len, Context *ctx)
{
	const char
		kw_layout[]="layout",
		kw_lang[]="lang", kw_url[]="url", kw_ascii[]="ascii",
		kw_numpad[]="numpad", kw_decnumpad[]="decnumpad",

		kw_portrait[]="portrait", kw_landscape[]="landscape";

	int k, len;
	Layout *layout;
	ArrayHandle *rows;
	Row *row;
	float *layout_height;
	Button *button;

	if(ctx->layouts)
		LOG_ERROR("Possible memory leak: ctx->layouts == %p", ctx->layouts);
	ARRAY_ALLOC(Layout, ctx->layouts, 0, 0);
	for(k=0;;)
	{
		if(skip_ws(text, text_len, &k))
			break;
		if((len=memcmp_ascii_ci(text+k, kw_lang)))//default language declaration
		{
			k+=len;
			if(skip_ws(text, text_len, &k))
				return parse_error(text, k, "Expected default language");
			if(!parse_langname(text, text_len, &k, &ctx->defaultlang))
				return 0;
			continue;
		}
		len=memcmp_ascii_ci(text+k, kw_layout);			//new layout
		if(!len)
		{
			parse_error(text, k, "Expected a layout definition");
			return 0;
		}
		k+=len;
		if(skip_ws(text, text_len, &k))
			return parse_error(text, k, "Expected layout type");
		layout=(Layout*)ARRAY_APPEND(ctx->layouts, 0, 1, 1, 0);
		if((len=memcmp_ascii_ci(text+k, kw_lang)))//layout type is 'lang'
		{
			k+=len;
			layout->type=LAYOUT_LANG;
			if(!parse_langname(text, text_len, &k, &layout->lang))
				break;
		}
		else if((len=memcmp_ascii_ci(text+k, kw_url)))//layout type is 'url'
		{
			k+=len;
			layout->type=LAYOUT_URL;
			if(!parse_langname(text, text_len, &k, &layout->lang))
				break;
		}
		else if((len=memcmp_ascii_ci(text+k, kw_ascii)))//layout type is 'ascii'
		{
			k+=len;
			layout->type=LAYOUT_ASCII;
		}
		else if((len=memcmp_ascii_ci(text+k, kw_numpad)))//layout type is 'numpad'
		{
			k+=len;
			layout->type=LAYOUT_NUMPAD;
		}
		else if((len=memcmp_ascii_ci(text+k, kw_decnumpad)))//layout type is 'decnumpad'
		{
			k+=len;
			layout->type=LAYOUT_DECNUMPAD;
		}
		else//layout must be one of the previous types
			return parse_error(text, k, "Expected layout type");

		for(int k2=0, nlayouts=(int)ctx->layouts->count-1;k2<nlayouts;++k2)			//check for duplicate layouts
		{
			Layout const *l2=(Layout const*)array_at(&ctx->layouts, k2);
			if(layout->type==l2->type)
			{
				if((layout->type==LAYOUT_LANG||layout->type==LAYOUT_URL))
				{
					if(!strcmp((char*)l2->lang->data, (char*)layout->lang->data))
					{
						int lineno=get_lineno(text, k);
						log_error("Config", lineno+1, "Duplicate \'layout %s %s\'", layout->type==LAYOUT_LANG?kw_lang:kw_url, (char*)layout->lang->data);
						//LOGE("Config(%d): Duplicate \'layout %s %s\'", lineno, layout->type==LAYOUT_LANG?kw_lang:kw_url, (char*)layout->lang->data);
						//return 0;
					}
				}
				else
				{
					int lineno=get_lineno(text, k);
					const char *a;
					switch(layout->type)
					{
					case LAYOUT_ASCII:a="ASCII";break;
					case LAYOUT_NUMPAD:a="numpad";break;
					case LAYOUT_DECNUMPAD:a="decnumpad";break;
					default:a="<unknown>";break;
					}
					log_error("Config", lineno+1, "Duplicate \'layout %s\'", a);
					//LOGE("Config(%d): Duplicate \'layout %s\'", lineno, a);
				}
			}
		}

		if(skip_ws(text, text_len, &k))
			return parse_error(text, k, "Expected portrait or landscape");
		for(int k2=0;k2<2;++k2)
		{
			if((len=memcmp_ascii_ci(text+k, kw_portrait)))
			{
				k+=len;
				rows=&layout->portrait;
				layout_height=&layout->p_percent;
			}
			else if((len=memcmp_ascii_ci(text+k, kw_landscape)))
			{
				k+=len;
				rows=&layout->landscape;
				layout_height=&layout->l_percent;
			}
			else
				return parse_error(text, k, "Expected portrait or landscape declaration");
			if(*rows)
				return parse_error(text, k, "Expected portrait or landscape declaration appeared before");

			if(skip_ws(text, text_len, &k))
				return parse_error(text, k, "Expected layout height");
			if(read_fraction(text, text_len, &k, layout_height)!=1)
				return parse_error(text, k, "Expected layout definition");
			if(*layout_height<=0||*layout_height>1)
				return parse_error(text, k, "Layout height should be between 0 and 1");

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
	if(!ctx->defaultlang)
		return parse_error(text, (int)text_len-1, "Missing default language declaration, for example: \'lang en\'");
	int nlayouts=(int)ctx->layouts->count, found_lang=0, found_url=0;
	for(int kl=0;kl<nlayouts;++kl)
	{
		layout=(Layout*)array_at(&ctx->layouts, kl);
		if((layout->type==LAYOUT_LANG||layout->type==LAYOUT_URL)&&!strcmp((char*)layout->lang->data, (char*)ctx->defaultlang->data))
		{
			if(layout->type==LAYOUT_LANG)
				found_lang=1;
			else
				found_url=1;
		}
	}
	int lno=0;
	if(!found_lang||!found_url)
		lno=get_lineno(text, (int)text_len-1);
	if(!found_lang)
		log_error("Config", lno+1, "Missing \'layout lang %s\'", (char*)ctx->defaultlang->data);
	if(!found_url)
		log_error("Config", lno+1, "Missing \'layout url %s\'", (char*)ctx->defaultlang->data);
	return found_lang&&found_url;
}

static int	calc_raster_sizes_rows(ArrayHandle rows, int width, int height, float kb_percent)
{
	int nrows, nbuttons, kb_height;
	Row *row;

	if(kb_percent<=0||kb_percent>1)
		return 0;
	kb_height=(int)((float)height*kb_percent);
	nrows=(int)rows->count;
	for(int ky=0;ky<nrows;++ky)
	{
		row=(Row*)array_at(&rows, ky);
		row->y1=ky*kb_height/nrows;
		row->y2=(ky+1)*kb_height/nrows;
		nbuttons=(int)row->buttons->count;
		float gain=0;
		for(int kx=0;kx<nbuttons;++kx)
		{
			Button *button=(Button*)array_at(&row->buttons, kx);
			gain+=button->relativeWidth;
		}
		gain=(float)width/gain;
		float x1=0;
		for(int kx=0;kx<nbuttons;++kx)
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
	return ret;
}
