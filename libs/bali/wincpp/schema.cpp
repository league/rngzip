#include "stdafx.h"
#include "validateletImpl.h"
#include "schema.h"

namespace test {
using namespace MSXML2;
using namespace bali;
using namespace bali::transition;

SingleState states[];
Datatype* datatypes[];
NameSignature nameTests[];

static Att aTr[] = {
 { {-1,-1}, false,NULL,NULL,NULL },
};

static Data dTr[] = {
  { NULL,states+3,datatypes+1,NULL },
};

static Element eTr[] = {
  { { -1,0 },states+2,states+3,NULL },
};

static Interleave iTr[] = {
 { NULL,NULL,NULL,false,NULL },
};

static List lTr[] = {
 { NULL,NULL,NULL },
};

static NoAtt nTr[] = {
  { states+1,nameTests+0,1,nameTests+1,0,NULL },
};

static Datatype* datatypes[] = {
  createDatatype(L"",L"string"),
  createValueDatatype(datatypes+0,L""),
};

static NameSignature nameTests[] = {
  { 0,0 },
};

static StateInfo stateInfos[] = {
{    0,false,true ,NULL   ,NULL   ,eTr+  0,NULL   ,NULL   ,NULL    },
{    1,false,true ,NULL   ,dTr+  0,NULL   ,NULL   ,NULL   ,NULL    },
{    2,false,false,NULL   ,NULL   ,NULL   ,NULL   ,NULL   ,nTr+  0 },
{    3,true ,true ,NULL   ,NULL   ,NULL   ,NULL   ,NULL   ,NULL    },
};

static SingleState states[4];

static NameLiteral nameLiterals[] = {
  { L"",L"foo",0 },
};

// schema object
Schema schema(states,stateInfos,4,states+0,nameLiterals,1,-1);

ISAXContentHandler* createSchemaSAXValidatelet() {
    ComObject<Validatelet>* p = new ComObject<Validatelet>();
    p->AddRef();
    p->setSchema(&schema);
    return p;
}
bali::IValidatelet* createSchemaDOMValidatelet() {
    ComObject<Validatelet>* p = new ComObject<Validatelet>();
    p->AddRef();
    p->setSchema(&schema);
    return p;
}
}
