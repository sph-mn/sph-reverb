(pre-include "stdlib.h" "math.h" "sph-reverb/sph-reverb.h")

(define (sp-reverb-complex-divide a-real a-imag b-real b-imag out-real out-imag)
  (void sp-sample-t sp-sample-t sp-sample-t sp-sample-t sp-sample-t* sp-sample-t*)
  (declare denom sp-sample-t)
  (set
    denom (+ (* b-real b-real) (* b-imag b-imag))
    *out-real (/ (+ (* a-real b-real) (* a-imag b-imag)) denom)
    *out-imag (/ (- (* a-imag b-real) (* a-real b-imag)) denom)))

(define (sp-reverb-complex-magnitude value-real value-imag) (sp-sample-t sp-sample-t sp-sample-t)
  (declare sum sp-sample-t)
  (set sum (+ (* value-real value-real) (* value-imag value-imag)))
  (return (sqrt sum)))

(define (sp-reverb-complex-argument value-real value-imag) (sp-sample-t sp-sample-t sp-sample-t)
  (return (atan2 value-imag value-real)))

(define (sp-reverb-band-gain-at config period) (sp-sample-t sp-reverb-late-config-t* sp-time-t)
  (declare band-index sp-time-t weight sp-sample-t)
  (if (= config:band-count 0) (return 0.0))
  (if (<= period (array-get config:band-periods 0)) (return (array-get config:band-gains 0)))
  (if (>= period (array-get config:band-periods (- config:band-count 1)))
    (return (array-get config:band-gains (- config:band-count 1))))
  (set band-index 0)
  (while (< (array-get config:band-periods (+ band-index 1)) period)
    (set band-index (+ band-index 1)))
  (set weight
    (/ (convert-type (- period (array-get config:band-periods band-index)) sp-sample-t)
      (convert-type
        (- (array-get config:band-periods (+ band-index 1))
          (array-get config:band-periods band-index))
        sp-sample-t)))
  (return
    (+ (array-get config:band-gains band-index)
      (*
        (- (array-get config:band-gains (+ band-index 1)) (array-get config:band-gains band-index))
        weight))))

(define (sp-reverb-build-feedback-matrix config period matrix-real matrix-imag)
  (void sp-reverb-late-config-t* sp-time-t sp-sample-t* sp-sample-t*)
  (declare
    line-count sp-time-t
    row-index sp-time-t
    column-index sp-time-t
    index sp-time-t
    delay-samples sp-time-t
    band-gain sp-sample-t
    total-gain sp-sample-t
    inv-period sp-sample-t
    angle sp-sample-t
    phase-real sp-sample-t
    phase-imag sp-sample-t
    mix-value sp-sample-t
    scale sp-sample-t)
  (set
    line-count config:delay-count
    band-gain (sp-reverb-band-gain-at config period)
    total-gain (* config:strength band-gain)
    inv-period (/ (convert-type 1.0 sp-sample-t) (convert-type period sp-sample-t))
    row-index 0)
  (while (< row-index line-count)
    (set
      delay-samples (array-get config:delays row-index)
      angle
      (* -2.0 (convert-type sp-pi sp-sample-t) (convert-type delay-samples sp-sample-t) inv-period)
      phase-real (cos angle)
      phase-imag (sin angle)
      column-index 0)
    (while (< column-index line-count)
      (set
        index (convert-type (+ (* row-index line-count) column-index) sp-time-t)
        mix-value (array-get config:mix-row-major index)
        scale (* total-gain mix-value)
        (array-get matrix-real index) (* phase-real scale)
        (array-get matrix-imag index) (* phase-imag scale)
        column-index (+ column-index 1)))
    (set row-index (+ row-index 1))))

(define (sp-reverb-build-feedback-matrix-from-polar config radius angle matrix-real matrix-imag)
  (void sp-reverb-late-config-t* sp-sample-t sp-sample-t sp-sample-t* sp-sample-t*)
  (declare
    line-count sp-time-t
    row-index sp-time-t
    column-index sp-time-t
    index sp-time-t
    delay-samples sp-time-t
    total-gain sp-sample-t
    log-radius sp-sample-t
    radius-power sp-sample-t
    delay-float sp-sample-t
    phase-angle sp-sample-t
    phase-real sp-sample-t
    phase-imag sp-sample-t
    mix-value sp-sample-t
    scale sp-sample-t)
  (set line-count config:delay-count total-gain config:strength log-radius (log radius) row-index 0)
  (while (< row-index line-count)
    (set
      delay-samples (array-get config:delays row-index)
      delay-float (convert-type delay-samples sp-sample-t)
      radius-power (exp (* -1.0 log-radius delay-float))
      phase-angle (* -1.0 angle delay-float)
      phase-real (* radius-power (cos phase-angle))
      phase-imag (* radius-power (sin phase-angle))
      column-index 0)
    (while (< column-index line-count)
      (set
        index (convert-type (+ (* row-index line-count) column-index) sp-time-t)
        mix-value (array-get config:mix-row-major index)
        scale (* total-gain mix-value)
        (array-get matrix-real index) (* phase-real scale)
        (array-get matrix-imag index) (* phase-imag scale)
        column-index (+ column-index 1)))
    (set row-index (+ row-index 1))))

(define
  (sp-reverb-form-identity-minus-feedback line-count feedback-real feedback-imag a-real a-imag)
  (void sp-time-t sp-sample-t* sp-sample-t* sp-sample-t* sp-sample-t*)
  (declare row-index sp-time-t column-index sp-time-t index sp-time-t)
  (set row-index 0)
  (while (< row-index line-count)
    (set column-index 0)
    (while (< column-index line-count)
      (set index (convert-type (+ (* row-index line-count) column-index) sp-time-t))
      (if (= row-index column-index)
        (set (array-get a-real index)
          (- (convert-type 1.0 sp-sample-t) (array-get feedback-real index)))
        (set (array-get a-real index)
          (- (convert-type 0.0 sp-sample-t) (array-get feedback-real index))))
      (set
        (array-get a-imag index) (- (convert-type 0.0 sp-sample-t) (array-get feedback-imag index))
        column-index (+ column-index 1)))
    (set row-index (+ row-index 1))))

