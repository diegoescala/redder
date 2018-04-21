(ns redder.events
  (:require [re-frame.core :as rf]
            [reagent.core :as reagent]))
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Register re-frame events
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn init-rf
    []
    (do 
        (rf/reg-event-db 
            :init
            (fn [_ _]
                (js/console.log "initializing...")
                {:subreddit-name "NoSub"}))
            
        (rf/reg-event-fx
            :set-post-list
            (fn [{:keys [db]} [_ details]]
                {:db (assoc db :subreddit-data details)}))
            
        (rf/reg-event-fx
            :set-comments
            (fn [{:keys [db]} [_ details]]
                {:db (assoc db :comments details)}))

        (rf/reg-event-db
            :open-subreddit
            (fn
                [{:keys [db]} [_ subreddit-name]]
                {:subreddit-name subreddit-name}))

        (rf/reg-event-fx
            :open-post
            (fn 
                [{:keys [db]} [_ post-id]]
                {:db (assoc db :view :post-detail :post-id post-id)}))

        (rf/reg-sub :subreddit-name  (fn [db _] (get db :subreddit-name )))
        (rf/reg-sub :subreddit-posts (fn [db _] (get db :subreddit-data )))
        (rf/reg-sub :post-comments   (fn [db _] (get db :comments )))

        (rf/dispatch-sync [:init])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; End re-frame event registration
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

