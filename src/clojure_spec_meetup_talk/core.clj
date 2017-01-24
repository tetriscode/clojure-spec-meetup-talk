(ns clojure-spec-meetup-talk.core
  (:require [clojure.spec :as s]
            [clojure.string :as str]
            [com.gfredericks.test.chuck.generators :as genc]
            [clojure.test.check.generators :as gen]))

;; First up, validation with predicates
;; (s/valid? <predicates> 10)
(s/valid? even? 10)
(s/valid? #(> (count %) 0) "2017-01-01T01:01:01Z")

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
(s/def ::longitude double?)
(s/def ::latitude double?)
(s/def :wgs/coordinates (s/tuple ::longitude ::latitude))
(s/valid? :wgs/coordinates [1.0 2.0])

(s/explain :wgs/coordinates [1.8 2.8])
(def test-val [:test 2.8])
(s/explain :wgs/coordinates test-val)
(s/explain-str :wgs/coordinates test-val)
(s/explain-data :wgs/coordinates test-val)
(s/describe :wgs/coordinates)
(s/def ::seq (s/* int?))
(s/valid? ::seq [:key "foo" 1])
(s/conform ::seq [:key "foo" 1])
(s/valid? ::seq [1 3 4])
(s/conform ::seq [0])
(s/valid? ::seq [])

(s/def :cart/coordinates (s/cat :x ::longitude :y ::latitude))

(gen/generate (s/gen :wgs/coordinates)) ; vec
(type (clojure.spec.gen/generate (s/gen :wgs/coordinates))) ; vec
(type (clojure.spec.gen/generate (s/gen :cart/coordinates))) ; seq

(s/def ::type #{"Point"})

(s/def ::geometry (s/keys :req-un [::type :wgs/coordinates]))

(gen/generate (s/gen ::geometry))

(gen/sample (s/gen ::geometry))

(s/def ::longitude (s/double-in :min -180.0 :max 180 :NaN false :infinite? false))
(s/def ::latitude (s/double-in :min -90.0 :max 90 :NaN false :infinite? false))
(s/def ::coordinates (s/tuple ::longitude ::latitude))
(s/def ::geometry (s/keys :req-un [::type ::coordinates]))

(gen/sample (s/gen ::geometry))

(s/def :feature/type #{"Feature"})
(s/def ::properties map?)

(s/def ::feature (s/keys :req-un [:feature/type ::properties]))
(gen/sample (s/gen ::feature))

(s/def ::properties (s/map-of string? (s/or :s string? :n number? :m map? :c coll?)))

(s/def ::jsonobj (s/map-of string? (s/or :s string? :n number? :m ::jsonobj :c (s/coll-of ::jsonobj))))

;(s/def ::properties ::jsonobj) ; pegging the CPU time
(s/def ::id (s/or :p pos-int? :s (s/and string? #(not (str/blank? %)))))
(s/def ::feature (s/keys :req-un [::id :feature/type ::properties]))

(gen/sample (s/gen ::feature))

; namespace point
(s/def :pt/type #{"Point"})
(s/def :pt/geometry (s/keys :req-un [:pt/type ::coordinates]))
(s/def :pt/feature (s/keys :req-un [::id :pt/type :pt/geometry ::properties]))
; namespace poly
(s/def :poly/type #{"Polygon"})

(defn circle-gen [x y]
  (let [vertices (+ (rand-int 8) 4)
        radius (rand 3) ;2 dec degrees radius length
        rads (/ (* 2.0 Math/PI) vertices)
        pts (map (fn [r]
                   [(+ x (* radius (Math/cos (* r rads))))
                    (+ y (* radius (Math/sin (* rads r))))])
                 (range vertices))]
    (conj pts (last pts))))
(s/def :poly/coordinates (s/with-gen
                           coll?
                           #(gen/fmap (fn [[lon lat]] (list (circle-gen lon lat)))
                                      (gen/tuple (s/gen ::longitude) (s/gen ::latitude)))))
(s/def :poly/geometry (s/keys :req [:poly/type :poly/coordinates]))
(s/def :poly/feature (s/keys :req-un [::id :poly/geometry :poly/type ::properties]))

(s/def :feat/geometry (s/or :poly/feature :pt/feature))
(s/def ::feature (s/keys :req-un [::id :feature/type :feat/geometry ::properties]))

(gen/sample (s/gen :poly/feature))

(s/def ::feature-spec (s/keys :req-un
                                 [:gfeature/id :gfeature/type
                                  :gj/geometry :gfeature/properties]))

(s/def :gj/features (s/coll-of ::feature-spec))
(s/def :fc/type #{"FeatureCollection"})
(s/def ::featurecollection-spec (s/keys :req-un [:fc/type :gj/features]))

(s/def ::string-test (s/with-gen #(> (count %) 200)
                                 gen/string-alphanumeric))

(gen/sample (s/gen ::string-test)) ; custom gen?

(s/def ::string-test (s/with-gen #(> (count %) 0)
                                 gen/string-alphanumeric))
(gen/sample (s/gen ::string-test)) ; custom gen?


(def email-regex #"^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,63}$")
(s/def :user/email-type (s/and string? #(re-matches email-regex %)))
(s/def :user/email :user/email-type)
(s/def :user/password string?)
(s/def :user/name string?)
(s/def ::user-spec (s/keys :req-un [:user/email :user/password]
                           :opt-un [:user/name]))

(gen/sample (s/gen ::user-spec) )

(def sqlcol-regex #"[a-z][a-z0-9_]*")
(s/def ::regex (s/with-gen
                 (s/and string? #(> (count %) 0) #(re-matches sqlcol-regex %))
                 #(genc/string-from-regex sqlcol-regex)))
(gen/sample (s/gen ::regex)) ; custom gen?

(def email-regex #"[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,63}")
(s/def :user/email-type (s/with-gen
                          (s/and string? #(re-matches email-regex %))
                          #(genc/string-from-regex email-regex))) ; no anchors
(s/def ::user-spec (s/keys :req-un [:user/email :user/password]
                           :opt-un [:user/name]))

(gen/sample (s/gen ::user-spec))


