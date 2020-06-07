(ns iss-sim-auto-docking.telemetry_test
  (:require [clojure.test :refer :all]
            [iss-sim-auto-docking.telemetry :refer :all]))

(deftest parse-metric-test
  "Test metric parser with different inputs"
  (testing "Test rate parser"
    (is (= (float 1.3) (parse-metric "1.3 °/s" :roll-rate))))
  (testing "Test range parser"
    (is (= (float -10.2) (parse-metric "-10.2 m" :range))))
  (testing "Test velocity parser"
    (is (= (float 1.4) (parse-metric "1.4 m/s" :vel)))))