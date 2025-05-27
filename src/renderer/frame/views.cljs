(ns renderer.frame.views
  (:require
   ["@radix-ui/react-context-menu" :as ContextMenu]
   ["react" :as react]
   ["react-frame-component" :default Frame :refer [useFrame]]
   [re-frame.core :as rf]
   [reagent.core :as reagent]
   [reagent.dom.server :as server]
   [renderer.app.subs :as-alias app.subs]
   [renderer.element.hierarchy :as element.hierarchy]
   [renderer.element.subs :as-alias element.subs]
   [renderer.element.views :as element.views]
   [renderer.event.impl.pointer :as event.impl.pointer]
   [renderer.event.impl.wheel :as event.impl.wheel]
   [renderer.frame.events :as-alias frame.events]
   [renderer.views :as views]))

(defn inner-component
  "We need access to the iframe's window to add the pointer move listener.
   This is required in order to track pointer movement outside of our canvas.
   https://github.com/ryanseddon/react-frame-component#accessing-the-iframes-window-and-document
   https://github.com/reagent-project/reagent/blob/master/doc/ReactFeatures.md#function-components"
  []
  (let [frame-window (.-window (useFrame))]
    (reagent/create-class
     {:component-did-mount
      (fn []
        (.addEventListener frame-window "pointermove" event.impl.pointer/handler!)
        (.addEventListener frame-window "pointerup" event.impl.pointer/handler!)
        (.addEventListener frame-window "wheel" event.impl.wheel/handler! #js {:passive false}))

      :component-will-unmount
      (fn []
        (.removeEventListener frame-window "pointerup" event.impl.pointer/handler!)
        (.removeEventListener frame-window "pointermove" event.impl.pointer/handler!)
        (.removeEventListener frame-window "wheel" event.impl.wheel/handler!))

      :reagent-render #()})))

(defonce initial-markup
  ;; https://github.com/ryanseddon/react-frame-component#initialcontent
  [:html
   [:head]
   [:body {:style {:width "100%"
                   :height "100%"
                   :overflow "hidden"
                   :user-select "none"
                   :touch-action "none"
                   :margin 0}}]])

(def resize-observer
  (js/ResizeObserver.
   (fn [entries]
     (let [dom-rect (-> entries
                        (.find (fn [] true))
                        (.. -target getBoundingClientRect toJSON)
                        (js->clj :keywordize-keys true))]
       (rf/dispatch-sync [::frame.events/resize dom-rect])))))

(defn root
  "Our canvas is wrapped within an iframe element that hosts anything
   that needs to be rendered.
   https://github.com/ryanseddon/react-frame-component
   https://medium.com/@ryanseddon/rendering-to-iframes-in-react-d1cb92274f86"
  []
  (let [ref (react/createRef)]
    (reagent/create-class
     {:component-did-mount
      #(.observe resize-observer (.-current ref))

      :component-will-unmount
      #(.disconnect resize-observer)

      :reagent-render
      (fn []
        (let [root-el @(rf/subscribe [::element.subs/root])
              {:keys [x y]} @(rf/subscribe [::app.subs/dom-rect])
              ;; This is a different browsing context inside an iframe.
              ;; We need to simulate the events to the parent window.
              on-keyboard-event (fn [e]
                                  ;; TODO: use re-pressed :prevent-default-keys
                                  (.preventDefault e)
                                  (.dispatchEvent js/window.parent.document
                                                  (js/KeyboardEvent. (.-type e)
                                                                     e)))]
          [:> Frame
           {:initial-content (server/render-to-static-markup initial-markup)
            :mount-target "body"
            :class "overflow-hidden flex-1 border-0"
            :on-key-down on-keyboard-event
            :on-key-up on-keyboard-event
            :id "frame"
            :title "main canvas"
            :ref ref
            :sandbox "allow-same-origin"
            :style {:background (-> root-el :attrs :fill)}}
           [:f> inner-component]
           [:> ContextMenu/Root
            [:> ContextMenu/Trigger
             [element.hierarchy/render root-el]]
            [:> ContextMenu/Portal
             (into [:> ContextMenu/Content
                    {:class "menu-content context-menu-content"
                     :on-close-auto-focus #(.preventDefault %)
                     :style {:margin-left (str x "px")
                             :margin-top (str y "px")}}]
                   (map views/context-menu-item element.views/context-menu))]]]))})))
