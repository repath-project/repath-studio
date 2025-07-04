(ns renderer.attribute.views
  (:require
   ["@radix-ui/react-hover-card" :as HoverCard]
   ["@radix-ui/react-select" :as Select]
   [camel-snake-kebab.core :as camel-snake-kebab]
   [clojure.string :as string]
   [re-frame.core :as rf]
   [renderer.app.subs :as-alias app.subs]
   [renderer.attribute.hierarchy :as attribute.hierarchy]
   [renderer.element.events :as-alias element.events]
   [renderer.element.hierarchy :as element.hierarchy]
   [renderer.element.subs :as-alias element.subs]
   [renderer.event.impl.keyboard :as event.impl.keyboard]
   [renderer.events :as-alias events]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.tool.subs :as-alias tool.subs]
   [renderer.utils.attribute :as utils.attribute]
   [renderer.utils.i18n :refer [t]]
   [renderer.views :as views]))

(defn browser-support
  [browser version-added]
  [:div.text-center.flex-1
   [:div {:title browser}
    [views/icon (name (case browser
                        :firefox_android :firefox
                        :chrome_android :chrome
                        :opera_android :opera
                        :safari_ios :safari
                        :webview_ios :safari
                        browser))]]
   [:div.text-2xs.mt-1
    (case version-added
      true [:div.bg-success (t [::all "all"])]
      false [:div.bg-error "x"]
      nil [:div.bg-warning "?"]
      [:div.bg-success (str "≥" version-added)])]])

(defn browser-compatibility
  [support-data]
  [:<>
   [:h4.font-bold.mb-1 (t [::browser-compatibility "Browser compatibility"])]
   [views/scroll-area
    [:div.flex.mb-4.gap-px
     (for [[browser {:keys [version_added]}] support-data]
       ^{:key browser} [browser-support browser version_added])]]])

