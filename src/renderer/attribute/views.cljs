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
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.ui :as ui]
   [renderer.utils.bcd :as bcd]
   [renderer.utils.dom :as dom]
   [renderer.utils.keyboard :as keyb]
   [renderer.window.events :as-alias window.e]))

(defn browser-support
  [browser version-added]
  [:div.text-center.flex-1
   [:div {:title browser}
    [ui/icon (name (case browser
                     :firefox_android :firefox
                     :chrome_android :chrome
                     :opera_android :opera
                     :safari_ios :safari
                     :webview_ios :safari
                     browser))]]
   [:div.text-2xs.mt-1
    (case version-added
      true [:div.bg-success "all"]
      false [:div.bg-error "x"]
      nil [:div.bg-warning "?"]
      [:div.bg-success (str "≥" version-added)])]])

(defn browser-compatibility
  [support-data]
  [:<>
   [:h4.font-bold.mb-1 "Browser compatibility"]
   [:div.flex.mb-4.gap-px
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
        property  (when attr @(rf/subscribe [::app.s/property attr]))
        spec-url (or (:spec_url data) (:href property))
        spec-url (if (vector? spec-url) (first spec-url) spec-url)
        mdn-url (or (when data (or (:mdn_url data) (construct-mdn-url (name attr))))
                    (:mdn_url property))]
    [:div.flex.flex-col
     (when (some :version_added (vals support-data))
       [browser-compatibility support-data])
     [:div.flex.gap-2
      (when mdn-url [info-button mdn-url "Learn more"])
      (when spec-url [info-button spec-url "Specification"])]]))

(defn on-change-handler!
  ([event k old-v]
   (on-change-handler! event k old-v true))
  ([event k old-v finalize?]
   (let [new-v (.. event -target -value)]
     (when-not (= new-v old-v)
       (rf/dispatch [(if finalize?
                       ::element.e/set-attr
                       ::element.e/preview-attr) k new-v])))))

(defn form-input
  [k v {:keys [disabled placeholder] :as attrs}]
  [:div.relative.flex.form-input.flex-1
   [:input (merge attrs
                  {:key v
                   :id (name k)
                   :default-value v
                   :placeholder (if v placeholder "multiple")
                   :on-blur #(on-change-handler! % k v)
                   :on-key-down #(keyb/input-key-down-handler! % v on-change-handler! k v)})]
   (when-not (or (empty? (str v)) disabled)
     [:button.button.bg-primary.text-muted.absolute.h-full.right-0.clear-input-button.hover:bg-transparent.invisible.p-1
      {:on-pointer-down #(rf/dispatch [::element.e/remove-attr k])}
      [ui/icon "times"]])])

(defmethod hierarchy/form-element :default
  [_ k v {:keys [disabled placeholder]}]
  [form-input k v {:disabled disabled
                   :placeholder (if v placeholder "multiple")}])

(defn range-input
  [k v {:keys [placeholder disabled step] :as attrs}]
  [:div.flex.w-full.gap-px
   [form-input k v {:disabled disabled
                    :placeholder placeholder
                    :class "w-20"
                    :on-wheel (fn [e]
                                (when (= (.-target e) (.-activeElement js/document))
                                  (if (pos? (.-deltaY e))
                                    (rf/dispatch [::element.e/update-attr-and-focus k - step])
                                    (rf/dispatch [::element.e/update-attr-and-focus k + step]))))}]
   [:div.px-1.w-full.bg-primary
    [:> Slider/Root
     (merge attrs {:class "slider-root"
                   :value [(if (= "" v) placeholder v)]
                   :onValueChange (fn [[v]] (rf/dispatch [::element.e/preview-attr k v]))
                   :onValueCommit (fn [[v]] (rf/dispatch [::element.e/set-attr k v]))})
     [:> Slider/Track {:class "slider-track"}
      [:> Slider/Range {:class "slider-range"}]]
     [:> Slider/Thumb {:class "slider-thumb"}]]]])

(defn select-input
  [k v {:keys [disabled items default-value] :as attrs}]
  [:div.flex.w-full.gap-px
   [form-input k v (select-keys attrs [:disabled :placeholder])]
   (when (seq items)
     [:> Select/Root
      {:value (if (empty? v) default-value v)
       :onValueChange #(rf/dispatch [::element.e/set-attr k %])
       :disabled disabled}
      [:> Select/Trigger
       {:class "form-control-button"
        :aria-label (name k)}
       [:> Select/Value ""]
       [:> Select/Icon
        [ui/icon "chevron-down"]]]
      [:> Select/Portal
       [:> Select/Content
        {:class "menu-content rounded select-content"
         :on-key-down dom/stop-propagation!}
        [:> Select/ScrollUpButton
         {:class "select-scroll-button"}
         [ui/icon "chevron-up"]]
        [:> Select/Viewport
         {:class "select-viewport"}
         (for [item items]
           ^{:key item}
           [:> Select/Item
            {:value (:value item) :class "menu-item"}
            (when (:icon item)
              [:div.absolute.left-2 [ui/icon (:icon item)]])
            [:> Select/ItemText (:label item)]])]
        [:> Select/ScrollDownButton
         {:class "select-scroll-button"}
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
         computed-value]]))
   (when-let [percentages (:percentages property)]
     (when-not (= percentages "N/A")
       [:<>
        [:h3.font-bold "Percentages"]
        [:p percentages]]))
   (when-let [animatable (:animatable property)]
     [:<>
      [:h3.font-bold "Animatable"]
      [:p animatable]])
   (when-let [animation-type (:animationType property)]
     [:<>
      [:h3.font-bold "Animation Type"]
      [:p (cond->> animation-type (vector? animation-type) (str/join " "))]])
   (when-let [style-declaration (:styleDeclaration property)]
     [:<>
      [:h3.font-bold "Style declaration"]
      [:p (str/join " | " style-declaration)]])
   (when-let [syntax (:syntax property)]
     [:<>
      [:h3.font-bold "Syntax"]
      [:p syntax]])])

(defn label
  [tag k]
  (let [clicked-element @(rf/subscribe [::app.s/clicked-element])
        property  @(rf/subscribe [::app.s/property k])
        dispatch-tag (if (contains? (methods hierarchy/description) [tag k]) tag :default)
        active (and (= (:type clicked-element) :handle)
                     (= (:key clicked-element) key))]
    [:> HoverCard/Root
     [:> HoverCard/Trigger
      [:label.w-28.truncate
       {:for (name k)
        :class (when active "text-active")} k]]
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
           (when property [property-list property])
           [caniusethis {:tag tag :attr k}]])]
       [:> HoverCard/Arrow {:class "popover-arrow"}]]]]))

