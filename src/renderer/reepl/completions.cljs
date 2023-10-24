(ns renderer.reepl.completions
  (:require [cljs.reader]
            [cljs.tools.reader]
            [reagent.core :as r]
            [reagent.dom :as dom]
            [renderer.reepl.helpers :as helpers]))

(def styles
  {:completion-list {:flex-direction :row
                     :position :absolute
                     :bottom "100%"
                     :left 0
                     :right 0
                     :overflow :hidden
                     :font-size 12}

   :completion-show-all {:top 0
                         :left 0
                         :right 0
                         :z-index 1000
                         :flex-direction :row
                         :flex-wrap :wrap}
   :completion-item {;; :cursor :pointer TODO make these clickable
                     :padding "3px 5px"
                     :background-color "var(--level-1)"}
   :completion-selected {:background-color "var(--level-2)"}
   :completion-active {:background-color "var(--level-3)"}})

(def view (partial helpers/view styles))

(def canScrollIfNeeded
  (not (nil? (.-scrollIntoViewIfNeeded js/document.body))))

(defn completion-item [_text _is-selected _is-active _set-active]
  (r/create-class
   {:component-did-update
    (fn [this [_ _ old-is-selected]]
      (let [[_ _ is-selected] (r/argv this)]
        (when (and (not old-is-selected)
                 is-selected)
          (when canScrollIfNeeded
            (.scrollIntoViewIfNeeded (dom/dom-node this) false)
            (.scrollIntoView (dom/dom-node this))))))
    :reagent-render
    (fn [text is-selected is-active set-active]
      [view {:on-click set-active
             :style [:completion-item
                     (and is-selected
                          (if is-active
                            :completion-active
                            :completion-selected))]}
       text])}))

(defn completion-list [{:keys [pos list active show-all]} set-active]
  (let [items (->> list
                   (map-indexed
                    #(-> [completion-item
                          (get %2 2)
                          (= %1 pos)
                          active
                          (partial set-active %1)])))]
    (when show-all
      (into [view :completion-show-all] items))
    (into
     [view :completion-list]
     items)))
