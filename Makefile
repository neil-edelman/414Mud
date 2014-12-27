# Makefile 1.1 (GNU Make 3.81; MacOSX gcc 4.2.1; MacOSX MinGW 4.3.0)

PROJ  := 414Mud
VA    := 1
VB    := 1
E     := entities
M     := main
C     := common
FILES := $(C)/Orcish $(C)/BoundedReader $(C)/TextReader $(C)/BitVector $(C)/Buffer $(C)/Chance $(C)/UnrecognisedTokenException $(M)/Mud $(M)/TestLoader $(M)/Hit $(M)/Loader $(M)/Area $(M)/Connection $(M)/Command $(M)/Mapper $(M)/Hit $(E)/Stuff $(E)/Character $(E)/Player $(E)/Mob $(E)/Object $(E)/Container $(E)/Money $(E)/Room
SDIR  := src
BDIR  := bin
BACK  := backup
ICON  :=
EXTRA := go.sh todo.txt data/* data/areas/*.area data/commandsets/*.cset
INST  := $(PROJ)-$(VA)_$(VB)
OBJS  := $(patsubst %,$(BDIR)/%.class,$(FILES))
SRCS  := $(patsubst %,$(SDIR)/%.java,$(FILES))
#H     := $(patsubst %,$(SDIR)/%.h,$(FILES))

CC   := javac
CF   := -g:none -O -d $(BDIR) $(SDIR)/main/*.java $(SDIR)/$(C)/*.java $(SDIR)/$(E)/*.java -Xlint:unchecked -Xlint:deprecation #-verbose
OF   := # -framework OpenGL -framework GLUT

# props Jakob Borg and Eldar Abusalimov
EMPTY :=
SPACE := $(EMPTY) $(EMPTY)
ifeq (backup, $(firstword $(MAKECMDGOALS)))
  ARGS := $(wordlist 2,$(words $(MAKECMDGOALS)),$(MAKECMDGOALS))
  BRGS := $(subst $(SPACE),_,$(ARGS))
  ifneq (,$(BRGS))
    BRGS := -$(BRGS)
  endif
  $(eval $(ARGS):;@:)
endif

default: $(OBJS)
#default: $(BDIR)/$(PROJ)

#$(BDIR)/$(PROJ): #$(OBJS)
#	$(CC) $(CF) $(OF) $^ #-o $@

$(BDIR)/%.class: $(SDIR)/%.java
	@mkdir -p $(BDIR)
	$(CC) $(CF) $?
#	$(CC) $(CF) -c $? -o $@

.PHONY: setup clean backup

setup: default
	@mkdir -p $(BDIR)/$(INST)
	cp $(BDIR)/$(PROJ) readme.txt gpl.txt copying.txt $(BDIR)/$(INST)
	rm -f $(BDIR)/$(INST)-MacOSX.dmg
	# or rm -f $(BDIR)/$(INST)-Win32.zip
	hdiutil create $(BDIR)/$(INST)-MacOSX.dmg -volname "$(PROJ) $(VA).$(VB)" -srcfolder $(BDIR)/$(INST)
	# or zip $(BDIR)/$(INST)-Win32.zip -r $(BDIR)/$(INST)
	rm -R $(BDIR)/$(INST)

clean:
	-rm -f $(OBJS)

gitback: backup git

backup:
	@mkdir -p $(BACK)
	zip $(BACK)/$(INST)-`date +%Y-%m-%dT%H%M%S`$(BRGS).zip readme.txt Makefile $(SRCS) $(EXTRA)

git:
	git commit -am "$(ARGS)"
