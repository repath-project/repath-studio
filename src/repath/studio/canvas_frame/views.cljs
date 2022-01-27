(ns repath.studio.canvas-frame.views
  (:require
   [reagent.core :as ra]
   [reagent.dom :as dom]
   [reagent.dom.server :as server]
   [re-frame.core :as rf]
   [repath.studio.elements.views :refer [element-menu]]
   [repath.studio.tools.base :as tools]
   [repath.studio.mouse :as mouse]
   ["react-frame-component" :default Frame]))

(defn frame-markup
  "SEE https://github.com/ryanseddon/react-frame-component#initialcontent"
  []
  [:html [:head] [:body {:style {:width "100%"
                                 :height "100%"
                                 :overflow "hidden"
                                 :margin 0}}]])

(defn frame
  "Our canvas is wrapped within an iframe element that hosts anything that needs to be rendered.
   SEE https://github.com/ryanseddon/react-frame-component
   SEE https://medium.com/@ryanseddon/rendering-to-iframes-in-react-d1cb92274f86"
  []
  (let [resize-observer (new js/ResizeObserver (fn [entries] (rf/dispatch [:canvas/resize (js->clj (.toJSON (.-contentRect (.find entries (fn [] true)))) :keywordize-keys true)])))]
    (ra/create-class
     {:component-did-mount
      (fn
        [this]
        ;; We observe the frame to get its contentRect on resize.
        (.observe resize-observer (dom/dom-node this))
        (.setTimeout js/window #(rf/dispatch [:pan-to-active-page :original])))

      :component-will-unmount
      (fn
        []
        (.disconnect resize-observer))

      :reagent-render
      (fn
        []
        (let [;; This is a different browsing context inside an iframe.
              ;; We need to simulate the events to the parent window.
              keyboard-event     #(.dispatchEvent js/window.parent.document (new js/KeyboardEvent (.-type %) %))]
          [:> Frame {:initialContent (server/render-to-static-markup [frame-markup])
                     :mountTarget    "body"
                     :id             "canvas-frame"
                     :on-mouse-move   #(mouse/event-handler % nil)
                     :on-key-down     keyboard-event
                     :on-key-up       keyboard-event 
                     :on-mouse-enter #(rf/dispatch [:set-mouse-over-canvas? true])
                     :on-mouse-leave #(rf/dispatch [:set-mouse-over-canvas? false])
                     :on-context-menu element-menu
                     :style          {:flex "1 1"
                                      :user-select "none"
                                      :overflow "hidden"
                                      :border 0}}
           [tools/render @(rf/subscribe [:elements/canvas])]]))})))
