(ns iss-sim-auto-docking.core
  (:require [etaoin.keys :as k]
            [etaoin.api :refer :all]
            [iss-sim-auto-docking.dragon :as dragon])
  (:gen-class))

(def sim-website-url "https://iss-sim.spacex.com")

;; Button selectors
(def begin-button {:id :begin-button})

(defn setup-sim
  "Setup the simulator."
  [driv]
  (println "Setting up the simulator...")
  (doto driv
    (go sim-website-url)
    (wait-visible begin-button)
    (click begin-button)
    (wait 10))
  (println "Simulator started."))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Starting up webdriver...")
  (with-chrome {} chr
      ;; Setup the simulator
    (setup-sim chr)))
