(ns segue.events
  "Event handlers for re-frame dispatch events.
  Used to manage app db updates and side-effects.
  https://github.com/Day8/re-frame/blob/master/docs/EffectfulHandlers.md"
  (:require
    [re-frame.core :as rf]
    [segue.track :refer [read-file load-track]]
    [segue.repl :refer [spawn-process]]))
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

(rf/reg-event-db
  :update
  (fn [db [_ data]]
    (merge db data)))

(rf/reg-event-db
  :set
  (fn [db [_ data]]
    data))

(rf/reg-event-db
  :update-track
  (fn [db [_ path data]]
    (assoc-in db (into [:track] path) data)))

(rf/reg-event-db
  :set-track
  (fn [db [_ data]]
    (assoc db :track data)))

(rf/reg-event-db
  :play-pattern
  (fn [db [_ row column]]
    (println "playing" row column)
    (assoc db :playback {:section row
                         :patterns (-> db :track :channels count (take (repeat row)))})))

; FIXME: This does not kill the process yet
(rf/reg-event-db
  :repl-kill
  (fn [db [_]]
    (if-let [{:keys [repl]} db]
        (do (println "killing current process")
            (-> repl :process (.kill "SIGINT"))
            (dissoc db :repl)))))

(rf/reg-event-db
  :repl-update-message
  (fn [db [_ message]]
    (for [i (range 1000)] (println message))
    (update-in db [:repl :messages]
      #(-> % (or []) (concat [message])))))
  

(rf/reg-event-db
  :repl-start
  (fn [db [_ command]]
    (let [proc (spawn-process command)] ;FIXME: Should read from plugin
      (.on (.-stdout proc) "data" #(rf/dispatch-sync [:repl-update-message (str %)] (str %)))
      (assoc-in db [:repl :process] proc)))) 
        
