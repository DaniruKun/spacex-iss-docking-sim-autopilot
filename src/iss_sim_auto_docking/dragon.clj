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
  ([driv dir max-roll-rate]
  (if (or
      (< (math/abs (@tel/telem :roll-rate)) max-roll-rate)
      (and
        (pos? (@tel/telem :roll-rate)) (= dir "left")
        )
      (and
        (neg? (@tel/telem :roll-rate)) (= dir "right"))
      )

    (case dir
      "left" (fill-active driv comma)
      "right" (fill-active driv point))))
  ([driv dir]
   (roll driv dir max-def-rot-rate))
  )

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

(defn align-roll
  "docstring"
  [driv]
  (if (neg? (@tel/telem :roll))
    (roll driv "left")
    (roll driv "right")))

(defn kill-pitch-rot
  "docstring"
  [driv]
  (Thread/sleep 100)
  (when (not (zero? (@tel/telem :pitch-rate)))
    (if (pos? (@tel/telem :pitch-rate))
      (pitch driv "up" 0.1)
      (pitch driv "down" 0.1))
    (recur driv)
    )
  )

(defn kill-yaw-rot
  "docstring"
  [driv]
  (Thread/sleep 100)
  (when (not (zero? (@tel/telem :yaw-rate)))
    (if (pos? (@tel/telem :yaw-rate))
      (yaw driv "port" 0.1)
      (yaw driv "starboard" 0.1))
    (recur driv)
    )
  )

(defn kill-roll-rot
  "docstring"
  [driv]
  (Thread/sleep 100)
  (when (not (zero? (@tel/telem :roll-rate)))
    (if (pos? (@tel/telem :roll-rate))
      (roll driv "left" 0.1)
      (roll driv "right" 0.1))
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

(defn roll-within-error?
  ""
  []
  (<= (math/abs (@tel/telem :roll)) max-rotation-error))


(defn align-rot
  "Align rotation of Dragon to match docking port."
  [driv]
  (Thread/sleep 200)    ;; min interval between RCS impulses
  (println @tel/telem)

  (if (roll-within-error?)
    (kill-roll-rot driv)
    (align-roll driv))

  (if (pitch-within-error?)
    (kill-pitch-rot driv)
    (align-pitch driv))

  (if (yaw-within-error?)
    (kill-yaw-rot driv)
    (align-yaw driv))

  (when (not (and
               (roll-within-error?)
               (pitch-within-error?)
               (yaw-within-error?)))
    (recur driv)
    )
  )

