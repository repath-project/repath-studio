(ns repath.studio.attrs.views
  (:require
   [repath.studio.attrs.db]
   [repath.studio.styles :as styles]
   [repath.studio.components :as comp]
   [reagent.core :as ra]
   [re-frame.core :as rf]
   [repath.studio.tools.base :as tools]
   [repath.studio.codemirror.views :as cm]
   [clojure.string :as str]
   [repath.config :as config]
   ["react-color" :refer [ChromePicker]]
   ["@fluentui/react" :as fui]))

(def css-properties (js->clj js/window.api.mdn.css.properties :keywordize-keys true))

(derive ::color ::attr)
(derive ::coordinate ::attr)
(derive ::length ::attr)
(derive ::radius ::attr)

(derive :fill ::color)
(derive :stroke ::color)

(derive :x ::coordinate)
(derive :y ::coordinate)
(derive :x1 ::coordinate)
(derive :y1 ::coordinate)
(derive :x2 ::coordinate)
(derive :y2 ::coordinate)
(derive :cx ::coordinate)
(derive :cy ::coordinate)
(derive :dx ::coordinate)
(derive :dy ::coordinate)
(derive :x ::coordinate)
(derive :x ::coordinate)
(derive :x ::coordinate)

(derive :width ::length)
(derive :height ::length)
(derive :stroke-width ::length)
(derive :r ::length)

(derive :rx ::radius)
(derive :ry ::radius)

(def attrs-order [:d
                  :points
                  :x :y
                  :x1 :y1
                  :x2 :y2
                  :cx :cy
                  :dx :dy
                  :width :height
                  :rx :ry
                  :r
                  :rotate
                  :transform
                  :font-family :font-size :font-weight :textLength :lengthAdjust
                  :viewBox :preserveAspectRatio
                  :stroke
                  :fill
                  :stroke-width :stroke-linejoin
                  :opacity
                  :overflow
                  :id :class :tabindex
                  :style])

