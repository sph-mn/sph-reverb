(declare
  sp-reverb-early-path-t
  (type
    (struct
      (delay sp-time-t)
      (direction-receiver sp-reverb-vec3-t)
      (direction-source sp-reverb-vec3-t)
      (triangle-index-chain uint32-t*)
      (triangle-index-count uint32-t)
      (order uint32-t)
      (path-type uint32-t)))
  sp-reverb-early-context-t
  (type
    (struct (scene sp-reverb-scene-t*) (embree-device-handle void*) (embree-scene-handle void*)))
  sp-reverb-early-cutoff-t
  (type (struct (min-energy sp-sample-t) (max-delay sp-time-t) (max-path-length sp-sample-t)))
  sp-reverb-early-source-t
  (type (struct (position-world sp-reverb-vec3-t) (orientation-world sp-reverb-vec3-t)))
  sp-reverb-early-receiver-t
  (type (struct (position-world sp-reverb-vec3-t) (orientation-world sp-reverb-vec3-t)))
  sp-reverb-early-path-set-t
  (type (struct (path-list sp-reverb-early-path-t*) (path-count uint32-t)))
  sp-reverb-early-partial-t
  (type
    (struct
      (channel-index sp-channel-count-t)
      (gain sp-sample-t)
      (period sp-time-t)
      (phase sp-time-t)
      (duration sp-time-t)))
  sp-reverb-early-noise-partial-t
  (type
    (struct
      (channel-index sp-channel-count-t)
      (gain sp-sample-t)
      (band-period-start sp-time-t)
      (band-period-end sp-time-t)
      (duration sp-time-t)))
  sp-reverb-early-statistics-t
  (type
    (struct
      (energy-envelope-per-band sp-sample-t*)
      (energy-envelope-sample-count uint32-t)
      (directional-histogram sp-sample-t*)
      (directional-bin-count uint32-t)
      (arrival-density sp-sample-t*)
      (arrival-density-sample-count uint32-t)))
  (sp-reverb-early-paths-union path-set-list path-set-count)
  (sp-reverb-early-path-set-t sp-reverb-early-path-set-t* uint32-t)
  (sp-reverb-early-paths-cull scene path-set cutoff max-path-count)
  (sp-reverb-early-path-set-t sp-reverb-scene-t* sp-reverb-early-path-set-t*
    sp-reverb-early-cutoff-t* sp-time-t)
  (sp-reverb-early-statistics-from-paths scene path-set out-statistics)
  (void sp-reverb-scene-t* sp-reverb-early-path-set-t* sp-reverb-early-statistics-t*)
  (sp-reverb-early-paths-diffraction context edge-index-list edge-index-count cutoff)
  (sp-reverb-early-path-set-t sp-reverb-early-context-t* uint32-t*
    uint32-t sp-reverb-early-cutoff-t*)
  (sp-reverb-early-noise-partials-from-paths scene path-set
    layout band-period-start band-period-end
    duration out-partial-list out-partial-capacity out-partial-count)
  (void sp-reverb-scene-t* sp-reverb-early-path-set-t*
    sp-reverb-layout-t* sp-time-t sp-time-t
    sp-time-t sp-reverb-early-noise-partial-t* sp-time-t sp-time-t*)
  (sp-reverb-early-partials-from-paths scene path-set
    layout partial-list partial-count out-partial-list out-partial-capacity out-partial-count)
  (void sp-reverb-scene-t* sp-reverb-early-path-set-t*
    sp-reverb-layout-t* sp-reverb-sampled-partial-t* sp-time-t
    sp-reverb-early-partial-t* sp-time-t sp-time-t*)
  (sp-reverb-early-context-init scene out-context)
  (void sp-reverb-scene-t* sp-reverb-early-context-t*)
  (sp-reverb-early-context-shutdown context) (void sp-reverb-early-context-t*)
  (sp-reverb-early-paths-image context source receiver cutoff path-capacity out-path-list)
  (sp-time-t sp-reverb-early-context-t* sp-reverb-early-source-t*
    sp-reverb-early-receiver-t* sp-reverb-early-cutoff-t* sp-time-t sp-reverb-early-path-t*)
  (sp-reverb-early context source
    receiver layout cutoff
    partial-list partial-count out-partial-list out-partial-capacity out-partial-count)
  (void sp-reverb-early-context-t* sp-reverb-early-source-t*
    sp-reverb-early-receiver-t* sp-reverb-layout-t* sp-reverb-early-cutoff-t*
    sp-reverb-sampled-partial-t* sp-time-t sp-reverb-early-partial-t* sp-time-t sp-time-t*))
