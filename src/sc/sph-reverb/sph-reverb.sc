(define (sp-reverb-late-lookup frequency-list response-list response-count query-frequency)
  (sp-reverb-response-t sp-sample-t* sp-reverb-response-t* sp-time-t sp-sample-t)
  (declare low sp-time-t high sp-time-t mid sp-time-t t sp-sample-t result sp-reverb-response-t)
  (set low 0 high (- response-count 1))
  (if (<= query-frequency (array-get frequency-list 0)) (return (array-get response-list 0)))
  (if (>= query-frequency (array-get frequency-list high)) (return (array-get response-list high)))
  (while (> (- high low) 1)
    (set mid (+ low (/ (- high low) 2)))
    (if (< query-frequency (array-get frequency-list mid)) (set high mid) (set low mid)))
  (set
    t
    (/ (- query-frequency (array-get frequency-list low))
      (- (array-get frequency-list high) (array-get frequency-list low)))
    result:gain
    (+ (struct-get (struct-get (array-get response-list low) gain))
      (*
        (- (struct-get (array-get response-list high) gain)
          (struct-get (array-get response-list low) gain))
        t))
    result:decay
    (convert-type
      (+ (convert-type (struct-get (array-get response-list low) decay) sp-sample-t)
        (*
          (- (convert-type (struct-get (array-get response-list high) decay) sp-sample-t)
            (convert-type (struct-get (array-get response-list low) decay) sp-sample-t))
          t))
      sp-time-t)
    result:phase
    (convert-type
      (+ (convert-type (struct-get (array-get response-list low) phase) sp-sample-t)
        (*
          (- (convert-type (struct-get (array-get response-list high) phase) sp-sample-t)
            (convert-type (struct-get (array-get response-list low) phase) sp-sample-t))
          t))
      sp-time-t))
  (return result))

(define (sp-reverb-late-project response layout channel-gains channel-count)
  (void sp-reverb-response-t* sp-reverb-layout-t* sp-sample-t* sp-time-t)
  (declare channel-index sp-time-t basis-index sp-time-t base-index sp-time-t sum sp-sample-t)
  (set channel-index 0 base-index 0)
  (while (< channel-index layout:channel-count)
    (set sum 0.0 basis-index 0)
    (while (< basis-index layout:basis-length)
      (set
        sum (+ sum (array-get layout:bases base-index))
        base-index (+ base-index 1)
        basis-index (+ basis-index 1)))
    (set
      (array-get channel-gains channel-index) (* response:gain sum)
      channel-index (+ channel-index 1))))

(define (sp-reverb-band-gain-at config frequency) (sp-sample-t sp-reverb-late-config-t* sp-time-t)
  (declare band-index sp-time-t weight sp-sample-t)
  (if (== config:band-count 0) (return 0.0))
  (if (<= frequency (array-get config:band-frequencies 0)) (return (array-get config:band-gains 0)))
  (if (>= frequency (array-get config:band-frequencies (- config:band-count 1)))
    (return (array-get config:band-gains (- config:band-count 1))))
  (set band-index 0)
  (while (< (array-get config:band-frequencies (+ band-index 1)) frequency)
    (set band-index (+ band-index 1)))
  (set weight
    (/ (convert-type (- frequency (array-get config:band-frequencies band-index)) sp-sample-t)
      (convert-type
        (- (array-get config:band-frequencies (+ band-index 1))
          (array-get config:band-frequencies band-index))
        sp-sample-t)))
  (return
    (+ (array-get config:band-gains band-index)
      (*
        (- (array-get config:band-gains (+ band-index 1)) (array-get config:band-gains band-index))
        weight))))

