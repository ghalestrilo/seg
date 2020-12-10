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
                        ["k" "up"]    #(swap! selected-row dec)
                        ["j" "down"]  #(swap! selected-row inc)}
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
                [:text {:style {:bg "magenta"}} (str idx " = " @selected-player)]]))]))))

; Reactive deref not supported in lazy seq, it should be wrapped in doall: ([:box {:key 0, :left 10, :width 6} [:text "me"]] [:box {:key 1, :left 16, :width 6} [:text "p2"]])

; TODO: How do I move the selection up/down?
(defn session-section-mode
  [{:keys [section-data selected toggle-mode select-next select-prev]}]
  (with-keys @screen {["j" "down"]  select-next
                      ["k" "up"]    select-prev
                      ["l" "right"] toggle-mode
                      ["e" "enter"] #(if on-select (on-select @selected)
                                                   (println "[session-section-mode] No on-select callback!"))}
    (let []
      [:listtable { :data section-data
                    :vi true
                    :style {:selected {:bold true :bg "magenta"}
                            :header   {:bold true}}}])))
      
      

