;;;;
;;;; This namespace is an experiment in creating nested, hierarchical tags
;;;; and generating HTML (hiccup) from those data structures.
;;;;

(ns cljs-tree.core
  (:require [cljs.pprint :as ppr]
            [clojure.string :as s]
            [clojure.walk :as w]
            [reagent.core :as r]
            [clojure.string :as string]))

(enable-console-print!)

;;;-----------------------------------------------------------------------------
;;; Global Data and Constants

; A character that is unlikely to be typed in normal operation. In this case,
; it is a half triangular colon modifier character. Used as a separator when
; building path strings through the hierarchy.
(def ^{:constant true} topic-separator \u02D1)

(def empty-test-topic {:topic "Empty Test Topic"})
(def empty-topic {:topic ""})

;; The hierarchical tree of tags is contained in the following. It is possible
;; to change it dynamically and have it re-render correctly.

(defonce test-hierarchy (r/atom {:title   "cljs-tree"
                                 :tagline "Some experiments with hierarchical data."
                                 :tree    [{:topic    "Journal"
                                            :expanded true
                                            :children [{:topic "2016"}
                                                       {:topic "2017"}
                                                       {:topic    "2018"
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
                                                                    :children [{:topic "Cook Turkey"}]}]}]}

                                           ; {:topic "Flamberge"}

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
  (let [first-style-declaration (.-style (get-element-by-id first-id))
        second-style-declaration (.-style (get-element-by-id second-id))
        first-display-value (.getPropertyValue first-style-declaration property)
        second-display-value (.getPropertyValue second-style-declaration property)]
    (.setProperty first-style-declaration property second-display-value)
    (.setProperty second-style-declaration property first-display-value)))

(defn swap-display-properties
  [first-id second-id]
  "Swap the display style properties for the two elements."
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
    ;(println event-data-map)
    event-data-map))

;;------------------------------------------------------------------------------
;; Vector-related manipulations.

(defn delete-at
  "Remove the nth element from the vector and return the result."
  [v n]
  (vec (concat (subvec v 0 n) (subvec v (inc n)))))

(defn remove-last
  "Remove the last element in the vector and return the result."
  [v]
  (subvec v 0 (dec (count v))))

(defn remove-last-two
  "Remove the last two elements in the vector and return the result."
  [v]
  (subvec v 0 (- (count v) 2)))

(defn remove-first
  "Remove the first element in the vector and return the result."
  [v]
  (subvec v 1))

(defn insert-at
  "Return a copy of the vector with new-item inserted at the given n. If
  n is less than zero, the new item will be inserted at the beginning of
  the vector. If n is greater than the length of the vector, the new item
  will be inserted at the end of the vector."
  [v n new-item]
  (vec (concat (conj (subvec v 0 n) new-item) (subvec v n))))

(defn replace-at
  "Replace the current element in the vector at index with the new-element
  and return it."
  [v index new-element]
  (-> (delete-at v index)
      (insert-at index new-element)))

(defn append-element-to-vector
  "Reaturn a copy of the vector with the new element appended to the end."
  [v new-item]
  (into [] (concat v [new-item])))

;;------------------------------------------------------------------------------
;; Tree id manipulation functions.

(defn tr-id->tr-id-parts
  "Split a DOM id string (as used in this program) into its parts and return
  a vector of the parts"
  [id]
  (s/split id topic-separator))

(defn tr-id-parts->tr-id-string
  "Return a string formed by interposing the topic-separator between the
  elements of the input vector."
  [v]
  (str (s/join topic-separator v)))

(defn increment-leaf-index
  "Given the tree id of a leaf node, return an id with the node index
  incremented."
  [tr-id]
  (let [parts (tr-id->tr-id-parts tr-id)
        index-in-vector (- (count parts) 2)
        leaf-index (int (nth parts index-in-vector))
        new-parts (replace-at parts index-in-vector (inc leaf-index))]
    (tr-id-parts->tr-id-string new-parts)))

