(dotimes [x 10]
  (r/create {:type :rect
             :attrs {:width "10"
                     :height "100"
                     :x "200"
                     :y "200"
                     :fill (str "hsl(" (* x 5) " ,50% , 50%)")
                     :transform (str "rotate(" (* x 18) " 205 250)")}}))

(dotimes [x 25] (r/create {:type :circle
                           :attrs {:cx (+ (* x 30) 40)
                                   :cy (+ (* (js/Math.sin x) 10) 200)
                                   :r 10
                                   :fill (str "hsl(" (* x 10) " ,50% , 50%)")}}))

(dotimes [x 200] (r/create {:type :circle
                            :attrs {:cx (rand 800)
                                    :cy (rand 600)
                                    :r (rand 2)
                                    :fill "white"
                                    :opacity (rand 1)}}))

(r/create (reduce (fn [list x] (conj list {:type :rect
                                           :attrs {:width "10"
                                                   :height "100"
                                                   :x "200"
                                                   :y "200"
                                                   :fill (str "hsl(" (* x 5) " ,50% , 50%)")
                                                   :transform (str "rotate(" (* x 18) " 205 250)")}})) [] (range 10)))
