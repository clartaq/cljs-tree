;; This test runner is intended to be run from the command line
(ns cljs-tree-test.test-runner
  (:require
    [cljs-tree-test.tree-data-test]
    [cljs-tree-test.tree-id-test]
    [cljs-tree-test.undo-redo-test]
    [cljs-tree-test.vector-util-test]
    [figwheel.main.testing :refer [run-tests-async]]))

(defn -main [& args]
  (run-tests-async 5000))
