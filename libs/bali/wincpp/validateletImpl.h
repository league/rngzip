//
// Header file that defines implementations of validatelet
//
// Client application shouldn't include this file
//
#pragma once
#include "validatelet.h"

namespace bali {
using namespace MSXML2;

typedef DWORD			Name;	// 32 bit name identifier
typedef	wchar_t			XMLChar;
typedef XMLChar*		XMLString;
_COM_SMARTPTR_TYPEDEF(ICreateErrorInfo, __uuidof(ICreateErrorInfo));
_COM_SMARTPTR_TYPEDEF(IErrorInfo, __uuidof(IErrorInfo));

class Schema;



struct XMLText {
public:
	XMLString str;		// string
	size_t len;			// length of the string
	bool isIgnorable;	// true if ignorable

	static inline bool isWhitespace( XMLChar ch ) {
		return ch==' ' || ch=='\t' || ch=='\r' || ch=='\n';
	}

	XMLText( XMLString s ) {
		set(s);
	}
	XMLText() {}
	void set( XMLString s ) {
		str = s;
		len = wcslen(str);
		isIgnorable = true;
		for( size_t i=0; i<len; i++ )
			if( !isWhitespace(str[i]) )
				isIgnorable = false;
	}
};



//
// AttributesSet holds a dictionary from attribute names (encoded as name codes)
// to their values.
//
// This object is used as immutable object, meaning that values can't be changed
// once an object is created.
class AttributesSet
{
private:
	// size of the attributes
	int size;

	Name* 		names;
	XMLText*	values;

public:
	// linked list to the previous attribute set
	AttributesSet* const		previous;

	inline int getSize() const { return size; }
	inline Name getName( int index ) const {
		return names[index];
	}

	// gets the attribute value.
	inline const XMLText& getValue( int index ) const {
		return values[index];
	}

	AttributesSet() : previous(NULL) { // empty constructor
		size = 0;
		names = new Name[0];
		values = new XMLText[0];
	}

	// construct an object from SAX attributes
	AttributesSet( const Schema* schema, ISAXAttributes* pAttributes, AttributesSet* _previous );

	// construct an object from DOM element
	AttributesSet( const Schema* schema, MSXML2::IXMLDOMElement* pElement, AttributesSet* _previous );

	~AttributesSet() {
		delete names;
		for( int i=0; i<size; i++ )
			delete values[i].str;
		delete values;
	}
	// delete this object and return the previous object
	AttributesSet* release() {
		AttributesSet* r = previous;
		delete this;
		return r;
	}

	// constant empty attributes
	static AttributesSet empty;
};







//
// datatypes
//

class Datatype
{
public:
	virtual ~Datatype() {}

	virtual bool isValid( const XMLString str /* context*/ ) =0;
	virtual void* createValue( const XMLString str /*context*/ ) =0;
	virtual void deleteValue( void* pValue ) =0;
	virtual bool sameValue( const void* value1, const void* value2 ) =0;
};

class ValueRestrictedDatatype : public Datatype {
private:
	Datatype* const	dt;		// this object doesn't own the datatype.
	void* value;	// value object to match. constructed lazily to avoid initialization order fiasco.
	const XMLString valueText;

public:
	ValueRestrictedDatatype( Datatype* _dt, XMLString _value ) : dt(_dt), valueText(_value), value(NULL) {}
	virtual ~ValueRestrictedDatatype() {
		if(value!=NULL)
			dt->deleteValue(value);
	}

	virtual bool isValid( const XMLString str /* context*/ ) {
		void* pNewVal = dt->createValue( str );
		if( pNewVal==NULL )		return false;

		if( value==NULL )
			value = dt->createValue(valueText);

		bool r = dt->sameValue( value, pNewVal );
		dt->deleteValue(pNewVal);
		return r;
	}
	virtual void* createValue( const XMLString str /*context*/ ) {
		return dt->createValue(str);
	}
	virtual void deleteValue( void* pValue ) {
		return dt->deleteValue(pValue);
	}
	virtual bool sameValue( const void* value1, const void* value2 ) {
		return dt->sameValue(value1,value2);
	}
};

Datatype* createDatatype( const XMLString nsUri, const XMLString localName, .../*parameters*/ );
Datatype* createValueDatatype( Datatype** dt, const XMLString value );







//
// transitions
//
class SingleState;

namespace transition
{
	// To initialize data structure without running code,
	// we can't have constructor nor const fields.
	// ssee C2552 error.

	class NameSignature
	{
	public:
//	private:
		/*const*/ Name mask,test;

