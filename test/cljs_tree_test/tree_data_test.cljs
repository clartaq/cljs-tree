;;;-----------------------------------------------------------------------------
;;; Tests of tree data structure manipulations.

(ns ^:figwheel-always cljs-tree-test.tree-data-test
  (:require [cljs-tree.core :as ct]
            [cljs.test :refer-macros [deftest is testing]]
            [reagent.core :as r]))

(enable-console-print!)

(defonce ts ct/topic-separator)

(deftest lower?-test
  (testing "The 'lower?' function"
    (is (true? (ct/lower?
                 (ct/tree-id-parts->tree-id-string ["root"  0  5 "topic"])
                 (ct/tree-id-parts->tree-id-string ["root"  0 "topic"]))))
    (is (false? (ct/lower?
                  (ct/tree-id-parts->tree-id-string ["root"  0 "topic"])
                  (ct/tree-id-parts->tree-id-string ["root"  0 5"topic"]))))
    ; Check against equal
    (is (false? (ct/lower?
                  (ct/tree-id-parts->tree-id-string ["root"  5 6 3 "topic"])
                  (ct/tree-id-parts->tree-id-string ["root"  5 6 3 "topic"]))))
    (is (true? (ct/lower?
                 (ct/tree-id-parts->tree-id-string ["root"  5 7 3 "topic"])
                 (ct/tree-id-parts->tree-id-string ["root"  5 6 2 "topic"]))))))

(def a-tree {:tree [{:topic "The First at Top"}
                    {:topic    "The Second at Top"
                     :expanded true
                     :children [{:topic "The First on Second Level"}
                                {:topic "The Second on Second Level"}
                                {:topic    "The Last on Second Level"
                                 :expanded nil
                                 :children [{:topic "The First on Third Level"}
                                            {:topic "The Second on Third Level"}
                                            {:topic "The Third on Third Level"}
                                            {:topic "The Fourth on Third Level"}
                                            {:topic "The Fifth on Third Level"}]}]}
                    {:topic    "The Third at Top"
                     :children [{:topic    "The Only Child of the Last at Top"
                                 :children [{:topic "The First on Third Level"}
                                            {:topic "The Second on Third Level"}
                                            {:topic "The Third on Third Level"}
                                            {:topic "The Fourth on Third Level"}
                                            {:topic    "The Real Fifth on Third Level"
                                             :expanded true
                                             :children [{:topic "Lone Child"}]}]}]}
                    {:topic "The Real Last Top Sibling"}]})

(deftest get-topic-test
  (testing "The 'get-topic' function"
    (is (thrown? js/Error (ct/get-topic nil (ct/tree-id-parts->tree-id-string ["root"  "2" "topic"]))))
    (let [ratom (r/atom (:tree a-tree))]
      (is (= {:topic "The First at Top"}
             (ct/get-topic ratom
                           (ct/tree-id-parts->tree-id-string ["root"  "0" "topic"]))))
      (is (= {:topic "The Second on Second Level"}
             (ct/get-topic ratom
                           (ct/tree-id-parts->tree-id-string ["root"  "1" 1 "topic"]))))
      (is (= {:topic "The Fifth on Third Level"}
             (ct/get-topic ratom
                           (ct/tree-id-parts->tree-id-string ["root"  "1" 2 4 "topic"]))))
      (is (= "The Third at Top"
             (:topic (ct/get-topic ratom
                                   (ct/tree-id-parts->tree-id-string ["root"  2 "topic"])))))
      (is (= "The Only Child of the Last at Top"
             (:topic (ct/get-topic ratom
                                   (ct/tree-id-parts->tree-id-string ["root"  2 0 "topic"])))))
      (is (= "The Real Fifth on Third Level"
             (:topic (ct/get-topic ratom
                                   (ct/tree-id-parts->tree-id-string ["root"  2 0 4 "topic"])))))
      (is (= "Lone Child"
             (:topic (ct/get-topic ratom
                                   (ct/tree-id-parts->tree-id-string ["root"  2 0 4 0 "topic"])))))
      (is (= (nil?
               (ct/get-topic ratom
                             (ct/tree-id-parts->tree-id-string ["root"  "2" 2 4 "topic"])))))
      (is (= (nil?
               (ct/get-topic ratom
                             (ct/tree-id-parts->tree-id-string ["root"  "2" 0 8 "topic"])))))
      (is (= {:topic "The Second on Second Level"}
             (ct/get-topic ratom
                           (ct/tree-id-parts->tree-id-string ["root"  "1" 1 "anything"])))))))

