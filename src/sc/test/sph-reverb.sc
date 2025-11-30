(pre-include "stdlib.h" "math.h" "sph-reverb/sph/test.h" "sph-reverb/sph-reverb.h")
(pre-define (feq a b) (<= (fabs (- a b)) 1.0e-12) sp-pi-half 1.5707963267948966)

(declare
  (sp-reverb-late-complex-divide a-real a-imag b-real b-imag out-real out-imag)
  (void sp-sample-t sp-sample-t sp-sample-t sp-sample-t sp-sample-t* sp-sample-t*)
  (sp-reverb-late-complex-magnitude value-real value-imag) (sp-sample-t sp-sample-t sp-sample-t)
  (sp-reverb-late-complex-argument value-real value-imag) (sp-sample-t sp-sample-t sp-sample-t)
  (sp-reverb-late-band-gain-at config period) (sp-sample-t sp-reverb-late-config-t* sp-time-t)
  (sp-reverb-late-build-feedback-matrix config period matrix-real matrix-imag)
  (void sp-reverb-late-config-t* sp-time-t sp-sample-t* sp-sample-t*)
  (sp-reverb-late-build-feedback-matrix-from-polar config radius angle matrix-real matrix-imag)
  (void sp-reverb-late-config-t* sp-sample-t sp-sample-t sp-sample-t* sp-sample-t*)
  (sp-reverb-late-form-identity-minus-feedback line-count feedback-real feedback-imag a-real a-imag)
  (void sp-time-t sp-sample-t* sp-sample-t* sp-sample-t* sp-sample-t*)
  (sp-reverb-late-lower-upper-factorization line-count matrix-real matrix-imag pivot-index-list)
  (void sp-time-t sp-sample-t* sp-sample-t* sp-time-t*)
  (sp-reverb-late-lower-upper-solve line-count matrix-real
    matrix-imag pivot-list right-real right-imag solution-real solution-imag)
  (void sp-time-t sp-sample-t*
    sp-sample-t* sp-time-t* sp-sample-t* sp-sample-t* sp-sample-t* sp-sample-t*)
  (sp-reverb-late-power-iteration-dominant-eigenpair line-count matrix-real
    matrix-imag eigenvalue-real eigenvalue-imag eigenvector-real eigenvector-imag iteration-limit)
  (void sp-time-t sp-sample-t*
    sp-sample-t* sp-sample-t* sp-sample-t* sp-sample-t* sp-sample-t* sp-time-t)
  (sp-reverb-late-eigen-equation-value config radius angle out-real out-imag)
  (void sp-reverb-late-config-t* sp-sample-t sp-sample-t sp-sample-t* sp-sample-t*)
  (sp-reverb-late-eigen-equation-jacobian-finite-difference config radius
    angle out-dfr-dr out-dfr-dtheta out-dfi-dr out-dfi-dtheta)
  (void sp-reverb-late-config-t* sp-sample-t
    sp-sample-t sp-sample-t* sp-sample-t* sp-sample-t* sp-sample-t*)
  (sp-reverb-late-newton-step-on-eigen-equation radius-current angle-current
    real-derivative-radius real-derivative-angle imag-derivative-radius
    imag-derivative-angle real-value imag-value radius-next angle-next)
  (void sp-sample-t sp-sample-t
    sp-sample-t sp-sample-t sp-sample-t sp-sample-t sp-sample-t sp-sample-t sp-sample-t* sp-sample-t*)
  (sp-reverb-late-build-state-excitation config position out-real out-imag)
  (void sp-reverb-late-config-t* sp-reverb-position-t* sp-sample-t* sp-sample-t*)
  (sp-reverb-late-null-vector-of-shifted-matrix line-count a-real
    a-imag use-transpose out-real out-imag)
  (void sp-time-t sp-sample-t* sp-sample-t* int sp-sample-t* sp-sample-t*)
  (sp-reverb-late-build-state-projection config layout position channel-index out-real out-imag)
  (void sp-reverb-late-config-t* sp-reverb-layout-t*
    sp-reverb-position-t* sp-channel-count-t sp-sample-t* sp-sample-t*)
  (sp-reverb-late-right-eigenvector-at-pole config radius angle out-real out-imag)
  (void sp-reverb-late-config-t* sp-sample-t sp-sample-t sp-sample-t* sp-sample-t*)
  (sp-reverb-late-left-eigenvector-at-pole config radius angle out-real out-imag)
  (void sp-reverb-late-config-t* sp-sample-t sp-sample-t sp-sample-t* sp-sample-t*))

