(ns renderer.utils.path
  (:require ["paper" :refer [Path]]))

(defn manipulate-paper-path
  [path action options]
  (case action
    :simplify (.simplify path options)
    :smooth (.smooth path options)
    :flatten (.flatten path options)
    :reverse (.reverse path options)
    nil)
  path)

(defn manipulate
  [el action & more]
  (update-in el [:attrs :d] #(-> (Path. %)
                                 (manipulate-paper-path action more)
                                 (.exportSVG)
                                 (.getAttribute "d"))))
