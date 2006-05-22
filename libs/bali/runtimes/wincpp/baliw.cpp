//
// validate files passed via the command line.
//
// exits with code 0 if no error was detected. Otherwise non-zero.
//
#include "stdafx.h"
#include "schema.h"

using namespace MSXML2;

int sax( int argc, _TCHAR* argv[] );
int dom( int argc, _TCHAR* argv[] );

int _tmain(int argc, _TCHAR* argv[])
{
	CoInitialize(NULL);
	
	if( !strcmp(argv[1],"sax") ) {
		puts("SAX mode");
		return sax(argc,argv);
	} else {
		puts("DOM mode");
		return dom(argc,argv);
	}
}

int dom( int argc, _TCHAR* argv[] ) {
	bali::IValidatelet* pValidatelet = test::createSchemaDOMValidatelet();

	int r = 0;

	for( int i=2; i<argc; i++ ) {
		printf("parsing %s\n",argv[i]);

		MSXML2::IXMLDOMDocumentPtr doc(__uuidof(MSXML2::DOMDocument));
		doc->async = false;
		doc->preserveWhiteSpace = VARIANT_TRUE;
		if( doc->load( _bstr_t(argv[i]) )==VARIANT_FALSE ) {
			puts("not well-formed");
			r++;
		} else {
			printf("validating %s\n",argv[i]);
			
			bool result = pValidatelet->validate( doc, NULL );
			if( result )
				puts("valid");
			else {
				r++;
				puts("invalid");
			}
		}
	}

	delete pValidatelet;

	return r;
}

int sax( int argc, _TCHAR* argv[] ) {
	MSXML2::ISAXContentHandlerPtr pValidator(test::createSchemaSAXValidatelet());
	MSXML2::ISAXXMLReaderPtr pReader(__uuidof(MSXML2::SAXXMLReader40));

	try {

		pReader->putFeature( L"http://xml.org/sax/features/namespaces", VARIANT_TRUE );
		pReader->putFeature( L"http://xml.org/sax/features/namespace-prefixes", VARIANT_FALSE );
		pReader->putContentHandler( pValidator );

		for( int i=2; i<argc; i++ ) {
			printf("parsing %s\n",argv[i]);
			pReader->parseURL( _bstr_t(argv[i]) );
		}

		return 0;

	} catch( const _com_error& e ) {
		printf("error(%08X):%s\n",e.Error(), e.ErrorMessage());
		_bstr_t desc = e.Description();
		if(!desc)
			puts("  no description");
		else
			puts(desc);
		
		return 1;
	}
}

