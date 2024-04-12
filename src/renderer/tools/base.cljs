(ns renderer.tools.base)

(derive ::transform ::tool)
(derive ::draw ::tool)
(derive ::misc ::tool)

(derive ::element ::tool)

(derive ::renderable ::element)
(derive ::never-renderable ::element)

(derive ::graphics ::renderable)

(derive ::gradient ::element)
(derive ::descriptive ::element)
(derive ::custom ::element)

(derive :foreignObject ::graphics)
(derive :textPath ::graphics)
(derive :tspan ::graphics)

(derive :linearGradient ::gradient)
(derive :radialGradient ::gradient)

(derive :desc ::descriptive)
(derive :metadata ::descriptive)
(derive :title ::descriptive)

(defmulti attrs keyword)
(defmulti properties keyword)

(defmulti render :tag)
(defmulti render-to-string :tag)
(defmulti path :tag)
(defmulti area :tag)
(defmulti centroid :tag)
(defmulti snapping-points :tag)

(defmulti render-edit #(:tag %))
(defmulti bounds #(:tag %))
(defmulti translate #(:tag %))
(defmulti position #(:tag %))
(defmulti scale #(:tag %))
(defmulti edit #(:tag %))

(defmulti pointer-down #(:tool %))
(defmulti pointer-move #(:tool %))
(defmulti pointer-up #(:tool %))
(defmulti double-click #(:tool %))
(defmulti drag #(:tool %))
(defmulti drag-start #(:tool %))
(defmulti drag-end #(:tool %))

(defmulti key-up #(:tool %))
(defmulti key-down #(:tool %))

(defmulti activate :tool)
(defmulti deactivate :tool)

(defn set-tool
  [db tool]
  (-> db
      (deactivate)
      (assoc :tool tool)
      (activate)))

(defmethod pointer-down :default [db] db)
(defmethod pointer-up :default [db] db)
(defmethod pointer-move :default [db] db)
(defmethod drag-start :default [db] db)
(defmethod double-click :default [db] db)

(defmethod key-up :default [db] db)
(defmethod key-down :default [db] db)

(defmethod drag :default [db event element] (pointer-move db event element))
(defmethod drag-end :default [db event element] (pointer-up db event element))
(defmethod properties :default [])
(defmethod render :default [])
(defmethod render-to-string :default [element] [render element])

(defmethod render-edit :default [])
(defmethod bounds :default [])
(defmethod area :default [])
(defmethod centroid :default [])
(defmethod snapping-points :default [])

(defmethod activate :default [db] (assoc db :cursor "default"))
(defmethod deactivate :default [db] (assoc db :cursor "default"))

(defmethod attrs :default [])
(defmethod path :default [element] element)
(defmethod scale :default [element] element)
(defmethod translate :default [element] element)
(defmethod position :default [element] element)
