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
(def max-approach-rate -2.0)
(def max-final-approach-rate -0.18)
(def max-rotation-error 0.2)
(def min-rot-impulse-interval 400) ;; ms
(def min-transl-impulse-interval 800)                       ;; ms
(def max-translation-error 0.2)
(def max-translation-rate 0.10)
(def safezone 10)

;; RCS control functions

(defn roll
  "Roll the Crew Dragon left or right (CCW or CW)."
  ([driv dir max-roll-rate]
   (if (or
        (< (math/abs (@tel/telem :roll-rate)) max-roll-rate)
        (and
         (pos? (@tel/telem :roll-rate)) (= dir "left"))
        (and
         (neg? (@tel/telem :roll-rate)) (= dir "right")))

     (case dir
       "left" (fill-active driv comma)
       "right" (fill-active driv point)))
   (Thread/sleep min-rot-impulse-interval)
   )
  ([driv dir]
   (roll driv dir max-def-rot-rate)))

(defn pitch
  "Pitch the Crew Dragon up or down."
  ([driv dir max-rot-rate]
   (if (or (< (math/abs (@tel/telem :pitch-rate)) max-rot-rate)
           (and
            (pos? (@tel/telem :pitch-rate)) (= dir "up"))
           (and
            (neg? (@tel/telem :pitch-rate)) (= dir "down")))
     (case dir
       "up" (fill-active driv k/arrow-up)
       "down" (fill-active driv k/arrow-down)))
   (Thread/sleep min-rot-impulse-interval)
   )
  ([driv dir]
   (pitch driv dir max-def-rot-rate)))

(defn yaw
  "Yaw the Crew Dragon port or starboard."
  ([driv dir max-rot-rate]
   (if (or (< (math/abs (@tel/telem :yaw-rate)) max-rot-rate)
           (and
            (neg? (@tel/telem :yaw-rate)) (= dir "starboard"))
           (and
            (pos? (@tel/telem :yaw-rate)) (= dir "port")))
     (case dir
       "port" (fill-active driv k/arrow-left)                  ;; left
       "starboard" (fill-active driv k/arrow-right)            ;; right
))
   (Thread/sleep min-rot-impulse-interval)
   )
  ([driv dir]
   (yaw driv dir max-def-rot-rate)))

(defn translate
  "Translate the Crew Dragon in 6 DoF."
  [driv dir]
  (case dir
    "up" (when (or
                (< (math/abs (@tel/telem :vz)) max-translation-rate)
                (neg? (@tel/telem :vz)))
           (fill-active driv "w"))
    "down" (when (or
                  (< (math/abs (@tel/telem :vz)) max-translation-rate)
                  (pos? (@tel/telem :vz)))
             (fill-active driv "s"))
    "left" (when (or
                  (< (math/abs (@tel/telem :vy)) max-translation-rate)
                  (pos? (@tel/telem :vy)))
             (fill-active driv "a"))
    "right" (when (or
                   (< (math/abs (@tel/telem :vy)) max-translation-rate)
                   (neg? (@tel/telem :vy)))
              (fill-active driv "d"))
    "fwd" (when (or
                 (< (math/abs (@tel/telem :vx)) max-approach-rate)
                 (pos? (@tel/telem :vx)))
            (fill-active driv "e"))
    "aft" (when (or
                 (< (math/abs (@tel/telem :vx)) max-approach-rate)
                 (neg? (@tel/telem :vx)))
            (fill-active driv "q")))
  (Thread/sleep min-transl-impulse-interval)
  )

;; Alignment functions

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

(defn align-y
  "docstring"
  [driv]
  (if (pos? (@tel/telem :y))
    (translate driv "left")
    (translate driv "right"))
  )

(defn align-z
  "docstring"
  [driv]
  (if (pos? (@tel/telem :z))
    (translate driv "down")
    (translate driv "up"))
  )

(defn kill-pitch-rot
  "docstring"
  [driv]
  (when (not (zero? (@tel/telem :pitch-rate)))
    (if (pos? (@tel/telem :pitch-rate))
      (pitch driv "up" 0.1)
      (pitch driv "down" 0.1))
    (recur driv)))

(defn kill-yaw-rot
  "docstring"
  [driv]
  (when (not (zero? (@tel/telem :yaw-rate)))
    (if (pos? (@tel/telem :yaw-rate))
      (yaw driv "port" 0.1)
      (yaw driv "starboard" 0.1))
    (recur driv)))

(defn kill-roll-rot
  "docstring"
  [driv]
  (when (not (zero? (@tel/telem :roll-rate)))
    (if (pos? (@tel/telem :roll-rate))
      (roll driv "left" 0.1)
      (roll driv "right" 0.1))
    (recur driv)))

(defn kill-y-translation
  "docstring"
  [driv]
  (let [vy (@tel/telem :vy)]
    (when (not (zero? vy))
      (if (pos? vy)
        (translate driv "left")
        (translate driv "right"))
      (recur driv))))

(defn kill-z-translation
  ""
  [driv]
  (let [vz (@tel/telem :vz)]
    (when (not (zero? vz))
      (if (pos? vz)
        (translate driv "down")
        (translate driv "up"))
      (recur driv))))

(defn slowdown-under-limit
  "docstring"
  [driv max-rate]
  (let [vx (@tel/telem :vx)]
    (when (> vx max-rate)
      (translate driv "aft")
      (recur driv max-rate))
    )
  )

;; Internal functions

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

(defn y-within-error?
  "docstring"
  []
  (<= (math/abs (@tel/telem :y)) max-translation-error))

(defn z-within-error?
  "docstring"
  []
  (<= (math/abs (@tel/telem :z)) max-translation-error))

(defn x-within-safe-zone?
  "Check if the spaceship is within the safe zone."
  []
  (<= (math/abs (@tel/telem :x)) safezone)
  )

(defn wait-rotation-stopped
  "Keep polling telemetry until rotation stop has been confirmed."
  []
  (if (and
       (zero? (@tel/telem :roll-rate))
       (zero? (@tel/telem :pitch-rate))
       (zero? (@tel/telem :yaw-rate)))
    true
    (recur)))

;; Public functions

; Rotation alignment

(defn align-roll-rot
  "docstring"
  [driv]
  (if (roll-within-error?)
    (kill-roll-rot driv)
    (align-roll driv))
  (recur driv))

(defn align-pitch-rot
  "docstring"
  [driv]
  (if (pitch-within-error?)
    (kill-pitch-rot driv)
    (align-pitch driv))
  (recur driv))

(defn align-yaw-rot
  "docstring"
  [driv]
  (if (yaw-within-error?)
    (kill-yaw-rot driv)
    (align-yaw driv))
  (recur driv))

; Translation alignment

(defn align-y-translation
  "Keep Dragon aligned on y axis."
  [driv]
  (if (y-within-error?)
    (kill-y-translation driv)
    (align-y driv))
  (Thread/sleep 1000)
  (recur driv))

(defn align-z-translation
  "Keep Dragon aligned on z axis."
  [driv]
  (if (z-within-error?)
    (kill-z-translation driv)
    (align-z driv))
  (Thread/sleep 1000)
  (recur driv))

(defn approach
  "Perform controlled approach on x axis."
  [driv]
  (if (x-within-safe-zone?)
    (slowdown-under-limit driv 0.18)
    )
  (recur driv)
  )