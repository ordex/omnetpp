#==========================================================================
#  TEXTEDIT.TCL -
#            part of the GNED, the Tcl/Tk graphical topology editor of
#                            OMNeT++
#   By Andras Varga
#==========================================================================

#----------------------------------------------------------------#
#  Copyright (C) 1992,98 Andras Varga
#  Technical University of Budapest, Dept. of Telecommunications,
#  Stoczek u.2, H-1111 Budapest, Hungary.
#
#  This file is distributed WITHOUT ANY WARRANTY. See the file
#  `license' for details on this and other legal matters.
#----------------------------------------------------------------#

# $keywords: list of highlighted NED keywords
#
# Problematic keywords: "in:" and "out:" (contain special chars)
# This is all one line, cannot be broken into several lines!
#
set keywords {include|import|network|module|simple|channel|delay|error|datarate|for|do|true|false|ref|ancestor|input|const|sizeof|endsimple|endmodule|endchannel|endnetwork|endfor|parameters|gates|gatesizes|in:|out:|submodules|connections|display|on|like|machines|to|if|index|nocheck|numeric|string|bool|anytype}


# configureEditor --
#
# Create tags and bondings for NED editor text widget.
#
proc configureEditor {w} {

    $w tag configure KEYWORD -foreground #a00000
    $w tag configure STRING  -foreground #008000
    $w tag configure COMMENT -foreground #808080

    $w tag configure SELECT -back #808080 -fore #ffffff

puts "dbg: syntax highlight: <paste> should update the whole file"

    bind $w <Key> {
        %W tag remove SELECT 0.0 end
        after idle {syntaxHighlight %W {insert linestart - 1 lines} {insert lineend}}
    }
    bind $w <Button-1> {
        %W tag remove SELECT 0.0 end
    }
    bind $w <Control-f> {editFind}
    bind $w <Control-F> {editFind}
    bind $w <Control-g> {editReplace}
    bind $w <Control-G> {editReplace}
}

