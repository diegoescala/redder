(ns redder.parser)

(defn get-comments [comment-soup]
  (let [top-level-listings comment-soup]
    (let [top-level-data
            (reduce
              (fn [comments listings] 
                (conj comments (get-in listings [:data :children]))) [] comment-soup)]
      (let [comment-data (filter #(= (:kind %) "t1") (apply concat top-level-data))]
        (map #(select-keys (:data %) [:author :body]) comment-data)))))


(defn get-posts [data]
  (let [listing (:data data)]
    (let [posts (get listing :children)]
      posts)))
