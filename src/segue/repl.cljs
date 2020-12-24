(ns segue.repl
  (:require
    [reagent.core :as r]
    [re-frame.core :as rf])
  (:require-macros [cljs.core.async.macros :refer [go alt!]])
  (:require [cljs.core.async :refer [<! take! chan close! put! go-loop >!] :as async])
  (:require ["child_process" :refer [spawn]]))


; TODO: Steps
; 1. make this component a box to the bottom of the screen
; 2. render some dummy text inside
; 3. try to spawn a child process inside it 
(defn repl
  [_]
  (let [state (r/atom {})
        content (r/atom "hello")
        restart #(println "time to restart the repl")
        channel (chan)
        messages (r/atom [])
        command "ghci -ghci-script /home/ghales/git/libtidal/boot.tidal"]
    (go-loop []
      (let [v (<! channel)]
        (if (done-message? v)
          (do
            (println "closing channel")
            (close! channel))
          (do
            (println (.toString v "UTF8"))
            (recur)))))
    (let [proc (spawn "zsh" (clj->js ["-c" command]))]
      (.on (.-stdout proc) "data" #(do (println %) (reset! content (str %))))
      
    ;  (.on (.-stdout proc) "data"  #(put! channel %))
    ;  (.on (.-stderr proc) "data"  #(put! channel %))
    ;  (.on proc "close" #(put! channel [:done %])))
      (fn [_]
        [:text {:bold true :content @content}]))))



; In the future, we want 2 running behaviors:
; standalone: default behavior, spawns and manages processes
; tmux: delegates process management to tmux, redirecting i/o to process.
;   some unix wizardry may be necessary to grab the PID and send/receive data to/from the process


(comment

  (ns testing.core
    (:require-macros [cljs.core.async.macros :refer [go alt!]])
    (:require [cljs.core.async :refer [<! take! chan close! put! go-loop >!] :as async])
    (:require ["child_process" :refer [spawn]]))

  (defn done-message? [message]
    (and
      (vector? message)
      (= (message 0) :done)))

  (defn main! []
    (let [channel (chan)
          command "echo 1 && sleep 1 && echo 2 && sleep 1 && echo 3"]

      (go-loop []
        (let [v (<! channel)]
          (if (done-message? v)
            (do
              (println "closing channel")
              (close! channel))
            (do
              (println (.toString v "UTF8"))
              (recur)))))

      (let [proc (spawn "bash" (clj->js ["-c" command]))]
        (.on (.-stdout proc) "data"  #(put! channel %))
        (.on (.-stderr proc) "data"  #(put! channel %))
        (.on proc "close" #(put! channel [:done %]))))))