(define (test-sp-reverb-late-complex-primitives) status-t
  status-declare
  (declare
    real-value sp-sample-t
    imag-value sp-sample-t
    mag-value sp-sample-t
    arg-value sp-sample-t)
  (sp-reverb-late-complex-divide 1.0 0.0 1.0 0.0 &real-value &imag-value)
  (test-helper-assert "complex_divide_1_real" (feq real-value 1.0))
  (test-helper-assert "complex_divide_1_imag" (feq imag-value 0.0))
  (sp-reverb-late-complex-divide 1.0 1.0 1.0 0.0 &real-value &imag-value)
  (test-helper-assert "complex_divide_2_real" (feq real-value 1.0))
  (test-helper-assert "complex_divide_2_imag" (feq imag-value 1.0))
  (set mag-value (sp-reverb-late-complex-magnitude 3.0 4.0))
  (test-helper-assert "complex_mag_3_4" (feq mag-value 5.0))
  (set arg-value (sp-reverb-late-complex-argument 0.0 1.0))
  (test-helper-assert "complex_arg_pi_over_2" (feq arg-value sp-pi-half))
  (label exit status-return))

(define (test-sp-reverb-late-feedback-matrix-basic) status-t
  status-declare
  (declare
    config sp-reverb-late-config-t
    delays (array sp-time-t 1)
    mix-row-major (array sp-sample-t 1)
    matrix-real (array sp-sample-t 1)
    matrix-imag (array sp-sample-t 1)
    band-gain sp-sample-t)
  (set
    (array-get delays 0) 100
    (array-get mix-row-major 0) 1.0
    config.delays delays
    config.delay-count 1
    config.mix-row-major mix-row-major
    config.mix-rows 1
    config.mix-columns 1
    config.band-periods NULL
    config.band-gains NULL
    config.band-count 0
    config.strength 0.5
    config.state-directions NULL
    config.state-dimension-count 0
    band-gain (sp-reverb-late-band-gain-at &config 200))
  (test-helper-assert "band_gain_not_nan" (not (isnan band-gain)))
  (sp-reverb-late-build-feedback-matrix &config 200 matrix-real matrix-imag)
  (test-helper-assert "feedback_matrix_real_not_nan" (not (isnan (array-get matrix-real 0))))
  (test-helper-assert "feedback_matrix_imag_not_nan" (not (isnan (array-get matrix-imag 0))))
  (sp-reverb-late-build-feedback-matrix-from-polar &config 0.8
    (* sp-pi 0.25) matrix-real matrix-imag)
  (test-helper-assert "feedback_matrix_polar_real_not_nan" (not (isnan (array-get matrix-real 0))))
  (test-helper-assert "feedback_matrix_polar_imag_not_nan" (not (isnan (array-get matrix-imag 0))))
  (label exit status-return))

