(ns repath.studio.canvas-frame.views
  (:require
   [reagent.core :as ra]
   [reagent.dom :as dom]
   [reagent.dom.server :as server]
   [re-frame.core :as rf]
   [repath.studio.elements.views :refer [element-menu]]
   [repath.studio.tools.base :as tools]
   [repath.studio.mouse :as mouse]
   [repath.studio.styles :as styles]
   ["react-frame-component" :default Frame :refer [useFrame]]))

(defn inner-component
  "We need access to the iframe's window to add the mouse move listener.
   This is required in order to track mouse movement outside of our canvas.
   SEE https://github.com/ryanseddon/react-frame-component#accessing-the-iframes-window-and-document
   SEE https://github.com/reagent-project/reagent/blob/master/doc/ReactFeatures.md#function-components"
  []
  (let [window (.-window (useFrame))]
    (ra/create-class
     {:component-did-mount
      (fn
        []
        (.addEventListener window "mousemove" #(mouse/event-handler % nil))
        (.addEventListener window "mouseup" #(mouse/event-handler % nil)))

      :reagent-render #() })))

(defn frame-markup
  "SEE https://github.com/ryanseddon/react-frame-component#initialcontent"
  []
  [:html [:head] [:body {:style {:width "100%"
                                 :height "100%"
                                 :overflow "hidden"
                                 :user-select "none"
                                 :margin 0}}]])

(defn frame
  "Our canvas is wrapped within an iframe element that hosts anything that needs to be rendered.
   SEE https://github.com/ryanseddon/react-frame-component
   SEE https://medium.com/@ryanseddon/rendering-to-iframes-in-react-d1cb92274f86"
  []
  (let [resize-observer (js/ResizeObserver. (fn [entries]
                                               (let [client-rect (.getBoundingClientRect (.-target (.find entries (fn [] true))))
                                                     content-rect (js->clj (.toJSON (.-contentRect (.find entries (fn [] true)))) :keywordize-keys true)]
                                                 (rf/dispatch [:canvas/resize (assoc content-rect
                                                                                     :x (.-x client-rect)
                                                                                     :y (.-y client-rect))]))))]
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
              keyboard-event #(.dispatchEvent js/window.parent.document (js/KeyboardEvent. (.-type %) %))]
          [:> Frame {:initialContent (server/render-to-static-markup [frame-markup])
                     :mountTarget    "body"
                     :id             "canvas-frame"
                     :on-key-down     keyboard-event
                     :on-key-up       keyboard-event
                     :on-mouse-enter #(rf/dispatch [:set-mouse-over-canvas? true])
                     :on-mouse-leave #(rf/dispatch [:set-mouse-over-canvas? false])
                     :on-context-menu element-menu
                     :style          {:flex "1 1"
                                      :overflow "hidden"
                                      :border 0}}
           [:f> inner-component]
           [tools/render @(rf/subscribe [:elements/canvas])]
           (when @(rf/subscribe [:overlay]) [:div {:style {:position "absolute"
                                                           :top "10px"
                                                           :left "10px"
                                                           :background-color styles/level-3
                                                           :padding styles/h-padding
                                                           :color styles/font-color
                                                           :font-family styles/font-family-mono}} @(rf/subscribe [:overlay])])]))})))
