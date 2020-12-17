(ns segue.track
  (:require
    [cljs.nodejs :as nodejs]
    [clojure.spec.alpha :as s]))


(s/def ::filename string?)
(s/def ::syntax   #{:tidal :foxdot})

(def syntaxes
  {:tidal
    { :block   #"( *).*(\n|(\1) +.*)*"
      :channel #"(?<=p *\")([^\".]*)|(?<=d)[1-8]"
      :pattern #"\$( |)\w+ .*"}})


      

; TODO: Regexes on file reading
; (defn! load-file)
; tidal
; - block: ( *).*(\n|(\1) +.*)*
; - section: do(\n|^).*?(?=\n|$)
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
  

(defn parse-content
  [content & {:keys [syntax]}]
  ;(s/valid? syntax :syntax)
  (let [regexes (:tidal syntaxes)
        assoc-track-field #(assoc %1 %2 (get-track-field content regexes %2))]
    (-> {}
      (assoc-track-field :channel)
      (update-in [:channel] #(-> % flatten distinct)) ; filter duplicate channels
      identity)))
      
  

(defn load-track
  [filename]
  (let [trackdata (-> filename read-file (parse-content :tidal))]
    trackdata))
