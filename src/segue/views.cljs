(ns segue.views
  "General views and helpers"
  (:require
   [reagent.core :as r]
   [segue.core :refer [screen]]
   [segue.keys :refer [with-keys]]))


(defn router
  "Takes a map of props:
  :views map     - Map of values (usually keywords) to hiccup view functions
  :view  keyword - Current view to display. Should be the key of :views map

  Returns the hiccup vector returned by the selected view-fn.

  Example:
  (router {:views {:home home-fn
                   :about about-fn}
           :view :home})
  "
  [{:keys [views view] :as props}]
  [(get views view) props])

(defn- find-index
  "Takes a target value and a map of options.
  Returns index of target value if found in map of options or nil if target
  was not found."
  [target options]
  (some->> options
           (keys)
           (map-indexed vector)
           (filter (fn [[idx v]] (= v target)))
           (first)
           (first)))

(defn- next-option
  "Takes the current keyword key of the options map and a map of options.
  Returns the next key of the map or the first key of the map."
  [current options]
  (let [total (count options)
        current-idx (find-index current options)
        next-idx (inc current-idx)]
    (-> options
        (vec)
        (nth (if (< next-idx total) next-idx 0))
        (key))))

(defn- prev-option
  "Takes the current keyword key of the options map and a map of options.
  Returns the previous key of options map or the last key of the options map."
  [current options]
  (let [total (count options)
        current-idx (find-index current options)
        prev-idx (dec current-idx)]
    (-> options
        (vec)
        (nth (if (< prev-idx 0) (dec total) prev-idx))
        (key))))

