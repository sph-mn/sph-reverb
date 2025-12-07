# sph-reverb
frequency-domain reverb that scales from instrument cavities to concert halls and supports arbitrary spatial layouts.

work in progress.

designed for offline, high-accuracy rendering.

# algorithm hierarchy
* early reflections
  * geometric acoustics by deterministic path enumeration
    * direct path and unbounded specular reflection chains with numeric pruning
    * visibility and occlusion by embree
  * diffraction
    * first-order uniform theory of diffraction from user-specified edges
* modal correction
  * low-frequency helmholtz eigenproblem on a uniform grid
  * robin boundary impedance
  * lanczos thick-restart eigensolver
* late reflections
  * modal feedback delay network in the frequency domain
    * configuration built from scene geometry, materials, and early geometric statistics
    * calibration of attenuation, mixing, and state directions from early energy, direction, and density
    * poles from the calibrated feedback matrix
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
  * frequency = sample_rate / period
  * phase is a time shift in samples
* sp_sample_t is the real type for all continuous-valued quantities such as gains, matrix entries, and intermediate values.

# top level interface
the top level interface defines a complete transformation from a physical scene and an input signal into early and late output partials per channel. the interface exposes no intermediate structures. the caller owns all memory.

the interface uses four domains:
* scene domain: geometry and materials
* actor domain: source and receiver descriptions
* signal domain: input partials and output partials
* reverb domain: early and late configurations

the interface returns only deterministic data-flow products. no global state exists.

## structures
~~~
reverb_context
  scene
  early_results
  late_config

early_results
  early_path_set
  early_statistics

render_output
  early_partials
  late_partials
  residual_tail
~~~

## functions
~~~
sp_reverb_build_scene ::
  geometry
  materials
  ->
  scene

sp_reverb_build_early ::
  scene
  source
  receiver
  ->
  early_results

sp_reverb_build_late ::
  scene
  layout
  early_statistics
  ->
  late_config

sp_reverb_render_early ::
  scene
  early_results
  layout
  sampled_input_partial_list:sampled_input_partial
  ->
  sampled_partial_list_per_channel

sp_reverb_render_late ::
  late_config
  source_position
  layout
  sampled_input_partial_list:sampled_input_partial
  ->
  (channel_output_partial_list_per_channel, residual_tail_per_channel)

sp_reverb_render ::
  scene
  source
  receiver
  layout
  sampled_input_partial_list:sampled_input_partial
  ->
  render_output
~~~

## procedures
~~~
sp_reverb_build_scene
  defines scene from geometry and materials.
  stores immutable geometric data.
  stores immutable spectral data.

sp_reverb_build_early
  defines early_results from scene, source, and receiver.
  computes early_path_set from geometric propagation.
  computes early_statistics from early_path_set.

sp_reverb_build_late
  defines late_config from scene, layout, and early_statistics.
  builds the fdn configuration from scene and layout.
  calibrates late_config from early_statistics.

sp_reverb_render_early
  maps scene, early_results, layout, and sampled_input_partial_list to early_partials.
  evaluates material response along early_path_set.
  projects early contributions into receiver channels defined by layout.
  outputs early_partials as sampled_partial_list_per_channel.

sp_reverb_render_late
  maps late_config, source_position, layout, and sampled_input_partial_list to late_partials and residual_tail.
  evaluates source coupling from source_position.
  evaluates channel coupling from layout.
  performs modal synthesis from late_config and input partials.
  outputs late_partials as channel_output_partial_list_per_channel.
  outputs residual_tail as residual_tail_per_channel.

sp_reverb_render
  composes scene, source, receiver, layout, and sampled_input_partial_list into render_output.
  builds or receives early_results and late_config.
  calls sp_reverb_render_early to obtain early_partials.
  calls sp_reverb_render_late to obtain late_partials and residual_tail.
  returns render_output with early_partials, late_partials, and residual_tail.
~~~

