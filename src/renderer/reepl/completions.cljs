(ns renderer.reepl.completions
  (:require
   ["react" :as react]
   [cljs.reader]
   [cljs.tools.reader]
   [reagent.core :as ra]
   [renderer.utils.dom :as dom]))

(defn completion-item
  [_text _selected? _active? _set-active]
  (let [ref (react/createRef)]
    (ra/create-class
     {:component-did-update
      (fn [this [_ _ old-selected?]]
        (let [[_ _ selected?] (ra/argv this)]
          (when (and (not old-selected?)
                     selected?)
            (dom/scroll-into-view (.-current ref)))))
      :reagent-render
      (fn [text selected? active? set-active]
        [:div.p-1.level-0.text-nowrap
         {:on-click set-active
          :class (and selected? (if active? "bg-accent" "level-1"))
          :ref ref}
         text])})))

(defn completion-list
  [docs {:keys [pos list active? show-all?]} set-active]
  (let [items (map-indexed
               #(-> [completion-item
                     (get %2 2)
                     (= %1 pos)
                     active?
                     (partial set-active %1)]) list)]
    [:div.absolute.bottom-full.left-0.w-full.text-xs
     (when docs [:div.level-1.drop-shadow.p-4.absolute.bottom-full docs])
     (into
      [:div.overflow-hidden.flex
       {:class (when show-all? "flex-wrap")}]
      items)]))
