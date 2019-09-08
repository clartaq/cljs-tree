;;;;
;;;; This namespace includes functions used to explore the contents of objects
;;;; that are not well know (for now) during development. They are not meant
;;;; to be included in production code.
;;;;

(ns cljs-tree.exploratory)

(defn unpack-keyboard-event
  "Unpack all of the information in a keyboard event and return a map
  of the contents."
  [evt]
  {:alt-key            (.-altKey evt)
   :char-code          (.-charCode evt)
   :cmd-key            (or (.-metaKey evt) (.-ctrlKey evt))
   :code               (.-code evt)
   :ctrl-key           (.-ctrlKey evt)
   :get-modifier-state (.getModifierState evt)
   :is-composing       (.-isComposing evt)
   :key                (.-key evt)
   :key-code           (.-keyCode evt)
   :location           (.-location evt)
   :meta-key           (.-metaKey evt)
   :repeating?         (.-repeat evt)
   :shift-key          (.-shiftKey evt)
   :which              (.-which evt)})

(defn js-obj->keys
  "Return an alphabetized vector of all of the keys in the JavaScript object."
  [js-obj]
  (sort (js-keys js-obj)))

(defn js-obj->functions
  "Return an alphabetized vector of the names of all of the functions supported
  by the JavaScript object."
  [obj]
  (let [filt-fn (fn [key]
                  (let [v (goog.object/get obj key)]
                    (when (= "function" (goog/typeOf v))
                      key)))]
    (sort (into [] (filter filt-fn (.getKeys goog/object obj))))))

