(ns renderer.attribute.views
  (:require
   [clojure.string :as str]
   [re-frame.core :as rf]
   ["@radix-ui/react-popover" :as Popover]
   ["@radix-ui/react-select" :as Select]
   [renderer.attribute.hierarchy :as hierarchy]
   [renderer.components :as comp]
   [renderer.tools.base :as tools]
   [config]
   [platform]))

(defn browser-support
  [browser version-added]
  [:div.text-center.flex-1
   [:div.flex-1 {:title browser}
    [comp/icon (name browser)]]
   (case version-added
     true [:div.support-cell.success "all"]
     false [:div.support-cell.error "x"]
     nil [:div.support-cell.warning "?"]
     [:div.support-cell.success (str "≥" version-added)])])

(defn caniusethis
  [{:keys [tag attr]}]
  (let [data (if attr
               (or (-> tools/svg-spec :elements tag attr :__compat)
                   (-> tools/svg-spec :attributes :presentation attr :__compat)
                   (-> tools/svg-spec :attributes :core attr :__compat)
                   (-> tools/svg-spec :attributes :style attr :__compat))
               (-> tools/svg-spec :elements tag :__compat))]
    (when (or (:support data) (isa? tag ::tools/animation))
      [:div.flex.flex-col
       (when (some :version_added (vals (:support data)))
         [:<>
          [:h4.font-bold.mb-1 "Browser compatibility"]
          [:div.flex.mb-4
           (map (fn [[browser {:keys [version_added]}]]
                  ^{:key browser}
                  [browser-support browser version_added]) (:support data))]])
       [:div.flex.gap-2
        (when (or data (:mdn_url data) (isa? tag ::tools/animation))
          [:button.button.px-3.level-2.grow
           {:on-click #(rf/dispatch
                        [:window/open-remote-url
                         (or (:mdn_url data)
                             (str "https://developer.mozilla.org/en-US/docs/Web/SVG/Attribute/" (name attr)))])}
           "Learn more"])
        (when (:spec_url data)
          [:button.button.px-3.grow
           {:on-click #(rf/dispatch [:window/open-remote-url (:spec_url data)])}
           "Specification"])]])))

(defn on-change-handler
  ([event key old-value finalize?]
   (let [value (.. event -target -value)]
     (when (not= value old-value)
       (rf/dispatch [(if finalize?
                       :elements/set-attribute
                       :elements/preview-attribute) key value]))))
  ([event key old-value]
   (on-change-handler event key old-value true)))

(defn on-key-down-handler
  [event key value]
  (case (.-keyCode event)
    13 (on-change-handler event key value)
    27 ((.. event -target blur)
        (set! (.. event -target -value) value))
    nil))

