;;; Tool to convert Audiveris' training set files (in XML) to bitmaps,
;;; usable by overscore

(ns overscore.tools.audiveris
  (:use [overscore.musicxml :only [parse-int]])
  (:require [clojure.xml :as xml])
  (:import (java.awt.image BufferedImage)
           (javax.imageio ImageIO)
           (java.io File)))

(defrecord run [x y length])

(def black 0x000000)
(def white 0xFFFFFF)

(defn extract-run
  "Extract one run from its XML representation"
  [run first-pos]
  (->run first-pos
         (parse-int (:start (:attrs run)))
         (parse-int (:length (:attrs run)))))

(defn extract-runs-from-section
  "Extract all the runs of an XML representation of a section"
  [section]
  (let [first-pos (parse-int (:first-pos (:attrs section)))]
    (map #(extract-run %1 (+ first-pos %2))
         (:content section)
         (iterate inc 0))))

(defn extract-all-runs
  "Extract all the runs from a XML file"
  [xml]
  (reduce (fn [acc sec] (concat acc (extract-runs-from-section sec)))
          '()
          (filter #(= (:tag %) :section)
                  (:content xml))))

(defn draw-run
  "Draw a run in an image"
  [run img]
  (doseq [y (range (:y run) (+ (:y run) (:length run)))]
    (.setRGB img (:x run) y black)))

(defn draw-runs
  "Draw all the runs from an XML representation into an image"
  [runs img]
  (doseq [run runs]
    (draw-run run img)))

(defn adjust-run
  "Adjust the start position of a run"
  [x y run]
  (->run (- (:x run) x)
         (- (:y run) y)
         (:length run)))

(defn fill
  "Fill an image with a certain color"
  [img color]
  (doseq [x (range (.getWidth img))
          y (range (.getHeight img))]
    (.setRGB img x y color)))

(defn to-image
  "Convert an XML representation to an image"
  [in out]
  (let [xml (xml/parse in)
        original-runs (extract-all-runs xml)
        xs (map :x (sort #(compare (:x %1) (:x %2)) original-runs))
        x (first xs)
        width (+ 1 (- (last xs) x))
        y (:y (first (sort #(compare (:y %1) (:y %2)) original-runs)))
        last-run (last (sort #(compare (+ (:y %1) (:length %1))
                                     (+ (:y %2) (:length %2)))
                           original-runs))
        height (+ 1 (- (+ (:y last-run) (:length last-run)) y))
        runs (map #(adjust-run x y %) original-runs)
        img (BufferedImage. width height BufferedImage/TYPE_BYTE_BINARY)]
    (fill img white)
    (draw-runs runs img)
    (ImageIO/write img "png" (File. out))))