(define (sp-reverb-lower-upper-factorization line-count matrix-real matrix-imag pivot-index-list)
  (void sp-time-t sp-sample-t* sp-sample-t* sp-time-t*)
  (declare
    k-index sp-time-t
    pivot-row sp-time-t
    row-index sp-time-t
    column-index sp-time-t
    index-a sp-time-t
    index-b sp-time-t
    pivot-abs-max sp-sample-t
    value-abs sp-sample-t
    temp-real sp-sample-t
    temp-imag sp-sample-t
    temp-index sp-time-t
    scale-real sp-sample-t
    scale-imag sp-sample-t
    prod-real sp-sample-t
    prod-imag sp-sample-t)
  (set k-index 0)
  (while (< k-index line-count)
    (set (array-get pivot-index-list k-index) k-index k-index (+ k-index 1)))
  (set k-index 0)
  (while (< k-index line-count)
    (set
      pivot-row k-index
      index-a (convert-type (+ (* k-index line-count) k-index) sp-time-t)
      pivot-abs-max
      (sp-reverb-complex-magnitude (array-get matrix-real index-a) (array-get matrix-imag index-a))
      row-index (+ k-index 1))
    (while (< row-index line-count)
      (set
        index-b (convert-type (+ (* row-index line-count) k-index) sp-time-t)
        value-abs
        (sp-reverb-complex-magnitude (array-get matrix-real index-b)
          (array-get matrix-imag index-b)))
      (if (> value-abs pivot-abs-max) (set pivot-abs-max value-abs pivot-row row-index))
      (set row-index (+ row-index 1)))
    (if (!= pivot-row k-index)
      (begin
        (set column-index 0)
        (while (< column-index line-count)
          (set
            index-a (convert-type (+ (* pivot-row line-count) column-index) sp-time-t)
            index-b (convert-type (+ (* k-index line-count) column-index) sp-time-t)
            temp-real (array-get matrix-real index-a)
            temp-imag (array-get matrix-imag index-a)
            (array-get matrix-real index-a) (array-get matrix-real index-b)
            (array-get matrix-imag index-a) (array-get matrix-imag index-b)
            (array-get matrix-real index-b) temp-real
            (array-get matrix-imag index-b) temp-imag
            column-index (+ column-index 1)))
        (set
          temp-index (array-get pivot-index-list pivot-row)
          (array-get pivot-index-list pivot-row) (array-get pivot-index-list k-index)
          (array-get pivot-index-list k-index) temp-index)))
    (set row-index (+ k-index 1))
    (while (< row-index line-count)
      (set
        index-a (convert-type (+ (* row-index line-count) k-index) sp-time-t)
        index-b (convert-type (+ (* k-index line-count) k-index) sp-time-t))
      (sp-reverb-complex-divide (array-get matrix-real index-a) (array-get matrix-imag index-a)
        (array-get matrix-real index-b) (array-get matrix-imag index-b) &scale-real &scale-imag)
      (set
        (array-get matrix-real index-a) scale-real
        (array-get matrix-imag index-a) scale-imag
        column-index (+ k-index 1))
      (while (< column-index line-count)
        (set
          index-a (convert-type (+ (* row-index line-count) column-index) sp-time-t)
          index-b (convert-type (+ (* k-index line-count) column-index) sp-time-t)
          prod-real
          (- (* scale-real (array-get matrix-real index-b))
            (* scale-imag (array-get matrix-imag index-b)))
          prod-imag
          (+ (* scale-real (array-get matrix-imag index-b))
            (* scale-imag (array-get matrix-real index-b)))
          (array-get matrix-real index-a) (- (array-get matrix-real index-a) prod-real)
          (array-get matrix-imag index-a) (- (array-get matrix-imag index-a) prod-imag)
          column-index (+ column-index 1)))
      (set row-index (+ row-index 1)))
    (set k-index (+ k-index 1))))

(define
  (sp-reverb-lower-upper-solve line-count matrix-real matrix-imag pivot-list right-real right-imag solution-real solution-imag)
  (void sp-time-t sp-sample-t* sp-sample-t* sp-time-t* sp-sample-t* sp-sample-t* sp-sample-t* sp-sample-t*)
  (declare
    row-number sp-time-t
    column-number sp-time-t
    position sp-time-t
    element-real sp-sample-t
    element-imag sp-sample-t
    quotient-real sp-sample-t
    quotient-imag sp-sample-t)
  (set row-number 0)
  (while (< row-number line-count)
    (set
      (array-get solution-real row-number) (array-get right-real (array-get pivot-list row-number))
      (array-get solution-imag row-number) (array-get right-imag (array-get pivot-list row-number))
      row-number (+ row-number 1)))
  (set row-number 1)
  (while (< row-number line-count)
    (set column-number 0)
    (while (< column-number row-number)
      (set
        position (convert-type (+ (* row-number line-count) column-number) sp-time-t)
        element-real (array-get matrix-real position)
        element-imag (array-get matrix-imag position)
        (array-get solution-real row-number)
        (- (array-get solution-real row-number)
          (- (* element-real (array-get solution-real column-number))
            (* element-imag (array-get solution-imag column-number))))
        (array-get solution-imag row-number)
        (- (array-get solution-imag row-number)
          (+ (* element-real (array-get solution-imag column-number))
            (* element-imag (array-get solution-real column-number))))
        column-number (+ column-number 1)))
    (set row-number (+ row-number 1)))
  (set row-number line-count)
  (while (> row-number 0)
    (set row-number (- row-number 1))
    (set column-number (+ row-number 1))
    (while (< column-number line-count)
      (set
        position (convert-type (+ (* row-number line-count) column-number) sp-time-t)
        element-real (array-get matrix-real position)
        element-imag (array-get matrix-imag position)
        (array-get solution-real row-number)
        (- (array-get solution-real row-number)
          (- (* element-real (array-get solution-real column-number))
            (* element-imag (array-get solution-imag column-number))))
        (array-get solution-imag row-number)
        (- (array-get solution-imag row-number)
          (+ (* element-real (array-get solution-imag column-number))
            (* element-imag (array-get solution-real column-number))))
        column-number (+ column-number 1)))
    (set position (convert-type (+ (* row-number line-count) row-number) sp-time-t))
    (sp-reverb-complex-divide (array-get solution-real row-number)
      (array-get solution-imag row-number) (array-get matrix-real position)
      (array-get matrix-imag position) (address-of quotient-real) (address-of quotient-imag))
    (set
      (array-get solution-real row-number) quotient-real
      (array-get solution-imag row-number) quotient-imag)))

