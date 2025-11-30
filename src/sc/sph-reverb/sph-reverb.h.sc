(pre-include-guard-begin sph-reverb-h-included)
(pre-include "inttypes.h")

(pre-define-if-not-defined
  sp-sample-t double
  sp-time-t uint32-t
  sp-pi 3.141592653589793
  sp-channel-count-t uint8-t
  sp-reverb-position-max-dimensions 8
  sp-rate (convert-type 48000 sp-sample-t)
  sp-reverb-sound-meter-sample (/ sp-rate (convert-type 343.0 sp-sample-t)))

(declare
  sp-reverb-sampled-partial-t (type (struct (gain sp-sample-t) (period sp-time-t) (phase sp-time-t)))
  sp-reverb-layout-t
  (type (struct (bases sp-sample-t*) (channel-count sp-channel-count-t) (basis-length uint8-t)))
  sp-reverb-position-t
  (type
    (struct
      (values (array sp-sample-t sp-reverb-position-max-dimensions))
      (dimension-count uint8-t))))

(pre-include "sph-reverb/late.h" "sph-reverb/early.h")
(pre-include-guard-end)
