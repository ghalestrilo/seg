(ns segue.repl
  (:require
    [reagent.core :as r]
    [re-frame.core :as rf]))

(defn repl
  [_]
  (let [state (r/atom {})
        restart #(println "time to restart the repl")] ;; you can include state
    [:text {:bold true :content "hello"}]))



