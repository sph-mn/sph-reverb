(define
  (sp-reverb-late-lookup frequency-list response-list response-count query-frequency)
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
      (struct-get (array-get response-list frequency-index) gain)
      (* config:strength mix-scale)
      (struct-get (array-get response-list frequency-index) decay) (convert-type band-gain sp-time-t)
      (struct-get (array-get response-list frequency-index) phase) delay-mean
      frequency-index (+ frequency-index 1))))
