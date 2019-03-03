;;;-----------------------------------------------------------------------------
;;; Tests of tree data structure manipulations.

(ns ^:figwheel-always cljs-tree-test.tree-data-test
  (:require [cljs-tree.core :as ct]
            [cljs.test :refer-macros [deftest is testing]]
            [reagent.core :as r]))

(enable-console-print!
  )
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
                  (str "root" ts 5 ts 6 ts 3 ts "topic"))))
    (is (true? (ct/lower?
                 (str "root" ts 5 ts 7 ts 3 ts "topic")
                 (str "root" ts 5 ts 6 ts 2 ts "topic"))))))

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
                    {:topic    "The Last at Top"
                     :children [{:topic    "The Only Child of the Last at Top"
                                 :children [{:topic "The First on Third Level"}
                                            {:topic "The Second on Third Level"}
                                            {:topic "The Third on Third Level"}
                                            {:topic "The Fourth on Third Level"}
                                            {:topic    "The Real Fifth on Third Level"
                                             :children [{:topic "Lone Child"}]}]}]}]})

(deftest get-topic-test
  (testing "The 'get-topic' function"
    (is (thrown? js/Error (ct/get-topic nil (str "root" ts "2" ts "topic"))))
    (let [ratom (r/atom (:tree a-tree))]
      (is (= {:topic "The First at Top"}
             (ct/get-topic ratom
                           (str "root" ts "0" ts "topic"))))
      (is (= {:topic "The Second on Second Level"}
             (ct/get-topic ratom
                           (str "root" ts "1" ts 1 ts "topic"))))
      (is (= {:topic "The Fifth on Third Level"}
             (ct/get-topic ratom
                           (str "root" ts "1" ts 2 ts 4 ts "topic"))))
      (is (= "The Last at Top"
             (:topic (ct/get-topic ratom
                                   (str "root" ts 2 ts "topic")))))
      (is (= "The Only Child of the Last at Top"
             (:topic (ct/get-topic ratom
                                   (str "root" ts 2 ts 0 ts "topic")))))
      (is (= "The Real Fifth on Third Level"
             (:topic (ct/get-topic ratom
                                   (str "root" ts 2 ts 0 ts 4 ts "topic")))))
      (is (= "Lone Child"
             (:topic (ct/get-topic ratom
                                   (str "root" ts 2 ts 0 ts 4 ts 0 ts "topic")))))
      (is (= (nil?
               (ct/get-topic ratom
                             (str "root" ts "2" ts 2 ts 4 ts "topic")))))
      (is (= (nil?
               (ct/get-topic ratom
                             (str "root" ts "2" ts 0 ts 8 ts "topic")))))
      (is (= {:topic "The Second on Second Level"}
             (ct/get-topic ratom
                           (str "root" ts "1" ts 1 ts "anything")))))))

(deftest has-children?-test
  (testing "The 'has-children?' function"
    (let [ratom (r/atom (:tree a-tree))]
      (is (nil? (ct/has-children? ratom (str "root" ts "0" ts "topic"))))
      (is (thrown? js/Error (ct/has-children? ratom nil)))
      (is (thrown? js/Error (ct/has-children? nil (str "root" ts "0" ts "topic"))))
      (is (nil? (ct/has-children? ratom (str "root" ts "1" ts 2 ts 4 ts "topic"))))
      (is (seq (ct/has-children? ratom (str "root" ts "1" ts 2 ts "topic"))))
      (is (seq (ct/has-children? ratom (str "root" ts "2" ts 0 ts 4 ts "topic")))))))