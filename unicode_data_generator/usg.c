//#include"array.h"
#include"unicode_search.h"
#include<stdio.h>
#include<sys/stat.h>
#include<ctype.h>
#include<stdarg.h>
#include<string.h>
#include<stdlib.h>
#ifdef _MSC_VER
#define WIN32_LEAN_AND_MEAN
#include<Windows.h>
#endif
static const char file[]=__FILE__;
#define 	G_BUF_SIZE	1024
char 		g_buf[G_BUF_SIZE]={0};
int			nErrors=0;
#ifdef _MSC_VER
void		set_console_buffer_size(short w, short h)
{
	COORD coords={w, h};
	void *handle=GetStdHandle(STD_OUTPUT_HANDLE);
	int success=SetConsoleScreenBufferSize(handle, coords);
	if(!success)
		printf("Failed to resize console buffer: %d\n\n", GetLastError());
}
#else
#define		set_console_buffer_size(...)
#endif
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
#ifdef DEBUG_ERROR
	LOGE("errors == %p", errors);//
#endif

	if(!errors)
		ARRAY_ALLOC(ArrayHandle, errors, 0, 0);
	ArrayHandle *e=(ArrayHandle*)ARRAY_APPEND(errors, 0, 1, 1, 0);
	STR_ALLOC(*e, 0);

#ifdef DEBUG_ERROR
	LOGE("e == %p", e);//
	if(e)//
		LOGE("*e == %p", *e);//
#endif

	array_assign(e, g_buf, printed+1);
#endif

	++nErrors;
	return 0;
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
		//LOG_ERROR("Cannot read %s", filename);
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
int			pause()
{
	int k;

	printf("Enter 0 to continue: ");
	scanf("%d", &k);
	return k;
}

typedef struct CodeDescrStruct
{
	int code;
	ArrayHandle description;
} CodeDescr;
typedef struct QueryStruct
{
	ArrayHandle word, codes;
} Query;
void free_descr(void* data)
{
	CodeDescr *d=(CodeDescr*)data;

	array_free(&d->description, 0);
}
void free_query(void *data)
{
	Query *q=(Query*)data;

	array_free(&q->word, 0);
	array_free(&q->codes, 0);
}
static ArrayHandle db=0;//'Query' array
static ArrayHandle descriptions=0;//'CodeDescr' array

static ArrayHandle text=0;
static ptrdiff_t idx=0, lineno=0;

#if 0
int skip_ws()
{
	for(;idx<(ptrdiff_t)text->count;)
	{
		switch(text->data[idx])
		{
		case '<':
			if(idx+3<(ptrdiff_t)text->count&&text->data[idx+1]=='!'&&text->data[idx+2]=='-'&&text->data[idx+3]=='-')//comment
			{
				idx+=4;
				for(;idx<(ptrdiff_t)text->count;)
				{
					if(idx+2<(ptrdiff_t)text->count&&text->data[idx]=='-'&&text->data[idx+1]=='-'&&text->data[idx+2]=='>')
					{
						idx+=3;
						break;
					}
					lineno+=text->data[idx]=='\n';
				}
			}
			continue;
		default:
			if(isspace(text->data[idx]))
			{
				++idx;
				lineno+=text->data[idx]=='\n';
				continue;
			}
			break;
		}
		break;
	}
	return idx>=(ptrdiff_t)text->count;
}
int expect(const char *label)
{
	ptrdiff_t k;

	for(k=idx;*label;++k, ++label)
		if(text->data[k]!=*label)
			return 0;
	idx=k;
	return 1;
}
int skip_till(const char *label)
{
	ptrdiff_t k, len, lineno2;

	k=idx;
	lineno2=lineno;
	len=strlen(label);
	for(;k<(ptrdiff_t)text->count;++k)
	{
		if(!memcmp(text->data+k, label, len))
		{
			k+=len;
			idx=k;
			lineno=lineno2;
			return 1;
		}
		lineno2+=text->data[idx]=='\n';
	}
	return 0;
}
#endif