(defn change-tr-id-type
  "Change the 'type' of a tree DOM element id to something else."
  [id new-type]
  (let [parts (tr-id->tr-id-parts id)
        shortened (remove-last parts)]
    (str (tr-id-parts->tr-id-string shortened) (str topic-separator new-type))))

(defn tr-id->nav-vector-for-parent
  "Return a vector of the numeric indices in the child vectors from the
  root to the element id."
  [tr-id]
  (-> (tr-id->tr-id-parts tr-id)
      (remove-last)
      (remove-first)))

(defn tr-id->sortable-nav-string
  "Convert the element id to a string containing the vector indices
  separated by a hyphen and return it. Result can be used to lexicographically
  determine if one element is 'higher' or 'lower' than another in the tree."
  [tr-id]
  (string/join "-" (tr-id->nav-vector-for-parent tr-id)))

(defn insert-child-index-into-parent-id
  "Return a new id where the index of the child in the parents children vector
  has been appended."
  [parent-id child-index]
  (-> (tr-id->tr-id-parts parent-id)
      (remove-last)
      (conj child-index)
      (conj "topic")
      (tr-id-parts->tr-id-string)))

(defn tr-id->tree-path-nav-vector
  "Return a vector of indices and keywords to navigate to the piece of data
  represented by the DOM element with the given id."
  [tr-id]
  (let [nav-vector (mapv int (tr-id->nav-vector-for-parent tr-id))
        interposed (interpose :children nav-vector)]
    (vec interposed)))

(defn tr-id->nav-vector-and-index
  "Parse the id into a navigation path vector to the parent of the node and an
  index within the vector of children. Return a map containing the two pieces
  of data. Basically, parse the id into a vector of information to navigate
  to the parent (a la get-n) and the index of the child encoded in the id."
  [tr-id]
  (let [string-vec (tr-id->tr-id-parts tr-id)
        idx (int (nth string-vec (- (count string-vec) 2)))
        without-last-2 (remove-last-two string-vec)
        without-first (delete-at without-last-2 0)
        index-vector (mapv int without-first)
        interposed (interpose :children index-vector)]
    {:path-to-parent (vec interposed) :child-index idx}))

;;------------------------------------------------------------------------------
;; Functions to manipulate the tree and subtrees.

(defn is-root?
  "Return true when the id represents a sibling in the root vector of nodes."
  [tr-id]
  (= 1 (count (tr-id->nav-vector-for-parent tr-id))))

(defn lower?
  "Return true if the first path is 'lower' in the tree than second path."
  [first-path second-path]
  (pos? (compare (tr-id->sortable-nav-string first-path)
                 (tr-id->sortable-nav-string second-path))))

(defn get-topic
  "Return the topic map at the requested id. Return nil f there is
  nothing at that location."
  [root-ratom topic-id]
  (get-in @root-ratom (tr-id->tree-path-nav-vector topic-id)))

(defn get-topic-children
  "If a tree topic has children, return them. Otherwise, return nil."
  [root-ratom topic-id]
  (let [surrounding-topic-path (tr-id->tree-path-nav-vector topic-id)
        new-nav-vector (into [] (append-element-to-vector surrounding-topic-path :children))]
    (get-in @root-ratom new-nav-vector)))

(defn remove-child!
  "Remove the specified child from the parents vector of children."
  [parent-ratom child-index]
  (let [vector-of-children (:children @parent-ratom)]
    (when (and vector-of-children
               (>= child-index 0)
               (< child-index (count vector-of-children)))
      (let [new-child-vector (delete-at vector-of-children child-index)]
        (if (empty? new-child-vector)
          (swap! parent-ratom dissoc :children)
          (swap! parent-ratom assoc :children new-child-vector))))))