	public:
//		NameSignature( Name _mask, Name _test ) : mask(_mask), test(_test) {}
		inline bool accepts( Name nameCode ) const {
			return (nameCode&mask)==test;
		}
	};


	class Att
	{
	public:
		/*const*/ NameSignature name;
		/*const*/ bool repeated;
		SingleState* /*const*/ left;
		SingleState* /*const*/ right;

		/*const*/ Att* /*const*/ next;

//		Att( NameSignature _name, bool _repeated, SingleState* _left, SingleState* _right, Att* _next )
//			: name(_name), repeated(_repeated), left(_left), right(_right), next(_next) {}
	};

	class Data
	{
	public:
		SingleState* /*const*/ left;
		SingleState* /*const*/ right;
		Datatype** /*const*/ datatype;
			// because of the problem in the order of initialization, we can't really asssume
			// that we have a correct pointer to the datatype. double indirection makes this safer.

		/*const*/ Data* next;
		// TODO: datatype

//		Data( SingleState* _left, SingleState* _right, Datatype* _dt, Data* _next )
//			: left(_left), right(_right), datatype(_dt), next(_next) {}
	};

	class Element
	{
	public:
		/*const*/ NameSignature name;
		SingleState* /*const*/ left;
		SingleState* /*const*/ right;

		/*const*/ Element* /*const*/ next;

//		Element( NameSignature _name, SingleState* _left, SingleState* _right, Element* _next )
//			: name(_name), left(_left), right(_right), next(_next) {}
	};

	class Interleave
	{
	public:
		SingleState* /*const*/ left;
		SingleState* /*const*/ right;
		SingleState* /*const*/ join;

		/*const*/ bool textToLeft;

		/*const*/ Interleave* /*const*/ next;

//		Interleave( SingleState* _left, SingleState* _right, SingleState* _join, bool _ttl, Interleave* _next )
//			: left(_left), right(_right), join(_join), textToLeft(_ttl), next(_next) {}
	};

	class List
	{
	public:
		SingleState* /*const*/ left;
		SingleState* /*const*/ right;
		/*const*/ List* /*const*/ next;

//		List( SingleState* _left, SingleState* _right, List* _next )
//			: left(_left), right(_right), next(_next) {}
	};

	class NoAtt
	{
	public:
		SingleState* /*const*/ right;
		/*const*/ NameSignature* negTests;
		/*const*/ int negTestSize;
		/*const*/ NameSignature* posTests;
		/*const*/ int posTestSize;
		
		/*const*/ NoAtt* /*const*/ next;

//		NoAtt( SingleState* _right,
//			NameSignature* _negTests, int _negTestSize,
//			NameSignature* _posTests, int _posTestSize,
//			NoAtt* _next ) : right(_right),
//				negTests(_negTests), negTestSize(_negTestSize),
//				posTests(_posTests), posTestSize(_posTestSize), next(_next) {}
		
		bool accepts( Name nameCode ) const {
			for( int i=posTestSize-1; i>=0; i-- )
				if( posTests[i].accepts(nameCode) )
					return false;
			
			for( int i=negTestSize-1; i>=0; i-- )
				if( negTests[i].accepts(nameCode) )
					return true;
			
			return false;
		}
	};

	//class Value
	//{
	//public:
	//	SingleState* const right;
	//	Datatype::Datatype* const datatype;
	//	const void* value;
	//	const Value* const next;

	//	Value( SingleState* _right, Datatype::Datatype* _dt, void* _p, Value* _next )
	//		: right(_right), datatype(_dt), value(_p), next(_next) {
	//	}

	//	bool accepts( const XMLText& text ) const {
	//		void* pNewVal = datatype->createValue( text.str );
	//		if( pNewVal==NULL )		return false;
	//		bool r = datatype->sameValue( value, pNewVal );
	//		datatype->deleteValue(pNewVal);
	//		return r;
	//	}
	//};
}














//
// states
//
class State;
_COM_SMARTPTR_TYPEDEF(State, __uuidof(IUnknown));

// efficient version of comparison that doesn't invoke QueryInterface
static inline bool operator == ( const StatePtr& sp1, const StatePtr& sp2 ) {
	return static_cast<State*>(sp1)==static_cast<State*>(sp2);
}


class State : IUnknown
{
private:
	int refCount;			// reference count that controls the life time of this object

protected:
	State( bool _isFinal ) : isFinal(_isFinal) {
		refCount=0;
	}
	virtual ~State() {}

public:
	// use IUnknown for reference counting
	STDMETHOD_(ULONG,AddRef)() { return ++refCount; }
	STDMETHOD_(ULONG,Release)() {
		int r= --refCount;
		if(r==0)		delete this;
		return r;
	}
	STDMETHOD(QueryInterface)( REFIID iid, void** ppvObject ) {
		if( iid==__uuidof(IUnknown) ) {
			*ppvObject = this;
			AddRef();
			return S_OK;
		}
		return E_NOINTERFACE;
	}

