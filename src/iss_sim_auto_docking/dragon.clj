(ns iss-sim-auto-docking.dragon
  (:require [etaoin.keys :as k]
            [etaoin.api :refer :all]
            [clojure.math.numeric-tower :as math]
            [iss-sim-auto-docking.telemetry :as tel])
  (:gen-class))

(def comma ",")
(def point ".")

;; thresholds
(def max-def-rot-rate 0.1)
(def max-approach-rate 0.5)
(def max-final-approach-rate 0.2)
(def max-rotation-error 0.1)

(defn roll
  "Roll the Crew Dragon left or right (CCW or CW)."
  [driv dir]
  (if (< (math/abs (@tel/telem :roll-rate)) max-def-rot-rate)
    (case dir
      "left" (fill-active driv comma)
      "right" (fill-active driv point))))

(defn pitch
  "Pitch the Crew Dragon up or down."
  ([driv dir max-rot-rate]
  (if (or (< (math/abs (@tel/telem :pitch-rate)) max-rot-rate)
          (and
            (pos? (@tel/telem :pitch-rate)) (= dir "up"))
          (and
            (neg? (@tel/telem :pitch-rate)) (= dir "down"))
          )
    (case dir
      "up" (fill-active driv k/arrow-up)
      "down" (fill-active driv k/arrow-down))))
  ([driv dir]
   (pitch driv dir max-def-rot-rate))
  )

(defn yaw
  "Yaw the Crew Dragon port or starboard."
  ([driv dir max-rot-rate]
  (if (or (< (math/abs (@tel/telem :yaw-rate)) max-rot-rate)
          (and
            (neg? (@tel/telem :yaw-rate)) (= dir "starboard"))
          (and
            (pos? (@tel/telem :yaw-rate)) (= dir "port"))
          ) ;;; just fucking repeat this everywhere else
    (case dir
      "port" (fill-active driv k/arrow-left)                  ;; left
      "starboard" (fill-active driv k/arrow-right)            ;; right
)))
  ([driv dir]
   (yaw driv dir max-def-rot-rate)
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
))

(defn align-pitch
  [driv]
  (if (neg? (@tel/telem :pitch))
    (pitch driv "up")
    (pitch driv "down")))

(defn align-yaw
  "docstring"
  [driv]
  (if (neg? (@tel/telem :yaw))
    (yaw driv "port")
    (yaw driv "starboard")))

(defn kill-pitch-rot
  "docstring"
  [driv]
  (if (pos? (@tel/telem :pitch-rate))
    (pitch driv "up" 0.1)
    (pitch driv "down" 0.1)))

(defn kill-yaw-rot
  "docstring"
  [driv]
  (if (pos? (@tel/telem :yaw-rate))
    (yaw driv "port" 0.1)
    (yaw driv "starboard" 0.1)))

(defn kill-rot
  "Kill all Dragon rotation"
  [driv]
  (println "KILL ROT")
  (when (not (or
             (zero? (@tel/telem :pitch-rate))
             (zero? (@tel/telem :yaw-rate))
             (zero? (@tel/telem :roll-rate))))

    (kill-pitch-rot driv)
    (kill-yaw-rot driv)
    (recur driv)
    )
  )

(defn pitch-within-error?
  "docstring"
  []
  (<= (math/abs (@tel/telem :pitch)) max-rotation-error))

(defn yaw-within-error?
  "docstring"
  []
  (<= (math/abs (@tel/telem :yaw)) max-rotation-error))

(defn align-rot
  "Align rotation of Dragon to match docking port."
  [driv]
  (Thread/sleep 200)    ;; min interval between RCS impulses
  (println @tel/telem)

  (when
    (> (math/abs (@tel/telem :pitch)) max-rotation-error)
    (align-pitch driv))
  (when
    (> (math/abs (@tel/telem :yaw)) max-rotation-error)
    (align-yaw driv))

  (if (not (and
          (pitch-within-error?)
          (yaw-within-error?)))
    (do
      (recur driv)
      )
    )
  )

