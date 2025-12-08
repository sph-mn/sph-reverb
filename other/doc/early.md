# early reflections
models geometric propagation by direct segments, specular reflections, and first-order diffraction on polygonal meshes and maps geometric paths to early partials per channel.

## structures
~~~
geometry
  vertex_list
  index_list
  triangle_material_index
  triangle_count
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

input_partial
  gain_function:{t -> gain}
  period_function:{t -> period}
  phase
  duration
  position

sampled_input_partial
  gain
  period
  phase
  position

early_sampled_partial
  channel_index
  gain
  period
  phase
  position
~~~

## signatures
~~~
sp_reverb_early_paths_direct ::
  scene
  source
  receiver
  ->
  path_set:(path ...)

sp_reverb_early_paths_specular ::
  scene
  source
  receiver
  cutoff
  ->
  path_set:(path ...)

sp_reverb_early_paths_diffraction ::
  scene
  source
  receiver
  edge_list
  cutoff
  ->
  path_set:(path ...)

sp_reverb_early_paths_union ::
  path_sets:(path ...) ...
  ->
  path_set:(path ...)

sp_reverb_early_paths_cull ::
  scene
  path_set:(path ...)
  threshold
  max_paths
  ->
  path_set:(path ...)

sp_reverb_early_partials_from_paths ::
  scene
  path_set:(path ...)
  layout
  sampled_input_partial_list:sampled_input_partial
  partial_count
  ->
  (early_sampled_partial ...)
~~~

## procedures
~~~
sp_reverb_early_paths_direct
  defines the direct geometric contribution between source and receiver inside scene.
  sets delay from geometric distance between source and receiver.
  sets direction_source and direction_receiver from the unique connecting segment in world coordinates.
  enforces visibility through the polygonal geometry so that occluded segments yield no path.
  returns at most one path with kind equal to the direct case and no spectral information.

sp_reverb_early_paths_specular
  defines specular reflection paths as geometric solutions of mirror constructions on triangle faces.
  uses mirrored receiver positions across sequences of triangles to enforce equality of incidence and reflection angles at each reflection.
  accepts only paths whose every segment satisfies visibility through scene geometry.
  uses cutoff so that paths with geometric extent or broadband energy below the cutoff do not appear in path_set.
  encodes each specular path by delay, direction_source, direction_receiver, triangle_chain, chain_length, order, and kind equal to the specular case.

sp_reverb_early_paths_diffraction
  defines first-order diffracted paths that involve exactly one user-provided edge.
  requires visibility of the segment from source to the edge and of the segment from the edge to receiver within scene.
  restricts diffracted paths to configurations where direct or specular propagation would be shadowed according to scene geometry.
  uses cutoff so that diffracted paths with geometric extent or broadband energy below the cutoff do not appear in path_set.
  encodes each diffracted path by delay, direction_source, direction_receiver, triangle_chain, chain_length, order, and kind equal to the diffraction case.

sp_reverb_early_paths_union
  treats each element of each input path_set as a candidate geometric path.
  defines two paths as equivalent when delay, direction_source, direction_receiver, triangle_chain, and kind agree up to representation.
  returns a path_set that contains exactly one representative of each equivalence class present in the inputs.
  makes the result independent of the ordering of the input path_sets.

sp_reverb_early_paths_cull
  associates each path in path_set with a broadband energy estimate derived from geometric length and material attenuation along triangle_chain in scene.
  removes all paths whose estimated energy lies below threshold.
  if the number of remaining paths exceeds max_paths, retains the suset with largest estimated energy and discards the others.
  preserves path identity so that surviving paths remain unchanged in their geometric fields.

sp_reverb_early_partials_from_paths
  maps each path in path_set and each sampled_input_partial in sampled_input_partial_list to early_sampled_partial instances.
  uses materials in scene and triangle_chain in path to derive attenuation and additional phase from geometric length for each band implied by band_period_list.
  uses layout and direction_receiver to derive channel_index and directional gain for each channel.
  sets gain and phase in each early_sampled_partial as a linear function of the input sampled_input_partial gain and phase and of the path response.
  sets period in each early_sampled_partial from the input sampled_input_partial period and the path delay relation.
  ensures that the early_sampled_partial list contains no contribution that cannot be attributed to some pair of path and sampled_input_partial.
~~~