;;;;
;;;; This namespace is an experiment in creating nested, hierarchical tags
;;;; and generating HTML (hiccup) from those data structures.
;;;;

(ns cljs-tree.core
  (:require
    ;[cljs.pprint :as ppr]
    [cljs-tree.vector-utils :refer [delete-at remove-first remove-last
                                    remove-last-two insert-at replace-at
                                    append-element-to-vector]]
    [clojure.string :as s]
    [reagent.core :as r]))

(enable-console-print!)

;;;-----------------------------------------------------------------------------
;;; Global Data and Constants

; A character that is unlikely to be typed in normal operation. In this case,
; it is a half triangular colon modifier character. Used as a separator when
; building path strings through the hierarchy.
(def ^{:constant true} topic-separator \u02D1)

;(def id-of-first-top-level-topic (str "root" topic-separator 0 topic-separator "topic"))
(def id-of-second-top-level-topic (str "root" topic-separator 1 topic-separator "topic"))

(def empty-test-topic {:topic "Empty Test Topic"})
;(def empty-topic {:topic ""})

;; The hierarchical tree of tags is contained in the following. It is possible
;; to change it dynamically and have it re-render correctly.

(defonce test-hierarchy
         (r/atom {:title   "cljs-tree"
                  :tagline "Some experiments with hierarchical data."
                  :tree    [{:topic    "Journal"
                             :expanded true
                             :children [{:topic "2016"}
                                        {:topic    "2017"
                                         :expanded nil
                                         :children [{:topic    "11 - November"
                                                     :expanded true
                                                     :children [{:topic "Christmas Shopping"}
                                                                {:topic "Buy Groceries"}]}
                                                    {:topic    "22 - November"
                                                     :expanded true
                                                     :children [{:topic "Bake Pies"}]}
                                                    {:topic    "25 - November"
                                                     :expanded true
                                                     :children [{:topic "Cook Turkey"}]}]}
                                        {:topic "2018"}]}

                            {:topic    "Books"
                             :expanded true
                             :children [{:topic    "Favorite Authors"
                                         :expanded true
                                         :children [{:topic    "Gum-Lickin' Warburger"
                                                     :expanded true
                                                     :children [{:topic "Age"}
                                                                {:topic "DOB"}
                                                                {:topic "Obituary"}]}
                                                    {:topic "Bob Martin"}]}
                                        {:topic    "Genre"
                                         :expanded true
                                         :children [{:topic    "Science"
                                                     :expanded nil
                                                     :children [{:topic    "Astrophysics for People in a Hurry"
                                                                 :expanded true
                                                                 :children [{:topic    "Author"
                                                                             :expanded true
                                                                             :children [{:topic "Neil de Grasse Tyson"}]}
                                                                            {:topic    "ISBN"
                                                                             :expanded true
                                                                             :children [{:topic "978-0-393-60939-4"}]}]}]}
                                                    {:topic    "Science Fiction"
                                                     :expanded nil
                                                     :children [{:topic "Space Opera"}
                                                                {:topic "Military"}]}
                                                    {:topic "Horror"}
                                                    {:topic "Fantasy"}
                                                    {:topic "Biography"}
                                                    {:topic "History"}
                                                    {:topic    "Programming"
                                                     :expanded true
                                                     :children [{:topic "On Lisp"}
                                                                {:topic "Getting Clojure"}
                                                                {:topic    "Clean Code"
                                                                 :expanded nil
                                                                 :children [{:topic    "Author"
                                                                             :expanded true
                                                                             :children [{:topic "Robert Martin"}]}
                                                                            {:topic    "ISBN-10"
                                                                             :expanded true
                                                                             :children [{:topic "0-13-235088-2"}]}
                                                                            {:topic    "ISBN-13"
                                                                             :expanded true
                                                                             :children [{:topic "978-0-13-235088-4"}]}]}]}]}]}

                            {:topic    "Programming"
                             :expanded true
                             :children [{:topic    "Language"
                                         :expanded true
                                         :children [{:topic    "Java"
                                                     :expanded true
                                                     :children [{:topic "Snippets"}
                                                                {:topic "Books"}
                                                                {:topic "Blogs"}
                                                                {:topic "Gui Development"}]}
                                                    {:topic    "Clojure"
                                                     :expanded true
                                                     :children [{:topic "Snippets"}
                                                                {:topic "Books"}
                                                                {:topic "Numerics"}]}
                                                    {:topic    "Lisp"
                                                     :expanded nil
                                                     :children [{:topic "History"}
                                                                {:topic "Weenies"}
                                                                {:topic "The All Powerful"}]}]}]}

                            {:topic    "Animals"
                             :expanded true
                             :children [{:topic "Birds"}
                                        {:topic    "Mammals"
                                         :expanded nil
                                         :children [{:topic "Elephant"}
                                                    {:topic "Mouse"}]}
                                        {:topic "Reptiles"}]}

                            {:topic    "Plants"
                             :expanded true
                             :children [{:topic    "Flowers"
                                         :expanded true
                                         :children [{:topic "Rose"}
                                                    {:topic "Tulip"}]}
                                        {:topic "Trees"}]}]}))

