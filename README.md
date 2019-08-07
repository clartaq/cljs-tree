# cljs-tree

An experiment with hierarchical data in ClojureScript.

## Overview

`cljs-tree` is a small experiment in how to use a hierarchical data structure in
ClojureScript. It implements a simple outliner/tree control. When run, the program will display a small sample outline in your default browser. (I've tested with Safari, Firefox, Opera and Brave, all on a Mac.) The outline is fully editable. The commands accepted by the outliner are described below. This demo does not support saving changes or exporting the modified outline. (The additional work to do so would move it beyond the "experimental" stage.)

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

From you project directory, clone the repository.

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

## Build and Run a "Production" Version

To clean all compiled files:

    lein clean

To create a production build run:

    lein fig:min

And open your browser in `resources/public/index.html`. You will not
get live reloading, nor a REPL. 

### Testing

The repository contains some unit tests. To run them

```
    lein fig:test
```

The rest results will appear in the console.

## Usage

This demo lets you manipulate a small sample outline. (Please forgive any odd coloring of the demo. I sometimes use altered CSS to help me see exactly how certain HTML elements are arranged in the demo.)

When the page is initially loaded, it will show an outline with some subtopics expanded while other are collapsed. There is a button at the bottom of the page that will add, move, and remove a sub-tree of additional data. Try that first.

Once satisfied that the rock data manipulation works, start adding to, modifying, and removing the outline. You can always restore the outline to its original state by reloading the page.

### Available Commands

- **Select a Headline for Editing**: To edit a headline, just click on it and make your changes. Headlines may be any length. The headline will wrap as needed to accomodate long headlines.
- **Moving Up and Down**: Use the up arrow and down arrow keys to move the editing focus up and down the outline. Moving to a collapsed headline will leave it in a collapsed state. Likewise, moving to a headline that is already expanded will not change the expansion state.
- **Adding a New Headline**: To add a new headline, click any existing headline and press `Return`. A new headline will be inserted below the current headline and the editing focus will be move to the new headline.

    If the existing headline already has sub-headlines, the new headline will also appear as a sub-headling. If the existing headline does not have any sub-headings, the new headline will be created as a "sibling" of the existing headline.

- **Promoting/Un-Indenting and Demoting/Indenting Headlines**: An existing headline can be indented by pressing the `Tab` key. A headline can be un-indented by holding down the `Shift` key then pressing the `Tab` key.

    Headlines can only be indented one level below their parent headline. They can, of course, have additional sub-headlines that are further indented.

    Headlines cannot be un-indented further than the top-level headlines in the outline.

- **Expand/Collapse Branches**: If a headline has a chevron next to it, you can toggle expanding or collapsing the branch by clicking the chevron.
- **Deleting Characters**: Pressing the "Delete" key will delete characters in front of the caret (towards the end of the outline.) Pressing the "Backspace" key will delete characters behind the caret (towards the beginning of the outline.)

    Completely deleting a headline will also delete any sub-headings it may have had.

    The entire outline can be deleted by placing the editing caret before the first character in the top-most headline and repeatedly pressing the "Delete" key.

    Likewise, placing the caret at the end of the last visible headline and repreatedly pressing the backspace key can erase the entire outline one character at a time.

    You cannot delete the last remaining headline. You can delete its contents but not the editing area of the headline.

## Documentation

Some technical documentation, including a description of the tree data structure, is included in the `docs` directory.

## To Do

- Allow deletion of entire headlines at once.
- Allow headlines to be reorganized by moving branches up and down in the hierarch.
- Provide a keystroke shortcut to toggle headline expansions for single headlines and for the entire outline at once.

## License

Copyright © 2019 David D. Clark

Distributed under the [Eclipse Public License](http://www.eclipse.org/legal/epl-v10.html) either version 1.0 or (at your option) any later version.
