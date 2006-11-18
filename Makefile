# Makefile -- requires GNU make

################################ Programs and options

JAVAC = javac
JVM = java
JAVADOC = javadoc
JAVACC = javacc
JAR = jar
TRANG = trang
MKDIR = mkdir -p
INSTALL = install -D -m 644

JAVAC_FLAGS = -Xlint:all -encoding UTF-8
JVM_FLAGS = -ea
JAVADOC_FLAGS = -author -use -quiet -link "file:///Developer/ADC Reference Library/documentation/Java/Reference/1.5.0/doc/api"

PSEP = :
ALL_JAVAC_FLAGS = $(JAVAC_FLAGS) -d $(BUILD) -cp $(BUILD)$(PSEP)$(CLASSPATH)
ALL_JAVADOC_FLAGS = $(JAVADOC_FLAGS) -encoding UTF-8 -charset UTF-8
ALL_JVM_FLAGS = $(JVM_FLAGS)

################################ Paths and files

JUNIT_JAR = /home/league/r/xml-group/junit4.1/junit-4.1.jar
CLASSPATH = 

NAME = rngzip
PACKAGE = net.contrapunctus.$(NAME)
VERSION = 0.1
NAME_VER = $(NAME)-$(VERSION)

LIBRARIES := bali iso-relax msv relaxng-datatype gnu-getopt \
    commons-compress colloquial
SOURCEPATH := net
BUILD = build

SOURCES := $(shell find $(SOURCEPATH) -name '*.java')

PARSER_DIR := com/sun/msv/datatype/xsd/datetime
PARSER_NAME := $(PARSER_DIR)/ISO8601
PARSER_GEN := $(PARSER_NAME)Parser.java $(PARSER_NAME)ParserConstants.java \
  $(PARSER_NAME)ParserTokenManager.java $(PARSER_DIR)/ParseException.java \
  $(PARSER_DIR)/SimpleCharStream.java $(PARSER_DIR)/Token.java \
  $(PARSER_DIR)/TokenMgrError.java
PARSER_FILES := $(addprefix libs/msv/,$(PARSER_GEN))

STRIP_PATH := sed 's:^[\.a-z]*/[-a-z]*/::'
LIBRARIES := $(addprefix libs/,$(LIBRARIES))
LIB_SOURCES := $(PARSER_GEN) \
  $(shell find $(LIBRARIES) -name '*.java' | $(STRIP_PATH))
AUX_GEN := \
  net/contrapunctus/rngzip/version.txt \
  net/contrapunctus/rngzip/context.txt 
AUX_FILES := com/sun/msv/reader/trex/ng/relaxng.rng \
  $(shell find $(LIBRARIES) -name '*.properties' | $(STRIP_PATH)) \
  net/contrapunctus/rngzip/help.txt $(AUX_GEN)

ALL_SOURCES := $(SOURCES) $(LIB_SOURCES)

CLASSES := $(patsubst %.java,%.class,$(SOURCES))
LIB_CLASSES := $(patsubst %.java,%.class,$(LIB_SOURCES))
ALL_CLASSES := $(patsubst %.java,%.class,$(ALL_SOURCES))
RESOURCES := $(addprefix $(BUILD)/,$(AUX_FILES))
DOCS := README INSTALL USAGE CAVEATS COPYING
DOCS_HTML := $(addprefix doc/,$(addsuffix .html,$(DOCS)))

vpath %.class $(BUILD)
vpath %.java $(LIBRARIES)
vpath %.jj $(LIBRARIES)
vpath %.properties $(LIBRARIES)
vpath %.rng $(LIBRARIES)

## include Makefile.local

################################ Java build hacks

default: all
all: compile

.PHONY: default compile recompile nofiles allfiles compilefiles doc dist \
	clean distclean mostlyclean maintainer-clean libsonly jvm \
	test buildtest
.DELETE_ON_ERROR:
.SUFFIXES: .rnc .rng

compile: nofiles $(ALL_CLASSES) compilefiles
recompile: nofiles allfiles compilefiles
libsonly: nofiles $(LIB_CLASSES) compilefiles

$(BUILD):
	$(MKDIR) $(BUILD)

nofiles:
	@$(RM) files

allfiles: $(ALL_SOURCES)
	@echo Compiling all .java files...
	@echo $^ > files

compilefiles: $(RESOURCES)
	@if [ -e files ]; then \
	  echo $(JAVAC) $(ALL_JAVAC_FLAGS) ... ;\
	  $(JAVAC) $(ALL_JAVAC_FLAGS) @files && echo Ok. ;\
	else echo Nothing to compile.; fi

%.class: %.java
	@echo Compiling $?
	@echo $? >> files

$(PARSER_FILES): $(PARSER_NAME).jj
	$(JAVACC) -OUTPUT_DIRECTORY=$(dir $^) $^

$(BUILD)/%: %
	$(INSTALL) $^ $@

################################ Test cases

