# sph-reverb
frequency-domain reverb that scales from instrument cavities to concert halls and supports arbitrary spatial layouts.

work in progress.

designed for offline, high-accuracy rendering.

# algorithm hierarchy
* early reflections
  * geometric acoustics by ray tracing
    * deterministic image-source enumeration with embree
    * first-order specular reflections and direct path
  * diffraction
    * first-order uniform theory of diffraction from user-specified edges
* modal correction
  * low-frequency helmholtz eigenproblem on a uniform grid
  * robin boundary impedance
  * lanczos thick-restart eigensolver
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
* dependencies: [embree](https://www.embree.org/) for the early reflections. glibc because embree distribution packages are often compiled against it.
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
  given a late configuration
  determine the set of complex poles z = r * exp(j*theta) of the feedback matrix
    within the region 0 < r < 1
  convert each pole to a modal representation
    period is derived from theta
    decay is derived from r
  collect all modes into a modal_pole_set
  the implementation may choose any stable and convergent numerical search strategy

sp_reverb_late_modal_state_excitation
  given a late configuration and a source position
  evaluate how strongly each delay-line state is excited by the source
    using any directional or positional model consistent with config.state_directions
  return a real-valued excitation vector b(position)
    normalized or scaled as chosen by the implementation
    as long as relative excitation across states is preserved

sp_reverb_late_modal_state_projection
  given a late configuration, a layout, a position, and a channel index
  evaluate how each delay-line state contributes to the specified channel
    using any directional or positional model consistent with layout.bases
  return a real-valued projection vector c(channel, position)
    up to a global gain convention chosen by the implementation

sp_reverb_late_modal_residues
  given a late configuration, a modal_pole_set, a position, and a layout
  compute modal residues using the excitation and projection vectors
    and the chosen eigensystem representation for each pole
  produce a channel_modal_set that associates every mode with
    an amplitude and a phase per channel
  the method of eigenvector computation, normalization, and residue
    scaling is not constrained as long as it is mathematically consistent

sp_reverb_late_modal_excitation
  given modal_poles, modal residues, and sampled input partials
  determine how each input partial excites each mode for each channel
    using any stable interpretation of the modal transfer function
  generate a set of decay_output_partial objects per channel
    each describing gain, period, phase, and decay for one modal contribution

sp_reverb_late_modal_render_partials
  given decay_output_partial objects
  construct channel_output_partial objects for each channel
    representing exponentially decaying sinusoids
    with time-varying or constant parameters according to the implementation choice
  durations may be determined from decay, global limits, or any consistent rule

sp_reverb_late_residual_tail
  optionally model the unresolved modal region
    using a noise-based or filtered residual consistent with the configuration
  return a residual_tail structure per channel

sp_reverb_late_render
  given configuration, position, layout, and input partials
  produce the complete late-field representation by
    generating modal_poles
    computing residues
    computing modal excitation
    converting modal contributions to channel partials
    optionally adding a residual tail
  return all late-field partials and optional residual tails per channel
~~~

## functions
~~~
sp_reverb_late_modal_poles(config) -> modal_pole_set
sp_reverb_late_modal_state_excitation(config, position) -> b_vector
sp_reverb_late_modal_state_projection(config, layout, position, channel_index) -> c_vector
sp_reverb_late_modal_residues(config, modal_pole_set, position, layout) -> channel_modal_set
sp_reverb_late_modal_excitation(modal_pole_set, channel_modal_set, sampled_input_partial list) -> decay_output_partial per channel
sp_reverb_late_modal_render_partials(decay_output_partial per channel) -> channel_output_partial per channel
sp_reverb_late_residual_tail(config, position, layout) -> residual_tail per channel
sp_reverb_late_render(config, position, layout, input_partial list) -> (channel_output_partial per channel, residual_tail per channel)
sp_reverb_build_feedback_matrix(config, period) -> matrix
sp_reverb_build_feedback_matrix_from_polar(config, r, theta) -> matrix
sp_reverb_form_identity_minus_feedback(matrix) -> matrix
sp_reverb_lower_upper_factorization(matrix) -> (l, u)
sp_reverb_lower_upper_solve((l, u), vector) -> vector
sp_reverb_power_iteration_dominant_eigenpair(matrix) -> (lambda, vector)
sp_reverb_eigen_equation_value(config, r, theta) -> (real, imag)
sp_reverb_eigen_equation_jacobian_finite_difference(config, r, theta) -> (dfr_dr, dfr_dtheta, dfi_dr, dfi_dtheta)
sp_reverb_newton_step_on_eigen_equation((r, theta), jacobian, function) -> (r_next, theta_next)
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
# early reflections
early reflections form the geometric acoustics part of the reverb system. the model is deterministic and operates on polygonal triangle meshes. propagation paths consist of straight line segments, specular reflections, and first order edge diffraction. all visibility tests use embree. frequency dependent effects are computed per band by combining triangle reflection magnitude and phase with air attenuation per meter.

path enumeration is unbounded in reflection count and stops only when the predicted band energy of a candidate branch falls below a numerical threshold. this yields a geometry aligned high frequency response. the low frequency region is handled by the late modal solver. both operate independently and combine at the partial level.

## algorithm layout
* direct path
  * straight segment from source to receiver
  * visibility by ray cast
  * per band air attenuation
* specular reflections
  * reflection chain via mirrored receiver construction
  * per segment visibility test
  * per band reflection magnitude and phase
  * termination when energy < threshold
* first order diffraction
  * user provided diffraction edges
  * uniform theory of diffraction per band
  * visibility tests for source to edge and edge to receiver
* band dependent propagation
  * band_period_list defines global band structure
  * reflectivity magnitude and phase per triangle per band
  * air attenuation per meter per band
  * multiplicative accumulation along a path
* termination
  * no fixed reflection order
  * branch ends when band energy < threshold

## procedures
~~~
sp_reverb_early_direct_path
  given scene, source, and receiver
  use embree to test line-of-sight between source and receiver
  if any occluder is hit, output no path
  otherwise
    compute geometric distance from source to receiver in meters
    convert distance to delay in samples by samples_per_meter
    compute direct gain from distance law
    set direction as unit vector from source to receiver
    define one order-0 path with no reflections and empty band response

sp_reverb_early_paths_image
  given scene, source, receiver, and an energy or order limit
  construct all specular reflection paths that satisfy the limit
  for each valid path
    reconstruct a geometric chain from source to receiver
    compute total path length in meters
    convert path length to delay in samples by samples_per_meter
    determine the sequence of hit triangles or materials
    apply material and air response per band to form band gain and phase
    set direction as the incoming unit vector at the receiver
    define one early path with order equal to the reflection count

sp_reverb_early_paths_diffraction
  given scene, a user-specified set of diffraction edges, and band periods
  detect source–receiver pairs where direct or specular paths are shadowed
  for each relevant edge and shadowed configuration
    evaluate a first-order utd diffraction coefficient per band
    compute the diffracted path length from source to edge to receiver
    convert this length to delay in samples by samples_per_meter
    construct band gain and phase from utd coefficients and air attenuation
    set direction as unit vector from edge to receiver
    define one diffraction path using the common early path structure

sp_reverb_early_paths_union
  given several early path sets
  merge all paths into a single list
  enforce a deterministic ordering, for example by increasing delay
  remove exact duplicates according to full path structure
  return the unified early path list

sp_reverb_early_paths_cull
  given an early path list and culling parameters
  remove paths whose broadband energy falls below a fixed threshold
  if a maximum path count is given
    keep only the most relevant paths under that count, using a fixed rule
  preserve a deterministic ordering for the remaining paths
  return the reduced path list

sp_reverb_early_partials_from_paths
  given an early path list, a layout, and sampled input partials
  for each path and each channel in the layout
    derive a channel gain from the path direction and channel basis
    for each input partial
      propagate the path delay into a phase offset for that partial
      combine path gain, channel gain, and input gain into one amplitude
      set the early partial duration according to the chosen early–late split
      define one early_partial with channel index, gain, period, phase, and duration
  return the early_partial list

sp_reverb_early_noise_partials_from_paths
  given an early path list, a layout, and a noise band specification
  for each path and each channel
    derive a channel gain from the path direction and channel basis
    compress the per-band path response into the requested noise band
    set gain and duration for a band-limited noise excitation
    define one early_noise_partial per channel and band where needed
  return the early_noise_partial list

sp_reverb_early
  given scene, source, receiver, layout, and sampled input partials
  compute direct and specular paths by sp_reverb_early_paths_image
  compute diffraction paths by sp_reverb_early_paths_diffraction
  merge all paths by sp_reverb_early_paths_union
  reduce the merged set by sp_reverb_early_paths_cull
  map the resulting paths to early_partial by sp_reverb_early_partials_from_paths
  return the early_partial list as the deterministic early reflection contribution
~~~

## functions
~~~
sp_reverb_early_build_scene :: geometry materials -> scene
sp_reverb_early_paths_direct :: scene source receiver -> path_set
sp_reverb_early_paths_specular :: scene source receiver energy_threshold path_capacity -> path_set
sp_reverb_early_paths_diffraction :: scene edge_list band_period_list band_count -> path_set
sp_reverb_early_paths_union :: path_set_list path_set_count -> path_set
sp_reverb_early_paths_cull :: path_set threshold max_paths -> path_set
sp_reverb_early_partials_from_paths :: path_set layout sampled_partial_list partial_count -> early_partial_list:channel
~~~

## structures
~~~
geometry
  vertex_list
  index_list
  triangle_count
  triangle_material_index
  edge_list

materials
  band_period_list
  band_count
  triangle_reflectivity_magnitude
  triangle_reflectivity_phase
  air_attenuation_per_meter

scene
  embree_device_handle
  embree_scene_handle
  geometry
  materials

source
  position_world
  orientation_world

receiver
  position_world
  orientation_world

path
  delay
  direction
  triangle_chain
  chain_length
  band_gain_list
  band_phase_list
  band_count
  kind

path_set
  path_list
  path_count
~~~

## description
* geometry: triangle mesh in world coordinates. each triangle has one material index. diffraction edges supplied explicitly.
* materials: reflectivity magnitude and phase per triangle per band. air attenuation per meter per band. all band arrays share the same band_period_list.
* direct path: straight segment from source to receiver. delay in samples from geometric distance and samples per meter. per band air attenuation applied.
* specular reflections: receiver is mirrored through triangle planes to form a reflection chain. each segment must be visible. per band reflection applied. branch stops when energy falls below threshold.
* diffraction: first order uniform theory of diffraction. diffracted point lies on a specified edge. per band diffraction coefficient applied. visibility between source and edge and between edge and receiver required.
* union: concatenates input path sets. removes duplicates. sorts by delay.
* cull: applies amplitude threshold or maximum count. preserves ordering.
* partials: for each sampled input partial, each geometric path generates per channel early partials. per band gain and phase form the partial gain and phase. channel index determined by the layout.
