(ns renderer.attribute.hierarchy)

(defmulti update-attr (fn [_ k & _more] k))
(defmulti description (fn [tag k] [tag k]))
(defmulti form-element (fn [tag k _v _attrs] [tag k]))

(defmethod update-attr :default
  ([el k f]
   (update-in el [:attrs k] f))
  ([el k f arg]
   (update-in el [:attrs k] f arg))
  ([el k f arg & more]
   (apply update-in el [:attrs k] f arg more)))
