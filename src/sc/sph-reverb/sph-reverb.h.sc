(pre-include-guard-begin sph-reverb-h-included)
(pre-include "inttypes.h" "stddef.h")
(pre-define-if-not-defined sp-sample-t double sp-time-t uint32-t sp-channel-count-t uint8-t)

(declare
  sp-reverb-response-t (type (struct (gain sp-sample-t) (decay sp-time-t) (phase sp-time-t)))
  sp-reverb-layout-t
  (type (struct (bases sp-sample-t*) (channel-count sp-channel-count-t) (basis-length sp-time-t)))
  sp-reverb-late-config-t
  (type
    (struct
      (delays sp-time-t*)
      (delay-count sp-time-t)
      (mix-row-major sp-sample-t*)
      (mix-rows sp-time-t)
      (mix-columns sp-time-t)
      (band-frequencies sp-time-t*)
      (band-gains sp-sample-t*)
      (band-count sp-time-t)
      (strength sp-sample-t))))

(declare
  (sp-reverb-late-table config frequencies frequency-count out-responses)
  (void sp-reverb-late-config-t* sp-sample-t* size-t sp-reverb-response-t*)
  (sp-reverb-late-lookup frequency-list response-list response-count query-frequency)
  (sp-reverb-response-t sp-sample-t* sp-reverb-response-t* sp-time-t sp-sample-t)
  (sp-reverb-late-project response layout out-channel-gains out-channel-count)
  (void sp-reverb-response-t* sp-reverb-layout-t* sp-sample-t* size-t))

(pre-include-guard-end)