(define (test-sp-reverb-late-lu-and-solve-basic) status-t
  status-declare
  (declare
    matrix-real (array sp-sample-t 4)
    matrix-imag (array sp-sample-t 4)
    right-real (array sp-sample-t 2)
    right-imag (array sp-sample-t 2)
    solution-real (array sp-sample-t 2)
    solution-imag (array sp-sample-t 2)
    pivot-list (array sp-time-t 2))
  (set
    (array-get matrix-real 0) 4.0
    (array-get matrix-real 1) 1.0
    (array-get matrix-real 2) 2.0
    (array-get matrix-real 3) 3.0
    (array-get matrix-imag 0) 0.0
    (array-get matrix-imag 1) 0.0
    (array-get matrix-imag 2) 0.0
    (array-get matrix-imag 3) 0.0
    (array-get right-real 0) 1.0
    (array-get right-real 1) 1.0
    (array-get right-imag 0) 0.0
    (array-get right-imag 1) 0.0)
  (sp-reverb-late-lower-upper-factorization 2 matrix-real matrix-imag pivot-list)
  (sp-reverb-late-lower-upper-solve 2 matrix-real
    matrix-imag pivot-list right-real right-imag solution-real solution-imag)
  (test-helper-assert "lu_solve_real_0_not_nan" (not (isnan (array-get solution-real 0))))
  (test-helper-assert "lu_solve_real_1_not_nan" (not (isnan (array-get solution-real 1))))
  (test-helper-assert "lu_solve_imag_0_not_nan" (not (isnan (array-get solution-imag 0))))
  (test-helper-assert "lu_solve_imag_1_not_nan" (not (isnan (array-get solution-imag 1))))
  (label exit status-return))

(define (test-sp-reverb-late-state-spatial-basic) status-t
  status-declare
  (declare
    config sp-reverb-late-config-t
    position sp-reverb-position-t
    layout sp-reverb-layout-t
    state-directions (array sp-sample-t 2)
    bases (array sp-sample-t 2)
    vec-real (array sp-sample-t 2)
    vec-imag (array sp-sample-t 2))
  (set
    (array-get state-directions 0) -1.0
    (array-get state-directions 1) 1.0
    (array-get bases 0) -1.0
    (array-get bases 1) 1.0
    config.delays NULL
    config.delay-count 2
    config.mix-row-major NULL
    config.mix-rows 0
    config.mix-columns 0
    config.band-periods NULL
    config.band-gains NULL
    config.band-count 0
    config.strength 1.0
    config.state-directions state-directions
    config.state-dimension-count 1
    position.dimension-count 1
    (array-get position.values 0) 0.0
    layout.bases bases
    layout.channel-count 2
    layout.basis-length 1)
  (sp-reverb-late-build-state-excitation &config &position vec-real vec-imag)
  (test-helper-assert "state_excitation_real_0_not_nan" (not (isnan (array-get vec-real 0))))
  (test-helper-assert "state_excitation_real_1_not_nan" (not (isnan (array-get vec-real 1))))
  (test-helper-assert "state_excitation_imag_0_zero" (feq (array-get vec-imag 0) 0.0))
  (test-helper-assert "state_excitation_imag_1_zero" (feq (array-get vec-imag 1) 0.0))
  (sp-reverb-late-build-state-projection &config &layout &position 0 vec-real vec-imag)
  (test-helper-assert "state_projection_real_0_not_nan" (not (isnan (array-get vec-real 0))))
  (test-helper-assert "state_projection_real_1_not_nan" (not (isnan (array-get vec-real 1))))
  (test-helper-assert "state_projection_imag_0_zero" (feq (array-get vec-imag 0) 0.0))
  (test-helper-assert "state_projection_imag_1_zero" (feq (array-get vec-imag 1) 0.0))
  (label exit status-return))

(define (test-sp-reverb-late-modal-basic) status-t
  status-declare
  (declare
    config sp-reverb-late-config-t
    poles sp-reverb-late-modal-set-t
    delays (array sp-time-t 2)
    mix-row-major (array sp-sample-t 4)
    state-directions (array sp-sample-t 2))
  (set
    (array-get delays 0) 100
    (array-get delays 1) 150
    (array-get mix-row-major 0) 1.0
    (array-get mix-row-major 1) 0.0
    (array-get mix-row-major 2) 0.0
    (array-get mix-row-major 3) 1.0
    (array-get state-directions 0) -1.0
    (array-get state-directions 1) 1.0
    config.delays delays
    config.delay-count 2
    config.mix-row-major mix-row-major
    config.mix-rows 2
    config.mix-columns 2
    config.band-periods NULL
    config.band-gains NULL
    config.band-count 0
    config.strength 0.8
    config.state-directions state-directions
    config.state-dimension-count 1
    poles (sp-reverb-late-modal &config))
  (test-helper-assert "modal_mode_list_not_null" (!= poles.mode-list NULL))
  (test-helper-assert "modal_mode_count_positive" (> poles.mode-count 0))
  (test-helper-assert "modal_period_positive"
    (> (struct-get (array-get poles.mode-list 0) period) 0))
  (test-helper-assert "modal_decay_positive" (> (struct-get (array-get poles.mode-list 0) decay) 0))
  (free poles.mode-list)
  (label exit status-return))

