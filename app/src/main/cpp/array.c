//
// Created by MSI on 2022-07-02.
//

#include"array.h"
#include<stdlib.h>
#include<string.h>
static const char file[]=__FILE__;

void				memfill(void *dst, const void *src, size_t dstbytes, size_t srcbytes)
{
	unsigned copied;
	char *d=(char*)dst;
	const char *s=(const char*)src;
	if(dstbytes<srcbytes)
	{
		memcpy(dst, src, dstbytes);
		return;
	}
	copied=srcbytes;
	memcpy(d, s, copied);
	while(copied<<1<=dstbytes)
	{
		memcpy(d+copied, d, copied);
		copied<<=1;
	}
	if(copied<dstbytes)
		memcpy(d+copied, d, dstbytes-copied);
}
void				memswap_slow(void *p1, void *p2, size_t size)
{
	unsigned char *s1=(unsigned char*)p1, *s2=(unsigned char*)p2, *end=s1+size;
	for(;s1<end;++s1, ++s2)
	{
		const unsigned char t=*s1;
		*s1=*s2;
		*s2=t;
	}
}
void 				memswap(void *p1, void *p2, size_t size, void *temp)
{
	memcpy(temp, p1, size);
	memcpy(p1, p2, size);
	memcpy(p2, temp, size);
}
void				memreverse(void *p, size_t count, size_t esize)
{
	size_t totalsize=count*esize;
	unsigned char *s1=(unsigned char*)p, *s2=s1+totalsize-esize;
	void *temp=malloc(esize);
	while(s1<s2)
	{
		memswap(s1, s2, esize, temp);
		s1+=esize, s2-=esize;
	}
	free(temp);
}
void 				memrotate(void *p, size_t byteoffset, size_t bytesize, void *temp)
{
	unsigned char *buf=(unsigned char*)p;

	if(byteoffset<bytesize-byteoffset)
	{
		memcpy(temp, buf, byteoffset);
		memmove(buf, buf+byteoffset, bytesize-byteoffset);
		memcpy(buf+bytesize-byteoffset, temp, byteoffset);
	}
	else
	{
		memcpy(temp, buf+byteoffset, bytesize-byteoffset);
		memmove(buf+bytesize-byteoffset, buf, byteoffset);
		memcpy(buf, temp, bytesize-byteoffset);
	}
}
int 				binary_search(const void *base, size_t count, size_t esize, int (*threeway)(const void*, const void*), const void *val, size_t *idx)
{
	const unsigned char *buf=(const unsigned char*)base;
	ptrdiff_t L=0, R=(ptrdiff_t)count-1, mid;
	int ret;

	while(L<=R)
	{
		mid=(L+R)>>1;
		ret=threeway(buf+mid*esize, val);
		if(ret<0)
			L=mid+1;
		else if(ret>0)
			R=mid-1;
		else
		{
			if(idx)
				*idx=mid;
			return 1;
		}
	}
	if(idx)
		*idx=L+(L<(ptrdiff_t)count&&threeway(buf+L*esize, val)<0);
	return 0;
}
void 				isort(void *base, size_t count, size_t esize, int (*threeway)(const void*, const void*))
{
	unsigned char *buf=(unsigned char*)base;
	size_t k;
	void *temp;

	if(count<2)
		return;

	temp=malloc((count>>1)*esize);
	for(k=1;k<count;++k)
	{
		size_t idx=0;
		binary_search(buf, k, esize, threeway, buf+k*esize, &idx);
		if(idx<k)
			memrotate(buf+idx*esize, (k-idx)*esize, (k+1-idx)*esize, temp);
	}
	free(temp);
}

int					valid(const void *p)
{
	size_t val=(size_t)p;

	if(sizeof(size_t)==8)//do not remove: the compiler should remove the dead code
	{
		if(val==0xCCCCCCCCCCCCCCCC)
			return 0;
		if(val==0xFEEEFEEEFEEEFEEE)
			return 0;
		if(val==0xEEFEEEFEEEFEEEFE)
			return 0;
		if(val==0xCDCDCDCDCDCDCDCD)
			return 0;
		if(val==0xBAADF00DBAADF00D)
			return 0;
		if(val==0xADF00DBAADF00DBA)
			return 0;
	}
	else
	{
		switch(val)
		{
		case 0:
		case 0xCCCCCCCC:
		case 0xFEEEFEEE:
		case 0xEEFEEEFE:
		case 0xCDCDCDCD:
		case 0xFDFDFDFD:
		case 0xBAAD0000:
			return 0;
		}
	}
	return 1;
}

//C array
#if 1
static void		array_realloc(ArrayHandle *arr, size_t count, size_t pad)//CANNOT be nullptr, array must be initialized with array_alloc()
{
	ArrayHandle p2;
	size_t size, newcap;

	ASSERT_P(*arr);
	size=(count+pad)*arr[0]->esize, newcap=arr[0]->esize;
	for(;newcap<size;newcap<<=1);
	if(newcap>arr[0]->cap)
	{
		p2=(ArrayHandle)realloc(*arr, sizeof(ArrayHeader)+newcap);
		ASSERT_P(p2);
		*arr=p2;
		if(arr[0]->cap<newcap)
			memset(arr[0]->data+arr[0]->cap, 0, newcap-arr[0]->cap);
		arr[0]->cap=newcap;
	}
	arr[0]->count=count;
}

