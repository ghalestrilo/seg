(ns segue.demo.views
  (:require
   [clojure.pprint :refer [pprint]]
   [clojure.string :refer [join]]
   [re-frame.core :as rf]
   [reagent.core :as r]
   [segue.views :refer [router vertical-menu]]))

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



; TDDO: Move to Components
(defn player
  [& player-info]
  (let [
        ; {:keys [name patterns]} player-info
        ; patlist (or patterns [])
        ; patlist (-> player-info :patterns (or []))
        ; name (-> player-info :name (or "test"))
        ; patlist (:patlist player-info)
        ; patlist (:name player-info)
        name "haha"
        patlist []
        ]
  [:box
    {:top 0
    :left 0
    :width 12
    :height "100%"
    :label (or name "Player1")
    }
    (vertical-menu
      {:on-select #()
       ; :options (zipmap (-> patlist (count) (range)) patlist)
       :options patlist
       })]))


(defn session
  "The main session view"
  [_]
  ; (let [players @(rf/subscribe [:track/players])]
  (let [players [{ :name "1" :patterns [ "0 0 0*2 0" ]}]]
  [:box#session
   {:top 0
    :right 0
    :width "70%"
    :height "50%"
    :style {:border {:fg :magenta}}
    :border {:type :line}
    :label " Session "}
    (for [player players]
    
   [:box#content
    {:top 1
     :left 1
     :right 1
     :label (:name (nth players 0))
     }
     ;FIXME: This line breaks
      ; [:player {}]
      ]
    )
      ]))

(defn terminal-view
  [_]
  [:box {} "haha"])

(comment 
(defn loader
  "Shows a mock-loader progress bar for dramatic effect.
  - Uses with-let to create a progress atom
  - Uses a js interval to update it every 15 ms until progress is 100.
  - Starts the timer on each mount.
  - Navigates to home page when completed.
  Returns hiccup :box vector."
  [_]
  (r/with-let [progress (r/atom 0)
               interval (js/setInterval #(swap! progress inc) 15)]
    (when (>= @progress 100)
      (js/clearInterval interval)
      (rf/dispatch [:update {:router/view :home}]))
    [:box#loader
     {:top 0
      :width "100%"}
     [:box
      {:top 1
       :width "100%"
       :align :center
       :content "Loading Demo"}]
     [:box
      {:top 2
       :width "100%"
       :align :center
       :style {:fg :gray}
       :content "Slow reveal for dramatic effect..."}]
     [:progressbar
      {:orientation :horizontal
       :style {:bar {:bg :magenta}
               :border {:fg :cyan}
               :padding 1}
       :border {:type :line}
       :filled @progress
       :left 0
       :right 0
       :width "100%"
       :height 3
       :top 4
       :label " progress "}]]))
)
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
   [router {:views {:terminal terminal-view
                    :home session}
            :view view}]
   child])
