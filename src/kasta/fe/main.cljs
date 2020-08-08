(ns kasta.fe.main
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [rum.core :as rum]
            [cljs-http.client :as http]
            [cljs.core.async :refer [<!]]))

(enable-console-print!)

(def img_url "https://kasta.ua/imgw/loc/640x352/")




(go 
  (let [response (<! (http/get "/api/campaigns" {:with-credentials? false}))]
      (let [ { menu :menu, items :items } (:body response)]
        (def tag_list menu)
        (def campaigns items)
      )
  )
)

(rum/defc tagItem
  [data]
  [:div {:key (get data :url)} 
    [:span (get data :name)]
  ]
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








