(ns cljs-tree.vector-utils)

(defn delete-at
  "Remove the nth element from the vector and return the result."
  [v n]
  (into (subvec v 0 n) (subvec v (inc n))))

(defn remove-first
  "Remove the first element in the vector and return the result."
  [v]
  (subvec v 1))

(defn remove-last
  "Remove the last element in the vector and return the result."
  [v]
  (subvec v 0 (dec (count v))))

(defn remove-last-two
  "Remove the last two elements in the vector and return the result."
  [v]
  (subvec v 0 (- (count v) 2)))

(defn insert-at
  "Return a copy of the vector with new-item inserted at the given n. If
  n is less than zero, the new item will be inserted at the beginning of
  the vector. If n is greater than the length of the vector, the new item
  will be inserted at the end of the vector."
  [v n new-item]
  (cond (neg? n) (into [new-item] v)
        (>= n (count v)) (conj v new-item)
        :default (into (conj (subvec v 0 n) new-item) (subvec v n))))

(defn replace-at
  "Replace the current element in the vector at index with the new-element
  and return it."
  [v index new-element]
  (insert-at (delete-at v index) index new-element))

(defn append-element-to-vector
  "Reaturn a copy of the vector with the new element appended to the end."
  [v new-item]
  (into [] (concat v [new-item])))

