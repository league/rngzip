#include "stdafx.h"
#include "validateletImpl.h"

using namespace bali;
using namespace bali::transition;

SingleState states[];

static Datatype* datatypes[] = {
	createDatatype(L"",L"string"),
	createDatatype(L"",L"token")
};
// TODO delete datatype objects

static Element eTr[] = {
	{ {5,2}, states+0, states+1, eTr+1 },
	{ {3,2}, states+0, states+1, NULL },
//	Element(NameSignature(5,2),states+0,states+1,eTr+1),
//	Element(NameSignature(3,2),states+0,states+1,NULL),
};

static Data dTr[] = {
	{ states+0, states+3, datatypes+0, dTr+1 },
	{ states+0, states+3, datatypes+1, NULL }
//	Data(states+0,states+3,datatypes[0],dTr+1),
//	Data(states+0,states+3,datatypes[0],NULL),
};

static StateInfo stateInfos[] = {
	{ 0,true ,true ,NULL,NULL,eTr+1,NULL,NULL,NULL/*,NULL*/ },
	{ 1,true ,false,NULL,NULL,NULL,NULL,NULL,NULL/*,NULL*/ }
};

static SingleState states[2];

Schema schema(states,stateInfos,2,states+0,NULL,0,-1);

NameLiteral n = { L"uri",L"local",123 };

// test
//void foo() {
//	
//	VBValidatelet* pValidator = new VBValidatelet();
//	pValidator->AddRef();
//
//	pValidator->setSchema(&schema);
//
//	pValidator->Release();
//}
