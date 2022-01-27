(ns repath.studio.context-menu.views
  (:require [reagent.core :as ra]
            [re-frame.core :as rf]))

;;; Make sure to create the context-menu element somewhere in the dom.
;;; Recommended: at the start of the document.



(defonce default-menu-atom (ra/atom {:actions [["Action" #(prn "hello")]]
                                     :left 0
                                     :top 0
                                     :display nil}))

(defn- show-context! [menu-atom actions x y]
  (swap! menu-atom assoc
         :actions actions
         :left x  
         :top (- y 10) ;; We want the menu to appear slightly under the mouse
         :display "block"))

(defn- hide-context! [menu-atom]
  (swap! menu-atom assoc :display nil))


;;;; container to be included into the document

(declare actions-to-components)

(defn- reposition!
  "Make sure the dom-node is within the viewport. Update the
  `offsets-a' with the necessary :top and :left."
  [offsets-a dom-node]
  (let [{:keys [top left]} @offsets-a
        bcr (.getBoundingClientRect dom-node)
        x (- (.-right bcr) js/window.innerWidth)
        y (- (.-bottom bcr) js/window.innerHeight)
        [new-left new-top] (map - [left top] [(if (pos? x) x 0)
                                              (if (pos? y) y 0)])]
    (swap! offsets-a assoc :margin-left new-left :margin-top new-top)))


(defn- inner-submenu [actions-coll s-menus-a hide-context!]
  (let [dom-node (atom nil)
        offsets (ra/atom {:margin-top 0 :margin-left 0})]
    (ra/create-class
     {:component-did-mount #(reposition! offsets @dom-node)
      :reagent-render
      (fn []
        (let [{:keys [margin-top margin-left]} @offsets]
          [:ul.dropdown-menu.context-menu
           {:class (when (not= 0 margin-left) "open-left")
            :style {:display :block
                    :margin-top margin-top
                    :margin-left margin-left}
            :ref (fn [this] (reset! dom-node this))}
           (actions-to-components actions-coll s-menus-a hide-context!)]))})))

(defn- submenu-component [showing-submenus-atom id name actions-coll hide-context!]
  (let [show? (ra/cursor showing-submenus-atom [id])
        s-menus-a (ra/cursor showing-submenus-atom [:sub id])]
    (ra/create-class
     {:component-did-mount (fn [])
      :reagent-render
      (fn []
        [:li {:class "context-submenu"}
         [:button {:class (str "context-button " (when @show? "selected"))
                   :on-mouse-over #(reset! showing-submenus-atom {id true})
                   :on-click #(do (.stopPropagation %)
                                  (swap! show? not))}
          name]
         (when @show?
           [inner-submenu actions-coll s-menus-a hide-context!])])})))

(defn- action-component [name action-fn hide-context!]
  [:button {:class "context-button"
            :on-click #(do (.stopPropagation %)
                        (hide-context!)
                        (action-fn %))} name])

(defn- action-or-submenu [[id item] showing-submenus-atom hide-context!]
  (let [[name fn-or-sub] item
        submenu (when (coll? fn-or-sub) fn-or-sub)
        clear-sub-menus! #(reset! showing-submenus-atom nil)]
    (cond submenu [submenu-component showing-submenus-atom id name submenu hide-context!]
          fn-or-sub [:li {:on-mouse-enter clear-sub-menus!}
                     [action-component name fn-or-sub hide-context!]]
          :else [:li {:class :disabled
                      :on-mouse-enter clear-sub-menus!}
                 [:button {:class "context-button"} name]])))


(defn- actions-to-components [actions-coll showing-submenus-atom hide-context!]
  (for [[id item] (map-indexed vector actions-coll)]
    (let [clear-sub-menus! #(reset! showing-submenus-atom nil)]
      (cond
        (coll? item) ^{:key id} [action-or-submenu [id item] showing-submenus-atom hide-context!]
        (keyword? item)
        ^{:key id} [:li.divider {:on-mouse-enter clear-sub-menus!}]

        :else
        ^{:key id} [:li.dropdown-header
                    {:style {:cursor :default}
                     :on-mouse-enter clear-sub-menus!}
                    item]))))


(defn- inner-context-menu
  [menu-atom hide-context!]
  (let [dom-node (atom nil)
        showing-submenus-atom (ra/atom {})
        offsets (ra/atom {:margin-top 0 :margin-left 0})]
    (ra/create-class
     {:component-did-mount #(reposition! offsets @dom-node)
      :reagent-render
      (fn []
        (let [{:keys [display actions left top]} @menu-atom
              {:keys [margin-top margin-left]} @offsets
              esc-handler! (fn [evt]
                             (when (= (.-keyCode evt) 27) ;; `esc' key
                               (.stopPropagation evt)
                               (hide-context!)))
              scroll! (fn [evt]
                        (let [dy (.-deltaY evt)]
                          (swap! menu-atom update-in [:top] #(- % dy))))]
          [:div.context-menu-container
           {:style {:position :fixed
                    :left left
                    :top top
                    :margin-left margin-left
                    :margin-top margin-top}
            :class (when (not= 0 margin-left) "open-left")}
           [:ul.dropdown-menu.context-menu
            {:ref (fn [this]
                    (reset! dom-node this)
                    (when this
                      (.focus this)))
             :on-key-up esc-handler!
             :tab-index -1
             :role "menu"
             :on-wheel scroll!
             :style {:display (or display "none")
                     :position :relative}}
            (when actions
              (actions-to-components actions showing-submenus-atom hide-context!))]]))})))


;; main component for the user


(defn context-menu
  "The context menu component. Will use a default (and global) state
  ratom if none is provided."
  ([] (context-menu default-menu-atom))
  ([menu-atom]
   ;; remove the context menu if we click out of it or press `esc' (like the normal context menu)  
   (let [hide-context! #(hide-context! menu-atom)
         display (get @menu-atom :display)]
     [:div
      (when display
        [:div.context-menu-backdrop
         {:on-context-menu (fn [e]
                             (hide-context!)
                             (.preventDefault e))
          :on-click hide-context!
          :style {:position :fixed
                  :top 0
                  :left 0
                  :width "100vw"
                  :height "100vh"}}
         [inner-context-menu menu-atom hide-context!]])])))



;;;;; Main function below

;; Use with a :on-context-menu to activate on right-click

(defn context!
  "Update the context menu with a collection of [name function] pairs.
  When function is nil, consider the button as 'disabled' and do not
  allow any click.  
  When passed a keyword instead of [name function], a divider is
  inserted.
  If a string is passed, convert it into a header.
  [\"Menu header\"
   [my-fn #(+ 1 2)]
   :divider
   [my-other-fn #(prn (str 1 2 3))]]"
  ([evt name-fn-coll] (context! evt default-menu-atom name-fn-coll))
  ([evt menu-atom name-fn-coll]
   (show-context! menu-atom name-fn-coll
                  (- (.-screenX evt) (.-screenX js/window))
                  (- (.-screenY evt) (.-screenY js/window)))
   (.preventDefault evt)))

(defn menu-item
  ([{:keys [name shortcut action submenu] :as item}]
   (if submenu [name (mapv menu-item submenu)]
    (if (= item :devider) :devider
        [[:div.command-row 
          [:span.cmd name]
          [:span.shortcut.muted shortcut]]
         (when action #(rf/dispatch action))
         (when submenu (mapv menu-item submenu))]))))

(defn gen-menu [e menu]
  (context!
   e
   (mapv menu-item menu)))