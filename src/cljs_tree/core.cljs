;;;;
;;;; This namespace is an experiment in creating nested, hierarchical tags
;;;; and generating HTML (hiccup) from those data structures.
;;;;

(ns cljs-tree.core
  (:require
    [cljs-tree.demo-hierarchy :as h]
    [cljs-tree.undo-redo :as ur]
    [cljs-tree.vector-utils :refer [delete-at remove-first remove-last
                                    remove-last-two insert-at replace-at
                                    append-element-to-vector]]
    [clojure.edn :as edn]
    [clojure.string :as s]
    [reagent.core :as r]))

(enable-console-print!)

;;;-----------------------------------------------------------------------------
;;; Global Data and Constants

(defn topic-separator
  "A character that is unlikely to be typed in normal operation. In this case
  it is a half trianglular colon modifier character. Used as a separator when
  building path strings through the hierarchy."
  []
  \u02D1)

(defn root-parts
  "Returns a vector of the components used to build various ids of the root."
  []
  ["root" "0"])

(defn indent-increment
  "Return the amount of indentation (in rems) to add for each level
  down the tree."
  []
  1.5)

(defn new-topic
  "Return the map to be used for a new topic."
  []
  {:topic ""})

;;;-----------------------------------------------------------------------------
;;; Utilities

;;------------------------------------------------------------------------------
;; DOM-related things

(defn get-element-by-id
  [id]
  (.getElementById js/document id))

(defn event->target-element
  [evt]
  (.-target evt))

(defn event->target-id
  [evt]
  (.-id (event->target-element evt)))

(defn event->target-value
  [evt]
  (.-value (event->target-element evt)))

(defn get-caret-position
  "Return the caret position of the text element with the id passed."
  [ele-id]
  (.-selectionStart (get-element-by-id ele-id)))

(defn is-ele-in-visible-area?
  "Return non-nil if the element is within the visible area of the
  scroll port. May not be visible due to CSS settings or if its parent
  is in a collapsed state. NOTE that, unlike many of the functions in
  this section, this function expects a DOM element, not an id."
  [ele]
  (let [r (.getBoundingClientRect ele)
        doc-ele (.-documentElement js/document)
        wdw-height (or (.-innerHeight js/window) (-.clientHeight doc-ele))
        wdw-width (or (.-innerWidth js/window) (.-clientWidth doc-ele))
        result (and (>= (.-left r) 0) (>= (.-top r) 0)
                    (<= (+ (.-left r) (.-width r)) wdw-width)
                    (<= (+ (.-top r) (.-height r)) wdw-height))]
    result))

(defn scroll-ele-into-view
  "Scroll the element with the given id into view. Note: This must be the id
  of a DOM element, not an element in the data tree."
  [ele-id]
  (when-let [ele (get-element-by-id ele-id)]
    (when-not (is-ele-in-visible-area? ele)
      (.scrollIntoView ele))))

(defn style-property-value
  "Return the value of the property for the element with the given id."
  [id property]
  (when-let [style-declaration (.-style (get-element-by-id id))]
    (.getPropertyValue style-declaration property)))

(defn swap-style-property
  "Swap the specified style settings for the two elements."
  [first-id second-id property]
  (let [style-declaration-of-first (.-style (get-element-by-id first-id))
        style-declaration-of-second (.-style (get-element-by-id second-id))
        value-of-first (.getPropertyValue style-declaration-of-first property)
        value-of-second (.getPropertyValue style-declaration-of-second property)]
    (.setProperty style-declaration-of-first property value-of-second)
    (.setProperty style-declaration-of-second property value-of-first)))

(defn swap-display-properties
  "Swap the display style properties for the two elements."
  [first-id second-id]
  (swap-style-property first-id second-id "display"))

(defn resize-textarea
  "Resize the element vertically."
  [text-id]
  (when-let [ele (get-element-by-id text-id)]
    (let [style (.-style ele)]
      (set! (.-overflow style) "hidden")
      (set! (.-height style) "5px")
      (set! (.-height style) (str (.-scrollHeight ele) "px")))))

(defn key-evt->map
  "Unpack the information in a keyboard event into a map that can be used
  easily to dispatch the event to a handler"
  [evt]
  {:key       (.-key evt)
   :modifiers {:ctrl  (.-ctrlKey evt)
               :shift (.-shiftKey evt)
               :alt   (.-altKey evt)
               :cmd   (.-metaKey evt)}})

;;------------------------------------------------------------------------------
;; Just a few miscellaneous utility functions.

