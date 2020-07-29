(ns iss-sim-auto-docking.telemetry
  (:require [iss-sim-auto-docking.calc :as calc]
            [clojure.string :as str]
            [etaoin.keys :as k]
            [etaoin.api :refer :all]
            [clojure.math.numeric-tower :as math])
  (:gen-class))

(def telem (atom {:x 200
                  :y 12
                  :z 30
                  :t (System/currentTimeMillis)}))

(def deg (new String "°"))
(def emptystr (new String ""))

(def poll-interval 150) ;; ms
;; Internal functions

(defn parse-delta
  [delta]
  (-> delta
      (.split deg)
      first
      Float/parseFloat))

(defn parse-metric
  "Parse a telemetry metric"
  [metric metric-type]
  (Float/parseFloat (str/replace metric (case metric-type
                                          :roll-rate #" °/s"
                                          :range #" m"
                                          :vel #" m/s") emptystr)))

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
    (parse-metric roll-rate :roll-rate)))

;; Pitch

(defn get-pitch-delta
  "Get the pitch displacement in degrees"
  [driv]
  (let [pitch-delta-q (query driv {:css "#pitch > div.error"})
        pitch-delta (get-element-text-el driv pitch-delta-q)]
    (parse-delta pitch-delta)))

(defn get-pitch-rate
  "docstring"
  [driv]
  (let [pitch-rate-q (query driv {:css "#pitch > div.rate"})
        pitch-rate (get-element-text-el driv pitch-rate-q)]
    (parse-metric pitch-rate :roll-rate)))

;; Yaw

(defn get-yaw-delta
  ""
  [driv]
  (let [yaw-delta-q (query driv {:css "#yaw > div.error"})
        yaw-delta (get-element-text-el driv yaw-delta-q)]
    (parse-delta yaw-delta)))

(defn get-yaw-rate
  ""
  [driv]
  (let [yaw-rate-q (query driv {:css "#yaw > div.rate"})
        yaw-rate (get-element-text-el driv yaw-rate-q)]
    (parse-delta yaw-rate)))

;; Distance

(defn get-range
  "Get the Euclidian distance to the ISS in meters."
  [driv]
  (let [range (->> {:css "#range > div.rate"}
                   (query driv)
                   (get-element-text-el driv))]
    (parse-metric range :range)))

(defn get-dist
  "Get the distance to ISS on a given axis."
  [driv axis]
  (let [sel (str "#" axis "-range > div")
        dist (->> {:css sel}
                  (query driv)
                  (get-element-text-el driv))]
    (parse-metric dist :range)))

(defn get-approach-rate
  "Get the approach rate relative to ISS."
  [driv]
  (let [v (->> {:css "#rate > div.rate"}
               (query driv)
               (get-element-text-el driv))]
    (-> v
        (parse-metric :vel)
        (math/abs))))

(defn poll
  "Poll telemetry and update the state."
  [driv]
  (if true                                        ;; TODO add proper termination of telemetry poller by checking if driver is active
    (do
      (Thread/sleep poll-interval)
      (let [t (System/currentTimeMillis)
            dt (* (+ poll-interval (- t (@telem :t))) 0.001)
            x (get-dist driv "x")
            y (get-dist driv "y")
            z (get-dist driv "z")

            dx (- x (@telem :x))
            dy (- y (@telem :y))
            dz (- z (@telem :z))

            vx (calc/get-vx dt dx)
            vy (calc/get-vy dt dy)
            vz (calc/get-vz dt dz)]

        (swap! telem assoc
               :roll (get-roll-delta driv)
               :pitch (get-pitch-delta driv)
               :yaw (get-yaw-delta driv)

               :roll-rate (get-roll-rate driv)
               :pitch-rate (get-pitch-rate driv)
               :yaw-rate (get-yaw-rate driv)

               :range (get-range driv)

               :x x
               :y y
               :z z

               :vx vx
               :vy vy
               :vz vz

               :t (System/currentTimeMillis)))

      (recur driv))))
