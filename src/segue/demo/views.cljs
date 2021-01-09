(ns segue.demo.views
  (:require
   [clojure.pprint :refer [pprint]]
   [clojure.string :refer [join]]
   [re-frame.core :as rf]
   [reagent.core :as r]
   [segue.core :refer [screen]]
   [segue.views :refer [router vertical-menu player-grid session-section-mode]]
   [segue.components :refer [help sidebar selection-display]]
   [segue.repl :refer [repl]]
   [segue.keys :refer [with-keys]]))

; TODO: Move to routines.cljs (Event Sequencers)
(defn edit-section
  []
  (let [selection @(rf/subscribe [:selection-content])
        settings  @(rf/subscribe [:settings])]
    (let [{keys [editor]} settings]
      (rf/dispatch [:edit-file "~/.filename"]))))


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

(defn session-view
  [_]
  (r/with-let
       [{:keys [column-width]} @(rf/subscribe [:settings])
        grid-mode      (r/atom true)
        row            (rf/subscribe [:selection])
        channels       (rf/subscribe [:channels])
        sections       (rf/subscribe [:sections])
        playback-data  (rf/subscribe [:playback])
        toggle-mode    #(swap! grid-mode not)
        play-pattern   #(rf/dispatch [:play-pattern @row %2])
        select-next    #(rf/dispatch [:update-selection (+ @row 1)])
        select-prev    #(rf/dispatch [:update-selection (- @row 1)])
        ;edit-section   #(rf/dispatch [:play-pattern 0 %2])
        old-channels [ {:name "p1" :def "# s \"supervibe\" # gain 0.8" :patterns [ "0 0 0*2 0"]}
                       {:name "p2" :def "# s \"gretsch\" # gain 0.8"   :patterns [ "0(3,8)" "0 0" "0*4" "degrade 8 $ \"0 0\""]}]]
    (fn [_]
      [:box
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
                    :on-select  #(doall (for [[idx channel] (map-indexed vector @channels)] (play-pattern % idx)))
                    :edit-section edit-section
                    :section-data  sections
                    :channel-data  channels
                    :playback playback-data
                    :selected    @row
                    :column-width column-width}]
              [player-grid
                {:options old-channels}
                :toggle-mode toggle-mode
                :play-pattern play-pattern])]        
        [:box { :bottom 0
                :height "60%"
                :style {:border { :fg :magenta}}
                :border {:type :line}}
          [repl]]
        [sidebar { :label " Section Preview "}
          selection-display
          help]])))

(defn editor-view [props]
  (r/create-class
    { :displayName "editor-view"
      :component-did-mount
        (fn [component]
          (do (.focus (r/dom-node component))
              (-> (r/dom-node component)
                  ;(.-process) ; FIXME: What prop + callback do I have to access to detect return of child process
                  (.on "close" #(rf/dispatch [:navigate :home])))))
                  
      
      :component-did-update #(rf/dispatch [:navigate :home])
      
      :reagent-render
      (let [{:keys [column-width editor]} @(rf/subscribe [:settings])]
        (fn [props]
          [:terminal
            { :parent @screen
              :cursor "block"
              :cursorBlink true
              :screenKeys false
              :label " editor "
              :args ["-c" editor]
              :left 0
              :right 0
              :width  "100%"
              :height "100%"
              :border "line"
              :style {:fg "default"
                      :bg "default"
                      :focus {:border {:fg "green"}}}}]))}))


(defn home
  "Main wrapper.

  Takes a hash-map and a hiccup child vector:

  hash-map:
  :view keyword - Current view keyword that maps to one of the views below.

  child:
  Typically something like a hiccup [:box ...] vector

  Returns hiccup :box vector."
  [{:keys [view]} child]
  (with-keys @screen
    {["C-home"] #(rf/dispatch [:navigate :home])}
    [:box#base {:left   0
                :right  0
                :width  "100%"
                :height "100%"}
      [router {:key "2"
                :views {:edit editor-view
                        :home session-view}
                :view view}]
      child]))
