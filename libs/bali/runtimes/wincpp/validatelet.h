#pragma once
//
// define public interface of validatelet
//

namespace bali {

/*
	Even though this interface starts with 'I', this is *NOT* a COM interface.
	It doesn't derive from IUnknown, and it doesn't support reference counting.
*/
class IValidatelet
{
public:
	virtual ~IValidatelet() {}

	/*
		validate a DOM tree rooted at the given node. 
		this method returns true if the validation is successful.
		
		If any error is found during the validation, this method returns false. In that case,
		the "ppErrorNode" parameter receives the node that causes an error.

		If you don't need this information, simply pass NULL to this parameter.

		Note that it is the caller's responsibility to call the Release method for the node
		returned by ppErrorNode. This is a standard COM convention.
	*/ 
	virtual bool validate( MSXML2::IXMLDOMNode* pDom, MSXML2::IXMLDOMNode** ppErrorNode = NULL ) = 0;
};

}