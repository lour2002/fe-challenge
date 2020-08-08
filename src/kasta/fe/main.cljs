(ns kasta.fe.main
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [rum.core :as rum]
            [cljs-http.client :as http]
            [cljs-time.core :as time]
            [cljs-time.format :as format]
            [cljs.core.async :refer [<!]]))

(enable-console-print!)

(def img_url "https://kasta.ua/imgw/loc/640x352/")
(def tagFilters (atom))




(go 
  (let [response (<! (http/get "/api/campaigns" {:with-credentials? false}))]
      (let [ { menu :menu, items :items } (:body response)]

        (defn isActual [campaign]
            (defn- getCurrentDate
              []
              (let [currentDateTime (time/now)]
                (format/unparse
                  (format/formatters :date-time-no-ms)
                  currentDateTime
                )
              )
            )

            (def currentTime (getCurrentDate))

            (and
              (> (compare currentTime (campaign :starts_at)) 0)
              (< (compare currentTime (campaign :finishes_at)) 0)
            )
        )

        (def tag_list menu)
        (def campaigns (filter isActual items))
      )
  )
)

(rum/defc tagItem
  [data]
  (let [ { tag :tag, name :name } data]
    [:div {:key (get data :url)} 
      [:button {:on-click #(reset! tagFilters tag)} name]
    ] 
  )
  
)

(rum/defc campaignItem
  [data]
  [:div {:key (get data :id)}
    [:img {:src (str img_url (get data :now_image))}]
  ]
)

(rum/defc Root []
  [:div {:class "container"} 
    (mapv tagItem tag_list) 
    (mapv campaignItem campaigns) 
  ]
)

(defn ^:export trigger-render []
  (rum/mount (Root) (js/document.getElementById "content"))
)
