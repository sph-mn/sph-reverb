(declare
  sp-reverb-vec3-t (type (struct (x float) (y float) (z float)))
  sp-reverb-early-geometry-t
  (type
    (struct
      (vertex-list sp-reverb-vec3-t*)
      (vertex-count uint32-t)
      (index-list uint32-t*)
      (index-count uint32-t)
      (face-wall-index-list uint32-t*)
      (face-count uint32-t)
      (embree-scene-handle void*)))
  sp-reverb-early-materials-t
  (type
    (struct
      (band-period-list sp-time-t*)
      (band-count sp-time-t)
      (wall-reflectivity-list sp-sample-t*)
      (wall-count uint32-t)
      (air-attenuation-list sp-sample-t*)))
  sp-reverb-early-scene-t
  (type
    (struct
      (geometry sp-reverb-early-geometry-t)
      (materials sp-reverb-early-materials-t)
      (embree-device-handle void*)
      (embree-scene-handle void*)))
  sp-reverb-early-source-t
  (type (struct (position-world sp-reverb-vec3-t) (orientation-world sp-reverb-vec3-t)))
  sp-reverb-early-receiver-t
  (type (struct (position-world sp-reverb-vec3-t) (orientation-world sp-reverb-vec3-t)))
  sp-reverb-early-path-t
  (type
    (struct
      (delay sp-time-t)
      (gain sp-sample-t)
      (direction sp-reverb-vec3-t)
      (wall-index-chain uint32-t*)
      (wall-index-count uint32-t)
      (order uint32-t)
      (band-gain-list sp-sample-t*)
      (band-count sp-time-t)))
  sp-reverb-early-path-set-t
  (type (struct (path-list sp-reverb-early-path-t*) (path-count sp-time-t)))
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
  (sp-reverb-early-build-scene geometry materials out-scene)
  (void sp-reverb-early-geometry-t* sp-reverb-early-materials-t* sp-reverb-early-scene-t*)
  (sp-reverb-early-paths-image scene source receiver max-order path-cap)
  (sp-reverb-early-path-set-t sp-reverb-early-scene-t* sp-reverb-early-source-t*
    sp-reverb-early-receiver-t* sp-time-t sp-time-t)
  (sp-reverb-early-paths-beam scene source
    receiver max-order path-cap portal-index-list portal-index-count)
  (sp-reverb-early-path-set-t sp-reverb-early-scene-t* sp-reverb-early-source-t*
    sp-reverb-early-receiver-t* sp-time-t sp-time-t uint32-t* uint32-t)
  (sp-reverb-early-paths-diffraction scene edge-index-list
    edge-index-count band-period-list band-count)
  (sp-reverb-early-path-set-t sp-reverb-early-scene-t* uint32-t* uint32-t sp-time-t* sp-time-t)
  (sp-reverb-early-paths-union path-set-list path-set-count)
  (sp-reverb-early-path-set-t sp-reverb-early-path-set-t* uint32-t)
  (sp-reverb-early-paths-cull path-set threshold max-paths)
  (sp-reverb-early-path-set-t sp-reverb-early-path-set-t* sp-sample-t sp-time-t)
  (sp-reverb-early-noise-partials-from-paths path-set layout
    band-period-start band-period-end duration
    out-partial-list out-partial-capacity out-partial-count)
  (void sp-reverb-early-path-set-t* sp-reverb-layout-t*
    sp-time-t sp-time-t sp-time-t sp-reverb-early-noise-partial-t* sp-time-t sp-time-t*)
  (sp-reverb-early-partials-from-paths path-set layout
    partial-list partial-count out-partial-list out-partial-capacity out-partial-count)
  (void sp-reverb-early-path-set-t* sp-reverb-layout-t*
    sp-reverb-sampled-partial-t* sp-time-t sp-reverb-early-partial-t* sp-time-t sp-time-t*)
  (sp-reverb-early scene source
    receiver layout partial-list
    partial-count out-partial-list out-partial-capacity out-partial-count)
  (void sp-reverb-early-scene-t* sp-reverb-early-source-t*
    sp-reverb-early-receiver-t* sp-reverb-layout-t* sp-reverb-sampled-partial-t*
    sp-time-t sp-reverb-early-partial-t* sp-time-t sp-time-t*))