(define (sp-reverb-build-g-matrix-real-imag config frequency g-real g-imag)
  (void sp-reverb-late-config-t* sp-time-t sp-sample-t* sp-sample-t*)
  (declare
    line-count sp-time-t
    row-index sp-time-t
    column-index sp-time-t
    band-gain sp-sample-t
    angle sp-sample-t
    phase-real sp-sample-t
    phase-imag sp-sample-t
    scale sp-sample-t
    idx sp-time-t)
  (set
    line-count config:delay-count
    band-gain (* config:overall-strength (band-gain-at config frequency))
    row-index 0)
  (while (< row-index line-count)
    (set
      angle
      (* -2.0 M_PI
        (convert-type frequency sp-sample-t)
        (convert-type (array-get config:delay-times row-index) sp-sample-t))
      phase-real (cos angle)
      phase-imag (sin angle)
      column-index 0)
    (while (< column-index line-count)
      (set
        scale
        (* band-gain
          (array-get config:mix-matrix-row-major
            (convert-type (+ (* row-index line-count) column-index) sp-time-t)))
        idx (convert-type (+ (* row-index line-count) column-index) sp-time-t)
        (array-get g-real idx) (* phase-real scale)
        (array-get g-imag idx) (* phase-imag scale)
        column-index (+ column-index 1)))
    (set row-index (+ row-index 1))))

(define (form-identity-minus-matrix line-count g-real g-imag a-real a-imag)
  (void sp-time-t sp-sample-t* sp-sample-t* sp-sample-t* sp-sample-t*)
  (declare row-index sp-time-t column-index sp-time-t idx sp-time-t)
  (set row-index 0)
  (while (< row-index line-count)
    (set column-index 0)
    (while (< column-index line-count)
      (set
        idx (convert-type (+ (* row-index line-count) column-index) sp-time-t)
        (array-get a-real idx) (- (if* (= row-index column-index) 1.0 0.0) (array-get g-real idx))
        (array-get a-imag idx) (- 0.0 (array-get g-imag idx))
        column-index (+ column-index 1)))
    (set row-index (+ row-index 1))))

(define (complex-pair-divide ar ai br bi rr ri)
  (void sp-sample-t sp-sample-t sp-sample-t sp-sample-t sp-sample-t* sp-sample-t*)
  (declare denom sp-sample-t)
  (set
    denom (+ (* br br) (* bi bi))
    *rr (/ (+ (* ar br) (* ai bi)) denom)
    *ri (/ (- (* ai br) (* ar bi)) denom)))

