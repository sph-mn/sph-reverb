(pre-include-guard-begin sph-reverb-h-included)

(pre-include "inttypes.h")

(pre-define-if-not-defined
  sp-sample-t double
  sp-time-t uint32-t
  sp-pi 3.141592653589793
  sp-channel-count-t uint8-t
  sph-reverb-position-max-dimensions 8)

(pre-include "sph-reverb/late.h" "sph-reverb/early.h")

(pre-include-guard-end)
