(ns segue.demo.views
  (:require
   [clojure.pprint :refer [pprint]]
   [clojure.string :refer [join]]
   [re-frame.core :as rf]
   [reagent.core :as r]
   [segue.views :refer [router vertical-menu horizontal-selector help]]))

(defn navbar
  "Displays a blessed js box with a vertical-menu used for navigation.
  User can use j/k or up/down to navigate items and either enter or l to view
  a page. Dispatches re-frame :update to set :router/view in app db.
  Returns a hiccup :box vector."
  [_]
  [:box#home
   {:top    0
    :left   0
    :width  "30%"
    :height "50%"
    :style  {:border {:fg :cyan}}
    :border {:type :line}
    :label  " Menu "}
   [vertical-menu {:options {:home "Intro"
                             :terminal "Terminal"}
                   :bg :magenta
                   :fg :black
                   :on-select #(rf/dispatch [:update {:router/view %}])}]])

; (with-keys @screen {["j" "down"]  #(swap! selected next-option options)
;                         ["k" "up"]    #(swap! selected prev-option options)
;                         ["l" "enter"] #(on-select @selected)})
; (let [loops (->> patterns (count) (range 0)) options (zipmap (->> loops (map str) (map keyword)) loops)] options)
    
(defn session
  [_]
 (let [width 10
       players [{:name "p1" :patterns [ "0 0 0*2 0"]}
                {:name "p2" :patterns [ "0(3,8)" "0 0" "0*4" "degrade 8 $ \"0 0\""]}]]
      [:box {:top 0
             :style {:border {:fg :magenta}}
             :border {:type :line}
             :label " Session "
             :right 0
             :width "100%"
             :height "100%"}
            [horizontal-selector {:options players}]])) 
        ;[:box {:left 0     :width width} [player-column (merge {:active true}  (nth players 1))]]
        ;[:box {:left width :width width} [player-column (merge {:active false} (nth players 1))]]]))
        
        


(defn demo
  "Main demo UI wrapper.

  Takes a hash-map and a hiccup child vector:

  hash-map:
  :view keyword - Current view keyword that maps to one of the views below.

  child:
  Typically something like a hiccup [:box ...] vector

  Returns hiccup :box vector."
  [{:keys [view]} child]
  [:box#base {:left   0
              :right  0
              :width  "100%"
              :height "100%"}
   (when (not= view :loader) [navbar])
   [router {:key "2"
            :views {:home session}
            :view view}]
   [help {:key "1" :items
                      ["up/down - choose pattern"
                       "left/right - choose player"]}]
   child])
