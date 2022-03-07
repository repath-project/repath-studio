(ns repath.studio.documents.events
  (:require
   [re-frame.core :as rf]
   [repath.studio.helpers :as helpers]
   [repath.studio.documents.db :as db]
   [repath.studio.history.handlers :as history]
   [repath.studio.canvas-frame.handlers :as canvas-frame]
   [repath.studio.tools.base :as tools]
   [repath.studio.de-dupe.core :as dd]
   [repath.studio.documents.handlers :as handlers]))

(defn reg-set-active-document-event
  [k]
  (rf/reg-event-db
   (keyword (str "set-" (name k)))
   (fn [db [_ v]]
     (assoc-in db [:documents (:active-document db) k] v))))

(doseq [x [:hovered-keys
           :selected-keys
           :active-page
           :stroke
           :filter
           :stroke-width
           :temp-element
           :rotate]] (reg-set-active-document-event x))

(rf/reg-event-db
 :swap-colors
 (fn [{active-document :active-document :as db} [_]]
   (let [fill (get-in db [:documents active-document :fill])
         stroke (get-in db [:documents active-document :stroke])]
     (-> db
         (assoc-in [:documents active-document :fill] stroke)
         (assoc-in [:documents active-document :stroke] fill)))))

(defn reg-toggle-active-document-event
  [k]
  (rf/reg-event-db
   (keyword (str "toggle-" (name k)))
   (fn [db [_]]
     (update-in db [:documents (:active-document db) (keyword (str (name k) "?"))] not))))

(doseq [x [:rulers-locked
           :grid
           :rulers]] (reg-toggle-active-document-event x))

(rf/reg-event-db
 :documents/set-fill
 (fn [db   [_ fill]]
   (-> db
       (handlers/set-fill fill)
       (history/finalize (str "Set fill " (tools/rgba fill))))))

(rf/reg-event-fx
 :documents/open
 (fn [_ [_]]
   (.then (js/window.api.send "toMain" #js {:action "openDocument"}))))

(rf/reg-event-db
 :documents/save
 (fn [{active-document :active-document :as db} [_]]
   (let [document (get-in db [:documents active-document])
         duped (assoc document :history (dd/de-dupe (:history document)))]
     (.then (js/window.api.send "toMain" #js {:action "saveDocument" :data (pr-str duped)})))))

(rf/reg-event-db
 :documents/new
 (fn [db [_]]
   (let [key (helpers/uid)
         title (str "Untitled-" (inc (count (:documents db))))]
     (-> db
         (assoc-in [:documents key] (merge db/default-document {:key key :title title}))
         (update :document-tabs conj key)
         (assoc :active-document key)
         (canvas-frame/pan-to-element (:active-page db/default-document))
         (history/init "Create document")))))

(rf/reg-event-db
 :documents/close
 (fn [{active-document :active-document :as db} [_ key]]
   (let [document-tabs (:document-tabs db)
         index (.indexOf document-tabs key)
         active-document (if (= active-document key) (get document-tabs (if (= index 0) (inc index) (dec index))) active-document)]
    (-> db
        (update :document-tabs #(filterv (complement #{key}) %))
        (assoc :active-document active-document)
        (update :documents dissoc key)))))

(rf/reg-event-db
 :documents/close-others
 (fn [db [_ key]]
   (-> db
       (assoc :document-tabs [key])
       (assoc :active-document key)
       (assoc-in [:documents key] (get-in db [:documents key])))))

(rf/reg-event-db
 :documents/close-all
 (fn [db [_]]
     (-> db
         (assoc :document-tabs [])
         (dissoc :active-document)
         (dissoc :documents))))

(rf/reg-event-db
 :documents/scroll
 (fn [db [_ direction]]
   (let [document-tabs (:document-tabs db)
         index (.indexOf document-tabs (:active-document db))
         scrolled-index (if (pos? direction) (inc index) (dec index))]
     (if (and (nat-int? scrolled-index) (< scrolled-index (count document-tabs))) (assoc db :active-document (get document-tabs scrolled-index)) db))))

(rf/reg-event-db
 :documents/swap-position
 (fn [db [_ dragged-key swapped-key]]
   (let [document-tabs (:document-tabs db)
         dragged-index (.indexOf document-tabs dragged-key)
         swapped-index (.indexOf document-tabs swapped-key)]
     (assoc db :document-tabs (helpers/vec-swap document-tabs dragged-index swapped-index)))))
