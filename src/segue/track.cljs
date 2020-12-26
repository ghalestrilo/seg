(ns segue.track
  (:require
    [cljs.nodejs :as nodejs]
    [re-frame.core :as rf]
    [clojure.spec.alpha :as s]
    [clojure.string :as string]))


(s/def ::filename string?)
;(s/def ::syntax   #{:tidal :foxdot})

(def syntax-map
  {:hs    :tidal
   :tidal :tidal
   :py    :foxdot})

; TODO:
; 1. Move this to segue.plugins
; 2. Select and label final regexes
(def plugins
  {:tidal
    { :regexes
      { :statement #"^(.+)(\n*( ).*)*"   ; t1 (top-level) regex. Will be run before all
        :variable-block #"^let (.|\n)+"  ; t2: Will be run on statements
        :block   #"( *).*(\n|(\1) +.*)*" ; FIXME: Deprecate in favor of statement
        
        :channel #"(?<=\sp *\")([^\".]*)|(?<=d)[1-8]" ; t2: will be run on statements
          ; TODO: substitute lookback

        :pattern #"\$( |)\w+ .*"
        :channel-command #"( +)p( |)\"(.|\n\1( +))*"
        ;:section #"do(\n|^).*?(?=\n|$)"
        ;:section #"do\n(\s)(.*\n\1)*.*"
        :section #"do\n( +)?(.*\n( +).*)*"
        :section-name #"(?<=-- @name( ))\w+" ; TODO: substitute lookback
        :setup        #"do(.|\n)*-- @setup(.|\n[( )+\t])+"} ; t2: will be run on statements
      ;:boot "ghci -ghci-script /home/ghales/git/libtidal/boot.tidal"}})
      :boot "ghci -ghci-script /home/ghales/git/libtidal/boot.tidal"
      :prep-command
        { :pre  ":{"
          :post ":}"}}})

(defn get-plugin
  []
  (:tidal plugins))

; 1. Set syntax to state
; 2. Functions subscribe to state in order to get syntax



; TODO: Regexes on file reading
; (defn! load-file)
; tidal
; - block: ( *).*(\n|(\1) +.*)*
; - section: do(\n|^).*?(?=\n|$)
;    shape: { variables (local): [], patterns,  }
; - variable: 
; - paragraph: (\ *(p\ *)\"\w+\"|d[1-8])((?:[^\n][\n]?)+)
;     extracts individual command paragraphs
; - channel: /(p\ )"\w+"|d[1-8]/
;     with lookbehind: (?<=p \")([^\".]*)|(?<=d)[1-8]
;     extracts channels from paragraph
; - pattern modifier: /\$(\ |)\w+ .*/
;     extracts pattern modifiers
; - effect: /\#(\ |)\w+ ".*"/
;     extracts effects from paragraph
; - comment: /--.*/

; (\1{,1})*

(defn node-slurp [path]
    (let [fs (nodejs/require "fs")]
        (.readFileSync ^js fs path "utf8")))

(defn read-file
  [filename]
  ;TODO: Validate file existance
  (node-slurp filename))

(defn get-track-field
  [content syntax-def fieldname & all]
  (let [db @(rf/subscribe [:db])
        syntax-name (:syntax db)]
    (println syntax-name)
    (-> syntax-def fieldname (re-seq content))))
  
(defn get-matches
  ""
  [regex strings]
  (->> strings
    (map first)
    (map #(re-find regex %))
    (filter some?)
    (into [])))

(defn get-syntax-field
  [fieldname strings]
  (let [syntax (:regexes (get-plugin))] ; TODO: subscribe to syntax, substituting :tidal
    (get-matches (get fieldname syntax) strings)))

(defn fassoc
  "Associate with the return of a function call on self"
  [the-map key f]
  (assoc the-map key (f the-map)))

; TODO: Retrieve channels from global state array
; NOTE:  This requires pushing to global state before calling this function.
;        which means refactoring parse-content
; IDEA: Create (set-track-field) events
;  set :syntax before calling anything

; IDEA: section-parsing algorithm:
; 1. Build channel-pattern map (iterate section, convert strings to keywords)
; 2. Map current channels to keywords, use keywords to retrieve map values

(defn get-section-name
  [text]
  (-> #"(?<=-- @name( ))\w+"
      (re-find text)
      first
      (or "?")))

(defn parse-section
  [section-text]
  (-> {}
    (assoc :definition section-text)
    ; (assoc :patterns (get-matches))
    (assoc :name (get-section-name section-text))))

; TODO: Move code @151 here
(defn get-sections
  [{:keys [block]}]
  ())

(defn get-setup
  [{:keys [block]}]
  ())

(defn get-variables
  [{:keys [block]}]
  (->> block
    (get-matches (:section regexes))
    (map (partial string/join ""))
    flatten))
  ;"d2 $ s \"sn*2\" # orbit 0")

;; FIXME: This code is hideous
(defn parse-content
  [content & {:keys [syntax]}]
  (let [regexes (:regexes (get-plugin))
        assoc-track-field #(assoc %1 %2 (get-track-field content regexes %2))]
    (-> {}
      (assoc-track-field :channel)
      (update-in [:channel] #(-> % flatten distinct)) ; filter duplicate channels
      (assoc-track-field :block)
      (assoc-track-field :statement)
      (fassoc :channels :channel)
      (dissoc :channel)

      ; FIXME: This is a workaround for an incorrect regex
      (fassoc :section-definitions
              #(->> %
                    :block
                    (get-matches (:section regexes))
                    (map (partial string/join ""))
                    flatten))

      ;(#(assoc % :sections (:section-definitions %)))
      ;(dissoc :block)
      (fassoc :sections #(->> % :section-definitions (into []) (map parse-section)))
      (dissoc :section-definitions)
      (assoc  :setup "d1 $ s \"bd\"")
      ;(fassoc :variables get-variables)
      (dissoc :block)
      identity)))


(defn state-assoc
  [key value]
  (rf/dispatch-sync [:update {key value}]))
  
(defn update-track
  [key value]
  (rf/dispatch-sync [:update-track {key value}]))


; FIXME: not sure this works at all
(defn prep-command
  [command]
  (let [track @(rf/subscribe [:track])]
    ;(if-let [{:keys [pre post]} (-> track :syntax plugins :prep-command)]
    (let [pre ":{\n" post "\n:}"]
      (println "command: " command)
      (clojure.string/join " " [pre (str command) post]))))
    ;command))

; TODO: This is currently getting the definition directly
; It should instead build the definition from the block data
(defn prep-section
  [section]
  (println "section:" (:definition section))
  (->> section :definition str prep-command)) ; FIXME: This returns nil
  ;section)


(defn run-track-setup
  [track]
  (if-let [setup (:setup track)]
    (rf/dispatch-sync [:eval setup] setup)
    (println "[info] track has no setup block: " track))
  (if-let [variables (:variables track)]
    (rf/dispatch-sync [:eval variables] variables)
    (println "[info] track has no variables block: " track)))


(defn load-track
  [filename]
  (let [extension (-> filename (string/split ".") last)
        syntax    (-> extension keyword syntax-map)]
    ;(rf/dispatch-sync [:set-track] {})
    ;(rf/dispatch-sync [:repl-kill] {})
    (state-assoc :syntax extension)
    (update-track :syntax syntax)
    (state-assoc :file filename)
    (state-assoc :track (-> filename read-file (parse-content syntax)))
    ; TODO: Start process here with plugin boot command
    (rf/dispatch-sync [:repl-start (-> syntax plugins :boot)])
    (run-track-setup @(rf/subscribe [:track]))))
      