(defn prune-topic!
  "Remove the subtree with the given id from the tree."
  [root-ratom id-of-existing-subtree]
  (let [path-and-index (tr-id->nav-vector-and-index id-of-existing-subtree)
        child-vector-target (r/cursor root-ratom (:path-to-parent path-and-index))]
    (remove-child! child-vector-target (:child-index path-and-index))))

(defn add-child!
  "Insert the given topic at the specified index in the parents vector of
  children. No data is deleted."
  [parent-topic-map-ratom index topic-to-add]
  (println "add-child!: parent-topic-map-ratom: " parent-topic-map-ratom
           "\nindex: " index ", topic-to-add: " topic-to-add)
  (let [child-topic-vector (:children @parent-topic-map-ratom)]
    (cond
      (nil? (:children @parent-topic-map-ratom))
      (do
        (println "trying to insert into nil child map")
        (swap! parent-topic-map-ratom assoc :children [topic-to-add]))

      (or (empty? child-topic-vector) (>= index (count child-topic-vector)))
      (let [new-child (conj child-topic-vector topic-to-add)]
        (println "inserting into empty or short child map")
        (swap! parent-topic-map-ratom assoc :children new-child))

      (and (>= index 0) (<= index (count child-topic-vector)))
      (let [new-child-vector (insert-at child-topic-vector index topic-to-add)]
        (println "inserting somewhere inside the topic vector")
        (swap! parent-topic-map-ratom assoc :children new-child-vector))

      :default (println "Aack! add-child!: Fell through to default case."))))

(defn graft-topic!
  "Add a new topic at the specified location in the tree. The topic is inserted
  into the tree. No data it removed. Any existing information of the graft is
  pushed down in the tree."
  [root-ratom id-of-desired-node topic-to-graft]
  (let [path-and-index (tr-id->nav-vector-and-index id-of-desired-node)]
    (add-child! (r/cursor root-ratom (:path-to-parent path-and-index))
                (:child-index path-and-index) topic-to-graft)))

;; These functions represent another way of adding child nodes.
;
;(defn insert-child!
;  [root-ratom ele-id new-topic index]
;  (println "insert-child!: ele-id: " ele-id ", new-topic: " new-topic ", index: " index)
;  (let [existing-child-vector (tr-id->tree-path-nav-vector ele-id)
;        ;(get-in @root-ratom (tr-id->tree-path-nav-vector ele-id))
;        _ (println "existing-child-vector: " existing-child-vector)
;        new-nav-vector (append-element-to-vector existing-child-vector :children)
;        _ (println "new-nav-vector:: " new-nav-vector)
;        retrieved-child-vector (get-in @root-ratom new-nav-vector)
;        _ (println "retrieved-child-vector: " retrieved-child-vector)
;        new-child-vector (insert-at retrieved-child-vector index new-topic)
;        _ (println "new-child-vector: " new-child-vector)
;
;        ]
;    (swap! root-ratom assoc-in new-nav-vector new-child-vector)
;    (println (get-in @root-ratom new-nav-vector))
;    ;  (println "new-child-vector: " new-child-vector)
;    ;  (reset! ele-id new-child-vector))
;    ))
;
;(defn insert-child-after!
;  [root-ratom ele-id new-topic]
;  (insert-child! root-ratom ele-id new-topic 0))

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
;;; Some data and functions to cycle through adding, moving, moving again and
;;; then deleting a child branch.

(def add-rock-dest-id (tr-id-parts->tr-id-string ["root" 1 1 2 "topic"]))

(def mov-rock-dest-id (tr-id-parts->tr-id-string ["root" 1 0 "topic"]))

(def fnl-rock-dest-id (tr-id-parts->tr-id-string ["root" 2 0 1 1 "topic"]))

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
  (println "move-rocks-again!")
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
  [evt full-tree-ratom]
  (let [ele-id (event->target-id evt)
        kwv (tr-id->tree-path-nav-vector ele-id)
        ekwv (conj kwv :expanded)]
    (swap! full-tree-ratom update-in ekwv not)))

