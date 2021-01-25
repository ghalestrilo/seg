(ns segue.track
  (:require
    ;[cljs.nodejs :as nodejs]
    [re-frame.core :as rf]
    [clojure.spec.alpha :as s]
    [clojure.string :as string]
    [segue.wrappers :refer [node-slurp]]
    [segue.plugins :refer  [get-plugin unparse get-nodes
                            legacy-plugins legacy-get-plugin legacy-get-regexes]])) ;; FIXME delete legacy code
    

;(defn node-slurp [path]
;    (let [fs (nodejs/require "fs")]
;        (.readFileSync ^js fs path "utf8")))

(s/def ::filename string?)
;(s/def ::syntax   #{:tidal :foxdot})

(def syntax-map
  {:hs    :tidal
   :tidal :tidal
   :py    :foxdot})



; ---------------------------------------------------------- FIXME: Legacy Code

; FIXME: This should be constructed from its parts
(defn get-definition
  [section]
  (:definition section))

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
  "receives a regex re and a list of strings [s]
    returns a list of all full matches of re each string in the list
    note: re-find returns a list when there are parenthesized regexes
      where the first element is the full match (hence map first)
      and the rest are partial matches"
  [regex strings]
  (->> strings
    (map first)
    (map #(re-find regex %))
    (filter some?)
    (into [])))


(defn exclude-matches
  "receives a regex re and a list of strings [s]
    returns a list of all strings that do not match that regex"
  [regex strings]
  (if regex
    (filter (comp nil? (partial re-find regex)) strings) 
    strings))


(defn get-syntax-field
  [fieldname strings]
  (let [syntax (:regexes (legacy-get-plugin))] ; TODO: subscribe to syntax, substituting :tidal
    (get-matches (get fieldname syntax) strings)))

(defn fassoc
  "Associate with the return of a function call on self"
  [the-map key f]
  (assoc the-map key (f the-map)))

(defn get-section-name
  [text]
  (-> #"(?<=-- @name( ))\w+"
      (re-find text)
      first
      (or "?")))

(defn get-section-statements
  [section]
  (let [regexes (legacy-get-regexes)]
    (->>  (get-definition section)
          (re-seq (:section-statement regexes))
          (map first) ; pick largest match
          flatten
          (into []))))

(defn get-pattern-list
  [section]
  (let [regexes       (legacy-get-regexes)
        channels      (->> @(rf/subscribe [:track]) :channels (map keyword))
        statements    (:statements section)
        channel-regex (:channel regexes)

    ; Extract channel maps from statemment block
        statement-map (->> statements
                        (map str)
                        (map #(seq (re-find channel-regex %) %))
                        (filter #(-> % second some?))
                        (map (fn [[name idx]] (hash-map (keyword name) idx)))
                        (into {}))]

    ; Find out what each channel does in that section (Access statement map)
    (map #(get statement-map %) channels)))

(defn parse-section
  [section-text]
  (let [regexes (legacy-get-regexes)]
    (-> {}
      (assoc  :definition section-text)
      (fassoc :statements get-section-statements)  
      (fassoc :patterns get-pattern-list)
      (assoc  :name (get-section-name section-text)))))

; TODO: Move code @151 here
(defn get-sections
  [{:keys [block]}]
  ())

(defn get-setup
  [{:keys [block]}]
  ;"d1 $ s \"bd\""
  (let [regexes (legacy-get-regexes)]
    (->> block
        (get-matches (:setup regexes))
        (map (partial string/join ""))
        flatten
        first)))

(defn get-section-definitions
  [{:keys [block]}]
  (let [regexes (legacy-get-regexes)]
    (->> block
        (get-matches (:section regexes))
        (map (partial string/join ""))
        flatten
        (into []))))

(defn get-sections
  [{:keys [section-definitions]}]
  (let [regexes (legacy-get-regexes)]
    (->> section-definitions
         (exclude-matches (:setup regexes))
         (into [])
         (map parse-section)
         (into []))))

(defn get-variables
  [{:keys [block]}]
  (let [regexes (legacy-get-regexes)]
    (->> block
      (get-matches (:variable-block regexes))
      (map (partial string/join ""))
      flatten
      first)))

;; FIXME: This code is hideous
(defn legacy-parse-content
  [content & {:keys [syntax]}]
  (let [regexes (:regexes (legacy-get-plugin))
        assoc-track-field #(assoc %1 %2 (get-track-field content regexes %2))]
    (-> {}
      (assoc-track-field :channel)
      (update-in [:channel] #(-> % flatten distinct)) ; filter duplicate channels
      (assoc-track-field :block)
      (assoc-track-field :statement)
      (fassoc :channels :channel)
      (dissoc :channel)

      ; FIXME: This is a workaround for an incorrect regex
      (fassoc :section-definitions get-section-definitions)
      (fassoc :sections #(->> % :section-definitions (into []) (map parse-section)))

      (dissoc :section-definitions)
      (fassoc :variables get-variables)
      (fassoc :setup     get-setup)
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
    ; FIXME: use pre and post as defined in syntax
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

; ---------------------------------------------------------- FIXME: /Legacy Code

(defn run-track-setup
  [track]
  (if-let [setup (:setup track)]
    (rf/dispatch-sync [:eval setup] setup)
    (println "[info] track has no setup block: " track))
  (if-let [variables (:variables track)]
    (rf/dispatch-sync [:eval variables] variables)
    (println "[info] track has no variables block: " track)))


(defn parse-content
  [file-content syntax-name]
  (let [plugin      (get-plugin syntax-name)
        parser      (:parser plugin)
        content     (parser file-content)
        get-content (partial get-nodes content)]
    (println (get-content :variable))
    (println (get-content :section))
    content))
  


(defn load-track
  [filename]
  (let [extension (-> filename (string/split ".") last)
        syntax    (-> extension keyword syntax-map)]
    (state-assoc :syntax extension)
    (update-track :syntax syntax)
    (state-assoc :file filename)

    (state-assoc :track (-> filename read-file (parse-content syntax)))

    ; FIXME: LEGACY CODE
    (state-assoc :track (-> filename read-file (legacy-parse-content syntax)))
    (rf/dispatch-sync [:repl-start (-> syntax legacy-plugins :boot)])
    (run-track-setup @(rf/subscribe [:track]))))