(define (test-sp-reverb-late-modal-residues-basic) status-t
  status-declare
  (declare
    config sp-reverb-late-config-t
    layout sp-reverb-layout-t
    position sp-reverb-position-t
    poles sp-reverb-late-modal-set-t
    channel-modes sp-reverb-late-channel-modal-set-t
    delays (array sp-time-t 2)
    mix-row-major (array sp-sample-t 4)
    state-directions (array sp-sample-t 2)
    bases (array sp-sample-t 2)
    total-modes sp-time-t)
  (set
    (array-get delays 0) 100
    (array-get delays 1) 150
    (array-get mix-row-major 0) 1.0
    (array-get mix-row-major 1) 0.0
    (array-get mix-row-major 2) 0.0
    (array-get mix-row-major 3) 1.0
    (array-get state-directions 0) -1.0
    (array-get state-directions 1) 1.0
    (array-get bases 0) -1.0
    (array-get bases 1) 1.0
    config.delays delays
    config.delay-count 2
    config.mix-row-major mix-row-major
    config.mix-rows 2
    config.mix-columns 2
    config.band-periods NULL
    config.band-gains NULL
    config.band-count 0
    config.strength 0.8
    config.state-directions state-directions
    config.state-dimension-count 1
    layout.bases bases
    layout.channel-count 2
    layout.basis-length 1
    position.dimension-count 1
    (array-get position.values 0) 0.0
    poles (sp-reverb-late-modal &config))
  (test-helper-assert "modal_res_poles_not_null" (!= poles.mode-list NULL))
  (test-helper-assert "modal_res_poles_count_positive" (> poles.mode-count 0))
  (set channel-modes.mode-list NULL channel-modes.mode-count 0 channel-modes.channel-count 0)
  (sp-reverb-late-modal-residues &config &poles &layout &position &channel-modes)
  (test-helper-assert "modal_res_mode_list_not_null" (!= channel-modes.mode-list NULL))
  (test-helper-assert "modal_res_channel_count_correct"
    (= channel-modes.channel-count layout.channel-count))
  (test-helper-assert "modal_res_mode_count_correct" (= channel-modes.mode-count poles.mode-count))
  (set total-modes (* channel-modes.mode-count channel-modes.channel-count))
  (test-helper-assert "modal_res_total_modes_positive" (> total-modes 0))
  (test-helper-assert "modal_res_period_match"
    (= (struct-get (array-get channel-modes.mode-list 0) period)
      (struct-get (array-get poles.mode-list 0) period)))
  (test-helper-assert "modal-res-decay-match"
    (= (struct-get (array-get channel-modes.mode-list 0) decay)
      (struct-get (array-get poles.mode-list 0) decay)))
  (free channel-modes.amplitude-list)
  (free channel-modes.phase-list)
  (free poles.mode-list)
  (label exit status-return))

(define (main) (int)
  status-declare
  (test-helper-test-one test-sp-reverb-late-modal-residues-basic)
  (test-helper-test-one test-sp-reverb-late-complex-primitives)
  (test-helper-test-one test-sp-reverb-late-feedback-matrix-basic)
  (test-helper-test-one test-sp-reverb-late-lu-and-solve-basic)
  (test-helper-test-one test-sp-reverb-late-state-spatial-basic)
  (test-helper-test-one test-sp-reverb-late-modal-basic)
  test-helper-display-summary
  (label exit (return status.id)))
