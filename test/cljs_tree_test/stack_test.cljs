;;;-----------------------------------------------------------------------------
;;; Tests for functions in the cljs-tree.s namespace.

(ns cljs-tree-test.stack-test
  (:require [cljs.test :refer-macros [deftest is testing]]
           ; [reagent.core :as r]
            [cljs-tree.stack :as s]))

(deftest created-stack-is-empty-test
  (testing "That newly created stacks are empty."
    (is (not (nil? (s/mk))))
    (is (empty? (s/mk)))
    (is (s/mt? (s/mk)))
    (is (zero? (s/sz (s/mk))))))

(deftest pushing-test
  (testing "That push produces expected results."
    (is (= [1 2 3] (-> (s/mk)
                       (s/ps 1)
                       (s/ps 2)
                       (s/ps 3))))
    (is (= [1 2 3 4 5 6] (-> (s/mk)
                             (s/ps 1)
                             (s/ps 2)
                             (s/ps 3)
                             (s/ps 4)
                             (s/ps 5)
                             (s/ps 6))))
    (is (= ["abc" \Q 42 1.2345] (-> (-> (s/mk)
                                        (s/ps "abc")
                                        (s/ps \Q)
                                        (s/ps 42))
                                    (s/ps 1.2345))))))

(deftest peek-pop-test
  (testing "That peek and pop functions work as expected."
    (let [s (-> (s/mk)
                (s/ps 1)
                (s/ps 2)
                (s/ps 3)
                (s/ps 4))]
      (is (= 4 (s/pk s)))
      (is (= [1 2 3] (s/pp s)))))
  (testing "That attempting to pop an empty stack produces nothing."
    (is (nil? (s/pp (s/mk)))))
  (testing "That attempting to peek an empty stack produces nothing."
    (is (nil? (s/pk (s/mk))))))

(deftest clear-test
  (testing "That the s clear function returns an empty stack."
    (let [s (-> (s/mk)
                (s/ps 1)
                (s/ps 2)
                (s/ps 3)
                (s/ps 4))]
      (is (not (s/mt? s)))
      (is (s/mt? (s/clr s)))
      (is (s/mt? (s/clr (s/mk)))))))

(deftest make-with-args-test
  (testing "That making a stack with initial elements works."
    (is (= [1 2 3 4] (s/mk '(1 2 3 4))))
    (is (= [[1 2 3 4]] (s/mk '([1 2 3 4]))))
    (is (= [[1 2] [3 4]] (s/mk '([1 2] [3 4]))))))

(deftest push-multiples-test
  (testing "That pushing multiple elements at once works."
    (is (= [1 2 3 4] (s/ps (s/mk) '(1 2 3 4))))
    (is (= [[1 2 3 4]] (s/ps (s/mk) '([1 2 3 4]))))
    (is (= [[1 2] [3 4]] (s/ps (s/mk) '([1 2] [3 4]))))
    (is (= [1 2 3 "a" "b" "c"] (s/ps (s/mk '(1 2 3)) '("a" "b" "c"))))))

(deftest size-test
  (testing "That the size function returns the number of elements in the stack."
    (is (zero? (s/sz (s/mk))))
    (is (= 4 (s/sz (-> (s/mk)
                       (s/ps 1)
                       (s/ps 2)
                       (s/ps 3)
                       (s/ps 4)))))
    (is (= 4 (s/sz (s/mk '(1 2 3 4)))))
    (is (= 1 (s/sz (s/mk '([1 2 3 4])))))
    (is (= 2 (s/sz (s/mk '([1 2] [3 4])))))))