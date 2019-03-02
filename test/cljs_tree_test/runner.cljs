(ns ^:figwheel-always cljs-tree-test.runner
  (:require [cljs.test :refer-macros [run-tests]]
            [cljs-tree-test.vector-util-test]))

(defn run-all-tests
  []
  (run-tests 'cljs-tree-test.vector-util-test))