(defn get-chevron
  "Get the expansion symbol to be used at the front of a topic. Returns
  a result based on whether the tree has children, and if so, whether they
  are expanded or not."
  [full-tree-ratom t id-prefix]
  (let [clickable-chevron {:class    "tree-control--expansion-span"
                           :id       (str id-prefix topic-separator "chevron")
                           :on-click (fn [evt] (handle-chevron-click! evt full-tree-ratom))
                           :cursor   "pointer"}
        invisible-chevron {:class "tree-control--expansion-span"
                           :style {:opacity "0.0"}}
        es (cond
             (and (:children t)
                  (:expanded t)) [:span clickable-chevron (str \u25BC \space)]
             (:children t) [:span clickable-chevron (str \u25BA \space)]
             ; No children, so no chevron is displayed.
             ; This stuff is to ensure consistent horizontal spacing
             ; even though no expansion chevron is visible.
             :default [:span invisible-chevron (str \u25BA \space)])]
    es))

;(defn insert-sibling!
;  [subtree-ratom index new-topic]
;  (println "insert-sibling!: index: " index ", new-topic: " new-topic)
;  (when (vector? @subtree-ratom)
;    (let [existing-topic-vector @subtree-ratom
;          new-child-vector (insert-at existing-topic-vector index new-topic)]
;      (println "inserting somewhere inside the existing topic vector")
;      (reset! subtree-ratom new-child-vector))))

;(defn insert-empty-sibling-after!
;  [root-ratom topic-ratom span-id]
;  (println "insert-empty-sibling-after!: span-id: " span-id ", topic-ratom: " topic-ratom)
;  (let [empty-topic {:topic "Empty Topic Here"}
;        path-and-index (tr-id->nav-vector-and-index span-id)
;        _ (println "path-and-index: " path-and-index)
;        parent-path (:path-to-parent path-and-index)
;        _ (println "parent-path: " parent-path)
;        parent-path-ratom (if (empty? parent-path)
;                            root-ratom
;                            (r/cursor root-ratom parent-path))
;        _ (println "parent-path-ratom: " parent-path-ratom)
;        index (inc (:child-index path-and-index))]
;    (println "index: " index)
;    (insert-sibling! parent-path-ratom index empty-topic)))

(defn handle-enter-key-down
  [root-ratom topic-ratom span-id]
  (println "Saw 'Enter' key down.")
  (println "    topic-ratom: " topic-ratom)
  (println "    @topic-ratom: " @topic-ratom)
  (println "    span-id:     " span-id)
  (println "    (tr-id->nav-vector-for-parent span-id): " (tr-id->nav-vector-for-parent span-id))
  (println "    (get-topic root-ratom span-id): " (get-topic root-ratom span-id))
  (println "    (:children (get-topic root-ratom span-id)): " (:children (get-topic root-ratom span-id)))
  ; If the topic span has children, add a new child in the zero-position
  ; Else add a new sibling below the current topic
  (let [id-of-new-child (if (get-topic-children root-ratom span-id)
                          (insert-child-index-into-parent-id span-id 0)
                          (increment-leaf-index span-id))
        id-of-new-editor (change-tr-id-type id-of-new-child "editor")
        id-of-new-label (change-tr-id-type id-of-new-child "label")]
    (println "id-of-new-editor: " id-of-new-editor)
    (println "id-of-new-label: " id-of-new-label)
    ; Assure all parents of new node are expanded
    ; Focus new node.
    (graft-topic! root-ratom id-of-new-child empty-test-topic)
    (swap-display-properties id-of-new-editor id-of-new-label)
    (.focus (get-element-by-id id-of-new-editor))))

(defn handle-key-down
  [evt root-ratom topic-ratom span-id]
  (println "Saw key down event: " evt)
  (let [evt-map (unpack-keyboard-event evt)]
    (cond
      (= (:key evt-map) "Enter") (handle-enter-key-down root-ratom topic-ratom span-id)
      :default nil)))

