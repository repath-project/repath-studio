(ns renderer.attribute.views
  (:require
   ["@radix-ui/react-hover-card" :as HoverCard]
   ["@radix-ui/react-select" :as Select]
   [clojure.string :as str]
   [config]
   [platform]
   [re-frame.core :as rf]
   [renderer.attribute.hierarchy :as hierarchy]
   [renderer.components :as comp]
   [renderer.tools.base :as tools]
   [renderer.utils.keyboard :as keyb]
   [renderer.utils.spec :as spec]))

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

(defn browser-compatibility
  [support-data]
  [:<>
   [:h4.font-bold.mb-1 "Browser compatibility"]
   [:div.flex.mb-4
    (for [[browser {:keys [version_added]}] support-data]
      ^{:key browser} [browser-support browser version_added])]])

(defn mdn-button
  [mdn_url attr]
  [:button.button.px-3.level-2.grow
   {:on-click #(rf/dispatch
                [:window/open-remote-url
                 (or mdn_url
                     (str "https://developer.mozilla.org/en-US/docs/Web/SVG/Attribute/"
                          (name attr)))])} "Learn more"])

(defn spec-button
  [url]
  [:button.button.px-3.grow
   {:on-click #(rf/dispatch [:window/open-remote-url (if (vector? url) (first url) url)])}
   "Specification"])

(defn caniusethis
  [{:keys [tag attr]}]
  (let [data (if attr (spec/compat-data tag attr) (spec/compat-data tag))
        support-data (:support data)
        animation? (isa? tag ::tools/animation)]
    (when (or support-data animation?)
      [:div.flex.flex-col
       (when (some :version_added (vals support-data))
         [browser-compatibility support-data])
       [:div.flex.gap-2
        (when (or data (:mdn_url data) animation?)
          [mdn-button (:mdn_url data) attr])
        (when (:spec_url data)
          [spec-button (:spec_url data)])]])))

(defn on-change-handler
  ([event k old-v]
   (on-change-handler event k old-v true))
  ([event k old-v finalize?]
   (let [new-v (.. event -target -value)]
     (when-not (= new-v old-v)
       (rf/dispatch [(if finalize?
                       :element/set-attr
                       :element/preview-attribute) k new-v])))))

(defn form-input
  [{:keys [key value disabled? placeholder on-wheel]}]
  [:div.relative.flex.form-input
   {:style {:flex "1 0 70px"}}
   [:input {:key value
            :id (name key)
            :default-value value
            :disabled disabled?
            :placeholder (if value placeholder "multiple")
            :on-wheel on-wheel
            :on-blur #(on-change-handler % key value)
            :on-key-down #(keyb/input-key-down-handler % value on-change-handler key value)}]
   (when-not (or (empty? (str value)) disabled?)
     [:button.button.ml-px.level-2.text-muted.absolute.right-0.clear-input-button
      {:style {:width "26px" :height "26px"}
       :on-pointer-down #(rf/dispatch [:element/remove-attr key])}
      [comp/icon "times" {:class "small"}]])])

(defmethod hierarchy/form-element :default
  [k v disabled?]
  [form-input {:key k
               :value v
               :disabled? disabled?}])

(defn range-input
  [k v attrs initial]
  [:div.flex.w-full
   [form-input {:key k
                :value v
                :disabled? (:disabled attrs)
                :placeholder initial}]
   [:input.ml-px
    (merge attrs
           {:value (if (= "" v) initial v)
            :type "range"
            :on-change #(on-change-handler % k v false)
            :on-pointer-up #(rf/dispatch [:element/set-attr k v])})]])

(defn select-input
  [{:keys [key value disabled? items initial]}]
  [:div.flex.w-full
   [form-input {:key key
                :value value
                :disabled? disabled?
                :placeholder initial}]
   [:> Select/Root {:value value
                    :onValueChange #(rf/dispatch [:element/set-attr key %])
                    :disabled disabled?}
    [:> Select/Trigger {:class "select-trigger ml-px"
                        :aria-label (name key)
                        :style {:background "var(--level-2)"
                                :border-radius 0
                                :width "26px"
                                :height "26px"}}
     [:> Select/Value ""]
     [:> Select/Icon
      [comp/icon "chevron-down" {:class "small"}]]]
    [:> Select/Portal
     [:> Select/Content {:class "menu-content rounded select-content"}
      [:> Select/ScrollUpButton {:class "select-scroll-button"}
       [comp/icon "chevron-up"]]
      [:> Select/Viewport {:class "select-viewport"}
       (for [item items]
         ^{:key item}
         [:> Select/Item {:value (:value item) :class "menu-item "}
          (when (:icon item)
            [:div.absolute.left-2 [comp/icon (:icon item)]])
          [:> Select/ItemText (:label item)]])]
      [:> Select/ScrollDownButton {:class "select-scroll-button"}
       [comp/icon "chevron-down"]]]]]])

(defn property-list
  [property]
  [:<>
   (when-let [applies-to (:appliesTo property)]
     [:<>
      [:h3.font-bold "Applies to"]
      [:p applies-to]])
   (when-let [computed-value (:computedValue property)]
     (when-not (= computed-value "as specified")
       [:<>
        [:h3.font-bold "Computed value"]
        [:p
         computed-value
         (when-let [percentages (:percentages property)]
           (when-not (= percentages "N/A")
             (str " (percentages " percentages ")")))]]))
   (when-let [animatable (:animatable property)]
     [:<>
      [:h3.font-bold "Animatable"]
      [:p animatable]])
   (when-let [animation-type (:animationType property)]
     [:<>
      [:h3.font-bold "Animation Type"]
      [:p animation-type]])
   (when-let [style-declaration (:styleDeclaration property)]
     [:<>
      [:h3.font-bold "Style declaration"]
      [:p (str/join " | " style-declaration)]])])

(defn label
  [tag key]
  (let [clicked-element @(rf/subscribe [:clicked-element])
        webref-property @(rf/subscribe [:webref-css-property key])
        css-property  @(rf/subscribe [:css-property key])
        active? (and (= (:type clicked-element) :handler)
                     (= (:key clicked-element) key))]
    [:> HoverCard/Root
     [:> HoverCard/Trigger
      [:label.max-w-full.text-ellipsis
       {:for (name key)
        :class (when active? "text-active")} key]]
     [:> HoverCard/Portal
      [:> HoverCard/Content
       {:side "left"
        :class "popover-content"
        :align "start"}
       [:div.p-6
        [:h2.mb-4.text-lg key]
        (when (get-method hierarchy/description key)
          [:p (hierarchy/description key)])
        (when webref-property [property-list webref-property])
        (when css-property
          [:<>
           [:h3.font-bold "Syntax"]
           [:p (:syntax css-property)]])

        [caniusethis {:tag tag :attr key}]]
       [:> HoverCard/Arrow {:class "popover-arrow"}]]]]))

(defn row
  [k v locked? tag]
  (let [property @(rf/subscribe [:webref-css-property k])]
    [:<>
     [label tag k]
     [:div.flex.h-full.overflow-visible
      [hierarchy/form-element k v locked? (when property (:initial property))]]]))

(defn tag-info
  [tag]
  [:div.py-px
   [:> HoverCard/Root
    [:> HoverCard/Trigger {:asChild true}
     [:span.pb-px
      [comp/icon-button "info" {:title "MDN Info"}]]]
    [:> HoverCard/Portal
     [:> HoverCard/Content
      {:sideOffset 5
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
      [:> HoverCard/Arrow {:class "popover-arrow"}]]]]])

(defn form
  []
  (let [selected-elements @(rf/subscribe [:element/selected])
        selected-tags @(rf/subscribe [:element/selected-tags])
        selected-attrs @(rf/subscribe [:element/selected-attrs])
        locked? @(rf/subscribe [:element/selected-locked?])
        tag (first selected-tags)]
    [:div.w-full.ml-px.v-scroll.flex.flex-col.level-1.h-full
     (when (seq selected-elements)
       [:div.w-full.overflow-x-hidden
        [:div.flex.level-2.py-4.pl-4.pr-2
         [:h1.self-center.flex-1.text-lg.p-1
          (if (empty? (rest selected-elements))
            (let [el (first selected-elements)
                  name (:name el)]
              (if (empty? name) tag name))
            (str (count selected-elements) " elements"))]
         (when (empty? (rest selected-tags))
           [tag-info tag])]
        [:div.attribute-grid
         (for [[k v] selected-attrs]
           ^{:key k} [row k v locked? tag])]])
     [:div.level-1.grow.w-full.flex]]))
