#ifndef GAME_UTIL_H_
#define GAME_UTIL_H_

/* Set up for C function definitions, even when using C++ */
#ifdef __cplusplus
extern "C" {
#endif



typedef struct tBlock 
{
	s16		x;
	s16		y;
	s16		w;
	s16		h;

	u16		imgIndex;

}tBlock;






/* Ends C function definitions when using C++ */
#ifdef __cplusplus
}
#endif

#endif /*GAME_UTIL_H_*/