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

CLASSPATH = 

NAME = rngzip
PACKAGE = net.contrapunctus.$(NAME)
VERSION = 0.6
NAME_VER = $(NAME)-$(VERSION)

LIBRARIES := bali iso-relax msv relaxng-datatype gnu-getopt \
    commons-compress colloquial lzma
SOURCEPATH := net
BUILD = build

SOURCES := $(shell find $(SOURCEPATH) -name '_darcs' -prune -false -o -name '*.java')

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
	test buildtest junit buildjunit bench public predist jar
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

TEST_CLASSPATH := tests$(PSEP)libs/junit$(PSEP)$(BUILD)$(PSEP)$(CLASSPATH)

JUNIT_SOURCES := $(shell find libs/junit -name '*.java' | $(STRIP_PATH))
JUNIT_CLASSES := $(addprefix libs/junit/,$(patsubst %.java,%.class,$(JUNIT_SOURCES)))

buildjunit: CLASSPATH = $(TEST_CLASSPATH)
buildjunit: ALL_JAVAC_FLAGS = $(JAVAC_FLAGS) -cp $(CLASSPATH)
buildjunit: nofiles $(JUNIT_CLASSES) compilefiles

junit:
	$(MAKE) buildjunit

TEST_SOURCES := $(shell find ./tests -name '*.java' | $(STRIP_PATH))
TEST_CLASSES := $(addprefix tests/,$(patsubst %.java,%.class,$(TEST_SOURCES)))
TESTS := $(subst /,.,$(patsubst %.java,%,$(TEST_SOURCES)))

buildtest: CLASSPATH = $(TEST_CLASSPATH)
buildtest: ALL_JAVAC_FLAGS = $(JAVAC_FLAGS) -d tests -cp $(CLASSPATH)
buildtest: nofiles $(TEST_CLASSES) compilefiles

TEST_RNC_SCHEMATA := $(shell find tests/cases -name '*.rnc')
TEST_RNG_SCHEMATA := $(patsubst %.rnc,%.rng,$(TEST_RNC_SCHEMATA))

test: junit buildtest $(TEST_RNG_SCHEMATA)
	$(JVM) $(ALL_JVM_FLAGS) -cp $(TEST_CLASSPATH) \
	    org.junit.runner.JUnitCore $(TESTS) | tee test-log.txt
	@echo Transcript of test run saved to test-log.txt

%.rng: %.rnc
	$(TRANG) $^ $@

%.rng: %.dtd
	$(TRANG) $^ $@

%.txt: %.rng
	$(JVM) $(ALL_JVM_FLAGS) -cp $(BUILD) \
	       org.kohsuke.bali.Driver -ot $^ >$@

# Useful for interactive runs: `make jvm` blah blah

jvm:
	@echo $(JVM) $(ALL_JVM_FLAGS) -cp $(TEST_CLASSPATH)

################################ Benchmarking

XMLLINT = xmllint
XMLPPM = ../../xmlppm-0.98.2/src/xmlppm.`arch`
TIME = /usr/bin/time

BENCH_DTDS := $(shell find tests -name 'schema.dtd')
BENCH_SCHEMATA := $(patsubst %.dtd,%.rng,$(BENCH_DTDS))

BENCH_DOCS := $(shell find tests -name 'ex*.xml')
BENCH_CHECK := $(patsubst %.xml,%.valid,$(BENCH_DOCS))
BENCH_ZIPPED := \
	$(patsubst %.xml,%.xml.nb,$(BENCH_DOCS)) \
	$(patsubst %.xml,%.xml.gz,$(BENCH_DOCS)) \
	$(patsubst %.xml,%.xml.bz2,$(BENCH_DOCS)) \
	$(patsubst %.xml,%.xml.ppm,$(BENCH_DOCS))
BENCH_OUTPUTS := $(patsubst %.xml,%.rnz,$(BENCH_DOCS))

bench: junit buildtest $(BENCH_SCHEMATA) \
	$(BENCH_CHECK) $(BENCH_ZIPPED) $(BENCH_OUTPUTS) \
	$(patsubst %.xml,%.summary,$(BENCH_DOCS))

%.xml.nb: %.xml
	$(XMLLINT) --noblanks $< >$@

%.xml.gz: %.xml.nb
	$(TIME) gzip -c $< >$@

%.xml.bz2: %.xml.nb
	$(TIME) bzip2 -c $< >$@

%.xml.ppm: %.xml.nb
	$(TIME) $(XMLPPM) <$< >$@

%.valid: %.xml
	$(XMLLINT) --relaxng `dirname $@`/schema.rng --noout $< >$@

%.rnz: %.xml
	$(TIME) $(JVM) $(ALL_JVM_FLAGS) -cp $(TEST_CLASSPATH) \
	    net.contrapunctus.rngzip.Benchmarks $< | tee $@

%.summary: %.valid
	du -b $*.* | sort -n >$@

################################ Packaging

predist: $(ALL_SOURCES) $(AUX_FILES) $(DOCS_HTML)

dist: $(NAME_VER).tar.gz

$(NAME_VER).tar.gz:
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


DARCS_DEST = comsci.liu.edu:public_html/dist/$(NAME)
DARCS_BRANCH = trunk

public: $(NAME_VER).jar $(NAME_VER).tar.gz
	scp $^ $(DARCS_DEST)

darcs-put:
	darcs put $(DARCS_DEST)/$(DARCS_BRANCH)

darcs-push:
	darcs push $(DARCS_DEST)/$(DARCS_BRANCH)

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

# Get rid of auxiliary files from benchmarking and testing.
sortaclean:
	$(RM) $(BENCH_CHECK) $(addsuffix .*,$(BENCH_DOCS))
	$(RM) $(patsubst %.rnc,%.txt,$(TEST_RNC_SCHEMATA))
	$(RM) test-log.txt files manifest.txt *~

# Like clean, but may refrain from deleting a few files that people
# normally don't want to recompile.  We leave behind all the library
# classes in the build/ directory, and the API documentation.
mostlyclean: sortaclean
	$(RM) -r $(BUILD)/net
	$(RM) $(TEST_RNG_SCHEMATA) tests/entrezgene/*.rng
	$(RM) $(subst $$,\$$,$(shell find tests -name '*.class'))

# Delete files that are normally created by building the program.
# Also preserve files that could be made by building, but normally
# aren't, because the distribution comes with them.
clean: mostlyclean
	$(RM) -r $(BUILD) doc/api
	$(RM) $(BENCH_SCHEMATA) $(BENCH_OUTPUTS)
	$(RM) $(subst $$,\$$,$(shell find libs/junit -name '*.class'))
	$(RM) $(NAME_VER).jar

# Delete files that are created by configuring or building the
# program.  Leave only the files that were in the distribution.
distclean: clean

# Delete almost everything that con be reconstructed with this
# Makefile.  This includes the output of javaCC.  This should leave
# behind only things that are in the repository.
maintainer-clean: distclean
	$(RM) $(PARSER_FILES) $(AUX_GEN) $(DOCS_HTML)