(defn vertical-menu
  "Display an interactive vertical-menu component.

  Takes a hash-map of props:
  :bg        keyword|str - Background color of highlighted item.
  :box       hash-map    - Map of props to merge into menu box properties.
  :default   keyword     - Selected options map keyword key
  :fg        keyword|str - Text color of highlighted item.
  :on-select function    - Function to call when item is selected
  :options   hash-map    - Map of keyword keys to item labels

  Returns a reagent hiccup view element.

  Example:
  (vertical-menu
   {:bg :cyan
    :box {:top 3}
    :default :a
    :fg :white
    :on-select #(println \"selected: \" %)
    :options {:a \"Item A\"
              :b \"Item B\"
              :c \"Item C\"}})"
  [{:keys [bg box default fg on-select options active]}]
  (r/with-let [selected (r/atom (or default (->> options first key)))]
    (with-keys @screen (if active {["j" "down"]  #(swap! selected next-option options)
                                   ["k" "up"]    #(swap! selected prev-option options)
                                   ["l" "enter"] #(on-select @selected)}
                                  {})
      (let [current @selected]
        [:box#menu
         (merge
          {:top 1
           :left 1
           :right 1
           :bottom 1}
          box)
         (for [[idx [value label]] (map-indexed vector options)]
          [:text {:key value
                  :top idx
                  :style {:bg (when (= value current) (or bg :green))
                          :fg (when (= value current) (or fg :white))
                          :transparent (not active)}
                  :width "100%"
                  :height 1
                  :content label}])]))))
            

; TODO: Move this
(def colors
  {:default {:bg :green
             :fg :white}
   :dim     {:bg :green
             :fg :white}})

(defn player-column
  [{:keys [name patterns active on-select]}]
  (let [loops (->> patterns (count) (range 0) (map #(str "pat" %)))
        dakeys (->> loops (map keyword))
        options (zipmap dakeys loops)]
    [vertical-menu {:options options
                    :active active
                    :on-select on-select ; #(rf/dispatch [:update {:router/view %}]) ; FIXME: Update this callback to display-pattern
                    :fg :black
                    :bg :magenta
                    :box {:scrollable true 
                          :label name
                          :border {:type :none}
                          :style {:border {:fg :magenta}
                                  :bold true}}}]))

(defn player-grid
  "Receives a set of players and renders an interactive grid to control pattern playback
  
  Receives a map:
  
  Returns a hiccup element"
  [{:keys [bg box default fg options column-width selected-row toggle-mode]}]
  (r/with-let [selected-player (r/atom 0)]
    (with-keys @screen {["h" "left"]  #(if (zero? @selected-player)
                                           (toggle-mode)
                                           (swap! selected-player dec))
                                            
                        ["l" "right"] #(swap! selected-player inc)
                        ["k" "up"]    select-prev
                        ["j" "down"]  select-next}
      (let [width (or column-width 6)
            offset 10
            player-selected? #(= selected-player-index %)]
        [:box {:top 0}
          [:box [:text "players"]]
            ;(for sections)
          (doall  
            (for [[idx player] (map-indexed vector options)]
              [:box { :key idx
                      :left (->> idx (* width) (+ offset))
                      :width width
                      :style (if (= @selected-player idx)
                                 {:bg "magenta"}
                                 {:bg "transparent"})}
                [:text {:style {:bg "magenta"}}
                       (str idx " = " @selected-player)]]))]))))

; Reactive deref not supported in lazy seq, it should be wrapped in doall: ([:box {:key 0, :left 10, :width 6} [:text "me"]] [:box {:key 1, :left 16, :width 6} [:text "p2"]])

(defn treat-nil-pattern
  "Helper function for session view
  Takes a list of patterns
  Replaces nil with \"nil\" string"
  [patlist]
  (map #(if (nil? %) " " %) patlist))

;TODO: I decided to implement my own listtable component so here's the deal
; 1. Find out why the patterns are being rendered wrong here
; 2. Make line-wise selection work
; 3. Make callbacks
  ; space: start/stop
  ; e: edit
  ; c/enter: choose section
  ; m + idx(s): mute idxes



; FIXME: "active" does not update. why? (always false/nil)
(defn text-cell
  "Wrapper for text, which can be styled through high-level props
  content:      str | The text that will be rendered
  active:      bool | Whether or not to bold the text
  highlighted: bool | Whether or not to highlight the text
  "
  [{:keys [content active highlighted left right top]}]
  (let [highlight (if highlighted { :bg "magenta" :fg "black" } { :bg "transparent" :fg "white"})]
    [:box (merge {:style highlight} { :height 1 :left left :top top})
      [:text (->> highlight
                  (merge {:bold active})
                  (array-map :style)
                  (merge {:content content}))]]))

(defn section-row
  "Displays a track section as a named row of patterns
  Receives two maps:
  1. Display data:
    top          int | Offset from top of parent
    cell-width   int | Width of each column in characters
    active      bool | Wether or not to display name in bold
    highlighted bool | Wether or not to highlight entire row
  2. Section data:
    key          str | unique key (html) for the section
    name         str | Name of section, as defined in the track
    patterns     str | List of patterns which the section will match to the channels
  "
  [{:keys [active top cell-width highlighted]} {:keys [key name patterns]}]
  [:box { :top top
            :key key
            :width "100%"}
      [text-cell {:key (str "section" idx "-title") 
                  :content name
                  :highlighted highlighted
                  :is-active active}]
      ; Section Patterns (Horizontal list)
      (for [[pat-idx pattern] (map-indexed vector patterns)]
        [text-cell
          {:key (str "sec" section-idx "-pat" pat-idx) 
            :highlighted highlighted
            :left (-> pat-idx inc (* 10))
            :content pattern}])])

(defn session-section-mode
  "...docstring"
  [{:keys [channel-data section-data playback-data selected on-select
           toggle-mode select-next select-prev play-pattern]}]
  ; Render
  (let [selected-row selected
        active-section (-> @playback-data :section)
        width 10]
      (with-keys @screen {["k" "up"]    select-prev
                          ["j" "down"]  select-next
                          ["l" "right"] toggle-mode
                          ["e" "enter"] #(if on-select (on-select) (println "[session-section-mode] No on-select callback!"))}
        [:box
          [section-row {:key "header"}
                       {:patterns @channel-data}]
          (for [[section-idx section] (map-indexed vector @section-data)]
            [section-row {:highlighted (= section-idx selected-row)
                          :key (str "section" number)
                          :top (inc section-idx)
                          :active (= active-section section-idx)}
                      section])])))
