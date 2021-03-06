(ns segue.events
  "Event handlers for re-frame dispatch events.
  Used to manage app db updates and side-effects.
  https://github.com/Day8/re-frame/blob/master/docs/EffectfulHandlers.md"
  (:require
    [re-frame.core :as rf]
    [segue.track :refer [read-file parse-section prep-section load-track]]
    [segue.repl :refer [spawn-process repl-send]]
    [segue.wrappers :refer [node-slurp]]))
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
    

(def default-settings
  { :column-width 12
    :shell "zsh"  ;TODO: check if process exists
    :editor "kak"})

(rf/reg-event-db
  :init
  (fn [db [_ opts terminal-size]]
    {:opts opts
     :settings default-settings
     :router/view :home
     :terminal/size terminal-size
     :dialog/help help-messages
     :session/selection 0
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
    ; Case 1: Playing a section
    (if-let [repl         (:repl db)]
      (if-let [section      (-> db :track :sections (nth row {}))]
        ;(println "section:" (prep-section section))
        (-> repl :process (repl-send (prep-section section))))) ; FIXME: find section definition
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
    (update-in db [:repl :messages]
      #(->> message
        (str %)
        (take-last 5000) ; improve this logic to account for newline characters
        (clojure.string/join "")
        (str)))))

(rf/reg-event-db
  :repl-start
  (fn [db [_ command]]
    (let [proc (spawn-process command)] ;FIXME: Should read from plugin
      (assoc-in db [:repl :process] proc)))) 

(rf/reg-event-db
  :eval
  (fn [db [_ command]]
    (if-let [{:keys [repl]} db]
      (-> repl :process (repl-send command)))    
    db))

(rf/reg-event-db
  :update-selection
  (fn [db [_ selection]]
    (if-let [{:keys [track]} db]
      (->> selection
        (max 0)
        (min (-> track :sections count (- 1)))
        (assoc db :session/selection)))))

(rf/reg-event-db
  :edit-file
  (fn [db [_ filename]]
    (let [editor-process (-> db :editor :process)]
      (if (some? editor-process)
          (.kill ^js editor-process "SIGTERM"))
      (let [{keys [editor]} (:settings db)]
        (-> db
          (assoc-in [:editor :filename] filename)
          (assoc-in [:router/view] :edit))))))

(defn consume-temp-file
  "Pushes content of temp file to current selection"
  [db]
  (if-let [filename (-> db :editor :filename)]
    (let [selection (:session/selection db)
          new-def   (node-slurp filename)]
      (update-in db [:track :sections]
        #(assoc-in (into [] %) [selection] (parse-section new-def))))
    db))

(rf/reg-event-db
  :navigate
  (fn [db [_ view]]
    (-> db
      ((if (= view :home) consume-temp-file identity))
      (assoc-in [:router/view] view))))
