(ns segue.repl
  (:require []))

(defn repl [a b c]
  ""
  (let [state (reagent/atom {})] ;; you can include state
    (reagent/create-class
      {:component-did-mount
       (fn [] (println "I mounted"))

       ;; ... other methods go here

       ;; name your component for inclusion in error messages
       :display-name "complex-component"

       ;; note the keyword for this method
       :reagent-render
       (fn [a b c]
         [:div {:class c}
           [:i a] " " b])})))
