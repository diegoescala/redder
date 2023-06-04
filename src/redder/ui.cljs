(ns redder.ui
  (:require [redder.parser :as parser]
            [redder.api-client :as api-client]
            [re-frame.core :as rf]
            [reagent.core :as reagent]))

(defn post-entry [post]
  (fn [post]
    (let [post-data (get post :data)]
      (let [title (get post-data :title)]
        (let [id (get post-data :id)]
          [:div {:class "post"
                 :on-click #(api-client/fetch-comments! @(rf/subscribe [:subreddit-name]) id)}
            title ])))))
        
(defn posts-panel []
  (fn []
    (let [sr-name (rf/subscribe [:subreddit-name])]
      (let [posts (rf/subscribe [:subreddit-posts])]
        [:div {:class "posts"} (str "Entries in " @sr-name)
          (for [post (parser/get-posts @posts )]
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
      (for [comment (parser/get-comments @comments)]
        ^{key comment} [comment-entry comment])]))
  
(defn choose-subreddit-panel []
  (fn []
    (let [subreddit-name (reagent/atom "NoSub")]
      [:div {:class "choose"} [:span {:class "subredditprompt"} "Choose a subreddit"]
        [:input {:type "text" :on-change #(reset! subreddit-name (-> % .-target .-value))}]
        [:button 
         {:on-click 
          #(do
            (rf/dispatch [:open-subreddit @subreddit-name])
            (api-client/fetch-posts! @subreddit-name))}
         "Go!"]])))
        
