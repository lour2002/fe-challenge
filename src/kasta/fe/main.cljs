(ns kasta.fe.main
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [rum.core :as rum]
            [clojure.string :as string]
            [cljs-http.client :as http]
            [cljs-time.core :as time]
            [cljs-time.format :as format]
            [cljs.core.async :refer [<!]]))

(enable-console-print!)

(def img-url "https://kasta.ua/imgw/loc/640x352/")
(def tag-list (atom []))
(def campaigns (atom []))
(def filter-tags (atom ""))

;; Check if tags-list contains values from another list
(defn has-tags [tags-list key]
  (fn [item] 
    (some (fn [tag] (contains? tags-list tag)) (apply vector (get item key)))))

;; Check if the current time falls within the interval
(defn is-actual [campaign]
  (defn- get-current-date []
    (let [current-date-time (time/now)]
      (format/unparse
        (format/formatters :date-time-no-ms)
        current-date-time
      )))

  (def current-time (get-current-date))

  (and
    (> (compare current-time (campaign :starts_at)) 0)
    (< (compare current-time (campaign :finishes_at)) 0)))

;; Get campaigns, prepare data
(go 
  (let [response (<! (http/get "/api/campaigns" {:with-credentials? false}))]
    (let [ 
      { menu :menu, items :items } (:body response)
      filterd-items (filter is-actual items)
      active-tags (set (reduce (fn [acc camp] (concat acc (apply vector (camp :tags)))) [] filterd-items))]
        
      (def campaigns-all filterd-items)
          
      (reset! campaigns filterd-items)
      (reset! tag-list (filter (has-tags active-tags :tag) menu)))))


;; Adds a watch function to filter-tags
(add-watch filter-tags :logger 
  (fn [& params]
    (let [ tags-list (nth params 3) ]
      (reset! campaigns (filter (has-tags (set tags-list) :tags) campaigns-all)))))

;; Components
(rum/defc TagItem  < rum/reactive [data]
  (let [ { tag :tag, name :name } data]
    [:div { :key (get data :url) 
            :class ["tag-list__item" (if (= (rum/react filter-tags) tag) "--active")] 
            :on-click #(reset! filter-tags tag)} 

      name]))

(rum/defc TagList < rum/reactive []
  [:div {:class "tag-list"}
    (mapv TagItem (rum/react tag-list))])


(rum/defc СampaignItem [data]
  [:div { :key (get data :id)
          :class "campaigns-list__item"}
          
    [:img { :src (str img-url (get data :now_image))}]])

(rum/defc CampaignsList < rum/reactive []
  [:div {:class "campaigns-list"} 
    (mapv СampaignItem (rum/react campaigns))])

;; Root component
(rum/defc Root []
  [:div {:class "container"} 
    (TagList)
    (CampaignsList)])

(defn ^:export trigger-render []
  (rum/mount (Root) (js/document.getElementById "content")))
