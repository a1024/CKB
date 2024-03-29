/*	unicode_search.c - The implementation of the unicode search by name
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
// Created by MSI on 2022-07-11.
//

#include"ckb.h"
#include"unicode_data.h"
#include<ctype.h>

typedef struct RangeStruct
{
	const char *start, *end;
} Range;

const int nqueries=SIZEOF(queries);

static const char* skip_till_ws(const char *p)
{
	for(;*p&&!isspace(*p);++p);
	return p;
}
static const char* skip_ws(const char *p)
{
	for(;*p&&isspace(*p);++p);
	return p;
}
static int strrange_ci_threeway(const void *left, const void *right)
{
	const char *L=*(const char**)left;
	Range const *r=(Range const*)right;
	const char *R=r->start;

	for(;*L&&R<r->end&&toupper(*L)==toupper(*R);++L, ++R);

	if(!*L&&R>=r->end)
		return 0;

	char lc=(char)toupper(*L), rc=(char)(R<r->end?toupper(*R):127);
	if(!lc)
		lc=127;
	return (lc>rc)-(lc<rc);
}
static int code_threeway(const void *left, const void *right)//to find code
{
	CodeRank const *L=(CodeRank const*)left;
	const int *R=(const int*)right;

	return (L->code>*R)-(L->code<*R);
}
static int rank_threeway(const void *left, const void *right)//to sort results by rank at the end
{
	CodeRank const *L=(CodeRank const*)left, *R=(CodeRank const*)right;

	return (L->rank>R->rank)-(L->rank<R->rank);
}
ArrayHandle unicode_search(const char *query)//returns CodeRank array
{
	ArrayHandle result;
	Range range;
	size_t idx;
	CodeRank *cr;

	ARRAY_ALLOC(CodeRank, result, 0, 0);
	for(range.start=query;*range.start;range.start=skip_ws(range.end))
	{
		range.end=skip_till_ws(range.start);
		if(binary_search(queries, nqueries, sizeof(void*), strrange_ci_threeway, &range, &idx))//<- can use standard bsearch here
		{
			int start=indices[idx], end=idx+1<nqueries?indices[idx+1]:nqueries;
			for(int k=start;k<end;++k)
			{
				if(binary_search(result->data, result->count, result->esize, code_threeway, allcodes+k, &idx))
				{
					cr=(CodeRank*)array_at(&result, idx);
					++cr->rank;
				}
				else
				{
					cr=array_insert(&result, idx, 0, 1, 1, 0);
					cr->code=allcodes[k];
					cr->rank=1;
				}
			}
		}
	}
	isort(result->data, result->count, result->esize, rank_threeway);
	return result;
}
