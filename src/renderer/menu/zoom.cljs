(ns renderer.menu.zoom)

(def menu
  [{:label "Set zoom to 50%"
    :key "50"
    :action [:set-zoom 0.5]}
   {:label "Set zoom to 100%"
    :key "100"
    :action [:set-zoom 1]}
   {:label "Set zoom to 200%"
    :key "200"
    :action [:set-zoom 2]}
   {:key :divider-1
    :type :separator}
   {:label "Restore zoom and pan"
    :key "restore-active-page"
    :action [:pan-to-active-page :original]}
   {:label "Zoom to fit page"
    :key "fit-active-page"
    :action [:pan-to-active-page :fit]}
   {:label "Zoom to fill page"
    :key "fill-active-page"
    :action [:pan-to-active-page :fill]}])