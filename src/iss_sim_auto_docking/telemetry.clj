(ns iss-sim-auto-docking.telemetry
  (:require [etaoin.keys :as k]
            [etaoin.api :refer :all])
  (:gen-class)
  )

(defn parse-delta
  [delta]
  (int (clojure.string/replace delta #"°" ""))
  )

(defn parse-rate
  [rate]
  (int (clojure.string/replace rate #" °/s" "")))

(defn parse-range
  [range]
  (int (clojure.string/replace range #" m" "")))

;; Roll

(defn get-roll-delta
  "Get the roll displacement in degrees"
  [driv]
  (let [roll-delta-q (query driv {:css "#roll > div.error"})
        roll-delta (get-element-text-el driv roll-delta-q)]
    (parse-delta roll-delta)))

(defn get-roll-rate
  "Get the roll rate in °/s"
  [driv]
  (let [roll-rate-q (query driv {:css "#roll > div.rate"})
        roll-rate (get-element-text-el driv roll-rate-q)]
    (parse-rate roll-rate)
    )
  )

;; Pitch

(defn get-pitch-delta
  "Get the pitch displacement in degrees"
  [driv]
  (let [pitch-delta-q (query driv {:css "#pitch > div.error"})
        pitch-delta (get-element-text-el driv pitch-delta-q)]
    (parse-delta pitch-delta)
    )
  )

(defn get-pitch-rate
  "docstring"
  [driv]
  (let [pitch-rate-q (query driv {:css "#pitch > div.rate"})
        pitch-rate (get-element-text-el driv pitch-rate-q)]
    (parse-rate pitch-rate)
    )
  )

;; Yaw

(defn get-yaw-delta
  ""
  [driv]
  (let [yaw-delta-q (query driv {:css "#yaw > div.error"})
        yaw-delta (get-element-text-el driv yaw-delta-q)]
    (parse-delta yaw-delta)
    ))

(defn get-yaw-rate
  ""
  [driv]
  (let [yaw-rate-q (query driv {:css "#yaw > div.rate"})
        yaw-rate (get-element-text-el driv yaw-rate-q)]
    (parse-delta yaw-rate)
    )
  )

(defn get-range
  "Get the Euclidian distance to the ISS in meters."
  [driv]
  (let [range-q (query driv {:css "#range > div.rate"})
        range (get-element-text-el driv range-q)]
    (parse-range range)
    )
  )