//Array API
ArrayHandle		array_construct(const void *src, size_t esize, size_t count, size_t rep, size_t pad, DebugInfo debug_info)
{
	ArrayHandle arr;
	size_t srcsize, dstsize, cap;

	srcsize=count*esize;
	dstsize=rep*srcsize;
	cap=dstsize+pad*esize;
	arr=(ArrayHandle)malloc(sizeof(ArrayHeader)+cap);
	ASSERT_P(arr);
	arr->count=count;
	arr->esize=esize;
	arr->cap=cap;
	arr->debug_info=debug_info;
	if(src)
	{
		ASSERT_P(src);
		memfill(arr->data, src, dstsize, srcsize);
	}
	else
		memset(arr->data, 0, dstsize);

	if(cap>dstsize)//zero pad
		memset(arr->data+dstsize, 0, cap-dstsize);
	return arr;
}
void 			array_assign(ArrayHandle *arr, const void *data, size_t count)//cannot be nullptr
{
	//LOGI("array_assign: arr = %p", arr);//
	//LOGI("array_assign: arr = %p, *arr = %p", arr, arr?*arr:0);//
	ASSERT_P(*arr);
	if(arr[0]->count<count)
		array_realloc(arr, count, 0);
	if(data)
		memcpy(arr[0]->data, data, count*arr[0]->esize);
	else
		memset(arr[0]->data, 0, count*arr[0]->esize);
	arr[0]->count=count;
}
ArrayHandle		array_copy(ArrayHandle *arr, DebugInfo debug_info)
{
	ArrayHandle a2;
	size_t bytesize;

	if(!*arr)
		return 0;
	bytesize=sizeof(ArrayHeader)+arr[0]->cap;
	a2=(ArrayHandle)malloc(bytesize);
	ASSERT_P(a2);
	memcpy(a2, *arr, bytesize);
	a2->debug_info=debug_info;
	return a2;
}
void			array_free(ArrayHandle *arr, void (*destructor)(void*))//can be nullptr
{
	if(*arr&&destructor)
	{
		for(size_t k=0;k<arr[0]->count;++k)
			destructor(array_at(arr, k));
	}
	free(*arr);
	*arr=0;
}
void			array_clear(ArrayHandle *arr, void (*destructor)(void*))//can be nullptr
{
	if(*arr)
	{
		if(destructor)
		{
			for(size_t k=0;k<arr[0]->count;++k)
				destructor(array_at(arr, k));
		}
		arr[0]->count=0;
	}
}
void*			array_insert(ArrayHandle *arr, size_t idx, const void *data, size_t count, size_t rep, size_t pad)
{
	size_t start, srcsize, dstsize, movesize;

	//LOGE("sizeof(void*) == %d, ARR == %p", sizeof(void*), arr);
	ASSERT_P(*arr);
	start=idx*arr[0]->esize;
	srcsize=count*arr[0]->esize;
	dstsize=rep*srcsize;
	movesize=arr[0]->count*arr[0]->esize-start;
	array_realloc(arr, arr[0]->count+rep*count, pad);
	memmove(arr[0]->data+start+dstsize, arr[0]->data+start, movesize);
	if(data)
		memfill(arr[0]->data+start, data, dstsize, srcsize);
	else
		memset(arr[0]->data+start, 0, dstsize);
	return arr[0]->data+start;
}
void*			array_erase(ArrayHandle *arr, size_t idx, size_t count, void (*destructor)(void*))
{
	size_t k;

	ASSERT_P(*arr);
	if(arr[0]->count<idx+count)
	{
		LOG_ERROR("array_erase() out of bounds: idx=%lld count=%lld size=%lld", (long long)idx, (long long)count, (long long)arr[0]->count);
		if(arr[0]->count<idx)
			return 0;
		count=arr[0]->count-idx;//erase till end of array if OOB
	}
	if(destructor)
	{
		for(k=0;k<count;++k)
			destructor(array_at(arr, idx+k));
	}
	memmove(arr[0]->data+idx*arr[0]->esize, arr[0]->data+(idx+count)*arr[0]->esize, (arr[0]->count-(idx+count))*arr[0]->esize);
	arr[0]->count-=count;
	return arr[0]->data+idx*arr[0]->esize;
}
void			array_fit(ArrayHandle *arr, size_t pad)//can be nullptr
{
	ArrayHandle p2;
	if(!*arr)
		return;
	arr[0]->cap=(arr[0]->count+pad)*arr[0]->esize;
	p2=(ArrayHandle)realloc(*arr, sizeof(ArrayHeader)+arr[0]->cap);
	ASSERT_P(p2);
	*arr=p2;
}
size_t			array_size(ArrayHandle const *arr)//can be nullptr
{
	if(!arr[0])
		return 0;
	return arr[0]->count;
}
void*			array_at(ArrayHandle *arr, size_t idx)
{
	if(!arr[0])
		return 0;
	if(idx>=arr[0]->count)
		return 0;
	return arr[0]->data+idx*arr[0]->esize;
}
const void*		array_at_const(ArrayConstHandle const *arr, int idx)
{
	if(!arr[0])
		return 0;
	return arr[0]->data+idx*arr[0]->esize;
}
void*			array_back(ArrayHandle *arr)
{
	if(!*arr||!arr[0]->count)
		return 0;
	return arr[0]->data+(arr[0]->count-1)*arr[0]->esize;
}
const void*		array_back_const(ArrayConstHandle const *arr)
{
	if(!*arr||!arr[0]->count)
		return 0;
	return arr[0]->data+(arr[0]->count-1)*arr[0]->esize;
}
#endif