	virtual StatePtr startElement( Name nameCode, const AttributesSet& attributes, const StatePtr& partialResult ) const =0;
	virtual StatePtr endElement( const AttributesSet& attributes, const StatePtr& partialResult ) const =0;
	virtual StatePtr expand( const AttributesSet& attributes, const StatePtr& partialResult ) =0;
	virtual StatePtr text( const XMLText& text, const AttributesSet& attributes, const StatePtr& partialResult ) =0;

	virtual StatePtr wrapAfterByAfter( const StatePtr& newThen, const StatePtr& partialResult ) const {
		_ASSERT(FALSE);
		return NULL;
	}
	virtual StatePtr wrapAfterByInterleaveLeft( const StatePtr& lhs, const transition::Interleave& alphabet, const StatePtr& partialResult ) const {
		_ASSERT(FALSE);
		return NULL;
	}
	virtual StatePtr wrapAfterByInterleaveRight( const StatePtr& rhs, const transition::Interleave& alphabet, const StatePtr& partialResult ) const {
		_ASSERT(FALSE);
		return NULL;
	}

	// true if this state is the final state
	/*const*/ bool isFinal;		// should be treated as read-only const field

	virtual bool contains( const StatePtr& s ) {
		return static_cast<State*>(s) == this;
	}
	virtual bool isChoice() { return false; }
	virtual bool isInterleave() { return false; }
	virtual bool isAfter() { return false; }



	// constants
	const static StatePtr emptySet;	// empty set



//
// utility functions for derived classes
//
protected:

	static StatePtr makeAfter( const StatePtr& child, const StatePtr& then );
	static StatePtr makeChoice( const StatePtr& child, const StatePtr& then );
	static StatePtr makeChoice2( const StatePtr& child, const StatePtr& then );
	static StatePtr makeInterleave( const StatePtr& child, const StatePtr& then, const transition::Interleave& alphabet );

};


// error state
class EmptyState : public State
{
public:
	EmptyState() : State(false) {
		// used as a singleton
		AddRef();	// increment the ref count so that it won't be released
	}

	StatePtr startElement( Name nameCode, const AttributesSet& attributes, const StatePtr& partialResult ) const {
		return partialResult;
	}
	StatePtr endElement( const AttributesSet& attributes, const StatePtr& partialResult ) const {
		return partialResult;
	}
	StatePtr expand( const AttributesSet& attributes, const StatePtr& partialResult ) {
		return partialResult;
	}
	StatePtr text( const XMLText& text, const AttributesSet& attributes, const StatePtr& partialResult ) {
		return partialResult;
	}
	virtual StatePtr wrapAfterByAfter( const StatePtr& newThen, const StatePtr& partialResult ) const {
		return partialResult;
	}
	virtual StatePtr wrapAfterByInterleaveLeft( const StatePtr& lhs, const transition::Interleave& alphabet, const StatePtr& partialResult ) const {
		return partialResult;
	}
	virtual StatePtr wrapAfterByInterleaveRight( const StatePtr& rhs, const transition::Interleave& alphabet, const StatePtr& partialResult ) const {
		return partialResult;
	}
};


struct StateInfo {
	// To initialize data structure without running code,
	// we can't have constructor, private fields, nor const fields.
	// see C2552 error.

	// since SingleState has a virtual function, which makes it impossible to do
	// static initialization (according to VC++ manual), these state information
	// is moved into a separate data structure

	/*const*/ int id;
	/*const*/ bool isFinal;
	/*const*/ bool isPersistent;

	/*const*/ transition::Att* /*const*/			aTr;	
	/*const*/ transition::Data* /*const*/			dTr;	
	/*const*/ transition::Element* /*const*/		eTr;	
	/*const*/ transition::Interleave* /*const*/		iTr;	
	/*const*/ transition::List* /*const*/			lTr;	
	/*const*/ transition::NoAtt* /*const*/			nTr;	
//	const transition::Value* const			vTr;	
};


class SingleState : public State
{
private:
	// state of this state
	StateInfo*		info;
public:
	SingleState() : State(false) {
		AddRef();	// make sure that this object won't be released
	}
	inline void init( StateInfo* p ) {
		info = p;
		State::isFinal = info->isFinal;	// copy the value
	}

