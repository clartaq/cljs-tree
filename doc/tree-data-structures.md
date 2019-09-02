Tree Data Structures
cljs-tree implements and manipulates a tree structure of arbitrary width and depth.

The top level of the tree is a `vector` of top-level "topics" or "headlines" or "rows". A `vector` us used since it maintains the order of its elements, something that is obviously important in such a control.

Each topic consists of a map containing at least one key, `:topic`, and an associated value, which is the text of the topic. Topics with empty or `nil` values are eliminated from the tree during the course of normal processing.

Each topic can have additional keys/values including:

- `:expanded` a "truthy" value indicating whether any existing tree of child nodes should be expanded or not. The presence or absence of the `:expanded` key does not imply the existence of children.
- `:children` with a value of a `vector` containing zero or more topics that are the children of the current, parent topic. the structure of the children `vector` and topics is exactly the same as described above.

Again, any child can have an arbitrary number of sibling or child topics.

At present, the data displayable to the user consists of the textual `:topic` values.

There is nothing to prevent additional elements from being added and displayed in the tree. In fact, that is the plan. For example, each node could contain the group of "tags" or "links" as leafs to be displayed in a special way or to receive clicks or act as drop targets.

All that is really needed to structure the tree are the vectors of sibling topic maps and the vectors of children topics.
