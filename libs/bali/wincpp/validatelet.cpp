#include "stdafx.h"
#include "validateletImpl.h"

namespace bali {



// static member variables
AttributesSet AttributesSet::empty;
const StatePtr EmptyState::emptySet = StatePtr(new EmptyState());



AttributesSet::AttributesSet(
	const Schema* schema,
	ISAXAttributes* pAttributes,
	AttributesSet* _previous )
	
	: previous(_previous) {

	size = pAttributes->getLength();
	names = new Name[size];
	values = new XMLText[size];

	for( int i=0; i<size; i++ ) {
		XMLString _uri,_local,qname;
		int cchUri,cchLocal,cchQname;

		pAttributes->getName( i, &_uri, &cchUri, &_local, &cchLocal, &qname, &cchQname );

		EXTRACT( uri, _uri, cchUri );
		EXTRACT( local, _local, cchLocal );

		names[i] = schema->getNameCode( uri, local );

		XMLString v; int cchValue;
		pAttributes->getValue( i, &v, &cchValue );

		XMLString vv = new XMLChar[cchValue+1];
		wcsncpy(vv,v,cchValue);
		vv[cchValue]=L'\0';

		values[i].set(vv);
	}
}

AttributesSet::AttributesSet( const Schema* schema, MSXML2::IXMLDOMElement* pElement, AttributesSet* _previous )
	: previous(_previous) {

	static _bstr_t XMLNS(L"xmlns");
	
	MSXML2::IXMLDOMNamedNodeMapPtr attributes = pElement->attributes;
	
	int maxSize = attributes->length;
	names = new Name[maxSize];
	values = new XMLText[maxSize];

	int j=0;
	for( int i=0; i<maxSize; i++ ) {
		MSXML2::IXMLDOMAttributePtr att = attributes->item[i];

		if( XMLNS==att->prefix || XMLNS==att->nodeName )
			continue;	// this is xmlns node. skip it.

		names[j] = schema->getNameCode( att->namespaceURI, att->baseName );

		_bstr_t value = att->value;

		XMLString vv = new XMLChar[value.length()+1];
		wcscpy(vv,static_cast<XMLString>(value));

		values[j].set(vv);
		j++;
	}

	size = j;
}




StatePtr SingleState::expandAttributes( const AttributesSet& attributes, StatePtr result ) {
	StatePtr qptr = StatePtr(this);
	
	if( result->contains(qptr) )	return result;

	if( info->isPersistent )
		result = makeChoice( result, qptr );

	for( const transition::Att* aa = info->aTr; aa!=NULL; aa=aa->next ) {
		int matchCount=0;
		int failCount=0;

		for( int j=attributes.getSize()-1; j>=0; j-- ) {
			if( aa->name.accepts( attributes.getName(j) ) ) {
				StatePtr sp = aa->left->text( attributes.getValue(j), AttributesSet::empty, emptySet );
				if( sp->isFinal )	matchCount++;
				else {
					failCount++;
					break;
				}
			}
		}

		if( failCount==0 ) {
			if( (matchCount==1 && !aa->repeated) || (matchCount!=0 && aa->repeated) )
				result = aa->right->expandAttributes( attributes, result );
		}
	}

	for( const transition::NoAtt* na = info->nTr; na!=NULL; na=na->next ) {
		int j;
		for( j=attributes.getSize()-1; j>=0; j-- ) {
			if( na->accepts( attributes.getName(j) ) )
				break;
		}
		if(j==-1)
			result = na->right->expandAttributes( attributes, result );
	}

	for( const transition::Interleave* ia = info->iTr; ia!=NULL; ia=ia->next ) {
		result = makeChoice( result, makeInterleave(
			ia->left->expandAttributes( attributes, emptySet ),
			ia->right->expandAttributes( attributes, emptySet ),
			*ia ));
	}

	return result;
}

StatePtr State::makeAfter( const StatePtr& child, const StatePtr& then ) {
	if(child==emptySet || then==emptySet)	return emptySet;
	return new AfterState( child, then );
}

StatePtr State::makeChoice( const StatePtr& lhs, const StatePtr& rhs ) {
	_ASSERT( !rhs->isChoice() );

	if( lhs==emptySet )		return rhs;
	if( rhs==emptySet )		return lhs;

	if( lhs->contains(rhs) )	return lhs;

	return new ChoiceState(lhs,rhs);
}
StatePtr State::makeChoice2( const StatePtr& lhs, const StatePtr& rhs ) {
	if( rhs->isChoice() ) {
		ChoiceState* cptr = static_cast<ChoiceState*>(static_cast<State*>(rhs));
		return makeChoice( makeChoice2( lhs, cptr->lhs), cptr->rhs );
	} else {
		return makeChoice( lhs, rhs );
	}
}
StatePtr State::makeInterleave( const StatePtr& child, const StatePtr& then, const transition::Interleave& alphabet ) {
	if(child==emptySet || then==emptySet)	return emptySet;
	return new InterleaveState( child, then, alphabet );
}















StatePtr SingleState::startElement( Name nameCode, const AttributesSet& attributes, const StatePtr& partialResult ) const {
	
	StatePtr result = partialResult;

	for( const transition::Element* e = info->eTr; e!=NULL; e = e->next ) {
		if( e->name.accepts(nameCode) )
			result = makeChoice( result, makeAfter(
			e->left->expandAttributes(attributes,emptySet),
			e->right ) );
	}
	return result;
}

StatePtr SingleState::endElement( const AttributesSet& attributes, const StatePtr& partialResult ) const {
	_ASSERT(FALSE);
	return emptySet;
}

StatePtr SingleState::expand( const AttributesSet& attributes, const StatePtr& partialResult ) {
	return expandAttributes(attributes,partialResult);
}

StatePtr SingleState::text( const XMLText& value, const AttributesSet& attributes, const StatePtr& partialResult ) {
	StatePtr result = partialResult;

	if( value.isIgnorable )
		result = makeChoice( result, this );

	for( const transition::Data* d = info->dTr; d!=NULL; d=d->next ) {
		if( (*d->datatype)->isValid(value.str)
		&& ( d->left==NULL/*optimization for emptySet*/
		 || !d->left->text( value, AttributesSet::empty, emptySet )->isFinal ) )
			result = d->right->expandAttributes(attributes,result);
	}

	//for( const transition::Value* v = vTr; v!=NULL; v=v->next ) {
	//	if( v->accepts(value) )
	//		result = v->right->expandAttributes(attributes,result);
	//}

	for( const transition::List* l = info->lTr; l!=NULL; l=l->next ) {
		XMLString buf = new XMLChar[value.len+1];
		wcscpy(buf,value.str);

		for( XMLString p=buf; p!=buf+value.len; p++ )
			if( XMLText::isWhitespace(*p) )
				*p = '\0';
		
		StatePtr child = l->left;

		for( XMLString p=buf; p!=buf+value.len+1; p+=wcslen(p)+1 ) {
			if( *p=='\0' )	continue;

			child = child->text( XMLText(p), AttributesSet::empty, emptySet );
		}

		if( child->isFinal )
			result = l->right->expandAttributes(attributes,result);

		delete buf;
	}

	return result;
}





//
// Built-in datatype
//
class BuiltinBaseType : public Datatype
{
public:
	bool isValid( const XMLString str /* context*/ ) {
		return true;
	}
	void deleteValue( void* pValue ) {
		free(pValue);
	}
	bool sameValue( const void* value1, const void* value2 ) {
		return !wcscmp(
			(const wchar_t*)value1,
			(const wchar_t*)value2);
	}
};
class StringType : public BuiltinBaseType
{
public:
	void* createValue( const XMLString str /*context*/ ) {
		return wcsdup(str);
	}
};
class TokenType : public BuiltinBaseType
{
public:
	void* createValue( const XMLString _str /*context*/ ) {
		XMLString str = _str;
		XMLString r = wcsdup(str);
		
		// collapse whitespaces
		bool inWhitespace = true;
		XMLString p;
		for( p=r; *str!='\0'; str++ ) {
			if( !XMLText::isWhitespace(*str) ) {
				*p++ = *str;
				inWhitespace = false;
			} else {
				if(!inWhitespace)
					*p++ = ' ';
				inWhitespace = true;
			}
		}

		if( inWhitespace && p!=r )	p--;
		
		*p = '\0';	// terminate the string
			
		return r;
	}
};
class ValueDatatype : public Datatype
{
private:
	Datatype**	pCore;	// double indirection to avoid initialization fiasco
	void*		pValue;
	XMLString valueStr;

public:
	ValueDatatype( Datatype** _pCore, const XMLString _valueStr ) {
		pCore = _pCore;
		pValue = NULL;
		valueStr = _valueStr;
	}
	~ValueDatatype() {
		if(pValue!=NULL)
			(*pCore)->deleteValue(pValue);
//		delete pCore;
	}

