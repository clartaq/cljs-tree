# cljs-tree

An experiment with hierarchical data in ClojureScript.

## Overview

`cljs-tree` is a small experiment in how to use a hierarchical data structure in ClojureScript. It implements a simple outliner/tree control. When run, the program will display a sample outline in your default browser. (I've tested with Safari, Firefox, Opera and Brave, all on a Mac.) The outline is fully editable. The commands accepted by the outliner are described below. This demo does not support saving changes or exporting the modified outline. (The additional work to do so would move it beyond the "experimental" stage.)

## Why

The intent is to use a descendant of this control as an organizational and search tool on a personal wiki. The user should be able to layout the way they think their data is organized in the tree control. The plan is that at some point, they could then click in the gutter (not yet present) along a certain path in the tree and a search would be run based on the path to the node clicked. E.g. a click next to the node "Books/Authors/Kurt Vonnegut Jr." would bring up any information in the wiki about books that Kurt Vonnegut has written. That's in the far future. What will probably happen next is that special leaf nodes will be added for simpler types of searches.

## Setup

You must have a version of Java 1.8 or later on your system. The example program does not run in Java, but the build tooling does.

### Install Clojure

Extensive instructions are available [here](https://www.clojure.org/guides/getting_started).

On the Mac, it is as simple as:

```
brew install clojure
```


### Install Leiningen

This project uses [Leiningen](https://leiningen.org) as the build tool. There are detailed instructions for manual installation on the site. Or, on a Mac:

```
brew install leiningen
```

### Clone the Repository

From your project directory, clone the repository. This repository uses the git DVCS. The repository is on github. This should do the job.

```
git clone https://github.com/clartaq/cljs-tree.git
```

## Development

### Run in an Interactive Development Environment

To get an interactive development environment, `cd` into the project directory, then run:

    lein fig:build

If everything is fine, your default web browser will open  at 
[localhost:9500](http://localhost:9500).
This will auto compile and send all changes to the browser without the
need to reload. After the compilation process is complete, you will
get a browser-connected REPL. An easy way to try it is:

    (js/alert "Am I connected?")

and you should see an alert in the browser window.

Any changes you make to functionality in the REPL or by making changes to a source file will be reflected (nearly) instantaneously in the browser window.

Running this setup also will start running unit tests automatically. To check the status of unit tests, open a browser tab to 
`http://localhost:9500/figwheel-extra-main/auto-testing`.

## Build and Run a "Production" Version

To clean all compiled files:

    lein clean

To create a production build run:

    lein fig:min

And open your browser in `resources/public/index.html`. You will not
get live reloading, nor a REPL. 

## Testing

The repository contains some unit tests. To run them once

```
    lein fig:test
```

The test results will appear in the console.

## Usage

This demo lets you manipulate a small sample outline. (Please forgive any odd coloring of the demo. I sometimes use altered CSS to help me see exactly how certain HTML elements are arranged in the demo.)

When the page is initially loaded, it will show an outline with some subtopics expanded while other are collapsed. There are buttons at the bottom that demonstrate some more complicated functionality

When you focus a topic, you can edit it -- the editing area is just a plain old HTML `textarea`. Any keyboard shortcuts that your browser supports can be used, with the exceptions listed below. For example, there is no special "delete-line-contents" shortcut since `Command-Backspace` does that, at least on macOS.

You can always restore the outline to its original state by reloading the page.

### Available Actions

- **Select a Headline for Editing**: To edit a headline, just click on it and make your changes. Headlines may be any length. The headline will wrap as needed to accomodate long headlines.
- **Moving Up and Down**: Use the up arrow and down arrow keys to move the editing focus up and down the outline. Moving to a collapsed headline will leave it in a collapsed state. Likewise, moving to a headline that is already expanded will not change the expansion state.
- **Adding a New Headline**: To add a new headline below an existing one, click any existing headline and press `Return`. A new headline will be inserted below the current headline and the editing focus will be moved to the new headline.

    If the existing headline already has sub-headlines, the new headline will also appear as a sub-headling. If the existing headline does not have any sub-headings, the new headline will be created as a "sibling" of the existing headline, that is, at the same level of indentation.
    
    You can add a new headline _above_ the current headline by pressing `Shift-Return` instead.

    In both cases, the inserted headline will be empty and show a blinking caret ready to accept typing.

- **Promoting/Outdenting and Demoting/Indenting Headlines**: An existing headline can be indented by pressing the `Tab` key while the headline is selected for editing. A headline can be outdented by holding down the `Shift` key then pressing the `Tab` key.

    Headlines can only be indented one level below their parent headline. They can, of course, have additional sub-headlines that are further indented.

    Headlines cannot be outdented further than the top-level headlines in the outline.

- **Moving Headlines Up or Down**: You can move a headline up or down among its siblings using `Option-Command-UpArrow` and `Option-Command-DownArrow`, respectively.

    You can only change the order of *siblings* this way, but when used with the Indent and Outdent functions, you can completely reorganize the outline.

- **Expand/Collapse Branches**: If a headline has a chevron next to it, you can toggle expanding or collapsing the branch by clicking the chevron. The keyboard shortcut `Option-Command-,` will also toggle the expansion state
- **Deleting Characters**: Pressing the "Delete" key will delete characters in front of the caret (towards the end of the outline.) Pressing the `Backspace` key will delete characters behind the caret (towards the beginning of the outline.)

    Completely deleting a headline will also delete any sub-headings it may have had.

    The entire outline can be deleted by placing the editing caret before the first character in the top-most headline and repeatedly pressing the `Delete` key.

    Likewise, placing the caret at the end of the last visible headline and repeatedly pressing the backspace key can erase the entire outline one character at a time.

    You cannot delete the last remaining headline. You can delete its contents but not the editing area of the headline.

- **Split Headline**: You can split a line at the current caret position by pressing `Control-Return`. The caret will remain where it was. If the headline had sub-trees, those will remain with the portion of the headline after the split.

- **Join Headlines**: If the headline containing the editing caret has a sibling below it, pressing `Control-Shift-Return` will join the two headlines. If either or both headlines had sub-trees, they will be combined and will remain in the same order as before the join.
 
- **Deleting a Branch**: You can delete an entire brach of the tree, including sub-trees, by pressing `Command-k` when the top of the branch is focused for editing. As mentioned, this will delete the focused headline _and all of its sub-trees.
    
    If you have experience with some other outliners, they may have a similar operation associated with the `Command-x` key. Other outliners may use this command to "Cut" the text, copying it to the clipboard. When you use `Command-k`, the branch is gone (unless you immediately press `Command-z` to undo the deletion.)

- **Undo/Redo**: The keyboard shortcuts `Command-z` and `Shift-Command-z` can be used to undo and redo changes to the tree. These commands work just like similar commands in other editors. But they can also undo/redo things like the expansion state of a headline, deletion of subtrees, _etc_.

    However, the part of the program that handles undo/redo is a bit feeble in that it will not always show the location where the change was made -- it might be outside of the part of the tree control currently being viewed.

    Likewise, the program may show you states of the tree that you never created yourself. This is because some operations, like moving subtrees, are done in multiple steps that you typically don't see. When you undo some actions, you will see the intermediate steps as well.

**A Note on Keyboard Shortcuts**: It's a bit of a problem coming up with appropriate keyboard commands in an SPA like this that does editing. Some of the choices have effects on the accessibility of the app. In this implmentation, the shortcuts described above are only active when a headline in the tree is focused -- so be a little careful. For example, pressing the `Tab` key when no headline is focused is likely to have different effects. Likewise, `Command-z` and `Shift-Command-z` will not activate undo/redo in the tree unless it is focused and may cause some jarring effects in the browser.

### Buttons

At the bottom of the tree, there are four buttons labeled "Reset", "New", "Save", and "Read."

- **Reset**: Clicking "Reset" will reset the data to the value it had at the start of the session. This is usually (always?) the built-in sample outline.

- **New**: Clicking the "New" button will delete the current contents of the tree and present the user with an empty tree to edit.

- **Save**: Clicking the "Save" button will save the current state of the tree to browser [localStorage](https://developer.mozilla.org/en-US/docs/Web/API/Window/localStorage). It will persist across browser sessions, but is limited in size.

- **Read**: Clicking the "Read" button will read any data stored in `localStorage` and replace the contents of the tree with the data.

## Documentation

Some technical documentation, including a description of the tree data structure, is included in the `doc` directory.

## To Do

- Maybe add a little formatting like bold and italic.
- Branch rearrangement with drag and drop.
- Change undo/redo machinery to use the Command pattern rather than the Memento pattern.
- Pause undo/redo when executing composite actions.
- Make undo/redo a little more "chunky" based on periods of inactivity.
- Investigate any potential accessibility problems caused by the shortcut keys used.
- Clicking `Control-x` on an editor with no selection should cut and delete the same branch. (But how to paste?)
- Check on compatibility of shortcuts with Windows and Linux.
- Convert the keyboard/command relationship into a sequence of maps, then search the sequence rather than using a large `cond`.

## License

Copyright © 2019 - 2020 David D. Clark

Distributed under the [Eclipse Public License](http://www.eclipse.org/legal/epl-v10.html) either version 1.0 or (at your option) any later version.