## description
* the caller first constructs a scene.
* the caller then supplies source and receiver data.
* the system builds early_results from the scene and actors.
* the system builds late_config from the scene and early_statistics.
* the system renders early and late contributions from the input partials.
* the system returns all partials without side effects.

# late reflections
late reflections model the dense modal region of the sound field. the late kernel is a frequency-domain feedback delay network. its configuration is built from the physical scene and is completed by calibration that uses geometric data from the early solver. all late rendering uses the calibrated configuration.

the late system outputs channel-specific decaying sinusoids derived from fdn poles. a rendering step converts these modal contributions into time-domain samples.

## configuration
* construction from scene
  * delay lengths derived from geometry scale
  * per-band attenuation derived from material reflectivity and air absorption
  * band structure taken from scene materials
    * band_period_list defines the center periods for each band.
    * all bandwise attenuation / energy envelopes operate on these discrete periods.
  * initial state_directions chosen from direction samples
  * mixing matrix chosen for diffusion and stability
* calibration from early geometry
  * energy envelope per band taken from early paths
  * directional distribution taken from early paths
  * arrival density taken from early paths
  * attenuation, mixing, global gain, and state_directions updated from these measurements

the result is the final late_config used for modal analysis and rendering.

# late reflections: structure
* modal kernel outputs decaying sinusoids per channel
* renderer produces time-domain signals
* positions and directions use the coordinate domain [-1,1]^d
  * normalize coordinates so that the bounding box of the scene maps to [-1,1]^3
* layout defines receiver channel positions and bases
* position means source position in the functions and structures listings
* state_directions encode propagation directions of the late field

## structures
~~~
late_config
  delays
  mix
  bands
  state_directions
  state_dimension_count
  modal_solver
  residual
  calibrated_energy
  calibrated_density
  calibrated_directions

early_statistics
  energy_envelope_per_band
  directional_distribution
  arrival_density

modal_pole
  period
  decay

modal_pole_set
  modal_pole_list
  mode_count

channel_modal_residue
  channel_index
  mode_index
  amplitude
  phase

channel_modal_set
  channel_modal_residue_list
  residue_count

sampled_input_partial
  gain
  period
  phase
  position

sampled_input_partial_list
  sampled_input_partial ...

decay_output_partial
  gain
  period
  phase
  decay

decay_output_partial_list_per_channel
  channel_index
  decay_output_partial_list

channel_output_partial
  channel_index
  {t -> gain}
  {t -> period}
  phase
  duration

late_channel_partial_list_per_channel
  channel_index
  channel_output_partial_list

residual_tail
  channel_index
  start_time
  duration
  noise_color
  decay_profile

residual_tail_per_channel
  residual_tail ...
~~~

## functions
~~~
sp_reverb_late_build_config_from_scene ::
  scene
  layout
  ->
  late_config

sp_reverb_late_collect_early_statistics ::
  scene
  early_path_set:path_set
  ->
  early_statistics

sp_reverb_late_calibrate ::
  late_config
  early_statistics
  ->
  late_config

sp_reverb_late_modal_poles ::
  late_config
  ->
  modal_pole_set

sp_reverb_late_modal_state_excitation ::
  late_config
  position
  ->
  excitation_vector

sp_reverb_late_modal_state_projection ::
  late_config
  layout
  channel_index
  ->
  projection_vector

sp_reverb_late_modal_residues ::
  late_config
  modal_pole_set
  position
  layout
  ->
  channel_modal_set

sp_reverb_late_modal_excitation ::
  modal_pole_set
  channel_modal_set
  sampled_input_partial_list:sampled_input_partial
  ->
  decay_output_partial_list_per_channel:decay_output_partial

sp_reverb_late_modal_render_partials ::
  decay_output_partial_list_per_channel:decay_output_partial
  ->
  late_channel_partial_list_per_channel:channel_output_partial

sp_reverb_late_residual_tail ::
  late_config
  position
  layout
  ->
  residual_tail_per_channel:residual_tail

sp_reverb_late_render ::
  late_config
  position
  layout
  sampled_input_partial_list:sampled_input_partial
  ->
  (late_channel_partial_list_per_channel:channel_output_partial,
   residual_tail_per_channel:residual_tail)