	bool isValid( const XMLString str /* context*/ ) {
		void* rhs = (*pCore)->createValue(str);
		if(rhs==NULL)	return false;

		if(pValue==NULL)
			pValue = (*pCore)->createValue(valueStr);	// lazy initialization

		bool result = (*pCore)->sameValue(pValue,rhs);
		(*pCore)->deleteValue(rhs);
		return result;
	}
	void* createValue( const XMLString str ) {
		return (*pCore)->createValue(str);
	}
	void deleteValue( void* pValue ) {
		(*pCore)->deleteValue(pValue);
	}
	bool sameValue( const void* value1, const void* value2 ) {
		return (*pCore)->sameValue(value1,value2);
	}
};


Datatype* createValueDatatype( Datatype** dt, const XMLString value ) {
	// TODO: context
	return new ValueDatatype( dt, value );
}

Datatype* createDatatype( const XMLString nsUri, const XMLString localName, .../*parameters*/ ) {
	// TODO: datatype library look up
	// TODO: context
	if( !wcscmp(nsUri,L"") ) {
		if( !wcscmp(localName,L"string") )
			return new StringType();
		if( !wcscmp(localName,L"token") )
			return new TokenType();
	}

//	_ASSERT(FALSE);
	return new StringType();

	return NULL;	// unable to find the datatype
}


}