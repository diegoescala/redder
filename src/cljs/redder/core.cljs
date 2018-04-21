(ns redder.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [reagent.core :as reagent]
            [cljsjs.react]
            [cljs-http.client :as http]
            [cognitect.transit :as transit]
            [cljs.core.async :refer [<!]]
            [alandipert.storage-atom :refer [local-storage]]
            [re-frame.core :as rf]))

(def app-state (reagent/atom {}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Register re-frame events
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
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

(rf/dispatch-sync [:init])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; End re-frame event registration
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn parse-json [s]
  (let [r (transit/reader :json)]
    (transit/read r s)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; AJAX
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;    
    
(defn fetch-posts!
  [subreddit]
  (go
    (js/console.log (str "Retrieving entries for " subreddit))
    (let [response (<! (http/get (str "https://www.reddit.com/r/" subreddit ".json") {:with-credentials? false}))]
      (let [response-body (get response :body)]
        (rf/dispatch [:set-post-list response-body])))))

(defn fetch-comments! 
  [subreddit post-id]
  (go
    (let [url (str "https://www.reddit.com/r/" subreddit "/comments/" post-id ".json")]
      (js/console.log (str "Getting comments from " url))
      (let [response (<! (http/get url {:with-credentials? false}))]
        (let [response-body (get response :body)]
          (rf/dispatch [:set-comments response-body]))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; End AJAX
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
          
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; UI components
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn choose-subreddit-panel []
  (fn []
    (let [subreddit-name (reagent/atom "NoSub")]
      [:div {:class "choose"} [:span {:class "subredditprompt"} "Choose a sr" ]
        [:input {:type "text" :name "subreddit" :on-change #(reset! subreddit-name (-> % .-target .-value))}]
        [:button {:on-click 
          #(do
            (rf/dispatch [:open-subreddit @subreddit-name])
            (fetch-posts! @subreddit-name)
            )} "Go!"]])))
        
(defn post-entry [post]
  (fn [post]
    (let [post-data (get post :data)]
      (let [title (get post-data :title)]
        (let [id (get post-data :id)]
          [:div {:class "post"
                 ;:on-click #(retrieve-comments! (:subreddit-name @app-state ) id)}
                 :on-click #(fetch-comments! @(rf/subscribe [:subreddit-name]) id)}
            title ])))))
  
(defn get-posts [data]
      (let [listing (:data data)]
        (let [posts (get listing :children)]
          posts)))
  
(defn get-comments [comment-soup]
  (let [top-level-listings comment-soup]
    (let [top-level-data 
            (reduce 
              (fn [comments listings] 
                (conj comments (get-in listings [:data :children]))) [] comment-soup)]
      (let [comment-data (filter #(= (:kind %) "t1") (apply concat top-level-data))]
        (map #(select-keys (:data %) [:author :body]) comment-data )))))
  
(defn posts-panel []
  (fn []
    (let [sr-name (rf/subscribe [:subreddit-name])]
      (let [posts (rf/subscribe [:subreddit-posts])]
        [:div {:class "posts"} (str "Entries in " @sr-name)
          (for [post (get-posts @posts )]
            ^{key post} [post-entry post])]))))
         
(defn comment-entry [comment]
  (fn [comment]
    [:div {:class "comment"}
      [:div {:class "comment-author"} (get comment :author)]
      [:div {:class "comment-body"} (get comment :body)]]))

(defn comments-view
  []
  (let [comments (rf/subscribe [:post-comments])]
    [:div {:class "comments"}
      (for [comment (get-comments @comments)]
        ^{key comment} [comment-entry comment])]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; End UI components
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn main-page
  []
  [:div
    [choose-subreddit-panel]
    [posts-panel]
    [comments-view]
    ])

(defn mount-root
  []
  (reagent/render [main-page] (.getElementById js/document "app")))

(defn init!
  []
  (mount-root))

