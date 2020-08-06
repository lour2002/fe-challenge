(ns kasta.fe.main
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [rum.core :as rum]
            [cljs-http.client :as http]
            [cljs.core.async :refer [<!]]))

(enable-console-print!)

(go 
  (let [response (<! (http/get "/api/campaigns" {:with-credentials? false}))]
      (def tagsList (:menu (:body response)))
  )
)

(rum/defc tagItem
  [data]
  [:div {:key (get data :url)}(get data :name)]
  )

(rum/defc Root []
  [:div {:class "container"} 
    (mapv tagItem tagsList) 
  ]
)


(defn ^:export trigger-render []
  (rum/mount (Root) (js/document.getElementById "content")))