(define
  (sp-reverb-power-iteration-dominant-eigenpair line-count matrix-real matrix-imag eigenvalue-real eigenvalue-imag eigenvector-real eigenvector-imag iteration-limit)
  (void sp-time-t sp-sample-t* sp-sample-t* sp-sample-t* sp-sample-t* sp-sample-t* sp-sample-t* sp-time-t)
  (declare
    next-vector-real sp-sample-t*
    next-vector-imag sp-sample-t*
    iteration-number sp-time-t
    row-number sp-time-t
    column-number sp-time-t
    position sp-time-t
    sum-real sp-sample-t
    sum-imag sp-sample-t
    norm-square sp-sample-t
    norm-value sp-sample-t
    inverse-norm-value sp-sample-t
    row-sum-real sp-sample-t
    row-sum-imag sp-sample-t)
  (set
    next-vector-real
    (__builtin-alloca
      (convert-type (* line-count (convert-type (sizeof sp-sample-t) sp-time-t)) size-t))
    next-vector-imag
    (__builtin-alloca
      (convert-type (* line-count (convert-type (sizeof sp-sample-t) sp-time-t)) size-t)))
  (set row-number 0)
  (while (< row-number line-count)
    (set
      (array-get eigenvector-real row-number) 1.0
      (array-get eigenvector-imag row-number) 0.0
      row-number (+ row-number 1)))
  (set norm-square 0.0 row-number 0)
  (while (< row-number line-count)
    (set
      norm-square
      (+ norm-square
        (+ (* (array-get eigenvector-real row-number) (array-get eigenvector-real row-number))
          (* (array-get eigenvector-imag row-number) (array-get eigenvector-imag row-number))))
      row-number (+ row-number 1)))
  (set norm-value (sqrt norm-square) inverse-norm-value (/ 1.0 norm-value) row-number 0)
  (while (< row-number line-count)
    (set
      (array-get eigenvector-real row-number)
      (* (array-get eigenvector-real row-number) inverse-norm-value)
      (array-get eigenvector-imag row-number)
      (* (array-get eigenvector-imag row-number) inverse-norm-value)
      row-number (+ row-number 1)))
  (set iteration-number 0)
  (while (< iteration-number iteration-limit)
    (set row-number 0)
    (while (< row-number line-count)
      (set sum-real 0.0 sum-imag 0.0 column-number 0)
      (while (< column-number line-count)
        (set
          position (convert-type (+ (* row-number line-count) column-number) sp-time-t)
          sum-real
          (+ sum-real
            (- (* (array-get matrix-real position) (array-get eigenvector-real column-number))
              (* (array-get matrix-imag position) (array-get eigenvector-imag column-number))))
          sum-imag
          (+ sum-imag
            (+ (* (array-get matrix-real position) (array-get eigenvector-imag column-number))
              (* (array-get matrix-imag position) (array-get eigenvector-real column-number))))
          column-number (+ column-number 1)))
      (set
        (array-get next-vector-real row-number) sum-real
        (array-get next-vector-imag row-number) sum-imag
        row-number (+ row-number 1)))
    (set norm-square 0.0 row-number 0)
    (while (< row-number line-count)
      (set
        norm-square
        (+ norm-square
          (+ (* (array-get next-vector-real row-number) (array-get next-vector-real row-number))
            (* (array-get next-vector-imag row-number) (array-get next-vector-imag row-number))))
        row-number (+ row-number 1)))
    (set norm-value (sqrt norm-square) inverse-norm-value (/ 1.0 norm-value) row-number 0)
    (while (< row-number line-count)
      (set
        (array-get eigenvector-real row-number)
        (* (array-get next-vector-real row-number) inverse-norm-value)
        (array-get eigenvector-imag row-number)
        (* (array-get next-vector-imag row-number) inverse-norm-value)
        row-number (+ row-number 1)))
    (set iteration-number (+ iteration-number 1)))
  (set sum-real 0.0 sum-imag 0.0 row-number 0)
  (while (< row-number line-count)
    (set row-sum-real 0.0 row-sum-imag 0.0 column-number 0)
    (while (< column-number line-count)
      (set
        position (convert-type (+ (* row-number line-count) column-number) sp-time-t)
        row-sum-real
        (+ row-sum-real
          (- (* (array-get matrix-real position) (array-get eigenvector-real column-number))
            (* (array-get matrix-imag position) (array-get eigenvector-imag column-number))))
        row-sum-imag
        (+ row-sum-imag
          (+ (* (array-get matrix-real position) (array-get eigenvector-imag column-number))
            (* (array-get matrix-imag position) (array-get eigenvector-real column-number))))
        column-number (+ column-number 1)))
    (set
      sum-real
      (+ sum-real
        (+ (* (array-get eigenvector-real row-number) row-sum-real)
          (* (array-get eigenvector-imag row-number) row-sum-imag)))
      sum-imag
      (+ sum-imag
        (- (* (array-get eigenvector-real row-number) row-sum-imag)
          (* (array-get eigenvector-imag row-number) row-sum-real)))
      row-number (+ row-number 1)))
  (set *eigenvalue-real sum-real *eigenvalue-imag sum-imag))

(define (sp-reverb-eigen-equation-value config radius angle out-real out-imag)
  (void sp-reverb-late-config-t* sp-sample-t sp-sample-t sp-sample-t* sp-sample-t*)
  (declare
    line-count sp-time-t
    feedback-real sp-sample-t*
    feedback-imag sp-sample-t*
    matrix-real sp-sample-t*
    matrix-imag sp-sample-t*
    pivot-list sp-time-t*
    visited-list uint8-t*
    row-number sp-time-t
    position sp-time-t
    sign-value int32-t
    cycle-begin sp-time-t
    cycle-point sp-time-t
    cycle-size sp-time-t
    determinant-real sp-sample-t
    determinant-imag sp-sample-t
    diagonal-real sp-sample-t
    diagonal-imag sp-sample-t
    prev-real sp-sample-t
    prev-imag sp-sample-t)
  (set line-count config:delay-count)
  (set
    feedback-real
    (__builtin-alloca (convert-type (* line-count line-count (sizeof sp-sample-t)) size-t))
    feedback-imag
    (__builtin-alloca (convert-type (* line-count line-count (sizeof sp-sample-t)) size-t))
    matrix-real
    (__builtin-alloca (convert-type (* line-count line-count (sizeof sp-sample-t)) size-t))
    matrix-imag
    (__builtin-alloca (convert-type (* line-count line-count (sizeof sp-sample-t)) size-t))
    pivot-list (__builtin-alloca (convert-type (* line-count (sizeof sp-time-t)) size-t))
    visited-list (__builtin-alloca (convert-type (* line-count (sizeof uint8-t)) size-t)))
  (sp-reverb-build-feedback-matrix-from-polar config radius angle feedback-real feedback-imag)
  (sp-reverb-form-identity-minus-feedback line-count feedback-real
    feedback-imag matrix-real matrix-imag)
  (sp-reverb-lower-upper-factorization line-count matrix-real matrix-imag pivot-list)
  (set row-number 0)
  (while (< row-number line-count)
    (set (array-get visited-list row-number) 0 row-number (+ row-number 1)))
  (set sign-value 1 cycle-begin 0)
  (while (< cycle-begin line-count)
    (if (= (array-get visited-list cycle-begin) 0)
      (begin
        (set cycle-point cycle-begin cycle-size 0)
        (while (= (array-get visited-list cycle-point) 0)
          (set
            (array-get visited-list cycle-point) 1
            cycle-point (array-get pivot-list cycle-point)
            cycle-size (+ cycle-size 1)))
        (if (> cycle-size 0)
          (if (= (modulo (- cycle-size 1) 2) 1) (set sign-value (* sign-value -1))))))
    (set cycle-begin (+ cycle-begin 1)))
  (set determinant-real 1.0 determinant-imag 0.0 row-number 0)
  (while (< row-number line-count)
    (set
      position (convert-type (+ (* row-number line-count) row-number) sp-time-t)
      diagonal-real (array-get matrix-real position)
      diagonal-imag (array-get matrix-imag position)
      prev-real determinant-real
      prev-imag determinant-imag
      determinant-real (- (* prev-real diagonal-real) (* prev-imag diagonal-imag))
      determinant-imag (+ (* prev-real diagonal-imag) (* prev-imag diagonal-real))
      row-number (+ row-number 1)))
  (if (< sign-value 0)
    (set determinant-real (- 0.0 determinant-real) determinant-imag (- 0.0 determinant-imag)))
  (set *out-real determinant-real *out-imag determinant-imag))

