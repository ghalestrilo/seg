(ns segue.demo.views
  (:require
   [clojure.pprint :refer [pprint]]
   [clojure.string :refer [join]]
   [re-frame.core :as rf]
   [reagent.core :as r]
   [segue.views :refer [router vertical-menu player-grid session-section-mode]]
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


; (def grid-mode (r/atom true)) ; FIXME: why does this not work inside let?

; (def s< (comp deref re-frame.core/subscribe))
; (def s> re-frame.core/dispatch)

(defn session
  [_]
  (r/with-let
       [width 10
        row         (r/atom 0)
        grid-mode   (r/atom true)
        channels       @(rf/subscribe [:channels])
        sections       @(rf/subscribe [:sections])
        playback-data  @(rf/subscribe [:db :playback])
        play-pattern   #(rf/dispatch  [:play-pattern @row %2])
        toggle-mode #(swap! grid-mode not)
        select-next #(swap! row (if (-> (rf/subscribe [:sections]) deref count (+ 1) (< @row)) identity inc)) ; FIXME: I have no clue if this is the best way to limit 
        select-prev #(swap! row (if (= @row 0) identity dec))
        old-channels [ {:name "p1" :def "# s \"supervibe\" # gain 0.8" :patterns [ "0 0 0*2 0"]}
                       {:name "p2" :def "# s \"gretsch\" # gain 0.8"   :patterns [ "0(3,8)" "0 0" "0*4" "degrade 8 $ \"0 0\""]}]]
    (fn [_]
      [:box { :top 0
              :style {:border { :fg :magenta}}
              :border {:type :line}
              :label (if @grid-mode " Choose Section " " Choose Pattern ")
              :right 0
              :width "100%"
              :height "100%"}
        (if @grid-mode
              [session-section-mode
                { :toggle-mode toggle-mode
                  :select-next select-next
                  :select-prev select-prev
                  ; :on-select  #(for [[idx channel] (map-indexed vector channels)] (do (println "playing" idx) (play-pattern % idx)))
                  :on-select  #(doall (for [[idx channel] (map-indexed vector channels)] (play-pattern % idx)))
                  :section-data  sections
                  :channel-data  channels
                  :playback-data playback-data
                  :selected    @row}]
            [player-grid
              {:options old-channels
               :toggle-mode toggle-mode
               :play-pattern play-pattern}])]))) 



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
