# sph-reverb
frequency-domain reverb that scales from instrument cavities to concert halls and supports arbitrary spatial layouts.

# algorithm hierarchy
* early reflections
  * geometric acoustics
    * image-source method
    * beam tracing
    * uniform theory of diffraction
* modal correction
  * low-frequency Helmholtz eigenproblem on a uniform grid
  * Robin boundary impedance
  * Lanczos thick-restart eigensolver
* late reflections
  * modal feedback delay network in the frequency domain
    * poles from delay geometry, absorption, and mixing
    * state directions for spatial embedding of delay lines
    * source coupling by state-excitation vector
    * channel coupling by state-projection vectors
    * residues from biorthogonal eigenvectors
    * time-domain modal synthesis per mode and channel

# general implementation details
* platform: c17, posix.1-2008, lp64, little endian, libc. primary target: x86_64, linux.
* dependencies: [embree](https://www.embree.org/) for the early reflections. glibc because embree distribution packages are often compiled against it
* all times and all frequencies are unsigned integer periods that represent counts of samples.
* sp_time_t is the discrete unsigned integer type for every quantity that is bounded by the systems maximum sample index. this includes time, frequency, decay, and phase.
* sp_sample_t is the real type for all continuous-valued quantities such as gains, matrix entries, and intermediate values.

# late reflections
* structure
  * late kernel outputs per channel decaying sinusoids
  * a separate generator renders them as time-domain samples
* space
  * all spatial objects use the coordinate domain [-1, 1] ** dimension_count
  * source position, channel positions, and state directions share this domain
  * state directions encode late-field propagation directions, not room surfaces
* geometry
  * source position p uses position.values[0..d-1]
  * channel c position x_c uses layout.bases[c * d .. c * d + d - 1]
  * state direction s_i uses state_directions[i * d .. i * d + d - 1]
  * d equals layout.basis_length equals position.dimension_count equals state_dimension_count

## mapping
~~~
position: (dimension ...)
input_partial
  {t -> gain}
  {t -> period}
  phase
  duration
  position
sampled_input_partial: gain period phase position
input_partial -> sampled_input_partial
modal_pole: period decay
modal_residue: amplitude phase
modal_pole_set: modal_pole:mode ...
channel_modal_residue: channel_index mode_index amplitude phase
channel_modal_set: channel_modal_residue ...
sampled_input_partial -> channel_modal_set
decay_output_partial: gain period phase decay
(channel_modal_residue sampled_input_partial) -> decay_output_partial ...
channel_output_partial
  channel_index
  {t -> gain}
  {t -> period}
  phase
  duration
decay_output_partial:channel ... -> channel_output_partial:channel ...
residual_tail
  channel_index
  start_time
  duration
  noise_color
  decay_profile
(config position layout) -> residual_tail:channel ...
~~~

## procedures
~~~
sp_reverb_late_modal_poles
  given config
  search complex plane for z_k = r_k * exp(j * theta_k) with 0 < r_k < 1
  solve det(I - G(z_k)) = 0
  for each solution k
    period_k = round(2 * pi / theta_k) in samples
    decay_k = round(1 / (-log(r_k))) in samples
    define modal_pole_k = (period_k decay_k)
  optionally sort modes by estimated energy or residue magnitude
  assemble modal_pole_set from modal_pole_k

sp_reverb_late_modal_state_excitation
  given config position
  for each delay line i
    read state direction s_i from config.state_directions
    compute unit source direction u_p from position
    compute unit state direction u_i from s_i
    cos_i = dot(u_p, u_i)
    x_i = max(cos_i, 0)
    b_i_real = f_source(x_i) for a chosen source lobe function
  normalize b_i_real so sum_i (b_i_real^2) = 1
  define input vector b(position) with real part b_i_real and zero imaginary part

sp_reverb_late_modal_state_projection
  given config layout position channel_index
  read channel position x_c from layout.bases
  compute unit channel direction u_c from x_c
  compute scalar channel pan gain g_c(position, x_c) from a chosen pan law on [-1, 1]^dimension
  for each delay line i
    read state direction s_i from config.state_directions
    compute unit state direction u_i from s_i
    cos_ci = dot(u_c, u_i)
    y_ci = max(cos_ci, 0)
    h_ci = f_channel(y_ci) for a chosen projection lobe function
    c_ci_real = g_c(position, x_c) * h_ci
  optionally normalize c_ci_real over i
  define output vector c(channel_index, position) with real part c_ci_real and zero imaginary part

sp_reverb_late_modal_residues
  given config modal_pole_set position layout
  construct shared input vector b(position) by sp_reverb_late_modal_state_excitation
  for each mode k with modal_pole_k in modal_pole_set
    reconstruct theta_k and r_k from period_k and decay_k
      theta_k = 2 * pi / period_k
      r_k = exp(-1 / decay_k)
    form complex pole z_k = r_k * exp(j * theta_k)
    build G_k = G(z_k) from config
    solve G_k * v_k = v_k for a nontrivial right eigenvector v_k
    solve transpose(w_k) * G_k = transpose(w_k) for a nontrivial left eigenvector w_k
    compute scalar sk = transpose(w_k) * b(position)
    compute scalar n_k = transpose(w_k) * v_k
    for each channel_index
      construct output vector c(channel_index, position) by sp_reverb_late_modal_state_projection
      compute scalar t_kc = transpose(c(channel_index,position)) * v_k
      alpha_kc = (t_kc * s_k) / n_k
      amplitude_kc = absolute_value(alpha_kc)
      phase_kc = round(argument(alpha_kc) * period_k / (2 * pi)) in samples
      define channel_modal_residue(channel_index, k) = (amplitude_kc phase_kc)
  assemble channel_modal_set from channel_modal_residue(channel_index, k)

sp_reverb_late_modal_excitation
  given modal_pole_set channel_modal_set sampled_input_partial:...
  for each sampled_input_partial with period_in
    for each channel_modal_residue(channel_index, k)
      read period_k and decay_k from modal_pole_k
      read amplitude_kc and phase_kc from channel_modal_residue
      if using direct mode add
        gain_kc = amplitude_kc
      else
        theta_k = 2 * pi / period_k
        r_k = exp(-1 / decay_k)
        z_k = r_k * exp(j * theta_k)
        z_ratio = exp(-j * 2 * pi * period_in / period_k)
        gain_kc = amplitude_kc / absolute_value(1 - z_k * z_ratio)
      define decay_output_partial_kc
        gain = gain_kc
        period = period_k
        phase = phase_kc
        decay = decay_k
  collect decay_output_partial_kc as decay_output_partial:channel ...

sp_reverb_late_modal_render_partials
  given decay_output_partial:channel ...
  for each decay_output_partial_kc
    construct channel_output_partial for channel_index
      {t -> gain(t)} is exponential decay with time constant decay
      {t -> period(t)} is constant period
      phase is phase
      duration is derived from decay or from a global tail limit
  assemble channel_output_partial:channel ...

sp_reverb_late_residual_tail
  given config position layout
  derive residual_tail:channel ... from unresolved mode region
  for example
    use colored noise per channel with gain shaped by a short decay_profile
    start at time where deterministic modal tail energy falls below residual.start_threshold
    limit duration per channel to residual.duration_limit

sp_reverb_late_render
  given config position layout input_partial:...
  sampled_input_partial:... = map input_partial:... -> sampled_input_partial:...
  modal_pole_set = sp_reverb_late_modal_poles(config)
  channel_modal_set = sp_reverb_late_modal_residues(config modal_pole_set position layout)
  decay_output_partial:channel ... = sp_reverb_late_modal_excitation(modal_pole_set channel_modal_set sampled_input_partial:...)
  channel_output_partial:channel ... = sp_reverb_late_modal_render_partials(decay_output_partial:channel ...)
  residual_tail:channel ... = sp_reverb_late_residual_tail(config position layout) (optional)
~~~

## functions
~~~
sp_reverb_late_modal_poles :: config -> modal_pole_set
sp_reverb_late_modal_state_excitation :: config position -> b_vector
sp_reverb_late_modal_state_projection :: config layout position channel_index -> c_vector
sp_reverb_late_modal_residues :: config modal_pole_set position layout -> channel_modal_set
sp_reverb_late_modal_excitation :: modal_pole_set channel_modal_set sampled_input_partial:... -> decay_output_partial:channel ...
sp_reverb_late_modal_render_partials :: decay_output_partial:channel ... -> channel_output_partial:channel ...
sp_reverb_late_residual_tail :: config position layout -> residual_tail:channel ...
sp_reverb_late_render :: config position layout input_partial:... -> (channel_output_partial:channel ... residual_tail:channel ...)
sp_reverb_build_feedback_matrix :: config period -> matrix
sp_reverb_build_feedback_matrix_from_polar :: config r theta -> matrix
sp_reverb_form_identity_minus_feedback :: matrix -> matrix
sp_reverb_lower_upper_factorization :: matrix -> (l u)
sp_reverb_lower_upper_solve :: (l u) vector -> vector
sp_reverb_power_iteration_dominant_eigenpair :: matrix -> (lambda vector)
sp_reverb_eigen_equation_value :: config r theta -> (real imag)
sp_reverb_eigen_equation_jacobian_finite_difference :: config r theta -> (dfr_dr dfr_dtheta dfi_dr dfi_dtheta)
sp_reverb_newton_step_on_eigen_equation :: (r theta) (jacobian function) -> (r_next theta_next)
~~~

## structures
~~~
config
  delays
  mix
  bands
  state_directions
  state_dimension_count
  modal_solver
  residual
layout: bases channel_count basis_length
bands: low_frequency high_frequency gain
modal_solver: max_mode_count search_region tolerance
residual: enabled start_threshold duration_limit noise_color_model
position: dimension_count values
modal_pole_set: period:mode ... decay:mode ...
channel_modal_residue: channel_index mode_index amplitude phase
channel_modal_set: channel_modal_residue ...
decay_output_partial: gain period phase decay
channel_output_partial
  channel_index
  {t -> gain}
  {t -> period}
  phase
  duration
residual_tail
  channel_index
  start_time
  duration
  noise_color
  decay_profile
~~~

## further reading
### references
* Jot R. (1991). "Etude et realisation d’un spatialisateur de sons par modeles physiques et perceptifs". Defines the FDN structure, mixing matrices, and multiband attenuation used in the late kernel.
* Jot R., Chaigne A. (1991). "Digital delay networks for designing artificial reverberators". Establishes delay-line networks with controlled energy decay; basis for band-dependent absorption.
* Schlecht S.J., Habets E.A.P. (2015). "Reverberation modeling using a time-varying feedback delay network". Provides det(I − G(z)) = 0 and the numerical root-finding used to obtain FDN poles.
* Schlecht S.J. (2019). "Feedback Delay Networks: Echo Density, Stability, and Mode Analysis". Formalizes FDN modal decomposition and biorthogonal residues; defines the mathematical model underlying the modal set.
* Schroeder M.R. (1962). "Natural sounding artificial reverberation". Introduces late reverberation as a sum of exponentially decaying sinusoids; supports modal synthesis.
* Poletti M.A. (1996). "A modal decomposition approach to sound field reproduction". Gives the projection model linking mode shapes to receiver positions; informs the definition of c_c(position).
* Daniel J., Rault J.-B., Polack J.-D. (1998). "Ambisonics encoding of the sound field associated with room acoustics". Shows late fields can be modeled as isotropic directional distributions; supports directional state sampling.
* Siltanen S., Lokki T. (2007). "Directional analysis of room impulse responses". Demonstrates structured directional energy in late fields; motivates using per-delay state directions.
* Samarasinghe P., Abhayapala T., Poletti M. (2015). "Room impulse response synthesis using 3D modal decomposition". Provides explicit source- and listener-dependent modal excitation; supports b(position) and c_c(position).

### wikipedia
* [Complex number](https://en.wikipedia.org/wiki/Complex_number)
* [Linear interpolation](https://en.wikipedia.org/wiki/Linear_interpolation)
* [LU decomposition](https://en.wikipedia.org/wiki/LU_decomposition)
* [Determinant](https://en.wikipedia.org/wiki/Determinant)
* [Jacobian matrix and determinant](https://en.wikipedia.org/wiki/Jacobian_matrix_and_determinant)
* [Newton's method](https://en.wikipedia.org/wiki/Newton%27s_method)
* [Eigenvalues and eigenvectors](https://en.wikipedia.org/wiki/Eigenvalues_and_eigenvectors)
* [Power iteration](https://en.wikipedia.org/wiki/Power_iteration)
* [Modal analysis](https://en.wikipedia.org/wiki/Modal_analysis)
* [Residue (complex analysis)](https://en.wikipedia.org/wiki/Residue_%28complex_analysis%29)
* [Residue theorem](https://en.wikipedia.org/wiki/Residue_theorem)

# early reflections
## algorithm layout
* geometric acoustics
  * image-source method (convex, planar, specular boundaries)
  * beam tracing (nonconvex or topologically complex spaces)
  * uniform theory of diffraction for edge contributions
* modal correction (low-frequency only: below the schroeder frequency or within instrument-scale cavities)
  * uniform-grid helmholtz eigenproblem with robin boundary impedance; lanczos (thick-restart) for the lowest modes

## functions
~~~
sp_reverb_early_geometry :: (vec3 ...) (index ...) -> geometry
sp_reverb_early_materials :: (frequency ...) {band -> reflectivity} {band -> attenuation} -> materials
sp_reverb_early_source :: vec3 vec3 -> source
sp_reverb_early_receiver :: vec3 vec3 -> receiver
sp_reverb_early_image :: geometry materials source receiver max_order:int path_cap:int -> config
sp_reverb_early_beam :: geometry materials source receiver max_order:int path_cap:int (index ...) -> config
sp_reverb_early_diffraction :: geometry (index ...) (frequency ...) -> config
sp_reverb_early_union :: config ... -> config
sp_reverb_early_cull :: config threshold:real max_paths:int -> config
sp_reverb_early_sine :: config sine -> sine:channel ...
sp_reverb_early_noise :: config noise -> noise:channel ...
~~~

## structures
~~~
geometry
  vertices: (vec3 ...)
  faces: (index ...)
materials
  bands: (frequency ...)
  walls: {band -> reflectivity}
  air: {band -> attenuation}
source
  position: vec3
  orientation: vec3
receiver
  position: vec3
  orientation: vec3
config
  paths: (path ...)
path
  delay: time
  gain: real
  direction: vec3
  wall_chain: (wall_index ...)
  order: integer
  band_response: (gain ...)
~~~

geometry
* defines polygonal scene surfaces
* faces are convex and planar
* all geometry is static and specified in world coordinates

materials
* defines frequency-dependent reflection and absorption for walls and air
* each wall uses a reflectivity value per frequency band
* air model defines attenuation per distance per frequency band

image
* deterministic geometric solver for convex or planar enclosures
* computes all specular reflection paths up to `max_order`
* each path stores delay, gain, direction, and band-limited attenuation

beam
* generalizes the image-source approach to nonconvex or occluded geometries
* propagates beams instead of point paths and handles portals and visibility clipping
* used when scene contains multiple rooms or nonplanar boundaries

diffraction
* adds edge diffraction paths using the uniform theory of diffraction
* edge list indexes geometry edges where diffraction is to be computed
* paths extend the specular solution to include diffracted arrivals

union
* merges multiple path sets into one
* preserves delay ordering and removes duplicates

cull
* reduces the path set by amplitude threshold or maximum count
* ensures bounded complexity while preserving dominant reflections

sine
* applies computed reflection paths to a sinusoidal partial
* per-path gain and delay modify amplitude and phase
* maps one input sine to one or more output channels depending on scene layout

noise
* same as `sine` but for broadband or stochastic excitation
* applies band-dependent reflection gains and delays