(define
  (sp-reverb-eigen-equation-jacobian-finite-difference config radius angle out-dfr-dr out-dfr-dtheta out-dfi-dr out-dfi-dtheta)
  (void sp-reverb-late-config-t* sp-sample-t sp-sample-t sp-sample-t* sp-sample-t* sp-sample-t* sp-sample-t*)
  (declare
    radius-step sp-sample-t
    angle-step sp-sample-t
    radius-up sp-sample-t
    radius-down sp-sample-t
    angle-up sp-sample-t
    angle-down sp-sample-t
    real-up-radius sp-sample-t
    imag-up-radius sp-sample-t
    real-down-radius sp-sample-t
    imag-down-radius sp-sample-t
    real-up-angle sp-sample-t
    imag-up-angle sp-sample-t
    real-down-angle sp-sample-t
    imag-down-angle sp-sample-t
    span-radius sp-sample-t
    span-angle sp-sample-t)
  (set radius-step radius)
  (if (< radius-step 1.0) (set radius-step 1.0))
  (set
    radius-step (* radius-step 1.0e-4)
    angle-step 1.0e-4
    radius-up (+ radius radius-step)
    radius-down (- radius radius-step)
    angle-up (+ angle angle-step)
    angle-down (- angle angle-step))
  (sp-reverb-eigen-equation-value config radius-up angle &real-up-radius &imag-up-radius)
  (sp-reverb-eigen-equation-value config radius-down angle &real-down-radius &imag-down-radius)
  (sp-reverb-eigen-equation-value config radius angle-up &real-up-angle &imag-up-angle)
  (sp-reverb-eigen-equation-value config radius angle-down &real-down-angle &imag-down-angle)
  (set
    span-radius (- radius-up radius-down)
    span-angle (- angle-up angle-down)
    *out-dfr-dr (/ (- real-up-radius real-down-radius) span-radius)
    *out-dfi-dr (/ (- imag-up-radius imag-down-radius) span-radius)
    *out-dfr-dtheta (/ (- real-up-angle real-down-angle) span-angle)
    *out-dfi-dtheta (/ (- imag-up-angle imag-down-angle) span-angle)))

(define
  (sp-reverb-newton-step-on-eigen-equation radius-current angle-current real-derivative-radius real-derivative-angle imag-derivative-radius imag-derivative-angle real-value imag-value radius-next angle-next)
  (void sp-sample-t sp-sample-t sp-sample-t sp-sample-t sp-sample-t sp-sample-t sp-sample-t sp-sample-t sp-sample-t* sp-sample-t*)
  (declare jacobian-determinant sp-sample-t radius-update sp-sample-t angle-update sp-sample-t)
  (set
    jacobian-determinant
    (- (* real-derivative-radius imag-derivative-angle)
      (* real-derivative-angle imag-derivative-radius))
    radius-update
    (/ (- (* imag-derivative-angle real-value) (* real-derivative-angle imag-value))
      jacobian-determinant)
    angle-update
    (/ (- (* imag-derivative-radius real-value) (* real-derivative-radius imag-value))
      jacobian-determinant)
    radius-update (- 0.0 radius-update)
    *radius-next (+ radius-current radius-update)
    *angle-next (+ angle-current angle-update)))

(define (sp-reverb-late-modal config) (sp-reverb-modal-set-t sp-reverb-late-config-t*)
  (declare
    modal-set sp-reverb-modal-set-t
    delay-line-count sp-time-t
    mode-count sp-time-t
    mode-index sp-time-t
    list-size size-t
    radius-current sp-sample-t
    angle-current sp-sample-t
    value-real sp-sample-t
    value-imag sp-sample-t
    derivative-real-radius sp-sample-t
    derivative-real-angle sp-sample-t
    derivative-imag-radius sp-sample-t
    derivative-imag-angle sp-sample-t
    radius-next sp-sample-t
    angle-next sp-sample-t
    iteration-index sp-time-t
    iteration-limit sp-time-t
    period-float sp-sample-t
    period-integer sp-time-t
    decay-float sp-sample-t
    decay-integer sp-time-t)
  (set modal-set.mode-list NULL modal-set.mode-count 0)
  (set delay-line-count config:delay-count mode-count delay-line-count)
  (if (= mode-count 0) (return modal-set))
  (set
    list-size (convert-type mode-count size-t)
    modal-set.mode-list
    (convert-type (malloc (* list-size (sizeof sp-reverb-mode-t))) sp-reverb-mode-t*))
  (if (= modal-set.mode-list NULL) (return modal-set))
  (set iteration-limit 16 mode-index 0)
  (while (< mode-index mode-count)
    (set
      radius-current 0.9
      angle-current
      (/ (* 2.0 sp-pi (+ (convert-type mode-index sp-sample-t) 0.5))
        (convert-type mode-count sp-sample-t))
      iteration-index 0)
    (while (< iteration-index iteration-limit)
      (sp-reverb-eigen-equation-value config radius-current
        angle-current (address-of value-real) (address-of value-imag))
      (sp-reverb-eigen-equation-jacobian-finite-difference config radius-current
        angle-current (address-of derivative-real-radius) (address-of derivative-real-angle)
        (address-of derivative-imag-radius) (address-of derivative-imag-angle))
      (sp-reverb-newton-step-on-eigen-equation radius-current angle-current
        derivative-real-radius derivative-real-angle derivative-imag-radius
        derivative-imag-angle value-real value-imag (address-of radius-next) (address-of angle-next))
      (set
        radius-current radius-next
        angle-current angle-next
        iteration-index (+ iteration-index 1)))
    (if (<= radius-current 0.0) (set radius-current 1.0e-6))
    (if (>= radius-current 1.0) (set radius-current (- 1.0 1.0e-6)))
    (if (<= angle-current 0.0) (set angle-current 1.0e-6))
    (set period-float (/ (* 2.0 sp-pi) angle-current))
    (if (< period-float 1.0) (set period-float 1.0))
    (set period-integer (convert-type (llround period-float) sp-time-t))
    (set decay-float (/ 1.0 (- (log radius-current))))
    (if (< decay-float 1.0) (set decay-float 1.0))
    (set decay-integer (convert-type (llround decay-float) sp-time-t))
    (set
      (struct-get (array-get modal-set.mode-list mode-index) period) period-integer
      (struct-get (array-get modal-set.mode-list mode-index) decay) decay-integer
      (struct-get (array-get modal-set.mode-list mode-index) amplitude) config:strength
      (struct-get (array-get modal-set.mode-list mode-index) phase) 0
      mode-index (+ mode-index 1)))
  (set modal-set.mode-count mode-count)
  (return modal-set))