sp_reverb_build_feedback_matrix ::
  late_config
  period
  ->
  matrix

sp_reverb_build_feedback_matrix_from_polar ::
  late_config
  radius
  angle
  ->
  matrix

sp_reverb_form_identity_minus_feedback ::
  matrix
  ->
  matrix

sp_reverb_lower_upper_factorization ::
  matrix
  ->
  (lower_matrix, upper_matrix)

sp_reverb_lower_upper_solve ::
  (lower_matrix, upper_matrix)
  vector
  ->
  vector

sp_reverb_power_iteration_dominant_eigenpair ::
  matrix
  ->
  (eigen_value, eigen_vector)

sp_reverb_eigen_equation_value ::
  late_config
  radius
  angle
  ->
  (real_value, imaginary_value)

sp_reverb_eigen_equation_jacobian_finite_difference ::
  late_config
  radius
  angle
  ->
  (d_real_by_radius, d_real_by_angle, d_imag_by_radius, d_imag_by_angle)

sp_reverb_newton_step_on_eigen_equation ::
  (radius, angle)
  jacobian
  value
  ->
  (next_radius, next_angle)
~~~

## procedures
~~~
sp_reverb_late_build_config_from_scene
  defines late_config as the parameterization of the fdn.
  delay lengths arise from geometric scale of the scene.
  band structure arises from material band_period_list.
  per-band attenuation arises from material reflectivity and air absorption.
  state_directions form a directional basis for late-field propagation.
  mix defines state interaction and diffusion rate.
  modal_solver and residual fields store solver and residual model parameters.

sp_reverb_late_collect_early_statistics
  extracts early_statistics from early_path_set.
  defines energy_envelope_per_band e(t,b) from geometric energy over time and band.
  defines directional_distribution p(omega) from path arrival directions.
  defines arrival_density d(t) from path count per time region.

sp_reverb_late_calibrate
  enforces agreement between late_config and early_statistics.
  adjusts attenuation to match energy_envelope_per_band in the overlap region.
  adjusts state_directions to match directional_distribution.
  adjusts mix and global gain to match arrival_density.
  writes calibrated_energy, calibrated_density, and calibrated_directions into late_config.

sp_reverb_late_modal_poles
  evaluates the fdn eigen equation det(i - g(z)) = 0 as det(i - g(z)) = det(i - g(z)).
  realizes g(z) by sp_reverb_build_feedback_matrix_from_polar.
  searches for roots in the open unit disk.
  maps each root z = r exp(j theta) to modal_pole with period from theta and decay from r.

sp_reverb_late_modal_state_excitation
  evaluates excitation_vector for a source at position.
  applies position normalization into the [-1,1]^d coordinate domain.
  couples the source position to state_directions.

sp_reverb_late_modal_state_projection
  evaluates projection_vector for a receiver channel defined by layout and channel_index.
  maps the channel position and basis into the state_directions frame.
  defines how each modal state contributes to that channel.

sp_reverb_late_modal_residues
  defines channel_modal_set from late_config, modal_pole_set, position, and layout.
  combines excitation_vector at position with projection_vector for each channel.
  applies the fdn eigensystem to obtain biorthogonal residues.
  writes channel_modal_residue_list with one residue per mode and per channel.

sp_reverb_late_modal_excitation
  evaluates the modal transfer function for each pole and each sampled_input_partial.
  combines channel_modal_set with sampled_input_partial_list in the z-domain.
  defines decay_output_partial_list_per_channel with gain, period, phase, and decay per mode and channel.

sp_reverb_late_modal_render_partials
  converts decay_output_partial_list_per_channel into late_channel_partial_list_per_channel.
  expresses each modal contribution as a decaying sinusoid in the time domain.
  defines {t -> gain} and {t -> period} over the modal duration for each channel.

sp_reverb_late_residual_tail
  models unresolved modal energy as a stochastic component for a source at position.
  derives noise_color from the residual spectral shape of late_config.
  derives decay_profile from calibrated residual decay.
  returns residual_tail_per_channel for all channels in layout.