(define (lu-decompose-complex-pairs line-count a-real a-imag pivot-index-list)
  (void sp-time-t sp-sample-t* sp-sample-t* sp-time-t*)
  (declare
    k-index sp-time-t
    pivot-row sp-time-t
    row-index sp-time-t
    column-index sp-time-t
    pivot-abs-max sp-sample-t
    value-abs sp-sample-t
    idx sp-time-t
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
      pivot-abs-max
      (hypot (array-get a-real (convert-type (+ (* k-index line-count) k-index) sp-time-t))
        (array-get a-imag (convert-type (+ (* k-index line-count) k-index) sp-time-t)))
      row-index (+ k-index 1))
    (while (< row-index line-count)
      (set value-abs
        (hypot (array-get a-real (convert-type (+ (* row-index line-count) k-index) sp-time-t))
          (array-get a-imag (convert-type (+ (* row-index line-count) k-index) sp-time-t))))
      (if (> value-abs pivot-abs-max) (set pivot-abs-max value-abs pivot-row row-index))
      (set row-index (+ row-index 1)))
    (if (!= pivot-row k-index)
      (begin
        (set column-index 0)
        (while (< column-index line-count)
          (set
            idx (convert-type (+ (* pivot-row line-count) column-index) sp-time-t)
            temp-real (array-get a-real idx)
            temp-imag (array-get a-imag idx)
            (array-get a-real idx)
            (array-get a-real (convert-type (+ (* k-index line-count) column-index) sp-time-t))
            (array-get a-imag idx)
            (array-get a-imag (convert-type (+ (* k-index line-count) column-index) sp-time-t))
            (array-get a-real (convert-type (+ (* k-index line-count) column-index) sp-time-t))
            temp-real
            (array-get a-imag (convert-type (+ (* k-index line-count) column-index) sp-time-t))
            temp-imag
            column-index (+ column-index 1)))
        (set
          temp-index (array-get pivot-index-list pivot-row)
          (array-get pivot-index-list pivot-row) (array-get pivot-index-list k-index)
          (array-get pivot-index-list k-index) temp-index)))
    (set row-index (+ k-index 1))
    (while (< row-index line-count)
      (complex-pair-divide
        (array-get a-real (convert-type (+ (* row-index line-count) k-index) sp-time-t))
        (array-get a-imag (convert-type (+ (* row-index line-count) k-index) sp-time-t))
        (array-get a-real (convert-type (+ (* k-index line-count) k-index) sp-time-t))
        (array-get a-imag (convert-type (+ (* k-index line-count) k-index) sp-time-t)) &scale-real
        &scale-imag)
      (set
        (array-get a-real (convert-type (+ (* row-index line-count) k-index) sp-time-t)) scale-real
        (array-get a-imag (convert-type (+ (* row-index line-count) k-index) sp-time-t)) scale-imag
        column-index (+ k-index 1))
      (whil (< column-index line-count)
        (set
          prod-real
          (-
            (* scale-real
              (array-get a-real (convert-type (+ (* k-index line-count) column-index) sp-time-t)))
            (* scale-imag
              (array-get a-imag (convert-type (+ (* k-index line-count) column-index) sp-time-t))))
          prod-imag
          (+
            (* scale-real
              (array-get a-imag (convert-type (+ (* k-index line-count) column-index) sp-time-t)))
            (* scale-imag
              (array-get a-real (convert-type (+ (* k-index line-count) column-index) sp-time-t)))))
        (set
          (array-get a-real (convert-type (+ (* row-index line-count) column-index) sp-time-t))
          (- (array-get a-real (convert-type (+ (* row-index line-count) column-index) sp-time-t))
            prod-real)
          (array-get a-imag (convert-type (+ (* row-index line-count) column-index) sp-time-t))
          (- (array-get a-imag (convert-type (+ (* row-index line-count) column-index) sp-time-t))
            prod-imag)
          column-index (+ column-index 1)))
      (set row-index (+ row-index 1)))
    (set k-index (+ k-index 1))))

(define
  (lu-solve-complex-pairs line-count a-real a-imag pivot-index-list right-real right-imag solution-real solution-imag)
  (void sp-time-t sp-sample-t* sp-sample-t* sp-time-t* sp-sample-t* sp-sample-t* sp-sample-t* sp-sample-t*)
  (declare
    row-index sp-time-t
    column-index sp-time-t
    prod-real sp-sample-t
    prod-imag sp-sample-t
    div-real sp-sample-t
    div-imag sp-sample-t)
  (set row-index 0)
  (while (< row-index line-count)
    (set
      (array-get solution-real row-index)
      (array-get right-real (array-get pivot-index-list row-index))
      (array-get solution-imag row-index)
      (array-get right-imag (array-get pivot-index-list row-index))
      row-index (+ row-index 1)))
  (set row-index 1)
  (while (< row-index line-count)
    (set column-index 0)
    (while (< column-index row-index)
      (set
        prod-real
        (-
          (* (array-get a-real (convert-type (+ (* row-index line-count) column-index) sp-time-t))
            (array-get solution-real column-index))
          (* (array-get a-imag (convert-type (+ (* row-index line-count) column-index) sp-time-t))
            (array-get solution-imag column-index)))
        prod-imag
        (+
          (* (array-get a-real (convert-type (+ (* row-index line-count) column-index) sp-time-t))
            (array-get solution-imag column-index))
          (* (array-get a-imag (convert-type (+ (* row-index line-count) column-index) sp-time-t))
            (array-get solution-real column-index))))
      (set
        (array-get solution-real row-index) (- (array-get solution-real row-index) prod-real)
        (array-get solution-imag row-index) (- (array-get solution-imag row-index) prod-imag)
        column-index (+ column-index 1)))
    (set row-index (+ row-index 1)))
  (set row-index line-count)
  (while (> row-index 0)
    (set row-index (- row-index 1) column-index (+ row-index 1))
    (while (< column-index line-count)
      (set
        prod-real
        (-
          (* (array-get a-real (convert-type (+ (* row-index line-count) column-index) sp-time-t))
            (array-get solution-real column-index))
          (* (array-get a-imag (convert-type (+ (* row-index line-count) column-index) sp-time-t))
            (array-get solution-imag column-index)))
        prod-imag
        (+
          (* (array-get a-real (convert-type (+ (* row-index line-count) column-index) sp-time-t))
            (array-get solution-imag column-index))
          (* (array-get a-imag (convert-type (+ (* row-index line-count) column-index) sp-time-t))
            (array-get solution-real column_index))))
      (set
        (array-get solution-real row-index) (- (array-get solution-real row-index) prod-real)
        (array-get solution-imag row-index) (- (array-get solution-imag row-index) prod-imag)
        column-index (+ column-index 1)))
    (complex-pair-divide (array-get solution-real row-index) (array-get solution-imag row-index)
      (array-get a-real (convert-type (+ (* row-index line-count) row-index) sp-time-t))
      (array-get a-imag (convert-type (+ (* row-index line-count) row-index) sp-time-t)) &div-real
      &div-imag)
    (set (array-get solution-real row-index) div-real (array-get solution-imag row-index) div-imag)))