(define (sp-reverb-build-state-excitation config position out-real out-imag)
  (void sp-reverb-late-config-t* sp-reverb-position-t* sp-sample-t* sp-sample-t*)
  (declare
    delay-index sp-time-t
    delay-count sp-time-t
    dimension-index sp-time-t
    dimension-count sp-time-t
    source-unit (array sp-sample-t sph-reverb-position-max-dimensions)
    source-norm-square sp-sample-t
    source-norm sp-sample-t
    inverse-source-norm sp-sample-t
    direction-norm-square sp-sample-t
    direction-norm sp-sample-t
    inverse-direction-norm sp-sample-t
    cosine-value sp-sample-t
    energy-sum sp-sample-t
    norm-value sp-sample-t
    inverse-norm-value sp-sample-t
    weight sp-sample-t)
  (set
    delay-count config:delay-count
    dimension-count config:state-dimension-count
    source-norm-square 0.0
    dimension-index 0)
  (while (< dimension-index dimension-count)
    (set
      (array-get source-unit dimension-index) (array-get position:values dimension-index)
      source-norm-square
      (+ source-norm-square
        (* (array-get source-unit dimension-index) (array-get source-unit dimension-index)))
      dimension-index (+ dimension-index 1)))
  (if (> source-norm-square 0.0)
    (begin
      (set
        source-norm (sqrt source-norm-square)
        inverse-source-norm (/ 1.0 source-norm)
        dimension-index 0)
      (while (< dimension-index dimension-count)
        (set
          (array-get source-unit dimension-index)
          (* (array-get source-unit dimension-index) inverse-source-norm)
          dimension-index (+ dimension-index 1))))
    (begin
      (set dimension-index 0)
      (while (< dimension-index dimension-count)
        (set (array-get source-unit dimension-index) 0.0 dimension-index (+ dimension-index 1)))))
  (set delay-index 0)
  (while (< delay-index delay-count)
    (set direction-norm-square 0.0 dimension-index 0)
    (while (< dimension-index dimension-count)
      (declare value sp-sample-t)
      (set
        value (array-get config:state-directions (+ (* delay-index dimension-count) dimension-index))
        direction-norm-square (+ direction-norm-square (* value value))
        dimension-index (+ dimension-index 1)))
    (if (and (> direction-norm-square 0.0) (> source-norm-square 0.0))
      (begin
        (set
          direction-norm (sqrt direction-norm-square)
          inverse-direction-norm (/ 1.0 direction-norm)
          cosine-value 0.0
          dimension-index 0)
        (while (< dimension-index dimension-count)
          (declare state-value sp-sample-t)
          (set
            state-value
            (*
              (array-get config:state-directions
                (+ (* delay-index dimension-count) dimension-index))
              inverse-direction-norm)
            cosine-value (+ cosine-value (* state-value (array-get source-unit dimension-index)))
            dimension-index (+ dimension-index 1)))
        (if (< cosine-value 0.0) (set cosine-value 0.0))
        (set weight cosine-value))
      (set weight 1.0))
    (set
      (array-get out-real delay-index) weight
      (array-get out-imag delay-index) 0.0
      delay-index (+ delay-index 1)))
  (set energy-sum 0.0 delay-index 0)
  (while (< delay-index delay-count)
    (set
      energy-sum (+ energy-sum (* (array-get out-real delay-index) (array-get out-real delay-index)))
      delay-index (+ delay-index 1)))
  (if (> energy-sum 0.0)
    (begin
      (set norm-value (sqrt energy-sum) inverse-norm-value (/ 1.0 norm-value) delay-index 0)
      (while (< delay-index delay-count)
        (set
          (array-get out-real delay-index) (* (array-get out-real delay-index) inverse-norm-value)
          delay-index (+ delay-index 1))))
    (if (> delay-count 0)
      (begin
        (set
          norm-value (sqrt (convert-type delay-count sp-sample-t))
          inverse-norm-value (/ 1.0 norm-value)
          delay-index 0)
        (while (< delay-index delay-count)
          (set (array-get out-real delay-index) inverse-norm-value delay-index (+ delay-index 1)))))))

