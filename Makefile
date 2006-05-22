# Makefile -- requires GNU make

default: build

.PHONY: default build rebuild nofiles allfiles buildfiles doc
.DELETE_ON_ERROR:

################################ Paths and files

BUILD = build
CLASSPATH = $(BUILD)

LIBRARIES := bali iso-relax msv relaxng-datatype

LIBRARIES := $(addprefix libs/,$(LIBRARIES))
SOURCES := $(shell find $(LIBRARIES) -name '*.java' | sed 's:^libs/[-a-z]*/::')
CLASSES := $(patsubst %.java,%.class,$(SOURCES))

vpath %.class $(BUILD)
vpath %.java $(LIBRARIES)

################################ Programs and options

JAVAC = javac
JVM = java
JAVADOC = javadoc
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
	  $(JAVAC) $(ALL_JAVACFLAGS) @files && echo Ok.; \
	else echo Nothing to do.; fi
	@$(RM) files

%.class: %.java
	@echo Compiling $?
	@echo $? >> files

################################ Documentation

doc: doc/api/index.html

doc/api/index.html: $(SOURCES)
	javadoc -d $(dir $@) -encoding UTF-8 -charset UTF-8 $(JAVADOCFLAGS) $^

################################ Packaging

################################ Cleanliness

