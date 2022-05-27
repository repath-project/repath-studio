(dotimes [x 25] (circle {:cx (+ (* x 30) 40)
                         :cy (+ (* (js/Math.sin x) 10) 200)
                         :r 10
                         :fill (str "hsl(" (* x 10) " ,50% , 50%)")}))


(ajax/GET "https://api.thecatapi.com/v1/images/search" {:response-format (ajax/json-response-format {:keywords? true})
                                                        :handler (fn [response]
                                                                   (let [{:keys [width height url]} (first response)]
                                                                     (image {:x 0 :y 0 :width width :height height :href url})))})
