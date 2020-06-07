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
(def max-approach-rate 2.0)
(def max-final-approach-rate -0.15)
(def max-rotation-error 0.2)
(def min-rot-impulse-interval 400) ;; ms
(def min-transl-impulse-interval 1500)                       ;; ms
(def max-translation-error 0.21)
(def max-translation-rate 0.04)
(def safezone 15)
(def deadzone 0.3)

(defn x-within-safe-zone?
  "Check if the spaceship is within the safe zone."
  []
  (<= (math/abs (@tel/telem :x)) safezone))

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
   (Thread/sleep min-rot-impulse-interval))
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
   (Thread/sleep min-rot-impulse-interval))
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
   (Thread/sleep min-rot-impulse-interval))
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
                 (> (math/abs (@tel/telem :vx)) max-approach-rate)
                 (and
                  (neg? (@tel/telem :vx))
                  (x-within-safe-zone?)
                  (< (@tel/telem :vx) -0.2)))
            (fill-active driv "q"))))

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
  (let [y (@tel/telem :y)
        vy (@tel/telem :vy)]
    (if (pos? y)
      (when (or (> (math/abs y) deadzone)
                (pos? vy))
        (translate driv "left"))
      (when (or (> (math/abs y) deadzone)
                (neg? vy))
        (translate driv "right")))))

(defn align-z
  "docstring"
  [driv]
  (let [z (@tel/telem :z)
        vz (@tel/telem :vz)]
    (if (pos? z)
      (when (or (> (math/abs z) deadzone)
                (pos? vz))
        (translate driv "down"))
      (when (or (> (math/abs z) deadzone)
                (neg? vz))
        (translate driv "up")))))

(defn kill-pitch-rot
  "Kill pitch rotation."
  [driv]
  (when (not (zero? (@tel/telem :pitch-rate)))
    (if (pos? (@tel/telem :pitch-rate))
      (pitch driv "up" 0.1)
      (pitch driv "down" 0.1))
    (recur driv)))

(defn kill-yaw-rot
  "Kill yaw rotation."
  [driv]
  (when (not (zero? (@tel/telem :yaw-rate)))
    (if (pos? (@tel/telem :yaw-rate))
      (yaw driv "port" 0.1)
      (yaw driv "starboard" 0.1))
    (recur driv)))

(defn kill-roll-rot
  "Kill roll rotation."
  [driv]
  (when (not (zero? (@tel/telem :roll-rate)))
    (if (pos? (@tel/telem :roll-rate))
      (roll driv "left" 0.1)
      (roll driv "right" 0.1))
    (recur driv)))

(defn kill-y-translation
  "Kill y axis translation velocity."
  [driv]
  (let [vy (@tel/telem :vy)]
    (when (not (zero? vy))
      (if (pos? vy)
        (translate driv "left")
        (translate driv "right"))
      (Thread/sleep min-transl-impulse-interval)
      (recur driv))))

(defn kill-z-translation
  "Kill z axis translation velocity."
  [driv]
  (let [vz (@tel/telem :vz)]
    (when (not (zero? vz))
      (if (pos? vz)
        (translate driv "down")
        (translate driv "up"))
      (Thread/sleep min-transl-impulse-interval)
      (recur driv))))

;; Internal functions

(defn pitch-within-error?
  ""
  []
  (<= (math/abs (@tel/telem :pitch)) max-rotation-error))

(defn yaw-within-error?
  ""
  []
  (<= (math/abs (@tel/telem :yaw)) max-rotation-error))

(defn roll-within-error?
  ""
  []
  (<= (math/abs (@tel/telem :roll)) max-rotation-error))

(defn y-within-error?
  ""
  []
  (<= (math/abs (@tel/telem :y)) max-translation-error))

(defn z-within-error?
  ""
  []
  (<= (math/abs (@tel/telem :z)) max-translation-error))

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
  "Perform roll alignment."
  [driv]
  (if (roll-within-error?)
    (kill-roll-rot driv)
    (align-roll driv))
  (recur driv))

(defn align-pitch-rot
  "Perform pitch alignment."
  [driv]
  (if (pitch-within-error?)
    (kill-pitch-rot driv)
    (align-pitch driv))
  (recur driv))

(defn align-yaw-rot
  "Perform yaw alignment."
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
  (Thread/sleep min-transl-impulse-interval)
  (recur driv))

(defn align-z-translation
  "Keep Dragon aligned on z axis."
  [driv]
  (if (z-within-error?)
    (kill-z-translation driv)
    (align-z driv))
  (Thread/sleep min-transl-impulse-interval)
  (recur driv))

(defn accelerate
  "docstring"
  [driv]
  (repeat 20 (translate driv "fwd")))

(defn decellerate
  "docstring"
  [driv]
  (Thread/sleep 1000)
  (if (< (@tel/telem :x) safezone)
    (repeat 18 (translate driv "aft"))
    (recur driv)))