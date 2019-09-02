The undo/redo functionality in cljs-tree is rather simplistic but better than nothing.

Since the headlines themselves are just HTML `texture`s, they have some undo/redo capability built in. But the undo/redo machinery in cljs-tree subsumes and extends those capabilities.

Since the tree is a Clojure `atom` (actually a `reagent/atom`), it can be "watched" for any changes in state. And that's how undo/redo is done. On any change in state of the tree atom, the old state is recorded in a stack like structure. Undo simply retrieves the state from the stack (saving the old "current" in another stack used for redo) and makes it the current state. The change in state triggers a re-render by Reagent, thus updating the view.

This method has the advantage of being very simple. It captures every change to the tree state, every character typed, every branch expansion/collapse, every deletion or movement of a branch.

It also has a number of disadvantages.

- Since every change causes the state of the entire tree to be saved, it could be very memory intensive. Since Clojure does "structure sharing", it may not be an issue, but I haven't thoroughly investigated it.

- Changes in the position of the cursor are not recorded.

- Since only the tree state is preserved, some appropriate visual changes are not recorded. For example, if a branch was deleted, and the user scrolls the display such that the deletion point is no longer visible, an undo operation will re-insert the deleted branch, but the user won't see that since they are scrolled to a position where the location of the branch is not visible.

- The undo/redo is extremely fine grained. Every single character change is recorded. It needs to be more "chunky". For example, if the user places the cursor at the end of the tree and deletes the tree one character at a time, undo would also recover the tree one character at a time. It seems like recovering the whole tree or larger parts of it at a time would be preferable.

- Operations which are implemented as a series of transformations record the results of each transformation. When played backwards, the user sees the result of each step, which can be very confusing. For example, moving a headline up or down makes use of the `move-branch!` function which prunes the old topic and grafts it in a new place. And those operations do not always occur in the same order. A use single stepping the undo operation might see a state where a part of their tree is completely gone or where it is duplicated in two different places.

Sometimes undo/redo is implemented using the "Command" pattern, where every saved change is the result of applying a command. Undo then "undoes" the command wholesale.

This seems like a good idea. IntelliJ has _very_ good undo/redo and I'm sure that's how it does it. (I should check the source of their Community Edition to see if I can tell.)
  