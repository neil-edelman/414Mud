# Makefile 1.1 (GNU Make 3.81; MacOSX gcc 4.2.1; MacOSX MinGW 4.3.0)

PROJ  := 414Mud
VA    := 1
VB    := 0
FILES := FourOneFourMud Connection Orcish
SDIR  := src
BDIR  := bin
BACK  := backup
ICON  := go.sh #icon.ico
EXTRA := 
INST  := $(PROJ)-$(VA)_$(VB)
OBJS  := $(patsubst %,$(BDIR)/%.class,$(FILES))
SRCS  := $(patsubst %,$(SDIR)/%.java,$(FILES))
#H     := $(patsubst %,$(SDIR)/%.h,$(FILES))

CC   := javac
CF   := -g:none -O -d $(BDIR) $(SDIR)/*.java -Xlint:unchecked -Xlint:deprecation #-verbose
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
