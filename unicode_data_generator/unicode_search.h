#pragma once
#ifndef UNICODE_SEARCH_H
#define UNICODE_SEARCH_H
#include"array.h"

typedef struct CodeRankStruct
{
	int code, rank;
} CodeRank;
ArrayHandle unicode_search(const char *query);//returns CodeRank array

#endif
