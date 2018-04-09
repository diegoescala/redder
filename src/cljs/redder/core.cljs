(ns redder.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [reagent.core :as reagent]
            [cljsjs.react]
            [cljs-http.client :as http]
            [cognitect.transit :as transit]
            [cljs.core.async :refer [<!]]
            [alandipert.storage-atom :refer [local-storage]]))

(def app-state
  (reagent/atom {}))

(defn parse-json [s]
  (let [r (transit/reader :json)]
    (transit/read r s)))

(defn retrieve-entries! [subreddit]
  (go
    (js/console.log "clicked.")
    (let [response (<! (http/get (str "https://www.reddit.com/r/" subreddit ".json") {:with-credentials? false}))]
      (let [response-body (get response :body)]
        (swap! app-state assoc :subreddit-name subreddit :subreddit-data response-body)))))

(defn retrieve-comments! [subreddit post-id]
  (go
    (let [url (str "https://www.reddit.com/r/" subreddit "/comments/" post-id ".json")]
      (js/console.log (str "Getting comments from " url))
      (let [response (<! (http/get url {:with-credentials? false}))]
        (let [response-body (get response :body)]
          (swap! app-state assoc :comments response-body))))))

(defn choose-subreddit-panel []
  (fn []
    (let [subreddit-name (atom nil)]
      [:div {:class "choose"}
        [:input {:type "text" :name "subreddit" :on-change #(reset! subreddit-name (-> % .-target .-value))}]
        [:button {:on-click #(retrieve-entries! @subreddit-name )} "Go!"]])))
        
(defn post-entry [post]
  (fn []
    (let [title (str (get (get post :data) :title))]
      (let [id (str (get (get post :data) :id))]
        [:div {:class "post" :on-click #(retrieve-comments! (:subreddit-name @app-state ) id)} title]))))
  
(defn get-posts [state]
  (if (contains? state :subreddit-data)
    (let [data (:subreddit-data state)]
      (let [listing (:data data)]
        (let [posts (get listing :children)]
          posts)))
  []))
  
(defn get-comments [comment-soup]
    comment-soup)
  ;(get (get comment-soup :data) :children))
  
(defn posts-panel []
  (fn []
    (let [posts (get-posts @app-state )]
      [:div {:class "posts"}
        (for [post posts]
         ^{key post} [post-entry post])])))
         
(defn comment-entry [comment]
  (fn []
    [:div {:class "comment"}
      (str comment)]))
         
(defn comments-panel []
  (fn []
    [:div {:class "comments"}
      (let [comments (get-comments (get @app-state :comments))]
        (for [comment comments]
          ^{key comment}[comment-entry comment]))]))

(defn main-page
  []
  [:div 
    [choose-subreddit-panel]
    [posts-panel]
    [comments-panel]])

(defn mount-root
  []
  (reagent/render [main-page] (.getElementById js/document "app")))

(defn init!
  []
  (mount-root))
