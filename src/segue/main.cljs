(ns segue.main
  "Main application entrypoint. Defines root UI view, cli-options,
  arg parsing logic, and initialization routine"
  (:require
   [clojure.tools.cli :refer [parse-opts]]
   [mount.core :refer [defstate] :as mount]
   [re-frame.core :as rf]
   [reagent.core :as r]
   [segue.core :refer [render screen]]
   [segue.demo.views :refer [home]]
   [segue.events]
   [segue.resize :refer [size]]
   [segue.subs]
   [segue.track :refer [read-file load-track]]
   [segue.views :as views]))

(defn ui
  "Root ui view.
  Takes no arguments.
  Returns hiccup home element to run the home app."
  [_]
  (let [view @(rf/subscribe [:view])]
    [home {:view view}]))
    
(def cli-options
  [["-p" "--port PORT" "port number"
    :default 80
    :parse-fn #(js/Number %)
    :validate [#(< 0 % 0x10000) "Must be a number between 0 and 65536"]]
   ["-v" nil "Verbosity level"
    :id :verbosity
    :default 0
    :update-fn inc]
   ["-h" "--help"]
   ["-d" "--debug" :id :debug-mode]
   ["--no-repl" :id :repl-disabled]])

(defn args->opts
  "Takes a list of arguments.
  Returns a map of parsed CLI args."
  [args]
  (parse-opts args cli-options))

(defn init!
  "Initialize the application.
  Takes a root UI view function that returns a hiccup element and optionally
  a map of parsed CLI args.
  Returns rendered reagent view."
  [view & {:keys [opts]}]
  (mount/start)
  (rf/dispatch-sync [:init (:options opts) (size @screen)])
  (if-let [filename (-> opts :arguments first)]
    (load-track filename))
  (-> (r/reactify-component view)
      (r/create-element #js {})
      (render @screen)))

(defn main!
  "Main application entrypoint function. Initializes app, renders root UI view
  and initializes the re-frame app db.
  Takes list of CLI args.
  Returns rendered reagent view."
  [& args]
  (init! ui :opts (args->opts args)))

(set! *main-cli-fn* main!)