(defn form-input
  [{:keys [key value disabled? placeholder on-wheel]}]
  [:div.relative.flex.form-input
   {:style {:flex "1 0 70px"}}
   [:input {:key value
            :default-value value
            :disabled disabled?
            :placeholder (if value placeholder "multiple")
            :on-wheel on-wheel
            :on-blur #(on-change-handler % key value)
            :on-key-down #(on-key-down-handler % key value)}]
   (when-not (or (empty? (str value)) disabled?)
     [:button.button.ml-px.level-2.text-muted.absolute.right-0.clear-input-button
      {:style {:width "26px" :height "26px"}
       :on-pointer-down #(rf/dispatch [:elements/set-attribute key ""])}
      [comp/icon "times" {:class "small"}]])])

(defmethod hierarchy/form-element :default
  [key value disabled?]
  [form-input {:key key
               :value value
               :disabled? disabled?}])

(defn range-input
  [key value attrs initial]
  [:div.flex.w-full
   [form-input {:key key 
                :value value 
                :disabled? (:disabled attrs) 
                :placeholder initial}]
   [:input.ml-px
    (merge attrs
           {:value (if (= "" value) initial value)
            :type "range"
            :on-change #(on-change-handler % key value false)
            :on-pointer-up #(rf/dispatch [:elements/set-attribute key value])})]])

(defn select-input
  [{:keys [key value disabled? items initial]}]
  [:div.flex.w-full
   [form-input {:key key
                :value value
                :disabled? disabled?
                :placeholder initial}]
   [:> Select/Root {:value value
                    :onValueChange #(rf/dispatch [:elements/set-attribute key %])
                    :disabled disabled?}
    [:> Select/Trigger {:class "select-trigger ml-px"
                        :aria-label (name key)
                        :style {:background "var(--level-2)"
                                :border-radius 0
                                :width "26px"
                                :height "26px"}}
     [:> Select/Value
      (let [selected-item (some #(when (= (:value %) value) %)
                                items)]
        (if-let [icon (:icon selected-item)]
          [comp/icon icon]
          [:> Select/Icon
           [comp/icon "chevron-down" {:class "small"}]]))]]
    [:> Select/Portal
     [:> Select/Content {:class "menu-content rounded select-content"}
      [:> Select/ScrollUpButton {:class "select-scroll-button"}
       [comp/icon "chevron-up"]]
      [:> Select/Viewport {:class "select-viewport"}
       (map (fn [item]
              ^{:key item}
              [:> Select/Item {:value (:value item) :class "menu-item "}
               (when (:icon item)
                 [:div.absolute.left-2 [comp/icon (:icon item)]])
               [:> Select/ItemText (:label item)]]) items)]
      [:> Select/ScrollDownButton {:class "select-scroll-button"}
       [comp/icon "chevron-down"]]]]]])

(defn label
  [tag key]
  (let [clicked-element @(rf/subscribe [:clicked-element])
        webref-property @(rf/subscribe [:webref-css-property key])
        css-property  @(rf/subscribe [:css-property key])
        active? (and (= (:type clicked-element) :handler)
                     (= (:key clicked-element) key))]
    [:> Popover/Root
     {:modal true}
     [:> Popover/Trigger
      [:label.max-w-full.text-ellipsis
       {:class (when active? "text-active")} key]]
     [:> Popover/Portal
      [:> Popover/Content
       {:side "left"
        :class "popover-content"
        :align "start"}
       [:div.p-6
        [:h2.mb-4.text-lg key]
        (when (get-method hierarchy/description key)
          [:p (hierarchy/description key)])
        (when webref-property
          [:<>
           (when-let [applies-to (:appliesTo webref-property)]
             [:<>
              [:h3.font-bold "Applies to"]
              [:p applies-to]])
           (when-let [computed-value (:computedValue webref-property)]
             (when (not= computed-value "as specified")
               [:<>
                [:h3.font-bold "Computed value"]
                [:p
                 computed-value
                 (when-let [percentages (:percentages webref-property)]
                   (when (not= percentages "N/A")
                     (str " (percentages " percentages ")")))]]))
           (when-let [animatable (:animatable webref-property)]
             [:<>
              [:h3.font-bold "Animatable"]
              [:p animatable]])
           (when-let [animation-type (:animationType webref-property)]
             [:<>
              [:h3.font-bold "Animation Type"]
              [:p animation-type]])
           (when-let [style-declaration (:styleDeclaration webref-property)]
             [:<>
              [:h3.font-bold "Style declaration"]
              [:p (str/join " | " style-declaration)]])])
        (when css-property
          [:<>
           [:h3.font-bold "Syntax"]
           [:p (:syntax css-property)]])

        [caniusethis {:tag tag :attr key}]]
       [:> Popover/Arrow {:class "popover-arrow"}]]]]))

(defn row
  [key value locked? tag]
  (let [property @(rf/subscribe [:webref-css-property key])]
    [:<>
     [label tag key]
     [:div.flex.h-full.overflow-visible
      [hierarchy/form-element key value locked? (when property (:initial property))]]]))

(defn form
  []
  (let [selected-elements @(rf/subscribe [:elements/selected])
        element (first selected-elements)
        selected-attrs @(rf/subscribe [:elements/selected-attrs])
        {:keys [tag name locked?]} element]
    [:div.w-full.ml-px.v-scroll.flex.flex-col
     (when (seq selected-elements)
       [:div.w-full
        [:div.flex.level-2.py-4.pl-4.pr-2
         [:h1.self-center.flex-1.text-lg
          (if (empty? (rest selected-elements))
            (if (empty? name) tag name)
            (str (count selected-elements) " elements"))]
         [:div.py-px
          [:> Popover/Root {:modal true}
           [:> Popover/Trigger {:asChild true}
            [:span.pb-px [comp/icon-button {:title "MDN Info"
                                            :icon "info"}]]]
           [:> Popover/Portal
            [:> Popover/Content {:sideOffset 5
                                 :class "popover-content"
                                 :align "end"}
             [:div.p-6
              [:h2.mb-4.text-lg tag]
              (when-let [description (:description (tools/properties tag))]
                [:p description])
              [caniusethis {:tag tag}]
              (when-let [url (:url (tools/properties tag))]
                [:button.button.px-3.level-2.w-full
                 {:on-click #(rf/dispatch [:window/open-remote-url url])}
                 "Learn more"])]
             [:> Popover/Arrow {:class "popover-arrow"}]]]]]]
        [:div.attribute-grid
         (map (fn [[k v]] ^{:key k} [row k v locked? tag]) selected-attrs)]])
     [:div.level-1.grow.w-full.flex]]))
