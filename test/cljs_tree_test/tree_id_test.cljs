(ns ^:figwheel-always cljs-tree-test.tree-id-test
  (:require [cljs-tree.core :as ct]
            [cljs.test :refer-macros [deftest is testing]]))

(defonce ts ct/topic-separator)

(deftest tree-id->tree-id-parts-test
  (testing "The 'tree-id->tree-id-parts' function")
  (is (= ["root" "0" "topic"] (ct/tree-id->tree-id-parts
                                (str "root" ts 0 ts "topic"))))
  (is (nil? (ct/tree-id->tree-id-parts nil)))
  (is (nil? (ct/tree-id->tree-id-parts ""))))

(deftest tree-id-parts->tree-id-string-test
  (testing "The 'tree-id-parts->tree-id-string-test"
    (is (= (str "root" ts 0 ts "topic") (ct/tree-id-parts->tree-id-string
                                          ["root" 0 "topic"])))
    (is (= (str "root" ts 0 ts "topic") (ct/tree-id-parts->tree-id-string
                                          ["root" "0" "topic"])))
    (is (nil? (ct/tree-id-parts->tree-id-string nil)))
    (is (nil? (ct/tree-id-parts->tree-id-string {})))))

(deftest roundtrip-test
  (testing "That the above two functions produce a correct round trip of the data"
    (is (= ["root" "0" "topic"] (ct/tree-id->tree-id-parts
                                  (ct/tree-id-parts->tree-id-string
                                    ["root" "0" "topic"]))))
    (is (= ["root" "0" "topic"]
           (ct/tree-id->tree-id-parts
             (ct/tree-id-parts->tree-id-string ["root" "0" "topic"]))))))

(deftest is-top-tree-id?-test
  (testing "The 'is-top-tree-id?' function"
    (is (true? (ct/is-top-tree-id? (str "root" ts "0" ts "topic"))))
    (is (true? (ct/is-top-tree-id? (str "root" ts "0" ts "anything"))))
    (is (false? (ct/is-top-tree-id? "root-0-topic")))))

(deftest tree-id->nav-index-vector-test
  (testing "The 'tree-id->nav-index-vector' function"
    (is (= ["0"] (ct/tree-id->nav-index-vector (str "root" ts 0 ts "topic"))))
    (is (= ["0"] (ct/tree-id->nav-index-vector (str "root" ts "0" ts "topic"))))
    (is (= ["0"] (ct/tree-id->nav-index-vector
                   (str "root" ts 0 ts "anything"))))
    (is (= ["0"] (ct/tree-id->nav-index-vector
                   (str "root" ts "0" ts "anything"))))
    (is (= ["0" "5" "32"] (ct/tree-id->nav-index-vector
                            (str "root" ts 0 ts 5 ts 32 ts "topic"))))
    (is (= ["0" "5" "32"] (ct/tree-id->nav-index-vector
                            (str "root" ts "0" ts "5" ts "32" ts "topic"))))))

(deftest nav-index-vector->tree-id-string-test
  (testing "The 'nav-index-vector->tree-id' function"
    (is (= (str "root" ts 0 ts "topic")
           (ct/nav-index-vector->tree-id-string [0])))
    (is (= (str "root" ts 0 ts "topic")
           (ct/nav-index-vector->tree-id-string ["0"])))
    (is (= (str "root" ts 0 ts "anything")
           (ct/nav-index-vector->tree-id-string [0] "anything")))
    (is (= (str "root" ts 0 ts "anything")
           (ct/nav-index-vector->tree-id-string ["0"] "anything")))
    (is (= (str "root" ts 0 ts 5 ts 32 ts "topic")
           (ct/nav-index-vector->tree-id-string [0 5 32])))
    (is (= (str "root" ts 0 ts 5 ts 32 ts "topic")
           (ct/nav-index-vector->tree-id-string ["0" "5" "32"])))))