int parse_error(const char *pos, const char *format, ...)
{
	const int count=200;

	va_list args;
	ptrdiff_t offset;

	offset=pos-(const char*)text->data;
	printf("Error at:\n%.*s\n", offset+count<(ptrdiff_t)text->count?count:text->count-offset);
	if(format)
	{
		va_start(args, format);
		vprintf(format, args);
		va_end(args);
		printf("\n\n");
	}
	return 0;
}
typedef struct RangeStruct
{
	const char *start, *end;
} Range, *RangeHandle;
typedef struct CodepointStruct
{
	int code;
	int cap, nnames;
	Range names[];
} Codepoint, *CodepointHandle;
CodepointHandle codepoint_alloc(int code)
{
	CodepointHandle ret;

	ret=(CodepointHandle)malloc(sizeof(Codepoint));
	if(!ret)
		return 0;
	ret->cap=sizeof(Codepoint);
	ret->code=code;
	ret->nnames=0;
	return ret;
}
int range_threeway(const void *left, const void *right)
{
	Range const *r1=(Range const*)left, *r2=(Range const*)right;
	const char *L=r1->start, *R=r2->start;

	for(;L<r1->end&&R<r2->end&&toupper(*L)==toupper(*R);++L, ++R);
	
	if(L>=r1->end&&R>=r2->end)
		return 0;

	char
		lc=L<r1->end?toupper(*L):127,
		rc=R<r2->end?toupper(*R):127;
	return (lc>rc)-(lc<rc);
}
RangeHandle codepoint_addrange(CodepointHandle *c, Range const *range)
{
	void *p;
	int bytesize;
	size_t idx;

	if(binary_search(c[0]->names, c[0]->nnames, sizeof(Range), range_threeway, range, &idx))
		return c[0]->names+idx;
	bytesize=sizeof(Codepoint)+(c[0]->nnames+1)*sizeof(Range);
	if(c[0]->cap<bytesize)
	{
		p=realloc(*c, bytesize);
		if(!p)
			return 0;
		*c=(CodepointHandle)p;
	}
	c[0]->names[c[0]->nnames]=*range;
	//c[0]->names[c[0]->nnames].start=start;
	//c[0]->names[c[0]->nnames].end=end;
	//if(range)
	//	c[0]->names[c[0]->nnames]=*range;
	//else
	//{
	//	c[0]->names[c[0]->nnames].start=0;
	//	c[0]->names[c[0]->nnames].end=0;
	//}
	++c[0]->nnames;
	return c[0]->names+c[0]->nnames-1;
}

const char* skip_till(const char *p, const char *end, const char *delim)
{
	const char *p2;

	for(;p<end;++p)//O(n^2)
		for(p2=delim;*p2;++p2)
			if(*p==*p2)
				return p;
	return p;
}
const char* skip(const char *p, const char *end, const char *delim)
{
	const char *p2;
	
found:
	if(p<end)
	{
		for(p2=delim;*p2;++p2)//O(n^2)
		{
			if(*p==*p2)
			{
				++p;
				goto found;
			}
		}
	}
	return p;
}
int query_threeway(const void *pair, const void *key)
{
	Query const *q=(const Query*)pair;
	Range const *r=(const Range*)key;
	const char *L=q->word->data, *R=r->start;
	
	for(;*L&&R<r->end&&*L==*R;++L, ++R);
	
	if(!*L&&R>=r->end)
		return 0;
	
	char lc=*L, rc=R<r->end?*R:127;
	if(!lc)
		lc=127;
	return (lc>rc)-(lc<rc);
}
int codepoint_threeway(const void *left, const void *right)
{
	const int *c1=(const int*)left, *c2=(const int *)right;

	return (*c1>*c2)-(*c1<*c2);
}
int descr_threeway(const void *left, const void *right)
{
	CodeDescr const *descr=(CodeDescr const*)left;
	const int *code=(const int*)right;

	return (descr->code>*code)-(descr->code<*code);
}
int	update_db(CodepointHandle code)
{
	//for each name
	//	for each word in name
	//		find word or its insertion idx in db using binary search
	//		possibly add new word to db		//BST?
	//		add unique codepoint to word

	static const char delim[]=" -\t\n\r";//word separators
	static const char desc_sep[]="; ";

	int k;
	size_t idx;
	RangeHandle range;
	Range r2;
	Query *q;
	CodeDescr *cd;

	for(k=0;k<code->nnames;++k)//for each name alias
	{
		range=code->names+k;
		for(r2.start=range->start;;r2.start=skip(r2.end, range->end, delim))//for each word in name
		{
			r2.end=skip_till(r2.start, range->end, delim);
			if(r2.start>=r2.end)
				break;
			if(!binary_search(db->data, db->count, db->esize, query_threeway, &r2, &idx))//word is new
			{
				q=array_insert(&db, idx, 0, 1, 1, 0);
				q->word=array_construct(r2.start, sizeof(char), r2.end-r2.start, 1, 1, __LINE__);
				q->codes=array_construct(&code->code, sizeof(int), 1, 1, 0, __LINE__);
			}
			else//word was already there
			{
				q=(Query*)array_at(&db, idx);
				if(!binary_search(q->codes->data, q->codes->count, q->codes->esize, codepoint_threeway, &code->code, &idx))//codepoint is new
					array_insert(&q->codes, idx, &code->code, 1, 1, 0);
			}
		}
	}

	if(binary_search(descriptions->data, descriptions->count, descriptions->esize, descr_threeway, &code->code, &idx))
		LOG_ERROR("code %08X, already exists at %d", code->code, idx);
	else
	{
		if(code->nnames>0)
		{
			cd=array_insert(&descriptions, idx, 0, 1, 1, 0);
			cd->code=code->code;
			cd->description=array_construct(code->names->start, sizeof(char), code->names->end-code->names->start, 1, 1, __LINE__);
			for(k=1;k<code->nnames;++k)
			{
				Range *r=code->names+k;
				STR_APPEND(cd->description, desc_sep, sizeof(desc_sep)-1, 1);
				STR_APPEND(cd->description, r->start, r->end-r->start, 1);
			}
		}
	}
	return 1;
}