sp_reverb_late_render
  composes the late-field mapping for a source at position.
  uses late_config to build or reuse modal_pole_set.
  forms channel_modal_set from excitation and projection.
  evaluates decay_output_partial_list_per_channel from sampled_input_partial_list.
  renders late_channel_partial_list_per_channel from modal parameters.
  computes residual_tail_per_channel from the residual model.
  returns late_channel_partial_list_per_channel and residual_tail_per_channel.

sp_reverb_build_feedback_matrix
  builds the fdn feedback matrix at a given discrete period.
  evaluates delay-line phases and band attenuation at that period.
  applies mix to form the full feedback operator.

sp_reverb_build_feedback_matrix_from_polar
  builds the fdn feedback matrix at a complex point z with given radius and angle.
  evaluates per-delay response at z.
  applies mix and attenuation to form g(z).

sp_reverb_form_identity_minus_feedback
  forms the matrix i - g from a feedback matrix g.
  prepares the eigen equation det(i - g(z)) = 0 and linear solves.

sp_reverb_lower_upper_factorization
  factors a matrix into lower and upper triangular matrices.
  prepares a reusable representation for repeated solves.

sp_reverb_lower_upper_solve
  solves a linear system with a stored lower and upper factorization.
  returns the solution vector for a given right-hand side.

sp_reverb_power_iteration_dominant_eigenpair
  applies power iteration to estimate the dominant eigenpair of a matrix.
  returns eigen_value and eigen_vector for validation and scaling.

sp_reverb_eigen_equation_value
  evaluates the eigen equation at a point defined by radius and angle.
  constructs g(z) from late_config by sp_reverb_build_feedback_matrix_from_polar.
  forms i - g(z) and computes its determinant.
  returns real_value and imaginary_value of the determinant.

sp_reverb_eigen_equation_jacobian_finite_difference
  approximates the jacobian of the eigen equation by finite differences in radius and angle.
  perturbs radius and angle around the current point.
  evaluates sp_reverb_eigen_equation_value at perturbed points.
  returns d_real_by_radius, d_real_by_angle, d_imag_by_radius, and d_imag_by_angle.

sp_reverb_newton_step_on_eigen_equation
  applies one newton step in the (radius, angle) plane.
  inverts the jacobian for the current point.
  updates radius and angle using the eigen equation value.
  returns next_radius and next_angle as the new iterate.
~~~

## description
* scene and early statistics define the late configuration.
* late configuration defines all modal behavior.
* modal poles follow from the fdn feedback matrix.
* residues follow from excitation and projection vectors.
* late rendering produces decaying sinusoids per channel and a residual tail.

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

# modal correction
* modal correction defines the low-frequency field.
* modal correction solves the helmholtz equation on a uniform grid.
* modal correction applies robin boundary conditions from scene materials.
* modal correction produces exact modes in the low-frequency band.
* modal correction replaces fdn modes in the transition region.
* modal correction preserves physical accuracy where geometric acoustics fails.
* modal correction defines a continuous connection to the fdn late field.

## algorithm hierarchy
* domain construction
  * scene geometry defines an axis-aligned bounding box.
  * the box defines a uniform grid.
  * each grid sample has an interior or exterior state.
  * boundary samples store robin impedance from scene materials.
* operator construction
  * the grid defines a discrete laplacian.
  * boundary samples define a robin operator.
  * the system defines a helmholtz operator a(k) = laplacian + k^2 i.
* eigenmodes
  * the solver finds eigenpairs in the low-frequency band.
  * the solver uses lanczos thick-restart.
  * each eigenvalue defines a modal period.
  * each mode defines a modal decay from boundary loss.
* modal coupling
  * the source position samples each eigenvector.
  * each channel position samples each eigenvector.
  * source samples define modal excitation.
  * channel samples define modal projection.
  * residues follow from excitation and projection.
