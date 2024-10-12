(ns renderer.app.handlers
  (:require
   [malli.experimental :as mx]
   [renderer.app.db :refer [State]]
   [renderer.app.effects :as-alias fx]
   [renderer.app.events :as-alias e]
   [renderer.frame.handlers :as frame.h]
   [renderer.tool.db :refer [Tool]]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.utils.keyboard :refer [KeyboardEvent]]
   [renderer.utils.pointer :as pointer :refer [PointerEvent]]
   [renderer.utils.wheel :refer [WheelEvent]]))

(mx/defn set-state
  [db, state :- State]
  (assoc db :state state))

(mx/defn set-cursor
  [db, cursor :- string?]
  (assoc db :cursor cursor))

(defn explain
  [db & more]
  (assoc db :explanation (apply str more)))

(defn add-fx
  [db effect]
  (update db :fx conj effect))

(mx/defn set-tool
  [db, tool :- Tool]
  (-> db
      (tool.hierarchy/deactivate)
      (assoc :tool tool)
      (tool.hierarchy/activate)))

(mx/defn pointer-handler
  [db, e :- PointerEvent, now :- number?]
  (let [{:keys [pointer-offset tool dom-rect drag primary-tool drag-threshold]} db
        {:keys [button buttons pointer-pos]} e
        adjusted-pointer-pos (frame.h/adjust-pointer-pos db pointer-pos)]
    (case (:type e)
      "pointermove"
      (-> (if pointer-offset
            (if (pointer/significant-drag? pointer-pos pointer-offset drag-threshold)
              (cond-> db
                (not= tool :pan)
                (frame.h/pan-out-of-canvas dom-rect pointer-pos pointer-offset)

                (not drag)
                (-> (tool.hierarchy/drag-start e)
                    (add-fx [::fx/set-pointer-capture (:pointer-id e)])
                    (assoc :drag true))

                :always
                (tool.hierarchy/drag e))
              db)
            (tool.hierarchy/pointer-move db e))
          (assoc :pointer-pos pointer-pos
                 :adjusted-pointer-pos adjusted-pointer-pos))

      "pointerdown"
      (cond-> db
        (= button :middle)
        (-> (assoc :primary-tool tool)
            (set-tool :pan))

        (not= buttons :right)
        (assoc :pointer-offset pointer-pos
               :adjusted-pointer-offset adjusted-pointer-pos)

        :always
        (tool.hierarchy/pointer-down e))

      "pointerup"
      (cond-> (if drag
                (-> (tool.hierarchy/drag-end db e)
                    (add-fx [::fx/release-pointer-capture (:pointer-id e)]))
                (if (= button :right)
                  db
                  (if (> (- now (:event-time db)) (:double-click-delta db))
                    (-> db
                        (assoc :event-time now)
                        (tool.hierarchy/pointer-up e))
                    (tool.hierarchy/double-click db e))))
        (and primary-tool (= button :middle))
        (-> (set-tool primary-tool)
            (dissoc :primary-tool))

        :always
        (-> (dissoc :pointer-offset :drag)
            (update :snap dissoc :nearest-neighbor)))

      db)))

(mx/defn wheel-handler
  [db, {:keys [modifiers delta-x delta-y]} :- WheelEvent]
  (if (some modifiers [:ctrl :alt])
    (let [factor (Math/pow (inc (/ (- 1 (:zoom-sensitivity db)) 100))
                           (- delta-y))]
      (-> db
          (frame.h/zoom-at-pointer factor)
          (add-fx [:dispatch [::e/persist]])))
    (frame.h/pan-by db [delta-x delta-y])))

(mx/defn key-handler
  [{:keys [tool] :as db}, e :- KeyboardEvent]
  (case (:type e)
    "keydown"
    (cond-> db
      (and (= (:code e) "Space")
           (not= tool :pan))
      (-> (assoc :primary-tool tool)
          (set-tool :pan))

      :always
      (tool.hierarchy/key-down e))

    "keyup"
    (cond-> db
      (and (= (:code e) "Space")
           (:primary-tool db))
      (-> (set-tool (:primary-tool db))
          (dissoc :primary-tool))

      :always
      (tool.hierarchy/key-up e))

    db))
