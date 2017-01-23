(ns clojure-spec-meetup-talk.core
  (:require [clojure.spec :as spec]
            [clojure.spec.gen :as gen]
            [clojure.string :as str]
            [com.gfredericks.test.chuck.generators :as genc]
            ))

;; First up, validation with predicates
;; (spec/valid? <predicates> 10)
(spec/valid? even? 10)
(spec/valid? #(> (count %) 0) "2017-01-01T01:01:01Z")

; Defining a spec. Specs for GeoJSON http://geojson.org
;{
;"type": "Feature",
;"geometry": {
;             "type": "Point",
;                   "coordinates": [125.6, 10.1]
;             },
;"properties": {
;               "name": "Dinagat Islands"
;               }
;}
(spec/def ::longitude double?)
(spec/def ::latitude double?)
(spec/def :wgs/coordinates (spec/tuple ::longitude ::latitude))
(spec/valid? :wgs/coordinates [1.0 2.0])

(spec/explain :wgs/coordinates [1.8 2.8])
(def test-val [:test 2.8])
(spec/explain :wgs/coordinates test-val)
(spec/explain-str :wgs/coordinates test-val)
(spec/explain-data :wgs/coordinates test-val)
(spec/describe :wgs/coordinates)
(spec/* :wgs/coordinates)

(spec/def :cart/coordinates (spec/cat :x ::longitude :y ::latitude))

(gen/generate (spec/gen :wgs/coordinates)) ; vec
(type (clojure.spec.gen/generate (spec/gen :wgs/coordinates))) ; vec
(type (clojure.spec.gen/generate (spec/gen :cart/coordinates))) ; seq

(spec/def ::type #{"Point"})

(spec/def ::geometry (spec/keys :req-un [::type :wgs/coordinates]))

(gen/generate (spec/gen ::geometry))

(gen/sample (spec/gen ::geometry))

(spec/def ::longitude (spec/double-in :min -180.0 :max 180 :NaN false :infinite? false))
(spec/def ::latitude (spec/double-in :min -90.0 :max 90 :NaN false :infinite? false))
(spec/def ::coordinates (spec/tuple ::longitude ::latitude))
(spec/def ::geometry (spec/keys :req-un [::type ::coordinates]))

(gen/sample (spec/gen ::geometry))

(spec/def :feature/type #{"Feature"})
(spec/def ::properties map?)

(spec/def ::feature (spec/keys :req-un [:feature/type ::properties]))
(gen/sample (spec/gen ::feature))

(spec/def ::properties (spec/map-of string? (spec/or :s string? :n number? :m map? :c coll?)))

(spec/def ::jsonobj (spec/map-of string? (spec/or :s string? :n number? :m ::jsonobj :c (spec/coll-of ::jsonobj))))

;(spec/def ::properties ::jsonobj) ; pegging the CPU time
(spec/def ::id (spec/or :p pos-int? :s (spec/and string? #(not (str/blank? %)))))
(spec/def ::feature (spec/keys :req-un [::id :feature/type ::properties]))

(gen/sample (spec/gen ::feature))

; namespace point
(spec/def :pt/type #{"Point"})
(spec/def :pt/geometry (spec/keys :req-un [:pt/type ::coordinates]))
(spec/def :pt/feature (spec/keys :req-un [::id :pt/type :pt/geometry ::properties]))
; namespace poly
(spec/def :poly/type #{"Polygon"})

(defn circle-gen [x y]
  (let [vertices (+ (rand-int 8) 4)
        radius (rand 3) ;2 dec degrees radius length
        rads (/ (* 2.0 Math/PI) vertices)
        pts (map (fn [r]
                   [(+ x (* radius (Math/cos (* r rads))))
                    (+ y (* radius (Math/sin (* rads r))))])
                 (range vertices))]
    (conj pts (last pts))))
(spec/def :poly/coordinates (spec/with-gen
                           coll?
                           #(gen/fmap (fn [[lon lat]] (list (circle-gen lon lat)))
                                      (gen/tuple (spec/gen ::longitude) (spec/gen ::latitude)))))
(spec/def :poly/geometry (spec/keys :req [:poly/type :poly/coordinates]))
(spec/def :poly/feature (spec/keys :req-un [::id :poly/geometry :poly/type ::properties]))

(spec/def :feat/geometry (spec/or :poly/feature :pt/feature))
(spec/def ::feature (spec/keys :req-un [::id :feature/type :feat/geometry ::properties]))

(gen/sample (spec/gen :poly/feature))

(spec/def ::feature-spec (spec/keys :req-un
                                 [:gfeature/id :gfeature/type
                                  :gj/geometry :gfeature/properties]))

(spec/def :gj/features (spec/coll-of ::feature-spec))
(spec/def :fc/type #{"FeatureCollection"})
(spec/def ::featurecollection-spec (spec/keys :req-un [:fc/type :gj/features]))

(spec/def ::string-test (spec/with-gen #(> (count %) 200)
                                      gen/string-alphanumeric))

(gen/sample (spec/gen ::string-test)) ; custom gen?

(spec/def ::string-test (spec/with-gen #(> (count %) 0)
                                       gen/string-alphanumeric))
(gen/sample (spec/gen ::string-test)) ; custom gen?


(def sqlcol-regex #"[a-z][a-z0-9_]*")
(spec/def ::regex (spec/with-gen
                          (spec/and string? #(> (count %) 0) #(re-matches sqlcol-regex %))
                          #(genc/string-from-regex sqlcol-regex)))
(gen/sample (spec/gen ::regex)) ; custom gen?
