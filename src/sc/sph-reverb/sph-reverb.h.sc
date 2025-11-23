(pre-include-guard-begin sph-reverb-h-included)
(pre-include "inttypes.h" "stddef.h")

(pre-define-if-not-defined
  sp-sample-t double
  sp-time-t uint32-t
  sp-pi 3.14159265358979323846
  sp-channel-count-t uint8-t
  sph-reverb-position-max-dimensions 8)

(declare
  sp-reverb-layout-t
  (type (struct (bases sp-sample-t*) (channel-count sp-channel-count-t) (basis-length uint8-t)))
  sp-reverb-position-t
  (type
    (struct
      (values (array sp-sample-t sph-reverb-position-max-dimensions))
      (dimension-count uint8-t)))
  sp-reverb-mode-t
  (type (struct (period sp-time-t) (decay sp-time-t) (amplitude sp-sample-t) (phase sp-time-t)))
  sp-reverb-modal-set-t (type (struct (mode-list sp-reverb-mode-t*) (mode-count sp-time-t)))
  sp-reverb-channel-modal-set-t
  (type
    (struct (mode-list sp-reverb-mode-t*) (mode-count sp-time-t) (channel-count sp-channel-count-t)))
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
  (sp-reverb-complex-divide a-real a-imag b-real b-imag out-real out-imag)
  (void sp-sample-t sp-sample-t sp-sample-t sp-sample-t sp-sample-t* sp-sample-t*)
  (sp-reverb-complex-magnitude value-real value-imag) (sp-sample-t sp-sample-t sp-sample-t)
  (sp-reverb-complex-argument value-real value-imag) (sp-sample-t sp-sample-t sp-sample-t)
  (sp-reverb-late-modal config) (sp-reverb-modal-set-t sp-reverb-late-config-t*)
  (sp-reverb-late-modal-residues config poles layout position out-modes)
  (void sp-reverb-late-config-t* sp-reverb-modal-set-t*
    sp-reverb-layout-t* sp-reverb-position-t* sp-reverb-channel-modal-set-t*)
  (sp-reverb-band-gain-at config period) (sp-sample-t sp-reverb-late-config-t* sp-time-t)
  (sp-reverb-build-feedback-matrix config period matrix-real matrix-imag)
  (void sp-reverb-late-config-t* sp-time-t sp-sample-t* sp-sample-t*)
  (sp-reverb-build-feedback-matrix-from-polar config radius angle matrix-real matrix-imag)
  (void sp-reverb-late-config-t* sp-sample-t sp-sample-t sp-sample-t* sp-sample-t*)
  (sp-reverb-form-identity-minus-feedback line-count feedback-real feedback-imag a-real a-imag)
  (void sp-time-t sp-sample-t* sp-sample-t* sp-sample-t* sp-sample-t*)
  (sp-reverb-lower-upper-factorization line-count matrix-real matrix-imag pivot-index-list)
  (void sp-time-t sp-sample-t* sp-sample-t* sp-time-t*)
  (sp-reverb-lower-upper-solve line-count matrix-real
    matrix-imag pivot-list right-real right-imag solution-real solution-imag)
  (void sp-time-t sp-sample-t*
    sp-sample-t* sp-time-t* sp-sample-t* sp-sample-t* sp-sample-t* sp-sample-t*)
  (sp-reverb-power-iteration-dominant-eigenpair line-count matrix-real
    matrix-imag eigenvalue-real eigenvalue-imag eigenvector-real eigenvector-imag iteration-limit)
  (void sp-time-t sp-sample-t*
    sp-sample-t* sp-sample-t* sp-sample-t* sp-sample-t* sp-sample-t* sp-time-t)
  (sp-reverb-eigen-equation-value config radius angle out-real out-imag)
  (void sp-reverb-late-config-t* sp-sample-t sp-sample-t sp-sample-t* sp-sample-t*)
  (sp-reverb-eigen-equation-jacobian-finite-difference config radius
    angle out-real-radius out-real-angle out-imag-radius out-imag-angle)
  (void sp-reverb-late-config-t* sp-sample-t
    sp-sample-t sp-sample-t* sp-sample-t* sp-sample-t* sp-sample-t*)
  (sp-reverb-newton-step-on-eigen-equation radius angle
    real-radius real-angle imag-radius
    imag-angle value-real value-imag out-radius-next out-angle-next)
  (void sp-sample-t sp-sample-t
    sp-sample-t sp-sample-t sp-sample-t sp-sample-t sp-sample-t sp-sample-t sp-sample-t* sp-sample-t*)
  (sp-reverb-build-state-excitation config position out-real out-imag)
  (void sp-reverb-late-config-t* sp-reverb-position-t* sp-sample-t* sp-sample-t*)
  (sp-reverb-build-state-projection config layout position channel-index out-real out-imag)
  (void sp-reverb-late-config-t* sp-reverb-layout-t*
    sp-reverb-position-t* sp-channel-count-t sp-sample-t* sp-sample-t*))

(pre-include-guard-end)
