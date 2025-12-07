(pre-include-guard-begin sph-reverb-h-included)
(pre-include "inttypes.h")

(pre-define-if-not-defined
  sp-sample-t double
  sp-time-t uint32-t
  sp-pi 3.141592653589793
  sp-channel-count-t uint8-t
  sp-reverb-position-max-dimensions 8
  sp-rate (convert-type 48000 sp-sample-t)
  sp-reverb-sound-meter-sample (/ sp-rate (convert-type 343.0 sp-sample-t))
  sp-bool-t uint8-t)

(declare
  sp-reverb-sampled-partial-t (type (struct (gain sp-sample-t) (period sp-time-t) (phase sp-time-t)))
  sp-reverb-position-t
  (type
    (struct
      (values (array sp-sample-t sp-reverb-position-max-dimensions))
      (dimension-count uint8-t)))
  sp-reverb-layout-t
  (type
    (struct
      (bases sp-sample-t*)
      (channel-position-list sp-reverb-position-t*)
      (channel-count sp-channel-count-t)
      (basis-length uint8-t)))
  sp-reverb-vec3-t (type (struct (x float) (y float) (z float)))
  sp-reverb-geometry-t
  (type
    (struct
      (vertex-list sp-reverb-vec3-t*)
      (vertex-count uint32-t)
      (index-list uint32-t*)
      (index-count uint32-t)
      (triangle-material-index-list uint32-t*)
      (triangle-count uint32-t)))
  sp-reverb-materials-t
  (type
    (struct
      (band-period-list sp-time-t*)
      (band-count uint32-t)
      (material-reflectivity-list sp-sample-t*)
      (material-count uint32-t)
      (air-attenuation-list sp-sample-t*)))
  sp-reverb-scene-t (type (struct (geometry sp-reverb-geometry-t) (materials sp-reverb-materials-t))))

(pre-include "sph-reverb/late.h" "sph-reverb/early.h")
(pre-include-guard-end)