(define (sp-reverb-late-table-exact config frequency-list frequency-count response-list)
  (void sp-reverb-late-config-t* sp-time-t* sp-time-t sp-reverb-response-t*)
  (declare
    line-count sp-time-t
    g-real sp-sample-t*
    g-imag sp-sample-t*
    a-real sp-sample-t*
    a-imag sp-sample-t*
    solution-real sp-sample-t*
    solution-imag sp-sample-t*
    right-real sp-sample-t*
    right-imag sp-sample-t*
    pivot-index-list sp-time-t*
    frequency-index sp-time-t
    index-count sp-time-t
    sum-real sp-sample-t
    sum-imag sp-sample-t
    magnitude sp-sample-t
    angle sp-sample-t
    spectral-radius sp-sample-t)
  (set
    line-count config:delay-count
    g-real
    (__builtin-alloca (convert-type (* line-count (* line-count (sizeof sp-sample-t))) size-t))
    g-imag
    (__builtin-alloca (convert-type (* line-count (* line-count (sizeof sp-sample-t))) size-t))
    a-real
    (__builtin-alloca (convert-type (* line-count (* line-count (sizeof sp-sample-t))) size-t))
    a-imag
    (__builtin-alloca (convert-type (* line-count (* line-count (sizeof sp-sample-t))) size-t))
    solution-real (__builtin-alloca (convert-type (* line-count (sizeof sp-sample-t)) size-t))
    solution-imag (__builtin-alloca (convert-type (* line-count (sizeof sp-sample-t)) size-t))
    right-real (__builtin-alloca (convert-type (* line-count (sizeof sp-sample-t)) size-t))
    right-imag (__builtin-alloca (convert-type (* line-count (sizeof sp-sample-t)) size-t))
    pivot-index-list (__builtin-alloca (convert-type (* line-count (sizeof sp-time-t)) size-t)))
  (set index-count 0)
  (while (< index-count line-count)
    (set
      (array-get right-real index-count) (/ 1.0 (convert-type line-count sp-sample-t))
      (array-get right-imag index-count) 0.0
      index-count (+ index-count 1)))
  (set frequency-index 0)
  (while (< frequency-index frequency-count)
    (build-g-matrix-real-imag config (array-get frequency-list frequency-index) g-real g-imag)
    (form-identity-minus-matrix line-count g-real g-imag a-real a-imag)
    (lu-decompose-complex-pairs line-count a-real a-imag pivot-index-list)
    (lu-solve-complex-pairs line-count a-real
      a-imag pivot-index-list right-real right-imag solution-real solution-imag)
    (set sum-real 0.0 sum-imag 0.0 index-count 0)
    (while (< index-count line-count)
      (set
        sum-real (+ sum-real (array-get solution-real index-count))
        sum-imag (+ sum-imag (array-get solution-imag index-count))
        index-count (+ index-count 1)))
    (set
      sum-real (/ sum-real (convert-type line-count sp-sample-t))
      sum-imag (/ sum-imag (convert-type line-count sp-sample-t))
      magnitude (sqrt (+ (* sum-real sum-real) (* sum-imag sum-imag)))
      angle (atan2 sum-imag sum-real))
    (set
      (struct-get (array-get response-list frequency-index) gain) magnitude
      (struct-get (array-get response-list frequency-index) phase)
      (convert-type (llround angle) sp-time-t)
      (struct-get (array-get response-list frequency-index) decay) (convert-type 0 sp-time-t)
      frequency-index (+ frequency-index 1))))

