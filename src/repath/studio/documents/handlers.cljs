(ns repath.studio.documents.handlers
  (:require
   [repath.studio.elements.handlers :as elements]
   [repath.studio.tools.base :as tools]))

(defn set-fill
  [{active-document :active-document :as db} fill]
  (-> db
      (assoc-in [:documents active-document :fill] fill)
      (elements/set-attribute :fill (tools/rgba fill))))