(define (sp-reverb-build-state-projection config layout position channel-index out-real out-imag)
  (void sp-reverb-late-config-t* sp-reverb-layout-t* sp-reverb-position-t* sp-channel-count-t sp-sample-t* sp-sample-t*)
  (declare
    delay-index sp-time-t
    delay-count sp-time-t
    dimension-index sp-time-t
    dimension-count sp-time-t
    basis-length sp-time-t
    channel-unit (array sp-sample-t sph-reverb-position-max-dimensions)
    channel-norm-square sp-sample-t
    channel-norm sp-sample-t
    inverse-channel-norm sp-sample-t
    direction-norm-square sp-sample-t
    direction-norm sp-sample-t
    inverse-direction-norm sp-sample-t
    cosine-value sp-sample-t
    weight sp-sample-t
    energy-sum sp-sample-t
    norm-value sp-sample-t
    inverse-norm-value sp-sample-t
    pan-position-normalized sp-sample-t
    pan-gain sp-sample-t)
  (set
    delay-count config:delay-count
    dimension-count config:state-dimension-count
    basis-length layout:basis-length
    channel-norm-square 0.0
    dimension-index 0)
  (while (< dimension-index dimension-count)
    (declare channel-value sp-sample-t)
    (if (< dimension-index basis-length)
      (set channel-value
        (array-get layout:bases
          (+ (* (convert-type channel-index sp-time-t) basis-length) dimension-index)))
      (set channel-value 0.0))
    (set
      (array-get channel-unit dimension-index) channel-value
      channel-norm-square (+ channel-norm-square (* channel-value channel-value))
      dimension-index (+ dimension-index 1)))
  (if (> channel-norm-square 0.0)
    (begin
      (set
        channel-norm (sqrt channel-norm-square)
        inverse-channel-norm (/ 1.0 channel-norm)
        dimension-index 0)
      (while (< dimension-index dimension-count)
        (set
          (array-get channel-unit dimension-index)
          (* (array-get channel-unit dimension-index) inverse-channel-norm)
          dimension-index (+ dimension-index 1))))
    (begin
      (set dimension-index 0)
      (while (< dimension-index dimension-count)
        (set (array-get channel-unit dimension-index) 0.0 dimension-index (+ dimension-index 1)))))
  (if (> dimension-count 0)
    (begin
      (set pan-position-normalized (* (+ (array-get position:values 0) 1.0) 0.5))
      (if (< pan-position-normalized 0.0) (set pan-position-normalized 0.0))
      (if (> pan-position-normalized 1.0) (set pan-position-normalized 1.0))
      (if (> basis-length 0)
        (begin
          (declare channel-axis-value sp-sample-t)
          (set channel-axis-value
            (array-get layout:bases (* (convert-type channel-index sp-time-t) basis-length)))
          (if (< channel-axis-value 0.0) (set pan-gain (sqrt (- 1.0 pan-position-normalized)))
            (if (> channel-axis-value 0.0) (set pan-gain (sqrt pan-position-normalized))
              (set pan-gain 1.0))))
        (set pan-gain 1.0)))
    (set pan-gain 1.0))
  (set delay-index 0)
  (while (< delay-index delay-count)
    (set direction-norm-square 0.0 dimension-index 0)
    (while (< dimension-index dimension-count)
      (declare value sp-sample-t)
      (set
        value (array-get config:state-directions (+ (* delay-index dimension-count) dimension-index))
        direction-norm-square (+ direction-norm-square (* value value))
        dimension-index (+ dimension-index 1)))
    (if (and (> direction-norm-square 0.0) (> channel-norm-square 0.0))
      (begin
        (set
          direction-norm (sqrt direction-norm-square)
          inverse-direction-norm (/ 1.0 direction-norm)
          cosine-value 0.0
          dimension-index 0)
        (while (< dimension-index dimension-count)
          (declare state-value sp-sample-t)
          (set
            state-value
            (*
              (array-get config:state-directions
                (+ (* delay-index dimension-count) dimension-index))
              inverse-direction-norm)
            cosine-value (+ cosine-value (* state-value (array-get channel-unit dimension-index)))
            dimension-index (+ dimension-index 1)))
        (if (< cosine-value 0.0) (set cosine-value 0.0))
        (set weight cosine-value))
      (set weight 1.0))
    (set
      (array-get out-real delay-index) weight
      (array-get out-imag delay-index) 0.0
      delay-index (+ delay-index 1)))
  (set energy-sum 0.0 delay-index 0)
  (while (< delay-index delay-count)
    (set
      energy-sum (+ energy-sum (* (array-get out-real delay-index) (array-get out-real delay-index)))
      delay-index (+ delay-index 1)))
  (if (> energy-sum 0.0)
    (begin
      (set norm-value (sqrt energy-sum) inverse-norm-value (/ 1.0 norm-value) delay-index 0)
      (while (< delay-index delay-count)
        (set
          (array-get out-real delay-index)
          (* (array-get out-real delay-index) inverse-norm-value pan-gain)
          delay-index (+ delay-index 1))))
    (if (> delay-count 0)
      (begin
        (set
          norm-value (sqrt (convert-type delay-count sp-sample-t))
          inverse-norm-value (/ 1.0 norm-value)
          delay-index 0)
        (while (< delay-index delay-count)
          (set
            (array-get out-real delay-index) (* inverse-norm-value pan-gain)
            delay-index (+ delay-index 1)))))))