(defn build-topic-span
  [root-ratom topic-ratom span-id]
  (let [span-id-suffix (str topic-separator "span")
        label-id (s/replace span-id span-id-suffix (str topic-separator "label"))
        editor-id (s/replace span-id span-id-suffix (str topic-separator "editor"))]
    [:span.tree-control--topic

     [:label {:id          label-id
              :style       {:display :initial}
              :class       "tree-control--topic-label"
              :onMouseOver #(println "id: " span-id)
              :onClick     (fn [e]
                             (swap-display-properties label-id editor-id)
                             (.focus (get-element-by-id editor-id))
                             (.stopPropagation e))}
      @topic-ratom]

     [:input {:type      "text"
              :id        editor-id
              :class     "tree-control--editor"
              :style     {:display :none}
              ;:onKeyUp       (fn [e]                        ;(println "Saw key up: ")
              ;                 (let [evt-map (unpack-keyboard-event e)]
              ;                   (when (= (:key evt-map) "Enter")
              ;                     (println "Saw enter key.")
              ;                     (insert-empty-sibling-after! root-ratom topic-ratom span-id))))
              :onKeyDown #(handle-key-down % root-ratom topic-ratom span-id)
              :onFocus   (fn [e] (.stopPropagation e))
              :onBlur    (fn [e] (swap-display-properties label-id editor-id))
              :onChange  (fn [e] (reset! topic-ratom (event->target-value e)))
              :value     @topic-ratom}]]))

(defn tree->hiccup
  "Given a data structure containing a hierarchical tree of topics, generate
  hiccup to represent that tree. Also generates a unique, structure-based
  id that is included in the hiccup so that the correct element in the
  application state can be located when its corresponding HTML element is
  clicked."
  ([root-ratom]
   (tree->hiccup root-ratom root-ratom "root"))
  ([root-ratom sub-tree-ratom path-so-far]
   [:ul
    (when (= path-so-far "root")
      ; Make sure the top-level group of elements use the CSS to represent
      ; it as an hierarchy.
      {:class "tree-control--list"
       :id    "a-tree-control-id"})
    (doall
      (for
        [index (range (count @sub-tree-ratom))]
        (let [t (r/cursor sub-tree-ratom [index])
              topic-ratom (r/cursor t [:topic])
              id-prefix (str path-so-far topic-separator index)
              ; _ (println "id-prefix: " id-prefix)
              topic-id (str id-prefix topic-separator "topic")
              span-id (str id-prefix topic-separator "span")]
          ^{:key topic-id}
          [:li {:id          topic-id
                :draggable   "true"
                :onDragStart (fn [evt] (println "Saw drag start: id: " topic-id)
                               (let [id-of-dragged (event->target-id evt)]
                                 (.setData (.-dataTransfer evt) "text" topic-id)))
                :onDrop      (fn [evt] (println "Saw drag drop: id: " topic-id)
                               (.preventDefault evt)
                               (let [data (.getData (.-dataTransfer evt) "text")]
                                 (println "getData returned: " data)
                                 (.appendChild (.-target evt) (get-element-by-id topic-id))))
                :onDragEnter (fn [evt] (println "Saw drag enter: id: " topic-id
                                                (.preventDefault evt)))
                :onDragOver  (fn [evt] (println "Saw drag over: id: " topic-id)
                               (.preventDefault evt))}
           [:div.tree-control--topic-div
            (get-chevron root-ratom @t id-prefix)
            (build-topic-span root-ratom topic-ratom span-id)
            (when (and (:children @t)
                       (:expanded @t))
              (tree->hiccup root-ratom
                            (r/cursor t [:children]) id-prefix))]])))]))

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
      [:div.tree-control--content (tree->hiccup (r/cursor app-state-ratom [:tree]))]
      [add-move-remove-rocks-play-text-button app-state-ratom]]]))

(r/render-component [home test-hierarchy]
                    (get-element-by-id "app"))

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
  )