	StatePtr startElement( Name nameCode, const AttributesSet& attributes, const StatePtr& partialResult ) const;
	StatePtr endElement( const AttributesSet& attributes, const StatePtr& partialResult ) const;
	StatePtr expand( const AttributesSet& attributes, const StatePtr& partialResult );
	StatePtr text( const XMLText& value, const AttributesSet& attributes, const StatePtr& result );
	StatePtr expandAttributes( const AttributesSet& attributes, StatePtr result );
};


class AfterState : public State
{
public:
	const StatePtr	child;
	const StatePtr	then;

	AfterState( const StatePtr& _child, const StatePtr& _then )
		: State(_child->isFinal), child(_child), then(_then) {}
	
	StatePtr startElement( Name nameCode, const AttributesSet& attributes, const StatePtr& partialResult ) const {
		return child->startElement( nameCode, attributes, emptySet )->wrapAfterByAfter( then, partialResult );
	}
	StatePtr endElement( const AttributesSet& attributes, const StatePtr& partialResult ) const {
		if( child->isFinal )	return then->expand( attributes, partialResult );
		else					return partialResult;
	}
	StatePtr expand( const AttributesSet& attributes, const StatePtr& partialResult ) {
		return makeChoice( partialResult,
			makeAfter( child->expand( attributes, emptySet ), then ) );
	}
	StatePtr text( const XMLText& text, const AttributesSet& attributes, const StatePtr& partialResult ) {
		return makeChoice( partialResult,
			makeAfter( child->text( text, attributes, emptySet ), then ) );
	}

	StatePtr wrapAfterByAfter( const StatePtr& newThen, const StatePtr& partialResult ) const {
		return makeChoice( partialResult, makeAfter( child, makeAfter( then, newThen ) ) );
	}
	StatePtr wrapAfterByInterleaveLeft( const StatePtr& lhs, const transition::Interleave& alphabet, const StatePtr& partialResult ) const {
		return makeChoice( partialResult, makeAfter( child, makeInterleave( lhs, then, alphabet ) ) );
	}
	StatePtr wrapAfterByInterleaveRight( const StatePtr& rhs, const transition::Interleave& alphabet, const StatePtr& partialResult ) const {
		return makeChoice( partialResult, makeAfter( child, makeInterleave( then, rhs, alphabet ) ) );
	}

	virtual bool contains( const StatePtr& s ) {
		if( !s->isAfter() )		return false; 

		AfterState* rhs = static_cast<AfterState*>(static_cast<State*>(s));

		if( this->child->contains(rhs->child)
		&&	this->then->contains(rhs->then)
		&&	rhs->then->contains(this->then) )
			return true;

		return false;
	}

	virtual bool isAfter() { return true; }
};




class ChoiceState : public State
{
public:
	const StatePtr	lhs;
	const StatePtr	rhs;

	ChoiceState( const StatePtr& _lhs, const StatePtr& _rhs )
		: State(_lhs->isFinal || _rhs->isFinal), lhs(_lhs), rhs(_rhs) {}

	StatePtr startElement( Name nameCode, const AttributesSet& attributes, const StatePtr& partialResult ) const {
		return rhs->startElement( nameCode, attributes,
			lhs->startElement( nameCode, attributes, partialResult ));
	}
	StatePtr endElement( const AttributesSet& attributes, const StatePtr& partialResult ) const {
		return rhs->endElement( attributes,
			lhs->endElement( attributes, partialResult ));
	}
	StatePtr expand( const AttributesSet& attributes, const StatePtr& partialResult ) {
		return rhs->expand( attributes,
			lhs->expand( attributes, partialResult ));
	}
	StatePtr text( const XMLText& value, const AttributesSet& attributes, const StatePtr& partialResult ) {
		return rhs->text( value, attributes,
			lhs->text( value, attributes, partialResult ));
	}

	StatePtr wrapAfterByAfter( const StatePtr& newThen, const StatePtr& partialResult ) const {
		return rhs->wrapAfterByAfter( newThen,
			lhs->wrapAfterByAfter( newThen, partialResult ) );
	}
	StatePtr wrapAfterByInterleaveLeft( const StatePtr& newLhs, const transition::Interleave& alphabet, const StatePtr& partialResult ) const {
		return rhs->wrapAfterByInterleaveLeft( newLhs, alphabet,
			lhs->wrapAfterByInterleaveLeft( newLhs, alphabet, partialResult ) );
	}
	StatePtr wrapAfterByInterleaveRight( const StatePtr& newRhs, const transition::Interleave& alphabet, const StatePtr& partialResult ) const {
		return rhs->wrapAfterByInterleaveRight( newRhs, alphabet,
			lhs->wrapAfterByInterleaveRight( newRhs, alphabet, partialResult ) );
	}