(defn caniusethis
  [{:keys [type attr]}]
  (let [data (if attr
               (or (get-in tools/svg-elements [type attr :__compat])
                   (get-in tools/svg-attributes [:presentation attr :__compat])
                   (get-in tools/svg-attributes [:core attr :__compat])
                   (get-in tools/svg-attributes [:style attr :__compat]))
               (get-in tools/svg-elements [type :__compat]))]
    (when (:support data)
      [::div.v-box
       [:h3 "Browser compatibility"]
       [:div.h-box
        (map (fn [[browser {:keys [version_added]}]]
               [:div {:key browser :style {:text-align "center" :flex "1 1"}}
                [:div {:title browser
                       :style {:flex "1 1"}} (comp/icon {:icon (name browser)
                                                         :set "brands"})]
                (case version_added
                  true [:div.support-cell.success "all"]
                  false [:div.support-cell.error "x"]
                  nil [:div.support-cell.warning "?"]
                  [:div.support-cell.success (str "≥" version_added)])]) (:support data))]
       (js/console.log (get-in tools/svg-elements [type attr :__compat])
                       (get-in tools/svg-attributes [:presentation attr :__compat])
                       (get-in tools/svg-attributes [:core attr :__compat])
                       (get-in tools/svg-attributes [:style attr :__compat]))
       (when (or data (:mdn_url data))
         [:a {:style {:margin-top "14px" :display "inline-block"} 
              :on-click #(rf/dispatch [:window/open-remote-url (if (:mdn_url data)
                                                                 (:mdn_url data)
                                                                 (str "https://developer.mozilla.org/en-US/docs/Web/SVG/Attribute/" (name attr)))])} "Learn more"])
       (when (:spec_url data) [:a {:style {:margin-top "14px" :display "inline-block"} :on-click #(rf/dispatch [:window/open-remote-url (:spec_url data)])} "Specification"])])))



(defn on-change-handler
  ([event key old-value finalize?]
   (let [value (.. event -target -value)]
     (when (not= value old-value) (rf/dispatch [:elements/set-attribute key value finalize?]))))
  ([event key old-value]
   (let [value (.. event -target -value)]
     (when (not= value old-value) (rf/dispatch [:elements/set-attribute key value true])))))

(defn on-key-down-handler
  [event key value]
  (case (.-keyCode event)
    13 (on-change-handler event key value)
    27 ((.. event -target blur)
        (set! (.. event -target -value) value))
    nil))

(defmulti form-group (fn [type key _] key))
(defmulti form-element (fn [key _] key))

(defmethod form-element :default
  [key value]
  ^{:key (or value key)} [:input {:default-value value
                                  :placeholder (when-not value "multiple")
                                  :on-blur #(on-change-handler % key value)
                                  :on-key-down #(on-key-down-handler % key value)}])

(defmethod form-element :font-family
  [key value]
  (let [fonts @(rf/subscribe [:font-options])]
    [:> fui/ComboBox {:text value
                      :dropdownMaxWidth 300
                      :allowFreeform true
                      :autoComplete "on"
                      :styles {:input {:font-size "12px"}}
                      :options fonts
                      :onChange #(on-change-handler % key value)}]))

(defmethod form-element :style
  [key value]
  [:div {:key (or value key)
         :style {:width "100%"
                 :background-color styles/level-2
                 :padding "0 8px"}} [cm/editor value {:on-blur #(rf/dispatch [:elements/set-attribute key %])}]])

(defn points-to-vec
  [points]
  (as-> points p
    (str/triml p)
    (str/split p #"\s+")
    (partition 2 p)))

(defmethod form-element :points
  [key value]
  [:div.v-box
   ^{:key value} [:input {:default-value value
                          :on-blur #(on-change-handler % key value)
                          :on-key-down #((when (= (.-keyCode %) 13) (on-change-handler % key value)))}]
   [:div.v-scroll {:key key
                   :style {:width "100%"
                           :max-height "300px"
                           :background-color styles/level-2}}
    [:dl (map (fn [node] [:<>
                          [:dt]
                          [:dd (map (fn [step]
                                      [:input {:style {:width "50%"} :value step}]) node)]]) (points-to-vec value))]]])

(defn range-input
  [key value attrs]
  [:div.h-box
   ^{:key value} [:input {:style {:flex "1 0 60px"}
                          :default-value value
                          :on-blur #(on-change-handler % key value)
                          :on-key-down #(on-key-down-handler % key value)}]
   [:input (merge attrs {:style {:flex "1 1 100%"
                                 :border-left (str "1px solid " styles/level-1)}
                         :value (if (= "" value) 1 value)
                         :type "range"
                         :on-change #(on-change-handler % key value false)
                         :on-mouse-up #(rf/dispatch [:elements/set-attribute key value true])})]])

(defmethod form-element :opacity
  [key value]
  [range-input key value {:min 0
                          :max 1
                          :step "0.01"}])

(defmethod form-element ::color
  [key value]
  (let [picker (ra/atom nil)]
    (fn [key value]
      [:<>
       ^{:key value} [:input {:default-value value
                              :on-blur #(on-change-handler % key value)
                              :on-key-down #(on-key-down-handler % key value)}]
       [:button {:type "button"
                 :on-click #(if @picker
                              (reset! picker nil)
                              (reset! picker (.-target %)))
                 :style {:border-left (str "1px solid " styles/level-1)
                         :flex "0 0 26px"
                         :height "26px"
                         :background styles/level-1
                         :box-sizing "border-box"}} [:div {:class "color-drip"
                                                           :style {:border (str "5px solid " styles/level-2)
                                                                   :background value}}]]
       (when @picker [:> fui/Callout {:styles {:root {:padding "12px" :z-index "1"}}
                                      :onDismiss #(reset! picker nil)
                                      :doNotLayer true
                                      :target @picker}
                      [:> ChromePicker
                       {:color value
                        :style {:overflowY "hidden"}
                        :on-change-complete #(rf/dispatch [:elements/set-attribute key (:hex (js->clj % :keywordize-keys true)) true])
                        :on-change #(rf/dispatch [:elements/set-attribute key (:hex (js->clj % :keywordize-keys true)) false])}]])])))

(defmethod form-group :default
  [type key value disabled?]
  (let [attr-info (ra/atom nil)]
    (fn [type key value disabled?]
      [:tr.form-group
       {:key key
        :class (when disabled? "disabled")}
       [:td {:style {:max-width "100px"
                     :min-width "80px"
                     :background-color styles/level-2}}
        [:label
         {:style {:max-width "100%" :text-overflow "ellipsis"}
          :onClick #(reset! attr-info (.-target %))} key]
        (when @attr-info [:> fui/Callout {:style {:padding "20px 24px"}
                                          :onDismiss #(reset! attr-info nil)
                                          :role "alertdialog"
                                          :shouldDismissOnWindowFocus true
                                          :target  @attr-info
                                          :directionalHint fui/DirectionalHint.bottomLeftEdge
                                          :calloutWidth 300}
                          [:<>
                           [:h2 {:style {:margin-top 0}} key]
                           (when (-> css-properties key)
                             [:p (-> css-properties key :syntax)])
                           [caniusethis {:type type :attr key}]]])]

       [:td [:div.h-box {:style {:overflow "visible"
                                 :height "100%"}}

             [form-element key value]]]])))

(when (not config/debug?) (defmethod form-group :key []))

(defn form
  []
  (let [info (ra/atom nil)]
    (fn []
      (let [width @(rf/subscribe [:right-sidebar-width])
            selected-elements @(rf/subscribe [:elements/selected])
            element (first selected-elements)
            selected-attrs @(rf/subscribe [:elements/selected-attrs])
            {:keys [type attrs name]} element]
        [:div.v-scroll {:on-submit #(.preventDefault %)
                        :style {:width width
                                :height "100%"}}
         (when (seq selected-elements)
           [:table {:style {:width width
                            :border-spacing "1px 1px"
                            :box-sizing "border-box"
                            :margin-top "-1px"
                            :height "1px"}}
            [:tbody [:tr [:th.form-group {:col-span 2
                                          :style {:font-weight "300"
                                                  :background-color styles/level-2
                                                  :text-align "left"
                                                  :font-size "18px"
                                                  :padding "23px 8px 23px 16px"}}
                          [:div.h-box
                           [:span {:style {:flex "1" :elements/align-self "center"}} (if (empty? (rest selected-elements))
                                                                                       (if (empty? name) type name)
                                                                                       (str (count selected-elements) " elements"))]
                           [:<>
                            [comp/icon-button {:title "MDN Info" :icon "info" :action #(if @info
                                                                                         (reset! info nil)
                                                                                         (reset! info (.-target %)))}]
                            (when @info [:> fui/Callout {:style {:padding "20px 24px"}
                                                         :onDismiss #(reset! info nil)
                                                         :role "alertdialog"
                                                         :target @info
                                                         :calloutWidth 300}
                                         [:<>
                                          [:h2 {:style {:margin-top 0}} type]
                                          [:p (:description (tools/properties type))]
                                          [caniusethis {:type type}]]])]]]]
             (map (fn [[k v]] ^{:key k} [form-group type k v]) (sort-by (fn [[k _]] (.indexOf attrs-order k)) selected-attrs))]])]))))