(deftest has-children?-test
  (testing "The 'has-children?' function"
    (let [ratom (r/atom (:tree a-tree))]
      (is (nil? (ct/has-children? ratom (ct/tree-id-parts->tree-id-string ["root"  "0" "topic"]))))
      (is (thrown? js/Error (ct/has-children? ratom nil)))
      (is (thrown? js/Error (ct/has-children? nil (ct/tree-id-parts->tree-id-string ["root"  "0" "topic"]))))
      (is (nil? (ct/has-children? ratom (ct/tree-id-parts->tree-id-string ["root"  "1" 2 4 "topic"]))))
      (is (seq (ct/has-children? ratom (ct/tree-id-parts->tree-id-string ["root"  "1" 2 "topic"]))))
      (is (seq (ct/has-children? ratom (ct/tree-id-parts->tree-id-string ["root"  "2" 0 4 "topic"])))))))

(deftest is-expanded?-test
  (testing "The 'is-expanded?' function")
  (let [ratom (r/atom (:tree a-tree))]
    (is (nil? (ct/is-expanded? ratom (ct/tree-id-parts->tree-id-string ["root"  "0" "topic"]))))
    (is (thrown? js/Error (ct/is-expanded? ratom nil)))
    (is (thrown? js/Error (ct/is-expanded? nil (ct/tree-id-parts->tree-id-string ["root"  "0" "topic"]))))
    (is (true? (ct/is-expanded? ratom (ct/tree-id-parts->tree-id-string ["root"  "1" "topic"]))))
    (is (nil? (ct/is-expanded? ratom (ct/tree-id-parts->tree-id-string ["root"  "1" 2 4 "topic"]))))
    (is (nil? (ct/is-expanded? ratom (ct/tree-id-parts->tree-id-string ["root"  "1" 2 "topic"]))))
    (is (true? (ct/is-expanded? ratom (ct/tree-id-parts->tree-id-string ["root"  "2" 0 4 "topic"]))))))

(deftest expand-node-test
  (testing "The 'expand-node' function"
    ; Make a local copy. Don't mess with the original.
    (let [local-map (:tree a-tree)
          ratom (r/atom local-map)]

      ; Check that it works for a branch that is not expanded.
      (let [node-id (ct/tree-id-parts->tree-id-string ["root"  1 2 "topic"])
            _ (ct/expand-node ratom node-id)
            ba (ct/get-topic ratom node-id)]
        (is (true? (:expanded ba))))

      ; Check that it works for a branch that is already expanded.
      (let [node-id (ct/tree-id-parts->tree-id-string ["root"  2 0 4 "topic"])
            _ (ct/expand-node ratom node-id)
            ba (ct/get-topic ratom node-id)]
        (is (true? (:expanded ba))))

      ; Check that it works for a branch that doesn't already have the keyword.
      (let [node-id (ct/tree-id-parts->tree-id-string ["root"  2 0 "topic"])
            _ (ct/expand-node ratom node-id)
            ba (ct/get-topic ratom node-id)]
        (is (true? (:expanded ba)))))))

(deftest collapse-node-test
  (testing "The 'collapse-node' function"
    ; Make a local copy. Don't mess with the original.
    (let [local-map (:tree a-tree)
          ratom (r/atom local-map)]

      ; Check that it works for a branch that is already collapsed.
      (let [node-id (ct/tree-id-parts->tree-id-string ["root"  1 2 "topic"])
            _ (ct/collapse-node ratom node-id)
            ba (ct/get-topic ratom node-id)]
        (is (nil? (:expanded ba))))

      ; Check that it works for a branch that is expanded.
      (let [node-id (ct/tree-id-parts->tree-id-string ["root"  2 0 4 "topic"])
            _ (ct/collapse-node ratom node-id)
            ba (ct/get-topic ratom node-id)]
        (is (nil? (:expanded ba))))

      ; Check that it works for a branch that doesn't already have the keyword.
      (let [node-id (ct/tree-id-parts->tree-id-string ["root"  2 0 "topic"])
            _ (ct/collapse-node ratom node-id)
            ba (ct/get-topic ratom node-id)]
        (is (nil? (:expanded ba)))))))

