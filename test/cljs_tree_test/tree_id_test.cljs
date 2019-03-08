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

(deftest tree-id->sortable-nav-string-test
  (testing "The 'tree-id->sortable-nav-string' function"
    (is (= "0" (ct/tree-id->sortable-nav-string
                 (str "root" ts "0" ts "topic"))))
    (is (= "2-5-32" (ct/tree-id->sortable-nav-string
                      (str "root" ts "2" ts "5" ts "32" ts "anything"))))))

(deftest increment-leaf-index-test
  (testing "The 'increment-leaf-index' function"
    (is (= (str "root" ts "1" ts "topic")
           (ct/increment-leaf-index (str "root" ts "0" ts "topic"))))
    (is (= (str "root" ts "2" ts "5" ts "33" ts "anything")
           (ct/increment-leaf-index
             (str "root" ts "2" ts "5" ts "32" ts "anything"))))))

(deftest change-tree-id-type-test
  (testing "The 'change-tree-id-type' function"
    (is (= (str "root" ts 0 ts "topic")
           (ct/change-tree-id-type (str "root" ts 0 ts "anything") "topic")))
    (is (= (str "root" ts "2" ts "5" ts "33" ts "franjooly")
           (ct/change-tree-id-type
             (str "root" ts "2" ts "5" ts "33" ts "anything") "franjooly")))))

(deftest insert-child-index-into-parent-id-test
  (testing "The insert-child-index-into-parent-id' function"
    ; Child index can be integer or string.
    (is (= (str "root" ts "2" ts "5" ts "33" ts "topic")
           (ct/insert-child-index-into-parent-id
             (str "root" ts "2" ts "5" ts "anything") 33)))
    (is (= (str "root" ts "2" ts "5" ts "33" ts "topic")
           (ct/insert-child-index-into-parent-id
             (str "root" ts "2" ts "5" ts "anything") "33")))))

(deftest tree-id->tree-path-nav-vector-test
  (testing "The 'tree-id->tree-path-nav-vector' function"
    (is (= [0] (ct/tree-id->tree-path-nav-vector
                 (str "root" ts 0 ts "topic"))))
    (is (= [2 :children 5 :children 33]
           (ct/tree-id->tree-path-nav-vector
             (str "root" ts "2" ts "5" ts "33" ts "anything"))))))

(deftest tree-id->nav-vector-and-index-test
  (testing "The 'tree-id->nav-vector-and-index' function"
    (let [nvi {:path-to-parent []
               :child-index    0}]
      (is (= nvi (ct/tree-id->nav-vector-and-index
                   (str "root" ts 0 ts "topic")))))
    (let [nvi {:path-to-parent [2 :children 5]
               :child-index    33}]
      (is (= nvi (ct/tree-id->nav-vector-and-index
                   (str "root" ts "2" ts "5" ts "33" ts "anything")))))))