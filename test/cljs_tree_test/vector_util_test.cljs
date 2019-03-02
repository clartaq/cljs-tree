(ns ^:figwheel-always cljs-tree-test.vector-util-test
  (:require
    [cljs-tree.core :as ct]
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
    (is (= [] (ct/delete-at [3] 0)))
    (is (= ["a" "c"] (ct/delete-at ["a" "b" "c"] 1)))
    (is (thrown? js/Error (ct/delete-at ["a" "b" "c"] 5)))
    (is (thrown? js/Error (ct/delete-at nil 5)))
    (is (thrown? js/Error (ct/delete-at ["a" "b" "c"] -5)))
    (is (= ["b" "c"] (ct/delete-at ["a" "b" "c"] 0)))
    (is (= [\a \b] (ct/delete-at [\a\b\c] 2)))))

(deftest remove-first-test
  (testing "The 'remove-first' function"
    (is (= [2 3] (ct/remove-first [1 2 3])))
    (is (= [] (ct/remove-first [1])))
    (is (thrown? js/Error (ct/remove-first [])))
    (is (thrown? js/Error (ct/remove-first nil)))))

(deftest remove-last-test
  (testing "The 'remove-last' function"
    (is (= [] (ct/remove-last [0])))
    (is (= [1 2] (ct/remove-last [1 2 3])))
    (is (thrown? js/Error (ct/remove-last [])))
    (is (thrown? js/Error (ct/remove-last nil)))))

(deftest remove-last-two-test
  (testing "The 'remove-last-two' function"
    (is (= [1] (ct/remove-last-two [1 2 3])))
    (is (= [] (ct/remove-last-two [1 2])))
    (is (thrown? js/Error (ct/remove-last-two [1])))
    (is (thrown? js/Error (ct/remove-last-two [])))
    (is (thrown? js/Error (ct/remove-last-two nil)))))

;-------------------------------------------------------------------------------
; Insert, replace and append functions.

(deftest insert-at-test
  (testing "The 'insert-at' function"
    (is (= [1] (ct/insert-at [] 1 1)))
    (is (= ["a" 1] (ct/insert-at [1] 0 "a")))
    (is (= ["a" 1] (ct/insert-at [1] -5 "a")))
    (is (= [5] (ct/insert-at [] 4 5)))
    (is (= [7] (ct/insert-at [] 0 7)))
    ; NOTE: This might be unexpected
    (is (= '(5)(ct/insert-at nil 0 5)))
    (is (thrown? js/Error (ct/insert-at [1 2 3] nil 5)))
    ; NOTE: This might be unexpected
    (is (= [123 4 nil] (ct/insert-at [123 4] 4 nil)))))

(deftest replace-at-test
  (testing "The 'replace-at function"
    (is (thrown? js/Error (ct/replace-at [] 0 1)))
    (is (= [1] (ct/replace-at ["x"] 0 1)))
    (is (= [9 2 3 4 5] (ct/replace-at [1 2 3 4 5] 0 9)))
    (is (= ["a"] (ct/replace-at [1] 0 "a")))
    (is (thrown? js/Error (ct/replace-at [1] -5 "a")))
    (is (= [1 2 3 4 9 6 7] (ct/replace-at [1 2 3 4 5 6 7] 4 9)))
    (is (= [1 2 3 4 5 6 "Testing"] (ct/replace-at [1 2 3 4 5 6 7] 6 "Testing")))
    (is (= [7] (ct/replace-at [1] 0 7)))
    (is (thrown? js/Error(ct/replace-at nil 0 5)))
    (is (thrown? js/Error (ct/replace-at [1 2 3] nil 5)))
    (is (thrown? js/Error (ct/replace-at [123 4] 4 nil)))))

(deftest append-element-to-vector-test
  (testing "The 'append-element-to-vector' function")
  (is (= ["Funny"] (ct/append-element-to-vector [] "Funny")))
  (is (= [1 2 3] (ct/append-element-to-vector [1 2] 3)))
  ; NOTE: This might be unexpected.
  (is (= [3] (ct/append-element-to-vector nil 3)))
  ; NOTE: This might be unexpected.
  (is (= [1 2 nil] (ct/append-element-to-vector [1 2] nil))))