(deftest toggle-node-expansion-test
  (testing "The 'toggle-node-expansion' function"
    ; Make a local copy. Don't mess with the original.
    (let [local-map (:tree a-tree)
          ratom (r/atom local-map)]

      ; Check that it works for a branch that is collapsed.
      (let [node-id (ct/tree-id-parts->tree-id-string ["root"  1 2 "topic"])
            _ (ct/toggle-node-expansion ratom node-id)
            ba (ct/get-topic ratom node-id)]
        (is (true? (:expanded ba))))

      ; Check that it works for a branch that is expanded.
      (let [node-id (ct/tree-id-parts->tree-id-string ["root"  2 0 4 "topic"])
            _ (println "topic: " (ct/get-topic ratom node-id))
            _ (ct/toggle-node-expansion ratom node-id)
            ba (ct/get-topic ratom node-id)]
        (println "ba: " ba)
        (is (false? (:expanded ba))))

      ; Check that it works for a branch that doesn't already have the keyword.
      (let [node-id (ct/tree-id-parts->tree-id-string ["root"  2 0 "topic"])
            _ (ct/toggle-node-expansion ratom node-id)
            ba (ct/get-topic ratom node-id)]
        (is (nil? (:expanded ba)))))))

(deftest remove-top-level-sibling!-test
  (testing "The 'remove-top-level-sibling!' function.")
  ; Make a local copy. Don't mess with the original.
  (let [local-map (:tree a-tree)
        ratom (r/atom local-map)
        org-topic-cnt (count local-map)]

    ; Assure that faulty tree arguments are caught.
    (is (nil? (ct/remove-top-level-sibling! nil 0)))
    (is (nil? (ct/remove-top-level-sibling! (r/atom "") 0)))
    (is (nil? (ct/remove-top-level-sibling! (atom {}) 0)))
    (is (nil? (ct/remove-top-level-sibling! [] 0)))

    ; Check behavior when the index is out of bounds.
    (is (nil? (ct/remove-top-level-sibling! ratom -1)))
    (is (nil? (ct/remove-top-level-sibling! ratom 4500)))

    ; Check behavior when deleting first sibling (top topic).
    (let [top-id (ct/tree-id-parts->tree-id-string ["root"  0 "topic"])
          top-topic-txt (:topic (ct/get-topic ratom top-id))
          _ (ct/remove-top-level-sibling! ratom 0)
          new-top-topic-txt (:topic (ct/get-topic ratom top-id))]
      (is (not= top-topic-txt new-top-topic-txt))
      ; ASSUMES DELETION WORKED.
      (is (= (count @ratom) (dec org-topic-cnt))))

    ; Check behavior of deleting last sibling. ASSUMES PREVIOUS
    ; TEST WORKED.
    (let [last-topic-id (ct/tree-id-parts->tree-id-string ["root"  2 "anything"])
          last-topic-txt (:topic (ct/get-topic ratom last-topic-id))
          _ (ct/remove-top-level-sibling! ratom 2)
          new-last-topic-id (ct/tree-id-parts->tree-id-string ["root"  1 "something"])
          new-last-topic-txt (:topic (ct/get-topic ratom new-last-topic-id))]
      (is (not= last-topic-txt new-last-topic-txt))
      (is (= (count @ratom) (- org-topic-cnt 2))))))


(deftest remove-child!-test
  (testing "The 'remove-child!' function.")
  ; Make a local copy. Don't mess with the original.
  (let [local-map (:tree a-tree)
        ratom (r/atom local-map)]

    ; Assure that faulty tree arguments are caught.
    (is (nil? (ct/remove-top-level-sibling! nil 0)))
    (is (nil? (ct/remove-top-level-sibling! (r/atom "") 0)))
    (is (nil? (ct/remove-top-level-sibling! (atom {}) 0)))
    (is (nil? (ct/remove-top-level-sibling! [] 0)))

    ; Check behavior when the index is out of bounds.
    (is (nil? (ct/remove-child! ratom -1)))
    (is (nil? (ct/remove-child! ratom 4500)))

    ))