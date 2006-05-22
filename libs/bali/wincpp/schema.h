#pragma once


// for this validatelet to compile,
// stdafx.h needs to have the following lines.
/*
#import <msxml4.dll>
#include <crtdbg.h>
#include "validatelet.h"
*/


// create a new instance of validatelet.
// A validatelet will be returned with its reference count
// incremented to 1. Thus its the callers responsibility to
// release the object.
namespace test {
MSXML2::ISAXContentHandler* createSchemaSAXValidatelet();
bali::IValidatelet* createSchemaDOMValidatelet();
}

