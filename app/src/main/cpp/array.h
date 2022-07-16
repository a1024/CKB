//
// Created by MSI on 2022-07-02.
//

#pragma once
#ifndef CUSTOMKB_ARRAY_H
#define CUSTOMKB_ARRAY_H
#ifdef _MSC_VER
#define _CRT_SECURE_NO_WARNINGS
#include<stdio.h>
#define snprintf sprintf_s
#define vsnprintf vsprintf_s
#define LOGI printf
#define LOGE printf
#else
#include<jni.h>
#include<android/log.h>
extern const char log_tag[];
#define		LOGI(...)  __android_log_print(ANDROID_LOG_INFO, log_tag, ##__VA_ARGS__)
#define		LOGE(...)  __android_log_print(ANDROID_LOG_ERROR, log_tag, ##__VA_ARGS__)
#ifdef __cplusplus
#define EXTERN_C	extern "C"
#else
#define EXTERN_C
#endif
#endif
#include<stddef.h>

#define		SIZEOF(ARR)		(sizeof(ARR)/sizeof(*(ARR)))

void		memfill(void *dst, const void *src, size_t dstbytes, size_t srcbytes);
void		memswap_slow(void *p1, void *p2, size_t size);
void 		memswap(void *p1, void *p2, size_t size, void *temp);
void		memreverse(void *p, size_t count, size_t esize);//calls memswap
void 		memrotate(void *p, size_t byteoffset, size_t bytesize, void *temp);//temp buffer is min(byteoffset, bytesize-byteoffset)
int 		binary_search(const void *base, size_t count, size_t esize, int (*threeway)(const void*, const void*), const void *val, size_t *idx);//returns true if found, otherwise the idx is where val should be inserted
void 		isort(void *base, size_t count, size_t esize, int (*threeway)(const void*, const void*));//binary insertion sort

int			log_error(const char *file, int line, const char *format, ...);
int			valid(const void *p);
#define		LOG_ERROR(format, ...)	log_error(file, __LINE__, format, ##__VA_ARGS__)
#define		ASSERT(SUCCESS)			((SUCCESS)!=0||log_error(file, __LINE__, #SUCCESS))
#define		ASSERT_P(POINTER)		(valid(POINTER)||log_error(file, __LINE__, #POINTER " == 0"))

//array
#if 1
#ifdef DEBUG_INFO_STR
typedef const char *DebugInfo;
#else
typedef size_t DebugInfo;
#endif
#ifdef _MSC_VER
#pragma warning(push)
#pragma warning(disable:4200)//no default-constructor for struct with zero-length array
#endif
typedef struct ArrayHeaderStruct
{
	size_t count, esize, cap;//cap is in bytes
	DebugInfo debug_info;
	unsigned char data[];
} ArrayHeader, *ArrayHandle;
typedef const ArrayHeader *ArrayConstHandle;
#ifdef _MSC_VER
#pragma warning(pop)
#endif
ArrayHandle		array_construct(const void *src, size_t esize, size_t count, size_t rep, size_t pad, DebugInfo debug_info);
void 			array_assign(ArrayHandle *arr, const void *data, size_t count);//cannot be nullptr
ArrayHandle		array_copy(ArrayHandle *arr, DebugInfo debug_info);//shallow
void			array_free(ArrayHandle *arr, void (*destructor)(void*));
void			array_clear(ArrayHandle *arr, void (*destructor)(void*));//keeps allocation
int 			array_fit(ArrayHandle *arr, size_t pad);

void*			array_insert(ArrayHandle *arr, size_t idx, const void *data, size_t count, size_t rep, size_t pad);//cannot be nullptr
void*			array_erase(ArrayHandle *arr, size_t idx, size_t count, void (*destructor)(void*));

size_t			array_size(ArrayHandle const *arr);//use arr->count instead
void*			array_at(ArrayHandle *arr, size_t idx);
const void*		array_at_const(ArrayConstHandle const *arr, int idx);
void*			array_back(ArrayHandle *arr);
const void*		array_back_const(ArrayConstHandle const *arr);

#ifdef DEBUG_INFO_STR
#define			ARRAY_ALLOC(ELEM_TYPE, ARR, COUNT, PAD, DEBUG_INFO)	ARR=array_construct(0, sizeof(ELEM_TYPE), COUNT, 1, PAD, DEBUG_INFO)
#else
#define			ARRAY_ALLOC(ELEM_TYPE, ARR, COUNT, PAD)				ARR=array_construct(0, sizeof(ELEM_TYPE), COUNT, 1, PAD, __LINE__)
#endif
#define			ARRAY_APPEND(ARR, DATA, COUNT, REP, PAD)			array_insert(&(ARR), (ARR)->count, DATA, COUNT, REP, PAD)
#define			ARRAY_DATA(ARR)			(ARR)->data
#define			ARRAY_I(ARR, IDX)		*(int*)array_at(&ARR, IDX)
#define			ARRAY_U(ARR, IDX)		*(unsigned*)array_at(&ARR, IDX)
#define			ARRAY_F(ARR, IDX)		*(double*)array_at(&ARR, IDX)


//null terminated array
#ifdef DEBUG_INFO_STR
#define			ESTR_ALLOC(TYPE, STR, LEN, DEBUG_INFO)				STR=array_construct(0, sizeof(TYPE), 0, 1, LEN+1, DEBUG_INFO)
#define			ESTR_COPY(TYPE, STR, SRC, LEN, REP, DEBUG_INFO)		STR=array_construct(SRC, sizeof(TYPE), LEN, REP, 1, DEBUG_INFO)
#else
#define			ESTR_ALLOC(TYPE, STR, LEN)				STR=array_construct(0, sizeof(TYPE), 0, 1, LEN+1, __LINE__)
#define			ESTR_COPY(TYPE, STR, SRC, LEN, REP)		STR=array_construct(SRC, sizeof(TYPE), LEN, REP, 1, __LINE__)
#endif
#define			STR_APPEND(STR, SRC, LEN, REP)	array_insert(&(STR), (STR)->count, SRC, LEN, REP, 1)
#define			STR_FIT(STR)					array_fit(&STR, 1)
#define			ESTR_AT(TYPE, STR, IDX)			*(TYPE*)array_at(&(STR), IDX)

#define			STR_ALLOC(STR, LEN)				ESTR_ALLOC(char, STR, LEN)
#define			STR_COPY(STR, SRC, LEN, REP)	ESTR_COPY(char, STR, SRC, LEN, REP)
#define			STR_AT(STR, IDX)				ESTR_AT(char, STR, IDX)

#define			WSTR_ALLOC(STR, LEN)			ESTR_ALLOC(wchar_t, STR, LEN)
#define			WSTR_COPY(STR, SRC, LEN, REP)	ESTR_COPY(wchar_t, STR, SRC, LEN, REP)
#define			WSTR_AT(STR, IDX)				ESTR_AT(wchar_t, STR, IDX)
#endif

#endif //CUSTOMKB_ARRAY_H
