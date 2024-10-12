(ns renderer.attribute.hierarchy)

(defmulti update-attr (fn [_ k & _more] k))
(defmulti description (fn [tag k] [tag k]))
(defmulti form-element (fn [tag k _v _attrs] [tag k]))

(defmethod update-attr :default
  ([el attr f arg1]
   (update-in el [:attrs attr] f arg1))
  ([el attr f arg1 arg2]
   (update-in el [:attrs attr] f arg1 arg2))
  ([el attr f arg1 arg2 arg3]
   (update-in el [:attrs attr] f arg1 arg2 arg3))
  ([el attr f arg1 arg2 arg3 & more]
   (apply update-in el [:attrs attr] f arg1 arg2 arg3 more)))