	virtual bool contains( const StatePtr& s ) {
		return lhs->contains(s) || rhs->contains(s);
	}
	virtual bool isChoice() { return true; }
};



class InterleaveState : public State
{
public:
	const StatePtr	lhs;
	const StatePtr	rhs;
	const transition::Interleave& alphabet;

	InterleaveState( const StatePtr& _lhs, const StatePtr& _rhs, const transition::Interleave& a )
		: State(_lhs->isFinal && _rhs->isFinal && a.join->isFinal), lhs(_lhs), rhs(_rhs), alphabet(a) {}

	StatePtr startElement( Name nameCode, const AttributesSet& attributes, const StatePtr& partialResult ) const {

		StatePtr result = partialResult;

		StatePtr l = lhs->startElement( nameCode, attributes, emptySet );
		result = l->wrapAfterByInterleaveRight( rhs, alphabet, result );

		StatePtr r = rhs->startElement( nameCode, attributes, emptySet );
		result = r->wrapAfterByInterleaveLeft( lhs, alphabet, result );

		return result;
	}
	StatePtr endElement( const AttributesSet& attributes, const StatePtr& partialResult ) const {
		_ASSERT(false);
		return NULL;
	}
	StatePtr expand( const AttributesSet& attributes, const StatePtr& partialResult ) {
		return makeChoice( partialResult, makeInterleave(
			lhs->expand(attributes,emptySet),
			rhs->expand(attributes,emptySet),
			alphabet ));
	}
	StatePtr text( const XMLText& value, const AttributesSet& attributes, const StatePtr& partialResult ) {
		StatePtr i,result;

		if( alphabet.textToLeft )
			i = makeInterleave(
				lhs->text(value,attributes,emptySet),
				rhs,
				alphabet );
		else
			i = makeInterleave(
				lhs,
				rhs->text(value,attributes,emptySet),
				alphabet );

		result = makeChoice( partialResult, i );

		if( i->isInterleave() && static_cast<InterleaveState*>(static_cast<State*>(i))->canJoin() )
			result = alphabet.join->expandAttributes( attributes, result );

		return result;
	}
	bool canJoin() { return lhs->isFinal && rhs->isFinal; }
	virtual bool isInterleave() { return true; }
};








struct NameLiteral
{
	// To initialize data structure without running code,
	// we can't have constructor nor const fields.
	// ssee C2552 error.

	/*const*/ XMLString	uri;
	/*const*/ XMLString 	local;
	/*const*/ Name		nameCode;

	// NameLiteral( XMLString _uri, XMLString _local, Name _nameCode )
	//	: uri(_uri), local(_local), nameCode(_nameCode) {}
};

class Schema
{
private:
	SingleState* const		initialState;	
	
	const Name				defaultNameCode;


	NameLiteral*			nameLiterals;
	int						nameLiteralSize;

public:
	Schema( SingleState* states, StateInfo* stateInfos, int stateSize,
			SingleState* _initialState, NameLiteral* _nameLiterals, int nameSize, int _defNameCode ) :
		initialState(_initialState), nameLiterals(_nameLiterals), nameLiteralSize(nameSize),
		defaultNameCode(_defNameCode) {

		// initialize all the states here.
		// if we initialize SingleState objects in a static array,
		// the compiler will produce a lengthy code to invoke the constructor for every single StateInfo.
		// this will be more efficient.
		for( int i=0; i<stateSize; i++ )
			states[i].init( stateInfos+i );
	}
	virtual ~Schema() {}

	Name getNameCode( XMLString uri, XMLString local ) const {
		if( uri==NULL )	uri=L"";	// DOM uses NULL for "".

		const NameLiteral* p = nameLiterals;
		for( int i=nameLiteralSize; i>0; i-- ) {
			if( wcscmp( p->uri, uri )==0 && wcscmp( p->local, local )==0 )
				return p->nameCode;
			p++;
		}

		local=L"*";
		p = nameLiterals;
		for( int i=nameLiteralSize; i>0; i-- ) {
			if( wcscmp( p->uri, uri )==0 && wcscmp( p->local, local )==0 )
				return p->nameCode;
			p++;
		}
		
		return defaultNameCode;
	}

