(ns repath.studio.core
  (:require
   [repath.studio.subs]
   [repath.studio.events]
   [repath.studio.db]
   [repath.studio.keyboard]
   [repath.studio.history.core]
   [repath.studio.tree.core]
   [repath.studio.color.core]
   [repath.studio.attrs.core]
   [repath.studio.tools.core]
   [repath.studio.rulers.core]
   [repath.studio.window.core]
   [repath.studio.documents.core]
   [repath.studio.canvas-frame.core]
   [repath.studio.elements.core]
   [repath.studio.codemirror.core]
   [repath.studio.reepl.core]
   [repath.studio.context-menu.core]
   [repath.studio.reepl.replumb :as replumb]
   [replumb.repl :as repl]
   [repath.studio.views :as views]
   [reagent.dom :as rdom]
   [re-frame.core :as rf]
   [re-pressed.core :as rp]
   [stylefy.core :as stylefy]
   [stylefy.reagent :as stylefy-reagent]
   [repath.user]
   [repath.studio.theme.db :as theme]
   ["@fluentui/react" :as fui]
   [shadow.cljs.bootstrap.browser :as bootstrap]
   [devtools.core :as devtools]))

(def console-easter-egg "
██████╗░███████╗██████╗░░█████╗░████████╗██╗░░██╗
██╔══██╗██╔════╝██╔══██╗██╔══██╗╚══██╔══╝██║░░██║
██████╔╝█████╗░░██████╔╝███████║░░░██║░░░███████║
██╔══██╗██╔══╝░░██╔═══╝░██╔══██║░░░██║░░░██╔══██║
██║░░██║███████╗██║░░░░░██║░░██║░░░██║░░░██║░░██║
╚═╝░░╚═╝╚══════╝╚═╝░░░░░╚═╝░░╚═╝░░░╚═╝░░░╚═╝░░╚═╝
 
░██████╗████████╗██╗░░░██╗██████╗░██╗░█████╗░
██╔════╝╚══██╔══╝██║░░░██║██╔══██╗██║██╔══██╗
╚█████╗░░░░██║░░░██║░░░██║██║░░██║██║██║░░██║
░╚═══██╗░░░██║░░░██║░░░██║██║░░██║██║██║░░██║
██████╔╝░░░██║░░░╚██████╔╝██████╔╝██║╚█████╔╝
╚═════╝░░░░╚═╝░░░░╚═════╝░╚═════╝░╚═╝░╚════╝░")

(defn ^:dev/after-load mount-root []
  (rf/clear-subscription-cache!)
  (let [root-el (.getElementById js/document "app")]
    (rdom/unmount-component-at-node root-el)
    (rdom/render [views/main-panel] root-el)))

(defn bootstrap-cb
  []
  (replumb/run-repl "(in-ns 'repath.user)" identity)
  (replumb/run-repl "(require '[ajax.core :as ajax])" identity)
  (print "Repl initialized"))

(defn init []
  (js/console.log console-easter-egg)
  (devtools/set-pref! :cljs-land-style (str "filter:invert(1);" (:cljs-land-style (devtools/get-prefs))))
  ;; SEE https://code.thheller.com/blog/shadow-cljs/2017/10/14/bootstrap-support.html
  (bootstrap/init repl/st {:path "js/bootstrap" :load-on-init '[repath.user]} bootstrap-cb)
  (fui/loadTheme (fui/createTheme (clj->js (:default theme/themes))))
  (stylefy/init {:dom (stylefy-reagent/init)})
  (rf/dispatch-sync [:initialize-db])
  (rf/dispatch-sync [:document/new])
  (rf/dispatch-sync [::rp/add-keyboard-event-listener "keydown"])
  (.then (.getFonts js/api.fontList #js {:disableQuoting true}) #(rf/dispatch-sync [:set-system-fonts (js->clj %)]))

  (js/window.api.receive "fromMain"
                         (fn [data]
                           (case (.-action data)
                             "fontsLoaded" (js/console.log "fontsLoaded")
                             "windowMaximized" (rf/dispatch [:window/set-maximized? true])
                             "windowUnmaximized" (rf/dispatch [:window/set-maximized? false])
                             "windowEnteredFullscreen" (rf/dispatch [:window/set-fullscreen? true])
                             "windowLeavedFullscreen" (rf/dispatch [:window/set-fullscreen? false])
                             "windowMinimized" (rf/dispatch [:window/set-minimized? true])
                             "windowRestored" (rf/dispatch [:window/set-minimized? false])
                             "windowPainted" (rf/dispatch [:window/set-bitmap-data (.-data data)])
                             "openDocument" (js/console.log (.-data data)))))

  (mount-root))
