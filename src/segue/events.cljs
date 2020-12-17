(ns segue.events
  "Event handlers for re-frame dispatch events.
  Used to manage app db updates and side-effects.
  https://github.com/Day8/re-frame/blob/master/docs/EffectfulHandlers.md"
  (:require
    [re-frame.core :as rf]
    [segue.track :refer [read-file load-track]]))
; Below are very general effect handlers. While you can use these to get
; going quickly you are likely better off creating custom handlers for actions
; specific to your application.

(def help-messages
  { :home
    { :up/down    "Choose section/pattern"
      :left/right "Choose player"
      :enter      "Trigger section"}})

; IDEA: maybe the demo track could become a "template" user setting
(def demo-track
  {:players      ["drums" "piano" "bass" "vibe"]
   :sections     [{:name "intro"
                   :patterns [nil    nil     "0"    nil]}
                  {:name "theme"
                   :patterns [nil    nil     "0"    nil]}
                  {:name "bridge"
                   :patterns [nil    "0*4"   "0"    nil]}
                  {:name "outro"
                   :patterns [nil    nil     "0"    "0(3,8)"]}]
                  
   :pattern-bank []})
    


(rf/reg-event-db
  :init
  (fn [db [_ opts terminal-size]]
    {:opts opts
     :router/view :home
     :terminal/size terminal-size
     :dialog/help help-messages
     :track demo-track}))

; FIXME: Remove unnecessary fields
(rf/reg-event-db
  :load-file
  (fn [db [_ file]]
    (-> db
     (assoc :file file)
     (assoc :content (read-file file))          
     (assoc :track-content (load-track file)))))

(rf/reg-event-db
  :update
  (fn [db [_ data]]
    (merge db data)))

(rf/reg-event-db
  :set
  (fn [db [_ data]]
    data))
