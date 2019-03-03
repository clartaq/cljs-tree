;;;-----------------------------------------------------------------------------
;;; Tests of tree data structure manipulations.

(ns ^:figwheel-always cljs-tree-test.tree-data-test
  (:require [cljs-tree.core :as ct]
            [cljs.test :refer-macros [deftest is testing]]))

(defonce ts ct/topic-separator)

(deftest lower?-test
  (testing "The 'lower?' function"
    (is (true? (ct/lower?
                 (str "root" ts 0 ts 5 ts "topic")
                 (str "root" ts 0 ts "topic"))))
    (is (false? (ct/lower?
                  (str "root" ts 0 ts "topic")
                  (str "root" ts 0 ts 5 ts "topic"))))
    ; Check against equal
    (is (false? (ct/lower?
                  (str "root" ts 5 ts 6 ts 3 ts "topic")
                  (str "root" ts 5 ts 6 ts 3 ts"topic"))))
    (is (true? (ct/lower?
                  (str "root" ts 5 ts 7 ts 3 ts "topic")
                  (str "root" ts 5 ts 6 ts 2 ts"topic"))))))