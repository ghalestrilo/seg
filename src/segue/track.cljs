(ns segue.track
  (:require
    [cljs.nodejs :as nodejs]
    [clojure.spec.alpha :as s]
    [clojure.string :as string]))


(s/def ::filename string?)
;(s/def ::syntax   #{:tidal :foxdot})

(def syntaxes
  {:tidal
    { :block   #"( *).*(\n|(\1) +.*)*"
      :channel #"(?<=\sp *\")([^\".]*)|(?<=d)[1-8]"
      :pattern #"\$( |)\w+ .*"
      :channel-command #"( +)p( |)\"(.|\n\1( +))*"
      ;:section #"do(\n|^).*?(?=\n|$)"
      ;:section #"do\n(\s)(.*\n\1)*.*"
      :section #"do\n( +)?(.*\n( +).*)*"
      :section-name #"(?<=-- @name( ))\w+"}})
      

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
  [content syntax-def fieldname]
  (-> syntax-def fieldname (re-seq content)))
  
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
  (let [syntax (:tidal syntaxes)]
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

(defn parse-content
  [content & {:keys [syntax]}]
  (let [regexes (:tidal syntaxes)
        assoc-track-field #(assoc %1 %2 (get-track-field content regexes %2))]
    (-> {}
      (assoc-track-field :channel)
      (update-in [:channel] #(-> % flatten distinct)) ; filter duplicate channels
      (assoc-track-field :block)

      ; FIXME: This is a workaround for an incorrect regex
      (fassoc :section-definitions
              #(->> %
                    :block
                    (get-matches (:section regexes))
                    (map (partial string/join ""))
                    flatten))
      ;(#(assoc % :sections (:section-definitions %)))
      (dissoc :block)
      (fassoc :sections #(->> % :section-definitions (into []) (map parse-section)))
      ;(dissoc :section-definitions)
      identity)))
      
  

(defn load-track
  [filename]
  (let [trackdata (-> filename read-file (parse-content :tidal))]
    trackdata))
