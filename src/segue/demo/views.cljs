(ns segue.demo.views
  (:require
   [clojure.pprint :refer [pprint]]
   [clojure.string :refer [join]]
   [re-frame.core :as rf]
   [reagent.core :as r]
   [segue.views :refer [router vertical-menu player-grid]]
   [segue.components :refer [help]]))

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


(defn treat-nil-pattern
  "Helper function for session view
  Takes a list of patterns
  Replaces nil with \"nil\" string"
  [patlist]
  (map #(if (nil? %) " " %) patlist))

(defn session
  [_]
 (let [width 10
       grid-mode (r/atom true)
       players @(rf/subscribe [:players])
       sections @(rf/subscribe [:sections])
       old-players [{:name "p1" :def "# s \"supervibe\" # gain 0.8" :patterns [ "0 0 0*2 0"]}
                    {:name "p2" :def "# s \"gretsch\" # gain 0.8"   :patterns [ "0(3,8)" "0 0" "0*4" "degrade 8 $ \"0 0\""]}]]
      [:box {:top 0
             :style {:border {:fg :magenta}}
             :border {:type :line}
             :label " Session "
             :right 0
             :width "100%"
             :height "100%"}
        (if (true? @grid-mode)
            (let [section-data (->> sections (map :patterns)
                                             (map treat-nil-pattern)
                                             (into [players]))]
              [:listtable {:data section-data
                           :default 0
                           :style {:selected {:bold true}}}])
            [player-grid {:options old-players}])])) 



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
