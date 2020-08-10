(ns kasta.fe.main
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [rum.core :as rum]
            [clojure.string :as string]
            [cljs-http.client :as http]
            [cljs-time.core :as time]
            [cljs-time.format :as format]
            [cljs.core.async :refer [<!]]))

(enable-console-print!)

(def img_url "https://kasta.ua/imgw/loc/640x352/")
(def tag_list (atom []))
(def campaigns (atom []))
(def filter_tags (atom ""))

;; Check if tags_list contains values from another list
(defn has-tags [tags_list key]
  (fn [item] 
    (some (fn [tag] (contains? tags_list tag)) (apply vector (get item key))))
)
;; Check if the current time falls within the interval
(defn is-actual [campaign]
  (defn- get-current-date []
    (let [current_date_time (time/now)]
    (format/unparse
      (format/formatters :date-time-no-ms)
      current_date_time
    ))
  )

  (def current_time (get-current-date))

  (and
    (> (compare current_time (campaign :starts_at)) 0)
    (< (compare current_time (campaign :finishes_at)) 0))
)

;; Get campaigns, prepare data
(go 
  (let [response (<! (http/get "/api/campaigns" {:with-credentials? false}))]
      (let [ 
        { menu :menu, items :items } (:body response)
        filterd_items (filter is-actual items)
        active_tags (set (reduce (fn [acc camp] (concat acc (apply vector (camp :tags)))) [] filterd_items))
      ]
        (def campaigns_all filterd_items)
          
        (reset! campaigns filterd_items)
        (reset! tag_list (filter (has-tags active_tags :tag) menu))
      )
  )
)
;; Adds a watch function to filter_tags
(add-watch filter_tags :logger 
  (fn [& params]
    (let [ tags_list (nth params 3) ]
      (reset! campaigns (filter (has-tags (set tags_list) :tags) campaigns_all))
    )
  )
)

;; Components
(rum/defc tagItem  < rum/reactive [data]
  (let [ { tag :tag, name :name } data]
    [:div {:key (get data :url) :class ["tag-list__item" (if (= (rum/react filter_tags) tag) "--active")] :on-click #(reset! filter_tags tag)} name]
  )
)

(rum/defc TagList < rum/reactive []
  [
    [:div {:class "tag-list"}
      (mapv tagItem (rum/react tag_list))
    ]
  ]
)


(rum/defc campaignItem [data]
  [:div {:class "campaigns-list__item" :key (get data :id)}
    [:img {:src (str img_url (get data :now_image))}]
  ]
)

(rum/defc CampaignsList < rum/reactive []
  [:div {:class "campaigns-list"} 
    (mapv campaignItem (rum/react campaigns)) 
  ]
)
;; Root component
(rum/defc Root []
  [:div {:class "container"} 
    (TagList)
    (CampaignsList)
  ]
)

(defn ^:export trigger-render []
  (rum/mount (Root) (js/document.getElementById "content"))
)
