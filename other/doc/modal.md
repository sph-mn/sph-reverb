# modal correction
defines low-frequency modes by a discrete helmholtz model and merges them with late-field modes across a transition band.

## structures
~~~
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
~~~

## signatures
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
~~~
sp_reverb_modal_build_grid
  treats scene geometry as the only source of spatial support for modal correction.
  defines a uniform cartesian grid whose bounding box encloses the scene geometry.
  marks interior_flag on grid samples so that interior and exterior regions form a partition.
  derives boundary_impedance only from scene materials on samples that lie on the geometric boundary.

sp_reverb_modal_build_operator
  defines modal_operator solely from modal_grid.
  forms laplacian as a linear discrete operator on grid samples with support limited to local neighbors.
  forms robin as a linear operator that acts only on boundary samples and depends only on boundary_impedance.
  ensures that the combined helmholtz operator is linear and real-valued for all admissible modal_grid.

sp_reverb_modal_solve_modes
  defines modal_mode_set as the set of eigenpairs of the helmholtz operator in the low-frequency band determined by modal_transition.
  interprets eigenvalues as defining modal periods and decays and stores these mappings in modal_mode.
  includes only modes whose periods lie on the low-frequency side of transition_period plus the overlap_width margin.
  ensures that eigenvectors form a basis for the represented low-frequency subspace up to numerical degeneracy.

sp_reverb_modal_state_excitation
  samples each modal eigenvector at position and defines excitation_vector entries from these samples.
  treats excitation_vector as a linear function of source amplitude for fixed modal_mode_set and position.
  ensures that modes with identical spatial patterns at position receive identical excitation in excitation_vector.

sp_reverb_modal_state_projection
  samples modal eigenvectors at receiver positions derived from layout and defines projection_vector_per_channel from these samples.
  treats projection_vector_per_channel as independent across channels for fixed modal_mode_set and layout.
  ensures that channels with identical spatial configuration share identical projection vectors.

sp_reverb_modal_residues
  combines excitation_vector and projection_vector_per_channel into modal_residue_set.
  defines each modal_residue as a separable product of excitation and projection factors possibly up to a global normalization.
  ensures that modal_residue_set is linear in excitation_vector and in projection_vector_per_channel for fixed modal_mode_set.

sp_reverb_modal_render_partials
  maps modal_mode_set and modal_residue_set to modal_output_partial_list_per_channel.
  interprets each mode as a single exponentially decaying sinusoid per channel.
  assigns gain from residue magnitude, period from modal_mode.period, phase from residue phase, and decay from modal_mode.decay.
  ensures that the mapping from modal_mode_set and modal_residue_set to modal_output_partial_list_per_channel is linear in modal_residue_set.

sp_reverb_modal_merge_with_late
  combines modal_output_partial_list_per_channel with channel_output_partial_list_per_channel from the late field under modal_transition.
  treats modal_transition.transition_period as the central period that separates helmholtz-dominated and fdn-dominated modes.
  uses modal_transition.overlap_width to define a crossfade band in period where both contributions coexist.
  ensures that the merged channel_output_partial_list_per_channel depends continuously on modal_transition within the overlap band and reduces to pure helmholtz or pure fdn outside it.
~~~
