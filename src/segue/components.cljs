(ns segue.components
  "Components and Popups"
  (:require
   [reagent.core :as r]
   [re-frame.core :as rf]))

(defn help
  "Display a help box on the corner of the screen with contextual usage information

  Takes a hash map of props:
  :items [str] - A list of current keybindings, one per line

  Returns a reagent hiccup view element."
  [{:keys [items]}]
  (let [help-content @(rf/subscribe [:help])]
    [:box { :top 0
            :style {:border {:fg :magenta}}
            :border {:type :line}
            :label " ?? "
            :right 0
            :width "25%"
            :height "50%"}
      ; for [[idx [value label]] (map-indexed vector options)]
      ; (println help-content)
      (for [[idx [keyname action]] (map-indexed vector help-content)]
        [:text {:key idx :top idx :left 1} (str keyname " - " action)])]))

