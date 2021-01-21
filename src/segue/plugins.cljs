(ns segue.plugins
  (:require
    [instaparse.core :as ip]
    [segue.wrappers :refer [node-slurp]]))

; WIP
(defn -js->clj+
  "For cases when built-in js->clj doesn't work. Source: https://stackoverflow.com/a/32583549/4839573"
  [x]
  (into {} (for [k (js-keys x)]
                [(keyword k) (aget x k)])))

(defn env
  "Returns current env vars as a Clojure map."
  []
  (-js->clj+ (.-env js/process)))

(def configdir
  (-> (:HOME (env))
      (str "/.seg/")))

(def plugindir (str configdir "plugins/"))

; TODO: load yaml fields
(defn load-plugin
  [plugin-name]
  (let [syntax (-> (str plugindir plugin-name ".ebnf") node-slurp ip/parser)]
    { :syntax syntax
      :props ""}))

(def plugins
  {:tidal (load-plugin "tidal")})

; FIXME: LEGACY CODE

; TODO:
; 2. Select and label final regexes
(def legacy-plugins
  {:tidal
    { :regexes
      { :statement        #"^(.+)(\n*( ).*)*"   ; t1 (top-level) regex. Will be run before all
        :variable-block   #"^let (.|\n)+"  ; t2: Will be run on statements
        :block            #"( *).*(\n|(\1) +.*)*" ; FIXME: Deprecate in favor of statement
        
        :channel          #"(?<=\sp *\")([^\".]*)|(?<=d)[1-8]" ; t2: will be run on statements

        :pattern          #"\$( |)\w+ .*"
        :channel-command  #"( +)p( |)\"(.|\n\1( +))*"
        :section          #"do\n( +)?(.*\n( +).*)*"
        :section-name     #"(?<=-- @name( ))\w+" ; TODO: substitute lookback
        :section-statement #"( +)(.*)((\n\1 )(.*))*"
        :setup            #"do(.|\n)*-- @setup(.|\n[( )+\t])+"} ; t2: will be run on statements

      :boot "ghci -ghci-script /home/ghales/git/libtidal/boot.tidal"
      :prep-command
        { :pre  ":{"
          :post ":}"}}})

(defn legacy-get-plugin
  []
  (:tidal legacy-plugins))

(defn legacy-get-regexes
  []
  (:regexes (legacy-get-plugin)))
