(ns renderer.core
  (:require
   [renderer.subs]
   [renderer.events]
   [renderer.effects]
   [renderer.db]
   [renderer.utils.keyboard :as keyboard]
   [renderer.history.core]
   [renderer.tools.core]
   [renderer.rulers.core]
   [renderer.window.core]
   [renderer.document.core]
   [renderer.frame.core]
   [renderer.elements.core]
   [renderer.reepl.core]
   [renderer.attribute.core]
   [renderer.reepl.replumb :as replumb]
   [replumb.repl :as repl]
   [renderer.views :as views]
   [reagent.dom :as rdom]
   [re-frame.core :as rf]
   [re-pressed.core :as rp]
   [shadow.cljs.bootstrap.browser :as bootstrap]
   [devtools.core :as devtools]
   [renderer.utils.error :as error]
   ["@sentry/electron/renderer" :as sentry-electron-renderer]
   ["@sentry/react" :as sentry-react]
   ["paper" :refer [paper]]
   [user]
   [config]
   [platform]))

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
    (rdom/render [error/boundary [views/main-panel]] root-el)))

(defn bootstrap-cb
  []
  (replumb/run-repl "(in-ns 'user)" identity)
  (print "Repl initialized"))

#_:clj-kondo/ignore
(defn init []
  (if platform/electron?
    (sentry-electron-renderer/init (clj->js config/sentry-options) sentry-react/init)
    (sentry-react/init (clj->js config/sentry-options)))

  (js/console.log (str "%c" console-easter-egg) "color: #bada55")

  (devtools/set-pref!
   :cljs-land-style
   (str "filter:invert(1);" (:cljs-land-style (devtools/get-prefs))))

  ;; SEE https://code.thheller.com/blog/shadow-cljs/2017/10/14/bootstrap-support.html
  (bootstrap/init repl/st {:path "js/bootstrap" :load-on-init '[user]} bootstrap-cb)

  (rf/dispatch-sync [:initialize-db])
  (rf/dispatch-sync [:document/new])

  (rf/dispatch-sync [::rp/add-keyboard-event-listener "keydown"])
  (rf/dispatch-sync [::rp/set-keydown-rules keyboard/keydown-rules])

  (.addEventListener js/window.document "keydown" keyboard/event-handler)
  (.addEventListener js/window.document "keyup" keyboard/event-handler)

  (.setup paper)

  (when platform/electron?
    (rf/dispatch-sync [:set-system-fonts (js->clj js/window.api.systemFonts  :keywordize-keys true)])
    (.then (js/window.api.webrefCss.listAll)
           #(rf/dispatch-sync [:set-webref-css (js->clj % :keywordize-keys true)]))
    (rf/dispatch-sync [:set-mdn (js->clj js/window.api.mdn :keywordize-keys true)])
    (js/window.api.receive
     "fromMain"
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
         "openDocument" (js/console.log (.-data data))))))

  (mount-root))
