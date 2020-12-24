(ns segue.repl
  (:require
    [reagent.core :as r]
    [re-frame.core :as rf])
  (:require-macros [cljs.core.async.macros :refer [go alt!]])
  (:require [cljs.core.async :refer [<! take! chan close! put! go-loop >!] :as async])
  (:require ["child_process" :refer [spawn]]))

(defn done-message? [message]
  (and
    (vector? message)
    (= (message 0) :done)))


; proc.kill("SIGINT")
; (.kill proc "SIGINT")

; TODO: Steps
; Move process to state (:repl)
; Start process when track is loaded
;   Kill previous process
(defn repl
  [_]
  (let [state (r/atom {})
        content (r/atom "hello")
        channel (chan)
        messages (r/atom [])
        command "ghci -ghci-script /home/ghales/git/libtidal/boot.tidal"]
    (comment
      ; NOTE: go-loop should be at same logical level as the (let) below
      (go-loop []
        (let [v (<! channel)]
          (if (done-message? v)
            (do
              (println "closing channel")
              (close! channel))
            (do
              ;(println (.toString v "UTF8"))
              ;(reset! content (.toString v "UTF8"))
              (recur))))))
    (let [proc (spawn "zsh" (clj->js ["-c" command]))]
      (.on (.-stdout proc) "data" #(reset! content (str %)))
      
      ; (.on (.-stdout proc) "data"  #(put! channel %)) ; FIXME: Once the async loop is fixed, use this

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
