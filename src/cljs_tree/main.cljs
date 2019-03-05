;;;
;;; This namespace simply starts the main demo page. It exists so that the
;;; testing framework can start up without running the demo in the browser.
;;;


(ns cljs-tree.main
  (:require [cljs-tree.core :as core]))

(enable-console-print!)

(println "cljs-tree.main calling core/start")

(core/start)
