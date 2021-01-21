(ns segue.plugins)

; TODO:
; 1. Move this to segue.plugins
; 2. Select and label final regexes
(def plugins
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

(defn get-plugin
  []
  (:tidal plugins))

(defn get-regexes
  []
  (:regexes (get-plugin)))