(defn convert-to-number-or-not
  "If every character in the string is a digit, convert the string to a
  number and return it. Otherwise, return the string unchanged."
  [s]
  (if (every? #(.includes "0123456789" %) s)
    (js/parseInt s)
    s))

;; From: https://stackoverflow.com/questions/5232350/clojure-semi-flattening-a-nested-sequence
(defn flatten-to-vectors
  "Flatten nested sequences of vectors to a flat sequence of those vectors."
  [s]
  (mapcat #(if (every? coll? %) (flatten-to-vectors %) (list %)) s))

(defn positions
  "Return a list of the index positions of elements in coll that satisfy pred."
  [pred coll]
  (keep-indexed #(when (pred %2) %1) coll))

;;------------------------------------------------------------------------------
;; Tree id manipulation functions. These are all basically string manipulation
;; functions that don't need to do anything with the data in the tree.

(defn tree-id->tree-id-parts
  "Split a DOM id string (as used in this program) into its parts and return
  a vector of the parts. Note that numeric indices in the parts vector are
  actually strings, not numbers."
  [id]
  (when (and id (seq id))
    (s/split id (topic-separator))))

(defn numberize-parts
  "Take a vector of tree id parts and convert any of the parts containing all
  digit characters to numbers. This can be useful with comparing parts vectors
  produced from tree ids to nav vectors produced by one of the tree
  traversal functions."
  [parts]
  (loop [p parts r []]
    (if (empty? p)
      r
      (recur (rest p) (conj r (convert-to-number-or-not (first p)))))))

(defn tree-id-parts->tree-id-string
  "Return a string formed by interposing the topic-separator between the
  elements of the input vector."
  [v]
  (when (and v (vector? v) (seq v))
    (str (s/join (topic-separator) v))))

(defn is-top-tree-id?
  "Return true if tree-id represents to first sibling at the root level of
  the tree. (This topic is always displayed at the top of the tree -- hence
  the function name.)"
  [tree-id]
  (= (root-parts) (remove-last (tree-id->tree-id-parts tree-id))))

(defn id-of-first-child
  "Return the expected id of the first child of the node with this id. There
  is no guarantee that an actual tree node with the id exists."
  [tree-id]
  (let [id-parts (remove-last (tree-id->tree-id-parts tree-id))
        new-parts (conj (conj id-parts 0) "topic")]
    (tree-id-parts->tree-id-string new-parts)))

(defn nav-index-vector->tree-id-string
  "Creates a DOM id string from a vector of indices used to navigate to
  the topic. If no id type is specified, the default value of 'topic'
  is used."
  [nav-index-vector & [type-to-use]]
  (let [id-type (or type-to-use "topic")]
    (str "root" (topic-separator)
         (tree-id-parts->tree-id-string nav-index-vector)
         (topic-separator) id-type)))

(defn tree-id->nav-index-vector
  "Return a vector of the numeric indices in the child vectors from the
  root to the element id."
  [tree-id]
  (-> (tree-id->tree-id-parts tree-id)
      (remove-last)
      (remove-first)))

(defn is-summit-id?
  "Return true if tree-id represents a member of the top-level of topics."
  [tree-id]
  (= 1 (count (tree-id->nav-index-vector tree-id))))

(defn tree-id->parent-id
  "Return the id of the parent of this id or nil if the id is already a
  summit id. Returns nil if the tree-id is the top-most summit node."
  [tree-id]
  (when-not (is-summit-id? tree-id)
    (let [parts (tree-id->tree-id-parts tree-id)
          id-type (last parts)]
      (tree-id-parts->tree-id-string (conj (remove-last-two parts) id-type)))))

(defn tree-id->sortable-nav-string
  "Convert the element id to a string containing the vector indices
  separated by a hyphen and return it. Result can be used to lexicographically
  determine if one element is 'higher' or 'lower' than another in the tree."
  [tree-id]
  (s/join "-" (tree-id->nav-index-vector tree-id)))

(defn set-leaf-index
  "Return a new version of the tree-id where the leaf index has been set to
  a new value."
  [tree-id new-index]
  (let [parts (tree-id->tree-id-parts tree-id)
        index-in-vector (- (count parts) 2)
        new-parts (replace-at parts index-in-vector new-index)]
    (tree-id-parts->tree-id-string new-parts)))

(defn increment-leaf-index-by
  "Given the tree id of a leaf node, return a version of the same
  tree id with the leaf index incremented by the given value."
  [tree-id by]
  (let [parts (tree-id->tree-id-parts tree-id)
        index-in-vector (- (count parts) 2)
        new-index (+ (int (nth parts index-in-vector)) by)
        new-parts (replace-at parts index-in-vector new-index)]
    (tree-id-parts->tree-id-string new-parts)))

(defn increment-leaf-index
  "Given the tree id of a leaf node, return an id with the node index
  incremented."
  [tree-id]
  (let [parts (tree-id->tree-id-parts tree-id)
        index-in-vector (- (count parts) 2)
        inc-index (inc (int (nth parts index-in-vector)))
        new-parts (replace-at parts index-in-vector inc-index)]
    (tree-id-parts->tree-id-string new-parts)))

(defn decrement-leaf-index
  "Given the tree id of a leaf node, return an id with the node index
  decremented. Can produce leaf indices < 0, which some functions
  depend on."
  [tree-id]
  (let [parts (tree-id->tree-id-parts tree-id)
        index-in-vector (- (count parts) 2)
        dec-index (dec (int (nth parts index-in-vector)))
        new-parts (replace-at parts index-in-vector dec-index)]
    (tree-id-parts->tree-id-string new-parts)))

(defn change-tree-id-type
  "Change the 'type' of a tree DOM element id to something else."
  [id new-type]
  (let [parts (tree-id->tree-id-parts id)
        shortened (remove-last parts)]
    (str (tree-id-parts->tree-id-string shortened) (str (topic-separator) new-type))))

(defn insert-child-index-into-parent-id
  "Return a new id where the index of the child in the parents children vector
  has been appended."
  [parent-id child-index]
  (-> (tree-id->tree-id-parts parent-id)
      (remove-last)
      (conj child-index)
      (conj "topic")
      (tree-id-parts->tree-id-string)))

(defn tree-id->tree-path-nav-vector
  "Return a vector of indices and keywords to navigate to the piece of data
  represented by the DOM element with the given id."
  [tree-id]
  (let [nav-vector (mapv int (tree-id->nav-index-vector tree-id))
        interposed (interpose :children nav-vector)]
    (vec interposed)))

(defn tree-id->nav-vector-and-index
  "Parse the id into a navigation path vector to the parent of the node and an
  index within the vector of children. Return a map containing the two pieces
  of data. Basically, parse the id into a vector of information to navigate
  to the parent (a la get-in) and the index of the child encoded in the id."
  [tree-id]
  (let [string-vec (tree-id->tree-id-parts tree-id)
        idx (int (nth string-vec (- (count string-vec) 2)))
        without-last-2 (remove-last-two string-vec)
        without-first (delete-at without-last-2 0)
        index-vector (mapv int without-first)
        interposed (interpose :children index-vector)]
    {:path-to-parent (vec interposed) :child-index idx}))

(defn editing?
  "Return true if the element with the given id is in the editing state."
  [id]
  (let [editor-id (change-tree-id-type id "editor")]
    (= (style-property-value editor-id "display") "initial")))

(defn lower?
  "Return true if the first path is 'lower' in the tree than second path."
  [first-path second-path]
  (pos? (compare (tree-id->sortable-nav-string first-path)
                 (tree-id->sortable-nav-string second-path))))

;;------------------------------------------------------------------------------
;; Functions to query and manipulate the tree and subtrees.

(defn tree->nav-vector-sequence
  "Return a sequence of (possibly nested) navigation vectors for all the nodes
  in the tree that satisfy the predicate. The sequence is generated from an
  'in-order' traversal."
  [tree so-far pred]
  (letfn [(helper [my-tree my-id-so-far]
            (map-indexed (fn [idx ele]
                           (let [new-id (conj my-id-so-far idx)]
                             (if-not (pred ele)
                               new-id
                               (cons new-id (helper (:children ele) new-id)))))
                         my-tree))]
    (helper tree so-far)))

(defn all-nodes
  "Return a sequence of vectors of the numerical indices used to travel from
  the root to each node in the tree. Includes nodes that may not be visible
  at the moment."
  [tree so-far]
  (flatten-to-vectors
    (tree->nav-vector-sequence tree so-far :children)))

(defn last-node-in-tree
  "Return the tree-id of the last node in the tree."
  [root-ratom]
  (tree-id-parts->tree-id-string
    (conj (last (all-nodes @root-ratom ["root"])) "topic")))

(defn has-visible-children?
  "Return true if the topic is expanded and has children."
  [topic-map]
  (and (:children topic-map) (:expanded topic-map)))

(defn visible-nodes
  "Return a sequence of vectors of the numerical indices used to travel from
  the root to each visible node."
  [tree so-far]
  (flatten-to-vectors
    (tree->nav-vector-sequence tree so-far has-visible-children?)))

(defn last-visible-node-in-tree
  "Return the tree-id of the last visible node in the tree."
  [root-ratom]
  (tree-id-parts->tree-id-string
    (conj (last (visible-nodes @root-ratom ["root"])) "topic")))

(defn is-top-visible-tree-id?
  "Return the same result as is-top-tree-id? since the top of the tree is
  always visible."
  [_ tree-id]
  (is-top-tree-id? tree-id))

(defn is-bottom-visible-tree-id?
  "Return true if the node with the given id is the bottom visible node in
  the tree; false otherwise."
  [root-ratom tree-id]
  (= tree-id (tree-id-parts->tree-id-string
               (conj (last (visible-nodes @root-ratom ["root"])) "topic"))))

(defn get-topic
  "Return the topic map at the requested id. Return nil if there is
  nothing at that location."
  [root-ratom topic-id]
  (get-in @root-ratom (tree-id->tree-path-nav-vector topic-id)))

(defn has-children?
  "Return the entire vector of children if present, nil otherwise."
  [root-ratom topic-id]
  (:children (get-topic root-ratom topic-id)))

(defn count-children
  "Return the number of children of the topic."
  [root-ratom topic-id]
  (count (has-children? root-ratom topic-id)))

(defn where-to-append-next-child
  "Return the location (tree id) where the next sibling should be added to
  a parent. That position is one below the last child or the first child if
  the parent has no children."
  [root-ratom parent-id]
  (let [number-of-children (count-children root-ratom parent-id)
        first-child (id-of-first-child parent-id)]
    (set-leaf-index first-child number-of-children)))

(defn expanded?
  "Return true if the subtree is in the expanded state (implying that it
  has children). Returns nil if the subtree is not expanded."
  [root-ratom tree-id]
  (:expanded (get-topic root-ratom tree-id)))

(defn expand-node!
  "Assure that the node is expanded."
  [root-ratom tree-id]
  (let [nav-vector (tree-id->tree-path-nav-vector tree-id)
        my-cursor (r/cursor root-ratom nav-vector)]
    (swap! my-cursor assoc :expanded true)))

(defn collapse-node!
  "Assure that the node is collapsed."
  [root-ratom tree-id]
  (let [nav-vector (tree-id->tree-path-nav-vector tree-id)
        my-cursor (r/cursor root-ratom nav-vector)]
    (swap! my-cursor assoc :expanded nil)))

(defn toggle-node-expansion!
  "Toggle the 'expanded' setting for the node. When the branch has no
  :expanded key, does nothing."
  [root-ratom tree-id]
  (let [nav-vector (tree-id->tree-path-nav-vector tree-id)
        my-cursor (r/cursor root-ratom nav-vector)]
    (when (seq (select-keys @my-cursor [:expanded]))
      (swap! my-cursor update :expanded not))))

(defn highlight-and-scroll-editor-for-id
  "Focus the editor associated with the id (assumes that the label associated
  with the id is visible). If needed scroll the editor into view. Select
  the region represented by begin-highlight and end-highlight."
  [tree-id begin-highlight end-highlight]
  (when tree-id
    (let [editor-id (change-tree-id-type tree-id "editor")
          editor-ele (get-element-by-id editor-id)]
      (when-not (editing? editor-id)
        (let [label-id (change-tree-id-type tree-id "label")]
          (swap-display-properties label-id editor-id)))
      (.focus editor-ele)
      (scroll-ele-into-view editor-id)
      (.setSelectionRange editor-ele begin-highlight end-highlight))))

(defn focus-and-scroll-editor-for-id
  "Focus the editor associated with the id (assumes that the label associated
  with the id is visible). If needed, scroll the editor into view. If a caret
  position is provided, place the editor caret at that position."
  [tree-id & [caret-pos]]
  (when tree-id
    (let [editor-id (change-tree-id-type tree-id "editor")
          editor-ele (get-element-by-id editor-id)]
      (when-not (editing? editor-id)
        (let [label-id (change-tree-id-type tree-id "label")]
          (swap-display-properties label-id editor-id)))
      (.focus editor-ele)
      (scroll-ele-into-view editor-id)
      (when caret-pos
        (.setSelectionRange editor-ele caret-pos caret-pos)))))

(defn id-of-previous-sibling
  "Return the id of the previous sibling of this tree id. Returns nil if this
  tree id is the first (zero'th) in a group of siblings."
  [current-sibling-id]
  (let [parts (tree-id->tree-id-parts current-sibling-id)
        last-path-index (int (nth parts (- (count parts) 2)))]
    (when (pos? last-path-index)
      (tree-id-parts->tree-id-string
        (into (remove-last-two parts) [(dec last-path-index) "topic"])))))

(defn siblings-above
  "Return a (possibly empty) seq of siblings that appear higher in the tree
  display than the one denoted by the tree-id."
  [root-ratom tree-id]
  (loop [id (decrement-leaf-index tree-id) res []]
    (if (nil? (get-topic root-ratom id))
      res
      (recur (decrement-leaf-index id) (conj res id)))))

(defn siblings-below
  "Return a (possibly empty) seq of siblings that appear lower in the tree
  display than the one denoted by tree-id."
  [root-ratom tree-id]
  (loop [id (increment-leaf-index tree-id) res []]
    (if (nil? (get-topic root-ratom id))
      res
      (recur (increment-leaf-index id) (conj res id)))))

(defn id-of-last-visible-child
  "Return the id of the last visible child of the branch starting at tree-id.
  The last visible child may be many levels deeper in the tree."
  [root-ratom tree-id]
  (loop [id-so-far tree-id
         topic-map (get-topic root-ratom id-so-far)]
    (if-not (has-visible-children? topic-map)
      id-so-far
      (let [next-child-vector (:children topic-map)
            next-index (dec (count next-child-vector))
            next-topic (get next-child-vector next-index)
            next-id (insert-child-index-into-parent-id id-so-far next-index)]
        (recur next-id next-topic)))))

(defn previous-visible-node
  "Return the tree id of the visible node one line up."
  [root-ratom current-node-id]
  (let [id-parts (remove-last (tree-id->tree-id-parts current-node-id))
        last-part (js/parseInt (last id-parts))
        short-parts (remove-last id-parts)
        new-id (if (zero? last-part)
                 ; The first child under a parent.
                 (tree-id-parts->tree-id-string (conj short-parts "topic"))
                 (id-of-last-visible-child
                   root-ratom
                   (tree-id-parts->tree-id-string
                     (conj (conj short-parts (dec last-part)) "topic"))))]
    new-id))

(defn brute-force-next-visible-node
  "Return the next visible node in the tree after the current id.  Return
  nil if the tree-id already corresponds to the last visible node in the
  tree. This method should only be called if no simplifying conditions exist
  to identify the needed node more easily."
  [root-ratom tree-id]
  (when-not (is-bottom-visible-tree-id? root-ratom tree-id)
    (let [vis-nav-vector-seq (flatten-to-vectors
                               (tree->nav-vector-sequence
                                 @root-ratom []
                                 has-visible-children?))
          nav-parts (numberize-parts (tree-id->nav-index-vector tree-id))
          inc-matched-nav (inc (first (positions (fn [x] (= x nav-parts)) vis-nav-vector-seq)))
          complete-parts (conj (into ["root"] (nth vis-nav-vector-seq inc-matched-nav)) "topic")]
      (tree-id-parts->tree-id-string complete-parts))))

(defn next-visible-node
  "Return the next visible node in the tree after the current node. Returns
  nil if the node is already the last visible node."
  [root-ratom current-node-id]
  (let [current-topic (get-topic root-ratom current-node-id)
        ; Pre-calculate one of the easy possibilities.
        next-sibling-id (increment-leaf-index current-node-id)]
    (cond
      (has-visible-children? current-topic) (id-of-first-child current-node-id)
      (get-topic root-ratom next-sibling-id) next-sibling-id
      :default (brute-force-next-visible-node root-ratom current-node-id))))

(defn remove-top-level-sibling!
  "Remove one of the top level topics from the tree. Return a copy of the
  branch (entire tree) with the sibling removed or nil if there was a problem
  with the arguments. Will not remove the last remaining top-level headline."
  [root-ratom sibling-index]
  (when (and (or (instance? reagent.ratom/RAtom root-ratom)
                 (instance? reagent.ratom/RCursor root-ratom))
             (vector @root-ratom)
             ; Don't delete the last remaining top-level topic.
             (> (count @root-ratom) 1)
             (>= sibling-index 0)
             (< sibling-index (count @root-ratom)))
    (swap! root-ratom delete-at sibling-index)))

(defn remove-child!
  "Remove the specified child from the parents vector of children. Return a
  copy of the branch with the child removed or nil if there was a problem
  with the arguments."
  [parent-ratom child-index]
  (when (or (instance? reagent.ratom/RAtom parent-ratom)
            (instance? reagent.ratom/RCursor parent-ratom))
    (let [vector-of-children (:children @parent-ratom)]
      (when (and vector-of-children
                 (vector? vector-of-children)
                 (>= child-index 0)
                 (< child-index (count vector-of-children)))
        (let [new-child-vector (delete-at vector-of-children child-index)]
          (if (empty? new-child-vector)
            (swap! parent-ratom dissoc :children)
            (swap! parent-ratom assoc :children new-child-vector)))))))

(defn prune-topic!
  "Remove the subtree with the given id from the tree. If the last child
  is deleted, the subtree is marked as having no children."
  ; THE RETURN VALUE IS INCONSISTENT HERE DEPENDING ON WHETHER A TOP LEVEL
  ; ITEM IS DELETED OR ONE LOWER IN THE TREE.
  [root-ratom id-of-existing-subtree]
  (let [path-and-index (tree-id->nav-vector-and-index id-of-existing-subtree)
        parent-nav-index-vector (:path-to-parent path-and-index)
        child-index (:child-index path-and-index)]
    (if (empty? parent-nav-index-vector)
      (remove-top-level-sibling! root-ratom child-index)
      (let [child-vector-target (r/cursor root-ratom parent-nav-index-vector)]
        (remove-child! child-vector-target child-index)))))

;; This is such a dirty hack! It requires special handling if the first
;; argument is actually the root because the root is a vector, not a map.
;; It all boils down to the choice we made to make the root different so
;; we don't have an always present "root" node at the top of the control.
(defn add-child!
  "Insert the given topic at the specified index in the parents vector of
  children. Return a new copy of the parent that includes the new data."
  [parent-topic-ratom index topic-to-add]
  (if (vector? @parent-topic-ratom)
    (swap! parent-topic-ratom insert-at index topic-to-add)
    (let [child-topic-vector (or (:children @parent-topic-ratom) [])
          new-child-vector (insert-at child-topic-vector index topic-to-add)]
      (swap! parent-topic-ratom assoc :children new-child-vector))))

(defn graft-topic!
  "Add a new topic at the specified location in the tree. The topic is inserted
  into the tree. No data it removed. Any existing information at the location
  where the new data is grafted is pushed down in the tree."
  [root-ratom id-of-desired-node topic-to-graft]
  (let [path-and-index (tree-id->nav-vector-and-index id-of-desired-node)]
    (add-child! (r/cursor root-ratom (:path-to-parent path-and-index))
                (:child-index path-and-index) topic-to-graft)))

(defn move-branch!
  "Move an existing branch to a new location."
  [root-ratom source-id destination-id]
  (let [topic-to-move (get-topic root-ratom source-id)
        id-to-focus (change-tree-id-type destination-id "label")]
    (if (lower? source-id destination-id)
      (do (prune-topic! root-ratom source-id)
          (graft-topic! root-ratom destination-id topic-to-move))
      (do (graft-topic! root-ratom destination-id topic-to-move)
          (prune-topic! root-ratom source-id)))
    (scroll-ele-into-view id-to-focus)))

(defn indent-branch!
  "Indent the given branch and return its new id. If the branch cannot be
  indented, return nil."
  [root-ratom branch-id]
  (when-not (is-top-tree-id? branch-id)
    (when-let [previous-sibling (id-of-previous-sibling branch-id)]
      (expand-node! root-ratom previous-sibling)
      (let [sibling-child-count (count-children root-ratom previous-sibling)
            sibling-parts (tree-id->tree-id-parts previous-sibling)
            with-added-leaf (conj (remove-last sibling-parts) sibling-child-count)
            demoted-prefix (tree-id-parts->tree-id-string with-added-leaf)
            demoted-id (str demoted-prefix (topic-separator) "topic")]
        (move-branch! root-ratom branch-id demoted-id)
        demoted-id))))

(defn outdent-branch!
  "Outdent (promote) the given branch and return its new id."
  [root-ratom branch-id]
  (when-not (is-summit-id? branch-id)
    (let [parts (tree-id->nav-index-vector branch-id)
          less-parts (remove-last parts)
          promoted-id (increment-leaf-index (nav-index-vector->tree-id-string less-parts))
          siblings-to-move (reverse (siblings-below root-ratom branch-id))]
      (when (seq siblings-to-move)
        (expand-node! root-ratom branch-id)
        (let [where-to-append (where-to-append-next-child root-ratom branch-id)]
          (loop [child (first siblings-to-move) siblings (rest siblings-to-move)]
            (when child
              (move-branch! root-ratom child where-to-append)
              (recur (first siblings) (rest siblings))))))
      (move-branch! root-ratom branch-id promoted-id)
      promoted-id)))

(defn outdent-all-children!
  "Outdent (promote) all the children of the given node."
  [root-ratom span-id & [children]]
  (let [child-array (or children (has-children? root-ratom span-id))
        first-id (id-of-first-child span-id)]
    ;; Runs from the bottom child to the top. Doing it from top to bottom
    ;; would "capture" lower children under the higher children.
    (doseq [idx (range (dec (count child-array)) -1 -1)]
      (let [nxt-id (set-leaf-index first-id idx)]
        (outdent-branch! root-ratom nxt-id)))
    span-id))

;;;-----------------------------------------------------------------------------
;;; Functions to handle keystroke events. Editing commands.
;;;
;;; NOTE that most of these functions actually mutate the tree of data, but
;;; do not follow the convention of having the function name end with an
;;; exclamation point.

(defn delete-one-character-backward
  "Handle the special case where the current headline has no more characters.
  Delete it and any children, then move the editor focus to the headline
  above it. Will not delete the last remaining top-level headline."
  [{:keys [root-ratom evt topic-ratom span-id]} & [caret-pos]]
  (when (zero? (count @topic-ratom))
    (.preventDefault evt)
    (let [previous-visible-topic-id (previous-visible-node root-ratom span-id)]
      (when-let [previous-topic-value (get-topic root-ratom previous-visible-topic-id)]
        (let [caret-position (or caret-pos (count (:topic previous-topic-value)))
              previous-visible-editor-id (change-tree-id-type previous-visible-topic-id "editor")]
          (prune-topic! root-ratom span-id)
          (when (get-element-by-id previous-visible-editor-id)
            (focus-and-scroll-editor-for-id previous-visible-topic-id caret-position)))))))

(defn delete-one-character-forward
  "Handle the special case where there are no more characters in the headline.
  In that case the headline will be deleted and the focus will move to the
  previous visible node. Will not delete the last remaining top-level node."
  [{:keys [root-ratom evt topic-ratom span-id] :as args}]
  (when (zero? (count @topic-ratom))
    (.preventDefault evt)
    (if-let [children (has-children? root-ratom span-id)]
      (do
        (when (expanded? root-ratom span-id)
          (outdent-all-children! root-ratom span-id children))
        (prune-topic! root-ratom span-id)
        ;; Did we delete all of the children that might have advanced to the
        ;; same id as span-id. That is, was it the last visible branch in the
        ;; tree?
        (let [node-to-focus (if (lower? span-id (last-node-in-tree root-ratom))
                              (last-visible-node-in-tree root-ratom)
                              span-id)]
          (r/after-render
            (fn []
              (focus-and-scroll-editor-for-id node-to-focus 0)))))
      ;; else
      (if-let [next-topic-id (if-not (empty? (siblings-below root-ratom span-id))
                               span-id
                               (next-visible-node root-ratom span-id))]
        (let [next-topic-editor-id (change-tree-id-type next-topic-id "editor")]
          (prune-topic! root-ratom span-id)
          (r/after-render
            (fn []
              (focus-and-scroll-editor-for-id next-topic-editor-id 0))))
        (delete-one-character-backward args 0)))))

(defn indent
  "Indent the current headline one level."
  [{:keys [root-ratom evt span-id]}]
  (.preventDefault evt)
  (let [editor-id (change-tree-id-type span-id "editor")
        caret-position (get-caret-position editor-id)]
    (when-let [demoted-id (indent-branch! root-ratom span-id)]
      (r/after-render
        (fn []
          (focus-and-scroll-editor-for-id demoted-id caret-position))))))

(defn outdent
  "Outdent the current headline one level."
  [{:keys [root-ratom evt span-id]}]
  (.preventDefault evt)
  (let [editor-id (change-tree-id-type span-id "editor")
        caret-position (get-caret-position editor-id)]
    (when-let [promoted-id (outdent-branch! root-ratom span-id)]
      (r/after-render
        (fn []
          (focus-and-scroll-editor-for-id promoted-id caret-position))))))

(defn move-headline-up
  "Move the current headline up one position in its group of siblings."
  [{:keys [root-ratom evt span-id]}]
  (.preventDefault evt)
  (let [siblings-above (siblings-above root-ratom span-id)]
    (when (pos? (count siblings-above))
      (let [editor-id (change-tree-id-type span-id "editor")
            caret-position (get-caret-position editor-id)
            new-id (first siblings-above)
            new-editor-id (change-tree-id-type new-id "editor")]
        (move-branch! root-ratom span-id new-id)
        (focus-and-scroll-editor-for-id new-editor-id caret-position)))))

(defn move-headline-down
  "Move the current headline down one position in its group of siblings."
  [{:keys [root-ratom evt span-id]}]
  (.preventDefault evt)
  (let [siblings-below (siblings-below root-ratom span-id)]
    (when (pos? (count siblings-below))
      (let [editor-id (change-tree-id-type span-id "editor")
            caret-position (get-caret-position editor-id)
            new-id (increment-leaf-index-by span-id 2)
            new-editor-id (change-tree-id-type (increment-leaf-index span-id) "editor")]
        (move-branch! root-ratom span-id new-id)
        (focus-and-scroll-editor-for-id new-editor-id caret-position)))))

(defn move-focus-up-one-line
  "Move the editor and focus to the next higher up visible headline."
  [{:keys [root-ratom evt span-id]}]
  (.preventDefault evt)
  (when-not (is-top-visible-tree-id? root-ratom span-id)
    (let [editor-id (change-tree-id-type span-id "editor")
          saved-caret-position (.-selectionStart (get-element-by-id editor-id))
          previous-visible-topic (previous-visible-node root-ratom span-id)]
      (focus-and-scroll-editor-for-id previous-visible-topic saved-caret-position))))

(defn move-focus-down-one-line
  "Move the editor and focus to the next lower down visible headline."
  [{:keys [root-ratom evt span-id]}]
  (.preventDefault evt)
  (when-not (is-bottom-visible-tree-id? root-ratom span-id)
    (let [editor-id (change-tree-id-type span-id "editor")
          saved-caret-position (.-selectionStart (get-element-by-id editor-id))
          next-visible-topic (next-visible-node root-ratom span-id)]
      (focus-and-scroll-editor-for-id next-visible-topic saved-caret-position))))

(defn insert-new-headline-below
  "Insert a new headline in the tree above the currently focused one and leave
   the placeholder text highlighted ready to be overwritten when the user
   starts typing."
  [{:keys [root-ratom evt span-id]}]
  ; If the topic span has children, add a new child in the zero-position
  ; Else add a new sibling below the current topic
  (.preventDefault evt)
  (let [id-of-new-child (if (expanded? root-ratom span-id)
                          (insert-child-index-into-parent-id span-id 0)
                          (increment-leaf-index span-id))
        new-headline (new-topic)
        num-chars (count (:topic new-headline))]
    (graft-topic! root-ratom id-of-new-child new-headline)
    (r/after-render
      (fn [] (highlight-and-scroll-editor-for-id id-of-new-child 0 num-chars)))))

(defn insert-new-headline-above
  "Insert a new headline above the current headline, pushing the current
  headline down. Leave the new topic placeholder text highlighted ready to
  be overwritten when the user starts typing."
  [{:keys [root-ratom evt span-id]}]
  (.preventDefault evt)
  (let [new-headline (new-topic)
        num-chars (count (:topic new-headline))]
    (graft-topic! root-ratom span-id new-headline)
    (r/after-render
      (fn [] (highlight-and-scroll-editor-for-id span-id 0 num-chars)))))

(defn delete-branch
  "Delete the branch specified, including all of its children."
  [{:keys [root-ratom evt span-id]}]
  (.preventDefault evt)
  (prune-topic! root-ratom span-id))

(defn split-headline
  "Split the headline at the caret location. Text to the left of the caret
  will remain at the existing location. Text to the right of the caret (and
  any children) will appear as a new sibling branch below the existing
  headline."
  ;; I can make different arguments for whether the caret should be left at
  ;; the end of the top headline or moved to the beginning of the new branch.
  ;; Went with top headline for now.
  [{:keys [root-ratom evt span-id]}]
  (.preventDefault evt)
  (when-let [ele (get-element-by-id (change-tree-id-type span-id "editor"))]
    (let [sel-end (.-selectionEnd ele)
          existing-topic (get-topic root-ratom span-id)
          topic-text (:topic existing-topic)
          text-before (s/trimr (s/join (take sel-end topic-text)))
          cnt (count text-before)
          text-after (s/triml (s/join (drop sel-end topic-text)))
          headline-above {:topic text-before}
          branch-below (assoc existing-topic :topic text-after)]
      (prune-topic! root-ratom span-id)
      (graft-topic! root-ratom span-id branch-below)
      (graft-topic! root-ratom span-id headline-above)
      (r/after-render
        #(focus-and-scroll-editor-for-id span-id cnt)))))

(defn join-headlines
  "Joins the current headline with the sibling branch below it."
  [{:keys [root-ratom evt span-id]}]
  (.preventDefault evt)
  (let [id-below (increment-leaf-index span-id)]
    (when-let [branch-below (get-topic root-ratom id-below)]
      (let [top-topic (get-topic root-ratom span-id)
            cnt (count top-topic)
            new-headline (str (:topic top-topic) " " (:topic branch-below))
            with-new-headline (assoc top-topic :topic new-headline)
            with-children (if (or (:children top-topic) (:children branch-below))
                            (let [exp-state (or (:expanded top-topic) (:expanded branch-below))
                                  startv (or (:children top-topic) [])
                                  children (into startv (map identity (:children branch-below)))]
                              (-> with-new-headline
                                  (assoc :children children)
                                  (assoc :expanded exp-state)))
                            with-new-headline)]
        (prune-topic! root-ratom span-id)
        (prune-topic! root-ratom span-id)
        (graft-topic! root-ratom span-id with-children)
        (focus-and-scroll-editor-for-id span-id cnt)))))

(defn toggle-headline-expansion
  "Toggle the expansion state of the current headline."
  [{:keys [root-ratom evt span-id]}]
  (.preventDefault evt)
  (toggle-node-expansion! root-ratom span-id))

(defn- def-mods
  "Return a map containing the default values for keyboard modifiers."
  []
  {:ctrl false :alt false :shift false :cmd false})

(defn- merge-def-mods
  "Merge a map of modifiers (containing any modifiers which should be present)
  with a default map of false values for all modifiers."
  [m]
  (merge (def-mods) m))

(defn handle-key-down
  "Handle key-down events and dispatch them to the appropriate handlers."
  [root-ratom evt topic-ratom span-id]
  (let [km (key-evt->map evt)
        args {:root-ratom  root-ratom
              :evt         evt
              :topic-ratom topic-ratom
              :span-id     span-id}]
    ;(println "km: " km)
    (cond

      (= km {:key "Enter" :modifiers (merge-def-mods {:shift true})})
      (insert-new-headline-above args)

      (= km {:key "Enter" :modifiers (def-mods)})
      (insert-new-headline-below args)

      (= km {:key "Enter" :modifiers (merge-def-mods {:ctrl true})})
      (split-headline args)

      (= km {:key "Enter" :modifiers (merge-def-mods {:ctrl true :shift true})})
      (join-headlines args)

      (= km {:key "k" :modifiers (merge-def-mods {:cmd true})})
      (delete-branch args)

      (= km {:key "Delete" :modifiers (def-mods)})
      (delete-one-character-forward args)

      (= km {:key "Backspace" :modifiers (def-mods)})
      (delete-one-character-backward args)

      (= km {:key "Tab" :modifiers (merge-def-mods {:shift true})})
      (outdent args)

      (= km {:key "Tab" :modifiers (def-mods)})
      (indent args)

      (= km {:key "ArrowUp" :modifiers (merge-def-mods {:alt true :cmd true})})
      (move-headline-up args)

      (= km {:key "ArrowDown" :modifiers (merge-def-mods {:alt true :cmd true})})
      (move-headline-down args)

      (= km {:key "ArrowUp" :modifiers (def-mods)})
      (move-focus-up-one-line args)

      (= km {:key "ArrowDown" :modifiers (def-mods)})
      (move-focus-down-one-line args)

      ;; Option-Command-, despite what the :key looks like
      (= km {:key "≤" :modifiers (merge-def-mods {:cmd true :alt true})})
      (toggle-headline-expansion args)

      :default nil)))

(defn handle-keydown-for-tree-container
  "Handle undo!/redo! for the tree container."
  [evt root-ratom um]
  (let [km (key-evt->map evt)]
    (cond
      (= km {:key "z" :modifiers (merge-def-mods {:cmd true})})
      (do
        (.preventDefault evt)
        (when (ur/can-undo? um)
          (let [active-ele-id (.-id (.-activeElement js/document))]
            (ur/undo! um)
            (when-not (expanded? root-ratom (tree-id->parent-id active-ele-id))
              (focus-and-scroll-editor-for-id (previous-visible-node root-ratom active-ele-id))))))

      (= km {:key "z" :modifiers (merge-def-mods {:cmd true :shift true})})
      (do
        (.preventDefault evt)
        (when (ur/can-redo? um)
          (let [active-ele-id (.-id (.-activeElement js/document))]
            (ur/redo! um)
            (when-not (expanded? root-ratom (tree-id->parent-id active-ele-id))
              (focus-and-scroll-editor-for-id (previous-visible-node root-ratom active-ele-id))))))

      :default nil)))

;;;-----------------------------------------------------------------------------
;;; Buttons for the demo.

(defn add-reset-button
  "Return a function that will create a button that, when clicked, will undo
  all changes made to the tree since the program was started."
  [app-state-ratom]
  (let [button-id "reset-button"
        reset-fn (fn [_]
                   (let [um (:undo-redo-manager @app-state-ratom)]
                     (while (ur/can-undo? um)
                       (ur/undo! um))))]
    (fn [app-state-ratom]
      [:input.tree-demo--button
       {:type     "button"
        :id       button-id
        :title    "Reset the tree to its original state"
        :value    "Reset"
        :on-click #(reset-fn %)}])))

(defn add-new-button
  "Return a function that will produce a button that, when clicked,
  will delete the current contents of the control and replace it with a
  fresh, empty version."
  [app-state-ratom]
  (let [button-id "new-button"
        my-cursor (r/cursor app-state-ratom [:tree])
        empty-tree [(new-topic)]
        empty-tree-id (tree-id-parts->tree-id-string (conj (root-parts) "editor"))
        new-fn (fn [_]
                 (reset! my-cursor empty-tree)
                 (r/after-render
                   (fn []
                     (resize-textarea empty-tree-id)
                     (highlight-and-scroll-editor-for-id
                       empty-tree-id 0
                       (count (:topic (first @my-cursor)))))))]
    (fn [app-state-ratom]
      [:input.tree-demo--button
       {:type     "button"
        :id       button-id
        :title    "Remove all contents from the tree control and start anew"
        :value    "New"
        :on-click #(new-fn %)}])))

(defn add-save-button
  "Return a fuction that will produce a button that, when clicked,
  will save the current state of the tree in local storage."
  [app-state-ratom]
  (let [button-id "save-button"
        save-fn (fn [_] (.setItem (.-localStorage js/window) "tree"
                                  (pr-str (:tree @app-state-ratom))))]
    (fn [app-state-ratom]
      [:input.tree-demo--button
       {:type     "button"
        :id       button-id
        :title    "Save the current state of the tree"
        :value    "Save"
        :on-click #(save-fn %)}])))

(defn add-read-button
  "Return a fuction that will produce a button that, when clicked,
  will read the saved state of the tree in local storage."
  [app-state-ratom]
  (let [button-id "read-button"
        read-fn (fn [_]
                  (when-let [data (.getItem (.-localStorage js/window) "tree")]
                    (let [edn (edn/read-string data)]
                      (swap! app-state-ratom assoc :tree edn))))]
    (fn [app-state-ratom]
      [:input.tree-demo--button
       {:type     "button"
        :id       button-id
        :title    "Read the saved tree from storage"
        :value    "Read"
        :on-click #(read-fn %)}])))

(defn add-buttons
  "Adds buttons to the button bar."
  [app-state-ratom]
  (fn [app-state-ratom]
    [:div.tree-demo--button-area
     [add-reset-button app-state-ratom]
     [add-new-button app-state-ratom]
     [add-save-button app-state-ratom]
     [add-read-button app-state-ratom]]))

;;;-----------------------------------------------------------------------------
;;; Functions to build the control.

(defn handle-chevron-click!
  "Handle the click on the expansion chevron by toggling the state of
  expansion in the application state atom. This will cause the tree
  to re-render visually."
  [root-ratom evt]
  (let [ele-id (event->target-id evt)
        kwv (tree-id->tree-path-nav-vector ele-id)
        ekwv (conj kwv :expanded)]
    (swap! root-ratom update-in ekwv not)
    (focus-and-scroll-editor-for-id ele-id)))

(defn indent-div [indent-id]
  (let [id-v (tree-id->nav-index-vector indent-id)
        indent (* (indent-increment) (dec (count id-v)))
        indent-style (str 0 " " 0 " " indent "rem")]
    ^{:key indent-id}
    [:div#indent-id.tree-control--indent-div {:style {:flex indent-style}}]))

(defn chevron-div
  "Get the expansion symbol to be used at the front of a topic. Returns
  a result based on whether the tree has children, and if so, whether they
  are expanded or not."
  [root-ratom subtree-ratom chevron-id]
  (let [want-bullets true
        bullet-opacity (if want-bullets "0.7" "0.0")
        base-attrs {:class "tree-control--chevron-div"
                    :id    chevron-id}
        clickable-chevron-props (merge base-attrs
                                       {:on-click #(handle-chevron-click! root-ratom %)})
        invisible-chevron-props (merge base-attrs {:style {:opacity bullet-opacity}})
        es (cond
             (has-visible-children? @subtree-ratom) [:div clickable-chevron-props
                                                     (str \u25BC \space)]
             (:children @subtree-ratom) [:div clickable-chevron-props
                                         (str \u25BA \space)]
             ; Headlines with no children can be displayed with or without
             ; a bullet depending on the setting of "want-bullets" above.
             :default [:div invisible-chevron-props (str \u25cf \space)])]
    es))

(defn topic-info-div
  "Build the textual/interactive part of a topic/headline."
  [root-ratom sub-tree-ratom ids-for-row]
  (let [topic-ratom (r/cursor sub-tree-ratom [:topic])
        label-id (:label-id ids-for-row)
        editor-id (:editor-id ids-for-row)
        topic-id (:topic-id ids-for-row)]
    [:div.tree-control--topic-info-div
     [:label.tree-control--topic-label
      {:id      label-id
       :style   {:display :initial}
       :for     editor-id
       ; debugging
       ;:onMouseOver #(println "topic-id: " topic-id ", label-id: " label-id ", editor-id: " editor-id)
       :onClick (fn [e]
                  (let [ed-ele (get-element-by-id editor-id)
                        ofs (.-focusOffset (.getSelection js/window))]
                    (swap-display-properties label-id editor-id)
                    (.focus ed-ele)
                    (.setSelectionRange ed-ele ofs ofs)
                    (.stopPropagation e)))}
      @topic-ratom]

     [:textarea.tree-control--topic-editor
      {:id           editor-id
       :style        {:display :none}
       :autoComplete "off"
       :onKeyDown    #(handle-key-down root-ratom % topic-ratom topic-id)
       :onKeyUp      #(resize-textarea editor-id)
       :onFocus      (fn on-focus [evt]
                       ; Override default number of rows (2).
                       (resize-textarea editor-id)
                       (.stopPropagation evt))
       :onBlur       #(swap-display-properties label-id editor-id)
       :onChange     #(reset! topic-ratom (event->target-value %))
       :value        @topic-ratom}]]))

(defn dom-ids-for-row
  "Return a map of all of the ids used in building a row of the control."
  [parts]
  (let [row-id-parts (conj parts "row")
        row-id (tree-id-parts->tree-id-string row-id-parts)]
    {:row-id     row-id
     :indent-id  (change-tree-id-type row-id "indent")
     :chevron-id (change-tree-id-type row-id "chevron")
     :topic-id   (change-tree-id-type row-id "topic")
     :label-id   (change-tree-id-type row-id "label")
     :editor-id  (change-tree-id-type row-id "editor")}))

(defn outliner-row-div
  "Return one row of the outliner."
  [root-ratom index-vector]
  (let [ids-for-row (dom-ids-for-row index-vector)
        row-id (:row-id ids-for-row)
        indent-id (:indent-id ids-for-row)
        chevron-id (:chevron-id ids-for-row)
        nav-path (tree-id->tree-path-nav-vector row-id)
        subtree-ratom (r/cursor root-ratom nav-path)]
    ^{:key row-id}
    [:div.tree-control--row-div
     [indent-div indent-id]
     [chevron-div root-ratom subtree-ratom chevron-id]
     [topic-info-div root-ratom subtree-ratom ids-for-row]]))

(defn tree->hiccup
  "Return a div containing all of the visible content of the tree based on
  the current state of the tree."
  [root-ratom]
  (fn [root-ratom]
    (let [nav-vectors (visible-nodes @root-ratom ["root"])]
      (into [:div.tree-control--list]
            (map #(outliner-row-div root-ratom %) nav-vectors)))))

(defn home
  "Return a function to layout the home (only) page."
  [app-state-ratom]
  (let [root-ratom (r/cursor app-state-ratom [:tree])
        um (ur/undo-manager root-ratom)]
    (swap! app-state-ratom assoc :undo-redo-manager um)
    (fn [app-state-ratom]
      [:div.page
       [:div.title-div
        [:h1 "cljs-tree"]
        [:h3 "Some experiments with hierarchical data."]]
       [:div.tree-demo--container
        [:div.tree-control--container-div
         {:onKeyDown #(handle-keydown-for-tree-container % root-ratom um)}
         [tree->hiccup root-ratom]]
        [add-buttons app-state-ratom]]])))

(defn start []
  (r/render-component [home h/test-hierarchy]
                      (get-element-by-id "app")))

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
  )
