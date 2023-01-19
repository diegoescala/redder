(ns redder.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [reagent.core :as reagent]
            [reagent.dom :as rdom]
            [cljsjs.react]
            [cljs-http.client :as http]
            [cognitect.transit :as transit]
            [cljs.core.async :refer [<!]]
            [alandipert.storage-atom :refer [local-storage]]
            [re-frame.core :as rf]
            [redder.events :as events]
            [redder.ui :as ui]))

(def app-state (reagent/atom {}))

(defn parse-json [s]
  (let [r (transit/reader :json)]
    (transit/read r s)))

(defn main-page
  []
  [:div
    [ui/choose-subreddit-panel]
    [ui/posts-panel]
    [ui/comments-view]])


(defn mount-root
  []
  (rdom/render [main-page] (.getElementById js/document "app")))

(defn init!
  []
  (println "halo")
  (events/init-rf)
  (mount-root))

(init!)