(define
  (sp-reverb-null-vector-of-shifted-matrix line-count a-real a-imag use-transpose out-real out-imag)
  (void sp-time-t sp-sample-t* sp-sample-t* int sp-sample-t* sp-sample-t*)
  (declare
    reduced-count sp-time-t
    pivot-index sp-time-t
    row-index sp-time-t
    column-index sp-time-t
    reduced-row sp-time-t
    reduced-column sp-time-t
    index-full sp-time-t
    index-reduced sp-time-t
    reduced-real sp-sample-t*
    reduced-imag sp-sample-t*
    right-real sp-sample-t*
    right-imag sp-sample-t*
    solution-real sp-sample-t*
    solution-imag sp-sample-t*
    pivot-list sp-time-t*
    norm-square sp-sample-t
    norm-value sp-sample-t
    inverse-norm-value sp-sample-t)
  (if (= line-count 0) (return))
  (if (= line-count 1) (begin (set (array-get out-real 0) 1.0 (array-get out-imag 0) 0.0) (return)))
  (set reduced-count (- line-count 1))
  (set
    reduced-real
    (__builtin-alloca (convert-type (* reduced-count reduced-count (sizeof sp-sample-t)) size-t))
    reduced-imag
    (__builtin-alloca (convert-type (* reduced-count reduced-count (sizeof sp-sample-t)) size-t))
    right-real (__builtin-alloca (convert-type (* reduced-count (sizeof sp-sample-t)) size-t))
    right-imag (__builtin-alloca (convert-type (* reduced-count (sizeof sp-sample-t)) size-t))
    solution-real (__builtin-alloca (convert-type (* reduced-count (sizeof sp-sample-t)) size-t))
    solution-imag (__builtin-alloca (convert-type (* reduced-count (sizeof sp-sample-t)) size-t))
    pivot-list (__builtin-alloca (convert-type (* reduced-count (sizeof sp-time-t)) size-t)))
  (set pivot-index 0)
  (set reduced-row 0 row-index 0)
  (while (< row-index line-count)
    (if (!= row-index pivot-index)
      (begin
        (set reduced-column 0 column-index 0)
        (while (< column-index line-count)
          (if (!= column-index pivot-index)
            (begin
              (if (= use-transpose 0)
                (set index-full (convert-type (+ (* row-index line-count) column-index) sp-time-t))
                (set index-full (convert-type (+ (* column-index line-count) row-index) sp-time-t)))
              (set index-reduced
                (convert-type (+ (* reduced-row reduced-count) reduced-column) sp-time-t))
              (set
                (array-get reduced-real index-reduced) (array-get a-real index-full)
                (array-get reduced-imag index-reduced) (array-get a-imag index-full)
                reduced-column (+ reduced-column 1)))
            (set column-index (+ column-index 1)))
          (if (= use-transpose 0)
            (set index-full (convert-type (+ (* row-index line-count) pivot-index) sp-time-t))
            (set index-full (convert-type (+ (* pivot-index line-count) row-index) sp-time-t)))
          (set
            (array-get right-real reduced-row) (- 0.0 (array-get a-real index-full))
            (array-get right-imag reduced-row) (- 0.0 (array-get a-imag index-full))
            reduced-row (+ reduced-row 1))))
      (set row-index (+ row-index 1)))
    (sp-reverb-lower-upper-factorization reduced-count reduced-real reduced-imag pivot-list)
    (sp-reverb-lower-upper-solve reduced-count reduced-real
      reduced-imag pivot-list right-real right-imag solution-real solution-imag)
    (set row-index 0)
    (while (< row-index line-count)
      (if (= row-index pivot-index)
        (set (array-get out-real row-index) 1.0 (array-get out-imag row-index) 0.0)
        (if (< row-index pivot-index)
          (set
            (array-get out-real row-index) (array-get solution-real row-index)
            (array-get out-imag row-index) (array-get solution-imag row-index))
          (set
            (array-get out-real row-index) (array-get solution-real (- row-index 1))
            (array-get out-imag row-index) (array-get solution-imag (- row-index 1)))))
      (set row-index (+ row-index 1)))
    (set norm-square 0.0 row-index 0)
    (while (< row-index line-count)
      (set
        norm-square
        (+ norm-square (* (array-get out-real row-index) (array-get out-real row-index))
          (* (array-get out-imag row-index) (array-get out-imag row-index)))
        row-index (+ row-index 1)))
    (if (> norm-square 0.0)
      (begin
        (set norm-value (sqrt norm-square) inverse-norm-value (/ 1.0 norm-value) row-index 0)
        (while (< row-index line-count)
          (set
            (array-get out-real row-index) (* (array-get out-real row-index) inverse-norm-value)
            (array-get out-imag row-index) (* (array-get out-imag row-index) inverse-norm-value)
            row-index (+ row-index 1)))))))

(define (sp-reverb-right-eigenvector-at-pole config radius angle out-real out-imag)
  (void sp-reverb-late-config-t* sp-sample-t sp-sample-t sp-sample-t* sp-sample-t*)
  (declare
    line-count sp-time-t
    feedback-real sp-sample-t*
    feedback-imag sp-sample-t*
    a-real sp-sample-t*
    a-imag sp-sample-t*)
  (set
    line-count config:delay-count
    feedback-real
    (__builtin-alloca (convert-type (* line-count line-count (sizeof sp-sample-t)) size-t))
    feedback-imag
    (__builtin-alloca (convert-type (* line-count line-count (sizeof sp-sample-t)) size-t))
    a-real (__builtin-alloca (convert-type (* line-count line-count (sizeof sp-sample-t)) size-t))
    a-imag (__builtin-alloca (convert-type (* line-count line-count (sizeof sp-sample-t)) size-t)))
  (sp-reverb-build-feedback-matrix-from-polar config radius angle feedback-real feedback-imag)
  (sp-reverb-form-identity-minus-feedback line-count feedback-real feedback-imag a-real a-imag)
  (sp-reverb-null-vector-of-shifted-matrix line-count a-real a-imag 0 out-real out-imag))

(define (sp-reverb-left-eigenvector-at-pole config radius angle out-real out-imag)
  (void sp-reverb-late-config-t* sp-sample-t sp-sample-t sp-sample-t* sp-sample-t*)
  (declare
    line-count sp-time-t
    feedback-real sp-sample-t*
    feedback-imag sp-sample-t*
    a-real sp-sample-t*
    a-imag sp-sample-t*)
  (set
    line-count config:delay-count
    feedback-real
    (__builtin-alloca (convert-type (* line-count line-count (sizeof sp-sample-t)) size-t))
    feedback-imag
    (__builtin-alloca (convert-type (* line-count line-count (sizeof sp-sample-t)) size-t))
    a-real (__builtin-alloca (convert-type (* line-count line-count (sizeof sp-sample-t)) size-t))
    a-imag (__builtin-alloca (convert-type (* line-count line-count (sizeof sp-sample-t)) size-t)))
  (sp-reverb-build-feedback-matrix-from-polar config radius angle feedback-real feedback-imag)
  (sp-reverb-form-identity-minus-feedback line-count feedback-real feedback-imag a-real a-imag)
  (sp-reverb-null-vector-of-shifted-matrix line-count a-real a-imag 1 out-real out-imag))

