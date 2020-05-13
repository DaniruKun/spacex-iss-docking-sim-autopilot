(ns iss-sim-auto-docking.dragon
  (:require [etaoin.keys :as k]
            [etaoin.api :refer :all])
  (:gen-class))

(def comma ",")
(def point ".")

(defn roll
  "Roll the Crew Dragon left or right (CCW or CW)."
  [driv dir]
  (case dir
    "left" (fill-active driv comma)
    "right" (fill-active driv point)
    )
  )

(defn pitch
  "Pitch the Crew Dragon up or down."
  [driv dir]
  (case dir
    "up" (fill-active driv k/arrow-up)
    "down" (fill-active driv k/arrow-down)
    )
  )


(defn yaw
  "Yaw the Crew Dragon port or starboard."
  [driv dir]
  (case dir
    "port" (fill-active driv k/arrow-left)                  ;; left
    "starboard" (fill-active driv k/arrow-right)            ;; right
    )
  )

(defn translate
  "Translate the Crew Dragon in 6 DoF."
  [driv dir]
  (case dir
    "up" (fill-active driv "w")
    "down" (fill-active driv "s")
    "left" (fill-active driv "a")
    "right" (fill-active driv "d")
    "fwd" (fill-active driv "e")                            ;; forward
    "aft" (fill-active driv "q")                            ;; back
    )
  )