(define (sp-reverb-late-table config frequency-list frequency-count response-list)
  (void sp-reverb-late-config-t* sp-time-t* sp-time-t sp-reverb-response-t*)
  (declare
    delay-index sp-time-t
    frequency-index sp-time-t
    band-index sp-time-t
    row-index sp-time-t
    col-index sp-time-t
    delay-sum sp-time-t
    delay-mean sp-time-t
    row-energy-sum sp-sample-t
    row-energy-mean sp-sample-t
    mix-scale sp-sample-t
    band-gain sp-sample-t
    weight sp-sample-t)
  (set delay-sum 0 delay-index 0)
  (while (< delay-index config:delay-count)
    (set
      delay-sum (+ delay-sum (array-get config:delays delay-index))
      delay-index (+ delay-index 1)))
  (if (== config:delay-count 0) (set delay-mean 0)
    (set delay-mean (/ delay-sum config:delay-count)))
  (set row-energy-sum 0.0 row-index 0)
  (while (< row-index config:mix-rows)
    (declare energy sp-sample-t v sp-sample-t)
    (set energy 0.0 col-index 0)
    (while (< col-index config:mix-columns)
      (set
        v
        (array-get config:mix-matrix-row-major
          (convert-type (+ (* row-index config:mix-columns) col-index) sp-time-t))
        energy (+ energy (* v v))
        col-index (+ col-index 1)))
    (set row-energy-sum (+ row-energy-sum energy) row-index (+ row-index 1)))
  (if (== config:mix-rows 0) (set row-energy-mean 1.0)
    (set row-energy-mean (/ row-energy-sum (convert-type config:mix-rows sp-sample-t))))
  (if (> row-energy-mean 0.0)
    (set mix-scale (convert-type (/ 1.0 (sqrt row-energy-mean)) sp-sample-t))
    (set mix-scale 1.0))
  (set frequency-index 0)
  (while (< frequency-index frequency-count)
    (if (== config:band-count 0) (set band-gain 0.0)
      (if (<= (array-get frequency-list frequency-index) (array-get config:band-frequencies 0))
        (set band-gain (array-get config:band-gains 0))
        (if
          (>= (array-get frequency-list frequency-index)
            (array-get config:band-frequencies ((- config:band-count 1))))
          (set band-gain (array-get config:band-gains (- config:band-count 1)))
          (begin
            (set band-index 0)
            (while
              (< (array-get config:band-frequencies (+ band-index 1))
                (array-get frequency-list frequency-index))
              (set band-index (+ band-index 1)))
            (set weight
              (/
                (convert-type
                  (- (array-get frequency-list frequency-index)
                    (array-get config:band-frequencies band-index))
                  sp-sample-t)
                (convert-type
                  (- (array-get config:band-frequencies (+ band-index 1))
                    (array-get config:band-frequencies band-index))
                  sp-sample-t)))
            (set band-gain
              (+ (array-get config:band-gains band-index)
                (*
                  (- (array-get config:band-gains (+ band-index 1))
                    (array-get config:band-gains band-index))
                  weight)))))))
    (set
      (struct-get (array-get response-list frequency-index) gain) (* config:strength mix-scale)
      (struct-get (array-get response-list frequency-index) decay) (convert-type band-gain sp-time-t)
      (struct-get (array-get response-list frequency-index) phase) delay-mean
      frequency-index (+ frequency-index 1))))