int read_hex(const char **p)
{
	int val;
	unsigned char c;

	for(val=0;**p;++*p)
	{
		c=**p-'0';
		if(c<10)
		{
			val<<=4;
			val|=c;
		}
		else
		{
			c=(**p&0xDF)-'A';
			if(c>=6)
				break;
			val<<=4;
			val|=c+10;
		}
	}
	return val;
}

int strmatch(const char *str, const char *label)
{
	for(;*str&&*label&&*str==*label;++str, ++label);
	return !*label;
}
int strstrn(const char **str, const char **labels, int nlabels)//advances *str to first label found and returns label index
{
	const char *p;
	int k;

	for(p=*str;*p;++p)//O(strlen*labellen*nlabels)
	{
		for(k=0;k<nlabels;++k)
		{
			if(strmatch(p, labels[k]))
			{
				*str=p+strlen(labels[k]);
				return k;
			}
		}
	}
	return -1;
}
const char kw_char[]="<char", kw_endchar[]="</char>", kw_endtag[]="/>",
	kw_cp[]="cp=\"",
	kw_na[]="na=\"",
	kw_na1[]="na1=\"",
	kw_namealias[]="<name-alias alias=\"";
const char
	*search_cp[]={kw_cp, kw_endchar, kw_endtag},
	*search_na[]={kw_na, kw_na1, kw_endchar, kw_endtag},
	*search_alias[]={kw_namealias, kw_endchar, kw_endtag},
	*search_end[]={kw_endtag, kw_endchar};
