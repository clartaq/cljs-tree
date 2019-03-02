(ns ^:figwheel-always cljs-tree-test.vector-util-test
  (:require
    [cljs-tree.core :as ct]
    [cljs-tree.vector-utils :as v]
    [cljs.test :as t]
    [cljs.test :refer-macros [deftest is testing]]))

;(deftest fake-failing-test
;  (testing "fake failing description"
;    (is (= 1 2))))

(enable-console-print!)

(println "Loading cljs-tree-test.core-test")

;;------------------------------------------------------------------------------
;; Tests for vector manipulation functions.

;-------------------------------------------------------------------------------
; Element deletion functions.

(deftest delete-at-test
  (testing "The 'delete-at' function"
    (is (= [] (v/delete-at [3] 0)))
    (is (= ["a" "c"] (v/delete-at ["a" "b" "c"] 1)))
    (is (thrown? js/Error (v/delete-at ["a" "b" "c"] 5)))
    (is (thrown? js/Error (v/delete-at nil 5)))
    (is (thrown? js/Error (v/delete-at ["a" "b" "c"] -5)))
    (is (= ["b" "c"] (v/delete-at ["a" "b" "c"] 0)))
    (is (= [\a \b] (v/delete-at [\a\b\c] 2)))))

(deftest remove-first-test
  (testing "The 'remove-first' function"
    (is (= [2 3] (v/remove-first [1 2 3])))
    (is (= [] (v/remove-first [1])))
    (is (thrown? js/Error (v/remove-first [])))
    (is (thrown? js/Error (v/remove-first nil)))))

(deftest remove-last-test
  (testing "The 'remove-last' function"
    (is (= [] (v/remove-last [0])))
    (is (= [1 2] (v/remove-last [1 2 3])))
    (is (thrown? js/Error (v/remove-last [])))
    (is (thrown? js/Error (v/remove-last nil)))))

(deftest remove-last-two-test
  (testing "The 'remove-last-two' function"
    (is (= [1] (v/remove-last-two [1 2 3])))
    (is (= [] (v/remove-last-two [1 2])))
    (is (thrown? js/Error (v/remove-last-two [1])))
    (is (thrown? js/Error (v/remove-last-two [])))
    (is (thrown? js/Error (v/remove-last-two nil)))))

;-------------------------------------------------------------------------------
; Insert, replace and append functions.

(deftest insert-at-test
  (testing "The 'insert-at' function"
    (is (= [1] (v/insert-at [] 1 1)))
    (is (= ["a" 1] (v/insert-at [1] 0 "a")))
    (is (= ["a" 1] (v/insert-at [1] -5 "a")))
    (is (= [5] (v/insert-at [] 4 5)))
    (is (= [7] (v/insert-at [] 0 7)))
    ; NOTE: This might be unexpected
    (is (= '(5)(v/insert-at nil 0 5)))
    (is (thrown? js/Error (v/insert-at [1 2 3] nil 5)))
    ; NOTE: This might be unexpected
    (is (= [123 4 nil] (v/insert-at [123 4] 4 nil)))))

(deftest replace-at-test
  (testing "The 'replace-at function"
    (is (thrown? js/Error (v/replace-at [] 0 1)))
    (is (= [1] (v/replace-at ["x"] 0 1)))
    (is (= [9 2 3 4 5] (v/replace-at [1 2 3 4 5] 0 9)))
    (is (= ["a"] (v/replace-at [1] 0 "a")))
    (is (thrown? js/Error (v/replace-at [1] -5 "a")))
    (is (= [1 2 3 4 9 6 7] (v/replace-at [1 2 3 4 5 6 7] 4 9)))
    (is (= [1 2 3 4 5 6 "Testing"] (v/replace-at [1 2 3 4 5 6 7] 6 "Testing")))
    (is (= [7] (v/replace-at [1] 0 7)))
    (is (thrown? js/Error(v/replace-at nil 0 5)))
    (is (thrown? js/Error (v/replace-at [1 2 3] nil 5)))
    (is (thrown? js/Error (v/replace-at [123 4] 4 nil)))))

(deftest append-element-to-vector-test
  (testing "The 'append-element-to-vector' function")
  (is (= ["Funny"] (v/append-element-to-vector [] "Funny")))
  (is (= [1 2 3] (v/append-element-to-vector [1 2] 3)))
  ; NOTE: This might be unexpected.
  (is (= [3] (v/append-element-to-vector nil 3)))
  ; NOTE: This might be unexpected.
  (is (= [1 2 nil] (v/append-element-to-vector [1 2] nil))))
