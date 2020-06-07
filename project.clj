(defproject iss-sim-auto-docking "0.2.0"
  :description "This app does automatic docking in the ISS SpaceX simulator."
  :url "https://github.com/DaniruKun/spacex-iss-docking-sim-autopilot"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [etaoin "0.3.6"]
                 [org.clojure/math.numeric-tower "0.0.4"]]
  :main ^:skip-aot iss-sim-auto-docking.core
  :target-path "target/%s"
  :jar-name "iss-sim-auto-docking.jar"
  :uberjar-name "iss-sim-auto-docking-standalone.jar"
  :min-lein-version "2.0.0"
  :repl-options {:init-ns iss-sim-auto-docking.core
                 :init (do
                         (use 'etaoin.api)
                         (require '[etaoin.keys :as k])
                         (require '[iss-sim-auto-docking.dragon :as dragon])
                         (require '[iss-sim-auto-docking.telemetry :as tel])
                         (require '[iss-sim-auto-docking.calc :as calc]))}
  :profiles {:uberjar {:aot :all}
             :dev {:plugins [[lein-cljfmt "0.5.7"]]}})