(defn row
  [k v locked tag]
  (let [property @(rf/subscribe [::app.s/property k])
        initial (:initial property)
        dispatch-tag (if (contains? (methods hierarchy/form-element) [tag k]) tag :default)]
    [:<>
     [label tag k]
     [:div.flex.w-full
      [hierarchy/form-element dispatch-tag k v {:disabled locked
                                                :placeholder initial}]]]))

(defn tag-info
  [tag]
  [:div
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
       (when-let [description (:description (tool.hierarchy/properties tag))]
         [:p description])
       [caniusethis {:tag tag}]
       (when-let [url (:url (tool.hierarchy/properties tag))]
         [:button.button.px-3.bg-primary.w-full
          {:on-click #(rf/dispatch [::window.e/open-remote-url url])}
          "Learn more"])]
      [:> HoverCard/Arrow {:class "popover-arrow"}]]]]])

(defn form
  []
  (let [selected-elements @(rf/subscribe [::element.s/selected])
        selected-tags @(rf/subscribe [::element.s/selected-tags])
        selected-attrs @(rf/subscribe [::element.s/selected-attrs])
        locked @(rf/subscribe [::element.s/selected-locked])
        tag (first selected-tags)]
    (when-first [el selected-elements]
      [:div.pr-px
       [:div.flex.bg-primary.py-4.pl-4.pr-2
        [:h1.self-center.flex-1.text-lg.p-1
         (if-not (next selected-elements)
           (let [el-label (:label el)]
             (if (empty? el-label) tag el-label))
           (str (count selected-elements) " elements"))]
        (when-not (next selected-tags)
          [tag-info tag])]
       [:div.grid.grid-cols-2.grid-flow-row.my-px.w-full.gap-px
        {:style {:grid-template-columns "minmax(100px, auto) 1fr"}}
        (for [[k v] selected-attrs]
          ^{:key k} [row k v locked tag])]])))

(defmethod tool.hierarchy/right-panel :default [] [form])
