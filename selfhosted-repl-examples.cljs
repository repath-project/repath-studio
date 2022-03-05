(dotimes [x 25] (create {:circle {:cx (+ (* x 30) 40)
                                  :cy (+ (* (js/Math.sin x) 10) 200)
                                  :r 10
                                  :fill (str "hsl(" (* x 10) " ,50% , 50%)")}}))