# syntaxHightlight --
#
# Applies NED syntax highlight to the text widget passed.
# Should be used like this:
#   bind $w <Key> {after idle {syntaxHighlight %W 1.0 end}}
#
proc syntaxHighlight {w startpos endpos} {

    #
    # BUG: if the end of a string constant falls into a comment,
    # highlighting will be wrong...
    #
    global keywords

    $w tag remove KEYWORD $startpos $endpos
    $w tag remove COMMENT $startpos $endpos
    $w tag remove STRING  $startpos $endpos

    # string constants...
    set cur $startpos
    while 1 {
        set cur [$w search -count length -regexp {"[^"]*"} $cur $endpos]
        if {$cur == ""} {
            break
        }
        $w tag add STRING $cur "$cur + $length char"
        set cur [$w index "$cur + $length char"]
    }

    # keywords...
    set cur $startpos
    while 1 {
        set cur [$w search -count length -regexp $keywords $cur $endpos]
        if {$cur == ""} {
            break
        }

        if {[$w compare $cur == "$cur wordstart"] && \
            [$w compare "$cur + $length char" == "$cur wordend"]} {
            $w tag add KEYWORD $cur "$cur + $length char"
        }
        set cur [$w index "$cur + $length char"]
    }

    # comments...
    set cur $startpos
    while 1 {
        set cur [$w search -count length -regexp {//.*$} $cur $endpos]
        if {$cur == ""} {
            break
        }
        $w tag add COMMENT $cur "$cur + $length char"
        set cur [$w index "$cur + $length char"]
    }
}

# findReplaceDialog --
#
# mode is either find or replace
#
proc findReplaceDialog {w mode} {

    global tmp

    # set tmp(case-sensitive)  $prefs(editor-case-sensitive)
    # set tmp(whole-words)     $prefs(editor-whole-words)
    # set tmp(regexp)          $prefs(editor-regexp)

    # create dialog with OK and Cancel buttons
    if {$mode == "find"} {
        set title "Find"
    } else {
        set title "Find/Replace"
    }
    createOkCancelDialog .dlg $title
    wm transient .dlg [winfo toplevel [winfo parent .dlg]]

    # add entry fields
    label-entry .dlg.f.find "Find string:"
    pack .dlg.f.find  -expand 0 -fill x -side top

    if {$mode == "replace"} {
        label-entry .dlg.f.repl "Replace with:"
        pack .dlg.f.repl  -expand 0 -fill x -side top
    }

    checkbutton .dlg.f.regexp -text {regular expression} -variable tmp(regexp)
    pack .dlg.f.regexp  -anchor w -side top

    checkbutton .dlg.f.case -text {case sensitive} -variable tmp(case-sensitive)
    pack .dlg.f.case  -anchor w -side top

    checkbutton .dlg.f.words -text {whole words only} -variable tmp(whole-words)
    pack .dlg.f.words  -anchor w -side top

    focus .dlg.f.find.e

    # exec the dialog, extract its contents if OK was pressed, then delete dialog
    if {[execOkCancelDialog .dlg] == 1} {
        set findstring [.dlg.f.find.e get]

        set case $tmp(case-sensitive)
        set words $tmp(whole-words)
        set regexp $tmp(regexp)

        if {$mode == "find"} {
            destroy .dlg
            doFind $w $findstring $case $words $regexp
        } else {
            set replstring [.dlg.f.repl.e get]
            destroy .dlg
            doReplace $w $findstring $replstring $case $words $regexp
        }
   }
   catch {destroy .dlg}
}

# doFind --
#
# Finds the given string, positions the cursor after its last char,
# and returns the length. Returns empty string and shows a dialog
# if not found.
#
proc doFind {w findstring case words regexp} {

    # remove previous highlights
    $w tag remove SELECT 0.0 end

    # find the string
    set cur insert
    while 1 {
        if {$case && $regexp} {
            set cur [$w search -count length -regexp -- $findstring $cur end]
        } elseif {$case} {
            set cur [$w search -count length -- $findstring $cur end]
        } elseif {$regexp} {
            set cur [$w search -count length -nocase -regexp -- $findstring $cur end]
        } else {
            set cur [$w search -count length -nocase -- $findstring $cur end]
        }
        if {$cur == ""} {
            break
        }
        if {!$words} {
            break
        }
        if {[$w compare $cur == "$cur wordstart"] && \
            [$w compare "$cur + $length char" == "$cur wordend"]} {
            break
        }
        set cur "$cur + 1 char"
    }

    # check if found
    if {$cur == ""} {
        tk_messageBox  -title "Find" -icon warning -type ok -message "No more '$findstring'."
        return ""
    }

    # highlight it and return length
    $w tag add SELECT $cur "$cur + $length chars"
    $w mark set insert "$cur + $length chars"
    $w see insert

    return $length
}

# askReplaceYesNo --
#
#
proc askReplaceYesNo {w} {

    global result

    catch {destroy .dlg}
    toplevel .dlg
    wm title .dlg "Find/Replace"
    wm protocol .dlg WM_DELETE_WINDOW { }
    wm transient .dlg [winfo toplevel [winfo parent .dlg]]

    set bbox [$w bbox insert]
    if {[llength $bbox] == 4} {
        set x [expr [winfo rootx $w] + [lindex $bbox 0] - 100 ]
        set y [expr [winfo rooty $w] + [lindex $bbox 1] + 40 ]
        wm geometry .dlg "+$x+$y"
    }

    frame .dlg.x
    label .dlg.x.bm -bitmap question
    label .dlg.x.l  -text "Replace this occurrence?"

    frame .dlg.f
    button .dlg.f.yes -text "Yes" -underline 0 \
        -command {set result yes ; destroy .dlg}
    button .dlg.f.no -text "No" -underline 0 \
        -command {set result no; destroy .dlg}
    button .dlg.f.all -text "All" -underline 0 \
        -command {set result all; destroy .dlg}
    button .dlg.f.close -text "Close" -underline 0 \
        -command {set result close; destroy .dlg}

    pack .dlg.x -side top  -fill x
    pack .dlg.x.bm -side left -padx 5  -pady 5
    pack .dlg.x.l -side top -fill x -expand 1
    pack .dlg.f -side bottom -anchor w
    pack .dlg.f.yes .dlg.f.no .dlg.f.all .dlg.f.close -side left -padx 5 -pady 5

    bind .dlg <y> {.dlg.f.yes invoke}
    bind .dlg <Y> {.dlg.f.yes invoke}
    bind .dlg <n> {.dlg.f.no invoke}
    bind .dlg <N> {.dlg.f.no invoke}
    bind .dlg <a> {.dlg.f.all invoke}
    bind .dlg <A> {.dlg.f.all invoke}
    bind .dlg <c> {.dlg.f.close invoke}
    bind .dlg <C> {.dlg.f.close invoke}
    bind .dlg <Enter> {.dlg.f.yes invoke}
    bind .dlg <Escape> {.dlg.f.close invoke}
    focus .dlg.f.yes

    tkwait variable result
    return $result
}

# doReplace --
#
#
proc doReplace {w findstring replstring case words regexp} {

    set doall 0
    while 1 {
        # find occurrence
        set length [doFind $w $findstring $case $words $regexp]
        if {$length == ""} {
            return
        }

        # ask whether to replace it
        if {$doall} {
            set action yes
        } else {
            set action [askReplaceYesNo $w]
        }
        if {$action == "all"} {
            set action yes
            set doall 1
        }
        case $action in {
            yes {
                $w delete "insert - $length char" insert
                $w insert insert $replstring
                syntaxHighlight $w 1.0 end  ;# brute force...
            }
            no {}
            close {
                return
            }
        }
    }
}


