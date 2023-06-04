(ns redder.api-client
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [reagent.core :as reagent]
            [cljsjs.react]
            [cljs-http.client :as http]
            [cognitect.transit :as transit]
            [cljs.core.async :refer [<!]]
            [alandipert.storage-atom :refer [local-storage]]
            [re-frame.core :as rf]))

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
