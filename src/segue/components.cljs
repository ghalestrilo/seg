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
  (let [help-content @(rf/subscribe [:help])
        expanded false] ;TODO: move this state to :interface
    [:box { :top 0
            :style {:border {:fg :magenta}}
            :border {:type :line}
            :label " Help "
            :height "100%"
            :width "100%"}
      ; for [[idx [value label]] (map-indexed vector options)]
      ; (println help-content)
      (for [[idx [keyname action]] (map-indexed vector help-content)]
        [:text {:key idx :top idx :left 1} (str keyname " - " action)])]))


(defn sidebar
  "Display a help box on the corner of the screen with contextual usage information

  Takes a hash map of props:
  :items [str] - A list of current keybindings, one per line

  Returns a reagent hiccup view element."
  [{:keys [items label]} & children]
  [:box { :top 0
          :style {:border {:fg :magenta}}
          :border {:type :line}
          :label (or label " Sidebar ")
          :right 0
          :width "25%"
          :height "100%"}
    children])

(defn selection-display
  []
  (let [selection true])
  [:box {}
    [:text "haha"]])