int parse_ucd()
{
	const char *p, *p2;
	int idx, status;
	CodepointHandle code;
	Range r;
	RangeHandle range;
	
	status=1;
	code=codepoint_alloc(0);
	if(!code)
		return 0;
	for(p=(char*)text->data;p=strstr(p, kw_char);)
	{
		p+=sizeof(kw_char)-1;
		
		idx=strstrn(&p, search_cp, SIZEOF(search_cp));
		if(idx==-1)//missing end tag
		{
			status=parse_error(p, "Missing closing char tag");
			break;
		}
		if(idx||*p=='\"')//no codepoint
		{
			status=parse_error(p, "Missing codepoint tag");
			continue;
		}
		code->code=read_hex(&p);
		code->nnames=0;
		//if(code->code==0x1F602)//"face with tears of joy"
		//	code->code=0x1F602;//

		for(int k=0;k<2;++k)
		{
			idx=strstrn(&p, search_na, SIZEOF(search_na));
			if(idx==-1)//missing end tag
			{
				status=parse_error(p, "Missing closing char tag");
				break;
			}
			if(idx>1)//end of this char
				continue;
			if(*p=='\"')
				++p;
			else
			{
				if(!(p2=strchr(p, '\"')))
				{
					status=parse_error(p, "Missing closing double quote");
					goto exit;
				}
				r.start=p;
				r.end=p2;
				range=codepoint_addrange(&code, &r);
				p=p2+1;
			}
		}

		for(;;)
		{
			idx=strstrn(&p, search_alias, SIZEOF(search_alias));
			if(idx==-1)//missing end tag
			{
				status=parse_error(p, "Missing closing char tag");
				goto exit;
			}
			if(idx)//end of this char
				break;
			if(*p=='\"')//no alias
				++p;
			else
			{
				if(!(p2=strchr(p, '\"')))
				{
					status=parse_error(p, "Missing closing double quote");
					goto exit;
				}
				r.start=p;
				r.end=p2;
				range=codepoint_addrange(&code, &r);
				p=p2+1;
			}

			idx=strstrn(&p, search_end, SIZEOF(search_end));
			if(idx==-1)
			{
				status=parse_error(p, "Missing closing tag");
				goto exit;
			}
		}

		update_db(code);//codepoint is "ready"
#if 0
		if(!(p2=strstr(p, kw_cp)))//no codepoints in rest of file
			continue;
		p=p2+sizeof(kw_cp)-1;

		if(*p=='\"')
			continue;
		code->code=read_hex(&p);
		code->nranges=0;

		if(!(p2=strstr(p, kw_na)))//na not even mentioned in rest of file
			continue;
		p=p2+sizeof(kw_na)-1;
		if(*p!='\"')
		{
			range=codepoint_addrange(&code, 0);
			range->start=p;
			for(;*p&&*p!='\"';++p);
			range->end=p;
		}
#endif
	}
exit:
	free(code);
	return status;
}
#if 0
int parse_xml_opentag(Range *ret)
{
	Range tag;

	if(!expect("<"))
		return LOG_ERROR("Invalid xml open tag, expected \'<\'");
	++idx;
	tag.start=idx;
	for(;idx<text->count&&isalpha(text->data[idx]);++idx);
	tag.end=idx;
	if(ret)
		*ret=tag;
	if(!skip_till(">"))
		return LOG_ERROR("Expected \'...>\'");
	return 1;
}
int parse_xml()
{
	idx=0;

	if(skip_ws())
		return LOG_ERROR("No data in file");
	if(!expect("<?xml"))
		return LOG_ERROR("Expected \'<?xml...\'");
	if(!skip_till("?>"))
		return LOG_ERROR("Expected \'...?>\'");

	if(skip_ws())
		return LOG_ERROR("No data in file");
	Range tag={0}; 
	if(!parse_xml_opentag(&tag))
		return 0;
	
	if(skip_ws())
		return LOG_ERROR("Unexpected end of file");
}
#endif

