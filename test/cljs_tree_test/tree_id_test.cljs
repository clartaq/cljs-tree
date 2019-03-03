(ns ^:figwheel-always cljs-tree-test.tree-id-test
  (:require [cljs-tree.core :as ct]
            [cljs.test :refer-macros [deftest is testing]]))

(defonce ts ct/topic-separator)

(deftest tree-id->tree-id-parts-test
  (testing "The 'tree-id->tree-id-parts' function")
  (is (= ["root" "0" "topic"] (ct/tree-id->tree-id-parts
                                (str "root" ts 0 ts "topic")))))

(deftest tree-id-parts->tree-id-string-test
  (testing "The 'tree-id-parts->tree-id-string-test"
    (is (= (str "root" ts 0 ts "topic") (ct/tree-id-parts->tree-id-string
                                          ["root" 0 "topic"])))
    (is (= (str "root" ts 0 ts "topic") (ct/tree-id-parts->tree-id-string
                                          ["root" "0" "topic"])))))

(deftest roundtrip-test
  (testing "That the above two functions produce a correct round trip of the data"
    (is (= ["root" "0" "topic"] (ct/tree-id->tree-id-parts
                                  (ct/tree-id-parts->tree-id-string
                                    ["root" "0" "topic"]))))
    (is (= ["root" "0" "topic"]
           (ct/tree-id->tree-id-parts
             (ct/tree-id-parts->tree-id-string ["root" "0" "topic"]))))))
