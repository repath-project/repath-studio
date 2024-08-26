(ns renderer.attribute.views
  (:require
   ["@radix-ui/react-hover-card" :as HoverCard]
   ["@radix-ui/react-select" :as Select]
   ["@radix-ui/react-slider" :as Slider]
   [clojure.string :as str]
   [re-frame.core :as rf]
   [renderer.app.subs :as-alias app.s]
   [renderer.attribute.hierarchy :as hierarchy]
   [renderer.element.events :as-alias element.e]
   [renderer.element.subs :as-alias element.s]
   [renderer.tool.base :as tool]
   [renderer.ui :as ui]
   [renderer.utils.bcd :as bcd]
   [renderer.utils.keyboard :as keyb]
   [renderer.window.events :as-alias window.e]))

(defn browser-support
  [browser version-added]
  [:div.text-center.flex-1
   [:div.flex-1 {:title browser}
    [ui/icon (name browser)]]
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

(defn info-button
  [url label]
  [:button.button.px-3.bg-primary.grow
   {:on-click #(rf/dispatch [::window.e/open-remote-url url])}
   label])

(defn construct-mdn-url
  [attr]
  (str "https://developer.mozilla.org/en-US/docs/Web/SVG/Attribute/" attr))

(defn caniusethis
  [{:keys [tag attr]}]
  (let [data (if attr (bcd/conmpatibility tag attr) (bcd/conmpatibility tag))
        support-data (:support data)
        webref-property (when attr @(rf/subscribe [::app.s/webref-css-property attr]))
        css-property  (when attr @(rf/subscribe [::app.s/css-property attr]))
        spec-url (or (:spec_url data) (:href webref-property) (:href css-property))
        spec-url (if (vector? spec-url) (first spec-url) spec-url)
        ;; mdn-url (or (:mdn_url css-property) (:mdn_url data)) ; REVIEW
        mdn-url (or (:mdn_url data) (when data (construct-mdn-url (name attr))))]
    [:div.flex.flex-col
     (when (some :version_added (vals support-data))
       [browser-compatibility support-data])
     [:div.flex.gap-2
      (when mdn-url [info-button mdn-url "Learn more"])
      (when spec-url [info-button spec-url "Specification"])]]))

(defn on-change-handler
  ([event k old-v]
   (on-change-handler event k old-v true))
  ([event k old-v finalize?]
   (let [new-v (.. event -target -value)]
     (when-not (= new-v old-v)
       (rf/dispatch [(if finalize?
                       ::element.e/set-attr
                       ::element.e/preview-attr) k new-v])))))

(defn form-input
  [{:keys [key value disabled? placeholder on-wheel class]}]
  [:div.relative.flex.form-input.flex-1
   [:input {:key value
            :id (name key)
            :default-value value
            :disabled disabled?
            :class class
            :placeholder (if value placeholder "multiple")
            :on-wheel on-wheel
            :on-blur #(on-change-handler % key value)
            :on-key-down #(keyb/input-key-down-handler % value on-change-handler key value)}]
   (when-not (or (empty? (str value)) disabled?)
     [:button.button.ml-px.bg-primary.text-muted.absolute.h-full.right-0.clear-input-button.hover:bg-transparent
      {:style {:width "26px"}
       :on-pointer-down #(rf/dispatch [::element.e/remove-attr key])}
      [ui/icon "times"]])])

(defmethod hierarchy/form-element :default
  [_ k v disabled? initial]
  [form-input {:key k
               :value v
               :disabled? disabled?
               :placeholder (if v initial "multiple")}])

(defn range-input
  [k v attrs initial]
  [:div.flex.w-full
   [form-input {:key k
                :value v
                :disabled? (:disabled attrs)
                :placeholder initial
                :class "w-20"
                :on-wheel (fn [e]
                            (if (pos? (.-deltaY e))
                              (rf/dispatch [::element.e/update-attr k - (:step attrs)])
                              (rf/dispatch [::element.e/update-attr k + (:step attrs)])))}]
   [:div.ml-px.px-1.w-full.bg-primary
    [:> Slider/Root
     (merge attrs {:class "slider-root"
                   :value [(if (= "" v) initial v)]
                   :disabled (:disabled attrs)
                   :onValueChange (fn [[v]] (rf/dispatch [::element.e/preview-attr k v]))
                   :onValueCommit (fn [[v]] (rf/dispatch [::element.e/set-attr k v]))})
     [:> Slider/Track {:class "slider-track"}
      [:> Slider/Range {:class "slider-range"}]]
     [:> Slider/Thumb {:class "slider-thumb"}]]]])

(defn select-input
  [{:keys [key value disabled? items initial default-value]}]
  [:div.flex.w-full
   [form-input
    {:key key
     :value value
     :disabled? disabled?
     :placeholder initial}]
   (when (seq items)
     [:> Select/Root
      {:value (if (empty? value) default-value value)
       :onValueChange #(rf/dispatch [::element.e/set-attr key %])
       :disabled disabled?}
      [:> Select/Trigger
       {:class "select-trigger ml-px h-full"
        :aria-label (name key)
        :style {:background "var(--bg-primary)"
                :border-radius 0
                :width "26px"
                :height "26px"}}
       [:> Select/Value ""]
       [:> Select/Icon
        [ui/icon "chevron-down" {:class "icon small"}]]]
      [:> Select/Portal
       [:> Select/Content {:class "menu-content rounded select-content"}
        [:> Select/ScrollUpButton {:class "select-scroll-button"}
         [ui/icon "chevron-up"]]
        [:> Select/Viewport {:class "select-viewport"}
         (for [item items]
           ^{:key item}
           [:> Select/Item {:value (:value item) :class "menu-item"}
            (when (:icon item)
              [:div.absolute.left-2 [ui/icon (:icon item)]])
            [:> Select/ItemText (:label item)]])]
        [:> Select/ScrollDownButton {:class "select-scroll-button"}
         [ui/icon "chevron-down"]]]]])])

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
  [tag k]
  (let [clicked-element @(rf/subscribe [::app.s/clicked-element])
        webref-property @(rf/subscribe [::app.s/webref-css-property k])
        css-property  @(rf/subscribe [::app.s/css-property k])
        dispatch-tag (if (contains? (methods hierarchy/description) [tag k]) tag :default)
        active? (and (= (:type clicked-element) :handle)
                     (= (:key clicked-element) key))]
    [:> HoverCard/Root
     [:> HoverCard/Trigger
      [:label.w-28.truncate
       {:for (name k)
        :class (when active? "text-active")} k]]
     [:> HoverCard/Portal
      [:> HoverCard/Content
       {:side "left"
        :class "popover-content"
        :align "start"}
       [:div.p-5
        [:h2.mb-4.text-lg k]
        (when (get-method hierarchy/description [dispatch-tag k])
          [:p (hierarchy/description dispatch-tag k)])
        (when (bcd/conmpatibility tag k)
          [:<>
           (when webref-property
             [property-list webref-property])
           (when css-property
             [:<>
              [:h3.font-bold "Syntax"]
              [:p (:syntax css-property)]])

           [caniusethis {:tag tag :attr k}]])]
       [:> HoverCard/Arrow {:class "popover-arrow"}]]]]))

(defn row
  [k v locked? tag]
  (let [property @(rf/subscribe [::app.s/webref-css-property k])
        initial (when property (:initial property))
        dispatch-tag (if (contains? (methods hierarchy/form-element) [tag k]) tag :default)]
    [:<>
     [label tag k]
     [:div.flex.h-full.overflow-visible
      [hierarchy/form-element dispatch-tag k v locked? initial]]]))

(defn tag-info
  [tag]
  [:div.py-px
   [:> HoverCard/Root
    [:> HoverCard/Trigger {:as-child true}
     [:span.pb-px
      [ui/icon-button "info" {:title "MDN Info"}]]]
    [:> HoverCard/Portal
     [:> HoverCard/Content
      {:sideOffset 5
       :class "popover-content"
       :align "end"}
      [:div.p-5
       [:h2.mb-4.text-lg tag]
       (when-let [description (:description (tool/properties tag))]
         [:p description])
       [caniusethis {:tag tag}]
       (when-let [url (:url (tool/properties tag))]
         [:button.button.px-3.bg-primary.w-full
          {:on-click #(rf/dispatch [::window.e/open-remote-url url])}
          "Learn more"])]
      [:> HoverCard/Arrow {:class "popover-arrow"}]]]]])

(defn form
  []
  (let [selected-elements @(rf/subscribe [::element.s/selected])
        selected-tags @(rf/subscribe [::element.s/selected-tags])
        selected-attrs @(rf/subscribe [::element.s/selected-attrs])
        locked? @(rf/subscribe [::element.s/selected-locked?])
        tag (first selected-tags)]
    (when (seq selected-elements)
      [:div.w-full.overflow-hidden
       [:div.flex.bg-primary.py-4.pl-4.pr-2
        [:h1.self-center.flex-1.text-lg.p-1
         (if (empty? (rest selected-elements))
           (let [el (first selected-elements)
                 el-label (:label el)]
             (if (empty? el-label) tag el-label))
           (str (count selected-elements) " elements"))]
        (when (empty? (rest selected-tags))
          [tag-info tag])]
       [:div.attribute-grid
        (for [[k v] selected-attrs]
          ^{:key k} [row k v locked? tag])]])))

(defmethod tool/right-panel :default [] [form])
