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