* partial construction
  * each mode defines a sinusoid.
  * gain arises from residue magnitude.
  * period arises from the modal eigenvalue.
  * phase arises from eigenvector phase.
  * decay arises from modal boundary loss.
* merging
  * define a modal transition period.
  * helmholtz modes dominate above the transition period.
  * fdn modes dominate below the transition period.
  * a crossfade defines continuity in the overlap region.

## structures
```
modal_grid
  origin
  spacing
  resolution
  interior_flag
  boundary_impedance

modal_operator
  laplacian
  robin

modal_mode
  eigenvalue
  eigenvector
  period
  decay

modal_mode_set
  mode_list
  mode_count

modal_residue
  channel_index
  mode_index
  amplitude
  phase

modal_residue_set
  residue_list
  residue_count

modal_output_partial
  channel_index
  gain
  period
  phase
  decay

modal_output_partial_list_per_channel
  channel_index
  modal_output_partial_list

modal_transition
  transition_period
  overlap_width
```

## functions
~~~
sp_reverb_modal_build_grid ::
  scene
  ->
  modal_grid

sp_reverb_modal_build_operator ::
  modal_grid
  ->
  modal_operator

sp_reverb_modal_solve_modes ::
  modal_operator
  modal_transition
  ->
  modal_mode_set

sp_reverb_modal_state_excitation ::
  modal_mode_set
  position
  ->
  excitation_vector

sp_reverb_modal_state_projection ::
  modal_mode_set
  layout
  ->
  projection_vector_per_channel

sp_reverb_modal_residues ::
  modal_mode_set
  excitation_vector
  projection_vector_per_channel
  ->
  modal_residue_set

sp_reverb_modal_render_partials ::
  modal_mode_set
  modal_residue_set
  ->
  modal_output_partial_list_per_channel:modal_output_partial

sp_reverb_modal_merge_with_late ::
  modal_output_partial_list_per_channel:modal_output_partial
  channel_output_partial_list_per_channel:channel_output_partial
  modal_transition
  ->
  channel_output_partial_list_per_channel:channel_output_partial
~~~

## procedures
```
sp_reverb_modal_build_grid
  defines modal_grid from scene geometry and materials.
  sets origin, spacing, and resolution.
  sets interior_flag for all grid samples.
  sets boundary_impedance from scene materials.

sp_reverb_modal_build_operator
  defines modal_operator from modal_grid.
  forms the discrete laplacian on the grid.
  forms the discrete robin operator on boundary samples.

sp_reverb_modal_solve_modes
  defines modal_mode_set from modal_operator and modal_transition.
  solves the helmholtz eigenproblem in the low-frequency band.
  extracts eigenvalues and eigenvectors.
  maps eigenvalues to modal periods and decays.

sp_reverb_modal_state_excitation
  samples modal eigenvectors at the source position.
  defines excitation_vector for all modes.

sp_reverb_modal_state_projection
  samples modal eigenvectors at channel positions from layout.
  defines projection_vector_per_channel for all modes and channels.

sp_reverb_modal_residues
  pairs excitation_vector and projection_vector_per_channel.
  defines modal_residue_set with one residue per mode and per channel.

sp_reverb_modal_render_partials
  maps modal_mode_set and modal_residue_set to modal_output_partial_list_per_channel.
  defines gain, period, phase, and decay for each modal_output_partial.

sp_reverb_modal_merge_with_late
  merges modal_output_partial_list_per_channel with channel_output_partial_list_per_channel.
  uses modal_transition to define the transition_period and overlap_width.
  crossfades helmholtz and fdn contributions in the overlap region.
  defines the combined channel_output_partial_list_per_channel.
```

## description
* scene geometry defines the grid domain.
* scene materials define boundary impedance.
* modal modes define low-frequency behavior.
* fdn modes define high-frequency behavior.
* modal correction bridges both regimes.
* the system outputs modal partials that merge with fdn partials.

# early reflections
early reflections model geometric propagation by straight segments, specular reflections, and first-order edge diffraction on polygonal meshes. path construction uses embree for visibility. the early solver produces bandless geometric paths. frequency-dependent quantities arise only when paths are mapped through scene materials.

