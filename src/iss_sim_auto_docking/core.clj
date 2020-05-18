(ns iss-sim-auto-docking.core
  (:require
            [etaoin.api :refer :all]
            [iss-sim-auto-docking.dragon :as dragon]
            [iss-sim-auto-docking.telemetry :as tel])
  (:gen-class))

(def sim-website-url "https://iss-sim.spacex.com")

;; Button selectors
(def begin-button {:id :begin-button})

(defn setup-sim
  "Setup the simulator."
  [driv]
  (println "Setting up the simulator...")
  (doto driv
    (set-window-size 1200 800)
    (go sim-website-url)
    (wait-visible begin-button {:timeout 30})
    (click begin-button)
    (wait 12))
  (println "Simulator started."))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Starting up webdriver...")
  (with-chrome {} chr
    ;; Setup the simulator
    (setup-sim chr)
    (println "Started telemetry poller")
    (future (tel/poll chr))
    (Thread/sleep 2000)
    ;; concurrent futures for each control axis
    (future (dragon/align-roll-rot chr))
    (future (dragon/align-pitch-rot chr))
    (future (dragon/align-yaw-rot chr))

    (Thread/sleep 120000)
    )
  (System/exit 0)
  )