int main(int argc, char **argv)
{
#if 1
	printf("Unicode search\n");
	for(;;)
	{
		printf("\nQuery: ");
		scanf("%s", g_buf);
		ArrayHandle results=unicode_search(g_buf);

		printf("%d results\n", results->count);
		for(int k=0;k<(int)results->count;++k)
		{
			CodeRank *cr=(CodeRank*)array_at(&results, k);
			printf("%3d\t\\U%08X\n", cr->rank, cr->code);
		}
	}
#endif
#if 0
	set_console_buffer_size(80, 4096);
	printf(
		"UnicodeSearch\n\n"
		"This program only parses the \'ucd.all.flat.xml\' file available at:\n"
		"\tftp://ftp.unicode.org/\n\n"
		);
	if(argc!=4)
	{
		printf("Usage:\n  program  input  output  output2\n");
		return 1;
	}
	text=load_text(argv[1]);
	if(!text)
	{
		LOG_ERROR("Cannot open %s", argv[1]);
		return 1;
	}

	ARRAY_ALLOC(Query, db, 0, 0);
	ARRAY_ALLOC(CodeDescr, descriptions, 0, 0);
	int result=parse_ucd();
	if(!result)
		printf("Parse error");
	array_free(&text, 0);

	//preview result
	Query *q;
	ptrdiff_t count=(ptrdiff_t)db->count;
	if(count>50)
		count=50;
	printf("DB has %d words, first %d words:\n", db->count, count);
	for(ptrdiff_t k=0;k<count;++k)
	{
		q=(Query*)array_at(&db, k);
		printf("%s:", (char*)q->word->data);
		for(int k2=0;k2<(int)q->codes->count;++k2)
		{
			int *code=(int*)array_at(&q->codes, k2);
			printf(" \\U%08X,", *code);
		}
		printf("\n");
	}

	//print db to string:
	//	const char *queries[]={...};
	//	int allcodes[]={...};
	//	int indices[]={...};
	STR_ALLOC(text, 0);
	const char header_queries1[]="const char *queries[]=\n{\n", header_queries2[]="};\n";
	STR_APPEND(text, header_queries1, sizeof(header_queries1)-1, 1);
	for(ptrdiff_t k=0;k<(ptrdiff_t)db->count;++k)			//print queries array
	{
		q=(Query*)array_at(&db, k);
		int printed=snprintf(g_buf, G_BUF_SIZE, "\t\"%s\",\n", (char*)q->word->data);
		STR_APPEND(text, g_buf, printed, 1);
	}
	STR_APPEND(text, header_queries2, sizeof(header_queries2)-1, 1);

	const char header_codes1[]="int allcodes[]=\n{\n", header_codes2[]="};\n";
	STR_APPEND(text, header_codes1, sizeof(header_codes1)-1, 1);
	for(ptrdiff_t k=0;k<(ptrdiff_t)db->count;++k)			//print codepoints array
	{
		q=(Query*)array_at(&db, k);
		STR_APPEND(text, "\t", 1, 1);
		for(int k2=0;k2<(int)q->codes->count;++k2)
		{
			int *code=(int*)array_at(&q->codes, k2);
			int printed=snprintf(g_buf, G_BUF_SIZE, "0x%X,%c", *code, k2+1<(int)q->codes->count?' ':'\n');
			STR_APPEND(text, g_buf, printed, 1);
		}
	}
	STR_APPEND(text, header_codes2, sizeof(header_codes2)-1, 1);

	const char header_indices1[]="int indices[]=\n{\n", header_indices2[]="\n};\n";
	STR_APPEND(text, header_indices1, sizeof(header_indices1)-1, 1);
	size_t idx=0;
	for(ptrdiff_t k=0;k<(ptrdiff_t)db->count;++k)			//print indices array
	{
		q=(Query*)array_at(&db, k);
		if(!(k&15))
			STR_APPEND(text, "\t", 1, 1);
		int printed=snprintf(g_buf, G_BUF_SIZE, "%d,%c", idx, (k+1)&15?' ':'\n');
		STR_APPEND(text, g_buf, printed, 1);
		idx+=q->codes->count;
	}
	STR_APPEND(text, header_indices2, sizeof(header_indices2)-1, 1);
	printf("allcodes array size: %d codes\n", idx);
	
	array_free(&db, free_query);

	int success;
	success=save_text(argv[2], text->data, text->count);//save as argv[2]
	array_free(&text, 0);


	STR_ALLOC(text, 0);

	const char header_descr1[]="CodeDescr descriptions[]=\n{\n", header_descr2[]="};\n";
	STR_APPEND(text, header_descr1, sizeof(header_descr1)-1, 1);
	for(ptrdiff_t k=0;k<(ptrdiff_t)descriptions->count;++k)	//print descriptions
	{
		CodeDescr *descr=(CodeDescr*)array_at(&descriptions, k);
		int printed=snprintf(g_buf, G_BUF_SIZE, "\t{0x%X, \"%s\"},\n", descr->code, (char*)descr->description->data);
		STR_APPEND(text, g_buf, printed, 1);
	}
	STR_APPEND(text, header_descr2, sizeof(header_descr2)-1, 1);
	printf("%d unique codes\n", idx);

	array_free(&descriptions, free_descr);

	success=save_text(argv[3], text->data, text->count);//save as argv[3]
	array_free(&text, 0);
#endif
	printf("Quitting...\n");
	pause();
	return 0;
}