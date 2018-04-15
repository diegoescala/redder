(ns redder.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [reagent.core :as reagent]
            [cljsjs.react]
            [cljs-http.client :as http]
            [cognitect.transit :as transit]
            [cljs.core.async :refer [<!]]
            [alandipert.storage-atom :refer [local-storage]]))

(def app-state (reagent/atom {}))

(defn parse-json [s]
  (let [r (transit/reader :json)]
    (transit/read r s)))

(defn retrieve-entries! [subreddit]
  (go
    (js/console.log (str "Retrieving entries for " subreddit))
    (let [response (<! (http/get (str "https://www.reddit.com/r/" subreddit ".json") {:with-credentials? false}))]
      (let [response-body (get response :body)]
        (swap! app-state assoc :subreddit-data response-body :subreddit-name subreddit )))))

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
        [:div {:class "choose"} [:span {:class "subredditprompt"} "Choose a subreddit:" ]
        [:input {:type "text" :name "subreddit" :on-change #(reset! subreddit-name (-> % .-target .-value))}]
        [:button {:on-click #(retrieve-entries! @subreddit-name )} "Go!"]])))
        
(defn post-entry [post]
  (fn [post]
    (let [post-data (get post :data)]
      (let [title (get post-data :title)]
        (let [id (get post-data :id)]
          [:div {:class "post" 
                 :on-click #(retrieve-comments! (:subreddit-name @app-state ) id)}
            title ])))))
  
(defn get-posts [state]
  (if (contains? state :subreddit-data)
    (let [data (:subreddit-data state)]
      (let [listing (:data data)]
        (let [posts (get listing :children)]
          posts)))
  []))
  
(defn get-comments [comment-soup]
  (let [top-level-listings comment-soup]
    (let [top-level-comments (reduce (fn [comments listings] (conj comments (get-in listings [:data :children]))) [] comment-soup)]
    (map #(:kind (first %)) top-level-comments))))
  ;(get (get comment-soup :data) :children))
  
(defn posts-panel []
  (fn []
    [:div {:class "posts"} (str "Entries in " (get @app-state :subreddit-name))
        (for [post (get-posts @app-state )]
         ^{key post} [post-entry post])]))
         
(defn comment-entry [comment]
  (fn [comment]
    [:div {:class "comment"}
      (str comment)]))

(defn test-comments []
  (let [comments {:kind "Listing" :data { :thing1 "Hi" :thing2 "Ho" :children [{:kind "t3"} {:kind "t1" :body "Comment #1"} {:kind "t1" :body "Comment #2"}]}}]
    (let [result (get-comments comments)]
      result)))
(defn comments-panel []
  (fn []
    [:div {:class "comments"}
      (let [comments (get-comments (get @app-state :comments))]
        (str comments))]))
;        (for [comment comments]
;          ^{key comment}[comment-entry comment]))]))

(defn main-page
  []
  [:div 
    (str (test-comments))
    [choose-subreddit-panel]
    [posts-panel]
    [comments-panel]])

(defn mount-root
  []
  (reagent/render [main-page] (.getElementById js/document "app")))

(defn init!
  []
  (mount-root))