	SingleState* getInitialState() { return initialState; }
};





template < class T >
class ComObject : public T
{
private:
	int refCount;

public:
	ComObject() { refCount=0; }

	// use IUnknown for reference counting
	STDMETHOD_(ULONG,AddRef)() { return ++refCount; }
	STDMETHOD_(ULONG,Release)() {
		int r= --refCount;
		if(r==0)		delete this;
		return r;
	}
};


class AbstractValidatelet
{
protected:
	Schema* schema;
	StatePtr currentState;

public:



	AbstractValidatelet() {
		currentAttributes = NULL;
		textBufferLen = 0;
		textBufferMax = 512;
		textBuffer = new XMLChar[textBufferMax];
		*textBuffer = L'\0';
	}
	virtual ~AbstractValidatelet() {
		delete textBuffer;
		deleteAttributes();
	}
	void setSchema( Schema* newSchema ) {
		schema = newSchema;
	}

private:
	void deleteAttributes() {
		for( AttributesSet* p = currentAttributes; p!=NULL; ) {
			AttributesSet* pp = p;
			p = p->previous;
			delete pp;
		}
		currentAttributes = NULL;
	}

//
// attribute handlings
//
protected:
	AttributesSet* currentAttributes;
public:
	void pushAttributes( AttributesSet* newAtts ) {
		_ASSERT( newAtts->previous == currentAttributes );
		currentAttributes = newAtts;
	}
	void popAttributes() {
		currentAttributes = currentAttributes->release();
	}



//
// text handlings
//
protected:
	XMLString textBuffer;
	size_t textBufferLen;
	size_t textBufferMax;
public:
	HRESULT processText() {
#ifdef	_DEBUG_VALIDATELET
		if(textBufferLen!=0)
			wprintf(L"\"%s\"\n",textBuffer);
#endif
		StatePtr newState = currentState->text( textBuffer, *currentAttributes, State::emptySet );
		
		// clear the buffer
		textBufferLen = 0;
		if(textBufferMax>=1024) { // avoid keeping a huge text in memory
			delete textBuffer;
			textBufferMax = 1024;
			textBuffer = new XMLChar[textBufferMax];
		}
		*textBuffer = L'\0';

		if( newState==State::emptySet )
			return error(L"unexpected text");

		currentState = newState;
		return S_OK;
	}



	STDMETHOD(raw_startDocument)() {
#ifdef	_DEBUG_VALIDATELET
		puts("startDocument");
#endif
		currentState = StatePtr(schema->getInitialState());
		deleteAttributes();
		return S_OK;
	}