path enumeration admits unbounded reflection order. pruning follows a numeric cutoff based on geometric length or a broadband energy estimate. geometric spreading and material absorption yield exponential decay once paths are evaluated through materials.

the early solver provides:
* path_set for early partial synthesis
* geometric data for computing early_statistics used in late calibration
* early_path_set is exactly the path_set returned by:
  * sp_reverb_early_paths_direct
  * sp_reverb_early_paths_specular
  * sp_reverb_early_paths_diffraction
  * then union and culling

## algorithm layout
* direct path
  * straight segment between source and receiver
  * visibility is a geometric relation
  * represented by delay and direction only
* specular reflections
  * defined by mirrored-receiver constructions
  * each segment must satisfy the visibility relation
  * reflection order unbounded
  * cutoff determines admissible geometric extent
* first-order diffraction
  * defined on user-provided edges
  * relevant when direct or specular propagation is shadowed
  * visibility of source–edge and edge–receiver required
* band independence
  * early paths hold no spectral data
  * spectral response arises in a separate mapping through materials

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
  direction_source
  direction_receiver
  triangle_chain
  chain_length
  order
  kind

path_set
  path_list
  path_count

input_partial
  {t -> gain}
  {t -> period}
  phase
  duration
  position

sampled_partial
  gain
  period
  phase
  position

early_channel_partial_list_per_channel
  channel_index
  sampled_partial_list
~~~

## functions
~~~
sp_reverb_early_paths_direct ::
  scene
  source
  receiver
  ->
  path_set

sp_reverb_early_paths_specular ::
  scene
  source
  receiver
  cutoff
  ->
  path_set

sp_reverb_early_paths_diffraction ::
  scene
  source
  receiver
  edge_list
  cutoff
  ->
  path_set

sp_reverb_early_paths_union ::
  path_set_list
  path_set_count
  ->
  path_set

sp_reverb_early_paths_cull ::
  scene
  path_set
  threshold
  max_paths
  ->
  path_set

sp_reverb_early_partials_from_paths ::
  scene
  path_set
  layout
  sampled_input_partial_list:sampled_input_partial
  partial_count
  ->
  early_channel_partial_list_per_channel
~~~

## procedures
~~~
sp_reverb_early_paths_direct
  defines the direct geometric contribution between source and receiver.
  delay arises from geometric distance.
  directions arise from the connecting segment.
  visibility is a geometric relation.
  no spectral quantities appear.

sp_reverb_early_paths_specular
  defines specular reflection paths as geometric solutions.
  constructs mirrored receivers to define candidate paths.
  admissibility arises from visibility and cutoff.
  each path is represented by delay, directions, reflection order, and triangle chain.

sp_reverb_early_paths_diffraction
  defines diffracted paths as geometric solutions involving an edge.
  uses user-provided edges as diffraction features.
  relevance arises when other geometric routes are shadowed.
  admissibility requires visibility of source–edge and edge–receiver.

sp_reverb_early_paths_union
  defines the union of path sets under geometric equality.
  ordering enforces reproducibility of the combined path set.
  duplicates collapse under path equivalence.

sp_reverb_early_paths_cull
  defines a selection rule based on a broadband energy estimate from length and materials.
  the threshold determines which geometric paths remain.
  max_paths limits the cardinality of the returned path_set.

sp_reverb_early_partials_from_paths
  maps geometric paths to spectral partials.
  material data define attenuation and phase along each path.
  layout defines directional gain into channels.
  sampled_input_partial_list defines temporal and spectral alignment.
  produces early_channel_partial_list_per_channel as the early contribution.
~~~

## description
* geometry defines the propagation domain.
* materials define spectral response; paths are purely geometric.
* visibility relations determine admissibility of geometric segments.
* specular and diffracted paths encode only geometry, not frequency dependence.
* culling uses a broadband estimate to bound geometric complexity.
* mapping applies material and layout information to produce early partials.
