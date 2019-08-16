;;;-----------------------------------------------------------------------------
;;; Yet another stack implemented in ClojureScript.
;;;
;;; In the current implementation, this is just a very thin layer over a
;;; vector.
;;;
;;; The extremely short function names are just to avoid conflicts with
;;; other functions in the library.

(ns cljs-tree.stack)

(defn ps
  "Push an item onto the stack."
  [stk item]
  (if (list? item)
    (vec (concat stk item))
    (conj stk item)))

(defn pk
  "\"Peek\" at the item in the top of the stack without removing it."
  [stk]
  (peek stk))

(defn pp
  "Remove the item at the top of the stack and remaining s."
  [stk]
  (when (seq stk)
    (pop stk)))

(defn mt?
  "Return true if the stack is empty, false otherwise."
  [stk]
  (empty? stk))

(defn sz
  "Return the number of items in the stack."
  [stk]
  (count stk))

(defn clr
  "Remove all elements from the stack."
  [stk]
  [])

(defn mk
  "Return a newly created stack."
  ([] [])
  ([eles] (vec eles)))