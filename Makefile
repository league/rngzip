# Makefile -- requires GNU make

default: compile

.PHONY: default compile recompile nofiles allfiles compilefiles doc dist \
	clean distclean mostlyclean maintainer-clean libsonly
.DELETE_ON_ERROR:

################################ Paths and files

NAME = rngzip
PACKAGE = net.contrapunctus.$(NAME)

BUILD = build
CLASSPATH = /sw/share/java/xerces-j/xercesSamples.jar

LIBRARIES := bali iso-relax msv relaxng-datatype gnu-getopt
SOURCEPATH := net

SOURCES := $(shell find $(SOURCEPATH) -name '*.java')

PARSER_DIR := com/sun/msv/datatype/xsd/datetime
PARSER_NAME := $(PARSER_DIR)/ISO8601
PARSER_GEN := $(PARSER_NAME)Parser.java $(PARSER_NAME)ParserConstants.java \
  $(PARSER_NAME)ParserTokenManager.java $(PARSER_DIR)/ParseException.java \
  $(PARSER_DIR)/SimpleCharStream.java $(PARSER_DIR)/Token.java \
  $(PARSER_DIR)/TokenMgrError.java
PARSER_FILES := $(addprefix libs/msv/,$(PARSER_GEN))

STRIP_PATH := sed 's:^libs/[-a-z]*/::'
LIBRARIES := $(addprefix libs/,$(LIBRARIES))
LIB_SOURCES := $(PARSER_GEN) \
  $(shell find $(LIBRARIES) -name '*.java' | $(STRIP_PATH))
AUX_FILES := net/contrapunctus/rngzip/help.txt \
  com/sun/msv/reader/trex/ng/relaxng.rng \
  $(shell find $(LIBRARIES) -name '*.properties' | $(STRIP_PATH))

ALL_SOURCES := $(SOURCES) $(LIB_SOURCES)

CLASSES := $(patsubst %.java,%.class,$(SOURCES))
LIB_CLASSES := $(patsubst %.java,%.class,$(LIB_SOURCES))
ALL_CLASSES := $(patsubst %.java,%.class,$(ALL_SOURCES))
RESOURCES := $(addprefix $(BUILD)/,$(AUX_FILES))

vpath %.class $(BUILD)
vpath %.java $(LIBRARIES)
vpath %.jj $(LIBRARIES)
vpath %.properties $(LIBRARIES)
vpath %.rng $(LIBRARIES)

################################ Programs and options

JAVAC = javac
JVM = java
JAVADOC = javadoc
JAVACC = javacc
JAR = jar
MKDIR = mkdir -p
INSTALL = install -D -m 644

JAVAC_FLAGS = -Xlint:all 
JVM_FLAGS = -ea
JAVADOC_FLAGS = -author -use -quiet

ALL_JAVAC_FLAGS = $(JAVAC_FLAGS) -d $(BUILD) -cp $(BUILD):$(CLASSPATH) -encoding UTF-8 
ALL_JAVADOC_FLAGS = $(JAVADOC_FLAGS) -encoding UTF-8 -charset UTF-8

################################ Java build hacks

compile: nofiles $(ALL_CLASSES) compilefiles
recompile: nofiles allfiles compilefiles
libsonly: nofiles $(LIB_CLASSES) compilefiles

$(BUILD):
	$(MKDIR) $(BUILD)

nofiles:
	@$(RM) files

allfiles: $(SOURCES)
	@echo Compiling all .java files...
	@echo $^ > files

compilefiles: $(RESOURCES)
	@if [ -e files ]; then \
	  $(JAVAC) $(ALL_JAVAC_FLAGS) @files && echo Ok.; \
	else echo Nothing to compile.; fi

%.class: %.java
	@echo Compiling $?
	@echo $? >> files

$(PARSER_FILES): $(PARSER_NAME).jj
	$(JAVACC) -OUTPUT_DIRECTORY=$(dir $^) $^

$(BUILD)/%: %
	$(INSTALL) $^ $@

################################ Packaging

dist: $(SOURCES)

$(NAME).jar: nofiles $(CLASSES) compilefiles manifest.txt
	$(JAR) cfm $@ manifest.txt -C build . 

manifest.txt: Makefile
	echo Main-Class: $(PACKAGE).Driver >$@
	echo Class-Path: $(CLASSPATH)     >>$@

################################ Documentation

doc: doc/api/index.html

doc/api/index.html: $(SOURCES)
	javadoc -d $(dir $@) $(ALL_JAVADOC_FLAGS) $^

################################ Cleanliness

# Like clean, but may refrain from deleting a few files that people
# normally don't want to recompile.  We leave behind all the library
# classes in the build/ directory, and the API documentation.
mostlyclean:
	$(RM) -r $(BUILD)/net
	$(RM) files manifest.txt *~

# Delete files that are normally created by building the program.
# Also preserve files that could be made by building, but normally
# aren't, because the distribution comes with them.
clean: mostlyclean
	$(RM) -r $(BUILD) doc/api
	$(RM) $(NAME).jar

# Delete files that are created by configuring or building the
# program.  Leave only the files that were in the distribution.
distclean: clean

# Delete almost everything that con be reconstructed with this
# Makefile.  This includes the output of javaCC.  This should leave
# behind only things that are in the repository.
maintainer-clean: distclean
	$(RM) $(PARSER_FILES)