;;;-----------------------------------------------------------------------------
;;; Utilities

;;------------------------------------------------------------------------------
;; DOM-related things

(defn get-element-by-id
  [id]
  (.getElementById js/document id))

(defn disable-element-by-id!
  [id]
  (set! (.-disabled (get-element-by-id id)) "true"))

(defn get-value-by-id
  [id]
  (.-value (get-element-by-id id)))

(defn set-value-by-id!
  [id value]
  (set! (.-value (get-element-by-id id)) value))

(defn event->target-element
  [evt]
  (.-target evt))

(defn event->target-id
  [evt]
  (.-id (event->target-element evt)))

(defn event->target-value
  [evt]
  (.-value (event->target-element evt)))

(defn scroll-ele-into-view
  "Scroll the element with the given id into view."
  [ele-id]
  (when-let [ele (get-element-by-id ele-id)]
    (.scrollIntoView ele)))

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

(defn unpack-keyboard-event
  [evt]
  (let [event-data-map (into (sorted-map)
                             {:alt-key            (.-altKey evt)
                              :char-code          (.-charCode evt)
                              :cmd-key            (or (.-metaKey evt) (.-ctrlKey evt))
                              :code               (.-code evt)
                              :ctrl-key           (.-ctrlKey evt)
                              :get-modifier-state (.getModifierState evt)
                              :is-composing       (.-isComposing evt)
                              :key                (.-key evt)
                              :key-code           (.-keyCode evt)
                              :location           (.-location evt)
                              :meta-key           (.-metaKey evt)
                              :repeating?         (.-repeat evt)
                              :shift-key          (.-shiftKey evt)
                              :which              (.-which evt)})]
    event-data-map))

;;------------------------------------------------------------------------------
;; Tree id manipulation functions.

(defn tree-id->tree-id-parts
  "Split a DOM id string (as used in this program) into its parts and return
  a vector of the parts."
  [id]
  (when (and id (not (empty? id)))
    (s/split id topic-separator)))

(defn tree-id-parts->tree-id-string
  "Return a string formed by interposing the topic-separator between the
  elements of the input vector."
  [v]
  (when (and v (vector? v) (not (empty? v)))
    (str (s/join topic-separator v))))

(defn is-top-tree-id?
  "Return true if tree-id represents to first sibling at the root level of
  the tree. (This topic is always displayed at the top of the tree -- hence
  the function name.)"
  [tree-id]
  (= ["root" "0"] (remove-last (tree-id->tree-id-parts tree-id))))

(defn nav-index-vector->tree-id-string
  "Creates a DOM id string from a vector of indices used to navigate to
  the topic. If no id type is specified, the default value of 'topic'
  is used."
  [nav-index-vector & type-to-use]
  (let [id-type (or (first type-to-use) "topic")
        result (str "root" topic-separator
                    (tree-id-parts->tree-id-string nav-index-vector)
                    topic-separator id-type)]
    result))

