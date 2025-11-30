(declare

  sp-reverb-late-mode-t (type (struct (period sp-time-t) (decay sp-time-t)))
  sp-reverb-late-modal-set-t
  (type (struct (mode-list sp-reverb-late-mode-t*) (mode-count sp-time-t)))
  sp-reverb-late-channel-modal-set-t
  (type
    (struct
      (mode-list sp-reverb-late-mode-t*)
      (mode-count sp-time-t)
      (amplitude-list sp-sample-t*)
      (phase-list sp-time-t*)
      (channel-count sp-channel-count-t)))
  sp-reverb-late-config-t
  (type
    (struct
      (delays sp-time-t*)
      (delay-count sp-time-t)
      (mix-row-major sp-sample-t*)
      (mix-rows sp-time-t)
      (mix-columns sp-time-t)
      (band-periods sp-time-t*)
      (band-gains sp-sample-t*)
      (band-count sp-time-t)
      (strength sp-sample-t)
      (state-directions sp-sample-t*)
      (state-dimension-count uint8-t)))
  (sp-reverb-late-modal config) (sp-reverb-late-modal-set-t sp-reverb-late-config-t*)
  (sp-reverb-late-modal-residues config poles layout position out-modes)
  (void sp-reverb-late-config-t* sp-reverb-late-modal-set-t*
    sp-reverb-layout-t* sp-reverb-position-t* sp-reverb-late-channel-modal-set-t*))