(define (sp-reverb-late-modal-residues config poles layout position out-modes)
  (void sp-reverb-late-config-t* sp-reverb-modal-set-t* sp-reverb-layout-t* sp-reverb-position-t* sp-reverb-channel-modal-set-t*)
  (declare
    state-count sp-time-t
    mode-count sp-time-t
    channel-count sp-channel-count-t
    total-mode-count size-t
    state-excitation-real sp-sample-t*
    state-excitation-imag sp-sample-t*
    right-vector-real sp-sample-t*
    right-vector-imag sp-sample-t*
    left-vector-real sp-sample-t*
    left-vector-imag sp-sample-t*
    channel-projection-real sp-sample-t*
    channel-projection-imag sp-sample-t*
    mode-index sp-time-t
    channel-index sp-channel-count-t
    period-value sp-sample-t
    decay-value sp-sample-t
    angle-value sp-sample-t
    radius-value sp-sample-t
    sum-real sp-sample-t
    sum-imag sp-sample-t
    s-real sp-sample-t
    s-imag sp-sample-t
    n-real sp-sample-t
    n-imag sp-sample-t
    t-real sp-sample-t
    t-imag sp-sample-t
    st-real sp-sample-t
    st-imag sp-sample-t
    alpha-real sp-sample-t
    alpha-imag sp-sample-t
    amplitude-value sp-sample-t
    phase-angle sp-sample-t
    phase-samples sp-time-t
    state-index sp-time-t
    mode-array sp-reverb-mode-t*)
  (set
    out-modes:mode-list NULL
    out-modes:mode-count 0
    out-modes:channel-count 0
    state-count config:delay-count
    mode-count poles:mode-count
    channel-count layout:channel-count)
  (if (= state-count 0) (return))
  (if (= mode-count 0) (return))
  (if (= channel-count 0) (return))
  (set
    total-mode-count (* (convert-type mode-count size-t) (convert-type channel-count size-t))
    mode-array
    (convert-type (malloc (* total-mode-count (sizeof sp-reverb-mode-t))) sp-reverb-mode-t*))
  (if (= mode-array NULL) (return))
  (set
    state-excitation-real
    (convert-type (__builtin-alloca (* (convert-type state-count size-t) (sizeof sp-sample-t)))
      sp-sample-t*)
    state-excitation-imag
    (convert-type (__builtin-alloca (* (convert-type state-count size-t) (sizeof sp-sample-t)))
      sp-sample-t*)
    right-vector-real
    (convert-type (__builtin-alloca (* (convert-type state-count size-t) (sizeof sp-sample-t)))
      sp-sample-t*)
    right-vector-imag
    (convert-type (__builtin-alloca (* (convert-type state-count size-t) (sizeof sp-sample-t)))
      sp-sample-t*)
    left-vector-real
    (convert-type (__builtin-alloca (* (convert-type state-count size-t) (sizeof sp-sample-t)))
      sp-sample-t*)
    left-vector-imag
    (convert-type (__builtin-alloca (* (convert-type state-count size-t) (sizeof sp-sample-t)))
      sp-sample-t*)
    channel-projection-real
    (convert-type (__builtin-alloca (* (convert-type state-count size-t) (sizeof sp-sample-t)))
      sp-sample-t*)
    channel-projection-imag
    (convert-type (__builtin-alloca (* (convert-type state-count size-t) (sizeof sp-sample-t)))
      sp-sample-t*))
  (sp-reverb-build-state-excitation config position state-excitation-real state-excitation-imag)
  (set mode-index 0)
  (while (< mode-index mode-count)
    (set
      period-value
      (convert-type (struct-get (array-get poles:mode-list mode-index) period) sp-sample-t)
      decay-value
      (convert-type (struct-get (array-get poles:mode-list mode-index) decay) sp-sample-t))
    (if (<= period-value 0.0) (set period-value 1.0))
    (if (<= decay-value 0.0) (set decay-value 1.0))
    (set angle-value (/ (* 2.0 sp-pi) period-value) radius-value (exp (/ -1.0 decay-value)))
    (sp-reverb-right-eigenvector-at-pole config radius-value
      angle-value right-vector-real right-vector-imag)
    (sp-reverb-left-eigenvector-at-pole config radius-value
      angle-value left-vector-real left-vector-imag)
    (set s-real 0.0 s-imag 0.0 state-index 0)
    (while (< state-index state-count)
      (set
        sum-real
        (-
          (* (array-get left-vector-real state-index) (array-get state-excitation-real state-index))
          (* (array-get left-vector-imag state-index) (array-get state-excitation-imag state-index)))
        sum-imag
        (+
          (* (array-get left-vector-real state-index) (array-get state-excitation-imag state-index))
          (* (array-get left-vector-imag state-index) (array-get state-excitation-real state-index)))
        s-real (+ s-real sum-real)
        s-imag (+ s-imag sum-imag)
        state-index (+ state-index 1)))
    (set n-real 0.0 n-imag 0.0 state-index 0)
    (while (< state-index state-count)
      (set
        sum-real
        (- (* (array-get left-vector-real state-index) (array-get right-vector-real state-index))
          (* (array-get left-vector-imag state-index) (array-get right-vector-imag state-index)))
        sum-imag
        (+ (* (array-get left-vector-real state-index) (array-get right-vector-imag state-index))
          (* (array-get left-vector-imag state-index) (array-get right-vector-real state-index)))
        n-real (+ n-real sum-real)
        n-imag (+ n-imag sum-imag)
        state-index (+ state-index 1)))
    (set st-real 0.0 st-imag 0.0 channel-index 0)
    (while (< channel-index channel-count)
      (sp-reverb-build-state-projection config layout
        position channel-index channel-projection-real channel-projection-imag)
      (set t-real 0.0 t-imag 0.0 state-index 0)
      (while (< state-index state-count)
        (set
          sum-real
          (-
            (* (array-get channel-projection-real state-index)
              (array-get right-vector-real state-index))
            (* (array-get channel-projection-imag state-index)
              (array-get right-vector-imag state-index)))
          sum-imag
          (+
            (* (array-get channel-projection-real state-index)
              (array-get right-vector-imag state-index))
            (* (array-get channel-projection-imag state-index)
              (array-get right-vector-real state-index)))
          t-real (+ t-real sum-real)
          t-imag (+ t-imag sum-imag)
          state-index (+ state-index 1)))
      (set
        st-real (- (* s-real t-real) (* s-imag t-imag))
        st-imag (+ (* s-real t-imag) (* s-imag t-real)))
      (sp-reverb-complex-divide st-real st-imag
        n-real n-imag (address-of alpha-real) (address-of alpha-imag))
      (set
        amplitude-value (sp-reverb-complex-magnitude alpha-real alpha-imag)
        phase-angle (sp-reverb-complex-argument alpha-real alpha-imag)
        phase-samples
        (convert-type (llround (/ (* phase-angle period-value) (* 2.0 sp-pi))) sp-time-t))
      (set
        (struct-get
          (array-get mode-array
            (+ (* (convert-type channel-index size-t) (convert-type mode-count size-t))
              (convert-type mode-index size-t)))
          period)
        (struct-get (array-get poles:mode-list mode-index) period)
        (struct-get
          (array-get mode-array
            (+ (* (convert-type channel-index size-t) (convert-type mode-count size-t))
              (convert-type mode-index size-t)))
          decay)
        (struct-get (array-get poles:mode-list mode-index) decay)
        (struct-get
          (array-get mode-array
            (+ (* (convert-type channel-index size-t) (convert-type mode-count size-t))
              (convert-type mode-index size-t)))
          amplitude)
        amplitude-value
        (struct-get
          (array-get mode-array
            (+ (* (convert-type channel-index size-t) (convert-type mode-count size-t))
              (convert-type mode-index size-t)))
          phase)
        phase-samples channel-index (+ channel-index 1)))
    (set+ mode-index 1))
  (set
    out-modes:mode-list mode-array
    out-modes:mode-count mode-count
    out-modes:channel-count channel-count))