(defn tree-id->nav-index-vector
  "Return a vector of the numeric indices in the child vectors from the
  root to the element id."
  [tree-id]
  (-> (tree-id->tree-id-parts tree-id)
      (remove-last)
      (remove-first)))

(defn tree-id->sortable-nav-string
  "Convert the element id to a string containing the vector indices
  separated by a hyphen and return it. Result can be used to lexicographically
  determine if one element is 'higher' or 'lower' than another in the tree."
  [tree-id]
  (s/join "-" (tree-id->nav-index-vector tree-id)))

(defn increment-leaf-index
  "Given the tree id of a leaf node, return an id with the node index
  incremented."
  [tree-id]
  (let [parts (tree-id->tree-id-parts tree-id)
        index-in-vector (- (count parts) 2)
        leaf-index (int (nth parts index-in-vector))
        new-parts (replace-at parts index-in-vector (inc leaf-index))]
    (tree-id-parts->tree-id-string new-parts)))

(defn change-tree-id-type
  "Change the 'type' of a tree DOM element id to something else."
  [id new-type]
  (let [parts (tree-id->tree-id-parts id)
        shortened (remove-last parts)]
    (str (tree-id-parts->tree-id-string shortened) (str topic-separator new-type))))

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
  to the parent (a la get-n) and the index of the child encoded in the id."
  [tree-id]
  (let [string-vec (tree-id->tree-id-parts tree-id)
        idx (int (nth string-vec (- (count string-vec) 2)))
        without-last-2 (remove-last-two string-vec)
        without-first (delete-at without-last-2 0)
        index-vector (mapv int without-first)
        interposed (interpose :children index-vector)]
    {:path-to-parent (vec interposed) :child-index idx}))

;;------------------------------------------------------------------------------
;; Functions to manipulate the tree and subtrees.

(defn lower?
  "Return true if the first path is 'lower' in the tree than second path."
  [first-path second-path]
  (pos? (compare (tree-id->sortable-nav-string first-path)
                 (tree-id->sortable-nav-string second-path))))

(defn get-topic
  "Return the topic map at the requested id. Return nil if there is
  nothing at that location."
  [root-ratom topic-id]
  (get-in @root-ratom (tree-id->tree-path-nav-vector topic-id)))

(defn has-children
  "Return the entire vector of children if present, nil otherwise."
  [root-ratom topic-id]
  (:children (get-topic root-ratom topic-id)))

(defn is-expanded?
  "Return true if the subtree is in the expanded state (implying that it
  has children). Returns nil if the subtree is not expanded."
  [root-ratom tree-id]
  (:expanded (get-topic root-ratom tree-id)))

(defn expand-node
  "Assure that the node is expanded."
  [root-ratom tree-id]
  (let [nav-vector (tree-id->tree-path-nav-vector tree-id)
        my-cursor (r/cursor root-ratom nav-vector)]
    (swap! my-cursor assoc :expanded true)))

(defn collapse-node
  "Assure that the node is collapsed."
  [root-ratom tree-id]
  (let [nav-vector (tree-id->tree-path-nav-vector tree-id)
        my-cursor (r/cursor root-ratom nav-vector)]
    (swap! my-cursor assoc :expanded nil)))

(defn toggle-node-expansion
  "Toggle the 'expanded' setting for the node. When the branch has no
  :expanded key, does nothing."
  [root-ratom tree-id]
  (let [nav-vector (tree-id->tree-path-nav-vector tree-id)
        my-cursor (r/cursor root-ratom nav-vector)]
    (when (not (empty? (select-keys @my-cursor [:expanded])))
      (swap! my-cursor update :expanded not))))

