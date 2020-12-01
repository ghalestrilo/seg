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

(defn horizontal-selector
  "Display an interactive horizontal selector component.

  Takes a hash-map of props:
  :bg        keyword|str - Background color of highlighted item.
  :box       hash-map    - Map of props to merge into menu box properties.
  :default   keyword     - Selected options map keyword key
  :fg        keyword|str - Text color of highlighted item.
  :options   hash-map    - Map of keyword keys to item labels
  :width     integer     - Width of each column

  Returns a reagent hiccup view element."
  [{:keys [bg box default fg options width]}]
  (r/with-let [selected (r/atom (or default (->> options first key)))]
    (with-keys @screen {["l" "right"]  #(swap! selected next-option options)
                        ["h" "left"]   #(swap! selected prev-option options)}
      (let [current @selected
            column-width (or width 10)]
        [:box#menu
         (merge
          {:top 1
           :left 1
           :right 1
           :bottom 1}
          box)
         (for [[idx [value label]] (map-indexed vector options)]
           [:box {:key value
                  :left (-> idx (* column-width) (+ 1))
                  :style {:bg (when (= value current) (or bg :green))
                          :fg (when (= value current) (or fg :white))}
                  :width column-width
                  :content label}])]))))


(defn listmap
  [items]
  ())

(defn horizontal-selector
  [{:keys [columns default]}]
  (r/with-let [selected (r/atom (->> 0 (str) (keyword) (or default)))]
    (with-keys @screen {["l" "right"]  #(swap! selected next-option options)
                        ["h" "left"]   #(swap! selected prev-option options)})
    [:box {:top 1}
      (for [column columns]
        [:box {:left 0 :width 10} [:text "haha"]])]))


(defn help
  "Display a help box on the corner of the screen with contextual usage information

  Takes a hash map of props:
  :items [str] - A list of current keybindings, one per line

  Returns a reagent hiccup view element."
  [{:keys [items]}]
  [:box { :top 0
          :style {:border {:fg :magenta}}
          :border {:type :line}
          :label " ?? "
          :right 0
          :width "25%"
          :height "50%"}
    ; for [[idx [value label]] (map-indexed vector options)]
    (for [[idx item] (map-indexed vector items)]
      [:text {:top idx :left 1} item])])