	HRESULT error( const XMLString msg ) {
		ICreateErrorInfoPtr pErr;
		
		CreateErrorInfo(&pErr);
		pErr->SetDescription(msg);
		SetErrorInfo( 0, IErrorInfoPtr(pErr) );

		return E_FAIL;
	}
};



class Validatelet : public AbstractValidatelet,
					public ISAXContentHandler,
					public ISupportErrorInfo,
					public IValidatelet {
private:
	ISAXLocatorPtr locator;

public:
//
// IUnknown
//
	STDMETHOD(QueryInterface)( REFIID iid, void** ppvObject ) {
		if( iid==__uuidof(IUnknown) ) {
			*ppvObject = static_cast<ISAXContentHandler*>(this);
			static_cast<ISAXContentHandler*>(this)->AddRef();
			return S_OK;
		}
		if( iid==__uuidof(ISAXContentHandler) ) {
			*ppvObject = static_cast<ISAXContentHandler*>(this);
			static_cast<ISAXContentHandler*>(this)->AddRef();
			return S_OK;
		}
		if( iid==__uuidof(ISupportErrorInfo) ) {
			*ppvObject = static_cast<ISupportErrorInfo*>(this);
			static_cast<ISAXContentHandler*>(this)->AddRef();
			return S_OK;
		}
		return E_NOINTERFACE;
	}

//
// ISupportErrorInfo
//
	STDMETHOD(InterfaceSupportsErrorInfo)( REFIID riid ) {
		if( riid==__uuidof(ISAXContentHandler) )
			return S_OK;
		return S_FALSE;
	}

//
// ISAXContentHandler
//
	STDMETHOD(raw_startDocument)() {
		return AbstractValidatelet::raw_startDocument();
	}
	STDMETHOD(raw_endDocument)() {
		if(locator!=NULL)
			locator.Release();
		return S_OK;
	}

	STDMETHOD(raw_putDocumentLocator)( ISAXLocator* pLocator ) {
		locator = ISAXLocatorPtr(pLocator);
		return S_OK;
	}

	STDMETHOD(raw_startPrefixMapping) ( XMLString pwchPrefix, int cchPrefix, XMLString pwchUri, int cchUri ) {
		// TODO: handle prefix
		return S_OK;
	}

	STDMETHOD(raw_endPrefixMapping) ( XMLString pwchPrefix, int cchPrefix ) {
		// TODO: handle prefix
		return S_OK;
	}

#define	EXTRACT(newName,oldName,size); \
	XMLString newName = (XMLString)_alloca((size+1)*2); \
	wcsncpy(newName,oldName,size); \
	newName[size]=0;

	STDMETHOD(raw_startElement) ( XMLString _uri, int cchUri, XMLString _localName, int cchLocalName,
		XMLString _qname, int cchQname, ISAXAttributes* pAttributes ) {

		EXTRACT( uri, _uri, cchUri );
		EXTRACT( localName, _localName, cchLocalName );
		EXTRACT( qname, _qname, cchQname );

		return startElement( uri, localName, qname,
			new AttributesSet( schema, pAttributes, currentAttributes ) );
	}

	HRESULT startElement( XMLString uri, XMLString localName, XMLString qname, AttributesSet* newAttributes ) {
		HRESULT hr = processText();
		if(FAILED(hr))	return hr;

#ifdef	_DEBUG_VALIDATELET
		wprintf(L"<%s>\n",qname);
#endif

		Name nameCode = schema->getNameCode(uri,localName);
		pushAttributes(newAttributes);

		StatePtr newState = currentState->startElement( nameCode, *currentAttributes, State::emptySet );
		if( newState==State::emptySet )
			return error(L"unexpected start tag");

		currentState = newState;
		return S_OK;
	}

	STDMETHOD(raw_endElement) ( XMLString uri, int cchUri, XMLString localName, int cchLocalName,
		XMLString _qname, int cchQname ) {

		EXTRACT( qname, _qname, cchQname );
		return endElement( qname );
	}

	HRESULT endElement( XMLString qname ) {
		HRESULT hr = processText();
		if(FAILED(hr))	return hr;

		popAttributes();

#ifdef	_DEBUG_VALIDATELET
		wprintf(L"</%s>\n",qname);
#endif

		StatePtr newState = currentState->endElement( *currentAttributes, State::emptySet );
		if( newState==State::emptySet )
			return error(L"unexpected end tag");

		currentState = newState;
		return S_OK;
	}


	STDMETHOD(raw_characters) ( XMLString chars, int cchChars ) {
		if( textBufferLen+cchChars >= textBufferMax ) {
			// reallocate the buffer
			XMLString newBuf = new XMLChar[max(textBufferMax*2,textBufferMax+cchChars)];
			wcscpy( newBuf, textBuffer );
			delete textBuffer;
			textBuffer = newBuf;
			textBufferMax *= 2;
		}

		wcsncpy( textBuffer+textBufferLen, chars, cchChars );
		textBufferLen += cchChars;
		textBuffer[textBufferLen] = L'\0';	// terminate the string

		return S_OK;
	}
	STDMETHOD(raw_ignorableWhitespace) ( XMLString chars, int cchChars ) {
		return raw_characters(chars,cchChars);
	}
	STDMETHOD(raw_processingInstruction) ( XMLString target, int cchTaret, XMLString data, int cchData ) {
		return S_OK;
	}
	STDMETHOD(raw_skippedEntity) ( XMLString name, int cchName ) {
		return S_OK;
	}

//
// IValidatelet
//
public:
	bool validate( MSXML2::IXMLDOMNode* _pDom, MSXML2::IXMLDOMNode** ppErrorNode = NULL ) {
		MSXML2::IXMLDOMNodePtr pDom = _pDom;
		
		raw_startDocument();
		
		MSXML2::IXMLDOMElementPtr root = pDom;
		if( root==NULL ) {
			MSXML2::IXMLDOMDocumentPtr doc = pDom;
			if( doc==NULL )
				// incorrect node type
				return setError( ppErrorNode, pDom );
			
			root = doc ->documentElement;
		}

		bool result = visitElement(root,ppErrorNode);

		raw_endDocument();

		return result;
	}

private:
	bool visitElement( const MSXML2::IXMLDOMElementPtr& element, MSXML2::IXMLDOMNode** ppErrorNode ) {
		HRESULT hr;

		hr = startElement( element->namespaceURI, element->baseName, element->nodeName,
				new AttributesSet(schema,element,currentAttributes) );
		if(FAILED(hr))
			return setError( ppErrorNode, element );

		MSXML2::IXMLDOMNodeListPtr childNodes = element->childNodes;
		int len = childNodes->length;
		for( int i=0; i<len; i++ ) {
			MSXML2::IXMLDOMNodePtr n = childNodes->item[i];

			MSXML2::IXMLDOMElementPtr ne = n;
			if(ne!=NULL) {
				bool result = visitElement( ne, ppErrorNode );
				if(result==false)	return false;
				continue;
			}
			if(MSXML2::IXMLDOMCDATASectionPtr(n)!=NULL || MSXML2::IXMLDOMTextPtr(n)!=NULL) {
				_bstr_t text = n->nodeValue;
				characters( static_cast<XMLString>(text), text.length() );
				continue;
			}
			// ignore other nodes
		}

		hr = endElement( element->nodeName );
		if(FAILED(hr))
			return setError( ppErrorNode, element );

		return true;
	}

	bool setError( MSXML2::IXMLDOMNode** ppErrorNode, const MSXML2::IXMLDOMNodePtr& e ) {
		if(ppErrorNode!=NULL) {
			*ppErrorNode = e;
			(*ppErrorNode)->AddRef();
		}
		return false;
	}

};


/*
class VBValidatelet : public AbstractValidatelet, public IVBSAXContentHandler
{
	IVBSAXLocatorPtr locator;

public:
	STDMETHOD(QueryInterface)( REFIID iid, void** ppvObject ) {
		if( iid==__uuidof(IUnknown) ) {
			*ppvObject = this;
			AddRef();
			return S_OK;
		}
		if( iid==__uuidof(IVBSAXContentHandler) ) {
			*ppvObject = this;
			AddRef();
			return S_OK;
		}
		return E_NOINTERFACE;
	}


//
//
// IVBSAXContentHandler
//
//
	STDMETHOD(putref_documentLocator)( IVBSAXLocator* pLocator ) {
		locator = IVBSAXLocatorPtr(pLocator);
		return S_OK;
	}

	STDMETHOD(raw_endDocument)() {
		locator.Release();
		return S_OK;
	}

	STDMETHOD(raw_startPrefixMapping)( BSTR* pwchPrefix, BSTR* pwchUri ) {
		// TODO: handle prefix
		return S_OK;
	}

	STDMETHOD(raw_endPrefixMapping)( BSTR* pwchPrefix ) {
		// TODO: handle prefix
		return S_OK;
	}
	STDMETHOD(raw_startElement)( BSTR* uri, BSTR* localName, BSTR* qname, IVBSAXAttributes* pAtts ) {
#ifdef	_DEBUG_VALIDATELET
		puts("startElement");
#endif

		HRESULT hr = processText();
		if(FAILED(hr))	return hr;

		Name nameCode = schema->getNameCode(*uri,*localName);
		pushAttributes(pAtts);

		StatePtr newState = currentState->startElement( nameCode, *currentAttributes, State::emptySet );
		if( newState==State::emptySet )
			return error(L"unexpected start tag");

		currentState = newState;
		return S_OK;
	}
	
	STDMETHOD(raw_endElement)( BSTR* uri, BSTR* localName, BSTR* qname ) {
		HRESULT hr = processText();
		if(FAILED(hr))	return hr;

		popAttributes();

		StatePtr newState = currentState->endElement( *currentAttributes, State::emptySet );
		if( newState==State::emptySet )
			return error(L"unexpected end tag");

		currentState = newState;
		return S_OK;
	}
	
	STDMETHOD(raw_characters)( BSTR* chars ) {
		size_t len = wcslen(*chars);

		if( textBufferLen+len >= textBufferMax ) {
			// reallocate the buffer
			XMLString newBuf = new XMLChar[max(textBufferMax*2,textBufferMax+len+1)];
			wcscpy( newBuf, textBuffer );
			delete textBuffer;
			textBuffer = newBuf;
			textBufferMax *= 2;
		}

		wcscpy( textBuffer+textBufferLen, *chars );
		textBufferLen += len;

		return S_OK;
	}

	STDMETHOD(raw_ignorableWhitespace)( BSTR* chars ) {
		return raw_characters(chars);
	}

	STDMETHOD(raw_processingInstruction)( BSTR* target, BSTR* data ) {
		return S_OK;
	}
	STDMETHOD(raw_skippedEntity)( BSTR* name ) {
		return S_OK;
	}
};
*/


}