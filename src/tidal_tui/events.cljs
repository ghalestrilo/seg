(ns tidal-tui.events
  "Event handlers for re-frame dispatch events.
  Used to manage app db updates and side-effects.
  https://github.com/Day8/re-frame/blob/master/docs/EffectfulHandlers.md"
  (:require [re-frame.core :as rf]))

; Below are very general effect handlers. While you can use these to get
; going quickly you are likely better off creating custom handlers for actions
; specific to your application.

(rf/reg-event-db
  :init
  (fn [db [_ opts terminal-size]]
    {:opts opts
     :router/view :session
     :terminal/size terminal-size}))

(rf/reg-event-db
  :update
  (fn [db [_ data]]
    (merge db data)))

(rf/reg-event-db
  :set
  (fn [db [_ data]]
    data))


; TODO: Remove this / replace with:
; 1. Receive arg (command line)
; 2. Read file, fill 
; Future improvements:
; Tidal should be a plugin, offering regexes for its players + patterns
; Support for additional user regexes for finding players + patterns
(rf/reg-event-db
  :read-file
  (fn [db [_]]
    (merge db {:track/players
      [{:name "1" :patterns []}
       {:name "2" :patterns []}]})))