(ns segue.repl
  (:require
    [reagent.core :as r]
    [re-frame.core :as rf])
  (:require-macros [cljs.core.async.macros :refer [go alt!]])
  (:require [cljs.core.async :refer [<! take! chan close! put! go-loop >!] :as async])
  (:require ["child_process" :refer [spawn]]))

(defn spawn-process
  "Takes a command, starts a node process with that command inside user-configured terminal
   and returns the process"
  [command]
  (let [term (-> @(rf/subscribe [:settings]) :shell)] ; FIXME: Should be user preference
    (let [proc (spawn term (clj->js ["-c" command]))
          send-msg #(rf/dispatch-sync [:repl-update-message (str %)])
          kill-repl #(.kill proc "SIGINT")]
      (for [event ["exit" "error" "close"]]
        (.on js/process event kill-repl)) 
      (.on (.-stdout proc) "data" send-msg)
      (.on (.-stderr proc) "data" send-msg)
      proc)))
      


; TODO: Does not work yet!
(defn repl-send
  [proc message]
  (-> proc (.-stdin) (.write message))
  (-> proc (.-stdin) (.write "\n")))


(defn done-message? [message]
  (and
    (vector? message)
    (= (message 0) :done)))

(defn repl
  [_]
  (let [repl-data @(rf/subscribe [:repl])]
    [:box
      {:scrollable true
       :alwaysScroll true
       :valign "bottom"}
      [:text 
        { :valign "bottom"
          :bottom 1
          :clickable true
          :content (->> repl-data :messages)}]]))

; In the future, we want 2 running behaviors:
; standalone: default behavior, spawns and manages processes
; tmux: delegates process management to tmux, redirecting i/o to process.
;   some unix wizardry may be necessary to grab the PID and send/receive data to/from the process
