//
//  CSprite.h
//  100_MusicGame
//
//  Created by wazazhang on 11-8-21.
//  Copyright 2011年 __MyCompanyName__. All rights reserved.
//

#ifndef _COM_CELL_GAME_CSPRITE
#define _COM_CELL_GAME_CSPRITE

#include "CGameObject.h"
#include "CUtil.h"

namespace com_cell_game
{	    
	using namespace com_cell;
	using namespace std;
	
	class CSpriteMeta : public ICellResource
	{	
		friend class CSprite;
		
		static const char CD_TYPE_MAP 	= 0;
		static const char CD_TYPE_ATK 	= 1;
		static const char CD_TYPE_DEF 	= 2;
		static const char CD_TYPE_EXT 	= 3;
		
	public:
		
		CAnimates animates;
		
		CCollides collides;
		
		//frameAnimate[animate id][frame id] = canimate frame index.
		vector<string>			AnimateNames;
		//
		vector<vector<int> >	FrameAnimate;
		// frameAnimate[animate id][frame id] = ccollide frame index.
		vector<vector<int> >	FrameCDMap;
		// frameAnimate[animate id][frame id] = ccollide frame index.
		vector<vector<int> >	FrameCDAtk;
		// frameAnimate[animate id][frame id] = ccollide frame index.
		vector<vector<int> >	FrameCDDef;
		// frameAnimate[animate id][frame id] = ccollide frame index.
		vector<vector<int> >	FrameCDExt;

	public:
		
		CAnimates*	getAnimates();	
		
		CCollides*	getCollides();	
		
		CTiles*		getTiles();
		
		
		/////////////////////////////////////////////////////////////////
		// images
		int		getFrameImageCount(int anim, int frame);
		Image*	getFrameImage(int anim,int frame,int sub);		
		int		getFrameImageX(int anim,int frame,int sub);		
		int		getFrameImageY(int anim,int frame,int sub);
		int		getFrameImageWidth(int anim, int frame, int sub);		
		int		getFrameImageHeight(int anim, int frame, int sub);		
		char	getFrameImageTransform(int anim, int frame, int sub);		
		
		/////////////////////////////////////////////////////////////////
		// cds
		int		getFrameCDCount(int anim, int frame, char type);
		bool	getFrameCD(int anim, int frame, char type, int sub, CCD &outcd);
		
		vector<vector<int> >* getCDAnimates(char type);
		
		/////////////////////////////////////////////////////////////////
		// bounds
		
		void getVisibleBounds(CCD &out_bounds);
		bool getVisibleBounds(int anim, int frame, CCD &out_bounds);
		bool getVisibleBounds(int anim, CCD &out_bounds);		
		
		void getCDBounds(CCD &out_bounds);
		//		bool getCDBounds(int anim, int frame, char type, CCD &out_bounds);
		//		bool getCDBounds(int anim, char type, CCD &out_bounds);
		
		/////////////////////////////////////////////////////////////////
		// animate
		int		getAnimateIndex(string animate_name) ;		
		string	getAnimateName(int anim) ;		 
		int		getAnimateCount() ;
		int		getFrameCount(int anim) ;
		
		/////////////////////////////////////////////////////////////////
		// gfx
		
		void render(Graphics2D &g, float x, float y, u32 anim, u32 frame) ;
		
		void renderDebug(Graphics2D &g, float x, float y, u32 anim, u32 frame) ;
		
	}; // class CSpriteMeta
	
	
	
	
	///////////////////////////////////////////////////////////////////////////////////////////
	// CSprite
	///////////////////////////////////////////////////////////////////////////////////////////
	
	class CSprite
	{
	public:
		static const char CD_TYPE_MAP 	= 0;
		static const char CD_TYPE_ATK 	= 1;
		static const char CD_TYPE_DEF 	= 2;
		static const char CD_TYPE_EXT 	= 3;

	protected:
		
		CSpriteMeta *meta;
		
		int			CurAnimate;
		int			CurFrame;
		
	public:
		
		bool IsDebug;
		
	public:
		
		CSprite(CSpriteMeta *meta);
		
		CSpriteMeta* getMeta();
		
		int		getCurrentFrame() ;
		int		getCurrentAnimate();
		void	setCurrentAnimate(string const &anim) ;	
		void	setCurrentAnimate(int anim) ;		
		void	setCurrentFrame(int anim, int index);	
		
		bool	isEndFrame() ;		
		bool	nextFrame() ;		
		void	nextCycFrame() ;		
		void	nextCycFrame(int restart) ;		
		
		bool	prewFrame();		
		void	prewCycFrame();		
		void	prewCycFrame(int restart);		

		
		void render(Graphics2D &g, float x, float y);
		
		void render(Graphics2D &g, float x, float y, u32 anim, u32 frame) ;
		
		
		
		
	}; // class CSprite
};

#endif // #define _COM_CELL_GAME_CSPRITE
