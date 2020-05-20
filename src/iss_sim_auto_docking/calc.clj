(ns iss-sim-auto-docking.calc
  (:gen-class))

(defn get-vx
  "Calculate velocity vector x component."
  [dt dx]
  (float (/ dx dt)))

(defn get-vy
  "Calculate velocity vector y component."
  [dt dy]
  (float (/ dy dt)))

(defn get-vz
  "Calculate velocity vector z component."
  [dt dz]
  (float (/ dz dt)))

