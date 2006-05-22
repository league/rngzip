# Makefile -- requires GNU make

default: build

.PHONY: default build rebuild nofiles allfiles buildfiles doc
.DELETE_ON_ERROR:

################################ Paths and files

BUILD = build
CLASSPATH = $(BUILD)

LIBRARIES := bali iso-relax msv relaxng-datatype
SOURCEPATH = net

PARSER_DIR := com/sun/msv/datatype/xsd/datetime
PARSER_NAME := $(PARSER_DIR)/ISO8601

PARSER_GEN := $(PARSER_NAME)Parser.java $(PARSER_NAME)ParserConstants.java \
  $(PARSER_NAME)ParserTokenManager.java $(PARSER_DIR)/ParseException.java \
  $(PARSER_DIR)/SimpleCharStream.java $(PARSER_DIR)/Token.java \
  $(PARSER_DIR)/TokenMgrError.java

PARSER_FILES := $(addprefix libs/msv/,$(PARSER_GEN))

LIBRARIES := $(addprefix libs/,$(LIBRARIES))
SOURCES := $(PARSER_GEN) \
  $(shell find $(LIBRARIES) -name '*.java' | sed 's:^libs/[-a-z]*/::')

CLASSES := $(patsubst %.java,%.class,$(SOURCES))

vpath %.class $(BUILD)
vpath %.java $(LIBRARIES)
vpath %.jj $(LIBRARIES)

################################ Programs and options

JAVAC = javac
JVM = java
JAVADOC = javadoc
JAVACC = javacc
MKDIR = mkdir -p

JAVAC_FLAGS = -Xlint:all 
JVM_FLAGS = -ea
JAVADOC_FLAGS = -author -use -quiet

ALL_JAVAC_FLAGS = $(JAVAC_FLAGS) -encoding UTF-8 -d $(BUILD) -cp $(CLASSPATH)
ALL_JAVADOC_FLAGS = $(JAVADOC_FLAGS) -encoding UTF-8 -charset UTF-8

################################ Java build hacks

build: nofiles $(CLASSES) buildfiles

rebuild: nofiles allfiles buildfiles

nofiles:
	-@$(MKDIR) $(BUILD)
	@$(RM) files

allfiles: $(SOURCES)
	@echo Compiling all .java files...
	@echo $^ > files

buildfiles:
	@if [ -e files ]; then \
	  $(JAVAC) $(ALL_JAVAC_FLAGS) @files && echo Ok.; \
	else echo Nothing to do.; fi
	@$(RM) files

%.class: %.java
	@echo Compiling $?
	@echo $? >> files

$(PARSER_FILES): $(PARSER_NAME).jj
	$(JAVACC) -OUTPUT_DIRECTORY=$(dir $^) $^

################################ Documentation

doc: doc/api/index.html

doc/api/index.html: $(SOURCES)
	javadoc -d $(dir $@) $(ALL_JAVADOC_FLAGS) $^

################################ Packaging

dist: $(SOURCES)

################################ Cleanliness

# Delete files that are normally created by building the program.
# Also preserve files that could be made by building, but normally
# aren't, because the distribution comes with them.
clean: mostlyclean
	$(RM) -r $(BUILD) doc/api

# Delete files that are created by configuring or building the
# program.  Leave only the files that were in the distribution.
distclean: clean

# Like clean, but may refrain from deleting a few files that people
# normally don't want to recompile.  We leave behind all the library
# classes in the build/ directory, and the API documentation.
mostlyclean:
	$(RM) -r $(BUILD)/net

# Delete almost everything that con be reconstructed with this
# Makefile.  This includes the output of javaCC.  This should leave
# behind only things that are in the repository.
maintainer-clean: distclean
	$(RM) $(PARSER_FILES)
