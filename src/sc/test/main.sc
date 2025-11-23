(pre-include "math.h" "sph-reverb/sph/test.h" "sph-reverb/sph-reverb.h")

(pre-define
  (feq a b) (<= (fabs (- a b)) 1.0e-12)
  m-pi-half 1.5707963267948966
  m-pi 3.141592653589793)

(define (test-sph-reverb-complex-primitives) (status-t)
  status-declare
  (declare
    real-value sp-sample-t
    imag-value sp-sample-t
    mag-value sp-sample-t
    arg-value sp-sample-t)
  (sp-reverb-complex-divide 1.0 0.0 1.0 0.0 (address-of real-value) (address-of imag-value))
  (test-helper-assert "complex_divide_1_real" (feq real-value 1.0))
  (test-helper-assert "complex_divide_1_imag" (feq imag-value 0.0))
  (sp-reverb-complex-divide 1.0 1.0 1.0 0.0 (address-of real-value) (address-of imag-value))
  (test-helper-assert "complex_divide_2_real" (feq real-value 1.0))
  (test-helper-assert "complex_divide_2_imag" (feq imag-value 1.0))
  (set mag-value (sp-reverb-complex-magnitude 3.0 4.0))
  (test-helper-assert "complex_mag_3_4" (feq mag-value 5.0))
  (set arg-value (sp-reverb-complex-argument 0.0 1.0))
  (test-helper-assert "complex_arg_pi_over_2" (feq arg-value m-pi-half))
  (label exit status-return))

(define (test-sph-reverb-feedback-matrix-basic) (status-t)
  status-declare
  (declare
    config sp-reverb-late-config-t
    delays (array sp-time-t 1)
    mix-row-major (array sp-sample-t 1)
    matrix-real (array sp-sample-t 1)
    matrix-imag (array sp-sample-t 1)
    band-gain sp-sample-t)
  (set (array-get delays 0) 100 (array-get mix-row-major 0) 1.0)
  (set
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
    config.state-dimension-count 0)
  (set band-gain (sp-reverb-band-gain-at (address-of config) 200))
  (test-helper-assert "band_gain_not_nan" (not (isnan band-gain)))
  (sp-reverb-build-feedback-matrix (address-of config) 200 matrix-real matrix-imag)
  (test-helper-assert "feedback_matrix_real_not_nan" (not (isnan (array-get matrix-real 0))))
  (test-helper-assert "feedback_matrix_imag_not_nan" (not (isnan (array-get matrix-imag 0))))
  (sp-reverb-build-feedback-matrix-from-polar (address-of config) 0.8
    (* m-pi 0.25) matrix-real matrix-imag)
  (test-helper-assert "feedback_matrix_polar_real_not_nan" (not (isnan (array-get matrix-real 0))))
  (test-helper-assert "feedback_matrix_polar_imag_not_nan" (not (isnan (array-get matrix-imag 0))))
  (label exit status-return))

(define (test-sph-reverb-lu-and-solve-basic) (status-t)
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
  (sp-reverb-lower-upper-factorization 2 matrix-real matrix-imag pivot-list)
  (sp-reverb-lower-upper-solve 2 matrix-real
    matrix-imag pivot-list right-real right-imag solution-real solution-imag)
  (test-helper-assert "lu_solve_real_0_not_nan" (not (isnan (array-get solution-real 0))))
  (test-helper-assert "lu_solve_real_1_not_nan" (not (isnan (array-get solution-real 1))))
  (test-helper-assert "lu_solve_imag_0_not_nan" (not (isnan (array-get solution-imag 0))))
  (test-helper-assert "lu_solve_imag_1_not_nan" (not (isnan (array-get solution-imag 1))))
  (label exit status-return))

(define (test-sph-reverb-state-spatial-basic) (status-t)
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
    (array-get bases 1) 1.0)
  (set
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
    config.state-dimension-count 1)
  (set position.dimension-count 1 (array-get position.values 0) 0.0)
  (set layout.bases bases layout.channel-count 2 layout.basis-length 1)
  (sp-reverb-build-state-excitation (address-of config) (address-of position) vec-real vec-imag)
  (test-helper-assert "state_excitation_real_0_not_nan" (not (isnan (array-get vec-real 0))))
  (test-helper-assert "state_excitation_real_1_not_nan" (not (isnan (array-get vec-real 1))))
  (test-helper-assert "state_excitation_imag_0_zero" (feq (array-get vec-imag 0) 0.0))
  (test-helper-assert "state_excitation_imag_1_zero" (feq (array-get vec-imag 1) 0.0))
  (sp-reverb-build-state-projection (address-of config) (address-of layout)
    (address-of position) 0 vec-real vec-imag)
  (test-helper-assert "state_projection_real_0_not_nan" (not (isnan (array-get vec-real 0))))
  (test-helper-assert "state_projection_real_1_not_nan" (not (isnan (array-get vec-real 1))))
  (test-helper-assert "state_projection_imag_0_zero" (feq (array-get vec-imag 0) 0.0))
  (test-helper-assert "state_projection_imag_1_zero" (feq (array-get vec-imag 1) 0.0))
  (label exit status-return))

(define (main) (int)
  status-declare
  (test-helper-test-one test-sph-reverb-complex-primitives)
  (test-helper-test-one test-sph-reverb-feedback-matrix-basic)
  (test-helper-test-one test-sph-reverb-lu-and-solve-basic)
  (test-helper-test-one test-sph-reverb-state-spatial-basic)
  test-helper-display-summary
  (label exit (return status.id)))
