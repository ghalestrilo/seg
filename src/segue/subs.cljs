(ns segue.subs
  "Re-frame app db subscriptions. Essentially maps a keyword describing a
  result to a function that retrieves the current value from the app db."
  (:require [re-frame.core :as rf]))

(rf/reg-sub
  :db
  (fn [db _]
    db))

(rf/reg-sub
  :view
  (fn [db _]
    (:router/view db)))

(rf/reg-sub
  :size
  (fn [db _]
    (:terminal/size db)))

(rf/reg-sub
  :help
  (fn [db _]
    (let [help-content (:dialog/help db)
          view @(rf/subscribe [:view])]
      (if view (view help-content) help-content)))) ; FIXME: Replace :home with view here

(rf/reg-sub
  :opts
  (fn [db _]
    (:opts db)))

(rf/reg-sub
  :track
  (fn [db _]
    (:track db)))

(rf/reg-sub
  :channels
  (fn [db _]
    (-> db :track :channels)))

(rf/reg-sub
  :sections
  (fn [db _]
    (-> db :track :sections)))

(rf/reg-sub
  :playback
  (fn [db _]
    (-> db :playback)))

(rf/reg-sub
  :repl
  (fn [db _]
    (-> db :repl (or {:process nil :messages []}))))

(rf/reg-sub
  :settings
  (fn [db _]
    (:settings db)))

(rf/reg-sub
  :editor
  (fn [db _]
    (:editor db)))

; NOTE: The two subs below and their calls must be updated to account for multi-select

(rf/reg-sub
  :selection
  (fn [db _]
    (:session/selection db)))

(rf/reg-sub
  :selection-content
  (fn [db _]
    (let [selection (-> db :session/selection)
          sections  (-> db :track :sections)]
      (nth sections selection))))