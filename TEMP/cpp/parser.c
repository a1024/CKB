//
// Created by MSI on 7/3/2022.
//

#include"ckb.h"
#include<string.h>
#include<ctype.h>
#include<math.h>
static const char file[]=__FILE__;

int			get_lineno(ArrayConstHandle text, int k)
{
	int lineno=0;
	for(int k2=0;k2<k;lineno+=text->data[k2]=='\n', ++k2);
	return lineno;
}
int			parse_error(ArrayConstHandle text, int k, const char *msg)
{
	int lineno=get_lineno(text, k);
#ifdef _MSC_VER
	LOGE("Config error line %d: %s\n", lineno+1, msg);
#else
	LOGE("Config error line %d: %s", lineno+1, msg);
#endif
	++nerrors;
	return 0;
}
void 		parse_error_kw(ArrayConstHandle text, int k, const char *kw_expected)
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
				for(;*k<(int)text->count&&text->data[*k]!='\n';++*k);
				//*k+=*k<(int)text->count;//there is ++*k in loop header
			}
			else if(text->data[*k+1]=='*')//block comment
			{
				*k+=2;
				for(;*k+1<(int)text->count&&!(text->data[*k]=='*'&&text->data[*k+1]=='/');++*k);
				*k+=*k<(int)text->count&&text->data[*k]=='*';
				//*k+=*k<(int)text->count&&text->data[*k]=='/';//there is ++*k in loop header
			}
			continue;
		default:
			if(isspace(text->data[*k]))
				continue;
			break;
		}
		break;
	}
	return *k>=(int)text->count;
}
int 		acme_strcmp_ascii_ci(ArrayConstHandle text, int *k, const char *kw)//returns zero on match
{
	int match=0;
	for(int k2=0;kw[k2];++*k, ++k2)
	{
		int diff=tolower(text->data[*k])-tolower(kw[k2]);
		if(diff)
		{
			match=(diff>0)-(diff<0);
			break;
		}
	}
	return match;
}
int 		get_id(ArrayConstHandle text, int k)//returns length of identifier
{
	int end=k;

	if(end<(int)text->count&&(isalpha(text->data[end])||text->data[end]=='_'))
	{
		++end;
		for(;end<(int)text->count&&(isalnum(text->data[end])||text->data[end]=='_');++end);
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
	*k+=len;
	return 1;
}
long long 	read_int(ArrayConstHandle text, int *k, int base, int *ret_ndigits)//ret_ndigits: if a variable is provided, a non-zero digit limit can be provided
{
	long long val;
	int ndigits, limit, end;
	unsigned char c;

	val=0;
	ndigits=0;
	limit=base<10?base:10;
	end=(int)text->count;
	if(ret_ndigits&&*ret_ndigits&&*k+*ret_ndigits<end)
		end=*k+*ret_ndigits;
	for(;*k<end;++*k, ++ndigits)
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
		ndigits=0;
		temp=read_int(text, k, 10, &ndigits);
		val+=(float)temp*powf(10, -(float)ndigits);
	}
	return val;
}
int			esc2codepoint(ArrayConstHandle text, int *k)//one escape sequence -> code point
{
	int code, ndigits;

	if(text->data[*k]!='\\')
	{
		code=text->data[*k];
		++*k;
		return code;
	}
	++*k;
	switch(text->data[*k])
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
		code=(int)read_int(text, k, 8, &ndigits);
		break;
	case 'x':
		++*k;
		ndigits=0;//unlimited
		code=(int)read_int(text, k, 16, &ndigits);
		break;
	case 'u'://4 hex
		++*k;
		ndigits=4;
		code=(int)read_int(text, k, 16, &ndigits);
		break;
	case 'U'://8 hex
		++*k;
		ndigits=8;
		code=(int)read_int(text, k, 16, &ndigits);
		break;
	default:
		LOGE("Invalid escape sequence: %.*s", 3, text->data+*k-1);
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
int			kwmap_cmp(const void *left, const void *right)
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
	return ac<bc;
}
#ifdef _MSC_VER
void		debug_print_keywords()
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
void		kwmap_init(ArrayHandle *kwmap)
{
	const int nkw=MKEY_COUNT;
//	const int nkw=SIZEOF(mcodes);

	ARRAY_ALLOC(Keyword, *kwmap, nkw, 0);
	for(int k=0;k<nkw;++k)
	{
		Keyword *kw=array_at(kwmap, k);
		kw->code=MODMASK|k;
		kw->kw=mcodes[k];
	}
	//debug_print_keywords();//
#if 0
	qsort(kwmap[0]->data, nkw, sizeof(Keyword), kwmap_cmp);//BROKEN
#else
	for(int k=0;k<nkw-1;++k)//insertion sort
	{
		Keyword *left=(Keyword*)array_at(kwmap, k), *right;
		for(int k2=k+1;k2<nkw;++k2)
		{
			right=(Keyword*)array_at(kwmap, k2);
			if(kwmap_cmp(right, left))
				memswap(left, right, sizeof(Keyword));
		}
	}
#endif
	//debug_print_keywords();//
}
int			find_codepoint(ArrayConstHandle text, int *k, ArrayConstHandle kwmap)
{
	int L, R, mid;

	L=0, R=(int)kwmap->count-1;
	while(L<=R)
	{
		mid=(L+R)>>1;
		int k2=*k;
		Keyword const *kw=(Keyword const*)array_at_const(&kwmap, mid);
		int ret=acme_strcmp_ascii_ci(text, &k2, kw->kw);
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
int			read_codepoint(ArrayConstHandle text, int *k)
{
	int code;

	if(!kwmap)
		kwmap_init(&kwmap);

	switch(text->data[*k])
	{
	case '\'':
		++*k;
		code=esc2codepoint(text, k);
		if(text->data[*k]!='\'')
		{
			int lineno=get_lineno(text, *k);
			LOGE("Config error line %d: Expected a closing quote", lineno);
			return 0;
		}
		++*k;
		break;
	case '0':
		++*k;
		if((text->data[*k]&0xDF)=='X')
		{
			++*k;
			code=(int)read_int(text, k, 16, 0);
		}
		else
			code=(int)read_int(text, k, 8, 0);
		break;
	default:
		if(text->data[*k]<'1'||text->data[*k]>'9')
			code=find_codepoint(text, k, kwmap);
		else
			code=(int)read_int(text, k, 10, 0);
		break;
	}
	return code;
}
int 		parse_state(ArrayConstHandle text, Context *ctx)
{
	const char
		kw_layout[]="layout",
		kw_lang[]="lang", kw_url[]="url", kw_ascii[]="ascii",
		kw_numpad[]="numpad", kw_decnumpad[]="decnumpad",

		kw_portrait[]="portrait", kw_landscape[]="landscape";

	int k, len;
	Layout *layout;
	ArrayHandle *rows, *buttons;
	float *layout_height;
	ButtonInfo *button;

	if(ctx->layouts)
		LOG_ERROR("Possible memory leak: cts->layouts == %p", ctx->layouts);
	ARRAY_ALLOC(Layout, ctx->layouts, 0, 0);
	for(k=0;;)
	{
		if(skip_ws(text, &k))
			break;
		if(acme_strcmp_ascii_ci(text, &k, kw_layout))
		{
			parse_error_kw(text, k, kw_layout);
			return 0;
		}
		if(skip_ws(text, &k))
			return parse_error(text, k, "Expected layout type");
		layout=(Layout*)ARRAY_APPEND(ctx->layouts, 0, 1, 1, 0);
		if(len=memcmp_ascii_ci((char*)text->data+k, kw_lang))
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
			return parse_error(text, k, "Expected layout type");
		if(skip_ws(text, &k))
			return parse_error(text, k, "Expected portrait or landscape declarations");
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
				return parse_error(text, k, "Expected portrait or landscape declaration");
			if(*rows)
				return parse_error(text, k, "Expected portrait or landscape declaration appeared before");
			if(skip_ws(text, &k))
				return parse_error(text, k, "Expected layout height");
			*layout_height=read_float(text, &k);
			if(skip_ws(text, &k)||text->data[k]!='{')
				return parse_error(text, k, "Expected layout definition");
			++k;//skip '{'
			if(skip_ws(text, &k))
				return parse_error(text, k, "Expected layout definition");
			ARRAY_ALLOC(ArrayHandle, *rows, 0, 0);
			for(;k<(int)text->count;)//for each row
			{
				if(text->data[k]=='}')
				{
					if(!rows[0]->count)
						return parse_error(text, k, "Layout is empty");
					break;
				}
				buttons=(ArrayHandle*)ARRAY_APPEND(*rows, 0, 1, 1, 0);
				ARRAY_ALLOC(ButtonInfo, *buttons, 0, 0);
				for(;k<(int)text->count;)//for each button
				{
					if(skip_ws(text, &k))
						break;
					button=ARRAY_APPEND(*buttons, 0, 1, 1, 0);
					button->code=read_codepoint(text, &k);
					if(button->code==-1)
						return 0;
					if(skip_ws(text, &k))
						return parse_error(text, k, "Expected button width");
					button->width=read_float(text, &k);
					if(skip_ws(text, &k))
						return parse_error(text, k, "Expected semicolon and closing brace");
					if(text->data[k]==',')
					{
						++k;
						continue;
					}
					if(text->data[k]==';')
					{
						++k;
						break;
					}
				}
				if(skip_ws(text, &k))
					return parse_error(text, k, "Expected a closing brace");
				if(text->data[k]=='}')
				{
					++k;
					break;
				}
			}
			if(skip_ws(text, &k))
				break;
		}
	}

	return 1;
}