(defn info-button
  [url label]
  [:button.button.px-3.bg-primary.grow
   {:on-click #(rf/dispatch [::events/open-remote-url url])}
   label])

(defn construct-mdn-url
  [attr]
  (str "https://developer.mozilla.org/en-US/docs/Web/SVG/Reference/Attribute/" attr))

(defn caniusethis
  [{:keys [tag attr]}]
  (let [data (if attr
               (utils.attribute/compatibility tag attr)
               (utils.attribute/compatibility tag))
        support-data (:support data)
        property (when attr (utils.attribute/property-data-memo attr))
        spec-url (or (:spec_url data) (:href property))
        spec-url (if (vector? spec-url) (first spec-url) spec-url)
        mdn-url (or (when data (or (:mdn_url data) (construct-mdn-url (name attr))))
                    (:mdn_url property))]
    [:div.flex.flex-col
     (when (some :version_added (vals support-data))
       [browser-compatibility support-data])
     [:div.flex.gap-2
      (when mdn-url [info-button mdn-url (t [::learn-more "Learn more"])])
      (when spec-url [info-button spec-url (t [::specification "Specification"])])]]))

(defn on-change-handler!
  ([event k old-v]
   (on-change-handler! event k old-v true))
  ([event k old-v finalize?]
   (let [new-v (.. event -target -value)]
     (when-not (= new-v old-v)
       (rf/dispatch [(if finalize?
                       ::element.events/set-attr
                       ::element.events/preview-attr) k new-v])))))

(defn form-input
  [k v {:keys [disabled placeholder] :as attrs}]
  [:div.relative.flex.form-input.flex-1
   [:input.form-element
    (merge attrs
           {:key v
            :dir "ltr"
            :class "rtl:text-right"
            :id (name k)
            :default-value v
            :placeholder (if v placeholder "multiple")
            :on-blur #(on-change-handler! % k v)
            :on-key-down #(event.impl.keyboard/input-key-down-handler!
                           % v
                           on-change-handler! k v)})]
   (when-not (or (empty? (str v)) disabled)
     [:button.button.bg-primary.text-muted.absolute.h-full.right-0.p-1.invisible
      {:class "clear-input-button hover:bg-transparent rtl:right-auto rtl:left-0"
       :on-pointer-down #(rf/dispatch [::element.events/remove-attr k])}
      [views/icon "times"]])])

(defmethod attribute.hierarchy/form-element :default
  [_ k v {:keys [disabled placeholder]}]
  [form-input k v {:disabled disabled
                   :placeholder (if v placeholder "multiple")}])

(defn range-input
  [k v {:keys [placeholder disabled] :as attrs}]
  [:div.flex.flex-1.gap-px
   [form-input k v {:disabled disabled
                    :placeholder placeholder
                    :class "font-mono w-20"}]
   [:div.px-1.flex-1.bg-primary
    [views/slider
     (merge
      attrs
      {:value [(if (empty? v) placeholder v)]
       :on-value-change (fn [[v]] (rf/dispatch [::element.events/preview-attr k v]))
       :on-value-commit (fn [[v]] (rf/dispatch [::element.events/set-attr k v]))})]]])

(defn select-input
  [k v {:keys [disabled items default-value] :as attrs}]
  [:div.flex.w-full.gap-px
   [form-input k v (select-keys attrs [:disabled :placeholder])]
   (when (seq items)
     [:> Select/Root
      {:value (if (empty? v) default-value v)
       :onValueChange #(rf/dispatch [::element.events/set-attr k %])
       :disabled disabled}
      [:> Select/Trigger
       {:class "form-control-button"
        :aria-label (str "Select " (name k))}
       [:> Select/Value ""]
       [:> Select/Icon
        [views/icon "chevron-down"]]]
      [:> Select/Portal
       [:> Select/Content
        {:class "menu-content rounded-sm select-content"
         :on-key-down #(.stopPropagation %)}
        [:> Select/ScrollUpButton
         {:class "select-scroll-button"}
         [views/icon "chevron-up"]]
        [:> Select/Viewport
         {:class "select-viewport"}
         (for [item items]
           ^{:key item}
           [:> Select/Item
            {:value (:value item) :class "menu-item"}
            (when (:icon item)
              [:div.absolute.left-2 [views/icon (:icon item)]])
            [:> Select/ItemText (:label item)]])]
        [:> Select/ScrollDownButton
         {:class "select-scroll-button"}
         [views/icon "chevron-down"]]]]])])

(defn property-list-item
  [property k]
  (when-let [v (get property k)]
    [:<>
     [:h3.font-bold (if (= k :appliesto)
                      (t [::applies-to "Applies to"])
                      (t [(keyword "renderer.attribute.views" (name k))
                          (-> (camel-snake-kebab/->kebab-case-string k)
                              (string/replace "-" " ")
                              (string/capitalize))]))]
     [:p (cond->> v (vector? v) (string/join " | "))]]))

(defn property-list
  [property]
  (into [:<>]
        (map (partial property-list-item property))
        [:appliesto :computed :percentages :animatable :animationType :syntax]))

(defn label
  [tag k]
  (let [clicked-element @(rf/subscribe [::app.subs/clicked-element])
        property (utils.attribute/property-data-memo k)
        dispatch-tag (if (contains? (methods attribute.hierarchy/description) [tag k])
                       tag
                       :default)
        active (and (= (:type clicked-element) :handle)
                    (= (:key clicked-element) key))]
    [:> HoverCard/Root
     [:> HoverCard/Trigger
      {:as-child true}
      [:label.form-element.w-28.truncate
       {:for (name k)
        :dir "ltr"
        :class ["rtl:text-left!" (when active "text-active")]} k]]
     [:> HoverCard/Portal
      [:> HoverCard/Content
       {:side "left"
        :class "popover-content"
        :align "start"}
       [:div.p-5
        [:h2.mb-4.text-lg k]
        (when (get-method attribute.hierarchy/description [dispatch-tag k])
          [:p.text-pretty (attribute.hierarchy/description dispatch-tag k)])
        (when (utils.attribute/compatibility tag k)
          [:<>
           (when property [property-list property])
           [caniusethis {:tag tag :attr k}]])]
       [:> HoverCard/Arrow {:class "popover-arrow"}]]]]))

(defn row
  [k v locked? tag]
  (let [initial (utils.attribute/initial-memo tag k)
        dispatch-tag (if (contains? (methods attribute.hierarchy/form-element) [tag k])
                       tag
                       :default)]
    [:<>
     [label tag k]
     [:div.flex.flex-1
      [attribute.hierarchy/form-element dispatch-tag k v {:disabled locked?
                                                          :placeholder initial}]]]))

(defn tag-info
  [tag]
  [:div
   [:> HoverCard/Root
    [:> HoverCard/Trigger {:as-child true}
     [:span.pb-px
      [views/icon-button "info" {:title (t [::mdn-info "MDN Info"])
                                 :class "hover:bg-transparent"}]]]
    [:> HoverCard/Portal
     [:> HoverCard/Content
      {:sideOffset 5
       :class "popover-content"
       :align "end"}
      [:div.p-5
       [:h2.mb-4.text-lg (or (:label (element.hierarchy/properties tag)) tag)]
       (when-let [description (:description (element.hierarchy/properties tag))]
         [:p.text-pretty description])
       [caniusethis {:tag tag}]
       (when-let [url (:url (element.hierarchy/properties tag))]
         [:button.button.px-3.bg-primary.w-full
          {:on-click #(rf/dispatch [::events/open-remote-url url])}
          (t [::learn-more "Learn more"])])]
      [:> HoverCard/Arrow {:class "popover-arrow"}]]]]])

(defn form
  []
  (let [selected-elements @(rf/subscribe [::element.subs/selected])
        selected-tags @(rf/subscribe [::element.subs/selected-tags])
        selected-attrs @(rf/subscribe [::element.subs/selected-attrs])
        selected-locked? @(rf/subscribe [::element.subs/selected-locked?])
        tool-state @(rf/subscribe [::tool.subs/state])
        tool-cached-state @(rf/subscribe [::tool.subs/cached-state])
        locked? (or selected-locked?
                    (not= tool-state :idle)
                    (and tool-cached-state (not= tool-cached-state :idle)))
        tag (first selected-tags)
        multitag? (next selected-tags)]
    (when-first [el selected-elements]
      [:div
       [:div.flex.bg-primary.py-4.gap-1
        [:h1.self-center.flex-1.text-lg.px-4
         (if-not (next selected-elements)
           (let [el-label (:label el)
                 properties (element.hierarchy/properties tag)
                 tag-label (or (:label properties) (string/capitalize (name tag)))]
             (if (empty? el-label) tag-label el-label))
           (string/join " " [(count selected-elements)
                             (when-not multitag? (name tag))
                             "elements"]))]
        (when-not multitag?
          [tag-info tag])]
       [:div.grid.grid-cols-2.grid-flow-row.my-px.w-full.gap-px
        {:style {:grid-template-columns "minmax(120px, 120px) 1fr"}}
        (for [[k v] selected-attrs]
          ^{:key k} [row k v locked? tag])]])))

(defmethod tool.hierarchy/right-panel :default [] [form])
