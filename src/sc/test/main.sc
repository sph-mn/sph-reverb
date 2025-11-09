(pre-include "sph-reverb/sph-reverb.h" "sph-reverb/sph/test.h")

(define (test-sp-map-event) status-t
  status-declare
  (declare
    size sp-time-t
    block sp-block-t
    amod sp-sample-t*
    config sp-wave-event-config-t*
    map-event-config sp-map-event-config-t*)
  (sp-declare-event parent)
  (sp-declare-event child)
  (error-memory-init 2)
  (status-require (sp-wave-event-config-new &config))
  (error-memory-add config)
  (status-require (sp-map-event-config-new &map-event-config))
  (error-memory-add map-event-config)
  (set size (* 10 _sp-rate))
  (status-require (sp-path-samples2 &amod size 1.0 1.0))
  (struct-pointer-set config channel-count 1)
  (struct-pointer-set config:channel-config frq 300 fmod 0 amp 1 amod amod)
  (struct-set child start 0 end size)
  (sp-wave-event &child config)
  (status-require (sp-block-new 1 size &block))
  (struct-pointer-set map-event-config
    event child
    map-generate test-sp-map-event-generate
    isolate #t)
  (struct-set parent start child.start end child.end)
  (sp-map-event &parent map-event-config)
  (status-require (parent.prepare &parent))
  (status-require (parent.generate 0 (/ size 2) &block &parent))
  (status-require (parent.generate (/ size 2) size &block &parent))
  (parent.free &parent)
  (sp-block-free &block)
  (free amod)
  (label exit (if status-is-failure error-memory-free) status-return))

(define (main) int
  status-declare
  (test-helper-test-one test-sp-map-event)
  (label exit test-helper-display-summary (return status.id)))