(defn remove-top-level-sibling!
  "Remove one of the top level topics from the tree. Return a copy of the
  branch (entire tree) with the sibling removed or nil if there was a problem
  with the arguments."
  [root-ratom sibling-index]
  (when (and (instance? reagent.ratom/RAtom root-ratom)
             (vector @root-ratom)
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
  children. No data is deleted."
  [parent-topic-ratom index topic-to-add]
  (if (vector? @parent-topic-ratom)
    (swap! parent-topic-ratom insert-at index topic-to-add)
    (let [child-topic-vector (:children @parent-topic-ratom)
          new-child-vector (insert-at child-topic-vector index topic-to-add)]
      (swap! parent-topic-ratom assoc :children new-child-vector))))

(defn graft-topic!
  "Add a new topic at the specified location in the tree. The topic is inserted
  into the tree. No data it removed. Any existing information of the graft is
  pushed down in the tree."
  [root-ratom id-of-desired-node topic-to-graft]
  (let [path-and-index (tree-id->nav-vector-and-index id-of-desired-node)]
    (add-child! (r/cursor root-ratom (:path-to-parent path-and-index))
                (:child-index path-and-index) topic-to-graft)))

(defn move-branch!
  "Move an existing branch to a new location."
  [root-ratom id-of-existing-subtree id-of-new-subtree]
  (let [topic-to-move (get-topic root-ratom id-of-existing-subtree)]
    (if (lower? id-of-existing-subtree id-of-new-subtree)
      (do (prune-topic! root-ratom id-of-existing-subtree)
          (graft-topic! root-ratom id-of-new-subtree topic-to-move))
      (do (graft-topic! root-ratom id-of-new-subtree topic-to-move)
          (prune-topic! root-ratom id-of-existing-subtree)))
    (scroll-ele-into-view id-of-new-subtree)))

;;;-----------------------------------------------------------------------------
;;; Functions to handle keystroke events.

(defn handle-enter-key-down
  "Handle a key-down event for the Enter/Return key. Insert a new headline
  in the tree and focus it, ready for editing."
  [root-ratom span-id]
  ; If the topic span has children, add a new child in the zero-position
  ; Else add a new sibling below the current topic
  (let [id-of-new-child (if (is-expanded? root-ratom span-id)
                          (insert-child-index-into-parent-id span-id 0)
                          (increment-leaf-index span-id))]
    (graft-topic! root-ratom id-of-new-child empty-test-topic)
    (let [id-of-new-editor (change-tree-id-type id-of-new-child "editor")
          id-of-new-label (change-tree-id-type id-of-new-child "label")]
      ;; Wait for rendering to catch up.
      (r/after-render
        (fn []
          (swap-display-properties id-of-new-label id-of-new-editor)
          (.focus (get-element-by-id id-of-new-editor)))))))

(defn id-of-previous-sibling
  "Return the id of the previous sibling of this tree id. Returns nil if this
  tree id is the first (zero'th) in a group of siblings."
  [current-sibling-id]
  (let [parts (tree-id->tree-id-parts current-sibling-id)
        last-path-index (int (nth parts (- (count parts) 2)))]
    (when (pos? last-path-index)
      (tree-id-parts->tree-id-string
        (into (remove-last-two parts) [(dec last-path-index) "topic"])))))

(defn id-of-last-visible-child
  "Return the id of the last visible child of the branch starting at tree-id.
  The last visible child may be many levels deeper in the tree."
  [root-ratom tree-id]
  (loop [id-so-far tree-id
         topic-map (get-topic root-ratom id-so-far)]
    (if (not (and (:expanded topic-map) (:children topic-map)))
      id-so-far
      (let [next-child-vector (:children topic-map)
            next-index (dec (count next-child-vector))
            next-id (insert-child-index-into-parent-id id-so-far next-index)]
        (recur next-id next-child-vector)))))

(defn focus-editor-for-id
  "Focus the editor associated with the id. Assumes the topic is visible and
  that the editor is not already focused. WILL UNFOCUS EDITOR IF IT IS ALREADY
  FOCUSED."
  [tree-id]
  (println "focus-editor-on-id: tree-id: " tree-id)
  (when tree-id
    (let [editor-id (change-tree-id-type tree-id "editor")
          label-id (change-tree-id-type tree-id "label")]
      (swap-display-properties label-id editor-id)
      (.focus (get-element-by-id editor-id)))))

(defn handle-backspace-key-down
  "Handle a key-down event for the Backspace key. Tries to intelligently handle
  cases when the key is tapped in an empty topic by deciding what happens to
  children of the deleted topic and where the focus should travel to."
  [root-ratom evt topic-ratom span-id]
  (when (zero? (count @topic-ratom))
    (.preventDefault evt)
    (if (is-top-tree-id? span-id)
      (when (get-topic root-ratom id-of-second-top-level-topic)
        ; Just delete the top-most headline.
        (println "Pruning top level headline")
        (prune-topic! root-ratom span-id))
      (do
        (println "")
        (prune-topic! root-ratom span-id)
        (if-let [id-to-focus (id-of-previous-sibling span-id)]
          (do
            (println "id-to-focus branch")
            (focus-editor-for-id
              (id-of-last-visible-child root-ratom id-to-focus)))
          (do
            (println "last-visible-child-branch")
            (focus-editor-for-id
              (id-of-last-visible-child root-ratom (id-of-previous-sibling span-id)))))))))

(defn promote-headline
  [root-ratom evt topic-ratom span-id]
  (println "promote-headline"))

(defn demote-headline
  [root-ratom evt topic-ratom span-id]
  (println "demote-headline")
  (println "(id-of-previous-sibling span-id): " (id-of-previous-sibling span-id))
  (if (has-children root-ratom (id-of-previous-sibling span-id))
    (println "Has children")
    (println "Has NO children"))
  (when-let [previous-sibling (id-of-previous-sibling span-id)]
    (expand-node root-ratom previous-sibling)
    (let [sibling-parts (tree-id->tree-id-parts previous-sibling)
          with-added-leaf (conj (remove-last sibling-parts) 0)
          demoted-prefix (tree-id-parts->tree-id-string with-added-leaf)
          demoted-id (str demoted-prefix topic-separator "topic")]
      (println "demoted-id: " demoted-id)
      (move-branch! root-ratom span-id demoted-id))))

(defn handle-tab-key-down
  [root-ratom evt topic-ratom span-id]
  (println "handle-tab-key-down: span-id: " span-id)
  (.preventDefault evt)
  (let [evt-map (unpack-keyboard-event evt)]
    (cond
      (and (:shift-key evt-map)
           (:cmd-key evt-map)
           (:alt-key evt-map)) (promote-headline root-ratom evt topic-ratom span-id)
      (and (:cmd-key evt-map)
           (:alt-key evt-map)) (demote-headline root-ratom evt topic-ratom span-id)
      :default nil)))

(defn handle-key-down
  "Detect key-down events and dispatch them to the appropriate handlers."
  [evt root-ratom topic-ratom span-id]
  (let [evt-map (unpack-keyboard-event evt)]
    (cond
      (= (:key evt-map) "Enter") (handle-enter-key-down root-ratom span-id)
      (= (:key evt-map) "Delete") (println "Delete")
      (= (:key evt-map) "Backspace") (handle-backspace-key-down
                                       root-ratom evt topic-ratom span-id)
      (= (:key evt-map) "Tab") (handle-tab-key-down root-ratom evt
                                                    topic-ratom span-id)
      :default nil)))

;;;-----------------------------------------------------------------------------
;;; Some data and functions to cycle through adding, moving, moving again and
;;; then deleting a child branch.

(def add-rock-dest-id (tree-id-parts->tree-id-string ["root" 1 1 2 "topic"]))

(def mov-rock-dest-id (tree-id-parts->tree-id-string ["root" 1 0 "topic"]))

(def fnl-rock-dest-id (tree-id-parts->tree-id-string ["root" 2 0 1 1 "topic"]))

(defn add-rocks!
  "Add some different stuff to the tree."
  [root-ratom]
  (let [new-info {:topic    "Rocks"
                  :expanded true
                  :children [{:topic "Igneous"}
                             {:topic "Sedimentary"}
                             {:topic "Metamorphic"}]}]
    (graft-topic! root-ratom add-rock-dest-id new-info)
    (scroll-ele-into-view add-rock-dest-id)))

(defn move-rocks!
  "Move the existing branch of rock data to a new location."
  [root-ratom]
  (move-branch! root-ratom add-rock-dest-id mov-rock-dest-id))

(defn move-rocks-again!
  [root-ratom]
  (move-branch! root-ratom mov-rock-dest-id fnl-rock-dest-id))

(defn remove-rocks!
  "Remove rock information that was previously added to tree."
  [root-ratom]
  (prune-topic! root-ratom fnl-rock-dest-id))

(defn add-move-remove-rocks-play-text-button
  [root-ratom]
  (fn [root-ratom]
    (let [add-text "Click to Add Rock Data"
          mov-text "Click to Move Rock Data"
          agn-text "Click to Move Rock Data Again"
          del-text "Click to Remove Rock Data"
          button-id "add-remove-rock-button-id"
          my-cursor (r/cursor root-ratom [:tree])]
      [:div.tree-control--button-area
       [:input.tree-control--button
        {:type     "button"
         :id       button-id
         :value    add-text
         :on-click (fn [evt]
                     (let [txt (get-value-by-id button-id)]
                       (cond
                         (= txt add-text) (do
                                            (add-rocks! my-cursor)
                                            (set-value-by-id! button-id mov-text))
                         (= txt mov-text) (do
                                            (move-rocks! my-cursor)
                                            (set-value-by-id! button-id agn-text))
                         (= txt agn-text) (do
                                            (move-rocks-again! my-cursor)
                                            (set-value-by-id! button-id del-text))
                         (= txt del-text) (do
                                            (remove-rocks! my-cursor)
                                            (set-value-by-id! button-id add-text))
                         :default (println "Aaak... Unexpected condition"))))}]])))

;;;-----------------------------------------------------------------------------
;;; Functions to build the control.

(defn handle-chevron-click!
  "Handle the click on the expansion chevron by toggling the state of
  expansion in the application state atom. This will cause the tree
  to re-render visually."
  [evt root-ratom]
  (let [ele-id (event->target-id evt)
        kwv (tree-id->tree-path-nav-vector ele-id)
        ekwv (conj kwv :expanded)]
    (swap! root-ratom update-in ekwv not)))

;; The amount of indentation to accumulate for each level down the tree.
(def indent-increment 1.5)

(defn indent-div [indent-id]
  (let [id-v (tree-id->nav-index-vector indent-id)
        indent (* indent-increment (- (count id-v) 1))
        indent-style (str 0 " " 0 " " indent "rem")]
    ^{:key indent-id}
    [:div#indent-id.tree-control--indent-div {:style {:flex indent-style}}]))

(defn chevron-div
  "Get the expansion symbol to be used at the front of a topic. Returns
  a result based on whether the tree has children, and if so, whether they
  are expanded or not."
  [root-ratom subtree-ratom chevron-id]
  (let [clickable-chevron-props {:class    "tree-control--chevron-div"
                                 :id       chevron-id
                                 :on-click #(handle-chevron-click! % root-ratom)}
        invisible-chevron-props {:class "tree-control--chevron-div"
                                 :id    chevron-id
                                 :style {:opacity "0.0"}}
        es (cond
             (and (:children @subtree-ratom)
                  (:expanded @subtree-ratom)) [:div clickable-chevron-props
                                               (str \u25BC \space)]
             (:children @subtree-ratom) [:div clickable-chevron-props
                                         (str \u25BA \space)]
             ; No children, so no chevron is displayed.
             ; This stuff is to ensure consistent horizontal spacing
             ; even though no expansion chevron is visible.
             :default [:div invisible-chevron-props (str \u25BA \space)])]
    es))

(defn topic-info-div
  "Build the textual/interactive part of a topic/headline."
  [root-ratom sub-tree-ratom span-id]
  (let [topic-ratom (r/cursor sub-tree-ratom [:topic])
        label-id (change-tree-id-type span-id "label")
        editor-id (change-tree-id-type span-id "editor")]
    [:div.tree-control--topic-info-div
     [:label {:id      label-id
              :style   {:display :initial}
              :class   "tree-control--topic-label"
              :for     editor-id
              ;:onMouseOver #(println "id: " span-id ", label-id: " label-id ", editor-id: " editor-id)
              :onClick (fn [e]
                         (swap-display-properties label-id editor-id)
                         (.focus (get-element-by-id editor-id))
                         (.stopPropagation e))}
      @topic-ratom]

     [:input {:type      "text"
              :id        editor-id
              :class     "tree-control--topic-editor"
              :style     {:display :none}
              :onKeyDown #(handle-key-down % root-ratom topic-ratom span-id)
              :onFocus   #(.stopPropagation %)
              :onBlur    #(swap-display-properties label-id editor-id)
              :onChange  #(reset! topic-ratom (event->target-value %))
              :value     @topic-ratom}]]))

(defn dom-ids-for-row
  "Return a map of all of the ids used in building a row of the control."
  [parts]
  (let [row-id-parts (conj parts "row")
        row-id (tree-id-parts->tree-id-string row-id-parts)]
    {:row-id     row-id
     :indent-id  (change-tree-id-type row-id "indent")
     :chevron-id (change-tree-id-type row-id "chevron")
     :topic-id   (change-tree-id-type row-id "topic")}))

(defn outliner-row-div
  "Return one row of the outliner."
  [root-ratom index-vector]
  (let [ids-for-row (dom-ids-for-row index-vector)
        row-id (:row-id ids-for-row)
        indent-id (:indent-id ids-for-row)
        chevron-id (:chevron-id ids-for-row)
        topic-id (:topic-id ids-for-row)
        nav-path (tree-id->tree-path-nav-vector row-id)
        subtree-ratom (r/cursor root-ratom nav-path)]
    ^{:key row-id}
    [:div.tree-control--row-div
     [indent-div indent-id]
     [chevron-div root-ratom subtree-ratom chevron-id]
     [topic-info-div root-ratom subtree-ratom topic-id]]))

;; From: https://stackoverflow.com/questions/5232350/clojure-semi-flattening-a-nested-sequence
(defn flatten-to-vectors
  "Flatten nested sequences of vectors to a flat sequence of those vectors."
  [s]
  (mapcat #(if (every? coll? %) (flatten-to-vectors %) (list %)) s))

(defn visible-nodes
  "Return a sequence of vectors of the numerical indices used to travel from the
  root to each visible node."
  [tree so-far]
  (flatten-to-vectors
    (map-indexed
      (fn [idx ele]
        (let [new-id (conj so-far idx)]
          (if (not (and (:children ele) (:expanded ele)))
            new-id
            (cons new-id (visible-nodes (:children ele) new-id)))))
      tree)))

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
  [app-state-atom]
  (fn [app-state-ratom]
    [:div.page
     [:div.title-div
      [:h1 "cljs-tree"]
      [:h3 "Some experiments with hierarchical data."]]
     [:div.tree-control
      [:p.tree-control--description "Here is the result of "
       [:code "tree->hiccup"] ":"]
      [:div.tree-control--container-div
       (let [root-ratom (r/cursor app-state-atom [:tree])]
         [tree->hiccup root-ratom])]
      [add-move-remove-rocks-play-text-button app-state-ratom]]]))

(defn start []
  (r/render-component [home test-hierarchy]
                      (get-element-by-id "app")))

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
  )
