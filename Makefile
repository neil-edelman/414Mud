# Makefile 1.1 (GNU Make 3.81; MacOSX gcc 4.2.1; MacOSX MinGW 4.3.0)

PROJ  := 414Mud
VA    := 1
VB    := 1
E     := entities
M     := main
FILES := $(M)/FourOneFourMud $(M)/Area $(M)/Connection $(M)/Commandset $(M)/Orcish $(M)/BoundedReader $(M)/TextReader $(E)/Stuff $(E)/Character $(E)/Player $(E)/Mob $(E)/Object $(E)/Container $(E)/Money $(E)/Room
SDIR  := src
BDIR  := bin
BACK  := backup
ICON  :=
EXTRA := go.sh $(BDIR)/data/* $(BDIR)/data/areas/*.area
INST  := $(PROJ)-$(VA)_$(VB)
OBJS  := $(patsubst %,$(BDIR)/%.class,$(FILES))
SRCS  := $(patsubst %,$(SDIR)/%.java,$(FILES))
#H     := $(patsubst %,$(SDIR)/%.h,$(FILES))

CC   := javac
CF   := -g:none -O -d $(BDIR) $(SDIR)/main/*.java $(SDIR)/$(E)/*.java -Xlint:unchecked -Xlint:deprecation #-verbose $(SDIR)/gamelogic/*.java
OF   := # -framework OpenGL -framework GLUT

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

backup:
	@mkdir -p $(BACK)
	zip $(BACK)/$(INST)-`date +%Y-%m-%dT%H%M%S`.zip readme.txt Makefile $(SRCS) $(EXTRA)
