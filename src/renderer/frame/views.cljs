(ns renderer.frame.views
  (:require
   [reagent.core :as ra]
   [reagent.dom :as dom]
   [reagent.dom.server :as server]
   [re-frame.core :as rf]
   [renderer.tools.base :as tools]
   [renderer.utils.mouse :as mouse]
   [renderer.components :as comp]
   ["react-frame-component" :default Frame :refer [useFrame]]
   ["@radix-ui/react-context-menu" :as ContextMenu]))

(defn mouse-handler
  [event]
  (mouse/event-handler event nil))

(defn inner-component
  "We need access to the iframe's window to add the mouse move listener.
   This is required in order to track mouse movement outside of our canvas.
   SEE https://github.com/ryanseddon/react-frame-component#accessing-the-iframes-window-and-document
   SEE https://github.com/reagent-project/reagent/blob/master/doc/ReactFeatures.md#function-components"
  []
  (let [frame-window (.-window (useFrame))]
    (ra/create-class
     {:component-did-mount
      #(doseq
        [event ["pointermove" "pointerup" "wheel"]]
         (.addEventListener frame-window event mouse-handler #js {:passive false}))

      :component-will-unmount
      #(doseq
        [event ["pointermove" "pointerup" "wheel"]]
         (.removeEventListener frame-window event mouse-handler))

      :reagent-render #()})))

(defn markup
  "SEE https://github.com/ryanseddon/react-frame-component#initialcontent"
  []
  [:html
   [:head]
   [:body {:style {:width "100%"
                   :height "100%"
                   :overflow "hidden"
                   :user-select "none"
                   :margin 0
                   :font-family "-apple-system, BlinkMacSystemFont, 
                                'Segoe UI (Custom)', 'Roboto', 
                                'Helvetica Neue', 'Open Sans (Custom)', 
                                system-ui, sans-serif, 'Apple Color Emoji', 
                                'Segoe UI Emoji'"}}]])

(def resize-observer
  (js/ResizeObserver.
   (fn [entries]
     (let [client-rect (.getBoundingClientRect (.-target (.find entries (fn [] true))))
           content-rect (js->clj (.toJSON (.-contentRect (.find entries (fn [] true))))
                                 :keywordize-keys true)]
       (rf/dispatch-sync [:frame/resize (assoc content-rect
                                               :x (.-x client-rect)
                                               :y (.-y client-rect))])))))

(defn main
  "Our canvas is wrapped within an iframe element that hosts anything 
   that needs to be rendered.
   SEE https://github.com/ryanseddon/react-frame-component
   SEE https://medium.com/@ryanseddon/rendering-to-iframes-in-react-d1cb92274f86"
  []
  (ra/create-class
   {:component-did-mount
    (fn
      [this]
        ;; We observe the frame to get its contentRect on resize.
      (.observe resize-observer (dom/dom-node this))
      (rf/dispatch [:pan-to-active-page :original]))

    :component-will-unmount
    #(.disconnect resize-observer)

    :reagent-render
    #(let [canvas @(rf/subscribe [:elements/canvas])
           {:keys [x y]} @(rf/subscribe [:content-rect])
           ;; This is a different browsing context inside an iframe.
           ;; We need to simulate the events to the parent window.
           on-keyboard-event (fn [event]
                               ;; TODO use re-pressed :prevent-default-keys
                               (.preventDefault event)
                               (.dispatchEvent js/window.parent.document
                                               (js/KeyboardEvent. (.-type event)
                                                                  event)))]
       [:> Frame {:initialContent (server/render-to-static-markup [markup])
                  :mountTarget "body"
                  :on-key-down on-keyboard-event
                  :on-key-up on-keyboard-event
                  :id "frame"
                  :style {:flex "1 1"
                          :overflow "hidden"
                          :border 0
                          :background (-> canvas :attrs :fill)}}
        [:f> inner-component]
        [:> ContextMenu/Root
         [:> ContextMenu/Trigger
          [tools/render canvas]]
         [:> ContextMenu/Portal
          (into [:> ContextMenu/Content
                 {:class "menu-content context-menu-content"
                  :style {:margin-left (str x "px")
                          :margin-top (str y "px")}}]
                (map (fn [item] [comp/context-menu-item item])
                     comp/element-menu))]]])}))









