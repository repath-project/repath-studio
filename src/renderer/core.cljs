(ns renderer.core
  (:require
   #_["@sentry/electron/renderer" :as sentry-electron-renderer]
   #_["@sentry/react" :as sentry-react]
   ["paper" :refer [paper]]
   [config]
   [devtools.core :as devtools]
   [platform]
   [re-frame.core :as rf]
   [re-pressed.core :as rp]
   [reagent.dom :as rdom]
   [renderer.attribute.core]
   [renderer.cmdk.core]
   [renderer.db]
   [renderer.document.core]
   [renderer.effects]
   [renderer.element.core]
   [renderer.events]
   [renderer.frame.core]
   [renderer.history.core]
   [renderer.menubar.core]
   [renderer.notification.core]
   [renderer.panel.core]
   [renderer.reepl.core]
   [renderer.reepl.replumb :as replumb]
   [renderer.rulers.core]
   [renderer.subs]
   [renderer.theme.core]
   [renderer.tools.core]
   [renderer.tree.core]
   [renderer.utils.error :as error]
   [renderer.utils.keyboard :as keyboard]
   [renderer.views :as views]
   [renderer.window.core]
   [replumb.repl :as repl]
   [shadow.cljs.bootstrap.browser :as bootstrap]
   [user]))

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

(defn ^:export init []
  #_(if platform/electron?
      (sentry-electron-renderer/init (clj->js config/sentry-options) sentry-react/init)
      (sentry-react/init (clj->js config/sentry-options)))

  (js/console.log (str "%c" console-easter-egg) "color: #e93976")

  (devtools/set-pref!
   :cljs-land-style
   (str "filter:invert(1);" (:cljs-land-style (devtools/get-prefs))))

  ;; https://code.thheller.com/blog/shadow-cljs/2017/10/14/bootstrap-support.html
  (bootstrap/init repl/st {:path "js/bootstrap" :load-on-init '[user]} bootstrap-cb)

  (rf/dispatch-sync [:initialize-db])
  (rf/dispatch-sync [:load-local-db])
  (rf/dispatch-sync [:theme/init-mode])
  (rf/dispatch-sync [:document/new])

  (rf/dispatch-sync [::rp/add-keyboard-event-listener "keydown"])
  (rf/dispatch-sync [::rp/set-keydown-rules keyboard/keydown-rules])

  (.addEventListener js/document "keydown" keyboard/event-handler)
  (.addEventListener js/document "keyup" keyboard/event-handler)

  (.setup paper)

  (if platform/electron?
    (do (let [fonts (js->clj js/window.api.systemFonts :keywordize-keys true)]
          (rf/dispatch-sync [:set-system-fonts fonts]))
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
             "openDocument" (js/console.log (.-data data))))))
    (.addEventListener js/document
                       "fullscreenchange"
                       #(rf/dispatch [:window/set-fullscreen? (boolean (.-fullscreenElement js/document))])))

  (mount-root))
