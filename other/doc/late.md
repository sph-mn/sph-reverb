# late reflections
models the dense modal region as a feedback delay network and produces modal and residual late contributions per channel.

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

channel_modal_residue
  channel_index
  mode_index
  amplitude
  phase

sampled_input_partial
  gain
  period
  phase
  position

decay_output_partial
  channel_index
  mode_index
  gain
  period
  phase
  decay

channel_output_partial
  channel_index
  gain
  period
  phase
  decay

residual_tail
  channel_index
  start_time
  duration
  noise_color
  decay_profile
~~~

## signatures
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
  modal_pole_set:(period decay) ...

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
  modal_pole_set:(period decay) ...
  position
  layout
  ->
  channel_modal_set:channel_modal_residue

sp_reverb_late_modal_excitation ::
  modal_pole_set:(period decay) ...
  channel_modal_set:channel_modal_residue
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
  treats scene and layout as the only geometric and spatial inputs to late_config before calibration.
  sets delay magnitudes so that geometric scale of the scene controls delay statistics.
  derives band structure and band periods from material band_period_list in scene.
  sets per-band attenuation so that material reflectivity and air attenuation define energy decay envelopes.
  sets state_directions so that they form a directional basis in the normalized coordinate domain of the scene.
  sets mix so that the induced feedback operator is stable and sufficiently diffusive for dense late fields.
  sets modal_solver and residual so that they define all later modal and residual late behavior.

sp_reverb_late_collect_early_statistics
  treats early_path_set as a purely geometric carrier and scene as the spectral carrier.
  defines energy_envelope_per_band as a function of time and band that aggregates geometric eergy from paths mapped through materials.
  defines directional_distribution as a distribution over arrival directions induced by early path directions.
  defines arrival_density as a function of time that counts path arrivals per time region.
  ensures that early_statistics depends on early_path_set only through these aggregated quantities.

sp_reverb_late_calibrate
  adjusts late_config so that its implied late energy, direction, and density agree with early_statistics in the overlap region.
  modifies per-band attenuation and global gain so that energy_envelope_per_band matches early behavior up to a controlled tolerance.
  modifies state_directions so that their directional distribution approximates directional_distribution.
  modifies mix so that arrival_density induced by the late field matches the early arrival density in the overlap region.
  writes calibrated_energy, calibrated_density, and calibrated_directions into late_config as the calibrated targets.

sp_reverb_late_modal_poles
  interprets late_config as defining a feedback operator g(z) on the unit disk.
  defines the eigen equation as det(i - g(z)) = 0 and treats its roots as the fdn poles.
  searches for roots only inside the open unit disk to enforce stability.
  maps each root z = r * exp(j * theta) to an element of modal_pole_set with period derived from theta and decay derived from r.
  ensures that modal_pole_set encodes all poles needed to represent the late field over the frequency range of interest.

sp_reverb_late_modal_state_excitation
  normalizes position into the coordinate domain that defines state_directions.
  maps the normalized position into a source coupling vector over the late states.
  defines excitation_vector so that it is linear in any change of source strength at fixed position.

sp_reverb_late_modal_state_projection
  maps a receiver channel defined by layout and channel_index into the state_directions frame.
  defines projection_vector so that it represents the contribution of each late state to that channel.
  ensures that projection_vector is linear in any change of channel basis at fixed position.

sp_reverb_late_modal_residues
  combines the excitation implied by position with the projection implied by layout and each channel.
  applies the fdn eigensystem so that residues are biorthogonal with respect to the modal basis.
  defines channel_modal_set as a collection of channel_modal_residue with one residue per mode and per channel.
  ensures that channel_modal_residue amplitude and phase are linear in source strength and channel gain.

sp_reverb_late_modal_excitation
  evaluates the modal transfer function for each element of modal_pole_set and each sampled_input_partial in the z-domain.
  combines channel_modal_set with sampled_input_partial_list through a linear convolution in the discrete-time frequency domain.
  defines decay_output_partial_list_per_channel as a collection of decay_output_partial where each entry carries its channel_index, mode_index, gain, period, phase, and decay.

sp_reverb_late_modal_render_partials
  interprets each decay_output_partial as defining a decaying sinusoid in time.
  maps each decay_output_partial into a channel_output_partial that carries equivalent time-domain behavior over its modal duration.
  preserves linearity in decay_output_partial_list_per_channel so that superposition holds per channel.

sp_reverb_late_residual_tail
  treats unresolved modal energy and high-order effects as a stochastic residual for a source at position.
  derives noise_color from the residual spectral shape implied by late_config after modal extraction.
  derives decay_profile from the calibrated residual decay parameters in late_config.
  defines residual_tail_per_channel as a collection of residual_tail so that residual tails add energy that complements but does not double-count modal energy.

sp_reverb_late_render
  treats late_config, position, layout, and sampled_input_partial_list as a complete description of the late-field drive.
  is linear in sampled_input_partial_list for fixed late_config, positon, and layout.
  is extensionally equivalent to a composition that maps sampled_input_partial_list to decay_output_partial_list_per_channel through modal_pole_set and channel_modal_set and then maps to late_channel_partial_list_per_channel and residual_tail_per_channel.
  ensures that the sum of modal and residual energy follows the decay behavior encoded in late_config.

sp_reverb_build_feedback_matrix
  interprets late_config at a given discrete period as a feedback operator on the state space.
  evaluates delay-line phase rotations and per-band attenuation at that period.
  applies mix so that the resulting matrix represents the full feedback operator g(z) on the unit circle at the corresponding angle.

sp_reverb_build_feedback_matrix_from_polar
  interprets radius and angle as defining a complex point z in the plane.
  evaluates per-delay response and attenuation at z using late_config.
  applies mix so that the resulting matrix represents g(z) at that point in the complex plane.

sp_reverb_form_identity_minus_feedback
  forms a matrix that represents i - g for a given feedback matrix g.
  prepares the operator needed both for determinant-based eigen equations and for linear solves with the same operator.

sp_reverb_lower_upper_factorization
  factors a matrix into lower_matrix and upper_matrix with a standard lu-like factorization.
  defines a representation that can be reused for multiple solves with the same coefficient matrix.

sp_reverb_lower_upper_solve
  solves a linear system for a given right-hand side using lower_matrix and upper_matrix.
  returns a vector that satisfies the system up to numerical solver accuracy.

sp_reverb_power_iteration_dominant_eigenpair
  applies power iteration to the matrix so that repeated application converges to the dominant eigenpair.
  returns eigen_value and eigen_vector that characterize the spectral radius and associated mode for validation and scaling.

sp_reverb_eigen_equation_value
  interprets radius and angle as a complex point z and builds g(z) from late_config.
  forms i - g(z) and evaluates its determinant as a complex number.
  returns real_value and imaginary_value as the real and imaginary parts of this determinant.

sp_reverb_eigen_equation_jacobian_finite_difference
  approximates the jacobian of the eigen equation with respect to radius and angle by finite differences.
  perturbs radius and angle around the current point with small increments.
  evaluates sp_reverb_eigen_equation_value at perturbed points.
  returns d_real_by_radius, d_real_by_angle, d_imag_by_radius, and d_imag_by_angle as finite-difference derivatives.

sp_reverb_newton_step_on_eigen_equation
  interprets jacobian and value as the local linearization of the eigen equation at the current (radius, angle).
  inverts the jacobian and applies a newton update in the (radius, angle) plane.
  returns next_radius and next_angle as the updated point for root refinement.
~~~