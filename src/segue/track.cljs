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
      ;:section #"do(\n|^).*?(?=\n|$)"
      ;:section #"do\n(\s)(.*\n\1)*.*"
      :section #"do\n( +)?(.*\n( +).*)*"}})
      




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
  [regex strings]
  ;(filter #(some? (re-matches regex %)) strings)
  (->> strings
    ;(map #(join "\n" %))
    (map first)
    (map #(re-find regex %))
    (filter some?)
    (into [])))
  ;(println regex))
  ;(->> strings first str println))
  ;(->> strings first str (re-seq regex)))
  ;strings)

(defn fassoc
  "Associate with the return of a function call on self"
  [the-map key f]
  (assoc the-map key (f the-map)))
  

(defn parse-content
  [content & {:keys [syntax]}]
  (let [regexes (:tidal syntaxes)
        ; get-matches #(re-seq (-> %1 regexes) %2)
        ; get-matches #(re-seq (-> %1 regexes) %2)
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
      ;(fassoc :sections #(identity []))
      ;(dissoc :section-definitions)
      identity)))
      
  

(defn load-track
  [filename]
  (let [trackdata (-> filename read-file (parse-content :tidal))]
    trackdata))
