(ns renderer.attribute.hierarchy)

(defmulti update-attr (fn [_ k & _more] k))
(defmulti description (fn [tag k] [tag k]))
(defmulti form-element (fn [tag k _v _attrs] [tag k]))

(defmethod update-attr :default
  [el k f & more]
  (apply update-in el [:attrs k] f more))
