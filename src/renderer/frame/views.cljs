(ns renderer.frame.views
  (:require
   ["@radix-ui/react-context-menu" :as ContextMenu]
   ["react" :as react]
   ["react-frame-component" :default Frame :refer [useFrame]]
   [re-frame.core :as rf]
   [reagent.core :as ra]
   [reagent.dom.server :as server]
   [renderer.app.subs :as-alias app.s]
   [renderer.document.subs :as-alias document.s]
   [renderer.element.subs :as-alias element.s]
   [renderer.element.views :as element.v]
   [renderer.frame.events :as-alias frame.e]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.ui :as ui]
   [renderer.utils.pointer :as pointer]
   [renderer.utils.wheel :as wheel]))

(defn inner-component
  "We need access to the iframe's window to add the pointer move listener.
   This is required in order to track pointer movement outside of our canvas.
   https://github.com/ryanseddon/react-frame-component#accessing-the-iframes-window-and-document
   https://github.com/reagent-project/reagent/blob/master/doc/ReactFeatures.md#function-components"
  []
  (let [frame-window (.-window (useFrame))]
    (ra/create-class
     {:component-did-mount
      (fn []
        (doseq
         [event ["pointermove" "pointerup"]]
          (.addEventListener frame-window event pointer/event-handler))
        (.addEventListener frame-window "wheel" wheel/event-handler #js {:passive false}))

      :component-will-unmount
      (fn []
        (doseq
         [event ["pointermove" "pointerup"]]
          (.removeEventListener frame-window event pointer/event-handler))
        (.removeEventListener frame-window "wheel" wheel/event-handler))

      :reagent-render #()})))

(defn markup
  "https://github.com/ryanseddon/react-frame-component#initialcontent"
  []
  [:html
   [:head]
   [:body {:style {:width "100%"
                   :height "100%"
                   :overflow "hidden"
                   :user-select "none"
                   :touch-action "none"
                   :margin 0
                   :font-family "-apple-system, BlinkMacSystemFont,
                                'Segoe UI (Custom)', 'Roboto',
                                'Helvetica Neue', 'Open Sans (Custom)',
                                system-ui, sans-serif, 'Apple Color Emoji',
                                'Segoe UI Emoji'"}}]])

(def resize-observer
  (js/ResizeObserver.
   (fn [entries]
     (let [dom-rect (-> entries
                        (.find (fn [] true))
                        (.. -target getBoundingClientRect toJSON)
                        (js->clj :keywordize-keys true))]
       (rf/dispatch-sync [::frame.e/resize dom-rect])))))

(defn root
  "Our canvas is wrapped within an iframe element that hosts anything
   that needs to be rendered.
   https://github.com/ryanseddon/react-frame-component
   https://medium.com/@ryanseddon/rendering-to-iframes-in-react-d1cb92274f86"
  []
  (let [ref (react/createRef)]
    (ra/create-class
     {:component-did-mount
      #(.observe resize-observer (.-current ref))

      :component-will-unmount
      #(.disconnect resize-observer)

      :reagent-render
      (fn []
        (let [root-el @(rf/subscribe [::element.s/root])
              {:keys [x y]} @(rf/subscribe [::app.s/dom-rect])
             ;; This is a different browsing context inside an iframe.
             ;; We need to simulate the events to the parent window.
              on-keyboard-event (fn [e]
                                 ;; TODO: use re-pressed :prevent-default-keys
                                  (.preventDefault e)
                                  (.dispatchEvent js/window.parent.document
                                                  (js/KeyboardEvent. (.-type e)
                                                                     e)))]
          [:> Frame {:initial-content (server/render-to-static-markup [markup])
                     :mount-target "body"
                     :class "overflow-hidden flex-1 border-0"
                     :on-key-down on-keyboard-event
                     :on-key-up on-keyboard-event
                     :id "frame"
                     :title "main canvas"
                     :ref ref
                     :style {:background (-> root-el :attrs :fill)}}
           [:f> inner-component]
           [:> ContextMenu/Root
            [:> ContextMenu/Trigger
             [tool.hierarchy/render root-el]]
            [:> ContextMenu/Portal
             (into [:> ContextMenu/Content
                    {:class "menu-content context-menu-content"
                     :on-close-auto-focus #(.preventDefault %)
                     :style {:margin-left (str x "px")
                             :margin-top (str y "px")}}]
                   (map ui/context-menu-item element.v/context-menu))]]]))})))
