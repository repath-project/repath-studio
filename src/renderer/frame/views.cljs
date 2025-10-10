(ns renderer.frame.views
  (:require
   ["@radix-ui/react-context-menu" :as ContextMenu]
   ["react" :as react]
   ["react-fps" :refer [FpsView]]
   ["react-frame-component" :default Frame :refer [useFrame]]
   [clojure.string :as string]
   [re-frame.core :as rf]
   [reagent.core :as reagent]
   [reagent.dom.server :as server]
   [renderer.app.subs :as-alias app.subs]
   [renderer.document.subs :as-alias document.subs]
   [renderer.element.hierarchy :as element.hierarchy]
   [renderer.element.subs :as-alias element.subs]
   [renderer.element.views :as element.views]
   [renderer.event.impl.wheel :as event.impl.wheel]
   [renderer.frame.events :as-alias frame.events]
   [renderer.frame.subs :as-alias frame.subs]
   [renderer.snap.subs :as-alias snap.subs]
   [renderer.tool.subs :as-alias tool.subs]
   [renderer.utils.i18n :refer [t]]
   [renderer.utils.length :as utils.length]
   [renderer.views :as views]
   [renderer.worker.subs :as-alias worker.subs]))

(defn coll->str
  [coll]
  (str "[" (string/join " " (map utils.length/->fixed coll)) "]"))

(defn map->str
  [m]
  (->> m
       (map (fn [[k v]]
              ^{:key k}
              [:span (str (name k) ": " (if (number? v)
                                          (utils.length/->fixed v)
                                          (coll->str v)))]))
       (interpose ", ")))

(defn debug-rows
  []
  (let [viewbox (rf/subscribe [::frame.subs/viewbox])
        pointer-pos (rf/subscribe [::app.subs/pointer-pos])
        adjusted-pos (rf/subscribe [::app.subs/adjusted-pointer-pos])
        pointer-offset (rf/subscribe [::app.subs/pointer-offset])
        adjusted-offset (rf/subscribe [::app.subs/adjusted-pointer-offset])
        drag? (rf/subscribe [::tool.subs/drag?])
        pan (rf/subscribe [::document.subs/pan])
        active-tool (rf/subscribe [::tool.subs/active])
        cached-tool (rf/subscribe [::tool.subs/cached])
        tool-state (rf/subscribe [::tool.subs/state])
        clicked-element (rf/subscribe [::app.subs/clicked-element])
        ignored-ids (rf/subscribe [::document.subs/ignored-ids])
        nearest-neighbor (rf/subscribe [::snap.subs/nearest-neighbor])]
    [["Viewbox" (coll->str @viewbox)]
     ["Pointer position" (coll->str @pointer-pos)]
     ["Adjusted pointer position" (coll->str @adjusted-pos)]
     ["Pointer offset" (coll->str @pointer-offset)]
     ["Adjusted pointer offset" (coll->str @adjusted-offset)]
     ["Pointer drag?" (str @drag?)]
     ["Pan" (coll->str @pan)]
     ["Active tool" @active-tool]
     ["Cached tool" @cached-tool]
     ["State" @tool-state]
     ["Clicked element" (:id @clicked-element)]
     ["Ignored elements" @ignored-ids]
     ["Snap" (map->str @nearest-neighbor)]]))

(defn debug-info
  []
  [:div
   {:dir "ltr"}
   (into [:div.absolute.top-1.left-2]
         (for [[s v] (debug-rows)]
           [:div.flex
            [:strong.mr-1 s]
            [:div v]]))
   [:div.fps-wrapper
    [:> FpsView #js {:width 240
                     :height 180}]]])

(defn help
  [message]
  [:div.hidden.justify-center.w-full.p-4.sm:flex
   [:div.bg-primary.overflow-hidden.shadow.rounded-full
    [:div.text-xs.gap-1.flex.flex-wrap.py-2.px-4.justify-center.truncate
     {:aria-live "polite"}
     message]]])

(defn read-only-overlay []
  [:div.absolute.inset-0.border-4.border-accent
   (when-let [preview-label @(rf/subscribe [::document.subs/preview-label])]
     [:div.absolute.bg-accent.top-2.left-2.px-1.rounded.text-accent-foreground
      preview-label])])

(defn inner-component
  "We need access to the iframe's window to add the wheel listener.
   This is required in order to prevent the default zoom behavior.
   https://github.com/ryanseddon/react-frame-component#accessing-the-iframes-window-and-document
   https://github.com/reagent-project/reagent/blob/master/doc/ReactFeatures.md#function-components"
  []
  (let [frame-window (.-window (useFrame))]
    (reagent/create-class
     {:component-did-mount
      #(.addEventListener frame-window "wheel"
                          event.impl.wheel/handler! #js {:passive false})

      :component-will-unmount
      #(.removeEventListener frame-window "wheel" event.impl.wheel/handler!)

      :reagent-render #()})))

(defn initial-markup
  "https://github.com/ryanseddon/react-frame-component#initialcontent
   The iframe is isolated, so we cannot access the css vars of the parent."
  []
  [:html {:data-theme "light"}
   [:head [:link {:rel "stylesheet"
                  :href "./css/styles.css"}]]
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
        (let [read-only? @(rf/subscribe [::document.subs/read-only?])
              help-message @(rf/subscribe [::tool.subs/help])
              help-bar @(rf/subscribe [::app.subs/help-bar])
              debug-info? @(rf/subscribe [::app.subs/debug-info])
              worker-active? @(rf/subscribe [::worker.subs/some-active?])
              root-el @(rf/subscribe [::element.subs/root])

              {:keys [x y]} @(rf/subscribe [::app.subs/dom-rect])
              ;; This is a different browsing context inside an iframe.
              ;; We need to simulate the events to the parent window.
              on-keyboard-event (fn [e]
                                  (.preventDefault e)
                                  (.dispatchEvent js/window.parent.document
                                                  (js/KeyboardEvent. (.-type e)
                                                                     e)))]
          [:> Frame
           {:initial-content (server/render-to-static-markup (initial-markup))
            :mount-target "body"
            :class "overflow-hidden flex-1 border-0"
            :on-key-down on-keyboard-event
            :on-key-up on-keyboard-event
            :id "frame"
            :title (t [::main-canvas "main canvas"])
            :ref ref
            :sandbox "allow-same-origin"
            :style {:background (-> root-el :attrs :fill)}}
           [:f> inner-component]
           [:> ContextMenu/Root
            [:> ContextMenu/Trigger
             [element.hierarchy/render root-el]
             [:div.absolute.inset-0.pointer-events-none.inset-shadow
              (when read-only?
                [read-only-overlay])
              (when debug-info?
                [debug-info])
              (when worker-active?
                [:button.icon-button.absolute.bottom-2.right-2
                 [views/loading-indicator]])
              (when (and help-bar (seq help-message))
                [help help-message])]]
            [:> ContextMenu/Portal
             (->> (element.views/context-menu)
                  (map views/context-menu-item)
                  (into [:> ContextMenu/Content
                         {:class "menu-content context-menu-content"
                          :on-close-auto-focus #(.preventDefault %)
                          :on-escape-key-down #(.stopPropagation %)
                          :style {:margin-left (str x "px")
                                  :margin-top (str y "px")}}]))]]]))})))
