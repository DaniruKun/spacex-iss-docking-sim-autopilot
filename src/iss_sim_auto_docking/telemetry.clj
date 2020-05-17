(ns iss-sim-auto-docking.telemetry
  (:require [clojure.string :as str]
            [etaoin.keys :as k]
            [etaoin.api :refer :all])
  (:gen-class)
  )

(def telem (atom {
                  :x 0.0
                  :y 0.0
                  :z 0.0
                  :roll 0.0
                  :pitch 0.0
                  :yaw 0.0
                  :roll-rate 0.0
                  :pitch-rate 0.0
                  :yaw-rate 0.0
                  }))


(def deg (new String "°"))
(def emptystr (new String ""))

;; Internal functions

(defn parse-delta
  [delta]
  (-> delta
      (.split deg)
      first
      Float/parseFloat)
  )

(defn parse-rate
  [rate]
  (Float/parseFloat (str/replace rate #" °/s" emptystr)))

(defn parse-range
  [range]
  (Float/parseFloat (str/replace range #" m" emptystr)))

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


(defn poller
  "Poll telemetry and update the state until dist to station is zero."
  [driv]
  (if true
    (do
      (swap! telem assoc
             :roll (get-roll-delta driv)
             :pitch (get-pitch-delta driv)
             :yaw (get-yaw-delta driv)
             :roll-rate (get-roll-rate driv)
             :pitch-rate (get-pitch-rate driv)
             :yaw-rate (get-yaw-rate driv)
             )
      (Thread/sleep 50)
      (recur driv)
      )
    )
  )