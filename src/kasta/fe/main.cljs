(ns kasta.fe.main
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [rum.core :as rum]
            [aleph.http :as http]
            [cljs.core.async :refer [<!]]))

(enable-console-print!)

(go 
  (let [response (http/get "https://kasta.ua/api/v2/campaigns"))]
    (def campaignsData (:body response))
    (println campaignsData)
))

(rum/defc Root []
  [:div {:class "container"} response ])


(defn ^:export trigger-render []
  (rum/mount (Root) (js/document.getElementById "content")))