TEST_SOURCES := $(shell find ./tests -name '*.java' | $(STRIP_PATH))
TEST_CLASSES := $(addprefix tests/,$(patsubst %.java,%.class,$(TEST_SOURCES)))
TESTS := $(subst /,.,$(patsubst %.java,%,$(TEST_SOURCES)))

TEST_CLASSPATH := tests$(PSEP)$(BUILD)$(PSEP)$(JUNIT_JAR)$(PSEP)$(CLASSPATH)

TEST_XML_CASES := $(shell find tests/cases -name '*.xml')
TEST_RNC_SCHEMATA := $(shell find tests/cases -name '*.rnc')
TEST_RNG_SCHEMATA := $(patsubst %.rnc,%.rng,$(TEST_RNC_SCHEMATA))

buildtest: CLASSPATH = $(TEST_CLASSPATH)
buildtest: ALL_JAVAC_FLAGS = $(JAVAC_FLAGS) -d tests -cp $(CLASSPATH)
buildtest: nofiles $(TEST_CLASSES) compilefiles $(TEST_RNG_SCHEMATA)

test: buildtest
	$(JVM) $(ALL_JVM_FLAGS) -cp $(TEST_CLASSPATH) \
	    org.junit.runner.JUnitCore $(TESTS) | tee test-log.txt
	@echo Transcript of test run saved to test-log.txt

%.rng: %.rnc
	$(TRANG) $^ $@

%.txt: %.rng
	$(JVM) $(ALL_JVM_FLAGS) -cp $(BUILD) \
	       org.kohsuke.bali.Driver -ot $^ >$@

# Useful for interactive runs: `make jvm` blah blah

jvm:
	@echo $(JVM) $(ALL_JVM_FLAGS) -cp $(TEST_CLASSPATH)

################################ Packaging

predist: $(ALL_SOURCES) $(AUX_FILES) $(DOCS_HTML)

dist: 
	REPODIR=$$PWD darcs dist --dist-name $(NAME_VER)

jar: $(NAME_VER).jar

$(NAME_VER).jar: compile manifest.txt
	$(JAR) cfm $@ manifest.txt -C build . 

manifest.txt: Makefile
	echo Main-Class: $(PACKAGE).Driver >$@

net/contrapunctus/rngzip/version.txt: LICENSE
	echo $(NAME) $(VERSION) >$@
	cat LICENSE >>$@

net/contrapunctus/rngzip/context.txt:
	(cd $${REPODIR:-$$PWD}; darcs changes --context) >$@

# for maintainer only: push changes up to web site
DARCS_BRANCH = trunk
DARCS_STAGING = $(HOME)/Sites/$(NAME)
DARCS_DEST = contrapunctus.net:public_html/dist/$(NAME)

darcs-put:
	darcs put -v --no-pristine-tree $(DARCS_STAGING)/$(DARCS_BRANCH)

push-sync:
	darcs push
	rsync -av --include='/$(DARCS_BRANCH)/' \
	          --include='/$(DARCS_BRANCH)/_darcs/' \
	          --include='/$(DARCS_BRANCH)/_darcs/**' \
	          --exclude='*' \
	      $(DARCS_STAGING)/ $(DARCS_DEST)

################################ Documentation

docs: $(DOCS_HTML)

api-docs: $(SOURCES)
	javadoc -d doc/api $(ALL_JAVADOC_FLAGS) $^

all-api-docs: $(ALL_SOURCES)
	javadoc -d $(dir $@) $(ALL_JAVADOC_FLAGS) $^

MARKDOWN = libs/markdown/Markdown.pl
doc/%.html: % doc/head doc/foot
	(cat doc/head; perl $(MARKDOWN) <$*; cat doc/foot) \
	  | recode -d u8..h4 >$@

################################ Cleanliness

# Like clean, but may refrain from deleting a few files that people
# normally don't want to recompile.  We leave behind all the library
# classes in the build/ directory, and the API documentation.
mostlyclean:
	$(RM) -r $(BUILD)/net
	$(RM) $(subst $$,\$$,$(shell find tests -name '*.class'))
	$(RM) $(TEST_RNG_SCHEMATA) 
	$(RM) $(patsubst %.xml,%.rnz,$(TEST_XML_CASES))
	$(RM) $(patsubst %.xml,%.xin,$(TEST_XML_CASES))
	$(RM) $(patsubst %.xml,%.xout,$(TEST_XML_CASES))
	$(RM) $(patsubst %.rnc,%.txt,$(TEST_RNC_SCHEMATA))
	$(RM) test-log.txt files manifest.txt *~

# Delete files that are normally created by building the program.
# Also preserve files that could be made by building, but normally
# aren't, because the distribution comes with them.
clean: mostlyclean
	$(RM) -r $(BUILD) doc/api
	$(RM) $(NAME_VER).jar

# Delete files that are created by configuring or building the
# program.  Leave only the files that were in the distribution.
distclean: clean

# Delete almost everything that con be reconstructed with this
# Makefile.  This includes the output of javaCC.  This should leave
# behind only things that are in the repository.
maintainer-clean: distclean
	$(RM) $(PARSER_FILES) $(AUX_GEN) $(DOCS_HTML)
